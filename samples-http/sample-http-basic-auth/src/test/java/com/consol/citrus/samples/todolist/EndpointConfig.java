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

package com.consol.citrus.samples.todolist;

import java.util.Collections;
import java.util.List;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.citrusframework.dsl.endpoint.CitrusEndpoints;
import org.citrusframework.endpoint.adapter.StaticEndpointAdapter;
import org.citrusframework.http.client.BasicAuthClientHttpRequestFactory;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.http.message.HttpMessage;
import org.citrusframework.http.security.BasicAuthConstraint;
import org.citrusframework.http.security.SecurityHandlerFactory;
import org.citrusframework.http.security.User;
import org.citrusframework.http.server.HttpServer;
import org.citrusframework.message.Message;
import org.citrusframework.xml.namespace.NamespaceContextBuilder;
import org.eclipse.jetty.security.SecurityHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

/**
 * @author Christoph Deppisch
 */
@Configuration
public class EndpointConfig {

    private static final String[] USER_ROLES = new String[] { "CitrusRole" };

    private final int port = 8090;

    @Bean
    public NamespaceContextBuilder namespaceContextBuilder() {
        NamespaceContextBuilder namespaceContextBuilder = new NamespaceContextBuilder();
        namespaceContextBuilder.setNamespaceMappings(Collections.singletonMap("xh", "http://www.w3.org/1999/xhtml"));
        return namespaceContextBuilder;
    }

    @Bean
    public HttpClient todoClient() {
        return CitrusEndpoints
            .http()
                .client()
                .requestUrl("http://localhost:" + port)
            .build();
    }

    @Bean
    public HttpClient todoBasicAuthClient() throws Exception {
        return CitrusEndpoints
            .http()
                .client()
                .requestUrl("http://localhost:" + port)
                .requestFactory(basicAuthRequestFactory())
            .build();
    }

    @Bean
    public BasicAuthClientHttpRequestFactory basicAuthRequestFactoryBean() {
        BasicAuthClientHttpRequestFactory requestFactory = new BasicAuthClientHttpRequestFactory();

        AuthScope authScope = new AuthScope("http", "localhost", port, "", "basic");
        requestFactory.setAuthScope(authScope);

        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("citrus", "secr3t".toCharArray());
        requestFactory.setCredentials(credentials);
        return requestFactory;
    }

    @Bean
    public HttpComponentsClientHttpRequestFactory basicAuthRequestFactory() throws Exception {
        return basicAuthRequestFactoryBean().getObject();
    }

    @Bean
    public HttpServer basicAuthHttpServer() throws Exception {
        return CitrusEndpoints
            .http()
                .server()
                .port(port)
                .endpointAdapter(staticEndpointAdapter())
                .securityHandler(basicAuthSecurityHandler())
                .autoStart(true)
            .build();
    }

    @Bean
    public StaticEndpointAdapter staticEndpointAdapter() {
        return new StaticEndpointAdapter() {
            @Override
            protected Message handleMessageInternal(Message message) {
                return new HttpMessage("<todo xmlns=\"http://citrusframework.org/samples/todolist\">" +
                            "<id>100</id>" +
                            "<title>todoName</title>" +
                            "<description>todoDescription</description>" +
                        "</todo>")
                        .status(HttpStatus.OK);
            }
        };
    }

    @Bean
    public SecurityHandlerFactory basicAuthSecurityHandlerFactoryBean() {
        SecurityHandlerFactory securityHandlerFactory = new SecurityHandlerFactory();
        securityHandlerFactory.setUsers(users());
        securityHandlerFactory.setConstraints(Collections.singletonMap("/todo/*", new BasicAuthConstraint(USER_ROLES)));

        return securityHandlerFactory;
    }

    @Bean
    public SecurityHandler basicAuthSecurityHandler() throws Exception {
        return basicAuthSecurityHandlerFactoryBean().getObject();
    }

    private List<User> users() {
        return Collections.singletonList(new User("citrus", "secr3t", USER_ROLES));
    }
}
