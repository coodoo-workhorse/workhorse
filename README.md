[logo]: https://gitlab.coodoo.io/workhorse/workhorse/-/raw/master/logo.png "Workhorse: Extendable Java Job Engine for background jobs and business critical tasks"

# Workhorse ![alt text][logo]

> Extendable Java Job Engine for background jobs and business critical tasks

## Table of Contents

- [Who is this Workhorse?](#who-is-this-workhorse)
- [Install](#install)
- [Getting started](#getting-started)
- [Features](#features)
  - [On-Demand](#on-demand-direct-execution)
  - [Scheduled-Jobs](#scheduled-jobs)
  - [Batch-Jobs](#batch-jobs)
  - [Delayed-Jobs](#delayed-jobs)
  - [Chained-Jos](#chained-jos)
  - [Unique Jobs in Queue](#unique-jobs-in-queue)
  - [Throughput control](#throughput-control)
  - [Multi Queues](#multi-queues)
  - [Retry on failed Jobs](#retry-on-failed-jobs)
  - [Logging](#logging)
  - [Error Handling/Callbacks](#error-handling-callbacks)
- [Configuration](#configuration)
- [Maintainers](#maintainers)
- [Changelog](#changelog)
- [Contribute](#contribute)
- [License](#license)


## Who is this Workhorse?

The coodoo Workhorse is a Java Job Engine for mostly all kind of tasks and background jobs as it is a combination of task scheduler and an event system. It can help you to send out thousands of e-mails or perform long running imports.

Just fire jobs on demand when ever from where ever in your code and Workhorse will take care of it. You can also define an interval or specific time the job has to be started by using the cron syntax. There are also many options like prioritizing, delaying, chaining, multithreading, uniquifying and retrying the jobs. 


## Install

Add the following dependency to your project ([published on Maven Central](http://search.maven.org/#artifactdetails%7Cio.coodoo%7Cworkhorse%7C2.0.0%7Cjar))
   
   ```xml
   <dependency>
       <groupId>io.coodoo</groupId>
       <artifactId>workhorse</artifactId>
       <version>2.0.0</version>
   </dependency>
   ```
   
Depending on your environment there may be additional steps necessary. Have a look into our example projects: 

- [Java SE](https://gitlab.coodoo.io/workhorse/workhorse-example-java-se)
- [Tomcat](https://gitlab.coodoo.io/workhorse/workhorse-example-tomcat)
- [Wildfly](https://gitlab.coodoo.io/workhorse/workhorse-example-wildfly)
- [Quarkus](https://gitlab.coodoo.io/workhorse/workhorse-example-quarkus)


## Getting started

Lets create a backup job. Therefore you just need to extend the `JobWorker` class that provides you the `doWork` method. And this method is where the magic happens!

```java
@Stateless
public class BackupJob extends JobWorker {

    private final Logger log = LoggerFactory.getLogger(BackupJob.class);

    @Override
    public void doWork() {

        log.info("Performing some fine backup!");
    }
}
```

Now we are able to inject this backup job to a service and trigger a job execution. After calling `createJobExecution` the job gets pushed into the job queue and the job engine will take care from this point.

```java
@Inject
BackupJob backupJob;

public void performBackup() {

    backupJob.createJobExecution();
}
```

Lets add some parameters to this job! Therefore we need just a POJO with the wanted attributes.
The service can pass the parameters object to the `createJobExecution` method.

```java
@Inject
BackupJob backupJob;

public void performBackup() {

    BackupJobParameters parameters = new BackupJobParameters();
    parameters.setEvironment("STAGE-2");
    parameters.setReplaceOldBackup(false);

    backupJob.createJobExecution(parameters);
}
```

You can access the parameters by changing the `JobWorker` to `JobWorkerWith` and using the parameters object as type.

```java
@Stateless
public class BackupJob extends JobWorkerWith<String> {

    private final Logger log = LoggerFactory.getLogger(BackupJob.class);

    @Override
    public void doWork(String parameters) {

        log.info("Performing some fine backup on " + parameters);
    }
}
```

Everybody knows backups should be made on a regular basis, so lets tell this job to run every night half past three by initially adding `@InitialJobConfig` annotation. Many other job configuration can initially defined by this annotation, have a [look](https://github.com/coodoo-io/workhorse/blob/master/src/main/java/io/coodoo/workhorse/jobengine/boundary/annotation/InitialJobConfig.java "@InitialJobConfig")!
In this case we overwrite the method `onSchedule()` witch triggers the job to add some parameters.

```java
@Stateless
@InitialJobConfig(schedule = "0 30 3 0 0 0")
public class BackupJob extends JobWorkerWith<String> {

    private final Logger log = LoggerFactory.getLogger(BackupJob.class);

    @Override
    public void onSchedule() {

        createExecution("STAGE-2");
    }

    @Override
    public void doWork(String parameters) {

        log.info("Performing some fine backup on " + parameters);
    }
}
```

Doesn't work? That is because you have to start the jobEngine using the method `start()` of the `JobEngineService` somewhere in your application. It takes the job queue polling interval in seconds as a parameter and there is also a `stop()` method to halt the job engine.

```java
@Inject
WorkhorseService workhorseService;

public void start() {

    workhorseService.start();
}
```


## Features

_TODO_


### On-Demand / Direct execution
Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum nec elit sapien. Sed varius augue finibus ex hendrerit, aliquam porta erat pulvinar. Fusce egestas sem ac accumsan vulputate. Proin suscipit in sem at tempus. Nunc nec nunc tellus. Sed at libero sed nunc ornare porttitor a ut orci. Integer iaculis mauris sed condimentum imperdiet. Maecenas maximus ultrices ipsum pharetra posuere. In posuere mauris at diam tempor, quis tristique velit placerat. Morbi sodales risus eu condimentum tempor. Nam vel sem a metus fermentum consectetur ac et nisl.

#### How to use
This is the default for most of the fire-and-forget task you have in mind. Just call `createExecution()` on your worker instance. It returns the ID of the resulting execution so you can log or track it afterwards.

#### Example
```java
someWorker.createExecution();
```

#### See also
There is lots of options available like a [delay](#delayed-jobs), priority or parameters.


### Scheduled-Jobs
Also known as CRON-Jobs. Start job executions with CRON syntax.

### Batch-Jobs
A batch-job is a group of background jobs executions that is created atomically and considered as a single entity. The Batch Job is finished if all job executions are finished. The job executions within a batch can be parallel executed.

### Delayed-Jobs
A delayed job is executed only once after a certain time interval. So not immediately.

### Chained-Jos
Start next Job Exection only if the previous was finished succesfully.

### Unique Jobs in Queue
If a job already exists as queued with the same parameters as the new job it can be configured whether the engine accepts this new same job or discards it.

### Throughput control
If needed the throughput of Job Executions can be limited JobContext create log for Execution

### Multi Queues
Each Job has its own queue (also priority queue)

### Retry on failed Jobs
Failed Job Execution can automaticlly get retried.

### Logging
A Job Execution can hold an own log

### Error Handling/Callbacks
Exceptions get logged trigger callback functions.


## Configuration

_TODO_


## Changelog

All release changes can be viewed on our [changelog](./CHANGELOG.md).

## Maintainers

[coodoo](https://github.com/orgs/coodoo-io/people)

## Contribute

Pull requests and [issues](https://github.com/coodoo-io/workhorse/issues) are welcome.

## License

[Apache-2.0 © coodoo GmbH](./LICENSE)

Logo: [Martin Bérubé](http://www.how-to-draw-funny-cartoons.com)

