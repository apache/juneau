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
package org.apache.juneau.rest.mock.classic;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;

/**
 * Coverage tests for {@link MockRestRequest} — exercises the fluent setters that aren't
 * already hit by the existing {@code MockServletRequest_Coverage_Test} tests.  The
 * existing tests already cover roughly half of the setters via {@code applyOverrides};
 * here we exercise the remaining setters by building a request and either inspecting
 * state via the {@code getXxx()} accessor or running the request to confirm the call
 * doesn't blow up.
 */
@SuppressWarnings({
	"java:S1130" // Test methods use the project-standard broad 'throws Exception' signature.
})
class MockRestRequest_Test extends TestBase {

	@Rest
	public static class A {
		@RestGet
		public String get() {
			return "ok";
		}

		@RestPost
		public String post() {
			return "posted";
		}
	}

	private static MockRestClient client() {
		return MockRestClient.create(A.class).build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// A. Attribute fluent setters.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_attribute_singleEntry() throws Exception {
		var req = client().get("/").attribute("foo", "bar");
		assertEquals("bar", req.getAttributeMap().get("foo"));
		req.run().assertStatus(200);
	}

	@Test void a02_attributes_replacesMap() throws Exception {
		var req = client().get("/")
			.attribute("preExisting", "x")
			.attributes(Map.of("k1", "v1", "k2", "v2"));
		var map = req.getAttributeMap();
		assertFalse(map.containsKey("preExisting"));
		assertEquals("v1", map.get("k1"));
		assertEquals("v2", map.get("k2"));
		req.run().assertStatus(200);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B. Form / query / path data setters.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_formDataPairs() throws Exception {
		client().post("/").formDataPairs("a", "1", "b", "2").run().assertStatus(200);
	}

	@Test void b02_queryDataPairs() throws Exception {
		client().get("/").queryDataPairs("a", "1", "b", "2").run().assertStatus(200);
	}

	@Test void b03_queryDataBean() throws Exception {
		client().get("/").queryDataBean(new Bean("x", "y")).run().assertStatus(200);
	}

	@Test void b04_pathDataBean() throws Exception {
		// pathDataBean accepts a bean to substitute path variables.  We don't run() since the
		// route doesn't have matching {placeholder} segments; calling the fluent setter is
		// sufficient for coverage of the override.
		var req = client().get("/").pathDataBean(new Bean("x", "y"));
		assertNotNull(req);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// C. Header setters that aren't already covered.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_headerPairs() throws Exception {
		client().get("/").headerPairs("X-A", "1", "X-B", "2").run().assertStatus(200);
	}

	@Test void c02_headersBean() throws Exception {
		// headersBean takes a bean whose properties become headers.  An empty bean is fine.
		client().get("/").headersBean(new Bean("x", "y")).run().assertStatus(200);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// D. Serialization-mode fluent setters.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_json5() throws Exception {
		// json5() flips serialization options.  Just call it; running would 406 since the
		// resource doesn't declare a JSON5 serializer.
		var req = client().get("/").json5();
		assertNotNull(req);
	}

	@Test void d02_bson() throws Exception {
		// bson() flips serialization options.  Don't run() since the resource doesn't
		// declare a BSON serializer.
		var req = client().get("/").bson();
		assertNotNull(req);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// E. Session-options fluent setters.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_serializerSessionOptionsHeader_string() throws Exception {
		client().get("/").serializerSessionOptionsHeader("{}").run().assertStatus(200);
	}

	@Test void e02_serializerSessionOptionsQuery_string() throws Exception {
		client().get("/").serializerSessionOptionsQuery("()").run().assertStatus(200);
	}

	@Test void e02b_serializerSessionOptionsQuery_map() throws Exception {
		client().get("/").serializerSessionOptionsQuery(Map.of("k", "v")).run().assertStatus(200);
	}

	@Test void e02c_serializerSessionOptionsHeader_map() throws Exception {
		client().get("/").serializerSessionOptionsHeader(Map.of("k", "v")).run().assertStatus(200);
	}

	@Test void e03_parserSessionOptionsHeader_string() throws Exception {
		client().get("/").parserSessionOptionsHeader("{}").run().assertStatus(200);
	}

	@Test void e04_parserSessionOptionsHeader_map() throws Exception {
		client().get("/").parserSessionOptionsHeader(Map.of("k", "v")).run().assertStatus(200);
	}

	@Test void e05_parserSessionOptionsQuery_string() throws Exception {
		client().get("/").parserSessionOptionsQuery("()").run().assertStatus(200);
	}

	@Test void e06_parserSessionOptionsQuery_map() throws Exception {
		client().get("/").parserSessionOptionsQuery(Map.of("k", "v")).run().assertStatus(200);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// F. Servlet-side fluent setters that record state on the MockRestRequest.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_servletContext_recordsValue() throws Exception {
		var req = client().get("/").servletContext((ServletContext) null);
		assertNull(req.getServletContext());
	}

	@Test void f01b_servletContext_appliedToServletRequest() throws Exception {
		// Build a non-null ServletContext stub so applyOverrides() actually copies it
		// onto the underlying MockServletRequest (covers MockServletRequest lines 180-181).
		var stub = (ServletContext) Proxy.newProxyInstance(
			Thread.currentThread().getContextClassLoader(),
			new Class<?>[]{ServletContext.class},
			(proxy, method, args) -> {
				if ("toString".equals(method.getName())) return "stub-servlet-context";
				if ("hashCode".equals(method.getName())) return 0;
				if ("equals".equals(method.getName())) return proxy == args[0];
				return null;
			});
		client().get("/").servletContext(stub).run().assertStatus(200);
	}

	@Test void f02_role_replacesRolesWithSingle() throws Exception {
		var req = client().get("/").role("ROLE_ADMIN");
		assertArrayEquals(new String[]{"ROLE_ADMIN"}, req.getRoles());
		req.run().assertStatus(200);
	}

	@Test void f03_requestDispatcher_storesByPath() throws Exception {
		var dispatcher = new RequestDispatcher() {
			@Override public void forward(ServletRequest r, ServletResponse s) { /* no-op */ }
			@Override public void include(ServletRequest r, ServletResponse s) { /* no-op */ }
		};
		var req = client().get("/").requestDispatcher("/path", dispatcher);
		assertSame(dispatcher, req.getRequestDispatcherMap().get("/path"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// G. Simple bean used by header/query/path-data tests.
	//-----------------------------------------------------------------------------------------------------------------

	public static class Bean {
		public String foo;
		public String bar;

		public Bean() { /* required for bean introspection */ }

		public Bean(String foo, String bar) {
			this.foo = foo;
			this.bar = bar;
		}
	}
}
