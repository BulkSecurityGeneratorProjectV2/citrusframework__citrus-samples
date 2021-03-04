/*
 * Copyright 2006-2015 the original author or authors.
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

package com.consol.citrus.samples.bakery;

import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.dsl.testng.TestNGCitrusTestRunner;
import com.consol.citrus.http.client.HttpClient;
import com.consol.citrus.jms.endpoint.JmsEndpoint;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.testng.annotations.Test;

/**
 * @author Christoph Deppisch
 * @since 2.4
 */
@Test
public class RouteMessagesHttpIT extends TestNGCitrusTestRunner {

    @Autowired
    @Qualifier("bakeryClient")
    private HttpClient bakeryClient;

    @Autowired
    @Qualifier("workerChocolateEndpoint")
    private JmsEndpoint workerChocolateEndpoint;

    @Autowired
    @Qualifier("workerCaramelEndpoint")
    private JmsEndpoint workerCaramelEndpoint;

    @Autowired
    @Qualifier("workerBlueberryEndpoint")
    private JmsEndpoint workerBlueberryEndpoint;

    @CitrusTest
    public void routeMessagesContentBased() {
        http(httpActionBuilder -> httpActionBuilder
            .client(bakeryClient)
            .send()
            .post("/order")
            .contentType(ContentType.APPLICATION_JSON.getMimeType())
            .payload("{ \"order\": { \"type\": \"chocolate\", \"id\": citrus:randomNumber(10), \"amount\": 1}}"));

        http(httpActionBuilder -> httpActionBuilder
            .client(bakeryClient)
            .receive()
            .response(HttpStatus.NO_CONTENT));

        receive(httpActionBuilder -> httpActionBuilder
            .endpoint(workerChocolateEndpoint)
            .payload("<order><type>chocolate</type><id>@ignore@</id><amount>1</amount></order>"));

        http(httpActionBuilder -> httpActionBuilder
            .client(bakeryClient)
            .send()
            .post("/order")
            .contentType(ContentType.APPLICATION_JSON.getMimeType())
            .payload("{ \"order\": { \"type\": \"caramel\", \"id\": citrus:randomNumber(10), \"amount\": 1}}"));

        http(httpActionBuilder -> httpActionBuilder
            .client(bakeryClient)
            .receive()
            .response(HttpStatus.NO_CONTENT));

        receive(receiveMessageBuilder -> receiveMessageBuilder
            .endpoint(workerCaramelEndpoint)
            .payload("<order><type>caramel</type><id>@ignore@</id><amount>1</amount></order>"));

        http(httpActionBuilder -> httpActionBuilder
            .client(bakeryClient)
            .send()
            .post("/order")
            .contentType(ContentType.APPLICATION_JSON.getMimeType())
            .payload("{ \"order\": { \"type\": \"blueberry\", \"id\": citrus:randomNumber(10), \"amount\": 1}}"));

        http(httpActionBuilder -> httpActionBuilder
            .client(bakeryClient)
            .receive()
            .response(HttpStatus.NO_CONTENT));

        receive(receiveMessageBuilder -> receiveMessageBuilder
            .endpoint(workerBlueberryEndpoint)
            .payload("<order><type>blueberry</type><id>@ignore@</id><amount>1</amount></order>"));
    }

    @CitrusTest
    public void routeUnknownOrderType() {
        http(httpActionBuilder -> httpActionBuilder
            .client(bakeryClient)
            .send()
            .post("/order")
            .payload("{ \"order\": { \"type\": \"brownie\", \"id\": citrus:randomNumber(10), \"amount\": 1}}"));

        http(httpActionBuilder -> httpActionBuilder
            .client(bakeryClient)
            .receive()
            .response(HttpStatus.NO_CONTENT));

        receive(receiveMessageBuilder -> receiveMessageBuilder
            .endpoint("jms:factory.unknown.inbound")
            .payload("<order><type>brownie</type><id>@ignore@</id><amount>1</amount></order>"));
    }
}
