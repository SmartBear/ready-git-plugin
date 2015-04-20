package com.smartbear.readyapi.plugin.git.ui;

import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.plugins.vcs.RepositorySelectionGui;
import com.smartbear.readyapi.plugin.git.ReadyApiGitIntegration;

import java.awt.Component;
import java.beans.PropertyChangeListener;

import static com.smartbear.readyapi.plugin.git.ui.help.HelpUrls.GIT_PLUGIN_WIKI;

public class GitRepositorySelectionGui extends AbstractRepositorySelectionGui implements RepositorySelectionGui {

    private WsdlProject project;
    private ReadyApiGitIntegration gitIntegration;

    public GitRepositorySelectionGui(WsdlProject project, ReadyApiGitIntegration gitIntegration) {
        this.project = project;
        this.gitIntegration = gitIntegration;
    }

    @Override
    public Component getComponent() {
        return createGui(GIT_PLUGIN_WIKI, "Learn about sharing projects with Git");
    }

    @Override
    public void createRemoteRepository() {
        gitIntegration.shareProject(project, getSelected().getRepositoryPath(), getSelected().getCredentialsProvider());
    }

    @Override
    public String getRemoteRepositoryId() {
        return getSelected().getRepositoryPath();
    }

    @Override
    public boolean isValidInput() {
        return getSelected().isValid();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
    }

}
