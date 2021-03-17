Selenium sample ![Logo][1]
==============

This sample shows how to use Selenium in order to create UI testing with Citrus. Why would somebody combine Citrus with Selenium?
On the one hand Citrus provides XML and Java fluent API integration for Selenium tasks so you do not need to write Selenium code yourself. 
Secondly you might want to combine UI testing with Citrus messaging capabilities such as deleting all entries on a Http REST service first and then
creating a new entry via UI interaction in a browser. Read about this feature in [reference guide][4]

Objectives
---------

The [todo-list](../todo-app/README.md) sample application provides a REST API for managing todo entries.
We open the browser and perform some user interaction with the todo app user interface. Besides that we call the Http REST API in order to clean up
all todo entries up front. The Selenium integration is also used to validate HTML elements and page objects in our test cases. 

First of all we add a Selenium browser component as Spring bean.

```java
@Bean
public SeleniumBrowser browser() {
    return CitrusEndpoints
        .selenium()
            .browser()
            .type(BrowserType.CHROME)
        .build();
}
```

As you can see the browser component in Citrus is a normal Selenium WebDriver. You can pick the target browser implementation and 
add web driver configuration as you like.

In addition to that basic browser component we add a normal Http client component for calling the Http REST API on the todo server.

```java
@Bean
public HttpClient todoClient() {
    return CitrusEndpoints
        .http()
            .client()
            .requestUrl("http://localhost:8080")
        .build();
}
```

Also we can use a new after suite hook in order to close the browser when all tests are finished:

```java
@Bean
@DependsOn("browser")
public SequenceAfterSuite afterSuite(SeleniumBrowser browser) {
    return new TestRunnerAfterSuiteSupport() {
        @Override
        public void afterSuite(TestRunner runner) {
            runner.selenium(builder -> builder.browser(browser).stop());
        }
    };
}
```

For demonstration purpose we add a `sleep` action in a new after test hook so we see actually what the browser is doing.

```java
@Bean
public SequenceAfterTest afterTest() {
    return new TestRunnerAfterTestSupport() {
        @Override
        public void afterTest(TestRunner runner) {
            runner.sleep(500);
        }
    };
}
```

In a test case we can now use the Selenium browser component in the Citrus fluent API.

```java
@Test
@CitrusTest
public void testAddEntry() {
    variable("todoName", "todo_citrus:randomNumber(4)");
    variable("todoDescription", "Description: ${todoName}");

    $(http()
        .client(todoClient)
        .send()
        .delete("/api/todolist"));

    $(http()
        .client(todoClient)
        .receive()
        .response(HttpStatus.OK));

    selenium(seleniumActionBuilder -> seleniumActionBuilder
        .browser(browser)
        .start());

    selenium(seleniumActionBuilder -> seleniumActionBuilder
        .navigate(todoClient.getEndpointConfiguration().getRequestUrl() + "/todolist"));

    selenium(seleniumActionBuilder -> seleniumActionBuilder
        .find()
        .element(By.xpath("(//li[@class='list-group-item'])[last()]"))
        .text("No todos found"));

    selenium(seleniumActionBuilder -> seleniumActionBuilder
        .setInput("${todoName}")
        .element(By.name("title")));

    selenium(seleniumActionBuilder -> seleniumActionBuilder
        .setInput("${todoDescription}")
        .element(By.name("description"))
    );

    selenium(seleniumActionBuilder -> seleniumActionBuilder
        .click()
        .element(By.tagName("button")));

    selenium(seleniumActionBuilder -> seleniumActionBuilder
        .waitUntil()
        .element(By.xpath("(//li[@class='list-group-item']/span)[last()]"))
        .timeout(2000L)
        .visible());

    selenium(seleniumActionBuilder -> seleniumActionBuilder
        .find()
        .element(By.xpath("(//li[@class='list-group-item']/span)[last()]"))
        .text("${todoName}"));
}
```

We call the `DELETE` option on the `/todolist` Http resource in order to have an empty todo list that we can operate on. The Selenium
browser is started and the user navigates to the todo list application. The testcase validates the empty todo list display and fills out the todo HTML form
with a sample entry. The form is submitted and a final validation checks that the todo entry is added and displayed in the list.

Page objects
---------

Using page objects that represent a application page with its elements and action capabilities is a good practice in UI testing with Selenium.
Citrus also supports the usage Java POJOs as page objects.

```java
public class TodoPage implements WebPage, PageValidator<TodoPage> {

    @FindBy(tagName = "h1")
    private WebElement heading;

    @FindBy(tagName = "form")
    private WebElement newTodoForm;

    @FindBy(xpath = "(//li[@class='list-group-item'])[last()]")
    private WebElement lastEntry;

    /**
     * Submits new entry.
     * @param title
     * @param description
     */
    public void submit(String title, String description) {
        newTodoForm.findElement(By.id("title")).sendKeys(title);

        if (StringUtils.hasText(description)) {
            newTodoForm.findElement(By.id("description")).sendKeys(description);
        }

        newTodoForm.submit();
    }

    @Override
    public void validate(TodoPage webPage, SeleniumBrowser browser, TestContext context) {
        Assert.assertEquals(heading.getText(), "TODO list");
        Assert.assertEquals(lastEntry.getText(), "No todos found");
    }
}
```

The page object listed above represents the todo list page with its elements and actions such as `submit`. The page also implements the
`PageValidator` interface adding validation methods for checking that the rendered page is as expected.

In a test case we can use this page objects for user interaction and validation:

```java
@Test
@CitrusTest
public void testAddEntry() {
    variable("todoName", "todo_citrus:randomNumber(4)");
    variable("todoDescription", "Description: ${todoName}");

    [...]

    TodoPage todoPage = new TodoPage();

     selenium(seleniumActionBuilder -> seleniumActionBuilder
         .page(todoPage)
         .validate());

     selenium(seleniumActionBuilder -> seleniumActionBuilder
         .page(todoPage)
         .argument("${todoName}")
         .argument("${todoDescription}")
         .execute("submit"));

    [...]
}
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
 [4]: https://citrusframework.org/reference/html#selenium
