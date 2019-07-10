package org.wso2.patchvalidator.revertor;

import java.util.Properties;
import java.util.logging.Logger;
import org.wso2.patchvalidator.client.SvnClient;
import org.wso2.patchvalidator.enums.PatchType;
import org.wso2.patchvalidator.exceptions.ServiceException;
import org.wso2.patchvalidator.util.LogBuilder;
import org.wso2.patchvalidator.util.PropertyLoader;

/**
 * <h1>SVN reverter</h1>
 * Revert SVN when reverting patch.
 *
 * @author Pramodya Mendis
 * @version 1.3
 * @since 2018-07-12
 */

@SuppressWarnings("ALL")
public class SvnReverter {


    private static Properties prop = PropertyLoader.getInstance().prop;
    private static org.slf4j.Logger LOG = LogBuilder.getInstance().LOG;


    public static boolean revertSvn(String patchId, String version, PatchType patchType) {

        version = prop.getProperty(version);
        boolean isSvnPatchRevertSuccess = false;
        boolean isSvnUpdateRevertSuccess = false;
        String url;

        if (patchType == PatchType.PATCH) {
            url = version + "/patches/patch" + patchId + "/";
            isSvnPatchRevertSuccess = SvnClient.unlockAndDeleteSvnRepository(url, patchId, version,
                    prop.getProperty("patch"));
            return isSvnPatchRevertSuccess;
        } else if (patchType == PatchType.UPDATE) {
            url = version + "/updates/update" + patchId + "/";
            isSvnUpdateRevertSuccess = SvnClient.unlockAndDeleteSvnRepository(url, patchId, version,
                    prop.getProperty("update"));
            return isSvnUpdateRevertSuccess;
        } else { //patchAndUpdate
            url = version + "/patches/patch" + patchId + "/";
            isSvnPatchRevertSuccess = SvnClient.unlockAndDeleteSvnRepository(url, patchId, version,
                    prop.getProperty("patch"));
            url = version + "/updates/update" + patchId + "/";
            isSvnUpdateRevertSuccess = SvnClient.unlockAndDeleteSvnRepository(url, patchId, version,
                    prop.getProperty("update"));
            if (isSvnPatchRevertSuccess && isSvnUpdateRevertSuccess) {
                return true;
            } else if (isSvnPatchRevertSuccess) {
                throw new ServiceException("Error occurred, SVN revert failed for the update in P&U type. "
                        + " patchId:" + patchId + " version:" + version + " url:" + url + " type:" + patchType +
                        "patchRevertSuccess:" + isSvnPatchRevertSuccess +
                        "updateRevertSuccess:" + isSvnUpdateRevertSuccess,
                        "SVN revert failed for the update \"" + version + "-" + patchId + "\" in P&U type, " +
                                "Please contact admin.");
            } else {
                throw new ServiceException("Error occurred, SVN revert failed for the patch in P&U type. "
                        + " patchId:" + patchId + " version:" + version + " url:" + url + " type:" + patchType +
                        "patchRevertSuccess:" + isSvnPatchRevertSuccess +
                        "updateRevertSuccess:" + isSvnUpdateRevertSuccess,
                        "SVN revert failed for the patch \"" + version + "-" + patchId + "\" in P&U type, " +
                                "Please contact admin.");
            }
        }
    }


}
