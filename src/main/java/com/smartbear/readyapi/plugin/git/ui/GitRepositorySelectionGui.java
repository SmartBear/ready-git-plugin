package com.smartbear.readyapi.plugin.git.ui;

import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.plugins.vcs.RepositorySelectionGui;
import com.smartbear.readyapi.plugin.git.GitCommandHelper;

import java.awt.Component;

import static com.smartbear.readyapi.plugin.git.ui.help.HelpUrls.GIT_PLUGIN_WIKI;

public class GitRepositorySelectionGui extends AbstractRepositorySelectionGui implements RepositorySelectionGui {

    private WsdlProject project;
    private GitCommandHelper gitCommandHelper;

    public GitRepositorySelectionGui(WsdlProject project) {
        this.project = project;
        this.gitCommandHelper = new GitCommandHelper();
    }

    @Override
    public Component getComponent() {
        return createGui(GIT_PLUGIN_WIKI, "Learn about sharing projects with Git", gitCommandHelper.getRemoteRepositoryUrl(project));
    }

    @Override
    public void initializeRepository() {
        gitCommandHelper.shareProject(project, getSelected().getRepositoryPath(), getSelected().getCredentialsProvider());
    }

    @Override
    public String getRemoteRepositoryId() {
        return getSelected().getRepositoryPath();
    }

    @Override
    public boolean isValidInput() {
        return getSelected().isValid();
    }
}
