package org.wso2.patchvalidator.util;

import org.wso2.patchvalidator.exceptions.ServiceException;
import org.wso2.patchvalidator.service.SyncService;

import java.util.Properties;

/**
 * <h1>Property loader</h1>
 * Create singleton instance for Properties.
 * Load property file
 *
 * @author Pramodya Mendis
 * @version 1.3
 * @since 2018-10-06
 */

public class PropertyLoader {

    private static PropertyLoader single_instance = null;
    public Properties prop;

    private PropertyLoader() {

        try {
            prop = new Properties();
            prop.load(SyncService.class.getClassLoader().getResourceAsStream("application.properties"));
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
