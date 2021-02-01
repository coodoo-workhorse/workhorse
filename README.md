[logo]: https://gitlab.coodoo.io/workhorse/workhorse/-/raw/master/logo.png "Workhorse: Extendable Java Job Engine for background jobs and business critical tasks"

# Workhorse ![alt text][logo]

> Extendable Java Job Engine for background jobs and business critical tasks

## Table of Contents

- [Who is this Workhorse?](#who-is-this-workhorse)
- [Install](#install)
- [Getting started](#getting-started)
- [Jobs](#jobs)
  - [On-Demand jobs](#on-demand-jobs)
  - [Scheduled jobs](#scheduled-jobs)
  - [Batch jobs](#batch-jobs)
  - [Chained jobs](#chained-jobs)
  - [Delayed jobs](#delayed-jobs)
  - [Planned jobs](#planned-jobs)
  - [Priority jobs](#priority-jobs)
- [Features](#features)
  - [Unique in Queue](#unique-in-queue)
  - [Throughput control](#throughput-control)
  - [Multi Queues](#multi-queues)
  - [Execution context](#execution-context)
  - [Retry on failed](#retry-on-failed)
  - [Logging](#logging)
  - [Error handling](#error-handling)
  - [Callbacks](#callbacks)
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

Lets create a backup job. Therefore you just need to extend the `Worker` class that provides you the `doWork` method. And this method is where the magic happens!

```java
@Dependent
public class BackupJob extends Worker {

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

You can access the parameters by changing the `Worker` to `WorkerWith` and using the parameters object as type.

```java
@Dependent
public class BackupJob extends WorkerWith<String> {

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
@Dependent
@InitialJobConfig(schedule = "0 30 3 0 0 0")
public class BackupJob extends WorkerWith<String> {

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

## Jobs

### On-Demand jobs
This is the default job, that is executed based on the position in the queue.

#### How to use
You just have to call the function `createExecution()` on your worker instance. It returns the ID of the resulting execution so you can log or track it afterwards.

#### Example
```java
@Inject
OnDemandWorker onDemandWorker

public void performOnDemandWorker {

  Long executionId = onDemandWorker.createExecution();
}
```

### Scheduled jobs
Also known as CRON-Jobs, they are recurring jobs that are configured with a cron syntax.

#### How to use
You just have to register your jobs with a schedule as cron-syntax during Worker's definition. Workhorse will fire off the corresponding execution on that schedule.

To register your Scheduled-job, just enter your `schedule` with the annotation `@InitialJobConfig`.

#### Example
```java
@Dependent
@InitialJobConfig(schedule = "30 * * * * *")
public class ScheduledWorker extends Worker {

    private static Logger log = Logger.getLogger(ScheduledWorker.class);

    @Override
    public void doWork() throws Exception {

        log.info(" Process a scheduled job");
    }
}
```
With the schedule above, an execution of a job `ScheduledWorker` is created at second 30 of every minute.

#### Time Zones
The cron expression above use the Time Zone defined in `WorkhorseConfig`. The default Time Zone is for example `UTC`. 
You can update it to correspond it to your Local Time. 

### Batch jobs
A Batch-Job is a group of executions of a single Worker that is created atomically and considered as a single entity. The Batch-Job is finished if all these executions are finished. Futhermore the executions within a Batch-Job can be executed parallel .

#### How to use
To create a Batch-Job, just call the function `createBatchExecutions(List<T> parameterList)`on your worker instance. It takes as argument the list of parameter for which Executions have to be created.

#### Example
Let's take as example a list of users as Excel spreadsheet to load into our database. This spreadsheet has thousand of rows and each row requires a few seconds of processing. 
In this case rather than process this spreadsheet as one normal job, we can break up the Excel spreadsheet into one job per row and get the benefit of parallelism offer by `Batch-Jobs` to significantly speed up the data load. 
In the following source code we suppose, we have created a Worker `LoadDataWorker` that takes one row of a spreadsheet as parameter to load the content into our database.

```java
@Inject
LoadDataWorker loadDataWorker;

public void performLoadToDataBase(List<User> rows) {

    loadDataWorker.createBatchExecutions(rows);
}
```
Here we have created a Batch-Job, that will create for each row an execution to perform the load of user's data into the database.
The Batch-Job is finished when all executions have been processed successfully. 

Futhermore Workhorse provides a callback Function that is called at the end of the Batch-Job. This function can be overridden to add a custom reaction, if  all executions of the Batch-Job have been processed. Just override in the worker's class the function `onFinishedBatch(Long batchId, Long jobExecutionId)`.
The following source code shows an example of usage of this function.

```java
@Dependent
public class LoadDataWorker extends WorkerWith<User> {

    private static Logger log = Logger.getLogger(LoadDataWorker.class);

    @Override
    public void doWork(User user) throws Exception {

        log.info(" Process a job with paramter: " + user);

        // Process of the job ...
    }

    @Override
    public void onFinishedBatch(Long batchId, Long jobExecutionId) {
    
        log.info(" End of the Batch-Job with Id: " + batchId);
    }
}
```


### Chained jobs
A Chained-Job is a list of executions of a single type of job, that are two by two linked.

The first execution of a Chained-Job is linked with an execution named child-execution. This child-execution is an execution, that get processed only if the first one has been successfully processed. The last execution of a Chained-Job is linked with an execution named parent-execution. This parent-execution is the contrapose of an child-execution. All other executions between these two have both, a parent- and a child-execution. 

The relation between this members is: An child execution can only be processed, if the process of the parent execution was successful.

#### How to use
To create a chained-Job, you just have to call the function `createChainedExecutions(List<T> parametersList)` on a Worker's instance. 

For each element of this list an execution with the given element as parameter is created to perform the task. 

Workhorse will process the Chained-Job with the order of the elements  specified in this list. It ensure that the element at `position = n` can't be processed before the successfully execution of a job with the element at `position = n+1`.

#### Example
Let's suppose we have a Betcommunity-application, that allows users to bet on the outcome of football matches. At the end of the season the application has to calcute how many points a lamda user has achieved. A obvious point here is: the global score achieved at the end of a given matchday also depends on the amount of point obtained on the last one. 
In this case rather than sequentially create a job for each matchday we can use the Chained-Job as follow.

In the following example we suppose that a Worker `PointcalculationWorker` already exists. This Worker takes informations about a given matchdays as parameter and perform the calculation of points. 
```java
@Inject
PointcalculationWorker pointcalculationWorker;

public void performPointcalculation(User user, List<Matchday> machtdays) {
   
    pointcalculationWorker.createChainedExecutions(machtdays);
}
```

Here the method `createChainedExecutions(machtdays)` is called to execute this job for the matchdays specidied in the list on a specific order. 

### Delayed jobs
A delayed job is a job that is executed only once after a certain time interval.

#### How to use
The time interval is specified when an new execution is created. 

You just have to call the function  `createDelayedJobExecution(Long delayValue, ChronoUnit delayUnit)` on the worker instance to create an execution, to be processed after a given delay as `delayValue`.

#### Example
Let's take as example a backup job, that haven't to be executed direct after calling.

```java
@Inject
BackupWoker backupWoker;

public void performDelayedJob() {

    backupWoker.createDelayedJobExecution(4,  ChronoUnit.HOURS);
}

```
By calling the function `performDelayedJob()`, an execution of the Worker `BackupWoker` is processed once after four hours.

### Planned jobs
A planned job is a job that is executed only once at a given timestamp.

#### How to use
You just have to call the function `createPlannedJobExecution(LocalDateTime maturity)` on the worker instance to create an execution to be processed at a given timstamp as `maturity`.

#### Example
Let us take as example a backup job, that have to be executed at a given timestamp.
```java
@Inject
BackupWoker backupWoker;

public void performPlannedJob() {

    backupWoker.createPlannedJobExecution(LocalDateTime.of(2021, Month.MAY, 1, 3, 30));
}
```

By Calling the function `performPlannedJob()`, an execution of the Worker `BackupWoker` is processed on 2021.03.01 at 3:30 hours.

### Priority jobs
An execution can be prioritized over other executions of the queue of the corresponding job.

#### How to use
To prioritize an execution just call the function `createPriorityExecution()` instead of `createExecution()` or the function `createPriorityExecution(T parameters)` instead of `createExecution(T parameters)`  on the instance of your Worker.

#### Example
Let's take the example of the Worker `SendEmailWorker`. This worker send an e-mail to to user specidied as paramater. In this example we created a function `sendEmailToAdmin`, that send an e-mail to an adminitrator. In order to priotirize sending an e-mail to a administrator over other users, an prioritized execution is created with the function `createPriorityExecution(EmailData parameters)`.

```java
@Inject
SendEmailWorker sendEmailWorker;

public void sendEmailToAdmin(EmailData admin) {

    sendEmailWorker.createPriorityExecution(admin);
}
```
## Features

### Unique in Queue
If an execution with some parameters already exists in the queue and a new execution is created with the same parameters, it can be configured whether workhorse accepts or discards the creation of this new execution.

#### How to use
You can configure this feature at the definition of your Worker. Under the annotation `@InitialJobConfig` you can activate or deactive the `Unique in Queue` with the paramater `uniqueInQueue`.

#### Example
Let's suppose we have a worker `SendEmailWorker`, which job is to send e-mails. In this case we don't want to send the same e-mail to an user two times. To avoid this, we just have to use the feature `uniqueInQueue` at the definition of the Worker `SendEmailWorker`.

```java
@Dependent
@InitialJobConfig(uniqueInQueue = true)
public class SendEmailWorker extends WorkerWith<EmailData> {

    private static Logger log = Logger.getLogger(SendEmailWorker.class);

    @Override
    public void doWork(EmailData emailData) throws Exception {

        log.info(" Process an execution with paramter: " + emailData);
        //Send the e-mail ...
    }
}
```
With the argument `uniqueInQueue` setted up to `true`, Workhorse ensure that the same e-mail can't be sent more than once to the same user.
 
### Throughput control
The throughput of executions of a job can be limited.

#### How to use
You can configure this feature at the definition of your Worker. Through the annotation `@InitialJobConfig` you can configure the `Throughput` with the paramater `maxPerMinute`.
`maxPerMinute` allow you to set the limit of executions to processed in a minute.

#### Example

```java
@Dependent
@InitialJobConfig(maxPerMinute = 1000)
public class MaxPerMinute extends Worker {

    private static Logger log = Logger.getLogger(MaxPerMinute.class);

    @Override
    public void doWork() throws Exception {

        log.info( "Process a job");
    }
}
```
Here the Worker `ExampleWorker` can't be executed more than `1000 times per minutes`. Workhorse ensures that, on the one hand, all created executions are processed and, on the other hand, the specified limit is adhered to. 

### Multi Queues
Each Job has its own queue.

#### How to use

#### Example

### Execution context

#### How to use

#### Example

### Retry on failed
If your job encounters a transient exception, it can be retried automatically after a given delay.

#### How to use
You can configure this feature at the definition of your Worker. Through the annotation `@InitialJobConfig` you can configure the number of retries by setting a value to the parameter `failRetries`. The delay before retrying the job can be setted using the parameter `retryDelay`.

#### Example
In this example a Worker `GenerateStatisticsWorker` is created to generate statistics data from the database.
```java
@Dependent
@InitialJobConfig(failRetries = 3, retryDelay = 2000)
public class GenerateStatisticsWorker extends Worker {

    private static Logger log = Logger.getLogger(GenerateStatisticsWorker.class);

    @Override
    public void doWork() throws Exception {

        log.info( "Process a job");
        // Generate statistics
    }
}
```
With the config `failRetries = 3` an execution is retried until three times after failed. With the config `retryDelay = 2000` a delay of `2000 milliseconds` is observed between two executions.

### Logging

An Execution can hold an own log. Workhorse provides functions to add log to an execution during the processing of a job. 

So the logs can be getted afterward.

#### How to use

These logs can be created in the context of the `doWork()` method of any Worker. 

Just call for example the function `logInfo(String message)` to add information's messages or `logError(String message)` to add error's messages.

#### Example
In this example a Worker `GenerateStatisticsWorker` is created to generate statistics data from the database.

```java
@Dependent
public class GenerateStatisticsWorker extends Worker {

    private static Logger log = Logger.getLogger(GenerateStatisticsWorker.class);

    @Override
    public void doWork() throws Exception {
        
        try() {

            logInfo("Begin of the job");
            //Do work.
            logInfo("End of the job");

        } catch (Exception exception) {

            logError("Generate the statistics encounters an exception.");
        }    
    }
}
```
In this example the messages `Begin of the job` and `End of the job` are included in all executions of the job `GenerateStatisticsWorker`. If an execution encounters an exception, the message `Generate the statistics encounters an exception.` is included in the execution.

### Error Handling
Executions of jobs can throw different types of exceptions. Workhorse provides some mechanism to handle them. Exceptions are automatically logged and trigger callback functions. These callback functions can be overridden, to provide the most suitable reaction depending on the type of job. 

#### How to use
To provide a custom callback function for error handling, you just have to override the function `onFailed(Long executionId)` in your worker's class. 
#### Example

```java
@Dependent
public class ErrorHandlingWorker extends Worker {

    private static Logger log = Logger.getLogger(ErrorHandlingWorker.class);

    @Override
    public void doWork() throws Exception {

        logInfo("Begin of the job");
        
        logInfo("End of the job");
    }

    @Override
    public void onFailed(Long executionId) {

        log.info("The execution " + executionId + " of the worker `ErrorHandlingWorker` throws an exception");
        // Do some stuff
    }
}
```
If an execution of the Worker `ErrorHandlingWorker` throws an exception, the function `onFailed(Long executionId)` is automatically called by Workhorse.

### Callbacks
Workhorse provides a set of callback functions that are called after certain event. These functions can be overridden to get the most appropriate reaction on a given event.

#### How to use
You just have to override the callback functions in a Worker's class.

#### Example
In this example a Worker `ImportDataWorker` was created. 
```java
@Dependent
public class ImportDataWorker extends Worker {

    private static Logger log = Logger.getLogger(ImportDataWorker.class);

    @Override
    public void doWork() throws Exception {

        logInfo("Begin of the job");
        // Do work
        logInfo("End of the job");
    }

    @Override
    public void onFinished(Long executionId) {
         log.info("The execution " + executionId + "is finished.");
    }

    @Override
    public void onRetry(Long failedExecutionId, Long retryExecutionId) {
        log.info("The execution " + failedExecutionId + " failed. The execution " + retryExecutionId + "is created to retry.");
    }

    @Override
    public void onFailed(Long executionId) {
        log.info("The execution " + executionId + " throws an exception");
    }

    @Override
    public void onAllExecutionsDone() {
        log.info("All executions of the Worker " + ImportDataWorker.getName() + " are done." );
    }
}
```

This Worker override four callback functions:

`onFinished(Long executionId)` after an execution is finished.

`onRetry(Long failedExecutionId, Long retryExecutionId)` after an execution has failed and there will be a retry of the failed execution.

`onFailed(Long executionId)` is called after an execution has failed.

`onAllExecutionsDone()` is called after all executions in the queue are done.

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

