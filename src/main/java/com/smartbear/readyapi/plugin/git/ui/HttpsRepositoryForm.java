package com.smartbear.readyapi.plugin.git.ui;

import com.eviware.soapui.support.StringUtils;
import net.miginfocom.swing.MigLayout;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Component;

public class HttpsRepositoryForm implements RepositoryForm {

    private JTextField repositoryUrlField;

    private JTextField usernameField;
    private JTextField passwordField;

    @Override
    public String getRepositoryPath() {
        return repositoryUrlField.getText();
    }

    @Override
    public CredentialsProvider getCredentialsProvider() {
        return new UsernamePasswordCredentialsProvider(usernameField.getText(), passwordField.getText());
    }

    @Override
    public Component getComponent() {
        JPanel httpsCard = new JPanel(new MigLayout("wrap 2", "0[shrink][grow,fill]0", "0[]8[]0"));

        httpsCard.add(new JLabel("Repository URL:"));
        repositoryUrlField = new JTextField();
        httpsCard.add(repositoryUrlField, "spanx");
        httpsCard.add(new JLabel("Username:"));
        usernameField = new JTextField();
        httpsCard.add(usernameField);

        httpsCard.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        httpsCard.add(passwordField);
        return httpsCard;
    }

    @Override
    public boolean isValid() {
        return StringUtils.hasContent(getRepositoryPath());
    }
}
