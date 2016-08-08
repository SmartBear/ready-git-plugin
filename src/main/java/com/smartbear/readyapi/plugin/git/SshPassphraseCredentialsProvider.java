package com.smartbear.readyapi.plugin.git;

import com.eviware.soapui.support.UISupport;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;

public class SshPassphraseCredentialsProvider extends CredentialsProvider implements Credentials {
    private String passphrase;
    private String privateKeyPath;

    public SshPassphraseCredentialsProvider(String passphrase, String privateKeyPath) {
        this.passphrase = passphrase;
        this.privateKeyPath = privateKeyPath;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public String getPassword() {
        return passphrase;
    }

    /**
     * @return private key path as username
     */
    @Override
    public String getUsername() {
        //Have a look at GitCredentialProviderCache before you change this. There it uses Credentials#getUsername() method to read username/key file path
        return getPrivateKeyPath();
    }

    @Override
    public boolean isInteractive() {
        return false;
    }

    @Override
    public boolean supports(CredentialItem... items) {
        return true;
    }

    @Override
    public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
        for (CredentialItem item : items) {
            if (item instanceof CredentialItem.StringType) {
                ((CredentialItem.StringType) item).setValue(passphrase);
            } else if (item instanceof CredentialItem.Password) {
                ((CredentialItem.Password) item).setValue(passphrase.toCharArray());
            } else if (item instanceof CredentialItem.YesNoType) {
                ((CredentialItem.YesNoType) item).setValue(UISupport.confirm(item.getPromptText(), "SSH connection"));
            }
        }
        return true;
    }
}
