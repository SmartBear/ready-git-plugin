package com.smartbear.readyapi.plugin.git;

import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.plugins.vcs.VcsIntegrationException;
import com.eviware.soapui.plugins.vcs.VcsUpdate;
import com.eviware.soapui.support.UISupport;
import com.smartbear.readyapi.plugin.git.ui.ConfirmMergeDialog;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.Sequence;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.merge.MergeResult;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.Merger;
import org.eclipse.jgit.merge.RecursiveMerger;
import org.eclipse.jgit.merge.StrategyOneSided;
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
import java.util.Map;
import java.util.Set;

import static com.eviware.soapui.plugins.vcs.VcsUpdate.VcsUpdateType.ADDED;
import static com.eviware.soapui.plugins.vcs.VcsUpdate.VcsUpdateType.DELETED;
import static com.eviware.soapui.plugins.vcs.VcsUpdate.VcsUpdateType.MODIFIED;

public class GitCommandHelper {

    private final static Logger logger = LoggerFactory.getLogger(GitCommandHelper.class);

    protected static final String FETCH_HEAD_TREE = "FETCH_HEAD^{tree}";
    protected static final String HEAD_TREE = "HEAD^{tree}";

    public void cloneRepository(String repositoryPath, CredentialsProvider credentialsProvider, File emptyDirectory) throws GitAPIException {
        CloneCommand cloneCommand = Git.cloneRepository().setURI(repositoryPath).setCredentialsProvider(credentialsProvider).setDirectory(emptyDirectory);
        if (credentialsProvider instanceof SshPassphraseCredentialsProvider) {
            cloneCommand.setTransportConfigCallback(new CommandRetrier.SshTransportConfigCallback((SshPassphraseCredentialsProvider) credentialsProvider));
        }
        AuthenticatorState authenticatorState = AuthenticatorHelper.resetGlobalAuthenticator();
        try {
            cloneCommand.call().close();
        } finally {
            AuthenticatorHelper.restoreGlobalAuthenticator(authenticatorState);
        }
        GitCredentialProviderCache.instance().addCredentialProvider(credentialsProvider, repositoryPath);
    }

    public void shareProject(WsdlProject project, String repositoryPath, CredentialsProvider credentialsProvider) {
        try {
            initLocalRepository(project, repositoryPath);
            GitCredentialProviderCache.instance().addCredentialProvider(credentialsProvider, repositoryPath);
        } catch (GitAPIException | IOException e) {
            throw new VcsIntegrationException("Failed to share project", e);
        }
    }

    public String getRemoteRepositoryUrl(WsdlProject project) {
        try {
            return createGitObject(project.getPath()).getRepository().getConfig().getString("remote", "origin", "url");
        } catch (Exception ignore) {
            return null;
        }
    }

    protected void gitFetch(final Git git) {
        CommandRetrier commandRetrier = new CommandRetrier(git) {
            @Override
            TransportCommand recreateCommand() {
                return git.fetch();
            }
        };
        commandRetrier.execute();
    }

    protected void gitCreateAndPushTag(String tagName, final Git git) throws GitAPIException {
        git.tag().setName(tagName).call();
        CommandRetrier commandRetrier = new CommandRetrier(git) {
            @Override
            TransportCommand recreateCommand() {
                return git.push().setPushTags();
            }
        };
        commandRetrier.execute();
    }

    protected boolean commitAndPushUpdates(Collection<VcsUpdate> vcsUpdates, String commitMessage, final Git git) {
        addFilesToIndex(vcsUpdates, git);
        try {
            Iterable<PushResult> dryRunResult = gitPushDryRun(commitMessage, git);
            return pushCommit(git, isSuccessfulPush(dryRunResult));

        } catch (GitAPIException e) {
            throw new VcsIntegrationException(e.getMessage(), e.getCause());
        }
    }

    protected Iterable<PushResult> gitPushDryRun(String commitMessage, final Git git) throws GitAPIException {
        git.commit().setMessage(commitMessage).call();
        CommandRetrier commandRetrier = new CommandRetrier(git) {
            @Override
            TransportCommand recreateCommand() {
                return git.push().setDryRun(true);
            }
        };
        return (Iterable<PushResult>) commandRetrier.execute();
    }

    protected Iterable<PushResult> gitPush(final Git git) {
        CommandRetrier commandRetrier = new CommandRetrier(git) {
            @Override
            TransportCommand recreateCommand() {
                return git.push();
            }
        };
        return (Iterable<PushResult>) commandRetrier.execute();
    }


    protected boolean pullWithMergeStrategy(final Git git, final MergeStrategy mergeStrategy) {
        if (!canMerge(git, mergeStrategy)) {
            return false;
        }

        CommandRetrier retrier = new CommandRetrier(git) {
            @Override
            TransportCommand recreateCommand() {
                return git.pull().setStrategy(mergeStrategy);
            }
        };

        PullResult pullResult = (PullResult) retrier.execute();

        return pullResult.getMergeResult().getMergeStatus().isSuccessful();
    }


    protected boolean pushCommit(final Git git, boolean isDryRunSuccessful) {
        Iterable<PushResult> results;

        if (!isDryRunSuccessful) {
            MergeStrategy mergeStrategy = promptForMergeStrategy();
            if (mergeStrategy == null || !pullWithMergeStrategy(git, mergeStrategy)) {
                return false;
            }
        }
        results = gitPush(git);
        return isSuccessfulPush(results);
    }

    protected Git createGitObject(final String localPath) {
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

    protected MergeStrategy promptForMergeStrategy() {
        List<String> list = new ArrayList<>();
        list.add(MergeStrategy.OURS.getName());
        list.add(MergeStrategy.THEIRS.getName());
        list.add(MergeStrategy.RECURSIVE.getName());

        String strategy = UISupport.prompt("Pulling changes from the remote repository may result in conflicts.\n" +
                        "Please select which merge strategy to use to resolve any such conflicts.\n",
                "Select Merge Strategy",
                list.toArray(new String[list.size()]),
                MergeStrategy.OURS.getName());
        return strategy == null ? null : MergeStrategy.get(strategy);
    }

    protected Set<String> getTagSetFromRefList(List<Ref> refList) {
        Set<String> tagSet = new HashSet<>();
        String tag;
        for (Ref ref : refList) {
            tag = ref.getName().replaceFirst("refs/tags/", "");
            tagSet.add(tag);
        }
        return tagSet;
    }

    protected void fillRemoteUpdates(Collection<VcsUpdate> updates, Git git, Repository repo, ObjectReader reader) throws GitAPIException, IOException {
        List<DiffEntry> diffs = git.diff().setShowNameAndStatusOnly(true)
                .setNewTree(createTreeParser(repo, reader, FETCH_HEAD_TREE))
                .setOldTree(createTreeParser(repo, reader, HEAD_TREE))
                .call();

        for (DiffEntry entry : diffs) {
            updates.add(new VcsUpdate(null, convertToVcsUpdateType(entry.getChangeType()), entry.getNewPath(), entry.getOldPath()));
        }
    }

    protected void fillLocalUpdates(WsdlProject project, Collection<VcsUpdate> updates, Status status) throws IOException {
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

    private boolean isEmptyDir(Path dir) throws IOException {
        DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir);
        try {
            Iterator files = directoryStream.iterator();

            while (files.hasNext()) {
                Path path = (Path) files.next();
                if (!path.toFile().isDirectory()) { // If there is any file other than only empty dirs then this is not an empty dir
                    return false;
                }
            }
            return true;
        } finally {
            directoryStream.close();
        }
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
                if (vcsUpdate.getType() == VcsUpdate.VcsUpdateType.DELETED) {
                    git.rm().addFilepattern(vcsUpdate.getRelativePath()).call();
                } else {
                    git.add().addFilepattern(vcsUpdate.getRelativePath()).call();
                }
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

    private Git initLocalRepository(WsdlProject project, String repositoryPath) throws GitAPIException, IOException {
        Git git = Git.init().setDirectory(new File(project.getPath())).call();

        StoredConfig config = git.getRepository().getConfig();
        config.setString("remote", "origin", "url", repositoryPath);
        config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
        config.save();

        return git;
    }

    private boolean canMerge(final Git git, final MergeStrategy mergeStrategy) {
        if (mergeStrategy instanceof StrategyOneSided) {
            return true;
        }

        gitFetch(git);

        try {
            Repository repository = git.getRepository();
            String branchName = repository.getFullBranch().substring("refs/heads/".length());
            ObjectId from = repository.resolve("refs/remotes/origin/" + branchName);
            ObjectId to = repository.resolve("refs/heads/" + branchName);
            Merger merger = mergeStrategy.newMerger(repository, true);
            if (!merger.merge(to, from)) {
                Map<String, MergeResult<? extends Sequence>> mergeResults = ((RecursiveMerger) merger).getMergeResults();
                List<String> conflictFiles = new ArrayList<>();
                for (String filePath : mergeResults.keySet()) {
                    if (mergeResults.get(filePath).containsConflicts()) {
                        conflictFiles.add(filePath);
                    }
                }
                if (!conflictFiles.isEmpty()) {
                    ConfirmMergeDialog confirmMergeDialog = new ConfirmMergeDialog(conflictFiles);
                    UISupport.showDialog(confirmMergeDialog);
                    return confirmMergeDialog.allowMerge();
                }
            }
        } catch (IOException exception) {
            logger.error(exception.getMessage(), exception);
            return false;
        }

        return true;
    }
}
