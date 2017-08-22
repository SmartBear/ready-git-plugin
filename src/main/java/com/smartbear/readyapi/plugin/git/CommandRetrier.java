package com.smartbear.readyapi.plugin.git;

import com.eviware.soapui.plugins.vcs.VcsIntegrationException;
import com.eviware.soapui.support.UISupport;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KnownHosts;
import com.jcraft.jsch.Session;
import com.smartbear.readyapi.plugin.git.ui.GitAuthenticationDialog;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

abstract class CommandRetrier {

    private final static Logger logger = LoggerFactory.getLogger(CommandRetrier.class);

    private Git git;

    protected CommandRetrier(Git git) {
        this.git = git;
    }

    abstract TransportCommand recreateCommand();

    Object execute() throws VcsIntegrationException {
        AuthenticatorState authenticatorState = AuthenticatorHelper.resetGlobalAuthenticator();
        TransportCommand command = recreateCommand();

        try {
            setCredentialsProviderFromCache(command);
            if (isSshAuthentication()) {
                CredentialsProvider credentialsProvider = askForCredentialsIfNotInCache();
                setCredentialsProvider(command, credentialsProvider);
            }
            return command.call();
        } catch (Exception e) {
            if (shouldRetry(e)) {
                CredentialsProvider credentialsProvider = askForCredentials(getRemoteRepoURL());
                if (credentialsProvider != null) {
                    command = recreateCommand();
                    try {
                        setCredentialsProvider(command, credentialsProvider);
                        return command.call();
                    } catch (Exception e1) {
                        throw new VcsIntegrationException(e.getMessage(), e);
                    }
                } else {
                    throw new VcsIntegrationException(e.getCause().getMessage(), e.getCause());
                }
            } else {
                throw new VcsIntegrationException(e.getMessage(), e);
            }
        } finally {
            AuthenticatorHelper.restoreGlobalAuthenticator(authenticatorState);
        }
    }

    private CredentialsProvider askForCredentialsIfNotInCache() {
        CredentialsProvider credentialsProvider = GitCredentialProviderCache.instance().getCredentialsProvider(getRemoteRepoURL());
        if (credentialsProvider == null) {
            credentialsProvider = askForCredentials(getRemoteRepoURL());
        }
        return credentialsProvider;
    }

    private void setCredentialsProvider(TransportCommand command, CredentialsProvider credentialsProvider) {
        if (credentialsProvider instanceof SshPassphraseCredentialsProvider) {
            command.setTransportConfigCallback(new SshTransportConfigCallback((SshPassphraseCredentialsProvider) credentialsProvider));
        }
        command.setCredentialsProvider(credentialsProvider);
    }

    private boolean isSshAuthentication() {
        return !getRemoteRepoURL().startsWith("http");
    }

    boolean shouldRetry(Throwable e) {
        return e instanceof TransportException
                && e.getMessage() != null
                && (e.getMessage().contains("not authorized")
                || e.getMessage().contains("Auth fail")
                || e.getMessage().contains("USERAUTH fail")
                || e.getMessage().contains("no CredentialsProvider has been registered")
                || e.getMessage().contains("Authorization Required"));
    }

    private void setCredentialsProviderFromCache(TransportCommand transportCommand) {
        String remoteRepoURL = getRemoteRepoURL();
        CredentialsProvider credentialsProvider = GitCredentialProviderCache.instance().getCredentialsProvider(remoteRepoURL);
        if (credentialsProvider != null) {
            setCredentialsProvider(transportCommand, credentialsProvider);
        }
    }

    private CredentialsProvider askForCredentials(String remoteRepoURL) {
        CredentialsProvider credentialsProvider;
        GitAuthenticationDialog authenticationDialog = new GitAuthenticationDialog(remoteRepoURL);
        UISupport.centerDialog(authenticationDialog);
        authenticationDialog.setVisible(true);

        credentialsProvider = authenticationDialog.getCredentialsProvider();
        GitCredentialProviderCache.instance().addCredentialProvider(credentialsProvider, remoteRepoURL);
        return credentialsProvider;
    }

    private String getRemoteRepoURL() {
        return git.getRepository().getConfig().getString("remote", "origin", "url");
    }

    private static class DefaultJschConfigSessionFactory extends JschConfigSessionFactory {
        private final SshPassphraseCredentialsProvider credentialsProvider;

        public DefaultJschConfigSessionFactory(SshPassphraseCredentialsProvider credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
        }

        @Override
        protected void configure(OpenSshConfig.Host hc, Session session) {
            session.setPassword(credentialsProvider.getPassword());
        }

        @Override
        protected JSch getJSch(OpenSshConfig.Host hc, FS fs) throws JSchException {
            JSch jsch = super.getJSch(hc, fs);
            jsch.removeAllIdentity();
            jsch.addIdentity(credentialsProvider.getPrivateKeyPath(), credentialsProvider.getPassword());
            try {
                // Hack for for the case when no known_hosts file exists on the machine. Using the public setter will
                // throw FileNotFoundException since the file doesn't exists yet, but the later code has functionality to create it.
                KnownHosts knownHosts = (KnownHosts) jsch.getHostKeyRepository();
                Field field = knownHosts.getClass().getDeclaredField("known_hosts");
                field.setAccessible(true);
                field.set(knownHosts, "~/.ssh/known_hosts");
                field.setAccessible(false);
            } catch (Exception e) {
                logger.warn("Unable to set known_hosts file", e);
            }
            return jsch;
        }
    }

    static class SshTransportConfigCallback implements TransportConfigCallback {
        private final SshPassphraseCredentialsProvider credentialsProvider;

        public SshTransportConfigCallback(SshPassphraseCredentialsProvider credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
        }

        @Override
        public void configure(Transport transport) {
            SshTransport sshTransport = (SshTransport) transport;
            sshTransport.setSshSessionFactory(new DefaultJschConfigSessionFactory(credentialsProvider));
        }
    }
}
