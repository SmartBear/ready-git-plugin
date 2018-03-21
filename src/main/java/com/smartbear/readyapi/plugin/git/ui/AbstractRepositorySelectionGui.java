package com.smartbear.readyapi.plugin.git.ui;

import net.miginfocom.swing.MigLayout;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.eviware.soapui.support.UISupport.createLabelLink;

public abstract class AbstractRepositorySelectionGui {
    public static final String LABEL_HTTPS = "HTTPS";
    public static final String LABEL_SSH = "SSH";

    private RepositoryForm sshRepositoryForm = new SshRepositoryForm();
    private RepositoryForm httpsRepositoryForm = new HttpsRepositoryForm();
    private LocalRepositoryForm localRepositoryForm = new LocalRepositoryForm();

    private JPanel cards = new JPanel(new CardLayout());

    private RepositoryForm selected;

    protected Component createGui(String helpUrl, String helpText, String remoteRepositoryUrl) {
        JPanel panel = new JPanel(new MigLayout("wrap", "8[grow,fill]8", "8[][][][grow,fill][]8"));

        boolean httpUrl = isHttpUrl(remoteRepositoryUrl);
        sshRepositoryForm = new SshRepositoryForm(httpUrl ? null : remoteRepositoryUrl, false);
        httpsRepositoryForm = new HttpsRepositoryForm(httpUrl ? remoteRepositoryUrl : null, false);

        ButtonGroup group = new ButtonGroup();

        JRadioButton sshRadioButton = createRadioButton(LABEL_SSH, group);
        panel.add(sshRadioButton);
        JRadioButton httpsRadioButton = createRadioButton(LABEL_HTTPS, group);
        panel.add(httpsRadioButton);
        JRadioButton localRadioButton = createRadioButton("Local", group);
        panel.add(localRadioButton);

        cards.add(sshRepositoryForm.getComponent(), LABEL_SSH);
        cards.add(httpsRepositoryForm.getComponent(), LABEL_HTTPS);
        cards.add(localRepositoryForm.getComponent(), "Local");

        panel.add(cards);
        panel.add(createLabelLink(helpUrl, helpText));
        (httpUrl ? httpsRadioButton : sshRadioButton).setSelected(true);
        selectCard(httpUrl ? LABEL_HTTPS : LABEL_SSH);
        return panel;
    }

    protected boolean isHttpUrl(String remoteRepositoryUrl) {
        return remoteRepositoryUrl != null && remoteRepositoryUrl.startsWith("http");
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
        } else if (LABEL_HTTPS.equals(label)) {
            selected = httpsRepositoryForm;
        } else {
            selected = localRepositoryForm;
        }
        CardLayout cardLayout = (CardLayout) cards.getLayout();
        cardLayout.show(cards, label);
    }

    protected RepositoryForm getSelected() {
        return selected;
    }
}
