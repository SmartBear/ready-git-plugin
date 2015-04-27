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
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.eviware.soapui.plugins.vcs.CommitResult.CommitStatus.FAILED;
import static com.eviware.soapui.plugins.vcs.CommitResult.CommitStatus.SUCCESSFUL;
import static com.eviware.soapui.plugins.vcs.VcsUpdate.VcsUpdateType.ADDED;
import static com.eviware.soapui.plugins.vcs.VcsUpdate.VcsUpdateType.DELETED;
import static com.eviware.soapui.plugins.vcs.VcsUpdate.VcsUpdateType.MODIFIED;

@VcsIntegrationConfiguration(name = "Git", description = "Git Version Control System")
public class ReadyApiGitIntegration implements VcsIntegration {

    public static final String FETCH_HEAD_TREE = "FETCH_HEAD^{tree}";
    public static final String HEAD_TREE = "HEAD^{tree}";
    public static final int MAX_LOG_ENTRIES = 500;
    private final static Logger logger = LoggerFactory.getLogger(ReadyApiGitIntegration.class);

    @Override
    public ActivationStatus activateFor(WsdlProject project) {
        return null;
    }

    @Override
    public RepositorySelectionGui buildRepositorySelectionGui(WsdlProject project) {
        return new GitRepositorySelectionGui(project, this);
    }

    @Override
    public ImportProjectFromVcsGui buildRepositoryDownloadGui(Workspace workspace) {
        return new ImportProjectFromGitGui(this);
    }

    @Override
    public Collection<VcsUpdate> getRemoteRepositoryUpdates(File projectFile) {
        Collection<VcsUpdate> updates = new ArrayList<>();
        try {
            final Git git = createGitObject(projectFile.getPath());

            gitFetch(git);

            Repository repo = git.getRepository();
            ObjectReader reader = repo.newObjectReader();

            List<DiffEntry> diffs = git.diff().setShowNameAndStatusOnly(true)
                    .setNewTree(createTreeParser(repo, reader, FETCH_HEAD_TREE))
                    .setOldTree(createTreeParser(repo, reader, HEAD_TREE))
                    .call();

            for (DiffEntry entry : diffs) {
                updates.add(new VcsUpdate(null, convertToVcsUpdateType(entry.getChangeType()), entry.getNewPath(), entry.getOldPath()));
            }
        } catch (GitAPIException | IOException e) {
            throw new VcsIntegrationException(e.getMessage(), e.getCause());
        }
        return updates;
    }


    @Override
    public Collection<VcsUpdate> getLocalRepositoryUpdates(WsdlProject project) {
        final Git git = createGitObject(project.getPath());
        Collection<VcsUpdate> updates = new ArrayList<>();

        try {
            final Status status = git.status().call();
            fillLocalUpdates(project, updates, status);
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
            final Git git = createGitObject(projectFile.getPath());

            final Set<String> uncommittedChanges = git.status().call().getUncommittedChanges();
            if (uncommittedChanges.size() > 0) {
                UISupport.showErrorMessage("There are uncommitted changes, commit or revert back those changes before updating from remote repo");
                return;
            }
            MergeStrategy mergeStrategy = promptForMergeStrategy();
            if (mergeStrategy == null) {
                return;
            }

            pullWithMergeStrategy(git, mergeStrategy);
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
        final Git git = createGitObject(project.getPath());

        final boolean successfulUpdate = commitAndPushUpdates(vcsUpdates, commitMessage, git);

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

        final Git gitObject = createGitObject(update.getProject().getPath());

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
        final Git git = createGitObject(project.getPath());

        try {
            gitFetch(git);
            refList = git.tagList().call();
        } catch (GitAPIException e) {
            throw new VcsIntegrationException(e.getMessage(), e.getCause());
        }
        git.getRepository().close();

        return getTagSetFromRefList(refList);
    }

    @Override
    public void createTag(WsdlProject project, String tagName) {
        final Git git = createGitObject(project.getPath());
        try {
            gitCreateAndPushTag(tagName, git);
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
        Git git = createGitObject(project.getPath());

        try {
            Iterable<RevCommit> revCommits = git.log().setMaxCount(MAX_LOG_ENTRIES).call();
            for (RevCommit revCommit : revCommits) {
                String revisionName = revCommit.getId().getName();
                PersonIdent authorIdent = revCommit.getAuthorIdent();
                String fullMessage = revCommit.getFullMessage();
                historyEntries.add(new HistoryEntry(revisionName, authorIdent.getWhen(), authorIdent.getName(), fullMessage));
            }
        } catch (GitAPIException e) {
            throw new VcsIntegrationException(e.getMessage(), e.getCause());
        }
        return historyEntries;
    }

    public void shareProject(WsdlProject project, String repositoryPath, CredentialsProvider credentialsProvider) {
        try {
            initLocalRepository(project, repositoryPath);
            GitCredentialProviderCache.instance().addCredentialProvider(credentialsProvider, repositoryPath);
        } catch (GitAPIException | IOException e) {
            throw new VcsIntegrationException("Failed to share project", e);
        }
    }

    private void gitFetch(final Git git) {
        CommandRetrier commandRetrier = new CommandRetrier(git) {
            @Override
            TransportCommand recreateCommand() {
                return git.fetch();
            }
        };
        commandRetrier.execute();
    }

    private void gitCreateAndPushTag(String tagName, final Git git) throws GitAPIException {
        git.tag().setName(tagName).call();
        CommandRetrier commandRetrier = new CommandRetrier(git) {
            @Override
            TransportCommand recreateCommand() {
                return git.push().setPushTags();
            }
        };
        commandRetrier.execute();
    }


    private boolean commitAndPushUpdates(Collection<VcsUpdate> vcsUpdates, String commitMessage, final Git git) {
        addFilesToIndex(vcsUpdates, git);
        try {
            Iterable<PushResult> dryRunResult = getPushDryRun(commitMessage, git);
            return pushCommit(git, isSuccessfulPush(dryRunResult));

        } catch (GitAPIException e) {
            throw new VcsIntegrationException(e.getMessage(), e.getCause());
        }
    }

    private Iterable<PushResult> getPushDryRun(String commitMessage, final Git git) throws GitAPIException {
        git.commit().setMessage(commitMessage).call();
        CommandRetrier commandRetrier = new CommandRetrier(git) {
            @Override
            TransportCommand recreateCommand() {
                return git.push().setDryRun(true);
            }
        };
        return (Iterable<PushResult>) commandRetrier.execute();
    }

    private Iterable<PushResult> gitPush(final Git git) {
        CommandRetrier commandRetrier = new CommandRetrier(git) {
            @Override
            TransportCommand recreateCommand() {
                return git.push();
            }
        };
        return (Iterable<PushResult>) commandRetrier.execute();
    }


    private void pullWithMergeStrategy(final Git git, final MergeStrategy mergeStrategy) {
        CommandRetrier retrier = new CommandRetrier(git) {
            @Override
            TransportCommand recreateCommand() {
                return git.pull().setStrategy(mergeStrategy);
            }
        };

        PullResult pullResult = (PullResult) retrier.execute();

        MergeResult.MergeStatus mergeStatus = pullResult.getMergeResult().getMergeStatus();
        if (mergeStatus.equals(MergeResult.MergeStatus.FAILED)) {
            UISupport.showErrorMessage("Failed to pull the changes from remote repository.");
        } else if (mergeStatus.equals(MergeResult.MergeStatus.CONFLICTING)) {
            UISupport.showErrorMessage("Update has resulted in merge conflicts, please resolve conflicts manually.");
        }
    }

    private boolean pushCommit(final Git git, boolean isDryRunSuccessful) {
        Iterable<PushResult> results;

        if (isDryRunSuccessful) {
            results = gitPush(git);
        } else {
            MergeStrategy mergeStrategy = promptForMergeStrategy();
            if (mergeStrategy == null) {
                return false;
            }

            pullWithMergeStrategy(git, mergeStrategy);
            results = gitPush(git);
        }
        return isSuccessfulPush(results);

    }

    private MergeStrategy promptForMergeStrategy() {
        List<String> list = new ArrayList<>();
        list.add(MergeStrategy.OURS.getName());
        list.add(MergeStrategy.THEIRS.getName());

        String strategy = UISupport.prompt("Pulling changes from the remote repository may result in conflicts.\n" +
                "Please select which merge strategy to use to resolve any such conflicts.\n",
                "Select Merge Strategy",
                list.toArray(new String[list.size()]),
                MergeStrategy.OURS.getName());
        return strategy == null ? null : MergeStrategy.get(strategy);
    }

    private boolean isEmptyDir(Path dir) throws IOException {
        DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir);

        Iterator files = directoryStream.iterator();
        while (files.hasNext()) {
            Path path = (Path) files.next();
            if (!path.toFile().isDirectory()){ // If there is any file other than only empty dirs then this is not an empty dir
                directoryStream.close();
                return false;
            }
        }

        directoryStream.close();
        return true;
    }

    private Git createGitObject(final String localPath) {
        final Repository localRepo;

        try {
            localRepo = new FileRepository(localPath + "/.git");
            if (!localRepo.getObjectDatabase().exists()) {
                logger.error("No git repo exist in: " + localPath);
                throw new IllegalStateException("No git repo exist in: " + localPath);
            }
        } catch (IOException e) {
            throw new VcsIntegrationException(e.getMessage(), e.getCause());
        }
        return new Git(localRepo);
    }

    private Set<String> getTagSetFromRefList(List<Ref> refList) {
        Set<String> tagSet = new HashSet<>();
        String tag;
        for (Ref ref : refList) {
            tag = ref.getName().replaceFirst("refs/tags/", "");
            tagSet.add(tag);
        }
        return tagSet;
    }

    private boolean isSuccessfulPush(Iterable<PushResult> resultIterable) {
        boolean isPushSuccessful = true;
        PushResult pushResult = resultIterable.iterator().next();

        for (final RemoteRefUpdate refUpdate : pushResult.getRemoteUpdates()) {
            final RemoteRefUpdate.Status status = refUpdate.getStatus();
            if (status != RemoteRefUpdate.Status.OK && status != RemoteRefUpdate.Status.UP_TO_DATE) {
                isPushSuccessful = false;
                logger.warn("Push to one of the remote " + refUpdate.getSrcRef() + " was not successful: " + status);
                break;
            }
        }

        return isPushSuccessful;
    }

    private CanonicalTreeParser createTreeParser(Repository repo, ObjectReader reader, String treeName) throws IOException {
        ObjectId head = repo.resolve(treeName);
        CanonicalTreeParser localRepoTreeParser = new CanonicalTreeParser();
        localRepoTreeParser.reset(reader, head);
        return localRepoTreeParser;
    }

    private void addFilesToIndex(Collection<VcsUpdate> vcsUpdates, Git git) {
        for (VcsUpdate vcsUpdate : vcsUpdates) {
            try {
                git.add().addFilepattern(vcsUpdate.getRelativePath()).call();

            } catch (GitAPIException e) {
                throw new VcsIntegrationException(e.getMessage(), e.getCause());
            }
        }
    }

    private VcsUpdate.VcsUpdateType convertToVcsUpdateType(DiffEntry.ChangeType changeType) {
        switch (changeType) {
            case ADD:
                return VcsUpdate.VcsUpdateType.ADDED;
            case DELETE:
                return VcsUpdate.VcsUpdateType.DELETED;
            case COPY:
            case RENAME:
                return VcsUpdate.VcsUpdateType.MOVED;
            case MODIFY:
                return VcsUpdate.VcsUpdateType.MODIFIED;
            default:
                return null;
        }
    }

    private void fillLocalUpdates(WsdlProject project, Collection<VcsUpdate> updates, Status status) throws IOException {
        for (String fileAdded : status.getAdded()) {
            updates.add(new VcsUpdate(project, ADDED, fileAdded, fileAdded));
        }

        for (String fileChanged : status.getChanged()) {
            updates.add(new VcsUpdate(project, MODIFIED, fileChanged, fileChanged));
        }

        for (String fileChanged : status.getRemoved()) {
            updates.add(new VcsUpdate(project, DELETED, fileChanged, fileChanged));
        }

        for (String fileChanged : status.getMissing()) {
            updates.add(new VcsUpdate(project, DELETED, fileChanged, fileChanged));
        }

        for (String fileChanged : status.getModified()) {
            updates.add(new VcsUpdate(project, MODIFIED, fileChanged, fileChanged));
        }

        for (String fileChanged : status.getUntracked()) {
            updates.add(new VcsUpdate(project, ADDED, fileChanged, fileChanged));
        }

        for (String fileChanged : status.getUntrackedFolders()) {
            File untrackedFolder = new File(project.getPath() + "/" + fileChanged);
            if (!isEmptyDir(untrackedFolder.toPath())) {
                updates.add(new VcsUpdate(project, ADDED, fileChanged, fileChanged));
            }
        }

        for (String fileChanged : status.getConflicting()) {
            final VcsUpdate update = new VcsUpdate(project, MODIFIED, fileChanged, fileChanged);
            update.setConflictingUpdate(true);
            updates.add(update);
        }
    }

    private Git initLocalRepository(WsdlProject project, String repositoryPath) throws GitAPIException, IOException {
        Git git = Git.init().setDirectory(new File(project.getPath())).call();

        StoredConfig config = git.getRepository().getConfig();
        config.setString("remote", "origin", "url", repositoryPath);
        config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
        config.save();

        return git;
    }

    public void cloneRepository(String repositoryPath, CredentialsProvider credentialsProvider, File emptyDirectory) throws GitAPIException {
        Git.cloneRepository().setURI(repositoryPath).setCredentialsProvider(credentialsProvider).setDirectory(emptyDirectory).call();
    }

    public String getRemoteRepositoryUrl(WsdlProject project) {
        try {
            return createGitObject(project.getPath()).getRepository().getConfig().getString("remote", "origin", "url");
        } catch (Exception ignore) {
            return null;
        }
    }
}
