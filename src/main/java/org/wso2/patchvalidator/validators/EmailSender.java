/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.patchvalidator.validators;

import java.util.ArrayList;
import java.util.Properties;
import org.wso2.patchvalidator.exceptions.ServiceException;
import org.wso2.patchvalidator.util.PropertyLoader;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Send email to developer and user with the results of validation.
 */
public class EmailSender {


    private static Properties prop = PropertyLoader.getInstance().prop;

    private static void sendEmail(String fromAddress, ArrayList<String> toList, ArrayList<String> ccList,
                                  String subject, String body){

        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "false");
        prop.put("mail.smtp.host", prop.getProperty("host"));
        prop.put("mail.smtp.port", prop.getProperty("emailPort"));

        javax.mail.Session session = javax.mail.Session.getDefaultInstance(prop, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {

                return new PasswordAuthentication(prop.getProperty("userEmail"), prop.getProperty("emailPassword"));
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));

            for (String aToList : toList) {
                message.addRecipient(Message.RecipientType.TO,
                        new InternetAddress(aToList));
            }
            for (String aCcList : ccList) {
                message.addRecipient(Message.RecipientType.CC,
                        new InternetAddress(aCcList));
            }

            message.setReplyTo(new InternetAddress[]
                    {new InternetAddress(prop.getProperty("ccList1"))});  //added now

            message.setSubject(subject);
            message.setContent(body, "text/html");

            Transport transport = session.getTransport(prop.getProperty("protocol"));
            transport.connect(prop.getProperty("host"), prop.getProperty("userEmail"),
                    prop.getProperty("emailPassword"));
            Transport.send(message);

        } catch (MessagingException mex) {
            throw new ServiceException("Messaging Exception occurred, Email sending failed. ",
                    "Sending email failed, Please contact admin.", mex);

        }
    }

    public static void executeSendMail(ArrayList<String> toList, ArrayList<String> ccList, String patchId,
                                              String version, String patchValidateStatus, String updateValidateStatus,
                                              String pmtUpdateStatus) {

        String subject =""; 
        String validationReturner= "";

        {
            String pmtUpdateStatusRow = "";
            if (!pmtUpdateStatus.equals("N/A")) {
                if (pmtUpdateStatus.equals(" PMT LC state updated to \"Released not in public svn\" state.") ||
                        pmtUpdateStatus.equals(" PMT LC state updated to \"UAT staging\" state.")) {

                    pmtUpdateStatusRow = "<table style=\"border-collapse:collapse;width:100%\"border=\"1px\">" +
                            "<tr style=\"font-size: 12\">" +
                            "<td align=\"center\" bgcolor=\"#def2d6\">" +
                            pmtUpdateStatus +
                            "</td>" +
                            "</tr></table>";

                    validationReturner = "<html><body><p>Patch signed successfully.</p> " +
                            "<table style=\"border-collapse:collapse;width:100%\"border=\"1px\">" +
                            "<tr style=\"font-size: 12\">" +
                            "<th bgcolor='black' width='50%'><font color=\"white\">Update validate status</font></th>" +
                            "<th bgcolor='black' width='50%'><font color=\"white\">Patch validate status</font></th>" +
                            "<tr>" +
                            "<td align=\"center\">" + updateValidateStatus + "</td>" +
                            "<td align=\"center\">" + patchValidateStatus + "</td>" +
                            "</tr></table>\n" +
                            pmtUpdateStatusRow +
                            "\n" +
                            "</body>\n" +
                            "</html>";

                    subject = "[SIGN REQUEST SUCCESS] Sign the patch WSO2-CARBON-PATCH-" + version + "-" + patchId;
                } else {
                    pmtUpdateStatusRow = "<table style=\"border-collapse:collapse;width:100%\"border=\"1px\">" +
                            "<tr style=\"font-size: 12\">" +
                            "<td align=\"center\" bgcolor=\"#ebc8c4\">" +
                            pmtUpdateStatus +
                            "</td>" +
                            "</tr></table>";

                    validationReturner = "<html><body><p>Failed to sign.</p> " +
                            "<table style=\"border-collapse:collapse;width:100%\"border=\"1px\">" +
                            "<tr style=\"font-size: 12\">" +
                            "<th bgcolor='black' width='50%'><font color=\"white\">Update validate status</font></th>" +
                            "<th bgcolor='black' width='50%'><font color=\"white\">Patch validate status</font></th>" +
                            "<tr>" +
                            "<td align=\"center\">" + updateValidateStatus + "</td>" +
                            "<td align=\"center\">" + patchValidateStatus + "</td>" +
                            "</tr></table>\n" +
                            pmtUpdateStatusRow +
                            "\n" +
                            "</body>\n" +
                            "</html>";

                    subject = "[SIGN REQUEST FAILED] Sign the patch WSO2-CARBON-PATCH-" + version + "-" + patchId;
                }
            }
            EmailSender.sendEmail(prop.getProperty("userEmail"), toList, ccList, subject, validationReturner);
        }
    }

    public static void setCCList(String developedBy, ArrayList<String> toList,
                           ArrayList<String> ccList) {

        toList.add(developedBy);
        ccList.add(prop.getProperty("ccList1"));
        ccList.add(prop.getProperty("ccList2"));
        ccList.add(prop.getProperty("ccList3"));
        ccList.add(prop.getProperty("ccList4"));
        ccList.add(prop.getProperty("ccList5"));

    }
}
