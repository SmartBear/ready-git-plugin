package com.smartbear.readyapi.plugin.git.ui;

import com.eviware.soapui.support.UISupport;
import net.miginfocom.swing.MigLayout;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.FlowLayout;
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

        JPanel panel = new JPanel(new MigLayout("wrap", "8[shrink]8[grow, fill]8", "8[]8"));
        panel.add(new JLabel("Repository:"));
        panel.add(new JLabel(repoUrl));

        panel.add(new JLabel("Username:"));
        userNameField = new JTextField();
        panel.add(userNameField);

        panel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        final JButton authenticateButton = new JButton(new AuthenticateAction());
        buttonPanel.add(authenticateButton);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel, "growx,spanx");

        setContentPane(panel);
        pack();
    }

    public String getPassword() {
        return passwordField.getText();
    }

    public String getUsername() {
        return userNameField.getText();
    }

    private ActionListener escapeActionListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };
    }

    private class AuthenticateAction extends AbstractAction {
        public AuthenticateAction() {
            super("Authenticate");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            dispose();
        }
    }
}
