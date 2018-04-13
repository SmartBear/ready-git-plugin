package com.smartbear.readyapi.plugin.git.ui;

import javax.swing.JRadioButton;

public class RepositorySelection {
    public final RepositoryForm repositoryForm;
    public final String label;
    public final JRadioButton radioButton;

    public RepositorySelection(String label, JRadioButton radioButton, RepositoryForm repositoryForm) {
        this.repositoryForm = repositoryForm;
        this.label = label;
        this.radioButton = radioButton;
    }
}
