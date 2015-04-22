package com.smartbear.readyapi.plugin.git;

import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class DefaultCredentialsProvider extends UsernamePasswordCredentialsProvider implements Credentials {
    private String username;
    private String password;

    public DefaultCredentialsProvider(String username, String password) {
        super(username, password);
        this.username = username;
        this.password = password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getPrivateKeyPath() {
        return "";
    }
}
