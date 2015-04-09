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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import com.smartbear.readyapi.plugin.git.ui.GitRepositorySelectionGui;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
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
        return new GitRepositorySelectionGui(project);
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
    public void moveFile(WsdlProject project, File file, File targetFile) throws IOException {

    }

    @Override
    public CommitResult commit(Collection<VcsUpdate> vcsUpdates) {
        return null;
    }

    @Override
    public void revert(Collection<VcsUpdate> vcsUpdates) throws VcsIntegrationException {

    }

    @Override
    public Set<String> getAvailableTags(WsdlProject project) throws VcsIntegrationException {
        final List<Ref> refList;
        Git git = getGitProject(project);

        try {
            git.fetch().call(); //To make sure we fetch the latest tags. Also fetch is more or less a harmless operation.
            refList = git.tagList().call();
        } catch (GitAPIException e) {
            throw new VcsIntegrationException(e.getMessage(), e.getCause());
        }
        git.getRepository().close();

        return getTagSetFromRefList(refList);
    }

    @Override
    public void createTag(WsdlProject project, String s) {

    }

    @Override
    public List<HistoryEntry> getFileHistory(WsdlProject project, File file) {
        return null;
    }

    private Git getGitProject(WsdlProject project) {
        final String localPath = project.getPath();
        final Repository localRepo;

        try {
            localRepo = new FileRepository(localPath + "/.git");
        } catch (IOException e) {
            throw new VcsIntegrationException(e.getMessage(), e.getCause());
        }
        return new Git(localRepo);
    }

    private Set<String> getTagSetFromRefList(List<Ref> refList) {
        Set<String> tagSet = new HashSet<String>();
        for (Ref ref : refList) {
            tagSet.add(ref.getName());
        }
        return tagSet;
    }

}
