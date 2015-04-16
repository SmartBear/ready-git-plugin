package com.smartbear.readyapi.plugin.git.ui;

import com.eviware.soapui.support.StringUtils;
import com.smartbear.readyapi.plugin.git.SshPassphraseCredentialsProvider;
import net.miginfocom.swing.MigLayout;
import org.eclipse.jgit.transport.CredentialsProvider;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class SshRepositoryForm implements RepositoryForm {

    private JTextField repositoryUrlField;

    private JTextField passphraseField;
    private JTextField sshKeyPathField;
    private String repoUrl;
    private boolean repoUrlReadOnly;

    public SshRepositoryForm() {
        this("", false);
    }

    public SshRepositoryForm(String repoUrl, boolean repoUrlReadOnly) {
        this.repoUrl = repoUrl;
        this.repoUrlReadOnly = repoUrlReadOnly;
    }

    @Override
    public String getRepositoryPath() {
        return repositoryUrlField.getText();
    }

    @Override
    public CredentialsProvider getCredentialsProvider() {
        return new SshPassphraseCredentialsProvider(passphraseField.getText(), sshKeyPathField.getText());
    }

    @Override
    public JPanel getComponent() {
        JPanel sshCard = new JPanel(new MigLayout("wrap 3", "0[shrink][grow,fill][shrink]0", "0[]8[]0"));

        sshCard.add(new JLabel("Repository URL:"));
        repositoryUrlField = new JTextField(repoUrl);
        repositoryUrlField.setEditable(!repoUrlReadOnly);
        sshCard.add(repositoryUrlField, "spanx");

        sshCard.add(new JLabel("SSH key path:"));
        sshKeyPathField = new JTextField();
        sshKeyPathField.setText(getDefaultKeyPath());
        sshCard.add(sshKeyPathField);
        final JButton selectFileButton = createSelectFileButton(sshKeyPathField);
        sshCard.add(selectFileButton);

        sshCard.add(new JLabel("Passphrase:"));
        passphraseField = new JPasswordField();
        sshCard.add(passphraseField, "spanx");

        return sshCard;
    }

    private String getDefaultKeyPath() {
        String privateFilePath = getPrivateKeyFilePath("id_dsa");
        if (!new File(privateFilePath).exists()) {
            privateFilePath = getPrivateKeyFilePath("id_rsa");
        }
        return privateFilePath;
    }

    private String getPrivateKeyFilePath(String fileName) {
        return StringUtils.join(new String[]{System.getProperty("user.home"), ".ssh", fileName}, File.separator);
    }

    @Override
    public boolean isValid() {
        return StringUtils.hasContent(getRepositoryPath());
    }

    private JButton createSelectFileButton(final JTextField textField) {
        final JFileChooser fileChooser = createFileChooser();
        final JButton selectFileButton = new JButton("Select");
        selectFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    textField.setText(selectedFile.getAbsolutePath());
                }
            }
        });
        return selectFileButton;
    }

    private JFileChooser createFileChooser() {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        fileChooser.setFileHidingEnabled(false);
        return fileChooser;
    }
}
