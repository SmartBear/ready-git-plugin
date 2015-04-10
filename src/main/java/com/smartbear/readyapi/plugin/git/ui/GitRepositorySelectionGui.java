package com.smartbear.readyapi.plugin.git.ui;

import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.plugins.vcs.RepositorySelectionGui;
import com.eviware.soapui.plugins.vcs.VcsIntegrationException;
import net.miginfocom.swing.MigLayout;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.transport.CredentialsProvider;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import static com.eviware.soapui.support.UISupport.createLabelLink;

public class GitRepositorySelectionGui implements RepositorySelectionGui {

    public static final String LABEL_HTTPS = "Https";
    public static final String LABEL_SSH = "SSH";

    private RepositoryForm sshRepositoryForm = new SshRepositoryForm();
    private RepositoryForm httpsRepositoryForm = new HttpsRepositoryForm();
    private Project project;

    private JPanel cards = new JPanel(new CardLayout());
    private RepositoryForm selected;

    public GitRepositorySelectionGui(Project project) {
        this.project = project;
    }

    @Override
    public Component getComponent() {
        JPanel panel = new JPanel(new MigLayout("wrap", "8[grow,fill]8", "8[][][grow,fill][]8"));

        ButtonGroup group = new ButtonGroup();

        panel.add(createRadioButton(LABEL_SSH, group));
        panel.add(createRadioButton(LABEL_HTTPS, group));

        sshRepositoryForm = new SshRepositoryForm();

        cards.add(sshRepositoryForm.getComponent(), LABEL_SSH);
        cards.add(httpsRepositoryForm.getComponent(), LABEL_HTTPS);

        panel.add(cards);
        panel.add(createLabelLink("https://google.com", "Learn about GIT")); // TODO: correct url
        selectCard(LABEL_SSH);
        return panel;
    }

    private JRadioButton createRadioButton(final String label, ButtonGroup group) {
        JRadioButton radioButton = new JRadioButton(label, null, true);
        radioButton.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        selectCard(label);
                    }
                }
        );
        group.add(radioButton);
        return radioButton;
    }

    private void selectCard(String label) {
        if (LABEL_SSH.equals(label)) {
            selected = sshRepositoryForm;
        } else {
            selected = httpsRepositoryForm;
        }
        CardLayout cardLayout = (CardLayout) cards.getLayout();
        cardLayout.show(cards, label);
    }

    @Override
    public void createRemoteRepository() {
        try {
            CredentialsProvider credentialsProvider = selected.getCredentialsProvider();

            Git git = initRepository(selected.getRepositoryPath());
            git.add().addFilepattern(".").call();
            git.commit().setMessage(selected.getCommitMessage()).call();
            git.pull().setCredentialsProvider(credentialsProvider).setStrategy(MergeStrategy.OURS).call();
            git.push().setCredentialsProvider(credentialsProvider).setPushAll().call();
        } catch (GitAPIException | IOException e) {
            throw new VcsIntegrationException("Failed to share project", e);
        }
    }

    private Git initRepository(String path) throws GitAPIException, IOException {
        Git git = Git.init().setDirectory(new File(project.getPath())).call();

        StoredConfig config = git.getRepository().getConfig();
        config.setString("remote", "origin", "url", path);
        config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
        config.save();

        return git;
    }

    @Override
    public String getRemoteRepositoryId() {
        return selected.getRepositoryPath();
    }

    @Override
    public boolean isValidInput() {
        return selected.isValid();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
    }

}
