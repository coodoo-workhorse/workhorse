package io.coodoo.workhorse.core.boundary;

import io.coodoo.workhorse.core.entity.ExecutionStatus;

public final class Config {

    private Config() {
    }

    /**
     * ZoneId Object time zone for LocalDateTime instance creation. Default is UTC
     */
    public static String TIME_ZONE = "UTC";

    /**
     * Max amount of executions to load into the memory queue per job
     */
    public static Long BUFFER_MAX = 1000L;

    /**
     * Min amount of executions in memory queue before the poller gets to add more
     */
    public static int BUFFER_MIN = 1;

    /**
     * Job queue poller interval in seconds
     */
    public static int BUFFER_POLL_INTERVAL = 5;

    /**
     * TODO
     */
    public static int BUFFER_PUSH_FALL_BACK_POLL_INTERVAL = 120;

    /**
     * Name of the peristence
     */
    public static String PERSISTENCE_NAME = "MEMORY";

    /**
     * TODO implement me!
     * 
     * A zombie is an execution that is stuck in status
     * {@link ExecutionStatus#RUNNING} for this amount of minutes (if set to 0 there
     * the hunt is off)
     */
    public static int EXECUTION_TIMEOUT = 120;

    /**
     * TODO implement me!
     * 
     * If an execution is stuck in status {@link ExecutionStatus#RUNNING} and
     * doesn't change, it has became a zombie! Once found we have a cure!
     */
    public static ExecutionStatus EXECUTION_TIMEOUT_STATUS = ExecutionStatus.ABORTED;

    /**
     * Log change pattern. Placeholder <code>%s</code> for changeParameter,
     * changeOld and changeNew in this order <br>
     * Default is <code>Changed %s from '%s' to '%s'</code>
     */
    public static String LOG_CHANGE = "%s changed from '%s' to '%s'";

    /**
     * Execution log timestamp pattern. Default is <code>[HH:mm:ss.SSS]</code>
     */
    public static String LOG_TIME_FORMATTER = "'['HH:mm:ss.SSS']'";

    /**
     * Execution log info marker. Default is none
     */
    public static String LOG_INFO_MARKER = "";

    /**
     * Execution log warn marker. Default is <code>[WARN]</code>
     */
    public static String LOG_WARN_MARKER = "[WARN]";

    /**
     * Execution log error marker. Default is <code>[ERROR]</code>
     */
    public static String LOG_ERROR_MARKER = "[ERROR]";

}
