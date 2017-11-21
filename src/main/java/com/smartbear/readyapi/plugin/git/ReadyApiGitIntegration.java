package com.smartbear.readyapi.plugin.git;

import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.model.workspace.Workspace;
import com.eviware.soapui.plugins.vcs.ActivationStatus;
import com.eviware.soapui.plugins.vcs.CommitResult;
import com.eviware.soapui.plugins.vcs.HistoryEntry;
import com.eviware.soapui.plugins.vcs.ImportProjectFromVcsGui;
import com.eviware.soapui.plugins.vcs.LockHandler;
import com.eviware.soapui.plugins.vcs.RepositorySelectionGui;
import com.eviware.soapui.plugins.vcs.VcsBranch;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.eviware.soapui.plugins.vcs.CommitResult.CommitStatus.FAILED;
import static com.eviware.soapui.plugins.vcs.CommitResult.CommitStatus.SUCCESSFUL;

@VcsIntegrationConfiguration(name = "Git", description = "Git Version Control System")
public class ReadyApiGitIntegration implements VcsIntegration {

    public static final int MAX_LOG_ENTRIES = 500;
    private final static Logger logger = LoggerFactory.getLogger(ReadyApiGitIntegration.class);
    private GitCommandHelper gitCommandHelper = new GitCommandHelper();

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
            final Git gitObject = gitCommandHelper.createGitObject(projectFile.getPath());

            gitCommandHelper.gitFetch(gitObject);

            Repository repo = gitObject.getRepository();
            ObjectReader reader = repo.newObjectReader();

            gitCommandHelper.fillRemoteUpdates(updates, gitObject, repo, reader);
            gitObject.getRepository().close();
        } catch (GitAPIException | IOException e) {
            throw new VcsIntegrationException(e.getMessage(), e.getCause());
        }
        return updates;
    }

    @Override
    public Collection<VcsUpdate> getLocalRepositoryUpdates(WsdlProject project) {
        final Git gitObject = gitCommandHelper.createGitObject(project.getPath());
        Collection<VcsUpdate> updates = new ArrayList<>();

        try {
            final Status status = gitObject.status().call();
            gitCommandHelper.fillLocalUpdates(project, updates, status);
        } catch (GitAPIException | IOException e) {
            logger.error("Failed to read local changes", e);
            throw new VcsIntegrationException(e.getMessage(), e.getCause());
        }
        gitObject.getRepository().close();

        return updates;
    }

    @Override
    public LockHandler getLockHandler() {
        return null;
    }

    @Override
    public void updateFromRemoteRepository(File projectFile, boolean b) {
        try {
            final Git gitObject = gitCommandHelper.createGitObject(projectFile.getPath());

            final Set<String> uncommittedChanges = gitObject.status().call().getUncommittedChanges();
            if (uncommittedChanges.size() > 0) {
                UISupport.showErrorMessage("There are uncommitted changes, commit or revert back those changes before updating from remote repo");
                return;
            }
            MergeStrategy mergeStrategy = gitCommandHelper.promptForMergeStrategy();
            if (mergeStrategy == null) {
                return;
            }

            final boolean successfulPull = gitCommandHelper.pullWithMergeStrategy(gitObject, mergeStrategy);
            gitObject.getRepository().close();

            if (successfulPull) {
                UISupport.showInfoMessage("Remote changes were pulled successfully.");
            } else {
                UISupport.showErrorMessage("Failed to pull remote changes.");
            }

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
        final Git gitObject = gitCommandHelper.createGitObject(project.getPath());

        final boolean successfulUpdate = gitCommandHelper.commitAndPushUpdates(vcsUpdates, commitMessage, gitObject);

        CommitResult result = successfulUpdate ? new CommitResult(SUCCESSFUL, "Commit was successful") :
                new CommitResult(FAILED, "Commit Failed");

        gitObject.getRepository().close();

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

        final Git gitObject = gitCommandHelper.createGitObject(update.getProject().getPath());

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
        final Git gitObject = gitCommandHelper.createGitObject(project.getPath());

        try {
            gitCommandHelper.gitFetch(gitObject);
            refList = gitObject.tagList().call();
            gitObject.getRepository().close();
        } catch (GitAPIException e) {
            throw new VcsIntegrationException(e.getMessage(), e.getCause());
        }

        return gitCommandHelper.getTagSetFromRefList(refList);
    }

    @Override
    public void createTag(WsdlProject project, String tagName) {
        final Git gitObject = gitCommandHelper.createGitObject(project.getPath());
        try {
            gitCommandHelper.gitCreateAndPushTag(tagName, gitObject);
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
        Git gitObject = gitCommandHelper.createGitObject(project.getPath());

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

    @Override
    public boolean switchBranch(WsdlProject project, VcsBranch branch) {
        final Git gitObject = gitCommandHelper.createGitObject(project.getPath());
        if (!branch.isCurrent()) {
            gitCommandHelper.checkout(branch.getName(), gitObject);
            return true;
        }
        return false;
    }

    @Override
    public List<VcsBranch> getBranchList(WsdlProject project) {
        final Git gitObject = gitCommandHelper.createGitObject(project.getPath());
        String currentBranch = gitCommandHelper.getCurrentBranch(gitObject);
        return Collections.unmodifiableList(gitCommandHelper.getBranchList(gitObject)
                .stream()
                .filter(Objects::nonNull)
                .map(s -> new VcsBranch(s, s.equals(currentBranch)))
                .collect(Collectors.toList()));
    }
}
