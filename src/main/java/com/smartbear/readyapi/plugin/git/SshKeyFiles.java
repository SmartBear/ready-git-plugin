package com.smartbear.readyapi.plugin.git;

import com.eviware.soapui.support.StringUtils;

import java.io.File;
import java.util.Scanner;

public class SshKeyFiles {

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
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return false;
    }

    static boolean privateKeyHasAPassPhrase() {
        File privateKeyFile = new File(getDefaultKeyPath());
        return privateKeyFile.exists() && keyFileIsEncrypted(privateKeyFile);
    }
}
