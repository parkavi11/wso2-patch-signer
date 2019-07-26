package org.wso2.patchvalidator.revertor;

import java.util.Properties;
import org.wso2.patchvalidator.enums.PatchType;
import org.wso2.patchvalidator.exceptions.ServiceException;
import org.wso2.patchvalidator.store.PatchRequestDatabaseHandler;
import org.wso2.patchvalidator.util.PropertyLoader;

import static org.wso2.patchvalidator.util.Util.getPatchType;

/**
 * Revert patch.
 */

public class Reverter {

    private static Properties prop = PropertyLoader.getInstance().prop;

    /**
     * Revert patch.
     * if patch type is Update or Patch&Update, delete jars in TEMP_JAR_TIMESTAMP.
     * 1. revert PMT.
     * 2. delete UAT database rows related to Update.
     * 3. revert SVN.
     *
     * @param version carbon version
     * @param patchId 4 digit patch id
     */
    public static void patchRevert(String version, String patchId) {

        boolean isPmtReverted;
        boolean isUatReverted;
        boolean isSvnReverted;

        //get patch type
        PatchType patchType = getPatchType(patchId, version);

        //delete jars in the TEMP_JAR_TIMESTAMP for the reverting update
        if (patchType != PatchType.PATCH) {
            try {
                String updateId = prop.getProperty("orgUpdate") + version + "-" + patchId;
                PatchRequestDatabaseHandler db = new PatchRequestDatabaseHandler();
                db.deleteJarFromTemp(updateId);
            } catch (ServiceException ex) {
                throw new ServiceException("Exception occurred when deleting jars for update \"" + version + "-" +
                        patchId + "\" from TEMP_JAR_TIMESTAMP table.", ex.getDeveloperMessage(), ex);
            }
        }

        //1. revert pmt
        try {
            isPmtReverted = PmtReverter.revertPmt(version, patchId, patchType);
            if (!isPmtReverted) {
                throw new ServiceException("PMT revert failed for the patch:" + version + "-" + patchId,
                        "PMT revert failed for the patch \"" + version + "-" + patchId + "\", " +
                                "Please contact admin.");
            }
        } catch (ServiceException ex) {
            throw new ServiceException("Exception occurred when reverting PMT, patchId:" + patchId + " version:" +
                    version, ex.getDeveloperMessage(), ex);
        }

        //2. revert uat
        //do not revert uat, if patch type is patch only
        if (!(patchType == PatchType.PATCH)) {
            try {
                UatReverter uatReverter = new UatReverter();
                isUatReverted = uatReverter.revertUat(patchId);
                if (!isUatReverted) {
                    throw new ServiceException("PMT revert successful. UAT revert failed, version:" + version +
                            " patchId:" + patchId,
                            "PMT revert successful, UAT revert failed for the patch \"" + version + "-" +
                                    patchId + "\", Please contact admin.");
                }
            } catch (ServiceException ex) {
                throw new ServiceException("Exception occurred when reverting UAT, patchId:" + patchId + " version:" +
                        version, "Reverting PMT successful. UAT revert failed. " + ex.getDeveloperMessage(), ex);
            }
        }

        //3.revert svn
        try {
            isSvnReverted = SvnReverter.revertSvn(patchId, version, patchType);
            if (!isSvnReverted) {
                throw new ServiceException("PMT revert successful. UAT revert successful. SVN revert failed, " +
                        "version:" + version + " patchId:" + patchId,
                        "Revert PMT successful, revert UAT successful, revert SVN failed for the patch \"" +
                                version + "-" + patchId + ", Please contact admin.");
            }
        } catch (ServiceException ex) {
            throw new ServiceException("Exception occurred when reverting SVN, patchId:" + patchId + " version:" +
                    version, "Reverting PMT and WUM UAT successful. revert SVN failed. " + ex.getDeveloperMessage(),
                    ex);
        }
    }
}
