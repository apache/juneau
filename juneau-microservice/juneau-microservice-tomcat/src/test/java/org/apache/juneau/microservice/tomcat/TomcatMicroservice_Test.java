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
package org.apache.juneau.microservice.tomcat;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.stream.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;

/**
 * Boot-and-shutdown smoke test for {@link TomcatMicroservice}.
 *
 * <p>
 * Verifies that a bean-store-supplied ephemeral {@link TomcatSettings} port plus a single
 * {@code @Bean Servlet} are sufficient to stand up a Tomcat-backed Juneau microservice and serve a 200
 * from a {@link Rest @Rest}-annotated resource.  Asserts both the facade entry-point and a clean shutdown
 * via {@link Microservice#stop()}.
 *
 * <p>
 * Unlike the Jetty facade (which resolves its bind port from a bundled {@code jetty.xml} via a
 * pre-published {@code availablePort} system property), the embedded-Tomcat path binds programmatically,
 * so the ephemeral port ({@code TomcatSettings.ports(0)}) is supplied directly via the power-user
 * {@link TomcatMicroservice#run(String[], WritableBeanStore, boolean, Class[])} overload and read back
 * from {@link TomcatServerComponent#getPort()} after start.
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.TomcatMicroserviceTest
class TomcatMicroservice_Test {

	@Rest(paths="/*")
	public static class Root extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/hello")
		public String hello() {
			return "OK";
		}
	}

	@Test
	@SuppressWarnings({
		"resource" // ms is a server-lifetime resource stopped in the finally block; try-with-resources is not applicable.
	})
	void runStartsServerServesRequestAndStops() throws Exception {
		var beanStore = new BasicBeanStore();
		beanStore.addBean(Servlet.class, new Root());
		beanStore.addBean(TomcatSettings.class, TomcatSettings.create().ports(0).build());
		var ms = TomcatMicroservice.run(new String[0], beanStore, false, TomcatConfiguration.class);
		try {
			var tsc = ms.getBeanStore().getBean(TomcatServerComponent.class).orElseThrow();
			var localPort = tsc.getPort();
			assertTrue(localPort > 0, "Tomcat should bind to an ephemeral port");

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
		}
	}
}
