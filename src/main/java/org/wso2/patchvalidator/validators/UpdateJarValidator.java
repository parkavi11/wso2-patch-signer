package org.wso2.patchvalidator.validators;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import org.wso2.patchvalidator.exceptions.ServiceException;
import org.wso2.patchvalidator.store.PatchRequestDatabaseHandler;
import org.wso2.patchvalidator.util.Util;

/**
 * Validate JAR files inside updates. Ensure updated JARs releasing through WUM.
 */
class UpdateJarValidator {

    enum JAR_TIMESTAMP_TABLE {
        MASTER, TEMP
    }

    private static List<File> jarFiles = new ArrayList<>();

    /**
     * Validate jar files in a update.
     * If jar is completely new add it to Master table. Else if, jars manifest timestamp is greater than timestamp in
     * the Master table, insert manifest time to Temp and validate it. Else, fails the validation.
     *
     * @param updateFolderPath "/Users/Downloads/WSO2-CARBON-UPDATE-4.4.0-2243"
     * @param updateId         "WSO2-CARBON-UPDATE-4.4.0-2243"
     */
    static void validateUpdateFiles(String updateFolderPath, String updateId) {

        PatchRequestDatabaseHandler db = new PatchRequestDatabaseHandler();

        HashMap<String, String> newJars = new HashMap<>();
        HashMap<String, String> updatedJars = new HashMap<>();

        listJarFilesAndFilesSubDirectories(updateFolderPath);

        for (File jarFile : jarFiles) {

            String jarFileName = jarFile.getName();
            String dbTimestamp = db.getJarTimestamp(jarFileName);
            long jarTimestamp = getJarManifestTime(jarFile.getAbsolutePath());

            if (dbTimestamp.equals("")) { // jar is not in the db
                newJars.put(jarFileName, String.valueOf(jarTimestamp));
            } else if (Long.parseLong(dbTimestamp) < jarTimestamp) { // jar timestamp greater then db timestamp
                updatedJars.put(jarFileName, String.valueOf(jarTimestamp));
            } else {
                //remove all files from jarFiles
                jarFiles.clear();
                throw new ServiceException("Cannot validate this update, \"" + jarFileName + "\" is not updated. " +
                        "dbTimestamp:" + dbTimestamp + " jarTimestamp:" + jarTimestamp,
                        "Cannot validate this update, \"" + jarFileName + "\" is not updated. " +
                                "Please update it and re-submit.");
            }
        }

        //insert new jars to Master table
        insertMapToDb(newJars, JAR_TIMESTAMP_TABLE.MASTER, db, updateId);
        //insert updated jars to Temp table
        insertMapToDb(updatedJars, JAR_TIMESTAMP_TABLE.TEMP, db, updateId);
        //remove all files from jarFiles
        jarFiles.clear();

    }

    /**
     * Add all jar files to jarFiles arrayList in the given directory and the its sub directories.
     *
     * @param directoryName update directory path in localhost
     */
    private static void listJarFilesAndFilesSubDirectories(String directoryName) {

        File directory = new File(directoryName);
        //get all the files from a directory
        File[] fList = directory.listFiles();
        if (fList != null) {
            for (File file : fList) {
                if (file.isFile()) {
                    try {
                        if (Util.isJarFile(file.getAbsoluteFile())) { //file is a jar or war file
                            jarFiles.add(file);
                        }
                    } catch (IOException ex) {
                        throw new ServiceException("IO Exception occurred when reading file, file:" +
                                file.getAbsoluteFile(),
                                "Cannot read file \"" + file.getAbsoluteFile() + "\", Please contact admin.");
                    }
                } else if (file.isDirectory()) {
                    listJarFilesAndFilesSubDirectories(file.getAbsolutePath());
                }
            }
        } else {
            throw new ServiceException("Update zip folder is empty, directoryName:" + directoryName,
                    "Update zip folder \"" + directoryName + "\" cannot be empty, Please re-submit.");
        }
    }

    /**
     * get manifest time of a jar file.
     *
     * @param jarFile path of the jar file in the localhost
     * @return timestamp value of the manifest time
     */
    private static long getJarManifestTime(String jarFile) {

        try {
            JarFile jf = new JarFile(jarFile);
            ZipEntry manifest = jf.getEntry("META-INF/MANIFEST.MF");
            return manifest.getTime();
        } catch (IOException ex) {
            throw new ServiceException("IO Exception occurred when reading file, file:" + jarFile,
                    "Cannot read file \"" + jarFile + "\", Please contact admin.");
        }
    }

    /**
     * insert jars in to the database.
     *
     * @param map                 contains jar name and the manifest time as k,v
     * @param jar_timestamp_table table jars should be inserted
     * @param db                  PatchRequestDatabaseHandler object
     * @param updateId            update id
     */
    private static void insertMapToDb(HashMap map, JAR_TIMESTAMP_TABLE jar_timestamp_table,
                                      PatchRequestDatabaseHandler db, String updateId) {

        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            if (jar_timestamp_table == JAR_TIMESTAMP_TABLE.MASTER) {
                db.insertJarTimestamp(pair.getKey().toString(), pair.getValue().toString());
            } else if (jar_timestamp_table == JAR_TIMESTAMP_TABLE.TEMP) {
                db.insertTempJarTimestamp(pair.getKey().toString(), pair.getValue().toString(), updateId);
            }
            it.remove(); // avoids a ConcurrentModificationException
        }
    }
}
