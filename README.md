Using Kafka 2.11-0.10.0.0.
Using the quick and dirty zookeeper that comes packaged with kafka.
Starting 3-broker kafka cluster with default configs.
Creating topics called CustomerLogins and ConsumerTopic with 4 partitions and replication factor of 3.
Created new spark streaming application using Spark 2.0.0.
Use sbt assembly to build, with sbt version 0.13.12.

I'm assuming that input messages are valid json.
I'm assuming that customers are partitioned by key and that no customers are spread across partitions.

I chose 4 partitions as I ran the application on a single 4-core spark node. It's possible that increasing the number of Kafka partitions would lead to performance improvements, although I didn't carry out any performance testing...

I focused on code readability, succinctness, maintainability, functional-ness, and overall affinity with parallelism. Data accuracy (i.e. that an accurate count is kept of failed logins) was also a priorty although I believe I failed to achieve that, as explained below.

I chose Spark for built-in resilience and scalability, computational speed, and for streaming support. Other options included
Apache Storm and Flume. Kafka Streaming would likely be an excellent option once it is production ready.

The spec says to 'store' the number of failed logins. With the assumption that no other application needs to access that 
number, it seems to me to be wasteful to have to write to disk, and much worse, to have to constantly poll the database to 
check the value of the number. Perhaps there is some tool designed for this task that I'm unaware of...In any case I have 
decided to do everything in-memory and to avoid communicating with any outside services apart from Kafka and s3 (for 
checkpointing).

I have used an accumulator to keep track of the failed logins so as to ensure accuracy in a distributed environment. I have
chosen not to reset the failed logins count to 0, as this is difficult with Accumulators. Instead I check whether it is a 
a multiple of 2 (I only increment the accumulator on the second and third consecutive failure). It seems like tracking a
counter would be easier with the AccumulatorV2 Type which apparently is available with Spark 2.0.0 but I could not figure out how to bring it into the project. I suspect that it is not actually available yet...

I have used vars to keep track of the failure status of the last message in the stream. This is probably not valid for a 
distributed environment (I have built and tested this on my local box only), however it is the best solution that I could 
come up with in the time I had. Achieving the goals of the challenge in Spark is difficult, and perhaps Spark is not the best
tool for the job. Needless to say this task would be much easier in a non-distributed environment, and as a batch job. If we
could extract the RDD of messages into a Scala collection after running a batch job we could then use Scala's sliding 
function. If I had more time, I would look at improving the streaming solution by wrapping both the lastValueFailure var and
the call to foreachRDD on loginAttemptMessageStream within a closure. Then this closure could be sent out to executors and
each executor would be able to calculate consecutive failures on its partition, in isolation from the rest of the cluster.

Data integrity on restart is provided by checkpointing. Checkpointing data is saved to S3, and upon Spark restarting the
application will read the kafka offset from the checkpointing data, and pick up where it left off.

Logs can be monitored by running scripts which query the Spark REST api at http://<domain>:4040/api/v1 (for a running
application). Metrics gathered from the REST endpoints may be surfaced using graphite/grafana, to pick two of the most popular among many possible tools. Although this will not directly surface exceptions, alerts can be distributed by tools such as Seyren whenever performance metrics exceed or fall below certain thresholds. The logs (likely to be found in the work
directory on the relevant servers) can then be manually inspected by developers for exceptions.

The application can be easily deployed to a running Spark cluster in a cloud environment such as AWS EC2 by stopping the
application, sending the jar up to the master, and restarting the application. Of course this can all be automated via the use of bash scripts and a Continuous Deployment tool such as Bamboo or Travis. I particularly like Ansible for how easy it makes the automation of interacting with remote servers. 

The app compiles but doesn't work due to a particular serialization error in the closure which computes the number of
consecutive failures and sends a message to ConsumerTopic. I have chosen to submit an incomplete solution for the following
reasons:

1) Given that the instructions emphasise that the app doesn't even need to compile, I've made the assumption that an
incomplete solution is acceptable. I believe that my code shows that I know my way around Scala and the Spark API. Hopefully my choices and assumptions outlined here demonstrate that I am knowledgeable regarding issues specific to a distributed 
computing environment.

2) Given that I have already spent a day on the solution while the spec specifies that it shouldn't take me more than 4 hours, I decided that spending longer would be contrary to the spirit of the exercise.

3) I am eager to progress the recruitment process, as if all goes well I will offered another role in the next day or so. 
This role at Betsson is by far and away my first choice, and I would like to know if I am successful or not before having to
make a decision on the other role. 

While I have included tests, I have been unable to run them. I'm sure that this is due to some trivial mistake in my project
setup. I have not written exhaustive unit tests due to time constraints. I also would have liked to write an integration test 
but it would have been a difficult task while being unable to run the test! 

All in all, I found this task to be challenging and extremely fun. Thank you for the opportunity, and I hope you enjoy 
evaluating my solution!
