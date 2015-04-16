package com.smartbear.readyapi.plugin.git.ui;

import com.eviware.soapui.support.UISupport;
import net.miginfocom.swing.MigLayout;
import org.eclipse.jgit.transport.CredentialsProvider;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class GitAuthenticationDialog extends JDialog {

    private RepositoryForm repositoryForm;
    private boolean cancelled;

    public GitAuthenticationDialog(String repoUrl) {
        super(UISupport.getMainFrame(), "Authenticate", true);

        AuthenticateActionListener authenticateActionListener = new AuthenticateActionListener();
        CancelActionListener cancelActionListener = new CancelActionListener();

        getRootPane().registerKeyboardAction(cancelActionListener,
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        getRootPane().registerKeyboardAction(authenticateActionListener,
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        setResizable(false);
        setBackground(Color.WHITE);

        JPanel contentPanel = new JPanel(new MigLayout("wrap", "8[grow, fill]8", "8[]8"));


        repositoryForm = createRepositoryAuthenticationForm(repoUrl);
        contentPanel.add(repositoryForm.getComponent());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        final JButton authenticateButton = new JButton("Authenticate");
        authenticateButton.addActionListener(authenticateActionListener);
        buttonPanel.add(authenticateButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(cancelActionListener);
        buttonPanel.add(cancelButton);

        contentPanel.add(buttonPanel, "growx,spanx");
        setContentPane(contentPanel);
        pack();
    }

    public CredentialsProvider getCredentialsProvider() {
        return isCancelled() ? null : repositoryForm.getCredentialsProvider();
    }

    public boolean isCancelled() {
        return cancelled;
    }

    private RepositoryForm createRepositoryAuthenticationForm(String repoUrl) {
        if (repoUrl.startsWith("http")) {
            return new HttpsRepositoryForm(repoUrl, true);
        } else {
            return new SshRepositoryForm(repoUrl, true);
        }
    }

    private class CancelActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            dispose();
            cancelled = true;
        }
    }

    private class AuthenticateActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            dispose();
        }
    }
}
