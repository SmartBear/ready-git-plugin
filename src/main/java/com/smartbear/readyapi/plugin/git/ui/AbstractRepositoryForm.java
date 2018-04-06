package com.smartbear.readyapi.plugin.git.ui;

public abstract class AbstractRepositoryForm implements RepositoryForm {

    protected final String repoUrl;
    protected final boolean repoUrlReadOnly;

    public AbstractRepositoryForm(String repoUrl, boolean repoUrlReadOnly) {
        this.repoUrl = repoUrl;
        this.repoUrlReadOnly = repoUrlReadOnly;
    }
}
