package com.smartbear.readyapi.plugin.git.ui;

import com.eviware.soapui.support.StringUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.CardLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import static com.eviware.soapui.support.StringUtils.repeat;
import static com.eviware.soapui.support.UISupport.createLabelLink;

public abstract class AbstractRepositorySelectionGui {
    public static final String LABEL_HTTPS = "HTTPS";
    public static final String LABEL_SSH = "SSH";

    private RepositoryForm sshRepositoryForm = new SshRepositoryForm();
    private RepositoryForm httpsRepositoryForm = new HttpsRepositoryForm();

    protected JPanel cards = new JPanel(new CardLayout());

    protected RepositoryForm selected;

    protected Component createGui(String helpUrl, String helpText, String remoteRepositoryUrl) {

        boolean isHttp = isHttpUrl(remoteRepositoryUrl);

        sshRepositoryForm = new SshRepositoryForm(isHttp ? null : remoteRepositoryUrl, false);
        httpsRepositoryForm = new HttpsRepositoryForm(isHttp ? remoteRepositoryUrl : null, false);

        List<RepositorySelection> repositorySelections = getRepositorySelections();

        JPanel panel = new JPanel(new MigLayout("wrap", "8[grow,fill]8", "8" + repeat("[]", repositorySelections.size()) + "[grow,fill][]8"));

        ButtonGroup group = new ButtonGroup();

        for (RepositorySelection repositorySelection : repositorySelections) {
            panel.add(repositorySelection.radioButton);
            group.add(repositorySelection.radioButton);
            cards.add(repositorySelection.repositoryForm.getComponent(), repositorySelection.label);
        }

        panel.add(cards);
        panel.add(createLabelLink(helpUrl, helpText));

        boolean shown = false;
        for (RepositorySelection repositorySelection : repositorySelections) {

            if ((LABEL_HTTPS.equals(repositorySelection.label) && isHttp)
                    || (LABEL_SSH.equals(repositorySelection.label) && !isHttp)) {
                switchRepositorySource(repositorySelection);
                shown = true;
                break;
            }
        }
        if (!shown) {
            repositorySelections.stream().findFirst().ifPresent(this::switchRepositorySource);
        }
        return panel;
    }

    private void switchRepositorySource(RepositorySelection repositorySelection) {
        repositorySelection.radioButton.setSelected(true);
        switchRepositorySourcePanel(repositorySelection.label);
    }

    private void switchRepositorySourcePanel(String label) {
        selectCard(label);
        showCard(label);
    }

    protected boolean isHttpUrl(String remoteRepositoryUrl) {
        return remoteRepositoryUrl != null && remoteRepositoryUrl.startsWith("http");
    }

    protected List<RepositorySelection> getRepositorySelections() {
        List<RepositorySelection> result = new ArrayList<>();
        result.add(new RepositorySelection(LABEL_SSH, createRadioButton(LABEL_SSH), sshRepositoryForm));
        result.add(new RepositorySelection(LABEL_HTTPS, createRadioButton(LABEL_HTTPS), httpsRepositoryForm));
        return result;
    }

    protected JRadioButton createRadioButton(final String label) {
        JRadioButton radioButton = new JRadioButton(label, null, true);
        radioButton.addActionListener(e -> switchRepositorySourcePanel(label));
        return radioButton;
    }

    protected void selectCard(String label) {
        if (LABEL_SSH.equals(label)) {
            selected = sshRepositoryForm;
        } else if (LABEL_HTTPS.equals(label)) {
            selected = httpsRepositoryForm;
        }
    }

    protected void showCard(String label) {
        CardLayout cardLayout = (CardLayout) cards.getLayout();
        cardLayout.show(cards, label);
    }

    protected RepositoryForm getSelected() {
        return selected;
    }
}
