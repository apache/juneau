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
package org.apache.juneau.rest.server.convention;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/**
 * Validates the {@link VersionServlet} servlet flavor mounted directly as a top-level servlet.
 *
 * <p>
 * The servlet pins its op at {@code /*} and delegates to a shared {@link VersionMixin} worker, so
 * this mirrors {@link VersionMixin_AsMixin_Test} but exercises the standalone-servlet deployment.
 * Cases:
 * <ul>
 * 	<li>The endpoint emits the deployment-metadata JSON map with {@code Content-Type:
 * 		application/json}.
 * 	<li>The default payload carries the JVM {@code javaVersion} at minimum.
 * 	<li>A builder-configured delegate's programmatic entries surface through the servlet.
 * </ul>
 *
 * @since 10.0.0
 */
class VersionServlet_Test extends TestBase {

	private static final MockRestClient c = MockRestClient.buildLax(VersionServlet.class);

	@Test void a01_versionServed() throws Exception {
		c.get("/version")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("application/json");
	}

	@Test void a02_defaultPayloadHasJavaVersion() throws Exception {
		var body = c.get("/version").run().assertStatus(200).getContent().asString();
		var parsed = JsonParser.DEFAULT.read(body, Map.class);
		Assertions.assertNotNull(parsed.get("javaVersion"), "javaVersion present");
	}

	/** Servlet subclass supplying a builder-configured worker (constructor-injected). */
	public static class B extends VersionServlet {
		private static final long serialVersionUID = 1L;
		public B() {
			super(VersionProvider.create()
				.entry("name", "my-app")
				.entry("version", "1.2.3")
				.fromJavaVersion()
				.build());
		}
	}

	private static final MockRestClient cb = MockRestClient.buildLax(B.class);

	@Test void b01_customDelegateEntriesSurface() throws Exception {
		var body = cb.get("/version").run().assertStatus(200).getContent().asString();
		var parsed = JsonParser.DEFAULT.read(body, Map.class);
		Assertions.assertEquals("my-app", parsed.get("name"));
		Assertions.assertEquals("1.2.3", parsed.get("version"));
		Assertions.assertNotNull(parsed.get("javaVersion"));
	}
}
