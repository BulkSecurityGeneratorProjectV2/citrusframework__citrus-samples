/*
 * Copyright 2006-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.consol.citrus.samples.todolist;

import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.http.client.HttpClient;
import com.consol.citrus.jdbc.message.JdbcMessage;
import com.consol.citrus.jdbc.server.JdbcServer;
import com.consol.citrus.testng.spring.TestNGCitrusSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import static com.consol.citrus.actions.ReceiveMessageAction.Builder.receive;
import static com.consol.citrus.actions.SendMessageAction.Builder.send;
import static com.consol.citrus.container.Wait.Builder.waitFor;
import static com.consol.citrus.http.actions.HttpActionBuilder.http;

public class TodoListIT extends TestNGCitrusSpringSupport {

    @Autowired
    private JdbcServer jdbcServer;


    @Autowired
    private HttpClient todoClient;

    @Test
    @CitrusTest
    public void testTransaction() {
        variable("todoName", "citrus:concat('todo_', citrus:randomNumber(4))");
        variable("todoDescription", "Description: ${todoName}");

        $(waitFor().http()
                .status(HttpStatus.OK.value())
                .method(HttpMethod.GET.name())
                .milliseconds(20000L)
                .interval(1000L)
                .url(todoClient.getEndpointConfiguration().getRequestUrl()));

        $(http()
            .client(todoClient)
            .send()
            .post("/todolist")
            .fork(true)
            .message()
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .body("title=${todoName}&description=${todoDescription}"));

        $(receive()
            .endpoint(jdbcServer)
            .message(JdbcMessage.startTransaction()));

        $(send().endpoint(jdbcServer).message(JdbcMessage.success()));

        $(receive()
            .endpoint(jdbcServer)
            .message(JdbcMessage.execute("@startsWith('INSERT INTO todo_entries (id, title, description, done) VALUES (?, ?, ?, ?)')@")));

        $(send()
            .endpoint(jdbcServer)
            .message(JdbcMessage.success().rowsUpdated(1)));

        $(receive()
            .endpoint(jdbcServer)
            .message(JdbcMessage.commitTransaction()));

        $(send().endpoint(jdbcServer).message(JdbcMessage.success()));

        $(http()
            .client(todoClient)
            .receive()
            .response(HttpStatus.FOUND));
    }

    @Test
    @CitrusTest
    public void testRollback() {
        variable("todoName", "citrus:concat('todo_', citrus:randomNumber(4))");
        variable("todoDescription", "Description: ${todoName}");

        $(waitFor().http()
                .status(HttpStatus.OK.value())
                .method(HttpMethod.GET.name())
                .milliseconds(20000L)
                .interval(1000L)
                .url(todoClient.getEndpointConfiguration().getRequestUrl()));

        $(http()
            .client(todoClient)
            .send()
            .post("/todolist")
            .fork(true)
            .message()
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .body("title=${todoName}&description=${todoDescription}"));

        $(receive()
            .endpoint(jdbcServer)
            .message(JdbcMessage.startTransaction()));

        $(send().endpoint(jdbcServer).message(JdbcMessage.success()));

        $(receive()
            .endpoint(jdbcServer)
            .message(JdbcMessage.execute("@startsWith('INSERT INTO todo_entries (id, title, description, done) VALUES (?, ?, ?, ?)')@")));

        $(send()
            .endpoint(jdbcServer)
            .message(JdbcMessage.error().exception("Could not execute something")));

        $(receive()
            .endpoint(jdbcServer)
            .message(JdbcMessage.rollbackTransaction()));

        $(send().endpoint(jdbcServer).message(JdbcMessage.success()));

        $(http()
            .client(todoClient)
            .receive()
            .response(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    @CitrusTest
    public void testWithoutTransactionVerification() {
        variable("todoName", "citrus:concat('todo_', citrus:randomNumber(4))");
        variable("todoDescription", "Description: ${todoName}");

        jdbcServer.getEndpointConfiguration().setAutoTransactionHandling(true);

        $(waitFor().http()
                .status(HttpStatus.OK.value())
                .method(HttpMethod.GET.name())
                .milliseconds(20000L)
                .interval(1000L)
                .url(todoClient.getEndpointConfiguration().getRequestUrl()));

        $(http()
            .client(todoClient)
            .send()
            .post("/todolist")
            .fork(true)
            .message()
            .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .body("title=${todoName}&description=${todoDescription}"));

        $(receive()
            .endpoint(jdbcServer)
            .message(JdbcMessage.execute("@startsWith('INSERT INTO todo_entries (id, title, description, done) VALUES (?, ?, ?, ?)')@")));

        $(send()
            .endpoint(jdbcServer)
            .message(JdbcMessage.success().rowsUpdated(1)));

        $(http()
            .client(todoClient)
            .receive()
            .response(HttpStatus.FOUND));
    }

    @AfterTest
    public void resetTransactionState(){
        jdbcServer.getEndpointConfiguration().setAutoTransactionHandling(false);
    }
}
