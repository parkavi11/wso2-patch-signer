package org.wso2.patchvalidator.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.patchvalidator.exceptions.ServiceException;
import org.wso2.patchvalidator.service.SyncService;

/**
 * <h1>Log Builder</h1>
 * Create singleton instance for Logger.
 *
 * @author Pramodya Mendis
 * @version 1.3
 * @since 2018-10-06
 */

public class LogBuilder {


    private static LogBuilder single_instance = null;
    public Logger LOG;

    private LogBuilder() {

        try {
            LOG = LoggerFactory.getLogger(SyncService.class);
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
