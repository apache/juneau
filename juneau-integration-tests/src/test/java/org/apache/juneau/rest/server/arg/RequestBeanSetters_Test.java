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
package org.apache.juneau.rest.server.arg;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.http.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Tests the {@link Request @Request} bean setter check: a concrete bean with a getter that has no matching setter, or
 * no public no-arg constructor, fails when the {@link RequestBeanArg} is built with an {@link IllegalArgumentException}
 * instead of surfacing a {@code NoSuchMethodException} only on a request that happens to supply the missing parameter.
 *
 * <p>
 * {@code RestOpContext} checks each {@code @Request} parameter when it is constructed, so a broken bean fails when the
 * resource's op table is assembled: at startup with {@code @Rest(eagerInit="true")}, otherwise on the first request
 * (which then fails for every request, not just those supplying the affected parameter).
 */
class RequestBeanSetters_Test extends TestBase {

	private static RequestBeanArg createArg(Class<?> resource, String method) throws Exception {
		for (var m : resource.getDeclaredMethods())
			if (m.getName().equals(method))
				return RequestBeanArg.create(ParameterInfo.of(m.getParameters()[0]), AnnotationWorkList.create());
		throw new IllegalArgumentException(method);
	}

	private static String rootMessage(Throwable t) {
		var c = t;
		while (c.getCause() != null && c.getCause() != c)
			c = c.getCause();
		return c.getMessage() == null ? "" : c.getMessage();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Registration failures
	//-----------------------------------------------------------------------------------------------------------------

	@Request
	public static class MissingSetter {
		@Query
		public String getFoo() { return null; }
		// Deliberately no setFoo(String).
	}

	@Rest
	public static class A {
		@RestGet
		public String a(MissingSetter q) { return "unreachable"; }
	}

	@Test void a01_missingSetter_failsWhenArgBuilt() {
		var e = assertThrows(IllegalArgumentException.class, () -> createArg(A.class, "a"));
		assertString("Request bean " + MissingSetter.class.getName() + " has @Query getter getFoo() with no matching setter setFoo(String).", e.getMessage());
	}

	@Rest(eagerInit="true")
	public static class AEager {
		@RestGet
		public String a(MissingSetter q) { return "unreachable"; }
	}

	@Test void a01b_missingSetter_failsServletInit() {
		// Before: the lookup was per-request, so only requests supplying ?foo= failed (with a 500).
		var e = assertThrows(RuntimeException.class, () -> MockRestClient.build(AEager.class));
		assertContains("has @Query getter getFoo() with no matching setter setFoo(String).", rootMessage(e));
	}

	@Test void a01c_missingSetter_lazyInit_failsEveryRequest() throws Exception {
		// Without eagerInit the op table is assembled on the first request, so that is where registration fails.
		var c = MockRestClient.create(A.class).ignoreErrors().build();
		c.get("/a").run().assertStatus(500);
		c.get("/a?foo=x").run().assertStatus(500);
	}

	@Rest(eagerInit="true")
	public static class BEager {
		@RestGet
		public String b(NoNoArgCtor q) { return "unreachable"; }
	}

	@Test void a02b_noNoArgConstructor_failsServletInit() {
		var e = assertThrows(RuntimeException.class, () -> MockRestClient.build(BEager.class));
		assertContains("must have a public no-arg constructor.", rootMessage(e));
	}

	@Request
	public static class NoNoArgCtor {
		public NoNoArgCtor(String required) { /* no-op */ }
		@Query
		public String getFoo() { return null; }
		public void setFoo(String v) { /* no-op */ }
	}

	@Rest
	public static class B {
		@RestGet
		public String b(NoNoArgCtor q) { return "unreachable"; }
	}

	@Test void a02_noNoArgConstructor_failsWhenArgBuilt() {
		var e = assertThrows(IllegalArgumentException.class, () -> createArg(B.class, "b"));
		assertString("Request bean " + NoNoArgCtor.class.getName() + " must have a public no-arg constructor.", e.getMessage());
	}

	@Request
	public static class MissingHeaderSetter {
		@Header("X-Foo")
		public Integer getFoo() { return null; }
		public void setFoo(String v) { /* no-op: wrong parameter type */ }
	}

	@Rest
	public static class B2 {
		@RestGet
		public String b2(MissingHeaderSetter q) { return "unreachable"; }
	}

	@Test void a03_wrongSetterType_namesActualPartAnnotation() {
		var e = assertThrows(IllegalArgumentException.class, () -> createArg(B2.class, "b2"));
		assertString("Request bean " + MissingHeaderSetter.class.getName() + " has @Header getter getFoo() with no matching setter setFoo(Integer).", e.getMessage());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Unaffected / valid paths
	//-----------------------------------------------------------------------------------------------------------------

	@Request
	public interface InterfaceBean {
		@Query
		String getFoo();
	}

	@Rest
	public static class C {
		@RestGet
		public String c(InterfaceBean q) { return "foo:" + q.getFoo(); }
	}

	@Test void b01_interfaceBean_unaffected() throws Exception {
		assertNotNull(createArg(C.class, "c"));
		var c = MockRestClient.build(C.class);
		c.get("/c?foo=bar").run().assertContent("foo:bar");
	}

	@Request
	public static class ValidBean {
		private String foo;
		private boolean flag;
		@Query
		public String getFoo() { return foo; }
		public void setFoo(String v) { foo = v; }
		@Query("flag")
		public boolean isFlag() { return flag; }
		public void setFlag(boolean v) { flag = v; }
	}

	@Rest
	public static class D {
		@RestGet
		public String d(ValidBean q) { return "foo:" + q.getFoo() + ",flag:" + q.isFlag(); }
	}

	@Test void b02_validBean_bindsAcrossMultipleRequests() throws Exception {
		// The constructor/setters are resolved once per op and reused across requests.
		var c = MockRestClient.build(D.class);
		c.get("/d?foo=x").run().assertContent("foo:x,flag:false");
		c.get("/d?foo=y&flag=true").run().assertContent("foo:y,flag:true");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RequestBeanSetters directly
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_resolve_newInstanceAndSet() throws Exception {
		var meta = org.apache.juneau.httppart.bean.RequestBeanMeta.create(ValidBean.class, AnnotationWorkList.create());
		var setters = RequestBeanSetters.resolve(ValidBean.class, meta);
		ValidBean bean = setters.newInstance();
		setters.set(bean, meta.getProperty("getFoo").getGetter(), "v");
		setters.set(bean, meta.getProperty("isFlag").getGetter(), true);
		assertBean(bean, "foo,flag", "v,true");
	}

	@Test void c02_resolve_missingSetter_throws() {
		var meta = org.apache.juneau.httppart.bean.RequestBeanMeta.create(MissingSetter.class, AnnotationWorkList.create());
		var e = assertThrows(IllegalArgumentException.class, () -> RequestBeanSetters.resolve(MissingSetter.class, meta));
		assertContains("with no matching setter setFoo(String).", e.getMessage());
	}

	@Test void c03_lazyPath_getRequestByClass_sameMessage() throws Exception {
		var c = MockRestClient.build(E.class);
		c.get("/e?foo=x").run().assertContent("Request bean " + MissingSetter.class.getName() + " has @Query getter getFoo() with no matching setter setFoo(String).");
	}

	@Rest
	public static class E {
		@RestGet
		public String e(RestRequest req) {
			try {
				req.getRequest(MissingSetter.class);
				return "unreachable";
			} catch (RuntimeException ex) {
				return rootMessage(ex);
			}
		}
	}
}
