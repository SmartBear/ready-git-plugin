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
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class SshRepositoryForm implements RepositoryForm {

    private JTextField repositoryUrlField;
    private JTextArea commitMessageField;

    private JTextField passphraseField;
    private JTextField sshKeyPathField;

    @Override
    public String getRepositoryPath() {
        return repositoryUrlField.getText();
    }

    @Override
    public String getCommitMessage() {
        return commitMessageField.getText();
    }

    @Override
    public CredentialsProvider getCredentialsProvider() {
        return new SshPassphraseCredentialsProvider(passphraseField.getText());
    }

    @Override
    public Component getComponent() {
        JPanel sshCard = new JPanel(new MigLayout("wrap 3", "0[shrink][grow,fill][shrink]0", "0[]8[]0"));

        sshCard.add(new JLabel("Repository URL:"));
        repositoryUrlField = new JTextField();
        sshCard.add(repositoryUrlField, "spanx");

        sshCard.add(new JLabel("SSH key path:"));
        sshKeyPathField = new JTextField();
        sshCard.add(sshKeyPathField);
        final JButton selectFileButton = createSelectFileButton(sshKeyPathField);
        sshCard.add(selectFileButton);

        sshCard.add(new JLabel("Passphrase:"));
        passphraseField = new JPasswordField();
        sshCard.add(passphraseField, "spanx");

        sshCard.add(new JLabel("Commit Message: "), "t");
        sshCard.add(createCommitMessageField(), "spanx");

        return sshCard;
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
                fileChooser.showOpenDialog(null);
                File selectedFile = fileChooser.getSelectedFile();
                textField.setText(selectedFile.getAbsolutePath());
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

    private JScrollPane createCommitMessageField() {
        commitMessageField = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(commitMessageField);
        scrollPane.setPreferredSize(new Dimension(100, 100));
        return scrollPane;
    }
}
