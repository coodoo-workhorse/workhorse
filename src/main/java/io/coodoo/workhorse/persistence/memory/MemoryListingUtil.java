package io.coodoo.workhorse.persistence.memory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import io.coodoo.workhorse.core.control.StaticConfig;

/**
 * @author coodoo GmbH (coodoo.io)
 */
public class MemoryListingUtil {

    public static final String DESC = "-";
    public static final String ASC = "+";
    public static final String GT = ">";
    public static final String LT = "<";

    public static String toIso8601(LocalDateTime timestamp) {
        return timestamp.atZone(ZoneId.of(StaticConfig.TIME_ZONE)).toString();
    }

    public static LocalDateTime fromIso8601(String timestamp) {
        return ZonedDateTime.parse(timestamp).toLocalDateTime();
    }
}
