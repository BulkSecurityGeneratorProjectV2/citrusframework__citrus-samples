JDBC callable statements sample ![Logo][1]
==============

This sample uses a JDBC database connection to verify the management of callable statements by the todo application.

Objectives
---------

The [todo-list](../todo-app/README.md) sample application stores data to a relational database. This sample shows 
the usage of callable statements validation actions in Citrus.
See the [reference guide][4] database chapter for details.

The database server and its datasource are configured in the endpoint configuration context ***EndpointConfig.java***.
    
```java
@Bean
public JdbcServer jdbcServer() {
    return CitrusEndpoints
        .jdbc()
            .server()
            .host("localhost")
            .databaseName("testdb")
            .port(13306)
            .timeout(10000L)
            .autoStart(true)
            .autoCreateStatement(false)
        .build();
}

@Bean
public SingleConnectionDataSource dataSource() {
    SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
    dataSource.setDriverClassName(JdbcDriver.class.getName());
    dataSource.setUrl("jdbc:citrus:http://localhost:13306/testdb");
    dataSource.setUsername("sa");
    dataSource.setPassword("");
    return dataSource;
}
```
    
As you can see we are using a citrus database server here which is configured to validate all statement related actions
by setting `.autoCreateStatement(false)`.    

In the test case we can now verify the callable statement behavior of our application if a client request hits our API. 

```java
variable("todoId", "citrus:randomUUID()");
variable("todoName", "citrus:concat('todo_', citrus:randomNumber(4))");
variable("todoDescription", "Description: ${todoName}");

[...]

$(http()
    .client(todoClient)
    .send()
    .get("api/todolist/1")
    .fork(true));

$(receive()
    .endpoint(jdbcServer)
    .message(JdbcMessage.createCallableStatement("{CALL limitedToDoList(?)}")));

$(receive()
    .endpoint(jdbcServer)
    .message(JdbcMessage.execute("{CALL limitedToDoList(?)} - (1)")));

$(send()
    .endpoint(jdbcServer)
    .type(MessageType.JSON)
    .message(JdbcMessage.success().dataSet("[ {" +
            "\"id\": \"${todoId}\"," +
            "\"title\": \"${todoName}\"," +
            "\"description\": \"${todoDescription}\"," +
            "\"done\": \"false\"" +
            "} ]")));

$(receive()
    .endpoint(jdbcServer)
    .message(JdbcMessage.closeStatement()));

$(http()
    .client(todoClient)
    .receive()
    .response(HttpStatus.OK)
    .body("[ {" +
                "\"id\": \"${todoId}\"," +
                "\"title\": \"${todoName}\"," +
                "\"description\": \"${todoDescription}\"," +
                "\"done\": false" +
            "} ]"));
```

Run
---------

**NOTE:** This test depends on the [todo-app](../todo-app/) WAR which must have been installed into your local maven repository using `mvn clean install` beforehand.

The sample application uses Maven as build tool. So you can compile, package and test the
sample with Maven.
 
     mvn clean verify -Dsystem.under.test.mode=embedded
    
This executes the complete Maven build lifecycle. The embedded option automatically starts a Jetty web
container before the integration test phase. The todo-list system under test is automatically deployed in this phase.
After that the Citrus test cases are able to interact with the todo-list application in the integration test phase.

During the build you will see Citrus performing some integration tests.
After the tests are finished the embedded Jetty web container and the todo-list application are automatically stopped.

System under test
---------

The sample uses a small todo list application as system under test. The application is a web application
that you can deploy on any web container. You can find the todo-list sources [here](../todo-app). Up to now we have started an 
embedded Jetty web container with automatic deployments during the Maven build lifecycle. This approach is fantastic 
when running automated tests in a continuous build.
  
Unfortunately the Jetty server and the sample application automatically get stopped when the Maven build is finished. 
There may be times we want to test against a standalone todo-list application.  

You can start the sample todo list application in Jetty with this command.

     mvn jetty:run

This starts the Jetty web container and automatically deploys the todo list app. Point your browser to
 
    http://localhost:8080/todolist/

You will see the web UI of the todo list and add some new todo entries.

Now we are ready to execute some Citrus tests in a separate JVM.

Citrus test
---------

Once the sample application is deployed and running you can execute the Citrus test cases.
Open a separate command line terminal and navigate to the sample folder.

Execute all Citrus tests by calling

     mvn verify

You can also pick a single test by calling

     mvn verify -Dit.test=<testname>

You should see Citrus performing several tests with lots of debugging output in both terminals (sample application server
and Citrus test client). And of course green tests at the very end of the build.

Of course you can also start the Citrus tests from your favorite IDE.
Just start the Citrus test using the TestNG IDE integration in IntelliJ, Eclipse or Netbeans.

Further information
---------

For more information on Citrus see [www.citrusframework.org][2], including
a complete [reference manual][3].

 [1]: https://citrusframework.org/img/brand-logo.png "Citrus"
 [2]: https://citrusframework.org
 [3]: https://citrusframework.org/reference/html/
 [4]: https://citrusframework.org/reference/html#actions-database
