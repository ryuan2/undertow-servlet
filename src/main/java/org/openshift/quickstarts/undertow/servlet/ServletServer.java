/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.openshift.quickstarts.undertow.servlet;

import javax.servlet.ServletException;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;

import org.jboss.logging.Logger;

import static io.undertow.servlet.Servlets.defaultContainer;
import static io.undertow.servlet.Servlets.deployment;
import static io.undertow.servlet.Servlets.servlet;

/**
 * @author Stuart Douglas
 */
public class ServletServer {
    private static final Logger LOGGER = Logger.getLogger(ServletServer.class);

    public static final String MYAPP = "/";

    public static void main(final String[] args) {
        try {

            DeploymentInfo servletBuilder = deployment()
                    .setClassLoader(ServletServer.class.getClassLoader())
                    .setContextPath(MYAPP)
                    .setDeploymentName("test.war")
                    .addServlets(
                            servlet("MessageServlet", MessageServlet.class)
                                    .addInitParam("message", "Hello World")
                                    .addMapping("/*"),
                            servlet("MyServlet", MessageServlet.class)
                                    .addInitParam("message", "MyServlet")
                                    .addMapping("/myservlet"));

            DeploymentManager manager = defaultContainer().addDeployment(servletBuilder);
            manager.deploy();

            HttpHandler servletHandler = manager.start();
            PathHandler path = Handlers.path(Handlers.redirect(MYAPP))
                    .addPrefixPath(MYAPP, servletHandler)
                    .addPrefixPath("/voice", exchange -> {
                        HeaderMap headers = exchange.getRequestHeaders();
                        for (HeaderValues header : headers) {
                            LOGGER.infof("%s:", header.getHeaderName());
                            for (String s : header) {
                                LOGGER.infof("  %s", s);
                            }
                        }

                        LOGGER.infof("QueryString: %s", exchange.getQueryString());
                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/xml");
                        exchange.getResponseSender().send("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><Response><Say>Sip domain</Say></Response>");
                    })
                    .addPrefixPath("/webhook", exchange -> {
                        HeaderMap headers = exchange.getRequestHeaders();
                        for (HeaderValues header : headers) {
                            LOGGER.infof("%s:", header.getHeaderName());
                            for (String s : header) {
                                LOGGER.infof("  %s", s);
                            }
                        }

                        LOGGER.infof("QueryString: %s", exchange.getQueryString());
                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/xml");
                        exchange.getResponseSender().send("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><Response><Say>Web hook</Say></Response>");
                    });
            Undertow server = Undertow.builder()
                    .addHttpListener(8080, "0.0.0.0")
                    .setHandler(path)
                    .build();
            server.start();
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }
}
