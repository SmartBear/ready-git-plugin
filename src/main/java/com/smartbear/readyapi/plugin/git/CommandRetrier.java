package com.smartbear.readyapi.plugin.git;

import com.eviware.soapui.plugins.vcs.VcsIntegrationException;
import com.eviware.soapui.support.UISupport;
import com.smartbear.readyapi.plugin.git.ui.GitAuthenticationDialog;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.transport.CredentialsProvider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

abstract class CommandRetrier {

    private Git git;

    protected CommandRetrier(Git git) {
        this.git = git;
    }

    abstract TransportCommand recreateCommand();

    public void execute() throws Throwable {
        TransportCommand command = recreateCommand();

        setCredentialsProviderFromCache(command, git);
        try {
            Method call = getMethodCall(command);
            call.invoke(command);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
            throw new VcsIntegrationException(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            if (shouldRetry(e.getCause())) {
                CredentialsProvider credentialsProvider = askForCredentials(getRemoteRepoURL(git));
                if (credentialsProvider != null) {
                    command = recreateCommand();
                    try {
                        Method setCredentialsProvider = getMethodSetCredentialsProvider(command);
                        setCredentialsProvider.invoke(command, credentialsProvider);
                        Method call = getMethodCall(command);
                        call.invoke(command);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                } else {
                    throw e.getCause();
                }
            }  else {
                e.printStackTrace();
                throw new VcsIntegrationException(e.getMessage(), e);
            }
        }
    }

    boolean shouldRetry(Throwable e) {
        return e instanceof TransportException
                && e.getMessage() != null
                && (e.getMessage().contains("not authorized") || e.getMessage().contains("no CredentialsProvider has been registered"));
    }

    private Method getMethodSetCredentialsProvider(Object command) throws NoSuchMethodException {
        return command.getClass().getMethod("setCredentialsProvider", new Class[]{CredentialsProvider.class});
    }

    private Method getMethodCall(Object command) throws NoSuchMethodException {
        return command.getClass().getMethod("call", new Class[]{});
    }

    private void setCredentialsProviderFromCache(TransportCommand transportCommand, Git git) {
        String remoteRepoURL = getRemoteRepoURL(git);
        CredentialsProvider credentialsProvider = GitCredentialProviderCache.getCredentialsProvider(remoteRepoURL);
        if (credentialsProvider != null) {
            transportCommand.setCredentialsProvider(credentialsProvider);
        }
    }

    private CredentialsProvider askForCredentials(String remoteRepoURL) {
        CredentialsProvider credentialsProvider;
        GitAuthenticationDialog authenticationDialog = new GitAuthenticationDialog(remoteRepoURL);
        UISupport.centerDialog(authenticationDialog);
        authenticationDialog.setVisible(true);

        credentialsProvider = authenticationDialog.getCredentialsProvider();
        GitCredentialProviderCache.addCredentialProvider(credentialsProvider, remoteRepoURL);
        return credentialsProvider;
    }

    private String getRemoteRepoURL(Git git) {
        return git.getRepository().getConfig().getString("remote", "origin", "url");
    }
}
