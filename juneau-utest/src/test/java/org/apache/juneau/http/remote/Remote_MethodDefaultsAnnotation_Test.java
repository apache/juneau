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
package org.apache.juneau.http.remote;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

/**
 * Tests for method-level default values on remote proxy interfaces.
 * 
 * @since 9.2.0
 */
class Remote_MethodDefaultsAnnotation_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// @Header defaults on methods
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A implements BasicJson5Config {
		@RestGet(path="/x1")
		public String x1(@Header("Foo") String foo, @Header("Bar") String bar) {
			return "Foo=" + foo + ",Bar=" + bar;
		}
	}

	@Remote
	public interface A1 {
		@RemoteGet("/x1")
		@Header(name="Foo", def="defaultFoo")
		String x1(@Header("Foo") String foo, @Header("Bar") String bar);
	}

	@Test
	void a01_headerDefaults_providedValue() {
		var x = MockRestClient.buildJson(A.class).getRemote(A1.class);
		assertEquals("Foo=customFoo,Bar=customBar", x.x1("customFoo", "customBar"));
	}

	@Test
	void a02_headerDefaults_nullValue() {
		var x = MockRestClient.buildJson(A.class).getRemote(A1.class);
		assertEquals("Foo=defaultFoo,Bar=customBar", x.x1(null, "customBar"));
	}

	@Test
	void a03_headerDefaults_bothNull() {
		var x = MockRestClient.buildJson(A.class).getRemote(A1.class);
		assertEquals("Foo=defaultFoo,Bar=null", x.x1(null, null));
	}

	@Remote
	public interface A2 {
		@RemoteGet("/x1")
		@Header(name="Foo", def="defaultFoo")
		@Header(name="Bar", def="defaultBar")
		String x1(@Header("Foo") String foo, @Header("Bar") String bar);
	}

	@Test
	void a04_headerDefaults_multipleDefaults() {
		var x = MockRestClient.buildJson(A.class).getRemote(A2.class);
		assertEquals("Foo=defaultFoo,Bar=defaultBar", x.x1(null, null));
		assertEquals("Foo=customFoo,Bar=defaultBar", x.x1("customFoo", null));
		assertEquals("Foo=defaultFoo,Bar=customBar", x.x1(null, "customBar"));
		assertEquals("Foo=customFoo,Bar=customBar", x.x1("customFoo", "customBar"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Query defaults on methods
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B implements BasicJson5Config {
		@RestGet(path="/x1")
		public String x1(@Query("foo") String foo, @Query("bar") String bar) {
			return "foo=" + foo + ",bar=" + bar;
		}
	}

	@Remote
	public interface B1 {
		@RemoteGet("/x1")
		@Query(name="foo", def="defaultFoo")
		String x1(@Query("foo") String foo, @Query("bar") String bar);
	}

	@Test
	void b01_queryDefaults_providedValue() {
		var x = MockRestClient.buildJson(B.class).getRemote(B1.class);
		assertEquals("foo=customFoo,bar=customBar", x.x1("customFoo", "customBar"));
	}

	@Test
	void b02_queryDefaults_nullValue() {
		var x = MockRestClient.buildJson(B.class).getRemote(B1.class);
		assertEquals("foo=defaultFoo,bar=customBar", x.x1(null, "customBar"));
	}

	@Remote
	public interface B2 {
		@RemoteGet("/x1")
		@Query(name="foo", def="defaultFoo")
		@Query(name="bar", def="defaultBar")
		String x1(@Query("foo") String foo, @Query("bar") String bar);
	}

	@Test
	void b03_queryDefaults_multipleDefaults() {
		var x = MockRestClient.buildJson(B.class).getRemote(B2.class);
		assertEquals("foo=defaultFoo,bar=defaultBar", x.x1(null, null));
		assertEquals("foo=customFoo,bar=defaultBar", x.x1("customFoo", null));
		assertEquals("foo=defaultFoo,bar=customBar", x.x1(null, "customBar"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @FormData defaults on methods
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C implements BasicJson5Config {
		@RestPost(path="/x1")
		public String x1(@FormData("foo") String foo, @FormData("bar") String bar) {
			return "foo=" + foo + ",bar=" + bar;
		}
	}

	@Remote
	public interface C1 {
		@RemotePost("/x1")
		@FormData(name="foo", def="defaultFoo")
		String x1(@FormData("foo") String foo, @FormData("bar") String bar);
	}

	@Test
	void c01_formDataDefaults_providedValue() {
		var x = MockRestClient.buildJson(C.class).getRemote(C1.class);
		assertEquals("foo=customFoo,bar=customBar", x.x1("customFoo", "customBar"));
	}

	@Test
	void c02_formDataDefaults_nullValue() {
		var x = MockRestClient.buildJson(C.class).getRemote(C1.class);
		assertEquals("foo=defaultFoo,bar=customBar", x.x1(null, "customBar"));
	}

	@Remote
	public interface C2 {
		@RemotePost("/x1")
		@FormData(name="foo", def="defaultFoo")
		@FormData(name="bar", def="defaultBar")
		String x1(@FormData("foo") String foo, @FormData("bar") String bar);
	}

	@Test
	void c03_formDataDefaults_multipleDefaults() {
		var x = MockRestClient.buildJson(C.class).getRemote(C2.class);
		assertEquals("foo=defaultFoo,bar=defaultBar", x.x1(null, null));
		assertEquals("foo=customFoo,bar=defaultBar", x.x1("customFoo", null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Path defaults on methods
	// NOTE: Path variables with nulls cause validation errors, so we test with actual provided values only
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D implements BasicJson5Config {
		@RestGet(path="/x1/{foo}/{bar}")
		public String x1(@Path("foo") String foo, @Path("bar") String bar) {
			return "foo=" + foo + ",bar=" + bar;
		}
	}

	@Remote
	public interface D1 {
		@RemoteGet("/x1/{foo}/{bar}")
		@Path(name="foo", def="defaultFoo")
		String x1(@Path("foo") String foo, @Path("bar") String bar);
	}

	@Test
	void d01_pathDefaults_providedValue() {
		var x = MockRestClient.buildJson(D.class).getRemote(D1.class);
		assertEquals("foo=customFoo,bar=customBar", x.x1("customFoo", "customBar"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Content defaults on methods
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class E implements BasicJson5Config {
		@RestPost(path="/x1")
		public String x1(@Content String content) {
			return "content=" + content;
		}
	}

	@Remote
	public interface E1 {
		@RemotePost("/x1")
		@Content(def="{foo:'defaultBar'}")
		String x1(@Content String content);
	}

	@Test
	void e01_contentDefaults_providedValue() {
		var x = MockRestClient.buildJson(E.class).getRemote(E1.class);
		assertEquals("content={foo:'customBar'}", x.x1("{foo:'customBar'}"));
	}

	@Test
	void e02_contentDefaults_nullValue() {
		var x = MockRestClient.buildJson(E.class).getRemote(E1.class);
		assertEquals("content={foo:'defaultBar'}", x.x1(null));
	}

	@Remote
	public interface E2 {
		@RemotePost("/x1")
		@Content(def="{foo:'defaultBar'}")
		String x1();
	}

	@Test
	void e03_contentDefaults_noParameter() {
		var x = MockRestClient.buildJson(E.class).getRemote(E2.class);
		assertEquals("content={foo:'defaultBar'}", x.x1());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Combined defaults test (without @Path since nulls cause validation errors)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class F implements BasicJson5Config {
		@RestPost(path="/x1")
		public String x1(
			@Query("queryParam") String queryParam,
			@Header("HeaderParam") String headerParam,
			@Content String content
		) {
			return "queryParam=" + queryParam 
				+ ",HeaderParam=" + headerParam 
				+ ",content=" + content;
		}
	}

	@Remote
	public interface F1 {
		@RemotePost("/x1")
		@Query(name="queryParam", def="defaultQuery")
		@Header(name="HeaderParam", def="defaultHeader")
		@Content(def="defaultContent")
		String x1(
			@Query("queryParam") String queryParam,
			@Header("HeaderParam") String headerParam,
			@Content String content
		);
	}

	@Test
	void f01_combinedDefaults_allNull() {
		var x = MockRestClient.buildJson(F.class).getRemote(F1.class);
		assertEquals(
			"queryParam=defaultQuery,HeaderParam=defaultHeader,content=defaultContent",
			x.x1(null, null, null)
		);
	}

	@Test
	void f02_combinedDefaults_allProvided() {
		var x = MockRestClient.buildJson(F.class).getRemote(F1.class);
		assertEquals(
			"queryParam=customQuery,HeaderParam=customHeader,content=customContent",
			x.x1("customQuery", "customHeader", "customContent")
		);
	}

	@Test
	void f03_combinedDefaults_mixed() {
		var x = MockRestClient.buildJson(F.class).getRemote(F1.class);
		assertEquals(
			"queryParam=defaultQuery,HeaderParam=customHeader,content=customContent",
			x.x1(null, "customHeader", "customContent")
		);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parameter-level defaults (9.2.0)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class G implements BasicJson5Config {
		@RestGet(path="/x1")
		public String x1(
			@Header("Foo") String foo,
			@Query("bar") String bar
		) {
			return "Foo=" + foo + ",bar=" + bar;
		}
	}

	@Remote
	public interface G1 {
		@RemoteGet("/x1")
		String x1(
			@Header(name="Foo", def="paramDefaultFoo") String foo,
			@Query(name="bar", def="paramDefaultBar") String bar
		);
	}

	@Test
	void g01_parameterDefaults_bothNull() {
		var x = MockRestClient.buildJson(G.class).getRemote(G1.class);
		assertEquals("Foo=paramDefaultFoo,bar=paramDefaultBar", x.x1(null, null));
	}

	@Test
	void g02_parameterDefaults_oneProvided() {
		var x = MockRestClient.buildJson(G.class).getRemote(G1.class);
		assertEquals("Foo=customFoo,bar=paramDefaultBar", x.x1("customFoo", null));
		assertEquals("Foo=paramDefaultFoo,bar=customBar", x.x1(null, "customBar"));
	}

	@Test
	void g03_parameterDefaults_bothProvided() {
		var x = MockRestClient.buildJson(G.class).getRemote(G1.class);
		assertEquals("Foo=customFoo,bar=customBar", x.x1("customFoo", "customBar"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parameter-level defaults take precedence over method-level defaults
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class H implements BasicJson5Config {
		@RestGet(path="/x1")
		public String x1(@Query("param") String param) {
			return "param=" + param;
		}
	}

	@Remote
	public interface H1 {
		@RemoteGet("/x1")
		@Query(name="param", def="methodDefault")
		String x1(@Query(name="param", def="paramDefault") String param);
	}

	@Test
	void h01_parameterOverridesMethod_nullValue() {
		var x = MockRestClient.buildJson(H.class).getRemote(H1.class);
		// Parameter-level default should take precedence
		assertEquals("param=paramDefault", x.x1(null));
	}

	@Test
	void h02_parameterOverridesMethod_providedValue() {
		var x = MockRestClient.buildJson(H.class).getRemote(H1.class);
		assertEquals("param=customValue", x.x1("customValue"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Content parameter-level defaults
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class I implements BasicJson5Config {
		@RestPost(path="/x1")
		public String x1(@Content String content) {
			return "content=" + content;
		}
	}

	@Remote
	public interface I1 {
		@RemotePost("/x1")
		String x1(@Content(def="{paramDefault:true}") String content);
	}

	@Test
	void i01_contentParameterDefault_nullValue() {
		var x = MockRestClient.buildJson(I.class).getRemote(I1.class);
		assertEquals("content={paramDefault:true}", x.x1(null));
	}

	@Test
	void i02_contentParameterDefault_providedValue() {
		var x = MockRestClient.buildJson(I.class).getRemote(I1.class);
		assertEquals("content={custom:true}", x.x1("{custom:true}"));
	}

	// Test that parameter-level Content default overrides method-level
	@Remote
	public interface I2 {
		@RemotePost("/x1")
		@Content(def="{methodDefault:true}")
		String x1(@Content(def="{paramDefault:true}") String content);
	}

	@Test
	void i03_contentParameterOverridesMethod() {
		var x = MockRestClient.buildJson(I.class).getRemote(I2.class);
		assertEquals("content={paramDefault:true}", x.x1(null));
	}
}

