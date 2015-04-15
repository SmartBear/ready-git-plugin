package com.smartbear.readyapi.plugin.git;

import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.model.workspace.Workspace;
import com.eviware.soapui.plugins.vcs.ActivationStatus;
import com.eviware.soapui.plugins.vcs.AuthenticationStatus;
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
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.eviware.soapui.plugins.vcs.CommitResult.CommitStatus.FAILED;
import static com.eviware.soapui.plugins.vcs.CommitResult.CommitStatus.SUCCESSFUL;
import static com.eviware.soapui.plugins.vcs.VcsUpdate.VcsUpdateType.ADDED;
import static com.eviware.soapui.plugins.vcs.VcsUpdate.VcsUpdateType.DELETED;
import static com.eviware.soapui.plugins.vcs.VcsUpdate.VcsUpdateType.MODIFIED;

@VcsIntegrationConfiguration(name = "Git", description = "Git Version Control System")
public class ReadyApiGitIntegration implements VcsIntegration {

    private final static Logger logger = LoggerFactory.getLogger(ReadyApiGitIntegration.class);
    public static final String FETCH_HEAD_TREE = "FETCH_HEAD^{tree}";
    public static final String HEAD_TREE = "HEAD^{tree}";
    

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
    public AuthenticationStatus authenticate(WsdlProject project) {
        return null;
    }

    @Override
    public AuthenticationStatus checkAuthenticationStatus(WsdlProject project) {
        return null;
    }

    @Override
    public Collection<VcsUpdate> getRemoteRepositoryUpdates(File projectFile) {
        Collection<VcsUpdate> updates = new ArrayList<>();
        try {
            final Git git = createGitObject(projectFile.getPath());

            CommandRetrier commandRetrier = new CommandRetrier(git) {
                @Override
                TransportCommand recreateCommand() {
                    return git.fetch();
                }
            };
            commandRetrier.execute();

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
            e.printStackTrace();
            throw new VcsIntegrationException(e.getMessage(), e.getCause());
        } catch (Throwable e) {
            e.printStackTrace();
            throw new VcsIntegrationException(e.getMessage(), e.getCause());
        }
        return updates;
    }

    private CanonicalTreeParser createTreeParser(Repository repo, ObjectReader reader, String treeName) throws IOException {
        ObjectId head = repo.resolve(treeName);
        CanonicalTreeParser localRepoTreeParser = new CanonicalTreeParser();
        localRepoTreeParser.reset(reader, head);
        return localRepoTreeParser;
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

    @Override
    public Collection<VcsUpdate> getLocalRepositoryUpdates(WsdlProject project) {
        final Git git = createGitObject(project.getPath());
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
            if (untrackedFolder.list().length > 0) {
                updates.add(new VcsUpdate(project, ADDED, fileChanged, fileChanged));
            }
        }

        for (String fileChanged : status.getConflicting()) {
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
    public void updateFromRemoteRepository(File projectFile, boolean b) {
        try {
            final Git git = createGitObject(projectFile.getPath());

            CommandRetrier commandRetrier = new CommandRetrier(git) {
                @Override
                TransportCommand recreateCommand() {
                    PullCommand pull = git.pull();
                    pull.setStrategy(MergeStrategy.OURS);
                    return pull;
                }
            };
            PullResult pullResult = (PullResult)commandRetrier.execute();
            System.out.println("Pull result for "+ projectFile+ ": " +pullResult.isSuccessful());
        } catch (GitAPIException e) {
            e.printStackTrace();
            throw new VcsIntegrationException(e.getMessage(), e.getCause());
        } catch (Throwable e) {
            e.printStackTrace();
            throw new VcsIntegrationException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void deleteFile(WsdlProject project, File file) throws IOException {

    }

    @Override
    public void moveFile(WsdlProject project, File file, File targetFile) throws IOException {

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

        final boolean successfulUpdate = commitUpdates(vcsUpdates, commitMessage, git);

        CommitResult result = successfulUpdate? new CommitResult(SUCCESSFUL, "Commit was successful") :
                new CommitResult(FAILED, "Commit Failed");

        git.getRepository().close();

        return result;
    }

    private boolean commitUpdates(Collection<VcsUpdate> vcsUpdates, String commitMessage, final Git git) {
        addFilesToIndex(vcsUpdates, git);
        try {
            git.commit().setMessage(commitMessage).call();
            CommandRetrier retrier = new CommandRetrier(git) {
                @Override
                TransportCommand recreateCommand() {
                    return git.push().setDryRun(true);
                }
            };
            Iterable<PushResult> dryRunResult = (Iterable<PushResult>) retrier.execute();
            return pushCommit(git, isSuccessFulPush(dryRunResult));

        } catch (GitAPIException e) {
            throw new VcsIntegrationException(e.getMessage(), e.getCause());
        } catch (Throwable e) {
            throw new VcsIntegrationException(e.getMessage(), e.getCause());
        }
    }

    private boolean pushCommit(final Git git, boolean isDryRunSuccessful) throws GitAPIException {
        Iterable<PushResult> results;

        try {
            if (isDryRunSuccessful) {
                CommandRetrier commandRetrier = new CommandRetrier(git) {
                    @Override
                    TransportCommand recreateCommand() {
                        return git.push();
                    }
                };
                results = (Iterable<PushResult>) commandRetrier.execute();
            } else {
                if (UISupport.confirm("Your changes are conflicting, do you still want to commit and overwrite remote changes?",
                        "Overwrite remote changes")) {
                    results = git.push().setForce(true).call();
                } else {
                    return false;
                }
            }
            return isSuccessFulPush(results);
        } catch (Throwable throwable) {
            throw new VcsIntegrationException(throwable.getMessage(), throwable);
        }

    }

    private boolean isSuccessFulPush(Iterable<PushResult> resultIterable) {
        boolean isPushSuccessful = true;
        PushResult pushResult = resultIterable.iterator().next();

        for (final RemoteRefUpdate refUpdate : pushResult.getRemoteUpdates()) {
            final RemoteRefUpdate.Status status = refUpdate.getStatus();
            if (status != RemoteRefUpdate.Status.OK && status != RemoteRefUpdate.Status.UP_TO_DATE) {
                isPushSuccessful = false;
                logger.warn("Push to one of the remote "+ refUpdate.getSrcRef() +" was not successful: " + status);
                break;
            }
        }

        return isPushSuccessful;
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

    @Override
    public void revert(Collection<VcsUpdate> vcsUpdates) throws VcsIntegrationException {

        VcsUpdate update;

        if (vcsUpdates.isEmpty()) {
            return ;
        } else {
            update = vcsUpdates.iterator().next();
        }

        final Git gitObject = createGitObject(update.getProject().getPath());

        for(VcsUpdate vcsUpdate: vcsUpdates){
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
        Git git = createGitObject(project.getPath());

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
        Git git = createGitObject(project.getPath());
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
        for (Ref ref : refList) {
            tagSet.add(ref.getName());
        }
        return tagSet;
    }


    public void shareProject(WsdlProject project, String repositoryPath, CredentialsProvider credentialsProvider) {
        try {
            initLocalRepository(project, repositoryPath);
            GitCredentialProviderCache.addCredentialProvider(credentialsProvider, repositoryPath);
        } catch (GitAPIException | IOException e) {
            throw new VcsIntegrationException("Failed to share project", e);
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
        Git git = Git.cloneRepository().setURI(repositoryPath).setCredentialsProvider(credentialsProvider).setDirectory(emptyDirectory).call();
    }
}
