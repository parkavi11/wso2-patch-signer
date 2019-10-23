package org.wso2.patchvalidator.exceptions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Custom exception for the service to handle exceptions.
 */
public class ServiceException extends RuntimeException {

    private static Pattern REPLACE_PATTERN = Pattern.compile("\\{\\}");
    private String developerMessage;

    private static String generateMessage(String message, Object... args) {
        int index = 0;
        Matcher matcher = REPLACE_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(args[index++])));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }


    public ServiceException(String message, Object... args) {
        super((args.length > 0) ? generateMessage(message, args) : message);
    }

    public ServiceException(String message, Throwable cause, Object... args) {
        super((args.length > 0) ? generateMessage(message, args) : message, cause);
    }

    public ServiceException(String message, String developerMessage, Object... args) {
        super((args.length > 0) ? generateMessage(message, args) : message);
        setDeveloperMessage(developerMessage);
    }

    public ServiceException(String message, String developerMessage, Throwable cause, Object... args) {
        super((args.length > 0) ? generateMessage(message, args) : message, cause);
        setDeveloperMessage(developerMessage);
    }


    public String getDeveloperMessage() {
        return developerMessage;
    }

    private void setDeveloperMessage(String developerMessage) {
        this.developerMessage = developerMessage;
    }
}


