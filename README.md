# Workhorse 

> Extendable Java CDI Job Engine for background jobs and business critical tasks


## Table of Contents
<img align="right" height="200px" src="logo.png">

- [Who is this Workhorse?](#who-is-this-workhorse)
- [Install](#install)
- [Getting started](#getting-started)
- [Jobs](#jobs)
  - [On demand jobs](#on-demand-jobs)
  - [Scheduled jobs](#scheduled-jobs)
  - [Batch jobs](#batch-jobs)
  - [Chained jobs](#chained-jobs)
  - [Delayed jobs](#delayed-jobs)
  - [Planned jobs](#planned-jobs)
  - [Priority jobs](#priority-jobs)
- [Features](#features)
  - [Unique in Queue](#unique-in-queue)
  - [Throughput control](#throughput-control)
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

The coodoo Workhorse is a Java CDI Job Engine for mostly all kind of tasks and background jobs as it is a combination of task scheduler and an event system. It can help you to send out thousands of e-mails or perform long running imports.

Just fire jobs on demand when ever from where ever in your code and Workhorse will take care of it. You can also define an interval or specific time the job has to be started by using the cron syntax. There are also many options like prioritizing, delaying, chaining, multithreading, uniquifying and retrying the jobs. 


## Install

Add the following dependency to your project ([published on Maven Central](https://search.maven.org/artifact/io.coodoo/workhorse))
   
   ```xml
   <dependency>
       <groupId>io.coodoo</groupId>
       <artifactId>workhorse</artifactId>
       <version>2.0.0-RC1-SNAPSHOT</version>
   </dependency>
   ```
   
Depending on your environment there may be additional steps necessary. Have a look into our example projects: 

- [Quarkus](https://gitlab.coodoo.io/workhorse/workhorse-example-quarkus)
- [Wildfly](https://gitlab.coodoo.io/workhorse/workhorse-example-wildfly)
- [Tomcat](https://gitlab.coodoo.io/workhorse/workhorse-example-tomcat)
- [Java SE](https://gitlab.coodoo.io/workhorse/workhorse-example-java-se)


## Getting started

Lets create a hello world job . Therefore you just need to extend the `Worker` class that provides you the `doWork` method. And this method is where the magic happens!

```java
@Dependent
public class HelloWorldWorker extends Worker {

    private final Logger log = LoggerFactory.getLogger(HelloWorldWorker.class);

    @Override
    public void doWork() {
        log.info("Hello World!");
    }
}
```

Now we are able to inject this `HelloWorldWorker` to a service and trigger an execution. After calling `createExecution` the job gets pushed into the job queue and the job engine will take care from this point.

```java
@Inject
HelloWorldWorker helloWorldWorker;

public void performHelloWorld() {

    helloWorldWorker.createExecution();
}
```

Now you have to start up Workhorse using the method `start()` of the `WorkhorseService` somewhere in your application. There is also a `stop()` method to halt that beast.

```java
@Inject
WorkhorseService workhorseService;

public void init() {
    workhorseService.start();
}
```

Let's add some parameters to this job! You can access the parameters by changing the `Worker` to `WorkerWith` and using a type or an object as parameters.

```java
@Dependent
public class HelloWorldWorker extends WorkerWith<String> {

    private final Logger log = LoggerFactory.getLogger(HelloWorldWorker.class);

    @Override
    public void doWork(String environment) {
        log.info("Hello World to: " + environment);
    }
}
```

We can process the `HelloWorldWorker` on a regular basis, so lets tell this worker to run every night half past three by adding `@InitialJobConfig` annotation. Many other configuration on this worker can initially defined by this annotation, have a [look](src/main/java/io/coodoo/workhorse/core/boundary/annotation/InitialJobConfig.java "@InitialJobConfig")!
In this case we overwrite the method `onSchedule()` witch triggers an execution to add some parameters.

```java
@Dependent
@InitialJobConfig(schedule = "0 30 3 0 0 0", description = "Log nightly")
public class HelloWorldWorker extends WorkerWith<String> {

    private final Logger log = LoggerFactory.getLogger(HelloWorldWorker.class);

    @Override
    public void onSchedule() {
        createExecution("Workhorse");
    }

    @Override
    public void doWork(String environment) {
        log.info("Hello World to: " + environment);
    }
}
```

## Jobs

### On demand jobs
This is the default job, that is executed based on the position in the queue.

You just have to call `createExecution()` on your job worker instance. It returns the `ID` of the resulting execution so you can log or track it afterwards.

```java
@Inject
SomeNiceJobWorker someNiceJobWorker;

public void foo() {
  Long executionId = someNiceJobWorker.createExecution();
  log.info("Some nice job will execute with ID " + executionId);
}
```

### Scheduled jobs
Also known as CRON jobs are recurring jobs that are configured with a cron syntax.

Just provide a schedule in cron-syntax to your job and it will fire off the corresponding execution on that schedule.
To register your scheduled job, use the `@InitialJobConfig` annotation with the `schedule` attribute.

```java
@Dependent
@InitialJobConfig(schedule = "0 */20 * * * *")
public class SomeNiceJobWorker extends Worker {

    @Override
    public void doWork() throws Exception {
        log.info("This log will appear every 20 minutes");
    }
}
```
With the schedule above, an execution of `SomeNiceJobWorker` is created every 20 minutes using the workers callback method `onSchedule()`.

The cron expression above use the time tone defined in `WorkhorseConfig` (default is `UTC`). 
You can update it to correspond it to the time zone you operate in. 

### Batch jobs
A batch job is a group of executions of a Job that is created atomically and considered as a single entity. The batch job is finished if all these belonging executions are finished (of failed). Furthermore the executions within a batch job can also be executed parallel.

To create a batch job, just call the method `createBatchExecutions(List<T> parameterList)`on your worker instance. It takes as argument the list of parameter for which executions have to be created.

Let's take as example a list of users as Excel spreadsheet to load into our database. This spreadsheet has thousand of rows and each row requires a few seconds of processing. 
In this case rather than process this spreadsheet as one normal job, we can break up the Excel spreadsheet into one job per row and get the benefit of parallelism to significantly speed up the data load. 
In the following source code we suppose, we have created a Worker `LoadDataWorker` that takes one row of a spreadsheet as parameter to load the content into our database.

```java
@Inject
LoadDataWorker loadDataWorker;

public void performLoadToDataBase(List<User> rows) {
    Long batchId = loadDataWorker.createBatchExecutions(rows);
    log.info("Will import " + rows.size() + " user with Batch-ID " + batchId);
}
```
Here we have created a batch job, that will create for each row an execution to perform the load of user's data into the database.
The batch job is finished when all executions have been processed successfully. 

Workhorse provides a callback method that is called at the end of the batch job. This method can be overridden to add a custom reaction, if  all executions of the batch job have been processed. Just override the worker's `onFinishedBatch(Long batchId, Long jobExecutionId)` method.

```java
@Dependent
public class LoadDataWorker extends WorkerWith<User> {

    private static Logger log = LoggerFactory.getLogger(LoadDataWorker.class);

    @Override
    public void doWork(User user) throws Exception {
        log.info("Doing some serious loading with user: " + user);
    }

    @Override
    public void onFinishedBatch(Long batchId, Long jobExecutionId) {
        log.info("Completed Batch with ID: " + batchId);
    }
}
```


### Chained jobs
A chained job is a linked list of executions of a single type of job, that are processed one after another.

The first execution of a chained job is linked with an execution named child-execution. This child-execution is an execution, that get processed only if the first one has been successfully processed. The last execution of a chained job is linked with an execution named parent-execution. This parent-execution is the contrapose of an child-execution. All other executions between these two have both, a parent- and a child-execution. 

The relation between this members is: An child execution can only be processed, if the process of the parent execution was successful.

To create a chained-Job, you just have to call the method `createChainedExecutions(List<T> parametersList)` on a Worker's instance. For each element of this list an execution with the given element as parameter is created to perform the task. 

Workhorse will process the chained job with the order of the elements  specified in this list. It ensure that the element at `position = n` can't be processed before the successfully execution of a job with the element at `position = n+1`.


Let's suppose we have a Betcommunity-application, that allows users to bet on the outcome of football matches. At the end of the season the application has to calculate how many points and rank a user has achieved. A obvious point here is: the global score achieved at the end of a given matchday also depends on the amount of point obtained on the last one. 
In this case rather than sequentially create a job for each matchday we can use the chained job as follow assuming that a Worker `PointcalculationWorker` already exists. This Worker takes informations about a given matchdays as parameter and perform the calculation of points. 

```java
@Inject
PointcalculationWorker pointcalculationWorker;

public void performPointcalculation(List<Matchday> machtdays) {
   
    pointcalculationWorker.createChainedExecutions(machtdays);
}
```

The method `createChainedExecutions(machtdays)` is called to execute this job for the matchdays specified in the list on a specific order. 

As for the batch jobs, Workhorse provides callback methods `onFinishedChain(Long chainId, Long jobExecutionId)` and `onFailedChain(Long chainId, Long executionId)` that can be overwritten in the worker. The parameter `executionId` indicates the last execution processed in this chain.

### Delayed jobs
A delayed job is executed once after a certain time interval.

Call the method  `createDelayedJobExecution(Long delayValue, ChronoUnit delayUnit)` on the worker instance to create an execution, that will be processed after a given delay.

```java
@Inject
BackupWoker backupWoker;

public void performDelayedJob() {
    Long executionId = backupWoker.createDelayedJobExecution(4,  ChronoUnit.HOURS);
    log.info("Backup starts 4 hours from now with execution-ID " + executionId);
}
```

### Planned jobs
A planned job is executed once at a given moment.

Call the method `createPlannedJobExecution(LocalDateTime maturity)` on the worker instance to create an execution to be processed at a given time in the future.

Let us take as example a backup job, that have to be executed at a given timestamp.
```java
@Inject
BackupWoker backupWoker;

public void performPlannedJob() {
    LocalDateTime maturity = LocalDateTime.of(Year.now().getValue(), Month.OCTOBER, 4, 13, 37);
    Long executionId = backupWoker.createPlannedJobExecution(maturity);
    log.info("Backup starts at " + maturity + " with execution-ID " + executionId);
}
```

### Priority jobs
Executions can be prioritised over other executions of the queue of the corresponding job.

To prioritize an execution just call the method `createPriorityExecution()` instead of `createExecution()` on the instance of your Worker.

Let's take the example of the Worker `SendEmailWorker`. This worker sends an e-mail to the user specified as parameter. We send an e-mail to an administrator and in order to prioritise this e-mail over others, a prioritised execution is created with the method `createPriorityExecution(EmailData parameters)`.

```java
@Inject
SendEmailWorker sendEmailWorker;

public void sendEmail(EmailData emailData) {
    if(emailData.isAdmin()) {
	    sendEmailWorker.createPriorityExecution(emailData);
	} else {
	    sendEmailWorker.createExecution(emailData);
	}
}
```

## Features

### Unique in Queue
If an execution with some parameters already exists in the queue and a new execution is created with the same parameters, it can be configured whether workhorse accepts or discards the creation of this new execution.


You can configure this feature at the definition of your Worker. Under the annotation `@InitialJobConfig` you can activate or deactive the `Unique in Queue` with the paramater `uniqueInQueue`.


Let's suppose we have a worker `SendEmailWorker`, which job is to send e-mails. In this case we don't want to send the same e-mail to an user two times. To avoid this, we just have to use the feature `uniqueInQueue` at the definition of the Worker `SendEmailWorker`.

```java
@Dependent
@InitialJobConfig(uniqueInQueue = true)
public class SendEmailWorker extends WorkerWith<EmailData> {

    private static Logger log = LoggerFactory.getLogger(SendEmailWorker.class);

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


You can configure this feature at the definition of your Worker. Through the annotation `@InitialJobConfig` you can configure the `Throughput` with the paramater `maxPerMinute`.
`maxPerMinute` allow you to set the limit of executions to processed in a minute.



```java
@Dependent
@InitialJobConfig(maxPerMinute = 1000)
public class MaxPerMinute extends Worker {

    private static Logger log = LoggerFactory.getLogger(MaxPerMinute.class);

    @Override
    public void doWork() throws Exception {

        log.info( "Process a job");
    }
}
```
Here the Worker `ExampleWorker` can't be executed more than `1000 times per minutes`. Workhorse ensures that, on the one hand, all created executions are processed and, on the other hand, the specified limit is adhered to. 

### Execution context

The contextual Information about an execution can be obtained also outside the corresponding worker's class.
If the instructions to process a job aren't written in the worker's class under the `doWork()`-method, the context of the running execution is not lost.


To get the execution context you just have to inject the class `JobContext`. Through this class the running execution and the corresponding job can be retrieved and messages can be added to the execution as logs.


Let's define a Worker `SendEmailWorker`.

```java
@Dependent
@InitialJobConfig(uniqueInQueue = true)
public class SendEmailWorker extends WorkerWith<EmailData> {

    private static Logger log = LoggerFactory.getLogger(SendEmailWorker.class);

    @Inject
    EmailService emailService;

    @Override
    public void doWork(EmailData emailData) throws Exception {

        log.info(" Process an execution with paramter: " + emailData);
        emailService.send(emailData);
    }
}
```
The `doWork(EmailData emailData)`-method of this Worker don't contain the instructions to execute the job. Instead, the method `send(EmailData emailData)` of the class `EmailService` is called to process the job. To keep the context of the current execution in the class `EmailService`, we just have to inject `JobContext`. 

```java
public class EmailService {

    @Inject
    JobContext jobContext;

    public void send() {

        Execution execution = jobContext.getExecution();
        Job job = jobContext.getJob();

        jobContext.logInfo(" Start the processing of the execution " + execution + " of the job " + job);

        // Do send..
    }
}
```
In this example the `Execution` and `Job` are retrieved through the injected instance of `JobContext`.

### Retry on failed
If your job encounters a transient exception, it can be retried automatically after a given delay.


You can configure this feature at the definition of your Worker. Through the annotation `@InitialJobConfig` you can configure the number of retries by setting a value to the parameter `failRetries`. The delay before retrying the job can be setted using the parameter `retryDelay`.


In this example a Worker `GenerateStatisticsWorker` is created to generate statistics data from the database.
```java
@Dependent
@InitialJobConfig(failRetries = 3, retryDelay = 2000)
public class GenerateStatisticsWorker extends Worker {

    private static Logger log = LoggerFactory.getLogger(GenerateStatisticsWorker.class);

    @Override
    public void doWork() throws Exception {

        log.info( "Process a job");
        // Generate statistics
    }
}
```
With the config `failRetries = 3` an execution is retried until three times after failed. With the config `retryDelay = 2000` a delay of `2000 milliseconds` is observed between two executions.

### Logging

An Execution can hold an own log. Workhorse provides methods to add log to an execution during the processing of a job. 

So the logs can be getted afterward.



These logs can be created in the context of the `doWork()` method of any Worker. 

Just call for example the method `logInfo(String message)` to add information's messages or `logError(String message)` to add error's messages.


In this example a Worker `GenerateStatisticsWorker` is created to generate statistics data from the database.

```java
@Dependent
public class GenerateStatisticsWorker extends Worker {

    private static Logger log = LoggerFactory.getLogger(GenerateStatisticsWorker.class);

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
Executions of jobs can throw different types of exceptions. Workhorse provides some mechanism to handle them. Exceptions are automatically logged and trigger callback methods. These callback methods can be overridden, to provide the most suitable reaction depending on the type of job. 


To provide a custom callback method for error handling, you just have to override the method `onFailed(Long executionId)` in your worker's class. 


```java
@Dependent
public class ErrorHandlingWorker extends Worker {

    private static Logger log = LoggerFactory.getLogger(ErrorHandlingWorker.class);

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
If an execution of the Worker `ErrorHandlingWorker` throws an exception, the method `onFailed(Long executionId)` is automatically called by Workhorse.

### Callbacks
Workhorse provides a set of callback methods that are called after certain event. These methods can be overridden to get the most appropriate reaction on a given event.


You just have to override the callback methods in a Worker's class.


In this example a Worker `ImportDataWorker` was created. 
```java
@Dependent
public class ImportDataWorker extends Worker {

    private static Logger log = LoggerFactory.getLogger(ImportDataWorker.class);

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

This Worker override four callback methods:

`onFinished(Long executionId)` after an execution is finished.

`onRetry(Long failedExecutionId, Long retryExecutionId)` after an execution has failed and there will be a retry of the failed execution.

`onFailed(Long executionId)` is called after an execution has failed.

`onAllExecutionsDone()` is called after all executions in the queue are done.

## Configuration

The job engine can be started using the method `start()` of the `WorkhorseService`. This should be done in an startup routine of your application. The method `start()` without the configuration parameters uses the default persistence `Memory` and all default configuration values.

```java
@Inject
WorkhorseService workhorseService;

public void init() {
    workhorseService.start();
}
```
The configuration depends on the selected persistence. So every persistence comes with an configuration object which is designed as config builder. All persictens config builders are child classes of [WorkhorseConfigBuilder](src/main/java/io/coodoo/workhorse/core/entity/WorkhorseConfigBuilder.java), that allows you to call a lot of extension methods to customize workhorse for your use.

Let's take the example of the default persisitence `Memory` with it's config builder class [MemoryConfigBuilder](/src/main/java/io/coodoo/workhorse/persistence/memory/MemoryConfigBuilder.java). 
 
 ```java
@Inject
WorkhorseService workhorseService;

public void init() {
    workhorseService.start(new MemoryConfigBuilder().timeZone("Africa/Douala").executionTimeout(30).bufferMaximumSize(1000).build());
}
```
Because of the builder style you can easily add your custom configurations. In this example the timezone is set to Afrifca/Douala, the default timeouts for long running executions are 30 seconds and the buffer maximum size for prebuffering next queued executions is set to 1000.

All configuration settings are saved persistently in the configured persistence. In this example the settings are persistet in memory as long as the apllication runs. Therefore they can also be retrieved and updated at runtime.

 ```java
@Inject
WorkhorseService workhorseService;

public void changeExecutionTimeout() {
  
  WorkhorseConfig newWorkhorseConfig = workhorseService.getWorkhorseConfig;

  newWorkhorseConfig.setExecutionTimeout(25);

  workhorseService.updateWorkhorseConfig(newWorkhorseConfig);

}
```
In the example above the current workhorse configuration are retrieved from the `WorkhorseService`. This configuration object can be used to update attributes at runtime. To really perform this update use the methode 

For more details about all parameters that can be configured, have a look on the class [WorkhorseConfig](src/main/java/io/coodoo/workhorse/core/entity/WorkhorseConfig.java)
## Changelog

All release changes can be viewed on our [changelog](./CHANGELOG.md).

## Maintainers

[coodoo](https://github.com/orgs/coodoo-io/people)

## Contribute

Pull requests and [issues](https://github.com/coodoo-io/workhorse/issues) are welcome.

You can read [how to contribute here](./CONTRIBUTING.md).


### Definition of Done

- There is JavaDoc 



To spice things up



## License

[Apache-2.0 © coodoo GmbH](./LICENSE)

Logo: [Martin Bérubé](http://www.how-to-draw-funny-cartoons.com)