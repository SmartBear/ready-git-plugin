package com.smartbear.readyapi.plugin.git;

import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.model.workspace.Workspace;
import com.eviware.soapui.plugins.vcs.ActivationStatus;
import com.eviware.soapui.plugins.vcs.CommitResult;
import com.eviware.soapui.plugins.vcs.HistoryEntry;
import com.eviware.soapui.plugins.vcs.ImportProjectFromVcsGui;
import com.eviware.soapui.plugins.vcs.LockHandler;
import com.eviware.soapui.plugins.vcs.RepositorySelectionGui;
import com.eviware.soapui.plugins.vcs.VcsIntegration;
import com.eviware.soapui.plugins.vcs.VcsIntegrationConfiguration;
import com.eviware.soapui.plugins.vcs.VcsIntegrationException;
import com.eviware.soapui.plugins.vcs.VcsUpdate;
import com.eviware.soapui.support.UISupport;
import com.smartbear.readyapi.plugin.git.ui.GitRepositorySelectionGui;
import com.smartbear.readyapi.plugin.git.ui.ImportProjectFromGitGui;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.eviware.soapui.plugins.vcs.CommitResult.CommitStatus.FAILED;
import static com.eviware.soapui.plugins.vcs.CommitResult.CommitStatus.SUCCESSFUL;

@VcsIntegrationConfiguration(name = "Git", description = "Git Version Control System")
public class ReadyApiGitIntegration implements VcsIntegration {

    public static final int MAX_LOG_ENTRIES = 500;
    private final static Logger logger = LoggerFactory.getLogger(ReadyApiGitIntegration.class);
    private GitCommandHelper gCommandHelper  = new GitCommandHelper();

    @Override
    public ActivationStatus activateFor(WsdlProject project) {
        return null;
    }

    @Override
    public RepositorySelectionGui buildRepositorySelectionGui(WsdlProject project) {
        return new GitRepositorySelectionGui(project);
    }

    @Override
    public ImportProjectFromVcsGui buildRepositoryDownloadGui(Workspace workspace) {
        return new ImportProjectFromGitGui();
    }

    @Override
    public Collection<VcsUpdate> getRemoteRepositoryUpdates(File projectFile) {
        Collection<VcsUpdate> updates = new ArrayList<>();
        try {
            final Git gitObject = gCommandHelper.createGitObject(projectFile.getPath());

            gCommandHelper.gitFetch(gitObject);

            Repository repo = gitObject.getRepository();
            ObjectReader reader = repo.newObjectReader();

            gCommandHelper.fillRemoteUpdates(updates, gitObject, repo, reader);
            gitObject.getRepository().close();
        } catch (GitAPIException | IOException e) {
            throw new VcsIntegrationException(e.getMessage(), e.getCause());
        }
        return updates;
    }

    @Override
    public Collection<VcsUpdate> getLocalRepositoryUpdates(WsdlProject project) {
        final Git git = gCommandHelper.createGitObject(project.getPath());
        Collection<VcsUpdate> updates = new ArrayList<>();

        try {
            final Status status = git.status().call();
            gCommandHelper.fillLocalUpdates(project, updates, status);
        } catch (GitAPIException | IOException e) {
            logger.error("Failed to read local changes", e);
            throw new VcsIntegrationException(e.getMessage(), e.getCause());
        }
        git.getRepository().close();

        return updates;
    }

    @Override
    public LockHandler getLockHandler() {
        return null;
    }

    @Override
    public void updateFromRemoteRepository(File projectFile, boolean b) {
        try {
            final Git gitObject = gCommandHelper.createGitObject(projectFile.getPath());

            final Set<String> uncommittedChanges = gitObject.status().call().getUncommittedChanges();
            if (uncommittedChanges.size() > 0) {
                UISupport.showErrorMessage("There are uncommitted changes, commit or revert back those changes before updating from remote repo");
                return;
            }
            MergeStrategy mergeStrategy = gCommandHelper.promptForMergeStrategy();
            if (mergeStrategy == null) {
                return;
            }

            gCommandHelper.pullWithMergeStrategy(gitObject, mergeStrategy);
            gitObject.getRepository().close();
            UISupport.showInfoMessage("Remote changes were pulled successfully.");
        } catch (GitAPIException e) {
            throw new VcsIntegrationException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void deleteFile(WsdlProject project, File file) throws IOException {

    }

    @Override
    public CommitResult commit(Collection<VcsUpdate> vcsUpdates, String commitMessage) {
        VcsUpdate update;

        if (vcsUpdates.isEmpty()) {
            return new CommitResult(CommitResult.CommitStatus.FAILED, "Nothing to commit");
        } else {
            update = vcsUpdates.iterator().next();
        }

        final WsdlProject project = update.getProject();
        final Git git = gCommandHelper.createGitObject(project.getPath());

        final boolean successfulUpdate = gCommandHelper.commitAndPushUpdates(vcsUpdates, commitMessage, git);

        CommitResult result = successfulUpdate ? new CommitResult(SUCCESSFUL, "Commit was successful") :
                new CommitResult(FAILED, "Commit Failed");

        git.getRepository().close();

        return result;
    }

    @Override
    public void revert(Collection<VcsUpdate> vcsUpdates) throws VcsIntegrationException {

        VcsUpdate update;

        if (vcsUpdates.isEmpty()) {
            return;
        } else {
            update = vcsUpdates.iterator().next();
        }

        final Git gitObject = gCommandHelper.createGitObject(update.getProject().getPath());

        for (VcsUpdate vcsUpdate : vcsUpdates) {
            try {
                gitObject.checkout().addPath(vcsUpdate.getRelativePath()).call();
            } catch (GitAPIException e) {
                throw new VcsIntegrationException(e.getMessage(), e.getCause());
            }
        }
        gitObject.getRepository().close();
    }

    @Override
    public Set<String> getAvailableTags(WsdlProject project) throws VcsIntegrationException {
        final List<Ref> refList;
        final Git gitObject = gCommandHelper.createGitObject(project.getPath());

        try {
            gCommandHelper.gitFetch(gitObject);
            refList = gitObject.tagList().call();
            gitObject.getRepository().close();
        } catch (GitAPIException e) {
            throw new VcsIntegrationException(e.getMessage(), e.getCause());
        }

        return gCommandHelper.getTagSetFromRefList(refList);
    }

    @Override
    public void createTag(WsdlProject project, String tagName) {
        final Git gitObject = gCommandHelper.createGitObject(project.getPath());
        try {
            gCommandHelper.gitCreateAndPushTag(tagName, gitObject);
            gitObject.getRepository().close();
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

    @Override
    public List<HistoryEntry> getProjectHistory(WsdlProject project) {
        List<HistoryEntry> historyEntries = new ArrayList<>();
        Git gitObject = gCommandHelper.createGitObject(project.getPath());

        try {
            Iterable<RevCommit> revCommits = gitObject.log().setMaxCount(MAX_LOG_ENTRIES).call();
            for (RevCommit revCommit : revCommits) {
                String revisionName = revCommit.getId().getName();
                PersonIdent authorIdent = revCommit.getAuthorIdent();
                String fullMessage = revCommit.getFullMessage();
                historyEntries.add(new HistoryEntry(revisionName, authorIdent.getWhen(), authorIdent.getName(), fullMessage));
                gitObject.getRepository().close();
            }
        } catch (GitAPIException e) {
            throw new VcsIntegrationException(e.getMessage(), e.getCause());
        }
        return historyEntries;
    }

}
