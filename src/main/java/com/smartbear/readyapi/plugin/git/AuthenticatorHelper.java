package com.smartbear.readyapi.plugin.git;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Authenticator;

class AuthenticatorHelper {
    private static final String INSTALL4J_AUTHENTICATOR_CLASS = "com.install4j.runtime.installer.helper.content.HttpAuthenticator";
    private static final String READY_API_PROXY_AND_SERVER_AUTHENTICATOR_CLASS = "com.eviware.soapui.impl.wsdl.support.http.ProxySettingsAndServerAuthenticator";
    private static final String IS_SERVER_REQUESTOR_ENABLED_METHOD = "isServerRequestorEnabled";
    private static final String ENABLE_SERVER_REQUESTOR_METHOD = "enableServerRequestor";
    private static final String THE_AUTHENTICATOR_FIELD = "theAuthenticator";

    static Authenticator getDefaultAuthenticator() throws NoSuchFieldException, IllegalAccessException {
        Field f = java.net.Authenticator.class.getDeclaredField(THE_AUTHENTICATOR_FIELD);
        f.setAccessible(true);
        Authenticator authenticator = (Authenticator) f.get(null);
        f.setAccessible(false);

        return authenticator;
    }

    static boolean isInstall4jAuthenticator(Authenticator authenticator) {
        return authenticator != null && INSTALL4J_AUTHENTICATOR_CLASS.equals(authenticator.getClass().getName());
    }

    static boolean isReadyApiProxyAndServerAuthenticator(Authenticator authenticator) {
        return authenticator != null && READY_API_PROXY_AND_SERVER_AUTHENTICATOR_CLASS.equals(authenticator.getClass().getName());
    }

    static boolean isServerRequestorEnabled(Authenticator proxyAndServerAuthenticator)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method isServerRequestorEnabledMethod = proxyAndServerAuthenticator.getClass().getMethod(IS_SERVER_REQUESTOR_ENABLED_METHOD);
        return (boolean) isServerRequestorEnabledMethod.invoke(proxyAndServerAuthenticator);
    }

    static void enableServerRequestor(Authenticator proxyAndServerAuthenticator, boolean enabled)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method enableServerRequestorMethod = proxyAndServerAuthenticator.getClass().getMethod(ENABLE_SERVER_REQUESTOR_METHOD, boolean.class);
        enableServerRequestorMethod.invoke(proxyAndServerAuthenticator, enabled);
    }
}
