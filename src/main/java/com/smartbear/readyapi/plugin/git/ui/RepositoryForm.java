package com.smartbear.readyapi.plugin.git.ui;

import org.eclipse.jgit.transport.CredentialsProvider;

import javax.swing.JPanel;

public interface RepositoryForm {
    String getRepositoryPath();

    CredentialsProvider getCredentialsProvider();

    JPanel getComponent();

    boolean isValid();
}
