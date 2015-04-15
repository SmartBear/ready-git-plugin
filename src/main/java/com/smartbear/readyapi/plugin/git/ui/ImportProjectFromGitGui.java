package com.smartbear.readyapi.plugin.git.ui;

import com.eviware.soapui.plugins.vcs.ImportProjectFromVcsGui;
import com.eviware.soapui.plugins.vcs.VcsIntegrationException;
import com.eviware.soapui.plugins.vcs.VcsRepositoryInfo;
import com.smartbear.readyapi.plugin.git.ReadyApiGitIntegration;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.awt.Component;
import java.io.File;

public class ImportProjectFromGitGui extends AbstractRepositorySelectionGui implements ImportProjectFromVcsGui {
    private ReadyApiGitIntegration readyApiGitIntegration;

    public ImportProjectFromGitGui(ReadyApiGitIntegration readyApiGitIntegration) {
        this.readyApiGitIntegration = readyApiGitIntegration;
    }

    @Override
    public Component getComponent() {
        return createGui("http://readyapi.smartbear.com","Learn about importing projects using Git");
    }

    @Override
    public VcsRepositoryInfo downloadProjectFiles(File emptyDirectory) {
        try {
            readyApiGitIntegration.cloneRepository(getSelected().getRepositoryPath(), getSelected().getCredentialsProvider(), emptyDirectory);
            return new VcsRepositoryInfo("Git", getSelected().getRepositoryPath());
        } catch (GitAPIException e) {
            throw new VcsIntegrationException("Failed to clone remote repository", e);
        }
    }

    @Override
    public boolean isValidInput() {
        return getSelected().isValid();
    }
}
