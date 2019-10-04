package org.wso2.patchvalidator.util;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wso2.patchvalidator.client.PmtClient;
import org.wso2.patchvalidator.enums.PatchType;
import org.wso2.patchvalidator.exceptions.ServiceException;
import org.wso2.patchvalidator.store.PatchRequestDatabaseHandler;

import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.wso2.patchvalidator.validators.PatchZipValidator.extractFile;

/**
 * Common helper methods for the service.
 */
public class Util {

    /**
     * Convert http stream to string.
     *
     * @param is input stream
     * @return input stream string
     */
    public static String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException ex) {
            throw new ServiceException("Exception occurred when converting http stream to string",
                    "IO exception occurred when converting http stream, Please contact admin.", ex);
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                throw new ServiceException("Exception occurred when closing the http input stream",
                        "IO exception occurred when closing http input stream, Please contact admin.", ex);
            }
        }
        return sb.toString();
    }

    /**
     * Create list from json array.
     *
     * @param arr json array
     * @return list of strings
     */
    public static List<String> createListFromJsonArray(JSONArray arr) {

        List<String> list = new ArrayList<>();
        if (arr != null) {
            for (Object anArr : arr) {
                list.add((String) anArr);
            }
        }
        return list;
    }

    /**
     * Get all kernel products list from db.
     * productNameArray is the products array taken from patch info, this may contain wso2 products & kernel products.
     * will return wso2 products only, by taking all wso2 products for kernel products and add them with wso2 products
     * in the productNameArray.
     *
     * @param productNameArray array of names products & kernel products
     * @return array of product names
     */
    public static String getChannelName(String[] productNameArray) {
        String name ="";
        String channel = "";
        for (String productName : productNameArray) {
            productName = productName.replaceAll("\\s+", "");
            productName = productName.trim().toLowerCase();
            String pattern = "([a-z][a-z]*)+([0-9][0-9]*)+(\\.[0-9][0-9]*)+(\\.[0-9][0-9]*\\.)";
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(productName);
            while (matcher.find()) {
                name = (matcher.group(0));
            }
            if (name.equals("")){
                channel ="full";
            } else {
                channel =productName.replaceAll(name,"");
                channel= channel.trim();
            }
        }
        return channel;
    }

    public static String[] getProductList(String[] productNameArray) {

        PatchRequestDatabaseHandler patchRequestDatabaseHandler = new PatchRequestDatabaseHandler();
        ArrayList<String> kernelProduct = new ArrayList<>();
        for (String productName : productNameArray) {
            int product;
            String versions = "";
            for (product = 0; product < productName.length(); product++) {
                char c = productName.charAt(product);
                if ('0' <= c && c <= '9')
                    break;
            }
            String name = productName.substring(0, product);
            name = name.trim().toLowerCase();
            String patternStyle = "([0-9][0-9]*)+(\\.[0-9][0-9]*)+(\\.[0-9][0-9]*)";
            Pattern regex = Pattern.compile(patternStyle);
            Matcher matcher = regex.matcher(productName);
            //String versionString = productName.substring(product);
            while(matcher.find()){
                versions = (matcher.group(0));
            }
            if (name.equals("carbon")) {
                ArrayList<String> tempArr;
                try {
                    tempArr = patchRequestDatabaseHandler.getProductsByKernalVersion(versions);
                } catch (SQLException ex) {
                    throw new ServiceException(
                            "SQL exception occurred when retrieving products by kernel version," + " productName: "
                                    + productName,
                            "Cannot get products by kernel version \"" + productName + "\", Please contact admin "
                                    + "and update the database.", ex);
                }
                //remove carbon product from the retrieved products list and add all other products
                kernelProduct.addAll(removeCarbonProductFromList(tempArr));
            } else {
                String productAdd;
                try {
                    productAdd = patchRequestDatabaseHandler.getProductAbbreviation(name, versions);
                } catch (SQLException ex) {
                    throw new ServiceException("SQL exception occurred when retrieving product abbreviation by product"
                            + " name & version, name:" + name + " version:" + versions,
                            "Cannot get product abbreviation for the product \"" + name + "-" + versions + ","
                                    + " Please contact admin and update the database.", ex);
                }
                kernelProduct.add(productAdd);
            }
        }
        //remove the duplicates
        Set<String> hash = new HashSet<>();
        hash.addAll(kernelProduct);
        kernelProduct.clear();
        kernelProduct.addAll(hash);

        //convert array list to array
        String[] productArr = new String[kernelProduct.size()];
        productArr = kernelProduct.toArray(productArr);

        return productArr;
    }

    private static ArrayList<String> removeCarbonProductFromList(ArrayList<String> list) {

        for (String product : list) {
            if (product.contains("carbon")) {
                list.remove(product);
                return list;
            }
        }
        return list;
    }

    /**
     * Check two lists contain same elements, ignore order.
     *
     * @param list1 list one
     * @param list2 list two
     * @return whether both lists contain same elements without considering the order.
     */
    public static <T> boolean listEqualsIgnoreOrder(List<T> list1, List<T> list2) {
        return new HashSet<>(list1).equals(new HashSet<>(list2));
    }

    /**
     * Get patch type using product details table.
     *
     * @param patchId patch ID
     * @param version kernel version
     * @return type of the patch (Patch, Update or Patch&Update)
     */
    public static PatchType getPatchType(String patchId, String version) {

        try {
            JSONObject pmtJson = PmtClient.getPatchInfo(version, patchId);
            JSONArray pmtArray = (JSONArray) pmtJson.get("pmtResult");
            List<String> productsList = Collections.emptyList();

            for (Object aJsonArray : pmtArray) {

                JSONObject element = (JSONObject) aJsonArray;
                try {
                    if (element.get("name").equals("overview_products")) {
                        productsList = Util.createListFromJsonArray((JSONArray) element.get("value"));
                    }
                } catch (Exception ex) {
                    throw new ServiceException("Exception occurred, pmt patch info is not valid",
                            "Invalid patch information. Please re-submit.", ex);
                }
            }

            if (productsList.size() < 1) {
                throw new ServiceException("products list is empty",
                        "Invalid patch information, " + "products list is empty. Please amend and re-submit.");
            }

            //convert product array list to an array
            String[] productNameArray = new String[productsList.size()];
            productNameArray = productsList.toArray(productNameArray);
            //product list with kernel products
            String[] fullProductsList = Util.getProductList(productNameArray);

            boolean isPatch = false;
            boolean isUpdate = false;

            for (String product : fullProductsList) {

                PatchRequestDatabaseHandler db = new PatchRequestDatabaseHandler();
                int productType = db.getProductType(product);

                if (productType == 1)
                    isPatch = true;
                else if (productType == 2)
                    isUpdate = true;
                else if (productType == 3)
                    return PatchType.PATCH_AND_UPDATE;
                else
                    throw new ServiceException(
                            "cannot get the patch type of the patch from the database, patch:" + version + "-" + patchId
                                    + " product:" + product,
                            "Patch type is empty for the product \"" + product + "\" Please contact admin.");
            }

            if (isPatch && isUpdate)
                return PatchType.PATCH_AND_UPDATE;
            else if (isPatch)
                return PatchType.PATCH;
            else if (isUpdate)
                return PatchType.UPDATE;
            else
                throw new ServiceException(
                        "cannot get the patch type of the patch from the database, patch:" + version + "-" + patchId,
                        "Cannot get the patch type for the product list \"" + Arrays.toString(fullProductsList)
                                + "\" Please contact admin.");
        } catch (Exception ex) {
            throw new ServiceException("retrieving patch type failed for the patch:" + version + "-" + patchId,
                    "retrieving patch type failed for the patch \"" + version + "-" + patchId + "\", "
                            + "Please contact admin.", ex);
        }
    }

    /**
     * Determine whether a file is a JAR File.
     *
     * @param file file needed to be checked
     */
    public static boolean isJarFile(File file) throws IOException {
        if (!isZipFile(file)) {
            return false;
        }
        ZipFile zip = new ZipFile(file);
        boolean manifest = zip.getEntry("META-INF/MANIFEST.MF") != null;
        zip.close();
        return manifest;
    }

    /**
     * Determine whether a file is a ZIP File.
     *
     * @param file file needed to be checked
     */
    private static boolean isZipFile(File file) throws IOException {
        if (file.isDirectory()) {
            return false;
        }
        if (!file.canRead()) {
            throw new IOException("Cannot read file " + file.getAbsolutePath());
        }
        if (file.length() < 4) {
            return false;
        }
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        int test = in.readInt();
        in.close();
        return test == 0x504b0304;
    }

    /**
     * Unzip zip file.
     *
     * @param zipFilePath  "/Users/pankaj/tmp.zip"
     * @param destFilePath "/Users/pankaj/output"
     */
    public static void unZip(File zipFilePath, String destFilePath) {
        try {
            if (!zipFilePath.exists()) {
                return;
            }
            File destDir = new File(destFilePath);
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath));
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                String filePath = destDir + File.separator + zipEntry.getName();
                if (!zipEntry.isDirectory()) {
                    new File(filePath).getParentFile().mkdirs();
                    extractFile(zipInputStream, filePath);
                } else {
                    File dir = new File(filePath);
                    dir.mkdirs();
                }
                zipInputStream.closeEntry();
                zipEntry = zipInputStream.getNextEntry();
            }
            zipInputStream.close();

        } catch (Exception e) {
            throw new ServiceException(
                    "IO exception occurred when unzipping, zipFilePath:" + zipFilePath + " destDir:" + destFilePath,
                    "Update zip \"" + zipFilePath + "\" unzipping failed. ", e);
        }
    }
}
