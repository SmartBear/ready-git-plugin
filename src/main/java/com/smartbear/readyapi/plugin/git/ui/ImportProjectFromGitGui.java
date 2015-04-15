package com.smartbear.readyapi.plugin.git.ui;

import com.eviware.soapui.plugins.vcs.ImportProjectFromVcsGui;
import com.eviware.soapui.plugins.vcs.VcsIntegrationException;
import com.eviware.soapui.plugins.vcs.VcsRepositoryInfo;
import com.smartbear.readyapi.plugin.git.ReadyApiGitIntegration;
import net.miginfocom.swing.MigLayout;
import org.eclipse.jgit.api.errors.GitAPIException;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import static com.eviware.soapui.support.UISupport.createLabelLink;

public class ImportProjectFromGitGui implements ImportProjectFromVcsGui {
    private ReadyApiGitIntegration readyApiGitIntegration;
    public static final String LABEL_HTTPS = "Https";
    public static final String LABEL_SSH = "SSH";
    private RepositoryForm sshRepositoryForm = new SshRepositoryForm();
    private RepositoryForm httpsRepositoryForm = new HttpsRepositoryForm();

    private JPanel cards = new JPanel(new CardLayout());
    private RepositoryForm selected;
    public ImportProjectFromGitGui(ReadyApiGitIntegration readyApiGitIntegration) {
        this.readyApiGitIntegration = readyApiGitIntegration;
    }

    @Override
    public Component getComponent() {
        JPanel panel = new JPanel(new MigLayout("wrap", "8[grow,fill]8", "8[][][grow,fill][]8"));

        ButtonGroup group = new ButtonGroup();

        panel.add(createRadioButton(LABEL_SSH, group));
        panel.add(createRadioButton(LABEL_HTTPS, group));

        cards.add(sshRepositoryForm.getComponent(), LABEL_SSH);
        cards.add(httpsRepositoryForm.getComponent(), LABEL_HTTPS);

        panel.add(cards);
        panel.add(createLabelLink("https://google.com", "Learn about GIT")); // TODO: correct url
        selectCard(LABEL_SSH);
        return panel;
    }

    @Override
    public VcsRepositoryInfo downloadProjectFiles(File emptyDirectory) {
        try {
            readyApiGitIntegration.cloneRepository(selected.getRepositoryPath(), selected.getCredentialsProvider(), emptyDirectory);
            return new VcsRepositoryInfo("Git", selected.getRepositoryPath());
        } catch (GitAPIException e) {
            throw new VcsIntegrationException("Failed to clone remote repository", e);
        }
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
    public boolean isValidInput() {
        return true;
    }
}
