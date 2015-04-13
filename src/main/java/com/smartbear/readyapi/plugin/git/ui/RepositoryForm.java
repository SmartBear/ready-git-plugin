package com.smartbear.readyapi.plugin.git.ui;

import org.eclipse.jgit.transport.CredentialsProvider;

import java.awt.Component;

public interface RepositoryForm {
    String getRepositoryPath();

    CredentialsProvider getCredentialsProvider();

    Component getComponent();

    boolean isValid();
}
