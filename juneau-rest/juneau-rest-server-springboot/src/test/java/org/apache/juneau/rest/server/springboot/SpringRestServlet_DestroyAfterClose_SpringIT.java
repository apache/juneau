// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest.server.springboot;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.builder.*;
import org.springframework.boot.web.servlet.*;
import org.springframework.context.annotation.*;

/**
 * Closes a Spring Boot application context running under embedded Tomcat and checks that the container's servlet
 * destroy (which happens after the context closed) neither throws nor logs a warning.
 *
 * <p>
 * Binds a socket, so it is off by default.  Run it with {@code -Djuneau.busIt=true}, outside the sandbox.
 *
 * @since 10.0.0
 */
@EnabledIfSystemProperty(named="juneau.busIt", matches="true",
	disabledReason="binds a socket; run with -Dtest='SpringRestServlet_DestroyAfterClose_SpringIT' -Djuneau.busIt=true")
@SuppressWarnings({
	"resource" // The JDK HttpClient is not a resource; the test closes the Spring context itself.
})
class SpringRestServlet_DestroyAfterClose_SpringIT {

	static final AtomicInteger DESTROYED = new AtomicInteger();

	@Rest(path="/child")
	public static class Child {
		@RestGet(path="/x") public String x() { return "x"; }
	}

	@Rest(paths="/root/*", children={Child.class})
	public static class Root extends BasicSpringRestServletGroup {
		private static final long serialVersionUID = 1L;

		@Override /* Overridden from RestServlet */
		public synchronized void destroy() {
			DESTROYED.incrementAndGet();
			super.destroy();
		}
	}

	@Configuration
	@EnableAutoConfiguration
	public static class App {
		@Bean public Root root() { return new Root(); }
		@Bean public Child child() { return new Child(); }
		@Bean public ServletRegistrationBean<Root> rootRegistration(Root root) { return JuneauServletRegistrations.forServlet(root, null); }
	}

	private static class Capture extends Handler {
		final List<LogRecord> records = new ArrayList<>();
		@Override public void publish(LogRecord r) { records.add(r); }
		@Override public void flush() { /* no-op */ }
		@Override public void close() { /* no-op */ }
	}

	@Test void a01_closeContext_containerDestroysServletQuietly() throws Exception {
		DESTROYED.set(0);
		var capture = new Capture();
		var root = Logger.getLogger("");
		root.addHandler(capture);
		try {
			var ctx = new SpringApplicationBuilder(App.class).properties("server.port=0").run();
			var port = ctx.getEnvironment().getProperty("local.server.port");
			var res = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/root/child/x")).build(), BodyHandlers.ofString());
			assertEquals(200, res.statusCode());
			ctx.close();
		} finally {
			root.removeHandler(capture);
		}
		assertEquals(1, DESTROYED.get(), "servlet destroy should have run once");
		var bad = capture.records.stream().filter(r -> r.getLevel().intValue() >= Level.WARNING.intValue()).toList();
		assertTrue(bad.isEmpty(), () -> "Unexpected log records: " + bad.stream().map(r -> r.getMessage() + " " + r.getThrown()).toList());
	}
}
