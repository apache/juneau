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
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.microservice.console.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;

/**
 * Tests for {@link JettyMicroservice.Builder} methods.
 */
class JettyMicroservice_Builder_Test extends TestBase {

	@Test void a01_create() {
		var b = JettyMicroservice.create();
		assertNotNull(b);
	}

	@Test void a02_getInstance_noInstance() {
		assertDoesNotThrow(() -> JettyMicroservice.getInstance());
	}

	@Test void a03_builder_args_array() {
		// Just verify the call doesn't throw - field is package-private in parent
		assertDoesNotThrow(() -> JettyMicroservice.create().args("--port", "8080"));
	}

	@Test void a04_builder_configName() {
		// Just verify fluent return works - field is package-private in parent
		var b = JettyMicroservice.create().configName("myapp.cfg");
		assertNotNull(b);
	}

	@Test void a05_builder_consoleEnabled() {
		var b = JettyMicroservice.create().consoleEnabled(true);
		assertNotNull(b);
	}

	@Test void a06_builder_consoleCommands() {
		var cmd = new ExitCommand();
		var b = JettyMicroservice.create().consoleCommands(cmd);
		assertNotNull(b);
	}

	@Test void a07_builder_listener() {
		var listener = new BasicJettyMicroserviceListener();
		var b = JettyMicroservice.create().listener(listener);
		// listener2 is accessible from within the jetty package
		assertSame(listener, b.listener2);
	}

	@Test void a08_builder_jettyServerFactory() {
		var factory = new BasicJettyServerFactory();
		var b = JettyMicroservice.create().jettyServerFactory(factory);
		assertSame(factory, b.factory);
	}

	@Test void a09_builder_ports() {
		var b = JettyMicroservice.create().ports(8080, 8443);
		assertArrayEquals(new int[]{8080, 8443}, b.ports);
	}

	@Test void a10_builder_servlet_instance() {
		var servlet = new MockServlet();
		var b = JettyMicroservice.create().servlet(servlet, "/test");
		assertTrue(b.servlets.containsKey("/test"));
		assertSame(servlet, b.servlets.get("/test"));
	}

	@Test void a11_builder_servletAttribute_nameValue() {
		var b = JettyMicroservice.create().servletAttribute("key1", "value1");
		assertEquals("value1", b.servletAttributes.get("key1"));
	}

	@Test void a12_builder_servletAttribute_map() {
		var b = JettyMicroservice.create().servletAttribute(Map.of("key2", "value2", "key3", "value3"));
		assertEquals("value2", b.servletAttributes.get("key2"));
		assertEquals("value3", b.servletAttributes.get("key3"));
	}

	@Test void a13_builder_servletAttribute_nullMap() {
		var b = JettyMicroservice.create().servletAttribute(null);
		assertTrue(b.servletAttributes.isEmpty());
	}

	@Test void a14_builder_servlets_map() {
		var servlet = new MockServlet();
		var b = JettyMicroservice.create().servlets(Map.of("/path", servlet));
		assertSame(servlet, b.servlets.get("/path"));
	}

	@Test void a15_builder_servlets_nullMap() {
		var b = JettyMicroservice.create().servlets(null);
		assertTrue(b.servlets.isEmpty());
	}

	@Test void a16_builder_workingDir_file() {
		assertDoesNotThrow(() -> JettyMicroservice.create().workingDir(new File("/tmp")));
	}

	@Test void a17_builder_workingDir_string() {
		assertDoesNotThrow(() -> JettyMicroservice.create().workingDir("/tmp"));
	}

	@Test void a18_builder_copy() {
		var b = JettyMicroservice.create()
			.ports(9090)
			.servletAttribute("attr1", "val1");
		var copy = b.copy();
		assertNotSame(b, copy);
		assertArrayEquals(new int[]{9090}, copy.ports);
		assertEquals("val1", copy.servletAttributes.get("attr1"));
	}

	@Test void a19_builder_jettyXml_reader() throws Exception {
		var xml = "<Configure />";
		var b = JettyMicroservice.create().jettyXml(new StringReader(xml), false);
		assertEquals(xml, b.jettyXml);
		assertEquals(Boolean.FALSE, b.jettyXmlResolveVars);
	}

	@Test void a20_builder_jettyXml_inputStream() throws Exception {
		var xml = "<Configure />";
		var b = JettyMicroservice.create().jettyXml(new ByteArrayInputStream(xml.getBytes()), false);
		assertEquals(xml, b.jettyXml);
	}

	@Test void a21_builder_jettyXml_invalidType() {
		assertThrows(RuntimeException.class, () -> JettyMicroservice.create().jettyXml(42, false));
	}

	@Test void a22_builder_jettyXml_resolveVars_true() throws Exception {
		var xml = "<Configure />";
		var b = JettyMicroservice.create().jettyXml(new StringReader(xml), true);
		assertEquals(Boolean.TRUE, b.jettyXmlResolveVars);
	}

	@Test void a23_builder_console() {
		var reader = new java.util.Scanner(System.in);
		var writer = new PrintWriter(System.out);
		assertDoesNotThrow(() -> JettyMicroservice.create().console(reader, writer));
	}

	@Test void a24_builder_logger() {
		assertDoesNotThrow(() -> JettyMicroservice.create().logger(java.util.logging.Logger.getLogger("test")));
	}

	@Test void a25_builder_manifest_null() {
		assertDoesNotThrow(() -> JettyMicroservice.create().manifest((Object)null));
	}

	// Minimal no-op servlet for testing.
	static class MockServlet implements Servlet {
		@Override public void init(ServletConfig config) {}
		@Override public ServletConfig getServletConfig() { return null; }
		@Override public void service(ServletRequest req, ServletResponse res) {}
		@Override public String getServletInfo() { return "mock"; }
		@Override public void destroy() {}
	}
}
