/*
 * Copyright 2004-2015 SmartBear Software
 *
 * Licensed under the EUPL, Version 1.1 or - as soon as they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the Licence for the specific language governing permissions and limitations
 * under the Licence.
*/
package com.smartbear.readyapi.plugin.git;

import com.eviware.soapui.impl.wsdl.WsdlProjectPro;
import com.eviware.soapui.plugins.vcs.CommitResult;
import com.eviware.soapui.plugins.vcs.VcsUpdate;
import com.eviware.soapui.support.UISupport;
import com.eviware.x.dialogs.XDialogs;
import com.eviware.x.impl.swing.SwingDialogs;
import com.smartbear.ready.util.ReadyTools;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

import java.awt.Component;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore("Depends on external Github repos")
public class ReadyApiGitIntegrationTest {

    private File localPath;
    private Git git;
    @Mock
    WsdlProjectPro dummyProject;
    XDialogs originalDialogs;
    ReadyApiGitIntegration gitIntegration;

    @Before
    public void setUp() throws Exception {
        originalDialogs = UISupport.getDialogs();
        StubbedDialogs dialog = new StubbedDialogs(null);
        UISupport.setDialogs(dialog);

        gitIntegration = new ReadyApiGitIntegration();
        localPath = File.createTempFile("TestGitPlugin", "");
        localPath.delete();
        String remoteUrl = "git@github.com:SmartBear/git-plugin-test-repo.git";
        git = Git.cloneRepository().setURI(remoteUrl).setDirectory(localPath).call();
        dummyProject = mock(WsdlProjectPro.class);
        when(dummyProject.getPath()).thenReturn(localPath.getAbsolutePath());
        ReadyTools.deleteDirectoryOnExit(localPath);
    }

    @After
    public void tearDown() throws Exception {
        UISupport.setDialogs(originalDialogs);
        git.getRepository().close();
    }

    @Test
    public void testListAvailableTags() throws Exception {
        final Set<String> availableTags = gitIntegration.getAvailableTags(dummyProject);
        assertThat(availableTags.size(), greaterThanOrEqualTo(1));
    }

    @Test
    public void testCreateTag() throws Exception {
        final int numberOfTags = git.tagList().call().size();
        gitIntegration.createTag(dummyProject, "V" + System.currentTimeMillis());
        assertThat(git.tagList().call().size(), greaterThan(numberOfTags));
    }

    @Test(expected=IllegalStateException.class)
    public void throwsIllegalStateExceptionForInvalidRepo() throws IOException {
        File gitConfig = new File(localPath + "/.git");
        FileUtils.forceDelete(gitConfig);
        gitIntegration.getAvailableTags(dummyProject);
    }

    @Test
    public void getUpdates() throws Exception {
        int added=0, modified=0, deleted=0;
        int expectedAdds=1, expectedModifications=1, expectedDeletions=1; //updates should ignore empty directories
        makeChangesToLocalRepo(localPath);
        final Collection<VcsUpdate> updates = gitIntegration.getLocalRepositoryUpdates(dummyProject);

        for(VcsUpdate update: updates){
            if (update.getType() == VcsUpdate.VcsUpdateType.ADDED){
                added++;
            }
            else if (update.getType() == VcsUpdate.VcsUpdateType.MODIFIED){
                modified++;
            }
            else if (update.getType() == VcsUpdate.VcsUpdateType.DELETED){
                deleted++;
            }
        }
        assertThat(added, is(expectedAdds));
        assertThat(modified, is(expectedModifications));
        assertThat(deleted, is(expectedDeletions));
    }

    @Test
    public void testCommit() throws Exception {
        updateFile(localPath);
        final Collection<VcsUpdate> updates = gitIntegration.getLocalRepositoryUpdates(dummyProject);
        final CommitResult commitResult = gitIntegration.commit(updates, "commit message");
        assertThat(commitResult.getCommitStatus(), is(CommitResult.CommitStatus.SUCCESSFUL));
    }

    @Test
    public void testConflictingCommit() throws Exception {
        updateFile(localPath);

        final File conflictingRepoPath = File.createTempFile("TestGitPluginForConflict", "");
        final WsdlProjectPro projectFromNewRepo = cloneNewRepo(conflictingRepoPath);
        updateFile(conflictingRepoPath);

        final Collection<VcsUpdate> updates = gitIntegration.getLocalRepositoryUpdates(dummyProject);
        final CommitResult commitResult = gitIntegration.commit(updates, "commit message");
        assertThat(commitResult.getCommitStatus(), is(CommitResult.CommitStatus.SUCCESSFUL));

        final Collection<VcsUpdate> conflictingUpdates = gitIntegration.getLocalRepositoryUpdates(projectFromNewRepo);
        final CommitResult conflictResult = gitIntegration.commit(conflictingUpdates, "Overriding previous commit");
        assertThat(conflictResult.getCommitStatus(), is(CommitResult.CommitStatus.SUCCESSFUL));
        ReadyTools.deleteDirectoryOnExit(conflictingRepoPath);

    }

    @Test
    public void testRevert() throws Exception {
        updateFile(localPath);
        final Collection<VcsUpdate> updates = gitIntegration.getLocalRepositoryUpdates(dummyProject);
        assertTrue(updates.size() == 1);

        gitIntegration.revert(updates);
        final Collection<VcsUpdate> revertedUpdates = gitIntegration.getLocalRepositoryUpdates(dummyProject);
        assertTrue(revertedUpdates.size() == 0);
    }

    private WsdlProjectPro cloneNewRepo(File path) throws Exception {
        String remoteUrl = "git@github.com:SmartBear/git-plugin-test-repo.git";
        path.delete();
        Git.cloneRepository().setURI(remoteUrl).setDirectory(path).call();
        WsdlProjectPro dummyProject = mock(WsdlProjectPro.class);
        when(dummyProject.getPath()).thenReturn(path.getAbsolutePath());
        return dummyProject;
    }

    private void makeChangesToLocalRepo(File path) throws IOException {
        addFile(path);
        addEmptyDir(path);
        updateFile(path);
        deleteFile(path);
    }

    private void addFile(File path) throws IOException {
        File changes = new File(path + "/sample.txt");
        changes.createNewFile();
    }

    private void addEmptyDir(File path) throws IOException {
        File emptyDir = new File(path + "/newfile");
        emptyDir.mkdir();
    }

    private void updateFile(File path) throws IOException {
        String data = " This is a test readme file. ";
        File file = new File(path + "/README.md");
        if (!file.exists()) {
            file.createNewFile();
        }
        FileWriter fileWritter = new FileWriter(file, true);
        BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
        bufferWritter.write(data);
        bufferWritter.close();
    }

    private void deleteFile(File path) throws IOException {
        File file = new File(path + "/settings.xml");
        FileUtils.forceDelete(file);
    }

    private class StubbedDialogs extends SwingDialogs {
        public StubbedDialogs(Component parent) {
            super(parent);
        }
        @Override
        public boolean confirm(String s, String s2) {
            return true;
        }
    }
}
