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
import com.smartbear.ready.util.ReadyTools;
import org.eclipse.jgit.api.Git;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReadyApiGitIntegrationTest {

    private final String remoteUrl = "git@github.com:SmartBear/git-plugin-test-repo.git";
    private File localPath;
    private Git git;
    @Mock
    WsdlProjectPro dummyProject;
    ReadyApiGitIntegration gitIntegration;

    @Before
    public void setUp() throws Exception {
        gitIntegration = new ReadyApiGitIntegration();
        localPath = File.createTempFile("TestGitPlugin", "");
        localPath.delete();
        git = Git.cloneRepository().setURI(remoteUrl).setDirectory(localPath).call();
        dummyProject = mock(WsdlProjectPro.class);
        when(dummyProject.getPath()).thenReturn(localPath.getAbsolutePath());
        ReadyTools.deleteDirectoryOnExit(localPath);
    }

    @After
    public void tearDown() throws Exception {
        git.getRepository().close();
    }

    @Ignore("Depends on external Github repos")
    @Test
    public void testListAvailableTags() throws Exception {
        final Set<String> availableTags = gitIntegration.getAvailableTags(dummyProject);
        assertThat(availableTags.size(), greaterThanOrEqualTo(1));
    }

    @Ignore("Depends on external Github repos")
    @Test
    public void testCreateTag() throws Exception {
        final int numberOfTags = git.tagList().call().size();
        gitIntegration.createTag(dummyProject, "V0.7");
        assertThat(git.tagList().call().size(), greaterThan(numberOfTags));
    }

}
