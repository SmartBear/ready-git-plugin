package com.smartbear.readyapi.plugin.git.ui;

import com.eviware.soapui.support.UISupport;
import net.miginfocom.swing.MigLayout;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class GitAuthenticationDialog extends JDialog {

    private final JTextField userNameField;
    private final JTextField passwordField;

    public GitAuthenticationDialog(String repoUrl) {
        super(UISupport.getMainFrame(), "Authenticate", true);
        getRootPane().registerKeyboardAction(escapeActionListener(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        setResizable(false);
        setBackground(Color.WHITE);
        setSize(new Dimension(400, 200));

        JPanel panel = new JPanel(new MigLayout("wrap", "0[grow, fill]0[grow, fill]0", "0[grow, fill]0"));
        panel.add(new Label("Repository:"));
        panel.add(new Label(repoUrl));

        panel.add(new Label("Username"));
        userNameField = new JTextField();
        panel.add(userNameField);

        panel.add(new Label("Password:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        final JButton authenticateButton = new JButton(new AuthenticateAction());
        panel.add(authenticateButton, "wrap");

        setContentPane(panel);
    }

    public String getPassword() {
        return passwordField.getText();
    }

    public ActionListener escapeActionListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };
    }

    public String getUsername() {
        return userNameField.getText();
    }

    private class AuthenticateAction extends AbstractAction {
        public AuthenticateAction() {
            super("Authenticate");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            GitAuthenticationDialog.this.setVisible(false);
        }
    }

    public static void main(String[] args) {
        GitAuthenticationDialog authenticationDialog = new GitAuthenticationDialog("git@github.com:SmartBear/git-plugin-test-repo.git");
        authenticationDialog.setVisible(true);
    }
}
