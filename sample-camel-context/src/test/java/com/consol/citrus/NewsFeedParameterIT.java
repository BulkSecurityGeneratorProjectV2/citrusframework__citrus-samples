/*
 * Copyright 2006-2014 the original author or authors.
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

package com.consol.citrus;

import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.jms.endpoint.JmsEndpoint;
import com.consol.citrus.testng.CitrusParameters;
import com.consol.citrus.testng.spring.TestNGCitrusSpringSupport;
import com.consol.citrus.ws.message.SoapMessageHeaders;
import com.consol.citrus.ws.server.WebServiceServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.consol.citrus.actions.SendMessageAction.Builder.send;
import static com.consol.citrus.ws.actions.SoapActionBuilder.soap;

/**
 * @author Christoph Deppisch
 * @since 2.1
 */
@Test
public class NewsFeedParameterIT extends TestNGCitrusSpringSupport {

    @Autowired
    private JmsEndpoint newsJmsEndpoint;

    @Autowired
    private WebServiceServer newsServer;

    @CitrusTest(name = "NewsFeed_DataProvider_Ok_IT")
    @CitrusParameters({ "message" })
    @Test(dataProvider = "messageDataProvider")
    public void newsFeed_DataProvider_Ok_Test(String message) {
        $(send()
            .endpoint(newsJmsEndpoint)
            .message()
            .body("<nf:News xmlns:nf=\"http://citrusframework.org/schemas/samples/news\">" +
                        "<nf:Message>${message}</nf:Message>" +
                    "</nf:News>"));

        $(soap()
            .server(newsServer)
            .receive()
            .message()
            .soapAction("newsFeed")
            .body("<nf:News xmlns:nf=\"http://citrusframework.org/schemas/samples/news\">" +
                        "<nf:Message>" + message + "</nf:Message>" +
                    "</nf:News>"));

        $(soap()
            .server(newsServer)
            .send()
            .message()
            .header(SoapMessageHeaders.HTTP_STATUS_CODE, "200"));
    }

    @DataProvider
    public Object[][] messageDataProvider() {
        return new Object[][] {{ "Citrus rocks!" },
                               { "Citrus really rocks!" },
                               { "Citrus is awesome!" }};
    }
}
