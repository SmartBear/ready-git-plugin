package com.smartbear.readyapi.plugin.git;

public interface Credentials {
    String getUsername();

    String getPassword();

    String getPrivateKeyPath();
}
