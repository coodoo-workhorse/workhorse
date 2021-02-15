package io.coodoo.workhorse.core.control;

import io.coodoo.workhorse.core.entity.ExecutionStatus;
import io.coodoo.workhorse.persistence.memory.MemoryPersistence;

/**
 * FOR INTERNAL USAGE ONLY!
 * 
 * The static members get updated by
 * {@link WorkhorseConfigController#initializeStaticConfig()}
 * 
 * @author coodoo GmbH (coodoo.io)
 */
public final class StaticConfig {

    private StaticConfig() {
    }

    /**
     * Name of the peristence (Default is {@link MemoryPersistence})
     */
    public static String PERSISTENCE_NAME = MemoryPersistence.NAME;

    /**
     * ZoneId Object time zone for LocalDateTime instance creation. Default is UTC
     */
    public static String TIME_ZONE;

    /**
     * Max amount of executions to load into the memory buffer per job
     */
    public static Long BUFFER_MAX;

    /**
     * Min amount of executions in memory buffer before the poller gets to add more
     */
    public static int BUFFER_MIN;

    /**
     * Polling interval in seconds at which the intern buffer is loaded
     */
    public static int BUFFER_POLL_INTERVAL;

    /**
     * Polling interval in seconds at which the intern buffer is loaded, that is
     * used as fallback mechanism when new Executions are pushed by the persistence
     */
    public static int BUFFER_PUSH_FALL_BACK_POLL_INTERVAL;

    /**
     * Duration in seconds after which an EXECUTION in status
     * {@link ExecutionStatus#RUNNING} is consider as expired.(if set to 0 the value
     * is ignored)
     */
    public static int EXECUTION_TIMEOUT;

    /**
     * 
     * If an execution is stuck in status {@link ExecutionStatus#RUNNING} and
     * doesn't change for {@link AbstractWorkhorseConfig#executionTimeout} seconds, it is
     * expired!
     * 
     * <code>executionTimeoutStatus</code> defines which status this expired
     * Execution have to get.
     */
    public static ExecutionStatus EXECUTION_TIMEOUT_STATUS;

    /**
     * Log change pattern. Placeholder <code>%s</code> for changeParameter,
     * changeOld and changeNew in this order <br>
     * Default is <code>Changed %s from '%s' to '%s'</code>
     */
    public static String LOG_CHANGE;

    /**
     * Execution log timestamp pattern. Default is <code>[HH:mm:ss.SSS]</code>
     */
    public static String LOG_TIME_FORMATTER;

    /**
     * Execution log info marker. Default is none
     */
    public static String LOG_INFO_MARKER;

    /**
     * Execution log warn marker. Default is <code>[WARN]</code>
     */
    public static String LOG_WARN_MARKER;

    /**
     * Execution log error marker. Default is <code>[ERROR]</code>
     */
    public static String LOG_ERROR_MARKER;

}
