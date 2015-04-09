package com.smartbear.readyapi.plugin.git.ui;

import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.plugins.vcs.RepositorySelectionGui;
import com.eviware.soapui.plugins.vcs.VcsIntegrationException;
import net.miginfocom.swing.MigLayout;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.merge.MergeStrategy;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Label;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

public class GitRepositorySelectionGui implements RepositorySelectionGui {

    private JTextField repositoryUrlField;
    private JTextField usernameField;
    private JTextField passwordField;
    private Project project;
    private JTextField commitMessageField;

    public GitRepositorySelectionGui(Project project) {
        this.project = project;
    }

    @Override
    public Component getComponent() {
        JPanel panel = new JPanel(new MigLayout("wrap 2", "8[shrink]8[grow,fill]8", "8[]8"));

        panel.add(new JLabel("Repository URL:"));
        repositoryUrlField = new JTextField();
        panel.add(repositoryUrlField);

        panel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        panel.add(usernameField);

        panel.add(new Label("Password:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        panel.add(new Label("Commit message: "));
        commitMessageField = new JTextField();
        panel.add(commitMessageField);
        return panel;
    }

    @Override
    public void createRemoteRepository() {
        try {
            Git git = initRepository();

            git.add().addFilepattern(".").call();
            git.commit().setMessage(commitMessageField.getText()).call();
            git.pull().setStrategy(MergeStrategy.OURS).call();
            git.push().setPushAll().call();
        } catch (GitAPIException | IOException e) {
            throw new VcsIntegrationException("Failed to share project", e);
        }
    }

    private Git initRepository() throws GitAPIException, IOException {
        Git git = Git.init().setDirectory(new File(project.getPath())).call();

        StoredConfig config = git.getRepository().getConfig();
        config.setString("remote", "origin", "url", repositoryUrlField.getText());
        config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
        config.save();

        return git;
    }

    @Override
    public String getRemoteRepositoryId() {
        return repositoryUrlField.getText();
    }

    @Override
    public boolean isValidInput() {
        return true; // TODO: validate input..
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
    }
}
