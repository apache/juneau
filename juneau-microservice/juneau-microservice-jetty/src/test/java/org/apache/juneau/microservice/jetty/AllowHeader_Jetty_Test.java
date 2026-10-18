/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.microservice.jetty;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.eclipse.jetty.ee11.servlet.*;
import org.eclipse.jetty.server.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Pins that the {@code Allow} header on a framework {@code 405} survives a real Jetty connector, including the empty
 * one sent when nothing answers on the path.
 */
@org.apache.juneau.testing.JettyMicroserviceTest
class AllowHeader_Jetty_Test extends TestBase {

	/** A start-call hook that leaves the status at {@code 405}, so an unmatched path reaches handleNotFound's 405. */
	@Rest
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;

		@RestStartCall
		public void start(HttpServletRequest req, HttpServletResponse res) {
			if (req.getRequestURI().startsWith("/nothing"))
				res.setStatus(405);
		}

		@RestGet("/a")
		public String get() {
			return "GET";
		}

		@RestPut("/a")
		public String put() {
			return "PUT";
		}
	}

	private static Server server;
	private static URI base;
	private static final HttpClient CLIENT = HttpClient.newHttpClient();

	@SuppressWarnings("resource") // The connector is added to the server, which closes it in stop().
	@BeforeAll
	static void start() throws Exception {
		server = new Server();
		var connector = new ServerConnector(server);
		connector.setPort(0);
		server.addConnector(connector);
		var ctx = new ServletContextHandler();
		ctx.setContextPath("/");
		ctx.addServlet(new ServletHolder(new A()), "/*");
		server.setHandler(ctx);
		server.start();
		base = URI.create("http://localhost:" + connector.getLocalPort());
	}

	@AfterAll
	static void stop() throws Exception {
		server.stop();
	}

	private static HttpResponse<String> send(String method, String path) throws Exception {
		var req = HttpRequest.newBuilder(base.resolve(path)).method(method, HttpRequest.BodyPublishers.noBody()).build();
		return CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
	}

	@Test void a01_pathMatchesOtherMethod_allow() throws Exception {
		var r = send("DELETE", "/a");
		assertEquals(405, r.statusCode());
		assertEquals("GET, PUT", r.headers().firstValue("Allow").orElse(null));
	}

	@Test void a02_emptyAllow_sentAsEmptyHeader() throws Exception {
		var r = send("GET", "/nothing");
		assertEquals(405, r.statusCode());
		assertEquals("", r.headers().firstValue("Allow").orElse(null));
	}
}
