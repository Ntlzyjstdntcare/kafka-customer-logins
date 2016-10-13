Spark application which ingests customer login messages from a Kafka topic of the following format:

{"CustomerId":"00002c28-7599-11e1-8028-1cc1dee5ebd","BrandName":"TestBrand","Timestamp":"2016-05-31T09:06:33.303","IsSuccessful":false}

If three consecutive failure messages are received then a message is sent to another kafka topic

{"CustomerId":"00002c28-7599-11e1-8028-1cc1dee5ebd","EventName":"Failed3ConsecutiveLogins","Timestamp":"2016-05-31T09:06:33.303}
