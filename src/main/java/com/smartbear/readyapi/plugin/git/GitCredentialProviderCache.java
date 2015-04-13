package com.smartbear.readyapi.plugin.git;

import org.eclipse.jgit.transport.CredentialsProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to keep the in memory copy of CredentialsProvider so that user doesn't have to enter credentials for VCS every operation (pull, push, fetch)
 */
public class GitCredentialProviderCache {
    private static Map<String, CredentialsProvider> credentialsProviderMap = new HashMap<>();

    public static void addCredentialProvider(CredentialsProvider credentialsProvider, String gitRepo) {
        credentialsProviderMap.put(gitRepo, credentialsProvider);
    }

    public static CredentialsProvider getCredentialsProvider(String gitRepo) {
        return credentialsProviderMap.get(gitRepo);
    }
}
