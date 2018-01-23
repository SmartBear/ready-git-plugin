package com.smartbear.readyapi.plugin.git.ui;

import com.eviware.soapui.support.UISupport;
import com.smartbear.ready.ui.style.GlobalStyles;
import net.miginfocom.swing.MigLayout;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.util.List;

public class ConfirmMergeDialog extends JDialog {
    private final DefaultListModel<String> model;
    private boolean confirm;

    public ConfirmMergeDialog(List<String> conflictFiles) {
        super(UISupport.getMainFrame(), "Merge Conflicts", true);
        model = new DefaultListModel<>();
        for (String filePath : conflictFiles) {
            model.addElement(filePath);
        }

        buildUI();
        setResizable(false);
        pack();
    }

    private void buildUI() {
        Container container = getContentPane();
        container.setLayout(new MigLayout("wrap, fill", "8[grow]8", "8[]15[]8"));
        container.add(buildContent());

        JPanel buttonsPanel = new JPanel(new MigLayout("", "0[]8[]0", "0[]0"));
        JButton mergeButton = new JButton(new MergeAction("Merge Automatically", true));
        buttonsPanel.add(mergeButton);
        JButton cancelButton = new JButton(new MergeAction("Cancel", false));
        buttonsPanel.add(cancelButton);
        container.add(buttonsPanel, "center");
    }

    private JPanel buildContent() {
        JPanel contentPanel = new JPanel(new MigLayout("wrap", "0[fill,grow]0", "0[32!]4[]8[32!]0"));
        JLabel topLabel = new JLabel("<html><body>ReadyAPI is about to merge the project files on your computer with their versions from the Git repository.<br/>" +
                "It detected merge conflicts in the following files:</body></html>");
        contentPanel.add(topLabel);

        JList conflictFilesList = new JList(model);
        conflictFilesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        conflictFilesList.setVisibleRowCount(5);
        JScrollPane scrollPane = new JScrollPane(conflictFilesList);
        scrollPane.setBorder(BorderFactory.createLineBorder(GlobalStyles.getDefaultBorderColor()));
        contentPanel.add(scrollPane);

        JLabel bottomLabel = new JLabel("<html><body>If you merge changes automatically, the project files may get corrupted.<br/>" +
                "In this case, close the project and resolve merge issues with an external tool like KDiff.</body></html>");
        contentPanel.add(bottomLabel);
        return contentPanel;
    }

    public boolean allowMerge() {
        return confirm;
    }

    private class MergeAction extends AbstractAction {
        private final boolean result;

        public MergeAction(String name, boolean result) {
            super(name);
            this.result = result;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            confirm = result;
            ConfirmMergeDialog.this.setVisible(false);
        }
    }
}
