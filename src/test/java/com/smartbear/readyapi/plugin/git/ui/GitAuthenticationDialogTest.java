package com.smartbear.readyapi.plugin.git.ui;

import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

public class GitAuthenticationDialogTest extends SwingTest {

    @Test
    @Ignore("Manual test")
    public void showDialog() throws InvocationTargetException, InterruptedException {
        GitAuthenticationDialog dialog = new GitAuthenticationDialog("https://google.com");
        embedInFrameAndShow(dialog.getContentPane());
        System.out.println(dialog.getPassword());
        System.out.println(dialog.getUsername());
    }

}