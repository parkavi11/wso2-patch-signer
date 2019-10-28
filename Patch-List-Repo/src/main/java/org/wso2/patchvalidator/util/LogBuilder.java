package org.wso2.patchvalidator.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.patchvalidator.exceptions.ServiceException;
import org.wso2.patchvalidator.service.Signer;

/**
 * Create singleton instance for Logger.
 */
public class LogBuilder {

    private static LogBuilder single_instance = null;
    public Logger LOG;

    private LogBuilder() {

        try {
            LOG = LoggerFactory.getLogger(Signer.class);
        } catch (Exception ex){
            throw new ServiceException("Exception occurred when creating Logger instance. ", ex);
        }
    }

    public static LogBuilder getInstance() {

        if (single_instance == null)
            single_instance = new LogBuilder();

        return single_instance;
    }
}
