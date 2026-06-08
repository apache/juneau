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

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.stream.*;

import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.eclipse.jetty.server.*;
import org.junit.jupiter.api.*;

/**
 * Boot-and-shutdown smoke test for {@link JettyMicroservice}.
 *
 * <p>
 * Verifies that the bundled defaults (classpath {@code jetty.xml} + {@code juneau.cfg}) plus a
 * single {@code @Bean Servlet} are sufficient to stand up a Jetty-backed Juneau microservice and serve a
 * 200 from a {@link Rest @Rest}-annotated resource.  Asserts both the facade entry-point and a clean
 * shutdown via {@link Microservice#stop()}.
 *
 * <p>
 * The test pre-grabs a free ephemeral port via {@code ServerSocket(0)} and publishes it as the
 * {@code availablePort} system property before {@link JettyMicroservice#run} is invoked.  The bundled
 * {@code jetty.xml} resolves {@code $S{availablePort,10000}} to that pre-set value, so Jetty binds there
 * deterministically &mdash; this avoids the bundled default port {@code 10000} colliding with whatever
 * else is bound on the developer's machine.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"serial"  // serialVersionUID not required for test classes.
})
class JettyMicroservice_Test {

	@Rest(paths="/*")
	public static class Root extends BasicRestServlet {
		@RestGet(path="/hello")
		public String hello() {
			return "OK";
		}
	}

	private static int reserveFreePort() throws Exception {
		try (var ss = new ServerSocket(0)) {
			return ss.getLocalPort();
		}
	}

	@Test
	@SuppressWarnings({
		"resource" // ms is a server-lifetime resource stopped in the finally block; try-with-resources is not applicable.
	})
	void runStartsServerServesRequestAndStops() throws Exception {
		// Pre-create the working-dir logs/ that the bundled jetty.xml's CustomRequestLog writes to.
		Files.createDirectories(Path.of("logs"));
		// Pre-publish a free ephemeral port; JettyServerComponent's @Value("${availablePort}") sentinel
		// honors a pre-set value, and the bundled jetty.xml binds the connector to $S{availablePort}.
		var port = reserveFreePort();
		System.setProperty("availablePort", String.valueOf(port));
		var ms = JettyMicroservice.run(new String[0], new Root(), false);
		try {
			var jsc = ms.getBeanStore().getBean(JettyServerComponent.class).orElseThrow();
			var localPort = -1;
			for (var c : jsc.getServer().getConnectors()) {
				if (c instanceof ServerConnector sc) {
					localPort = sc.getLocalPort();
					break;
				}
			}
			assertEquals(port, localPort, "Jetty bound port did not match pre-reserved availablePort");

			var url = URI.create("http://localhost:" + localPort + "/hello").toURL();
			var conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "text/plain");
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);
			try {
				assertEquals(200, conn.getResponseCode());
				try (var r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
					var body = r.lines().collect(Collectors.joining());
					assertTrue(body.contains("OK"), "Expected body to contain 'OK' but got: " + body);
				}
			} finally {
				conn.disconnect();
			}
		} finally {
			ms.stop();
			ms.stopConsole();
			System.clearProperty("availablePort");
			System.clearProperty("juneau.serverPort");
		}
	}
}
