package org.wso2.patchvalidator.authentication;

import java.util.Properties;
import org.slf4j.Logger;
import org.wso2.msf4j.security.basic.AbstractBasicAuthSecurityInterceptor;
import org.wso2.patchvalidator.util.LogBuilder;
import org.wso2.patchvalidator.util.PropertyLoader;

/**
 * <h1>Username and Password Validation</h1>
 * Before reply to the request from the outside requester, validate the
 * given username and password. This is a basic Auth.
 *
 * @author Kosala Herath
 * @version 1.3
 * @since 2017-12-14
 */
public class UsernamePasswordSecurityInterceptor extends AbstractBasicAuthSecurityInterceptor {

    private static final Logger LOG = LogBuilder.getInstance().LOG;
    private static final Properties prop = PropertyLoader.getInstance().prop;

    @Override
    protected boolean authenticate(String username, String password) {

        String validUsername = prop.getProperty("backend_service_username");
        String validPassword = prop.getProperty("backend_service_password");

        if (username.equals(validUsername) && password.equals(validPassword))
            return true;
        else{
            LOG.info("Authentication failed, username:" + username + " password:" + password);
            return false;
        }
    }
}
