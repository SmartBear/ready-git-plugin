package com.smartbear.readyapi.plugin.git.ui;

import com.eviware.soapui.plugins.vcs.ImportProjectFromVcsGui;
import com.eviware.soapui.plugins.vcs.VcsIntegrationException;
import com.eviware.soapui.plugins.vcs.VcsRepositoryInfo;
import com.eviware.soapui.support.StringUtils;
import com.smartbear.readyapi.plugin.git.GitCommandHelper;

import java.awt.Component;
import java.io.File;

import static com.smartbear.readyapi.plugin.git.ui.help.HelpUrls.GIT_PLUGIN_WIKI;

public class ImportProjectFromGitGui extends AbstractRepositorySelectionGui implements ImportProjectFromVcsGui {
    private GitCommandHelper gitCommandHelper;

    public ImportProjectFromGitGui() {
        this.gitCommandHelper = new GitCommandHelper();
    }

    @Override
    public Component getComponent() {
        return createGui(GIT_PLUGIN_WIKI, "Learn about importing projects using Git", null);
    }

    @Override
    public VcsRepositoryInfo downloadProjectFiles(File emptyDirectory) {
        try {
            String repositoryPath = getSelected().getRepositoryPath();
            if (isLocal() || StringUtils.isNullOrEmpty(repositoryPath)) {
                return new VcsRepositoryInfo("Git",
                        gitCommandHelper.getRemoteRepositoryUrl(emptyDirectory.getAbsolutePath()));
            } else {
                gitCommandHelper.cloneRepository(repositoryPath, getSelected().getCredentialsProvider(), emptyDirectory);
                return new VcsRepositoryInfo("Git", repositoryPath);
            }
        } catch (Exception e) {
            throw new VcsIntegrationException("Failed to clone the remote repository.", e);
        }
    }

    @Override
    public boolean isValidInput() {
        return getSelected().isValid();
    }

    @Override
    public boolean isLocal() {
        return getSelected().isLocal();
    }
}
