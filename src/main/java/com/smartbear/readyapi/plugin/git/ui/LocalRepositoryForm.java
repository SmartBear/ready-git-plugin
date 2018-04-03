package com.smartbear.readyapi.plugin.git.ui;

import org.eclipse.jgit.transport.CredentialsProvider;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class LocalRepositoryForm implements RepositoryForm {

    public LocalRepositoryForm() {
    }

    @Override
    public String getRepositoryPath() {
        return null;
    }

    @Override
    public CredentialsProvider getCredentialsProvider() {
        return null;
    }

    @Override
    public JPanel getComponent() {
        JPanel panel = new JPanel();
        panel.add(new JLabel("Specify the path to a local repository in the Project Directory field."));
        return panel;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isLocal() {
        return true;
    }
}
