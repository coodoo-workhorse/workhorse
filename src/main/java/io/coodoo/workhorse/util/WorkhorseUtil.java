package io.coodoo.workhorse.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.coodoo.workhorse.core.control.StaticConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkhorseUtil {

    private static Logger logger = LoggerFactory.getLogger(WorkhorseUtil.class);

    private static ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private WorkhorseUtil() {}

    /**
     * Maps a JSON to the corresponding Java class
     * 
     * @param <T> corresponding Java class
     * @param parametersJson JSON string
     * @param parametersClass corresponding Java class
     * @return Java class <tt>T</tt> object as defined in JSON string
     */
    public static <T> T jsonToParameters(String parametersJson, Class<T> parametersClass) {
        if (parametersJson == null || parametersJson.isEmpty()) {
            return null;
        }
        try {
            return (T) objectMapper.readValue(parametersJson, parametersClass);
        } catch (IOException e) {
            throw new RuntimeException("JSON Parameter could not be mapped to an object", e);
        }
    }

    /**
     * Maps a Java object to JSON
     * 
     * @param parametersObject Java object
     * @return JSON string
     */
    public static String parametersToJson(Object parametersObject) {
        if (parametersObject == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(parametersObject);
        } catch (IOException e) {
            throw new RuntimeException("Parameter object could not be mapped to json", e);
        }
    }

    /**
     * Calculates the timestamp of the given delay from now ({@link #timestamp()})
     * 
     * @param delayValue delay value, e.g. <tt>30</tt>
     * @param delayUnit delay unit, e.g. {@link ChronoUnit#MINUTES}
     * @return delay as timestamp
     */
    public static LocalDateTime delayToMaturity(Long delayValue, ChronoUnit delayUnit) {

        LocalDateTime plannedFor = null;
        if (delayValue != null && delayUnit != null) {
            plannedFor = timestamp().plus(delayValue, delayUnit);
        }
        return plannedFor;
    }

    /**
     * @return Current Time by zone defined in {@link WorkhorseConfig#TIME_ZONE}
     */
    public static LocalDateTime timestamp() {
        return LocalDateTime.now(ZoneId.of(StaticConfig.TIME_ZONE));
    }

    /**
     * Parses the stack trace of an exception into as String
     * 
     * @param throwable Exception
     * @return Stack trace as String (using line breaks)
     */
    public static String stacktraceToString(Throwable throwable) {
        try (StringWriter stringWriter = new StringWriter(); PrintWriter printWriter = new PrintWriter(stringWriter, true)) {
            throwable.printStackTrace(printWriter);
            printWriter.flush();
            stringWriter.flush();
            return stringWriter.toString();
        } catch (IOException iOException) {
            logger.error("Couldn't write exception!", throwable);
            return "Couldn't write exception!";
        }
    }

    /**
     * Combines all messages an exception contains to a joined message. Equals messages will not repeat themselves and in case there is no message, the root
     * exception name is the message e.g. "NullPointerException".
     * 
     * @param throwable Exception
     * @return concatenated messages as string
     */
    public static String getMessagesFromException(Throwable throwable) {
        try {
            List<String> messages = new ArrayList<>();
            collectMessagesFromException(messages, throwable);
            String joinedMessage = String.join(" | ", messages);

            if (joinedMessage.length() > 4000) { // avoid exceeding the column width
                joinedMessage = joinedMessage.substring(0, Math.min(joinedMessage.length(), 3997)) + "...";
            }
            return joinedMessage;
        } catch (Exception e) {
            logger.error("Couldn't get messages from exception!", throwable);
            return throwable.toString();
        }
    }

    private static void collectMessagesFromException(List<String> messages, Throwable throwable) {

        String message = throwable.toString(); // "class.name" OR "class.name : message"
        final String className = throwable.getClass().getName();
        // get rid of the class name
        if (message.startsWith(className + ": ")) {
            message = message.replace(className + ": ", "");
        }
        // check if it is same as the root cause
        if (throwable.getCause() != null && message.equals(throwable.getCause().toString())) {
            collectMessagesFromException(messages, throwable.getCause()); // shortcut
            return;
        }
        // check and add message
        if (!messages.contains(message) && !message.equals(className)) {
            messages.add(message);
        }
        if (throwable.getCause() != null) {
            // we need to go deeper
            collectMessagesFromException(messages, throwable.getCause());
        }
        if (messages.isEmpty()) {
            // at least put the exception name here
            messages.add(throwable.toString());
        }
    }

    /**
     * 
     * @return hostname of the running system
     */
    public static String getHostName() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            return ip.getHostName();
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
