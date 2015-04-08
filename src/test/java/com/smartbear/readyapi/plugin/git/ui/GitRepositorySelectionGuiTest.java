package com.smartbear.readyapi.plugin.git.ui;

import com.eviware.soapui.model.project.Project;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

public class GitRepositorySelectionGuiTest extends SwingTest {

    @Before
    public void setup() {
    }

    @Test
    @Ignore("This is a manual test")
    public void showDialog() throws Exception {
        Project project = Mockito.mock(Project.class);
        when(project.getPath()).thenReturn("/tmp/apan");
        embedInFrameAndShow(new GitRepositorySelectionGui(project).getComponent());
    }
}