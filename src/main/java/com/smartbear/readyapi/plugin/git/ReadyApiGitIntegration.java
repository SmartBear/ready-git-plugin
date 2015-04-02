package com.smartbear.readyapi.plugin.git;

import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.workspace.Workspace;
import com.eviware.soapui.plugins.vcs.ActivationStatus;
import com.eviware.soapui.plugins.vcs.AuthenticationStatus;
import com.eviware.soapui.plugins.vcs.CommitResult;
import com.eviware.soapui.plugins.vcs.HistoryEntry;
import com.eviware.soapui.plugins.vcs.LockHandler;
import com.eviware.soapui.plugins.vcs.RepositorySelectionGui;
import com.eviware.soapui.plugins.vcs.VcsIntegration;
import com.eviware.soapui.plugins.vcs.VcsIntegrationConfiguration;
import com.eviware.soapui.plugins.vcs.VcsIntegrationException;
import com.eviware.soapui.plugins.vcs.VcsUpdate;
import com.smartbear.readyapi.plugin.git.ui.GitRepositorySelectionGui;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@VcsIntegrationConfiguration(name = "Git", description = "Git Version Control System")
public class ReadyApiGitIntegration implements VcsIntegration {
    @Override
    public ActivationStatus activateFor(Project project) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public RepositorySelectionGui buildRepositorySelectionGui(Project project) {
        return new GitRepositorySelectionGui(project);
    }

    @Override
    public Component buildRepositoryDownloadGui(Workspace workspace) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public AuthenticationStatus authenticate() {
       return null;
    }

    @Override
    public AuthenticationStatus checkAuthenticationStatus(Project project) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<VcsUpdate> getRemoteRepositoryUpdates(Project project) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<VcsUpdate> getLocalRepositoryUpdates(Project project) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public LockHandler getLockHandler() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updateFromRemoteRepository(Project project, boolean b) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void deleteFile(File file) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void moveFile(File file) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public CommitResult commit(Collection<VcsUpdate> vcsUpdates) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void revert(VcsUpdate vcsUpdate) throws VcsIntegrationException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<String> getAvailableTags() throws VcsIntegrationException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void createTag(String s) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<HistoryEntry> getFileHistory(File file) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
