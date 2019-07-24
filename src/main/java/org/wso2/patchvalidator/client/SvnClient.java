package org.wso2.patchvalidator.client;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.*;
import org.wso2.patchvalidator.constants.Constants;
import org.wso2.patchvalidator.exceptions.ServiceException;
import org.wso2.patchvalidator.util.LogBuilder;
import org.wso2.patchvalidator.util.PropertyLoader;

import static org.tmatesoft.svn.core.SVNURL.parseURIEncoded;

/**
 * Client for accessing WSO2 SVN.
 */
public class SvnClient {

    private static Properties prop = PropertyLoader.getInstance().prop;
    private static Logger LOG = LogBuilder.getInstance().LOG;

    public static String svnConnection(String svnURL, String svnUser, String svnPass) {

        DAVRepositoryFactory.setup();
        SVNRepositoryFactoryImpl.setup();
        SVNRepository repository;
        try {
            repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(svnURL));
            ISVNAuthenticationManager authManager = new BasicAuthenticationManager(new SVNAuthentication[] {
                    new SVNPasswordAuthentication(svnUser, svnPass, false, parseURIEncoded(svnURL), false) });
            repository.setAuthenticationManager(authManager);
            repository.testConnection();
            return Constants.CONNECTION_SUCCESSFUL;
        } catch (SVNException e) {
            LOG.error("SVN connection failed. The failed URL is : " + svnURL);
            return Constants.SVN_CONNECTION_FAIL_STATE;
        }

    }

    public static String commitKeys(String patchUrl, String patchDestination, String patchId, String version,
            String patchType) {

        final String signingScriptPath = prop.getProperty("signingScriptPath");

        //copy signing script to the patch location
        File source = new File(signingScriptPath);
        File dest = new File(patchDestination);
        try {
            FileUtils.copyFileToDirectory(source, dest);
        } catch (IOException ex) {
            throw new ServiceException(
                    "IO Exception occurred when copying files to directory, signingScriptPath:" + signingScriptPath
                            + " patchDestination:" + patchDestination);
        }
        String resultSVNCommit;

        try {
            Runtime.getRuntime().exec("chmod a+rwx " + patchDestination + "signing-script.sh");
            Process executor = Runtime.getRuntime()
                    .exec("bash " + patchDestination + "signing-script.sh " + patchDestination);
            executor.waitFor();
            resultSVNCommit = commitToSVN(patchUrl, patchDestination, patchId, version, patchType);
            return resultSVNCommit;

        } catch (InterruptedException | IOException e) {
            resultSVNCommit = e.getMessage();
            LOG.error("Exception occurred when committing svn, " + resultSVNCommit);
            return resultSVNCommit;
        }
    }

    public static boolean unlockAndDeleteSvnRepository(String patchUrl, String patchId, String version, String type) {

        final String svnBaseUrl = prop.getProperty("staticURL");
        String[] svnRepositoryFiles = prop.getProperty("svnRepositoryFiles").split(",");
        String patchIdReplaceTerm = prop.getProperty("patchNoReplaceTerm");
        String patchTypeReplaceTerm = prop.getProperty("patchTypeReplaceTerm");
        String patchUpReplaceTerm = prop.getProperty("patchUpReplaceTerm");
        String versionNo = prop.getProperty(version);

        SVNClientManager clientManager = buildClientManager(patchUrl);

        SVNWCClient wcClient = clientManager.getWCClient();
        SVNCommitClient commitClient = clientManager.getCommitClient();
        for (String file : svnRepositoryFiles) {
            file = file.replaceAll(patchIdReplaceTerm, patchId);
            file = file.replaceAll(patchTypeReplaceTerm, versionNo);
            file = file.replaceAll(patchUpReplaceTerm, type);
            try {
                SVNURL[] unlockFilesArray = new SVNURL[1];
                String path = svnBaseUrl + patchUrl + file;
                unlockFilesArray[0] = SVNURL.parseURIDecoded(path);
                wcClient.doUnlock(unlockFilesArray, true);
                if (!file.endsWith("zip")) {
                    commitClient.doDelete(unlockFilesArray, "Delete for revert the patch");
                }
            } catch (SVNException ex) {
                throw new ServiceException(
                        "SVNException occurred in SVN unlock process, patchUrl:" + patchUrl + " patchId:" + patchId
                                + " version:" + version + " type:" + type + " svnBaseUrl:" + svnBaseUrl
                                + " svnRepositoryFiles:" + Arrays.toString(svnRepositoryFiles) + " patchIdReplaceTerm:"
                                + patchIdReplaceTerm + " patchTypeReplaceTerm:" + patchTypeReplaceTerm
                                + " patchUpReplaceTerm:" + patchUpReplaceTerm + " versionNo:" + versionNo + " file:"
                                + file, "SVN exception occurred in SVN unlock process for the patch \"" + patchUrl
                        + "\", Please contact admin.", ex);
            }
        }
        return true;
    }

    private static String commitToSVN(String patchUrl, String patchDestination, String patchId, String version,
            String patchType) {

        final String username = prop.getProperty("username");
        final String password = prop.getProperty("password");
        try {
            setupLibrary();
            SVNURL svnUrl = SVNURL.parseURIDecoded(
                    prop.get("staticURL") + //"https://svn.wso2.com/wso2/custom/projects/projects/carbon/"
                            patchUrl);
            SVNRepository repository = SVNRepositoryFactory.create(svnUrl, null);
            ISVNOptions myOptions = SVNWCUtil.createDefaultOptions(true);

            ISVNAuthenticationManager myAuthManager = SVNWCUtil.createDefaultAuthenticationManager(username, password);
            repository.setAuthenticationManager(myAuthManager);

            SVNClientManager clientManager = SVNClientManager.newInstance(myOptions, myAuthManager);
            SVNCommitClient commitClient = clientManager.getCommitClient();
            SVNWCClient wcClient = clientManager.getWCClient();

            //select each files for commit to svn
            File[] files = new File(patchDestination).listFiles();
            List<String> commitFilesList = new ArrayList<>();
            List<String> lockFilesList = new ArrayList<>();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && (!FilenameUtils.getExtension(file.getName()).equals("sh")) && (!FilenameUtils
                            .getExtension(file.getName()).equals("zip"))) {
                        commitFilesList.add(file.getName());
                    }
                    if (file.isFile() && (!FilenameUtils.getExtension(file.getName()).equals("sh"))) {
                        lockFilesList.add(file.getName());
                    }
                }
            }

            for (String commitFile : commitFilesList) {
                File fileToCheckIn = new File(patchDestination + commitFile);
                SVNCommitInfo importInfo = commitClient
                        .doImport(fileToCheckIn, SVNURL.parseURIDecoded(svnUrl + "/" + commitFile),
                                "sign and keys generate", true);
                importInfo.getNewRevision();
            }

            SVNURL[] lockFilesArray = new SVNURL[1];
            for (String lockFile : lockFilesList) {
                SVNURL fileToLock = SVNURL.parseURIDecoded(svnUrl + "/" + lockFile);
                lockFilesArray[0] = fileToLock;
                wcClient.doLock(lockFilesArray, true, "Patches are not allowed to modify after signed.");
            }
            try {
                validateSVNCheck(patchId, version, patchUrl, username, password, patchType);
            } catch (ServiceException ex) {
                LOG.error(Constants.COMMIT_KEYS_FAILURE, ex);
                return Constants.COMMIT_KEYS_FAILURE;
            }
            return (Constants.SUCCESSFULLY_KEY_COMMITTED);

        } catch (SVNException e) {
            SVNErrorMessage err = e.getErrorMessage();
            while (err != null) {
                LOG.error(err.getErrorCode().getCode() + " : " + err.getMessage());
                err = err.getChildErrorMessage();
            }
            return (Constants.COMMIT_KEYS_FAILURE);
        }
    }

    //build client to revert SVN
    private static SVNClientManager buildClientManager(String patchUrl) {

        final String username = prop.getProperty("username");
        final String password = prop.getProperty("password");
        final String svnBaseUrl = prop.getProperty("staticURL");

        try {
            DAVRepositoryFactory.setup();
            SVNRepositoryFactoryImpl.setup();
            FSRepositoryFactory.setup();

            SVNURL svnUrl = SVNURL.parseURIDecoded(svnBaseUrl + patchUrl);
            SVNRepository repository = SVNRepositoryFactory.create(svnUrl, null);
            ISVNOptions myOptions = SVNWCUtil.createDefaultOptions(true);

            ISVNAuthenticationManager myAuthManager = SVNWCUtil.createDefaultAuthenticationManager(username, password);
            repository.setAuthenticationManager(myAuthManager);

            return SVNClientManager.newInstance(myOptions, myAuthManager);
        } catch (SVNException ex) {
            throw new ServiceException(
                    "SVNException occurred when building client manager, patchUrl:" + patchUrl + " username:" + username
                            + " password:" + password + " svnBaseUrl:" + svnBaseUrl,
                    "SVN exception occurred when building client manager for the patch \"" + patchUrl
                            + "\", Please contact admin.", ex);
        }
    }

    //check files are committed to SVN
    private static void validateSVNCheck(String patchId, String version, String patchUrl, String username,
            String password, String patchType) {

        final String svnBaseUrl = prop.getProperty("staticURL");
        String[] svnRepositoryFiles = prop.getProperty("svnRepositoryFiles").split(",");
        String patchIdReplaceTerm = prop.getProperty("patchNoReplaceTerm");
        String patchTypeReplaceTerm = prop.getProperty("patchTypeReplaceTerm");
        String patchUpReplaceTerm = prop.getProperty("patchUpReplaceTerm");

        for (String file : svnRepositoryFiles) {
            file = file.replaceAll(patchIdReplaceTerm, patchId);
            file = file.replaceAll(patchTypeReplaceTerm, version);
            file = file.replaceAll(patchUpReplaceTerm, patchType);
            String svnBaseUrls = svnBaseUrl + patchUrl + file;

            try {
                DAVRepositoryFactory.setup();
                SVNRepositoryFactoryImpl.setup();
                FSRepositoryFactory.setup();

                SVNURL svnUrl = SVNURL.parseURIDecoded(svnBaseUrls);
                SVNRepository repository = SVNRepositoryFactory.create(svnUrl, null);
                ISVNAuthenticationManager myAuthManager = SVNWCUtil
                        .createDefaultAuthenticationManager(username, password);
                repository.setAuthenticationManager(myAuthManager);
                SVNNodeKind nodeKind = repository.checkPath("", -1);
                if (nodeKind != SVNNodeKind.FILE) {
                    String errorMessage = patchType + " Keys committed failed. missing key: " + file;
                    throw new ServiceException(errorMessage, errorMessage + " " + Constants.CONTACT_ADMIN);
                }
            } catch (SVNException ex) {
                throw new ServiceException(
                        "SVN Exception occurred when checking SVN directory after committing." + " patchId:" + patchId
                                + " version:" + version + " patchUrl:" + patchUrl + " username:" + username
                                + " password:" + password + " svnBaseUrl:" + svnBaseUrl + " svnRepositoryFiles:"
                                + Arrays.toString(svnRepositoryFiles) + " patchIdReplaceTerm:" + patchIdReplaceTerm
                                + " patchVersion:" + patchTypeReplaceTerm + " version:" + version,
                        "Exception occurred when checking patch SVN directory" + " after keys committing, "
                                + Constants.CONTACT_ADMIN, ex);
            }
        }
    }

    private static void setupLibrary() {

        DAVRepositoryFactory.setup();
        SVNRepositoryFactoryImpl.setup();
        FSRepositoryFactory.setup();
    }
}
