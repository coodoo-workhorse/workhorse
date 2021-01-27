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
  - [Chained-Jos](#chained-jos)
  - [Delayed-Jobs](#delayed-jobs)
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

Lets create a backup job. Therefore you just need to extend the `Worker` class that provides you the `doWork` method. And this method is where the magic happens!

```java
@Stateless
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
@Stateless
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
@Stateless
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


## Features

_TODO_


### On-Demand-Jobs
This is the default job, that is executed based on the position in the queue.

#### How to use
You just have to call the function `createExecution()` on your worker instance. It returns the ID of the resulting execution so you can log or track it afterwards.

#### Example
```java
@Inject
ExampleWorker exampleWorker

public void performExampleWorker {

  Long executionId = exampleWorker.createExecution();
}
```

#### See also
There is lots of options available like a [delay](#delayed-jobs), priority or parameters.


### Scheduled-Jobs
Also known as CRON-Jobs, they are recurring jobs that are configured with a cron syntax.

#### How to use
You just have to register your jobs with a schedule as cron-syntax during Worker's definition. Workhorse will fire off the corresponding execution on that schedule.

To register your Scheduled-job, just enter your `schedule` with the annotation `@InitialJobConfig`.

#### Example
```java
@Dependent
@InitialJobConfig(schedule = "30 * * * * *")
public class ExampleWorker extends Worker {

    private static Logger log = Logger.getLogger(ExampleWorker.class);

    @Override
    public void doWork() throws Exception {

        log.info(" Process a scheduled job");
    }
}
```
With the schedule above an execution of a job `ExampleWorker` is created at second 30 of every minute.

#### Time Zones
The cron expressions above use the Time Zone defined in `WorkhorseConfig` [look](link to WorkhorseConfig)!. The default Time Zone is for example `UTC`. 
You can update it to correspond it to your Local Time. 

### Batch-Jobs
A Batch-Job is a group of executions of a single Worker that is created atomically and considered as a single entity. The Batch-Job is finished if all these executions are finished. Futhermore the executions within a Batch-Job can be executed parallel .

#### How to use
To create a Batch-Job, just call the function `createBatchExecutions(List<T> parameterList)`on your worker instance. It takes as argument the list of parameter for which Executions have to be created.

#### Example
Let us take as example a list of users as Excel spreadsheet to load into our database. This spreadsheet has thousand of rows and each row requires a few seconds of processing. 
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
    }

    @Override
    public void onFinishedBatch(Long batchId, Long jobExecutionId) {
    
        log.info(" End of the Batch-Job with Id: " + batchId);
    }
}
```


### Chained-Jobs
A Chained-Job is a list of executions of a single type of job, that are two by two linked.

The first execution of a Chained-Job has a child execution, the last execution of a Chained-Job has a parent execution and
all other executions between these two have both, a parent and a child execution. 

The relation between this members is: An child execution can only be processed, if the process of the parent execution was successful.

#### How to use
To create a chained-Job, you just have to call the function `createChainedExecutions(List<T> parametersList)` on an instance of a Worker with the list of parameter.

For each element of this list an execution with the given element as parameter will be created to perform the task. 

Workhorse will process the Chained-Job with the order of the elements  specified in the list. It assure that the element at `position = n` can't be process before an successful execution of the element at `position = n+1`.

#### Example
Let us suppose we have a Betcommunity application, that allows users to bet on the outcome of football matches. At the end of the season the application has to calcute how many points a lamda user has achieved. A obvious point here is: the global score achieved at the end of a given matchday also depends on the amount of point obtained on the last one. 

In this case rather than sequentially create a job for each matchday we can use the Chained-Job as follow.

```java
@Inject
PointcalculationWorker pointcalculationWorker;

public void performPointcalculation(User user, List<Matchday> machtdays) {
   
    pointcalculationWorker.createChainedExecutions(machtdays);
}
```

In this Example we suppose that a Worker `PointcalculationWorker` already exists. This Worker takes information of a given matchdays as parameters and perform the calculation of points. 

Here the method `createChainedExecutions(machtdays)` is called to execute this job for the matchdays specidied in the list on a specific order. 

### Delayed-Jobs
A delayed job is executed only once either after a certain time interval or at a given timestamp.

#### How to use
The time is specified when a new  execution is created. 

You use `createDelayedJobExecution(Long delayValue, ChronoUnit delayUnit)` to create an execution, to be processed after a given delay as `delayValue`.

You use `createPlannedJobExecution(LocalDateTime maturity)` to create an execution to be processed at a given timstamp as `maturity`.

#### Example
```java
@Inject
ExampleJobWoker exampleJobWoker;

public void performDelayedJob() {

    exampleJobWoker.createDelayedJobExecution(4,  ChronoUnit.HOURS);
}

public void performPlannedJob() {

    exampleJobWoker.createPlannedJobExecution(LocalDateTime.of(2021, Month.MAY, 1, 3, 30));
}
```
By calling the function `performDelayedJob()` of the example above, the job `ExampleJobWoker` will  be processed once after four hours.
By Calling the function `performPlannedJob()` will trigger an execution of the job `ExampleJobWoker` on 2021.03.01 at 3:30 hours.


### Unique Jobs in Queue
If a job already exists as queued with the same parameters as the new job it can be configured whether the engine accepts this new same job or discards it.

#### How to use
You can configure this feature at the creation of your Worker. Under the annotation `@InitialJobConfig` you can activate or disactive the Unique Jobs in Queue with the paramater `uniqueInQueue`.

#### Example
```java
@Dependent
@InitialJobConfig(uniqueInQueue = true)
public class ExampleWorker extends WorkerWith<String> {

    private static Logger log = Logger.getLogger(ExampleWorker.class);

    @Override
    public void doWork(String parameter) throws Exception {

        log.info(" Process a job with paramter: " + parameter);
    }
}
```
In this example the argument uniqueInQueue is set to `true`. That means two Executions with the same parameter can't be created.

### Throughput control
The throughput of Executions of a type of job can be limited.

#### How to use
You can configure this feature at the creation of your Worker. Through the annotation `@InitialJobConfig` you can configure the Throughput with the field `maxPerMinute`.
You can choose how many executions of a given job you allow per minutes.
#### Example

```java
@Dependent
@InitialJobConfig(maxPerMinute = 1000)
public class ExampleWorker extends Worker {

    private static Logger log = Logger.getLogger(ExampleWorker.class);

    @Override
    public void doWork() throws Exception {

        log.info( "Process a job");
    }
}
```
Here the job `ExampleWorker` can't be executed more than `1000 times per minutes`. Workhorse ensures that, on the one hand, all created jobs are processed and, on the other hand, the specified limit is adhered to. 

### Multi Queues
Each Job has its own queue (also priority queue).

#### How to use

#### Example

### Retry on failed Jobs
If your job encounters a problem during its execution, it can be retried automatically after a given delay.

#### How to use
You can configure this feature at the creation of your Worker. Through the annotation `@InitialJobConfig` You can configure the number of retries by setting a value to the parameter `failRetries`. The delay before retrying the job can be set using the parameter `retryDelay`.

#### Example

```java
@Dependent
@InitialJobConfig(failRetries = 3, retryDelay = 2000)
public class ExampleWorker extends Worker {

    private static Logger log = Logger.getLogger(ExampleWorker.class);

    @Override
    public void doWork() throws Exception {

        log.info( "Process a job");
    }
}
```
Here an execution of type ExampleWorker can be retried until 3 times after failed. Between 2 executions a delay of `2000 milliseconds` is observed.

### Logging

An Execution can hold an own log. Workhorse provides functions to add log to an execution during the processing of a job. 

So the logs can be getted afterward.

#### How to use

These logs can be created in the context of the `doWork()` method of any Worker. 

Just call for example the function `logInfo(String message)` to add information's message or `logWarn(String message)` to add warning's message.

#### Example

```java
@Dependent
public class ExampleWorker extends Worker {

    private static Logger log = Logger.getLogger(ExampleWorker.class);

    @Override
    public void doWork() throws Exception {

        logInfo("Begin of the job");
        
        

        logInfo("End of the job");
    }
}
```
In this example the messages `Begin of the job` and `End of the job` are included in all executions of the job `ExampleWorker`.

### Error Handling/Callbacks
Executions of jobs can throw different types of exceptions. 

Workhorse provides some mechanism to handle them.

Exceptions are automatically logged  and  trigger callback functions.

These callback functions can be overridden, to provide the most suitable reaction depending on the type of job. 

#### How to use

To provide a custom callback function after an execution has failed, you just have to override the function `onFailed(Long executionId)` in your worker's class. 
The parameter `executionId` correspond to the `ID` of the affected execution.
#### Example

```java
@Dependent
public class ExampleWorker extends Worker {

    private static Logger log = Logger.getLogger(ExampleWorker.class);

    @Override
    public void doWork() throws Exception {

        logInfo("Begin of the job");
        
        logInfo("End of the job");
    }

    @Override
    public void onFailed(Long executionId) {

        log.info("The execution " + executionId + " of the worker ExampleWorker throws an exception");
        // Do some stuff
    }
}
```
If an execution of the Worker `ExampleWorker` throws an exception, the function `onFailed(Long executionId)` will automatically be called by Workhorse.
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

