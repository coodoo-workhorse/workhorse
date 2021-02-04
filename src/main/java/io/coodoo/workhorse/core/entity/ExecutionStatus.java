package io.coodoo.workhorse.core.entity;

public enum ExecutionStatus {

    /**
     * Execution is queued for processing.
     */
    QUEUED,

    /**
     * Execution is currently running, the doWork() Method is called.
     */
    RUNNING,

    /**
     * Execution finished without any error.
     */
    FINISHED,

    /**
     * The Execution failed because of an error.
     */
    FAILED,

    /**
     * The Execution was aborted by an user.
     */
    // TODO chain: nach FAILED alle folgenden auch in FAILED statt ABORTED setzten (mit entsprechende fail message), damit ABORTED exklusiv manuell ist
    ABORTED

    // TODO brainstorm neuer Status TIMEOUT oder vielleicht einfahc auch bei timeout in status FAILED
}
