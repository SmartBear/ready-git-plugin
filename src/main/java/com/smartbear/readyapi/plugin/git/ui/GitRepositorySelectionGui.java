package com.smartbear.readyapi.plugin.git.ui;

import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.plugins.vcs.RepositorySelectionGui;
import com.eviware.soapui.support.UISupport;
import net.miginfocom.swing.MigLayout;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class GitRepositorySelectionGui implements RepositorySelectionGui {

    private JTextField repositoryUrlField;
    private JTextField usernameField;
    private JTextField passwordField;
    private Project project;

    public GitRepositorySelectionGui(Project project) {
        this.project = project;
    }

    @Override
    public Component getComponent() {
        JPanel panel = new JPanel(new MigLayout("wrap 1", "8[fill,grow]8[fill,grow]8", "8[]8"));

        panel.add(new JLabel("Repository URL:"));
        repositoryUrlField = new JTextField();
        repositoryUrlField.getDocument().addDocumentListener(new RepositoryUrlListener());
        repositoryUrlField.setPreferredSize(new Dimension(300, 30));
        panel.add(repositoryUrlField);

        panel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        usernameField.setPreferredSize(new Dimension(150, 30));
        panel.add(usernameField);

        panel.add(new Label("Password:"));
        passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(150, 30));
        panel.add(passwordField);

        JButton authenticateButton = new JButton(new AuthenticateAction());
        authenticateButton.setPreferredSize(new Dimension(100, 30));
        panel.add(authenticateButton);

        return panel;
    }

    @Override
    public void createRemoteRepository() {
    }

    @Override
    public String getRemoteRepositoryId() {
        return null;
    }

    @Override
    public boolean isValidInput() {
        return false;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
    }

    private class RepositoryUrlListener implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent event) {
            validateRepositoryURL(repositoryUrlField.getText());
        }

        @Override
        public void removeUpdate(DocumentEvent event) {
        }

        @Override
        public void changedUpdate(DocumentEvent event) {
        }

        private void validateRepositoryURL(String text) {

        }
    }

    private class AuthenticateAction extends AbstractAction {
        private AuthenticateAction() {
            super("Authenticate");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            validateRepositoryURL();
        }

        private void validateRepositoryURL() {
            try {
                URL repositoryURL = new URL(repositoryUrlField.getText());
                Git git = Git.cloneRepository()
                        .setRemote("origin")
                        .setURI(repositoryUrlField.getText())
                        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(usernameField.getText(), passwordField.getText()))
                        .setDirectory(new File("D:/IdeaProjects/Smartbear/test-git-plugin"))
                        .call();

            } catch (MalformedURLException e) {
                UISupport.showErrorMessage("Invalid repository URL: " + repositoryUrlField.getText());
            } catch (IOException e) {
                UISupport.showErrorMessage("Incorrect project dir: " + project.getPath());
            } catch (GitAPIException e) {
                e.printStackTrace();
            }
        }
    }
}
