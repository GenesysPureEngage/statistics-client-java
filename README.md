# Statistics Client Library

The Statistics Client Library is a Java wrapper for the [Statistics API](https://developer.genesyscloud.com/api/reference/statistics/) that makes it easier to code against the API. The library provides much of the supporting code needed to make HTTP requests, process HTTP responses, and enable [CometD](https://cometd.org/) messaging.

The library is hosted on [GitHub](https://github.com/GenesysPureEngage/statistics-client-java) and Genesys welcomes pull requests for corrections.

## Install

Genesys recommends that you install the Statistics Client Library JAR file with [Gradle](https://gradle.org/). You should use latest version available in the Maven [repository](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.genesys%22%20AND%20a%3A%22statistics%22).

Add the following line to the **dependencies** block in your **build.gradle** file:

~~~gradle
compile 'com.genesys:statistics:<latest_version>'
~~~

## Related Links

* Follow along with the [Work with subscriptions](https://developer.genesyscloud.com/tutorials/subscriptions-statistics-java/#java) tutorial to see how to use this library.
* Learn more about the [Statistics API](https://developer.genesyscloud.com/api/reference/statistics/).
* Learn more about the [Statistics Client Library](https://developer.genesyscloud.com/api/client-libraries/statistics/).

## Classes

The Statistics Client Library includes the [Statistics class](https://developer.genesyscloud.com/client-libraries/statistics/java/Statistics/index.html), which contains all the resources and events that are part of the Statistics API, along with all the methods you need to access the API functionality.

## Examples

For usage examples for each method available in the library, see the documentation for the Statistics class. You can also check out the [Work with subscriptions](https://developer.genesyscloud.com/tutorials/subscriptions-statistics-java/#java) tutorial.