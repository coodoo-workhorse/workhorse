package io.coodoo.workhorse.storage.memoryAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.coodoo.workhorse.jobengine.entity.Job;
import io.coodoo.workhorse.jobengine.entity.JobStatus;
import io.coodoo.workhorse.storage.persistenceInterface.JobPersistence;
import io.coodoo.workhorse.storage.persistenceInterface.PersistenceTyp;

@ApplicationScoped
public class MemoryJobDAO implements JobPersistence{

    @Inject
    Memory memoryService;

    private AtomicLong incId = new AtomicLong(0);

    @Override
    public Job get(Long id) {

        return memoryService.getJobs().get(id);
    }

    @Override
    public Job getByWorkerClassName(String jobClassName) {
      for (Job job : memoryService.getJobs().values()) {
          if (job.getWorkerClassName().equals(jobClassName)) {
              return job;
          }
      }
      return null;
    }

    @Override
    public void persist(Job job) {
        Long id = incId.getAndIncrement();
        job.setId(id);
        memoryService.getJobs().put(id ,job);

    }

    @Override
    public void update(Long id, Job job) {
        memoryService.getJobs().put(id, job);
    }

    @Override
    public List<Job> getAll() {
        List<Job> result = new ArrayList<>();
        result.addAll(memoryService.getJobs().values());
        return result;
    }

    @Override
    public Long count() {
        return Long.valueOf( memoryService.getJobs().size());
    }

    @Override
    public PersistenceTyp getPersistenceTyp() {  
        return PersistenceTyp.MEMORY ;
    }

    
	@Override
	public Job getByName(String jobName) {
        for (Job job : memoryService.getJobs().values()) {  
            if (Objects.equals(job.getName(), jobName)) {
                return job;
            }
        }
        return null;
	}
    
	@Override
	public List<Job> getAllByStatus(JobStatus jobStatus) {
        List<Job> result = new ArrayList<>();
        
        for (Job job : memoryService.getJobs().values()) {  
            if (Objects.equals(job.getStatus(),  jobStatus)) {
                result.add(job);
            }
        }
        
        return result;
	}
    
	@Override
	public List<Job> getAllScheduled() {
        List<Job> result = new ArrayList<>();
        
        for (Job job : memoryService.getJobs().values()) {  
            if (job.getSchedule() != null && !job.getSchedule().isEmpty()) {
                result.add(job);
            }
        }
        
        return result;
	}
    
	@Override
	public Long countByStatus(JobStatus jobStatus) {
        return Long.valueOf( getAllByStatus(jobStatus).size());
    }
    
    @Override
    public void connect(Object... params) {
        // TODO Auto-generated method stub
    
    }
}
