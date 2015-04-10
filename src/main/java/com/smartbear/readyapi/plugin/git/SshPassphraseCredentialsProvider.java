package com.smartbear.readyapi.plugin.git;

import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;

public class SshPassphraseCredentialsProvider extends CredentialsProvider {
    private String passphrase;

    public SshPassphraseCredentialsProvider(String passphrase) {

        this.passphrase = passphrase;
    }

    @Override
    public boolean isInteractive() {
        return false;
    }

    @Override
    public boolean supports(CredentialItem... items) {
        return true;
    }

    @Override
    public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
        for (CredentialItem item : items) {
            ((CredentialItem.StringType) item).setValue(passphrase);
        }
        return true;
    }
}
