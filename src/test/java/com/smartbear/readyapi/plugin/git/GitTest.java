package com.smartbear.readyapi.plugin.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GitTest {

    private File projectDirectory = new File("/tmp/labdir");
    private String remoteUrl = "git@github.com:andersjaensson/test.git";
    private Git git;

    @Before
    public void setup() throws GitAPIException, IOException {
        // setup the local directory
        git = Git.init().setDirectory(projectDirectory).call();

        StoredConfig config = git.getRepository().getConfig();
        config.setString("remote", "origin", "url", remoteUrl);
        config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
        config.save();
    }


    @Test
    public void shareProjectRemotely() throws GitAPIException, IOException {
        boolean force = true;
        boolean dryRun = false;

        git.add().addFilepattern(".").call();
        git.commit().setMessage("First commit of project").call();
        git.pull().setStrategy(MergeStrategy.OURS).call();
        Iterable<PushResult> result = git.push().setPushAll().setDryRun(dryRun).call();

        for (PushResult pushResult : result) {
            for (RemoteRefUpdate remoteRefUpdate : pushResult.getRemoteUpdates()) {
                assertThat(remoteRefUpdate.getStatus(), is(RemoteRefUpdate.Status.OK));
            }
        }
    }

    @Test(expected = InvalidRemoteException.class)
    public void tryToShareToNonExistingRepo() throws IOException, GitAPIException {
        StoredConfig config = git.getRepository().getConfig();
        config.setString("remote", "origin", "url", "git@github.com:andersjaensson/test2.git");
        config.save();

        git.add().addFilepattern(".").call();
        git.commit().setMessage("First commit of project").call();
        git.pull().setStrategy(MergeStrategy.OURS).call();
        git.push().call();
    }


    @Test
    public void pushToRemoteRepository() {

    }

    @Test
    @Ignore
    public void testClone() throws GitAPIException {
        Git.cloneRepository().setURI(remoteUrl).setDirectory(new File("/tmp/apan")).call();
    }

    @Test
    public void pullFromRemote() throws IOException, GitAPIException {
        git.pull().call();
    }

    @Test
    public void fetchFromRemote() throws GitAPIException {
        git.fetch().call();
    }

    @Test
    @Ignore
    public void cloneAndPull() throws GitAPIException {
        Git repo = Git.cloneRepository().setURI(remoteUrl).setDirectory(new File("/tmp/apan2")).call();
        repo.pull().call();
    }
}