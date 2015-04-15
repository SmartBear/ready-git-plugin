package com.smartbear.readyapi.plugin.git;

import com.eviware.soapui.plugins.vcs.ImportProjectFromVcsGui;
import com.eviware.soapui.plugins.vcs.VcsIntegrationException;
import com.eviware.soapui.plugins.vcs.VcsRepositoryInfo;
import com.smartbear.readyapi.plugin.git.ui.SshRepositoryForm;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.awt.Component;
import java.io.File;

public class ImportProjectFromGitGui implements ImportProjectFromVcsGui {
    private ReadyApiGitIntegration readyApiGitIntegration;

    private SshRepositoryForm sshRepositoryForm = new SshRepositoryForm();

    public ImportProjectFromGitGui(ReadyApiGitIntegration readyApiGitIntegration) {
        this.readyApiGitIntegration = readyApiGitIntegration;
    }

    @Override
    public Component getComponent() {
        return sshRepositoryForm.getComponent();
    }

    @Override
    public VcsRepositoryInfo downloadProjectFiles(File emptyDirectory) {
        try {
            readyApiGitIntegration.cloneRepository(sshRepositoryForm.getRepositoryPath(), emptyDirectory);
            return new VcsRepositoryInfo("Git", sshRepositoryForm.getRepositoryPath());
        } catch (GitAPIException e) {
            throw new VcsIntegrationException("Failed to clone remote repository", e);
        }
    }

    @Override
    public boolean isValidInput() {
        return true;
    }
}
