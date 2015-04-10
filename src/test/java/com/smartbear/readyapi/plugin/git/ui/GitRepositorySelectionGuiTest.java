package com.smartbear.readyapi.plugin.git.ui;

import com.eviware.soapui.impl.wsdl.WsdlProject;
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
        WsdlProject project = Mockito.mock(WsdlProject.class);
        when(project.getPath()).thenReturn("/tmp/apan");
        embedInFrameAndShow(new GitRepositorySelectionGui(project, null).getComponent());
    }
}