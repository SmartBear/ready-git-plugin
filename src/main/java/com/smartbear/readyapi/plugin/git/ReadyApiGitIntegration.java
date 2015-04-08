package com.smartbear.readyapi.plugin.git;

import com.eviware.soapui.impl.wsdl.WsdlProject;
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

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@VcsIntegrationConfiguration(name = "Git", description = "Git Version Control System")
public class ReadyApiGitIntegration implements VcsIntegration {


    @Override
    public ActivationStatus activateFor(WsdlProject project) {
        return null;
    }

    @Override
    public RepositorySelectionGui buildRepositorySelectionGui(WsdlProject project) {
        return null;
    }

    @Override
    public Component buildRepositoryDownloadGui(Workspace workspace) {
        return null;
    }

    @Override
    public AuthenticationStatus authenticate(WsdlProject project) {
        return null;
    }

    @Override
    public AuthenticationStatus checkAuthenticationStatus(WsdlProject project) {
        return null;
    }

    @Override
    public Collection<VcsUpdate> getRemoteRepositoryUpdates(WsdlProject project) {
        return null;
    }

    @Override
    public Collection<VcsUpdate> getLocalRepositoryUpdates(WsdlProject project) {
        return null;
    }

    @Override
    public LockHandler getLockHandler() {
        return null;
    }

    @Override
    public void updateFromRemoteRepository(WsdlProject project, boolean b) {

    }

    @Override
    public void deleteFile(WsdlProject project, File file) throws IOException {

    }

    @Override
    public void moveFile(WsdlProject project, File file) throws IOException {

    }

    @Override
    public CommitResult commit(WsdlProject project, Collection<VcsUpdate> vcsUpdates) {
        return null;
    }

    @Override
    public void revert(WsdlProject project, VcsUpdate vcsUpdate) throws VcsIntegrationException {

    }

    @Override
    public Set<String> getAvailableTags(WsdlProject project) throws VcsIntegrationException {
        return null;
    }

    @Override
    public void createTag(WsdlProject project, String s) {

    }

    @Override
    public List<HistoryEntry> getFileHistory(WsdlProject project, File file) {
        return null;
    }
}
