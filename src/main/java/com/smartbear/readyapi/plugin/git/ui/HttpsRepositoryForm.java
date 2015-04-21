package com.smartbear.readyapi.plugin.git.ui;

import com.eviware.soapui.support.StringUtils;
import com.smartbear.readyapi.plugin.git.DefaultCredentialsProvider;
import net.miginfocom.swing.MigLayout;
import org.eclipse.jgit.transport.CredentialsProvider;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class HttpsRepositoryForm implements RepositoryForm {

    private JTextField repositoryUrlField;

    private JTextField usernameField;
    private JTextField passwordField;
    private String repoUrl;
    private boolean repoUrlReadOnly;

    public HttpsRepositoryForm() {
        this("", false);
    }

    public HttpsRepositoryForm(String repoUrl, boolean repoUrlReadOnly) {
        this.repoUrl = repoUrl;
        this.repoUrlReadOnly = repoUrlReadOnly;
    }

    @Override
    public String getRepositoryPath() {
        return repositoryUrlField.getText();
    }

    @Override
    public CredentialsProvider getCredentialsProvider() {
        return new DefaultCredentialsProvider(usernameField.getText(), passwordField.getText());
    }

    @Override
    public JPanel getComponent() {
        JPanel httpsCard = new JPanel(new MigLayout("wrap 2", "0[shrink][grow,fill]0", "0[]8[]0"));

        httpsCard.add(new JLabel("Repository URL:"));
        repositoryUrlField = new JTextField(repoUrl);
        repositoryUrlField.setEditable(!repoUrlReadOnly);
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
