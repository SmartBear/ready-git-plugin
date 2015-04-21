package com.smartbear.readyapi.plugin.git;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.settings.DESCipher;
import com.eviware.soapui.support.types.StringList;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to keep the in memory copy of CredentialsProvider so that user doesn't have to enter credentials for VCS every operation (pull, push, fetch)
 */
public class GitCredentialProviderCache {

    private static final String VCS_REPO_SETTINGS = "VcsSettings@repoSettings";
    public static final String SEPARATOR = "#_#";

    private Map<String, CredentialsProvider> credentialsProviderMap = new HashMap<>();


    private static GitCredentialProviderCache instance = new GitCredentialProviderCache();

    private GitCredentialProviderCache() {
        readSavedSettings();
    }

    public static GitCredentialProviderCache instance() {
        return instance;
    }

    public void addCredentialProvider(CredentialsProvider credentialsProvider, String gitRepo) {
        if (credentialsProvider != null) {
            credentialsProviderMap.put(gitRepo, credentialsProvider);
            recreateAndSaveRepositorySettings();
        }
    }

    public CredentialsProvider getCredentialsProvider(String gitRepo) {
        return credentialsProviderMap.get(gitRepo);
    }

    private void readSavedSettings() {
        try {
            String repoSettings = SoapUI.getSettings().getString(VCS_REPO_SETTINGS, "");
            StringList stringList = StringList.fromXml(repoSettings);
            for (String s : stringList) {
                String[] values = s.split(SEPARATOR);
                String repoUrl = values[0];
                String userNameOrKeyPath = DESCipher.decrypt(values[1]);
                String password = DESCipher.decrypt(values[2]);
                if (repoUrl.startsWith("http")) {
                    addCredentialProvider(new UsernamePasswordCredentialsProvider(userNameOrKeyPath, password), repoUrl);
                } else {
                    addCredentialProvider(new SshPassphraseCredentialsProvider(password, userNameOrKeyPath), repoUrl);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void recreateAndSaveRepositorySettings() {
        StringList repoSettingsList = new StringList();
        for (Map.Entry<String, CredentialsProvider> credentialsProviderEntry : credentialsProviderMap.entrySet()) {
            Credentials credentials = (Credentials) credentialsProviderEntry.getValue();
            try {
                StringBuilder stringBuilder = new StringBuilder(credentialsProviderEntry.getKey());
                stringBuilder.append(SEPARATOR);
                stringBuilder.append(DESCipher.encrypt(credentials.getUsername()));
                stringBuilder.append(SEPARATOR);
                stringBuilder.append(DESCipher.encrypt(credentials.getPassword()));
                repoSettingsList.add(stringBuilder.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        SoapUI.getSettings().setString(VCS_REPO_SETTINGS, repoSettingsList.toXml());
        try {
            SoapUI.saveSettings();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
