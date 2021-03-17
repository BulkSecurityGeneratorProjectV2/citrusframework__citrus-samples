/*
 * Copyright 2006-2017 the original author or authors.
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

import javax.jms.ConnectionFactory;

import com.consol.citrus.dsl.endpoint.CitrusEndpoints;
import com.consol.citrus.http.client.HttpClient;
import com.consol.citrus.jms.endpoint.JmsEndpoint;
import com.consol.citrus.mail.server.MailServer;
import com.consol.citrus.report.MessageTracingTestListener;
import com.consol.citrus.variable.GlobalVariables;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Christoph Deppisch
 */
@ContextConfiguration
@PropertySource(value = "citrus.properties")
public class CitrusEndpointConfig {

    @Value("${mail.server.port}")
    public int mailServerPort;

    @Value("${activemq.server.port}")
    public int activemqServerPort;

    @Value("${bakery.server.port}")
    public int bakeryServerPort;

    @Value("${report.server.port}")
    public int reportServerPort;

    @Bean
    public GlobalVariables globalVariables() {
        GlobalVariables variables = new GlobalVariables();
        variables.getVariables().put("project.name", "Citrus Bakery sample");
        return variables;
    }

    @Bean
    public MessageTracingTestListener messageTracingTestListener() {
        return new MessageTracingTestListener();
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(String.format("tcp://localhost:%s", activemqServerPort));
        connectionFactory.setWatchTopicAdvisories(false);
        return connectionFactory;
    }

    @Bean
    public HttpClient bakeryClient() {
        return CitrusEndpoints.http()
                    .client()
                    .requestUrl(String.format("http://localhost:%s/bakery/services", bakeryServerPort))
                    .build();
    }

    @Bean
    public JmsEndpoint bakeryOrderEndpoint(ConnectionFactory connectionFactory) {
        return CitrusEndpoints.jms()
                .asynchronous()
                .destination("bakery.order.inbound")
                .connectionFactory(connectionFactory)
                .build();
    }

    @Bean
    public JmsEndpoint unknownOrderEndpoint(ConnectionFactory connectionFactory) {
        return CitrusEndpoints.jms()
                .asynchronous()
                .destination("factory.unknown.inbound")
                .connectionFactory(connectionFactory)
                .build();
    }

    @Bean
    public HttpClient reportingClient() {
        return CitrusEndpoints.http()
                    .client()
                    .requestMethod(HttpMethod.GET)
                    .requestUrl(String.format("http://localhost:%s/report/services", reportServerPort))
                    .build();
    }

    @Bean
    public MailServer mailServer() {
        return CitrusEndpoints.mail()
                .server()
                .port(mailServerPort)
                .autoAccept(true)
                .autoStart(true)
                .build();
    }
}
