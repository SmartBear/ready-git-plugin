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
import com.smartbear.readyapi.plugin.git.ui.GitRepositorySelectionGui;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.PushResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.eviware.soapui.plugins.vcs.VcsUpdate.VcsUpdateType.ADDED;
import static com.eviware.soapui.plugins.vcs.VcsUpdate.VcsUpdateType.DELETED;
import static com.eviware.soapui.plugins.vcs.VcsUpdate.VcsUpdateType.MODIFIED;

@VcsIntegrationConfiguration(name = "Git", description = "Git Version Control System")
public class ReadyApiGitIntegration implements VcsIntegration {

    private final static Logger logger = LoggerFactory.getLogger(ReadyApiGitIntegration.class);


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
        final Git git = getGitObject(project);
        Collection<VcsUpdate> updates = new ArrayList<>();

        try {
            final Status status = git.status().call();
            fillLocalUpdates(project, updates, status);
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        git.getRepository().close();

        return updates;
    }

    private void fillLocalUpdates(WsdlProject project, Collection<VcsUpdate> updates, Status status) {
        for(String fileAdded: status.getAdded()){
            updates.add(new VcsUpdate(project, ADDED, fileAdded, fileAdded));
        }

        for(String fileChanged: status.getChanged()){
            updates.add(new VcsUpdate(project, MODIFIED, fileChanged, fileChanged));
        }

        for(String fileChanged: status.getRemoved()){
            updates.add(new VcsUpdate(project, DELETED, fileChanged, fileChanged));
        }

        for(String fileChanged: status.getMissing()){
            updates.add(new VcsUpdate(project, DELETED, fileChanged, fileChanged));
        }

        for(String fileChanged: status.getModified()){
            updates.add(new VcsUpdate(project, MODIFIED, fileChanged, fileChanged));
        }

        for(String fileChanged: status.getUntracked()){
            updates.add(new VcsUpdate(project, ADDED, fileChanged, fileChanged));
        }

        for(String fileChanged: status.getUntrackedFolders()){
            updates.add(new VcsUpdate(project, ADDED, fileChanged, fileChanged));
        }

        for(String fileChanged: status.getConflicting()){
            final VcsUpdate update = new VcsUpdate(project, MODIFIED, fileChanged, fileChanged);
            update.setConflictingUpdate(true);
            updates.add(update);
        }
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
    public CommitResult commit(Collection<VcsUpdate> vcsUpdates, String commitMessage) {
        VcsUpdate update = null;

        if (vcsUpdates.isEmpty()) {
            return new CommitResult(CommitResult.CommitStatus.FAILED, "Nothing to commit");
        } else {
            update = vcsUpdates.iterator().next();
        }

        final WsdlProject project = update.getProject();
        final Git git = getGitObject(project);

        /*for (VcsUpdate vcsUpdate : vcsUpdates) {

            if (!vcsUpdate.getProject().equals(project)) {
                throw new IllegalArgumentException("There are updates from different projects: " + project.getName() +
                        ",  " + vcsUpdate.getProject().getName());
            }
        }*/
        final Iterable<PushResult> pushResults = commitUpdates(vcsUpdates, commitMessage, git);

        return new CommitResult(CommitResult.CommitStatus.SUCCESSFUL,"");
    }

    private Iterable<PushResult> commitUpdates(Collection<VcsUpdate> vcsUpdates, String commitMessage, Git git) {
        for (VcsUpdate vcsUpdate : vcsUpdates) {
            try {
                git.add().addFilepattern(vcsUpdate.getRelativePath()).call();

            } catch (GitAPIException e) {
                throw new VcsIntegrationException(e.getMessage(), e.getCause());
            }
        }

        try {
            git.commit().setMessage(commitMessage).call();
            //FIXME: Should be a dry run and if there is any conflict it should ask user whether to go ahead or not
            final Iterable<PushResult> results = git.push().call();
            git.getRepository().close();
            return results;
        } catch (GitAPIException e) {
            throw new VcsIntegrationException(e.getMessage(), e.getCause());
        }
    }


    @Override
    public void revert(Collection<VcsUpdate> vcsUpdates) throws VcsIntegrationException {

    }

    @Override
    public Set<String> getAvailableTags(WsdlProject project) throws VcsIntegrationException {
        final List<Ref> refList;
        final Git git = getGitObject(project);

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
    public void createTag(WsdlProject project, String tagName) {
        Git git = getGitObject(project);
        try {
            git.tag().setName(tagName).call();
            git.push().setPushTags().call();
        } catch (RefAlreadyExistsException re) {
            logger.warn("Tag already exists: " + tagName);
            throw new IllegalArgumentException("Tag already exists: " + tagName);
        } catch (GitAPIException e) {
            throw new VcsIntegrationException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public List<HistoryEntry> getFileHistory(WsdlProject project, File file) {
        return null;
    }

    private Git getGitObject(WsdlProject project) {
        final String localPath = project.getPath();
        final Repository localRepo;

        try {
            localRepo = new FileRepository(localPath + "/.git");
            if(!localRepo.getObjectDatabase().exists()){
                logger.error("No git repo exist in: " + localPath);
                throw new IllegalStateException("No git repo exist in: " + localPath);
            }
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
