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

import org.citrusframework.dsl.endpoint.CitrusEndpoints;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.spi.Resources;
import org.citrusframework.variable.dictionary.json.JsonPathMappingDataDictionary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Christoph Deppisch
 */
@Import(TodoAppAutoConfiguration.class)
@Configuration
public class EndpointConfig {

    @Bean
    public HttpClient todoClient() {
        return CitrusEndpoints.http()
                            .client()
                            .requestUrl("http://localhost:8080")
                            .build();
    }

    @Bean
    public JsonPathMappingDataDictionary inboundDictionary() {
        JsonPathMappingDataDictionary dataDictionary = new JsonPathMappingDataDictionary();
        dataDictionary.setGlobalScope(false);
        dataDictionary.setMappingFile(Resources.fromClasspath("dictionary/inbound.properties"));
        return dataDictionary;
    }

    @Bean
    public JsonPathMappingDataDictionary outboundDictionary() {
        JsonPathMappingDataDictionary dataDictionary = new JsonPathMappingDataDictionary();
        dataDictionary.setGlobalScope(false);
        dataDictionary.setMappingFile(Resources.fromClasspath("dictionary/outbound.properties"));
        return dataDictionary;
    }
}
