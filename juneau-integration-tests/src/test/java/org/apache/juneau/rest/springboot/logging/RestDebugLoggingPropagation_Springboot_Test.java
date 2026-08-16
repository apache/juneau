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
package org.apache.juneau.rest.springboot.logging;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.time.*;
import java.util.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.builder.*;
import org.springframework.boot.web.servlet.*;
import org.springframework.context.*;
import org.springframework.context.annotation.*;

/**
 * Spring Boot end-to-end tests proving that logging-level properties drive JUL debug detail.
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.SpringbootTest
class RestDebugLoggingPropagation_Springboot_Test extends TestBase {

	private static final String HOST = RestDebugLoggingPropagation_HostResource.class.getName();
	private static final String OP_ONE = HOST + ".one";
	private static final String OP_TWO = HOST + ".two";
	private static final String OP_ECHO = HOST + ".echo";

	private static final HttpClient HTTP = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(5))
		.followRedirects(HttpClient.Redirect.NEVER)
		.build();

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class App {
		@Bean public RestDebugLoggingPropagation_HostResource host() { return new RestDebugLoggingPropagation_HostResource(); }
		@Bean public ServletRegistrationBean<RestDebugLoggingPropagation_HostResource> hostRegistration(RestDebugLoggingPropagation_HostResource host) {
			return new ServletRegistrationBean<>(host, "/api/*");
		}
	}

	private static final class CollectingHandler extends Handler {
		private final List<LogRecord> records = new ArrayList<>();

		@Override
		public void publish(LogRecord record) {
			if (isLoggable(record))
				records.add(record);
		}

		@Override public void flush() {}
		@Override public void close() {}

		List<LogRecord> records() {
			return records;
		}
	}

	private static final class LoggerState {
		private final Level level;
		private final boolean useParentHandlers;
		private final Handler[] handlers;

		LoggerState(Logger logger) {
			level = logger.getLevel();
			useParentHandlers = logger.getUseParentHandlers();
			handlers = logger.getHandlers();
		}

		void restore(Logger logger) {
			for (var h : logger.getHandlers())
				logger.removeHandler(h);
			for (var h : handlers)
				logger.addHandler(h);
			logger.setUseParentHandlers(useParentHandlers);
			logger.setLevel(level);
		}
	}

	private ConfigurableApplicationContext start(String...extraProperties) {
		var props = new ArrayList<String>();
		props.add("spring.main.banner-mode=off");
		props.add("server.port=0");
		props.add("logging.level.root=WARN");
		props.add("juneau.rest.logging.propagate-levels=true");
		props.addAll(Arrays.asList(extraProperties));
		return new SpringApplicationBuilder(App.class)
			.web(WebApplicationType.SERVLET)
			.properties(props.toArray(String[]::new))
			.run();
	}

	private int port(ConfigurableApplicationContext ctx) {
		return ctx.getEnvironment().getProperty("local.server.port", Integer.class, -1);
	}

	private HttpResponse<String> get(int port, String path, String...headers) throws Exception {
		var b = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + path))
			.timeout(Duration.ofSeconds(10))
			.GET();
		for (var i = 0; i < headers.length; i += 2)
			b.header(headers[i], headers[i + 1]);
		return HTTP.send(b.build(), BodyHandlers.ofString());
	}

	private HttpResponse<String> post(int port, String path, String body, String...headers) throws Exception {
		var b = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + path))
			.timeout(Duration.ofSeconds(10))
			.POST(HttpRequest.BodyPublishers.ofString(body));
		for (var i = 0; i < headers.length; i += 2)
			b.header(headers[i], headers[i + 1]);
		return HTTP.send(b.build(), BodyHandlers.ofString());
	}

	@Test void b01_tracePropertyDrivesFinestBodyDetail_withoutProgrammaticSetLevel() throws Exception {
		var logger = Logger.getLogger(OP_ECHO);
		var state = new LoggerState(logger);
		var handler = new CollectingHandler();
		try {
			for (var h : logger.getHandlers())
				logger.removeHandler(h);
			logger.setUseParentHandlers(false);
			handler.setLevel(Level.INFO);
			logger.addHandler(handler);

			try (var app = start("logging.level." + HOST + "=TRACE")) {
				var port = port(app);
				var resp = post(port, "/api/echo", "phase4-body", "Content-Type", "text/plain");
				assertEquals(200, resp.statusCode());
				assertTrue(resp.body().contains("phase4-body"), resp.body());
			}

			var record = handler.records().stream().filter(x -> OP_ECHO.equals(x.getLoggerName())).reduce((a, b) -> b).orElse(null);
			assertNotNull(record, "TRACE property should drive JUL detail without direct Logger.setLevel(...) calls");
			assertEquals(Level.INFO, record.getLevel());
			assertTrue(record.getMessage().contains("phase4-body"), record.getMessage());
		} finally {
			state.restore(logger);
		}
	}

	@Test void b02_debugAndInfoPropertiesSelectHeadersVsBasic_allEmitsStayInfo() throws Exception {
		var logger = Logger.getLogger(OP_ONE);
		var state = new LoggerState(logger);
		var handler = new CollectingHandler();
		try {
			for (var h : logger.getHandlers())
				logger.removeHandler(h);
			logger.setUseParentHandlers(false);
			handler.setLevel(Level.INFO);
			logger.addHandler(handler);

			try (var app = start("logging.level." + HOST + "=DEBUG")) {
				var port = port(app);
				var resp = get(port, "/api/one", "X-Debug", "true");
				assertEquals(200, resp.statusCode());
				assertTrue(resp.body().contains("one"), resp.body());
			}
			var debugRecord = handler.records().stream().filter(x -> OP_ONE.equals(x.getLoggerName())).reduce((a, b) -> b).orElse(null);
			assertNotNull(debugRecord);
			assertEquals(Level.INFO, debugRecord.getLevel());
			assertTrue(debugRecord.getMessage().contains("---Request Headers---"), debugRecord.getMessage());
			handler.records().clear();

			try (var app = start("logging.level." + HOST + "=INFO")) {
				var port = port(app);
				var resp = get(port, "/api/one", "X-Debug", "true");
				assertEquals(200, resp.statusCode());
				assertTrue(resp.body().contains("one"), resp.body());
			}
			var infoRecord = handler.records().stream().filter(x -> OP_ONE.equals(x.getLoggerName())).reduce((a, b) -> b).orElse(null);
			assertNotNull(infoRecord);
			assertEquals(Level.INFO, infoRecord.getLevel());
			assertTrue(infoRecord.getMessage().contains("[200] HTTP GET /api/one"), infoRecord.getMessage());
			assertFalse(infoRecord.getMessage().contains("---Request Headers---"), infoRecord.getMessage());
		} finally {
			state.restore(logger);
		}
	}

	@Test void b05_operationScopedPropertyElevatesOnlyOneOperation() throws Exception {
		var parent = Logger.getLogger(HOST);
		var opOne = Logger.getLogger(OP_ONE);
		var opTwo = Logger.getLogger(OP_TWO);
		var parentState = new LoggerState(parent);
		var opOneState = new LoggerState(opOne);
		var opTwoState = new LoggerState(opTwo);
		var handler = new CollectingHandler();
		try {
			for (var h : parent.getHandlers())
				parent.removeHandler(h);
			parent.setUseParentHandlers(false);
			handler.setLevel(Level.INFO);
			parent.addHandler(handler);

			try (var app = start(
				"logging.level." + HOST + "=INFO",
				"logging.level." + HOST + ".one=TRACE"
			)) {
				var port = port(app);
				assertEquals(200, get(port, "/api/one", "X-Scoped", "one").statusCode());
				assertEquals(200, get(port, "/api/two", "X-Scoped", "two").statusCode());
			}

			var oneRecord = handler.records().stream().filter(x -> OP_ONE.equals(x.getLoggerName())).reduce((a, b) -> b).orElse(null);
			var twoRecord = handler.records().stream().filter(x -> OP_TWO.equals(x.getLoggerName())).reduce((a, b) -> b).orElse(null);
			assertNotNull(oneRecord, "operation-level TRACE should emit for .one");
			assertNotNull(twoRecord, "sibling operation should still emit at INFO detail");
			assertEquals(Level.INFO, oneRecord.getLevel());
			assertEquals(Level.INFO, twoRecord.getLevel());
			assertTrue(oneRecord.getMessage().contains("---Request Headers---"), oneRecord.getMessage());
			assertFalse(twoRecord.getMessage().contains("---Request Headers---"), twoRecord.getMessage());
		} finally {
			parentState.restore(parent);
			opOneState.restore(opOne);
			opTwoState.restore(opTwo);
		}
	}
}
