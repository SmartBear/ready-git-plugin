package com.smartbear.readyapi.plugin.git;

import com.eviware.soapui.support.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Scanner;

public class SshKeyFiles {

    private final static Logger logger = LoggerFactory.getLogger(SshKeyFiles.class);

    public static String getDefaultKeyPath() {
        String privateFilePath = getPrivateKeyFilePath("id_dsa");
        if (!new File(privateFilePath).exists()) {
            privateFilePath = getPrivateKeyFilePath("id_rsa");
        }
        return privateFilePath;
    }

    private static String getPrivateKeyFilePath(String fileName) {
        return StringUtils.join(new String[]{System.getProperty("user.home"), ".ssh", fileName}, File.separator);
    }

    static boolean keyFileIsEncrypted(File keyFile) {
        try {
            if (keyFile.exists()) {
                Scanner scanner = new Scanner(keyFile);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.contains("ENCRYPTED")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error while reading private key", e);
        }

        return false;
    }

    static boolean privateKeyHasAPassPhrase() {
        File privateKeyFile = new File(getDefaultKeyPath());
        return privateKeyFile.exists() && keyFileIsEncrypted(privateKeyFile);
    }
}
