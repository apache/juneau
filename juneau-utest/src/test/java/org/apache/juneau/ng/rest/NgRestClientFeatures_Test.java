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
package org.apache.juneau.ng.rest;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.ng.http.HttpBody;
import org.apache.juneau.ng.http.entity.*;
import org.apache.juneau.ng.rest.client.*;
import org.apache.juneau.ng.rest.mock.*;
import org.junit.jupiter.api.*;

/**
 * Tests for RestCallInterceptor, RestLogger, RestLogEntry, RestLogLevelResolver,
 * BasicRestLogger, BodyConverter, and related NgRestClient features.
 */
public class NgRestClientFeatures_Test {

	// =================================================================================================================
	// A — RestCallInterceptor
	// =================================================================================================================

	@Test
	void a01_interceptor_onInit_called() throws Exception {
		var initCalled = new AtomicBoolean(false);
		var transport = MockHttpTransport.of(200, "ok");
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.interceptors(new RestCallInterceptor() {
					@Override public void onInit(NgRestRequest req) { initCalled.set(true); }
				})
				.build()) {
			try (var r = client.get("/").run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		assertTrue(initCalled.get(), "onInit should have been called");
	}

	@Test
	void a02_interceptor_onConnect_called() throws Exception {
		var connectStatus = new AtomicInteger(-1);
		var transport = MockHttpTransport.of(201, "created");
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.interceptors(new RestCallInterceptor() {
					@Override public void onConnect(NgRestRequest req, NgRestResponse res) {
						connectStatus.set(res.getStatusCode());
					}
				})
				.build()) {
			try (var r = client.post("/").run()) {
				assertEquals(201, r.getStatusCode());
			}
		}
		assertEquals(201, connectStatus.get(), "onConnect should have been called with status 201");
	}

	@Test
	void a03_interceptor_onClose_called_on_success() throws Exception {
		var closeCalled = new AtomicBoolean(false);
		var transport = MockHttpTransport.of(200, "ok");
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.interceptors(new RestCallInterceptor() {
					@Override public void onClose(NgRestRequest req, NgRestResponse res) {
						closeCalled.set(true);
						assertNotNull(res, "res should be non-null on success");
					}
				})
				.build()) {
			try (var r = client.get("/").run()) {}
		}
		assertTrue(closeCalled.get(), "onClose should have been called");
	}

	@Test
	void a04_interceptor_onClose_called_on_transport_error() throws Exception {
		var closeCalled = new AtomicBoolean(false);
		var responseOnClose = new AtomicReference<NgRestResponse>();
		var transport = MockHttpTransport.builder()
			.fallback(req -> { throw new TransportException("simulated network failure"); })
			.build();
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.interceptors(new RestCallInterceptor() {
					@Override public void onClose(NgRestRequest req, NgRestResponse res) {
						closeCalled.set(true);
						responseOnClose.set(res);
					}
				})
				.build()) {
			assertThrows(TransportException.class, () -> client.get("/").run());
		}
		assertTrue(closeCalled.get(), "onClose should be called even on transport error");
		assertNull(responseOnClose.get(), "res should be null when transport failed before response");
	}

	@Test
	void a05_interceptor_onInit_canModifyRequest() throws Exception {
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.interceptors(new RestCallInterceptor() {
					@Override public void onInit(NgRestRequest req) {
						req.header("X-Intercepted", "yes");
					}
				})
				.build()) {
			try (var r = client.get("/").run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		var req = transport.getRecordedRequests().get(0);
		assertEquals("yes", req.getFirstHeader("X-Intercepted").value());
	}

	@Test
	void a06_multipleInterceptors_calledInOrder() throws Exception {
		var order = new ArrayList<String>();
		var transport = MockHttpTransport.of(200, "ok");
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.interceptors(
					new RestCallInterceptor() {
						@Override public void onInit(NgRestRequest req) { order.add("init-1"); }
						@Override public void onConnect(NgRestRequest req, NgRestResponse res) { order.add("connect-1"); }
						@Override public void onClose(NgRestRequest req, NgRestResponse res) { order.add("close-1"); }
					},
					new RestCallInterceptor() {
						@Override public void onInit(NgRestRequest req) { order.add("init-2"); }
						@Override public void onConnect(NgRestRequest req, NgRestResponse res) { order.add("connect-2"); }
						@Override public void onClose(NgRestRequest req, NgRestResponse res) { order.add("close-2"); }
					})
				.build()) {
			try (var r = client.get("/").run()) {}
		}
		assertEquals(List.of("init-1", "init-2", "connect-1", "connect-2", "close-1", "close-2"), order);
	}

	@Test
	void a07_interceptor_onClose_throwsIsSwallowed() throws Exception {
		// onClose exceptions should be swallowed, not propagated
		var transport = MockHttpTransport.of(200, "ok");
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.interceptors(new RestCallInterceptor() {
					@Override public void onClose(NgRestRequest req, NgRestResponse res) throws Exception {
						throw new RuntimeException("close error should be swallowed");
					}
				})
				.build()) {
			// Should not throw despite onClose exception
			try (var r = client.get("/").run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
	}

	// =================================================================================================================
	// B — RestLogger
	// =================================================================================================================

	@Test
	void b01_logger_called_on_success() throws Exception {
		var entries = new ArrayList<RestLogEntry>();
		var transport = MockHttpTransport.of(200, "ok");
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.logger(entries::add)
				.build()) {
			try (var r = client.get("/api").run()) {}
		}
		assertEquals(1, entries.size());
		var entry = entries.get(0);
		assertNotNull(entry.getRequest());
		assertNotNull(entry.getResponse());
		assertNull(entry.getError());
		assertEquals("GET", entry.getRequest().getMethod());
		assertEquals(200, entry.getStatusCode());
		assertFalse(entry.isError());
		assertFalse(entry.isDebug());
		assertEquals(System.Logger.Level.INFO, entry.getLevel());
	}

	@Test
	void b02_logger_called_on_transport_error() throws Exception {
		var entries = new ArrayList<RestLogEntry>();
		var transport = MockHttpTransport.builder()
			.fallback(req -> { throw new TransportException("simulated failure"); })
			.build();
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.logger(entries::add)
				.build()) {
			assertThrows(TransportException.class, () -> client.get("/api").run());
		}
		assertEquals(1, entries.size());
		var entry = entries.get(0);
		assertNotNull(entry.getError());
		assertNull(entry.getResponse());
		assertEquals(0, entry.getStatusCode());
		assertTrue(entry.isError());
		assertEquals(System.Logger.Level.ERROR, entry.getLevel());
	}

	@Test
	void b03_logger_status400_is_warning() throws Exception {
		var entries = new ArrayList<RestLogEntry>();
		var transport = MockHttpTransport.of(404, "Not Found");
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.logger(entries::add)
				.build()) {
			try (var r = client.get("/missing").run()) {}
		}
		assertEquals(1, entries.size());
		assertEquals(System.Logger.Level.WARNING, entries.get(0).getLevel());
		assertTrue(entries.get(0).isError());
	}

	@Test
	void b04_logger_status500_is_error() throws Exception {
		var entries = new ArrayList<RestLogEntry>();
		var transport = MockHttpTransport.of(500, "Internal Server Error");
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.logger(entries::add)
				.build()) {
			try (var r = client.get("/crash").run()) {}
		}
		assertEquals(1, entries.size());
		assertEquals(System.Logger.Level.ERROR, entries.get(0).getLevel());
	}

	@Test
	void b05_debug_flag_set_on_entry() throws Exception {
		var entries = new ArrayList<RestLogEntry>();
		var transport = MockHttpTransport.of(200, "ok");
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.logger(entries::add)
				.build()) {
			try (var r = client.get("/").debug().run()) {}
		}
		assertTrue(entries.get(0).isDebug());
	}

	@Test
	void b06_logger_null_no_logging() throws Exception {
		// Without logger, run() should complete normally
		var transport = MockHttpTransport.of(200, "ok");
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.build()) {
			try (var r = client.get("/").run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
	}

	// =================================================================================================================
	// C — RestLogEntry.format()
	// =================================================================================================================

	@Test
	void c01_format_default_success() throws Exception {
		var entries = new ArrayList<RestLogEntry>();
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder().statusCode(200).reasonPhrase("OK").build())
			.build();
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.logger(entries::add)
				.build()) {
			try (var r = client.get("/users").run()) {}
		}
		var fmt = entries.get(0).format();
		assertTrue(fmt.contains("GET"), "format should contain method: " + fmt);
		assertTrue(fmt.contains("200"), "format should contain status: " + fmt);
		assertTrue(fmt.contains("ms"), "format should contain elapsed: " + fmt);
	}

	@Test
	void c02_format_with_template() throws Exception {
		var entries = new ArrayList<RestLogEntry>();
		var transport = MockHttpTransport.of(200, "OK");
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://example.com")
				.logger(entries::add)
				.build()) {
			try (var r = client.get("/items").run()) {}
		}
		var entry = entries.get(0);
		assertEquals("GET", entry.format("{method}"));
		assertTrue(entry.format("{uri}").contains("/items"));
		assertEquals("200", entry.format("{status}"));
		assertTrue(entry.format("{elapsed}").endsWith("ms"));
		assertEquals("", entry.format("{error}"));
	}

	@Test
	void c03_format_error_template() throws Exception {
		var entries = new ArrayList<RestLogEntry>();
		var transport = MockHttpTransport.builder()
			.fallback(req -> { throw new TransportException("connection refused"); })
			.build();
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.logger(entries::add)
				.build()) {
			assertThrows(TransportException.class, () -> client.get("/").run());
		}
		var entry = entries.get(0);
		assertTrue(entry.format("{error}").contains("connection refused"));
	}

	@Test
	void c04_hasResponseHeader() throws Exception {
		var entries = new ArrayList<RestLogEntry>();
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder()
				.statusCode(200)
				.header("X-Custom", "value")
				.build())
			.build();
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.logger(entries::add)
				.build()) {
			try (var r = client.get("/").run()) {}
		}
		var entry = entries.get(0);
		assertTrue(entry.hasResponseHeader("X-Custom"));
		assertFalse(entry.hasResponseHeader("X-Missing"));
	}

	@Test
	void c05_hasResponseHeader_noResponse() throws Exception {
		var entries = new ArrayList<RestLogEntry>();
		var transport = MockHttpTransport.builder()
			.fallback(req -> { throw new TransportException("fail"); })
			.build();
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.logger(entries::add)
				.build()) {
			assertThrows(TransportException.class, () -> client.get("/").run());
		}
		assertFalse(entries.get(0).hasResponseHeader("Anything"));
	}

	@Test
	void c06_getUri_set_after_run() throws Exception {
		var entries = new ArrayList<RestLogEntry>();
		var transport = MockHttpTransport.of(200, "ok");
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.logger(entries::add)
				.build()) {
			try (var r = client.get("/path").run()) {}
		}
		var uri = entries.get(0).getRequest().getUri();
		assertNotNull(uri);
		assertTrue(uri.toString().contains("/path"));
	}

	// =================================================================================================================
	// D — RestLogLevelResolver
	// =================================================================================================================

	@Test
	void d01_default_resolver_info_for_2xx() {
		var entry = stubEntry(200, null);
		assertEquals(System.Logger.Level.INFO, RestLogLevelResolver.DEFAULT.resolve(entry));
	}

	@Test
	void d02_default_resolver_warning_for_4xx() {
		var entry = stubEntry(404, null);
		assertEquals(System.Logger.Level.WARNING, RestLogLevelResolver.DEFAULT.resolve(entry));
	}

	@Test
	void d03_default_resolver_error_for_5xx() {
		var entry = stubEntry(500, null);
		assertEquals(System.Logger.Level.ERROR, RestLogLevelResolver.DEFAULT.resolve(entry));
	}

	@Test
	void d04_default_resolver_error_for_transport_error() {
		var entry = stubEntry(0, new TransportException("fail"));
		assertEquals(System.Logger.Level.ERROR, RestLogLevelResolver.DEFAULT.resolve(entry));
	}

	@Test
	void d05_custom_resolver_via_rules() {
		var resolver = RestLogLevelResolver.rules()
			.rule(System.Logger.Level.ERROR, e -> e.getStatusCode() >= 500)
			.rule(System.Logger.Level.WARNING, e -> e.getStatusCode() >= 400)
			.defaultLevel(System.Logger.Level.DEBUG)
			.build();
		assertEquals(System.Logger.Level.DEBUG, resolver.resolve(stubEntry(200, null)));
		assertEquals(System.Logger.Level.WARNING, resolver.resolve(stubEntry(400, null)));
		assertEquals(System.Logger.Level.ERROR, resolver.resolve(stubEntry(503, null)));
	}

	@Test
	void d06_default_resolver_warning_for_thrown_header() throws Exception {
		var entries = new ArrayList<RestLogEntry>();
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder()
				.statusCode(200)
				.header("Thrown", "com.example.MyException;message")
				.build())
			.build();
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.logger(entries::add)
				.build()) {
			try (var r = client.get("/").run()) {}
		}
		// Thrown header present with 200 → WARNING
		assertEquals(System.Logger.Level.WARNING, entries.get(0).getLevel());
	}

	// =================================================================================================================
	// E — BasicRestLogger
	// =================================================================================================================

	@Test
	void e01_basicRestLogger_of_logs_info() throws Exception {
		var messages = new ArrayList<String>();
		var jdkLogger = new System.Logger() {
			@Override public String getName() { return "test"; }
			@Override public boolean isLoggable(Level level) { return true; }
			@Override public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) { messages.add(level + ":" + msg); }
			@Override public void log(Level level, ResourceBundle bundle, String format, Object... params) { messages.add(level + ":" + format); }
		};
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder().statusCode(200).reasonPhrase("OK").build())
			.build();
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.logger(BasicRestLogger.of(jdkLogger))
				.build()) {
			try (var r = client.get("/test").run()) {}
		}
		assertEquals(1, messages.size());
		assertTrue(messages.get(0).startsWith("INFO:"), "Should log at INFO level: " + messages.get(0));
		assertTrue(messages.get(0).contains("200"), "Message should contain status: " + messages.get(0));
	}

	@Test
	void e02_basicRestLogger_logs_error_with_exception() throws Exception {
		var messages = new ArrayList<String>();
		var throwables = new ArrayList<Throwable>();
		var jdkLogger = new System.Logger() {
			@Override public String getName() { return "test"; }
			@Override public boolean isLoggable(Level level) { return true; }
			@Override public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
				messages.add(level + ":" + msg);
				throwables.add(thrown);
			}
			@Override public void log(Level level, ResourceBundle bundle, String format, Object... params) { messages.add(level + ":" + format); }
		};
		var transport = MockHttpTransport.builder()
			.fallback(req -> { throw new TransportException("network error"); })
			.build();
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.logger(BasicRestLogger.of(jdkLogger))
				.build()) {
			assertThrows(TransportException.class, () -> client.get("/fail").run());
		}
		assertEquals(1, messages.size());
		assertTrue(messages.get(0).startsWith("ERROR:"), "Should log at ERROR level: " + messages.get(0));
		assertNotNull(throwables.get(0), "Throwable should be included on ERROR");
	}

	@Test
	void e03_basicRestLogger_filter_suppresses_entry() throws Exception {
		var messages = new ArrayList<String>();
		var jdkLogger = new System.Logger() {
			@Override public String getName() { return "test"; }
			@Override public boolean isLoggable(Level level) { return true; }
			@Override public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) { messages.add(msg); }
			@Override public void log(Level level, ResourceBundle bundle, String format, Object... params) { messages.add(format); }
		};
		var transport = MockHttpTransport.of(200, "ok");
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.logger(BasicRestLogger.create()
					.logger(jdkLogger)
					.filter(e -> false) // filter all
					.build())
				.build()) {
			try (var r = client.get("/").run()) {}
		}
		assertTrue(messages.isEmpty(), "Filtered logger should log nothing");
	}

	@Test
	void e04_basicRestLogger_customTemplates() throws Exception {
		var messages = new ArrayList<String>();
		var jdkLogger = new System.Logger() {
			@Override public String getName() { return "test"; }
			@Override public boolean isLoggable(Level level) { return true; }
			@Override public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) { messages.add(msg); }
			@Override public void log(Level level, ResourceBundle bundle, String format, Object... params) { messages.add(format); }
		};
		var transport = MockHttpTransport.of(200, "ok");
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.logger(BasicRestLogger.create()
					.logger(jdkLogger)
					.infoTemplate("CUSTOM:{method}:{status}")
					.build())
				.build()) {
			try (var r = client.get("/").run()) {}
		}
		assertEquals(1, messages.size());
		assertTrue(messages.get(0).startsWith("CUSTOM:GET:200"), "Expected custom template: " + messages.get(0));
	}

	@Test
	void e05_basicRestLogger_debug_entry_uses_debugTemplate() throws Exception {
		var messages = new ArrayList<String>();
		var jdkLogger = new System.Logger() {
			@Override public String getName() { return "test"; }
			@Override public boolean isLoggable(Level level) { return true; }
			@Override public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) { messages.add(level + ":" + msg); }
			@Override public void log(Level level, ResourceBundle bundle, String format, Object... params) { messages.add(level + ":" + format); }
		};
		var transport = MockHttpTransport.of(200, "ok");
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.logger(BasicRestLogger.create()
					.logger(jdkLogger)
					.debugTemplate("DBG:{method}")
					.build())
				.build()) {
			try (var r = client.get("/").debug().run()) {}
		}
		assertEquals(1, messages.size());
		assertTrue(messages.get(0).startsWith("DEBUG:DBG:GET"), "Expected debug template: " + messages.get(0));
	}

	@Test
	void e06_basicRestLogger_warning_level() throws Exception {
		var messages = new ArrayList<String>();
		var jdkLogger = new System.Logger() {
			@Override public String getName() { return "test"; }
			@Override public boolean isLoggable(Level level) { return true; }
			@Override public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) { messages.add(level + ":" + msg); }
			@Override public void log(Level level, ResourceBundle bundle, String format, Object... params) { messages.add(level + ":" + format); }
		};
		var transport = MockHttpTransport.of(404, "Not Found");
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.logger(BasicRestLogger.create()
					.logger(jdkLogger)
					.warningTemplate("WARN:{status}")
					.build())
				.build()) {
			try (var r = client.get("/missing").run()) {}
		}
		assertEquals(1, messages.size());
		assertTrue(messages.get(0).startsWith("WARNING:WARN:404"), "Expected warning template: " + messages.get(0));
	}

	@Test
	void e07_basicRestLogger_customLevelResolver() throws Exception {
		var messages = new ArrayList<String>();
		var jdkLogger = new System.Logger() {
			@Override public String getName() { return "test"; }
			@Override public boolean isLoggable(Level level) { return true; }
			@Override public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) { messages.add(level + ":" + msg); }
			@Override public void log(Level level, ResourceBundle bundle, String format, Object... params) { messages.add(level + ":" + format); }
		};
		var transport = MockHttpTransport.of(200, "ok");
		var alwaysError = RestLogLevelResolver.rules()
			.defaultLevel(System.Logger.Level.ERROR)
			.build();
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.logger(BasicRestLogger.create()
					.logger(jdkLogger)
					.levelResolver(alwaysError)
					.build())
				.build()) {
			try (var r = client.get("/").run()) {}
		}
		assertEquals(1, messages.size());
		assertTrue(messages.get(0).startsWith("ERROR:"), "Should log at ERROR even for 200: " + messages.get(0));
	}

	// =================================================================================================================
	// F — BodyConverter + body(Object)
	// =================================================================================================================

	@Test
	void f01_body_HttpBody_passthrough() throws Exception {
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var r = client.post("/").body(StringBody.of("hello", "text/plain")).run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		var req = transport.getRecordedRequests().get(0);
		var baos = new ByteArrayOutputStream();
		req.getBody().writeTo(baos);
		assertEquals("hello", baos.toString(StandardCharsets.UTF_8));
		assertEquals("text/plain", req.getFirstHeader("Content-Type").value());
	}

	@Test
	void f02_body_Object_HttpBody_converter() throws Exception {
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var r = client.post("/").body((Object)StringBody.of("via-object", "text/plain")).run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		var req = transport.getRecordedRequests().get(0);
		var baos = new ByteArrayOutputStream();
		req.getBody().writeTo(baos);
		assertEquals("via-object", baos.toString(StandardCharsets.UTF_8));
	}

	@Test
	void f03_body_InputStream_converter() throws Exception {
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var r = client.post("/")
					.body(new ByteArrayInputStream("stream-content".getBytes(StandardCharsets.UTF_8)))
					.run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		var req = transport.getRecordedRequests().get(0);
		var baos = new ByteArrayOutputStream();
		req.getBody().writeTo(baos);
		assertEquals("stream-content", baos.toString(StandardCharsets.UTF_8));
	}

	@Test
	void f04_body_byteArray_converter() throws Exception {
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		var bytes = "byte-content".getBytes(StandardCharsets.UTF_8);
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var r = client.post("/").body(bytes).run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		var req = transport.getRecordedRequests().get(0);
		var baos = new ByteArrayOutputStream();
		req.getBody().writeTo(baos);
		assertEquals("byte-content", baos.toString(StandardCharsets.UTF_8));
	}

	@Test
	void f05_body_File_converter() throws Exception {
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		var tempFile = java.io.File.createTempFile("ng-test-", ".txt");
		tempFile.deleteOnExit();
		java.nio.file.Files.writeString(tempFile.toPath(), "file-content");

		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var r = client.post("/").body(tempFile).run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		var req = transport.getRecordedRequests().get(0);
		var baos = new ByteArrayOutputStream();
		req.getBody().writeTo(baos);
		assertEquals("file-content", baos.toString(StandardCharsets.UTF_8));
	}

	@Test
	void f06_body_Object_null_clears_body() throws Exception {
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var r = client.post("/").body((Object)null).run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		assertNull(transport.getRecordedRequests().get(0).getBody());
	}

	@Test
	void f07_body_noConverter_throws() throws Exception {
		var transport = MockHttpTransport.of(200, "ok");
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.bodyConverters() // empty converter list
				.build()) {
			assertThrows(IllegalArgumentException.class, () -> client.post("/").body("no-converter").run());
		}
	}

	@Test
	void f08_body_customConverter_prepended() throws Exception {
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		// Custom converter for Integer → "int:<value>"
		var converter = BodyConverter.of(Integer.class,
			i -> TransportBody.of(StringBody.of("int:" + i, "text/plain")));
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.bodyConverter(converter)
				.build()) {
			try (var r = client.post("/").body(42).run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		var req = transport.getRecordedRequests().get(0);
		var baos = new ByteArrayOutputStream();
		req.getBody().writeTo(baos);
		assertEquals("int:42", baos.toString(StandardCharsets.UTF_8));
	}

	@Test
	void f09_bodyConverters_replacesDefaults() throws Exception {
		var transport = MockHttpTransport.of(200, "ok");
		// Replace all converters — InputStream no longer handled
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.bodyConverters() // no converters
				.build()) {
			assertThrows(IllegalArgumentException.class,
				() -> client.post("/").body(new ByteArrayInputStream(new byte[0])).run());
		}
	}

	// =================================================================================================================
	// G — bodyString()
	// =================================================================================================================

	@Test
	void g01_bodyString_sends_raw_text() throws Exception {
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var r = client.post("/").bodyString("raw text body").run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		var req = transport.getRecordedRequests().get(0);
		var baos = new ByteArrayOutputStream();
		req.getBody().writeTo(baos);
		assertEquals("raw text body", baos.toString(StandardCharsets.UTF_8));
		assertEquals("text/plain", req.getFirstHeader("Content-Type").value());
	}

	@Test
	void g02_bodyString_null_clears_body() throws Exception {
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var r = client.post("/").bodyString(null).run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		assertNull(transport.getRecordedRequests().get(0).getBody());
	}

	// =================================================================================================================
	// H — BodyConverter.of() static factory
	// =================================================================================================================

	@Test
	void h01_bodyConverter_of_factory() throws IOException {
		var converter = BodyConverter.of(String.class,
			s -> TransportBody.of(StringBody.of("wrapped:" + s, "text/plain")));
		assertTrue(converter.canConvert("hello"));
		assertFalse(converter.canConvert(42));
		assertFalse(converter.canConvert(null));
		var body = converter.convert("test");
		assertNotNull(body);
		assertEquals("text/plain", body.getContentType());
		var baos = new ByteArrayOutputStream();
		body.writeTo(baos);
		assertEquals("wrapped:test", baos.toString(StandardCharsets.UTF_8));
	}

	@Test
	void h02_bodyConverter_of_throwingConverter() {
		var converter = BodyConverter.of(String.class, s -> { throw new IOException("conversion failed"); });
		assertTrue(converter.canConvert("hello"));
		assertThrows(IOException.class, () -> converter.convert("hello"));
	}

	// =================================================================================================================
	// I — RestCallInterceptor default methods (no-ops)
	// =================================================================================================================

	@Test
	void i01_interceptor_defaultMethods_areNoOps() throws Exception {
		// Default implementations should not throw
		var interceptor = new RestCallInterceptor() {};
		var transport = MockHttpTransport.of(200, "ok");
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.interceptors(interceptor)
				.build()) {
			try (var r = client.get("/").run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
	}

	// =================================================================================================================
	// J — NgRestClient DEFAULT_BODY_CONVERTERS
	// =================================================================================================================

	@Test
	void j01_defaultBodyConverters_canConvert_httpBody() {
		var converters = NgRestClient.DEFAULT_BODY_CONVERTERS;
		assertTrue(converters.stream().anyMatch(c -> c.canConvert(StringBody.of("x"))));
	}

	@Test
	void j02_defaultBodyConverters_canConvert_inputStream() {
		var converters = NgRestClient.DEFAULT_BODY_CONVERTERS;
		assertTrue(converters.stream().anyMatch(c -> c.canConvert(new ByteArrayInputStream(new byte[0]))));
	}

	@Test
	void j03_defaultBodyConverters_canConvert_byteArray() {
		var converters = NgRestClient.DEFAULT_BODY_CONVERTERS;
		assertTrue(converters.stream().anyMatch(c -> c.canConvert(new byte[0])));
	}

	@Test
	void j04_defaultBodyConverters_canConvert_file() {
		var converters = NgRestClient.DEFAULT_BODY_CONVERTERS;
		assertTrue(converters.stream().anyMatch(c -> c.canConvert(new java.io.File("x"))));
	}

	@Test
	void j05_defaultBodyConverters_cannot_convert_string() {
		var converters = NgRestClient.DEFAULT_BODY_CONVERTERS;
		assertFalse(converters.stream().anyMatch(c -> c.canConvert("hello")));
	}

	// =================================================================================================================
	// K — Additional coverage tests
	// =================================================================================================================

	@Test
	void k01_basicRestLogger_filter_allows_entry() throws Exception {
		// When filter is non-null and returns TRUE, the entry IS logged
		var messages = new ArrayList<String>();
		var jdkLogger = new System.Logger() {
			@Override public String getName() { return "test"; }
			@Override public boolean isLoggable(Level level) { return true; }
			@Override public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) { messages.add(msg); }
			@Override public void log(Level level, ResourceBundle bundle, String format, Object... params) { messages.add(format); }
		};
		var transport = MockHttpTransport.of(200, "ok");
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.logger(BasicRestLogger.create()
					.logger(jdkLogger)
					.filter(e -> true) // filter allows all entries
					.build())
				.build()) {
			try (var r = client.get("/").run()) {}
		}
		// With filter returning true, entry IS logged
		assertEquals(1, messages.size(), "Entry should be logged when filter returns true");
	}

	@Test
	void k02_restLogEntry_error_with_null_message() throws Exception {
		// Error with null message falls back to class simple name
		var entries = new ArrayList<RestLogEntry>();
		var transport = MockHttpTransport.builder()
			.fallback(req -> { throw new TransportException((String)null); })
			.build();
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.logger(entries::add)
				.build()) {
			assertThrows(TransportException.class, () -> client.get("/").run());
		}
		var entry = entries.get(0);
		// {error} should use class simple name when getMessage() is null
		var errorText = entry.format("{error}");
		assertTrue(errorText.contains("TransportException"), "Error text should contain class name when message is null: " + errorText);
	}

	@Test
	void k03_ngRestResponse_getHeaders() throws Exception {
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder()
				.statusCode(200)
				.header("X-Foo", "bar")
				.header("X-Baz", "qux")
				.build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var r = client.get("/").run()) {
				var headers = r.getHeaders();
				assertTrue(headers.stream().anyMatch(h -> h.name().equals("X-Foo")));
				assertTrue(headers.stream().anyMatch(h -> h.name().equals("X-Baz")));
			}
		}
	}

	@Test
	void k04_ngRestResponse_getBodyStream() throws Exception {
		var transport = MockHttpTransport.of(200, "streamed-body");
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var r = client.get("/").run()) {
				var stream = r.getBodyStream();
				assertNotNull(stream);
				var content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
				assertEquals("streamed-body", content);
			}
		}
	}

	@Test
	void k05_body_convertedBody_nullContentType_skipsContentTypeHeader() throws Exception {
		// A custom converter that returns a TransportBody with null content type
		var transport = MockHttpTransport.builder()
			.recordRequests()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		var nullContentTypeConverter = BodyConverter.of(Integer.class, i -> {
			var body = new HttpBody() {
				@Override public String getContentType() { return null; }
				@Override public long getContentLength() { return 0; }
				@Override public void writeTo(java.io.OutputStream out) {}
				@Override public boolean isRepeatable() { return true; }
			};
			return TransportBody.of(body);
		});
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.bodyConverter(nullContentTypeConverter)
				.build()) {
			try (var r = client.post("/").body(42).run()) {
				assertEquals(200, r.getStatusCode());
			}
		}
		// Content-Type header should NOT be set when converter returns null content type
		assertNull(transport.getRecordedRequests().get(0).getFirstHeader("Content-Type"));
	}

	@Test
	void k06_transportRequest_builder_uri_string() {
		var req = TransportRequest.builder()
			.method("GET")
			.uri("http://example.com/path")
			.build();
		assertEquals("http://example.com/path", req.getUri().toString());
	}

	@Test
	void k07_transportRequest_builder_uri_string_invalid() {
		assertThrows(IllegalArgumentException.class, () ->
			TransportRequest.builder()
				.method("GET")
				.uri("not a valid \\uri")
				.build());
	}

	@Test
	void k08_transportRequest_builder_headers_collection() {
		var headers = List.of(TransportHeader.of("X-Foo", "bar"), TransportHeader.of("X-Baz", "qux"));
		var req = TransportRequest.builder()
			.method("GET")
			.uri("http://example.com/")
			.headers(headers)
			.build();
		assertNotNull(req.getFirstHeader("X-Foo"));
		assertNotNull(req.getFirstHeader("X-Baz"));
	}

	// =================================================================================================================
	// L — ResponseBody and ResponseHeader
	// =================================================================================================================

	@Test
	void l01_responseBody_asString() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "hello world")).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				assertEquals("hello world", resp.body().asString());
			}
		}
	}

	@Test
	void l02_responseBody_asBytes() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "hi")).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				var bytes = resp.body().asBytes();
				assertNotNull(bytes);
				assertEquals("hi", new String(bytes, StandardCharsets.UTF_8));
			}
		}
	}

	@Test
	void l03_responseBody_asStream() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "stream")).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				var stream = resp.body().asStream();
				assertNotNull(stream);
				assertEquals("stream", new String(stream.readAllBytes(), StandardCharsets.UTF_8));
			}
		}
	}

	@Test
	void l04_responseBody_nullBody() throws Exception {
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder().statusCode(204).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				assertNull(resp.body().asString());
				assertNull(resp.body().asBytes());
				assertNull(resp.body().asStream());
				assertNull(resp.body().readAllBytes());
			}
		}
	}

	@Test
	void l05_responseBody_asString_charset() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "hello")).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				assertEquals("hello", resp.body().asString(StandardCharsets.UTF_8));
			}
		}
	}

	@Test
	void l06_responseHeader_present() throws Exception {
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder()
				.statusCode(200)
				.header("X-My-Header", "my-value")
				.build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				var h = resp.header("X-My-Header");
				assertEquals("X-My-Header", h.getName());
				assertTrue(h.isPresent());
				assertEquals("my-value", h.getValue());
				assertEquals("my-value", h.orElse("default"));
				assertEquals(Optional.of("my-value"), h.asOptional());
			}
		}
	}

	@Test
	void l07_responseHeader_absent() throws Exception {
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder().statusCode(200).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				var h = resp.header("X-Missing");
				assertFalse(h.isPresent());
				assertNull(h.getValue());
				assertEquals("default", h.orElse("default"));
				assertEquals(Optional.empty(), h.asOptional());
				assertEquals(-1, h.asInteger());
				assertEquals(-1L, h.asLong());
				assertTrue(h.getValues().isEmpty());
				assertTrue(h.asCsvList().isEmpty());
				assertEquals("X-Missing: <absent>", h.toString());
			}
		}
	}

	@Test
	void l08_responseHeader_asInteger() throws Exception {
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder()
				.statusCode(200)
				.header("Content-Length", "1024")
				.build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				assertEquals(1024, resp.header("Content-Length").asInteger());
			}
		}
	}

	@Test
	void l09_responseHeader_asInteger_notANumber() throws Exception {
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder()
				.statusCode(200)
				.header("X-Value", "not-a-number")
				.build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				assertEquals(-1, resp.header("X-Value").asInteger());
				assertEquals(-1L, resp.header("X-Value").asLong());
			}
		}
	}

	@Test
	void l10_responseHeader_asCsvList() throws Exception {
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder()
				.statusCode(200)
				.header("Allow", "GET, POST, PUT")
				.build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				var tokens = resp.header("Allow").asCsvList();
				assertEquals(List.of("GET", "POST", "PUT"), tokens);
			}
		}
	}

	@Test
	void l11_responseHeader_toString_present() throws Exception {
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder()
				.statusCode(200)
				.header("X-Value", "test")
				.build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				assertEquals("X-Value: test", resp.header("X-Value").toString());
			}
		}
	}

	@Test
	void l12_responseHeader_asLong() throws Exception {
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder()
				.statusCode(200)
				.header("Content-Length", "999999999999")
				.build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				assertEquals(999999999999L, resp.header("Content-Length").asLong());
			}
		}
	}

	@Test
	void l13_httpTransportBuilder_interface() throws IOException {
		HttpTransportBuilder builder = () -> MockHttpTransport.of(200, "ok");
		try (var transport = builder.build()) {
			assertNotNull(transport);
		}
	}

	// =================================================================================================================
	// M — ResponseAssertion, ResponseHeaderAssertion, ResponseBodyAssertion
	// =================================================================================================================

	@Test
	void m01_assertThat_statusCode() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "ok")).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				resp.assertThat().statusCode(200);
			}
		}
	}

	@Test
	void m02_assertThat_statusCode_fails() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(404, "not found")).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				assertThrows(AssertionError.class, () -> resp.assertThat().statusCode(200));
			}
		}
	}

	@Test
	void m03_assertThat_isOk() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(201, "created")).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				resp.assertThat().isOk();
			}
		}
	}

	@Test
	void m04_assertThat_isOk_fails() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(404, "")).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				assertThrows(AssertionError.class, () -> resp.assertThat().isOk());
			}
		}
	}

	@Test
	void m05_assertThat_isClientError() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(404, "")).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				resp.assertThat().isClientError();
			}
		}
	}

	@Test
	void m06_assertThat_isServerError() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(503, "")).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				resp.assertThat().isServerError();
			}
		}
	}

	@Test
	void m07_assertThat_isClientError_fails() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "")).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				assertThrows(AssertionError.class, () -> resp.assertThat().isClientError());
			}
		}
	}

	@Test
	void m08_assertThat_isServerError_fails() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "")).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				assertThrows(AssertionError.class, () -> resp.assertThat().isServerError());
			}
		}
	}

	@Test
	void m09_assertThat_header_isPresent() throws Exception {
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder().statusCode(200).header("X-Foo", "bar").build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				resp.assertThat().header("X-Foo").isPresent();
			}
		}
	}

	@Test
	void m10_assertThat_header_isPresent_fails() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "")).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				assertThrows(AssertionError.class, () -> resp.assertThat().header("X-Missing").isPresent());
			}
		}
	}

	@Test
	void m11_assertThat_header_isAbsent() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "")).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				resp.assertThat().header("X-Missing").isAbsent();
			}
		}
	}

	@Test
	void m12_assertThat_header_isAbsent_fails() throws Exception {
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder().statusCode(200).header("X-Foo", "bar").build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				assertThrows(AssertionError.class, () -> resp.assertThat().header("X-Foo").isAbsent());
			}
		}
	}

	@Test
	void m13_assertThat_header_equals() throws Exception {
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder().statusCode(200).header("X-Val", "hello").build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				resp.assertThat().header("X-Val").equals("hello");
			}
		}
	}

	@Test
	void m14_assertThat_header_equals_fails() throws Exception {
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder().statusCode(200).header("X-Val", "hello").build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				assertThrows(AssertionError.class, () -> resp.assertThat().header("X-Val").equals("world"));
			}
		}
	}

	@Test
	void m15_assertThat_header_contains() throws Exception {
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder().statusCode(200).header("Content-Type", "application/json; charset=UTF-8").build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				resp.assertThat().header("Content-Type").contains("application/json");
			}
		}
	}

	@Test
	void m16_assertThat_header_startsWith() throws Exception {
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder().statusCode(200).header("Content-Type", "application/json").build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				resp.assertThat().header("Content-Type").startsWith("application");
			}
		}
	}

	@Test
	void m17_assertThat_header_integerEquals() throws Exception {
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder().statusCode(200).header("Content-Length", "42").build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				resp.assertThat().header("Content-Length").integerEquals(42);
			}
		}
	}

	@Test
	void m18_assertThat_body_equals() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "hello")).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				resp.assertThat().body().equals("hello");
			}
		}
	}

	@Test
	void m19_assertThat_body_contains() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "hello world")).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				resp.assertThat().body().contains("world");
			}
		}
	}

	@Test
	void m20_assertThat_body_isEmpty() throws Exception {
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder().statusCode(204).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				resp.assertThat().body().isEmpty();
			}
		}
	}

	@Test
	void m21_assertThat_body_isNotEmpty() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "content")).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				resp.assertThat().body().isNotEmpty();
			}
		}
	}

	@Test
	void m22_assertThat_body_startsWith() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "{\"id\":1}")).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				resp.assertThat().body().startsWith("{");
			}
		}
	}

	@Test
	void m23_assertThat_and_returnsResponse() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "ok")).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				var assertion = resp.assertThat();
				assertSame(resp, assertion.and());
			}
		}
	}

	@Test
	void m24_assertThat_header_and_returnsParent() throws Exception {
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder().statusCode(200).header("X-Foo", "bar").build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				var responseAssertion = resp.assertThat();
				var headerAssertion = responseAssertion.header("X-Foo");
				assertSame(responseAssertion, headerAssertion.and());
			}
		}
	}

	@Test
	void m25_assertThat_chained() throws Exception {
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "application/json")
				.build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				resp.assertThat()
					.statusCode(200)
					.isOk()
					.header("Content-Type").contains("json").and()
					.body().isEmpty();
			}
		}
	}

	@Test
	void m26_responseHeaderAssertion_integerEquals_missingHeader_throws() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "")).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				assertThrows(AssertionError.class, () -> resp.assertThat().header("X-Missing").integerEquals(42));
			}
		}
	}

	@Test
	void m27_responseHeaderAssertion_integerEquals_notNumber_throws() throws Exception {
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder().statusCode(200).header("X-Val", "abc").build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				assertThrows(AssertionError.class, () -> resp.assertThat().header("X-Val").integerEquals(42));
			}
		}
	}

	@Test
	void m28_responseBodyAssertion_contains_fails() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "hello")).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				assertThrows(AssertionError.class, () -> resp.assertThat().body().contains("world"));
			}
		}
	}

	@Test
	void m29_responseBodyAssertion_isNotEmpty_fails() throws Exception {
		var transport = MockHttpTransport.builder()
			.fallback(req -> TransportResponse.builder().statusCode(204).build())
			.build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				assertThrows(AssertionError.class, () -> resp.assertThat().body().isNotEmpty());
			}
		}
	}

	@Test
	void m30_responseBodyAssertion_and_returnsParent() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "ok")).rootUrl("http://x.com").build()) {
			try (var resp = client.get("/").run()) {
				var responseAssertion = resp.assertThat();
				var bodyAssertion = responseAssertion.body();
				assertSame(responseAssertion, bodyAssertion.and());
			}
		}
	}

	// =================================================================================================================
	// Helpers
	// =================================================================================================================

	/** Creates a stub RestLogEntry by making a real mock call and capturing the logger entry. */
	private static RestLogEntry stubEntry(int status, Throwable error) {
		var entries = new ArrayList<RestLogEntry>();
		MockHttpTransport transport;
		if (error != null) {
			transport = MockHttpTransport.builder()
				.fallback(req -> { throw new TransportException(error.getMessage()); })
				.build();
		} else {
			transport = MockHttpTransport.builder()
				.fallback(req -> TransportResponse.builder().statusCode(status).build())
				.build();
		}
		try (var client = NgRestClient.builder()
				.transport(transport)
				.rootUrl("http://x.com")
				.logger(entries::add)
				.build()) {
			try {
				try (var r = client.get("/").run()) {}
			} catch (Exception ignored) {}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return entries.get(0);
	}
}
