package io.coodoo.workhorse.storage.persistenceInterface;

import java.util.List;

import io.coodoo.workhorse.jobengine.entity.Job;
import io.coodoo.workhorse.jobengine.entity.JobStatus;

public interface JobPersistence {
     
     /**
      * Get a job by his Id
      * @param id
      * @return
      */
     Job get(Long id);

     /**
      * Get a Job by his Name
      * @param jobName
      * @return
      */
     Job getByName(String jobName);
     
     /**
      * Get a job by the JobWorker classname
      * @param jobClassName
      * @return
      */
     Job getByWorkerClassName(String jobClassName);

     /**
      * Get all jobs in storage
      * @return List of all Jobs
      */
     List<Job> getAll();

     /**
      * Get all jobs filtered by a given status
      @param jobStatus
      @return List of Job filtered by status
      */
     List<Job> getAllByStatus(JobStatus jobStatus);

     /**
      * Get All scheduled Job
      * @return List of scheduled Job
      */
     List<Job> getAllScheduled();

     /**
      * Get the number of Job in storage
      * @return Number of Job
      */
     Long count();

     /**
      * Get the number of Jobs in storage filtered by given Status
      * @param jobStatus
      * @return Number of Job by Status
      */
     Long countByStatus(JobStatus jobStatus);

     /**
      * persist a Job in storage. 
      * The id of the job have to be set by the persistence.
      * @param job
      */
     void persist(Job job);
    
     /**
      * Update the job given by Id
      * @param id
      * @param job
      */
     void update(Long id, Job job); 

     /**
     * rollback fonction to initialize the connection with the persistence
     */
     void connect(Object... params);
 
     /**
         * 
         * @return retrieve the type of the persistence to initialize
         */
     PersistenceTyp getPersistenceTyp();
    
    
}
