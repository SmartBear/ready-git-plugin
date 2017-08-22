package com.smartbear.readyapi.plugin.git;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.wsdl.support.http.ProxyUtils;

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


    static AuthenticatorState resetGlobalAuthenticator() {
        try {
            AuthenticatorState state = new AuthenticatorState(AuthenticatorHelper.getDefaultAuthenticator());
            if (state.getAuthenticator() != null) {
                if (AuthenticatorHelper.isInstall4jAuthenticator(state.getAuthenticator())) {
                    // Reset global Authenticator, which the install4j update check messed up
                    ProxyUtils.setGlobalProxy(SoapUI.getSettings());
                    state.shouldBeRestored(true);
                }

                Authenticator currentAuthenticator = AuthenticatorHelper.getDefaultAuthenticator();
                if ((currentAuthenticator != null) &&
                        AuthenticatorHelper.isReadyApiProxyAndServerAuthenticator(currentAuthenticator)) {
                    if (AuthenticatorHelper.isServerRequestorEnabled(currentAuthenticator)) {
                        AuthenticatorHelper.enableServerRequestor(currentAuthenticator, false);
                        state.shouldBeRestored(true);
                    }
                }
            }
            return state;
        } catch (Exception ignore) {
        }

        return null;
    }

    static void restoreGlobalAuthenticator(AuthenticatorState state) {
        if (state == null || !state.isShouldBeRestored()) {
            return;
        }

        try {
            Authenticator currentAuthenticator = AuthenticatorHelper.getDefaultAuthenticator();
            if (currentAuthenticator != null &&
                    AuthenticatorHelper.isReadyApiProxyAndServerAuthenticator(currentAuthenticator)) {
                AuthenticatorHelper.enableServerRequestor(currentAuthenticator, true);
            }
        } catch (Exception ignore) {
        }

        Authenticator.setDefault(state.getAuthenticator());
        state.shouldBeRestored(false);
    }
}

class AuthenticatorState {
    private Authenticator authenticator = null;
    private boolean restore = false;

    AuthenticatorState(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    Authenticator getAuthenticator() {
        return authenticator;
    }

    boolean isShouldBeRestored() {
        return restore;
    }

    void shouldBeRestored(boolean restore) {
        this.restore = restore;
    }
}
