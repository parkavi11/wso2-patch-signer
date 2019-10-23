package org.wso2.patchvalidator.util;

import org.bouncycastle.crypto.Signer;
import org.wso2.patchvalidator.exceptions.ServiceException;

import java.util.Properties;

/**
 * Create singleton instance for Properties.
 * Load property file
 */
public class PropertyLoader {

    private static PropertyLoader single_instance = null;
    public Properties prop;

    private PropertyLoader() {

        try {
            prop = new Properties();
            prop.load(Signer.class.getClassLoader().getResourceAsStream("application.properties"));
        } catch (Exception ex){
            throw new ServiceException("Exception occurred when loading the property file. ", ex);
        }
    }

    public static PropertyLoader getInstance() {

        if (single_instance == null)
            single_instance = new PropertyLoader();

        return single_instance;
    }

}
