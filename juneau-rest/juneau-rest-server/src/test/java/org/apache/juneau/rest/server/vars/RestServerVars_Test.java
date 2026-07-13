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
package org.apache.juneau.rest.server.vars;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.swagger.*;
import org.apache.juneau.bean.swagger.Swagger;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.marshall.cp.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.httppart.*;
import org.junit.jupiter.api.*;

/**
 * Branch-coverage unit tests for the SVL variable classes in {@code org.apache.juneau.rest.server.vars}.
 *
 * <p>
 * Each variable class exposes a {@code canResolve(VarResolverSession)} method whose two branches
 * (bean present / bean absent) can be exercised without a live HTTP request by supplying or
 * withholding the required bean(s) in a {@link BasicBeanStore}-backed {@link VarResolverSession}.
 *
 * @since 10.0.0
 */
class RestServerVars_Test extends TestBase {

	// -----------------------------------------------------------------------------------------
	// Helper: session factory
	// -----------------------------------------------------------------------------------------

	private static VarResolverSession sessionWith(Object... beans) {
		var store = new BasicBeanStore();
		for (var b : beans)
			addToStore(store, b);
		return VarResolver.DEFAULT.createSession(store);
	}

	@SuppressWarnings({
		"unchecked", // Generic cast is safe: b.getClass() matches b's runtime type
		"resource"   // addBean returns 'this'; no new resource is allocated
	})
	private static <T> void addToStore(WritableBeanStore store, T b) {
		store.addBean((Class<T>) b.getClass(), b);
	}

	private static VarResolverSession emptySession() {
		return VarResolver.DEFAULT.createSession(new BasicBeanStore());
	}

	// -----------------------------------------------------------------------------------------
	// Helper: deep-stub RestRequest with pre-seeded stubs for tricky chains
	//
	// Uses RETURNS_DEEP_STUBS so arbitrary method chains produce mocks rather than null.
	// Two extra stubs prevent NPE in chains Mockito cannot handle automatically:
	//   • req.getContext() → ctx (RETURNS_DEEP_STUBS) with getResourceClass() → String.class
	//   • req.getAttributes().get(any()) → attr with asString() → Optional.empty()
	// -----------------------------------------------------------------------------------------

	private static RestRequest deepStubReq() {
		var req = mock(RestRequest.class, RETURNS_DEEP_STUBS);
		var ctx = mock(RestContext.class, RETURNS_DEEP_STUBS);
		doReturn(String.class).when(ctx).getResourceClass();
		when(req.getContext()).thenReturn(ctx);
		var attr = mock(RequestAttribute.class);
		when(attr.asString()).thenReturn(oe());
		when(req.getAttributes().get(any())).thenReturn(attr);
		return req;
	}

	// -----------------------------------------------------------------------------------------
	// a — FileVar ($F)
	// -----------------------------------------------------------------------------------------

	@Test void a01_fileVar_nameConstant() {
		assertEquals("F", FileVar.NAME);
	}

	@Test void a02_fileVar_canResolve_falseWhenNoRestRequest() {
		assertFalse(new FileVar().canResolve(emptySession()));
	}

	@Test void a03_fileVar_canResolve_trueWhenRestRequestPresent() {
		assertTrue(new FileVar().canResolve(sessionWith(mock(RestRequest.class))));
	}

	@Test void a04_fileVar_resolve_returnsNullWhenFileNotFound() throws Exception {
		var req = deepStubReq();
		when(req.getContext().getStaticFiles().getString(any(), any())).thenReturn(oe());
		assertNull(new FileVar().resolve(sessionWith(req), "missing.txt"));
	}

	@Test void a05_fileVar_resolve_plainTextUnchanged() throws Exception {
		var req = deepStubReq();
		when(req.getContext().getStaticFiles().getString(any(), any())).thenReturn(o("plain content"));
		assertEquals("plain content", new FileVar().resolve(sessionWith(req), "test.txt"));
	}

	@Test void a06_fileVar_resolve_stripsHtmlComments() throws Exception {
		var req = deepStubReq();
		when(req.getContext().getStaticFiles().getString(any(), any()))
			.thenReturn(o("<!-- comment -->content"));
		assertEquals("content", new FileVar().resolve(sessionWith(req), "test.html"));
	}

	@Test void a07_fileVar_resolve_stripsXhtmlComments() throws Exception {
		var req = deepStubReq();
		when(req.getContext().getStaticFiles().getString(any(), any()))
			.thenReturn(o("<!-- x -->body"));
		assertEquals("body", new FileVar().resolve(sessionWith(req), "test.xhtml"));
	}

	@Test void a08_fileVar_resolve_stripsXmlComments() throws Exception {
		var req = deepStubReq();
		when(req.getContext().getStaticFiles().getString(any(), any()))
			.thenReturn(o("<!-- x -->body"));
		assertEquals("body", new FileVar().resolve(sessionWith(req), "test.xml"));
	}

	@Test void a09_fileVar_resolve_stripsJsonBlockComments() throws Exception {
		var req = deepStubReq();
		when(req.getContext().getStaticFiles().getString(any(), any()))
			.thenReturn(o("/* comment */content"));
		assertEquals("content", new FileVar().resolve(sessionWith(req), "test.json"));
	}

	@Test void a10_fileVar_resolve_stripsJavascriptComments() throws Exception {
		var req = deepStubReq();
		when(req.getContext().getStaticFiles().getString(any(), any()))
			.thenReturn(o("/* comment */content"));
		assertEquals("content", new FileVar().resolve(sessionWith(req), "test.javascript"));
	}

	@Test void a11_fileVar_resolve_stripsCssComments() throws Exception {
		var req = deepStubReq();
		when(req.getContext().getStaticFiles().getString(any(), any()))
			.thenReturn(o("/* comment */content"));
		assertEquals("content", new FileVar().resolve(sessionWith(req), "test.css"));
	}

	// -----------------------------------------------------------------------------------------
	// b — LocalizationVar ($L)
	// -----------------------------------------------------------------------------------------

	@Test void b01_localizationVar_nameConstant() {
		assertEquals("L", LocalizationVar.NAME);
	}

	@Test void b02_localizationVar_canResolve_falseWhenNeitherPresent() {
		assertFalse(new LocalizationVar().canResolve(emptySession()));
	}

	@Test void b03_localizationVar_canResolve_trueWhenMessagesPresent() {
		var messages = mock(Messages.class);
		assertTrue(new LocalizationVar().canResolve(sessionWith(messages)));
	}

	@Test void b04_localizationVar_canResolve_trueWhenRestRequestPresent() {
		var req = mock(RestRequest.class);
		assertTrue(new LocalizationVar().canResolve(sessionWith(req)));
	}

	@Test void b05_localizationVar_resolve_emptyArgsReturnsEmptyString() {
		assertEquals("", new LocalizationVar().resolve(sessionWith(mock(RestRequest.class)), new String[0]));
	}

	@Test void b06_localizationVar_resolve_usesRestRequestMessages() {
		var req = mock(RestRequest.class);
		var messages = mock(Messages.class);
		when(req.getMessages()).thenReturn(messages);
		when(messages.getString(eq("key"), any(Object[].class))).thenReturn("value");
		assertEquals("value", new LocalizationVar().resolve(sessionWith(req), new String[]{"key"}));
	}

	@SuppressWarnings("resource")
	@Test void b07_localizationVar_resolve_fallsBackToMessagesBean_whenReqMessagesNull() {
		var req = mock(RestRequest.class);
		when(req.getMessages()).thenReturn(null);
		var messages = mock(Messages.class);
		when(messages.getString(eq("key"), any(Object[].class))).thenReturn("fallback");
		try (var store = new BasicBeanStore().addBean(RestRequest.class, req).addBean(Messages.class, messages)) {
			var session = VarResolver.DEFAULT.createSession(store);
			assertEquals("fallback", new LocalizationVar().resolve(session, new String[]{"key"}));
		}
	}

	@Test void b08_localizationVar_resolve_withMultipleArgs() {
		var req = mock(RestRequest.class);
		var messages = mock(Messages.class);
		when(req.getMessages()).thenReturn(messages);
		when(messages.getString(eq("hello"), any(Object[].class))).thenReturn("hello world");
		assertEquals("hello world", new LocalizationVar().resolve(sessionWith(req), new String[]{"hello", "world"}));
	}

	// -----------------------------------------------------------------------------------------
	// c — RequestPathVar ($RP)
	// -----------------------------------------------------------------------------------------

	@Test void c01_requestPathVar_nameConstant() {
		assertEquals("RP", RequestPathVar.NAME);
	}

	@Test void c02_requestPathVar_canResolve_falseWhenNoRestRequest() {
		assertFalse(new RequestPathVar().canResolve(emptySession()));
	}

	@Test void c03_requestPathVar_canResolve_trueWhenRestRequestPresent() {
		assertTrue(new RequestPathVar().canResolve(sessionWith(mock(RestRequest.class))));
	}

	@Test void c04_requestPathVar_resolve_remainderKeyPresent() {
		var req = mock(RestRequest.class, RETURNS_DEEP_STUBS);
		var rem = mock(RequestPathParam.class);
		when(rem.orElse(null)).thenReturn("/a/b/c");
		when(req.getPathParams().getRemainder()).thenReturn(rem);
		assertEquals("/a/b/c", new RequestPathVar().resolve(sessionWith(req), "REMAINDER"));
	}

	@Test void c05_requestPathVar_resolve_remainderKeyAbsent() {
		var req = mock(RestRequest.class, RETURNS_DEEP_STUBS);
		var rem = mock(RequestPathParam.class);
		when(rem.orElse(null)).thenReturn(null);
		when(req.getPathParams().getRemainder()).thenReturn(rem);
		assertNull(new RequestPathVar().resolve(sessionWith(req), "REMAINDER"));
	}

	@Test void c06_requestPathVar_resolve_regularKeyFound() {
		var req = mock(RestRequest.class);
		var pp = mock(RequestPathParam.class);
		when(req.getPathParam("id")).thenReturn(pp);
		when(pp.orElse(null)).thenReturn("42");
		assertEquals("42", new RequestPathVar().resolve(sessionWith(req), "id"));
	}

	@Test void c07_requestPathVar_resolve_regularKeyNotFound() {
		var req = mock(RestRequest.class);
		var pp = mock(RequestPathParam.class);
		when(req.getPathParam("id")).thenReturn(pp);
		when(pp.orElse(null)).thenReturn(null);
		assertNull(new RequestPathVar().resolve(sessionWith(req), "id"));
	}

	// -----------------------------------------------------------------------------------------
	// d — RequestVar ($R)
	// -----------------------------------------------------------------------------------------

	@Test void d01_requestVar_nameConstant() {
		assertEquals("R", RequestVar.NAME);
	}

	@Test void d02_requestVar_canResolve_falseWhenNoRestRequest() {
		assertFalse(new RequestVar().canResolve(emptySession()));
	}

	@Test void d03_requestVar_canResolve_trueWhenRestRequestPresent() {
		assertTrue(new RequestVar().canResolve(sessionWith(mock(RestRequest.class))));
	}

	@Test void d04_requestVar_resolve_authorityPath() {
		var req = deepStubReq();
		when(req.getAuthorityPath()).thenReturn("/ctx");
		assertEquals("/ctx", new RequestVar().resolve(sessionWith(req), "authorityPath"));
	}

	@Test void d05_requestVar_resolve_contextPath() {
		var req = deepStubReq();
		when(req.getContextPath()).thenReturn("/app");
		assertEquals("/app", new RequestVar().resolve(sessionWith(req), "contextPath"));
	}

	@Test void d06_requestVar_resolve_method() {
		var req = deepStubReq();
		when(req.getMethod()).thenReturn("POST");
		assertEquals("POST", new RequestVar().resolve(sessionWith(req), "method"));
	}

	@Test void d07_requestVar_resolve_pathInfo() {
		var req = deepStubReq();
		when(req.getPathInfo()).thenReturn("/info");
		assertEquals("/info", new RequestVar().resolve(sessionWith(req), "pathInfo"));
	}

	@Test void d08_requestVar_resolve_requestParentURI() {
		var req = deepStubReq();
		when(req.getUriContext().getRootRelativePathInfoParent()).thenReturn("/parent");
		assertEquals("/parent", new RequestVar().resolve(sessionWith(req), "requestParentURI"));
	}

	@Test void d09_requestVar_resolve_requestURI() {
		var req = deepStubReq();
		when(req.getRequestURI()).thenReturn("/req");
		assertEquals("/req", new RequestVar().resolve(sessionWith(req), "requestURI"));
	}

	@Test void d10_requestVar_resolve_servletClass() {
		var req = deepStubReq();
		// deepStubReq() stubs getContext().getResourceClass() → String.class
		assertEquals("java.lang.String", new RequestVar().resolve(sessionWith(req), "servletClass"));
	}

	@Test void d11_requestVar_resolve_servletClassSimple() {
		var req = deepStubReq();
		assertEquals("String", new RequestVar().resolve(sessionWith(req), "servletClassSimple"));
	}

	@Test void d12_requestVar_resolve_servletParentURI() {
		var req = deepStubReq();
		when(req.getUriContext().getRootRelativeServletPathParent()).thenReturn("/sparent");
		assertEquals("/sparent", new RequestVar().resolve(sessionWith(req), "servletParentURI"));
	}

	@Test void d13_requestVar_resolve_servletPath() {
		var req = deepStubReq();
		when(req.getServletPath()).thenReturn("/servlet");
		assertEquals("/servlet", new RequestVar().resolve(sessionWith(req), "servletPath"));
	}

	@Test void d14_requestVar_resolve_servletURI() {
		var req = deepStubReq();
		when(req.getUriContext().getRootRelativeServletPath()).thenReturn("/suri");
		assertEquals("/suri", new RequestVar().resolve(sessionWith(req), "servletURI"));
	}

	@Test void d15_requestVar_resolve_unknownKeyFallsBackToAttribute() {
		var req = deepStubReq();
		var attr = mock(RequestAttribute.class);
		when(attr.asString()).thenReturn(o("attrval"));
		when(req.getAttributes().get("unknownKey")).thenReturn(attr);
		assertEquals("attrval", new RequestVar().resolve(sessionWith(req), "unknownKey"));
	}

	// -----------------------------------------------------------------------------------------
	// e — SerializedRequestAttrVar ($SA)
	// -----------------------------------------------------------------------------------------

	@Test void e01_serializedRequestAttrVar_nameConstant() {
		assertEquals("SA", SerializedRequestAttrVar.NAME);
	}

	@Test void e02_serializedRequestAttrVar_canResolve_falseWhenNoRestRequest() {
		assertFalse(new SerializedRequestAttrVar().canResolve(emptySession()));
	}

	@Test void e03_serializedRequestAttrVar_canResolve_trueWhenRestRequestPresent() {
		assertTrue(new SerializedRequestAttrVar().canResolve(sessionWith(mock(RestRequest.class))));
	}

	@Test void e04_serializedRequestAttrVar_resolveTo_throwsWhenNoCommaInKey() {
		var req = mock(RestRequest.class);
		assertThrows(Exception.class,
			() -> new SerializedRequestAttrVar().resolveTo(sessionWith(req), new StringWriter(), "noCommaKey"));
	}

	@Test void e05_serializedRequestAttrVar_resolveTo_serializerNotFound_noOutput() throws Exception {
		var req = mock(RestRequest.class, RETURNS_DEEP_STUBS);
		when(req.getAttribute(any())).thenReturn(mock(RequestAttribute.class));
		when(req.getOpContext().getSerializers().getSerializer(any(String.class))).thenReturn(oe());
		var w = new StringWriter();
		new SerializedRequestAttrVar().resolveTo(sessionWith(req), w, "text/plain,myKey");
		assertEquals("", w.toString());
	}

	@Test void e06_serializedRequestAttrVar_resolveTo_serializerFound() throws Exception {
		// Serializer.serialize(Object,Object) is final so we use a real serializer (Json5).
		// The production code calls s.serialize(w, o) where w is the Writer and o is the attribute
		// value used as the output destination; we supply a StringBuilder so the serializer has a
		// valid output target and can write successfully.
		var req = mock(RestRequest.class, RETURNS_DEEP_STUBS);
		var outputSb = new StringBuilder();
		var attr = mock(RequestAttribute.class);
		when(attr.orElse(any())).thenReturn(outputSb);
		when(req.getAttribute(any())).thenReturn(attr);
		when(req.getOpContext().getSerializers().getSerializer(any(String.class))).thenReturn(Optional.of(Json5Serializer.DEFAULT));
		new SerializedRequestAttrVar().resolveTo(sessionWith(req), new StringWriter(), "application/json,myKey");
		assertFalse(outputSb.toString().isEmpty());
	}

	// -----------------------------------------------------------------------------------------
	// f — RequestQueryVar ($RQ)
	// -----------------------------------------------------------------------------------------

	@Test void f01_requestQueryVar_nameConstant() {
		assertEquals("RQ", RequestQueryVar.NAME);
	}

	@Test void f02_requestQueryVar_canResolve_falseWhenNoRestRequest() {
		assertFalse(new RequestQueryVar().canResolve(emptySession()));
	}

	@Test void f03_requestQueryVar_canResolve_trueWhenRestRequestPresent() {
		assertTrue(new RequestQueryVar().canResolve(sessionWith(mock(RestRequest.class))));
	}

	@Test void f04_requestQueryVar_resolve_found() {
		var req = mock(RestRequest.class);
		var qp = mock(RequestQueryParam.class);
		when(req.getQueryParam("q")).thenReturn(qp);
		when(qp.orElse(null)).thenReturn("hello");
		assertEquals("hello", new RequestQueryVar().resolve(sessionWith(req), "q"));
	}

	@Test void f05_requestQueryVar_resolve_notFound() {
		var req = mock(RestRequest.class);
		var qp = mock(RequestQueryParam.class);
		when(req.getQueryParam("q")).thenReturn(qp);
		when(qp.orElse(null)).thenReturn(null);
		assertNull(new RequestQueryVar().resolve(sessionWith(req), "q"));
	}

	// -----------------------------------------------------------------------------------------
	// g — RequestHeaderVar ($RH)
	// -----------------------------------------------------------------------------------------

	@Test void g01_requestHeaderVar_nameConstant() {
		assertEquals("RH", RequestHeaderVar.NAME);
	}

	@Test void g02_requestHeaderVar_canResolve_falseWhenNoRestRequest() {
		assertFalse(new RequestHeaderVar().canResolve(emptySession()));
	}

	@Test void g03_requestHeaderVar_canResolve_trueWhenRestRequestPresent() {
		assertTrue(new RequestHeaderVar().canResolve(sessionWith(mock(RestRequest.class))));
	}

	@Test void g04_requestHeaderVar_resolve_found() {
		var req = mock(RestRequest.class);
		var hp = mock(RequestHeader.class);
		when(req.getHeaderParam("Accept")).thenReturn(hp);
		when(hp.orElse(null)).thenReturn("application/json");
		assertEquals("application/json", new RequestHeaderVar().resolve(sessionWith(req), "Accept"));
	}

	@Test void g05_requestHeaderVar_resolve_notFound() {
		var req = mock(RestRequest.class);
		var hp = mock(RequestHeader.class);
		when(req.getHeaderParam("Accept")).thenReturn(hp);
		when(hp.orElse(null)).thenReturn(null);
		assertNull(new RequestHeaderVar().resolve(sessionWith(req), "Accept"));
	}

	// -----------------------------------------------------------------------------------------
	// h — RequestFormDataVar ($RF)
	// -----------------------------------------------------------------------------------------

	@Test void h01_requestFormDataVar_nameConstant() {
		assertEquals("RF", RequestFormDataVar.NAME);
	}

	@Test void h02_requestFormDataVar_canResolve_falseWhenNoRestRequest() {
		assertFalse(new RequestFormDataVar().canResolve(emptySession()));
	}

	@Test void h03_requestFormDataVar_canResolve_trueWhenRestRequestPresent() {
		assertTrue(new RequestFormDataVar().canResolve(sessionWith(mock(RestRequest.class))));
	}

	@Test void h04_requestFormDataVar_resolve_found() {
		var req = mock(RestRequest.class);
		var fp = mock(RequestFormParam.class);
		when(req.getFormParam("field")).thenReturn(fp);
		when(fp.orElse(null)).thenReturn("value");
		assertEquals("value", new RequestFormDataVar().resolve(sessionWith(req), "field"));
	}

	@Test void h05_requestFormDataVar_resolve_notFound() {
		var req = mock(RestRequest.class);
		var fp = mock(RequestFormParam.class);
		when(req.getFormParam("field")).thenReturn(fp);
		when(fp.orElse(null)).thenReturn(null);
		assertNull(new RequestFormDataVar().resolve(sessionWith(req), "field"));
	}

	// -----------------------------------------------------------------------------------------
	// i — RequestAttributeVar ($RA)
	// -----------------------------------------------------------------------------------------

	@Test void i01_requestAttributeVar_nameConstant() {
		assertEquals("RA", RequestAttributeVar.NAME);
	}

	@Test void i02_requestAttributeVar_canResolve_falseWhenNoRestRequest() {
		assertFalse(new RequestAttributeVar().canResolve(emptySession()));
	}

	@Test void i03_requestAttributeVar_canResolve_trueWhenRestRequestPresent() {
		assertTrue(new RequestAttributeVar().canResolve(sessionWith(mock(RestRequest.class))));
	}

	@Test void i04_requestAttributeVar_resolve_found() {
		var req = mock(RestRequest.class);
		var attr = mock(RequestAttribute.class);
		when(req.getAttribute("key")).thenReturn(attr);
		when(attr.asString()).thenReturn(o("attrval"));
		assertEquals("attrval", new RequestAttributeVar().resolve(sessionWith(req), "key"));
	}

	@Test void i05_requestAttributeVar_resolve_notFound() {
		var req = mock(RestRequest.class);
		var attr = mock(RequestAttribute.class);
		when(req.getAttribute("key")).thenReturn(attr);
		when(attr.asString()).thenReturn(oe());
		assertNull(new RequestAttributeVar().resolve(sessionWith(req), "key"));
	}

	// -----------------------------------------------------------------------------------------
	// j — OpenApiVar ($OS)
	// -----------------------------------------------------------------------------------------

	@Test void j01_openApiVar_nameConstant() {
		assertEquals("OS", OpenApiVar.NAME);
	}

	@Test void j02_openApiVar_canResolve_falseWhenNoRestRequest() {
		assertFalse(new OpenApiVar().canResolve(emptySession()));
	}

	@Test void j03_openApiVar_canResolve_trueWhenRestRequestPresent() {
		assertTrue(new OpenApiVar().canResolve(sessionWith(mock(RestRequest.class))));
	}

	@Test void j04_openApiVar_resolve_returnsNullWhenKeyNotFound() {
		var req = mock(RestRequest.class);
		when(req.getOpenApi()).thenReturn(oe());
		assertNull(new OpenApiVar().resolve(sessionWith(req), "nonexistent.key"));
	}

	// -----------------------------------------------------------------------------------------
	// k — RequestSwaggerVar ($RS)
	// -----------------------------------------------------------------------------------------

	@Test void k01_requestSwaggerVar_nameConstant() {
		assertEquals("RS", RequestSwaggerVar.NAME);
	}

	@Test void k02_requestSwaggerVar_canResolve_falseWhenNoRestRequest() {
		assertFalse(new RequestSwaggerVar().canResolve(emptySession()));
	}

	@Test void k03_requestSwaggerVar_canResolve_trueWhenRestRequestPresent() {
		assertTrue(new RequestSwaggerVar().canResolve(sessionWith(mock(RestRequest.class))));
	}

	@Test void k04_requestSwaggerVar_resolve_title() throws Exception {
		var req = mock(RestRequest.class);
		when(req.getSwagger()).thenReturn(o(new Swagger().setInfo(new Info().setTitle("MyAPI"))));
		when(req.getOperationSwagger()).thenReturn(oe());
		assertEquals("MyAPI", new RequestSwaggerVar().resolve(sessionWith(req), "title"));
	}

	@Test void k05_requestSwaggerVar_resolve_description() throws Exception {
		var req = mock(RestRequest.class);
		when(req.getSwagger()).thenReturn(o(new Swagger().setInfo(new Info().setDescription("A desc"))));
		when(req.getOperationSwagger()).thenReturn(oe());
		assertEquals("A desc", new RequestSwaggerVar().resolve(sessionWith(req), "description"));
	}

	@Test void k06_requestSwaggerVar_resolve_version() throws Exception {
		var req = mock(RestRequest.class);
		when(req.getSwagger()).thenReturn(o(new Swagger().setInfo(new Info().setVersion("2.0"))));
		when(req.getOperationSwagger()).thenReturn(oe());
		assertEquals("2.0", new RequestSwaggerVar().resolve(sessionWith(req), "version"));
	}

	@Test void k07_requestSwaggerVar_resolve_license() throws Exception {
		var req = mock(RestRequest.class);
		when(req.getSwagger()).thenReturn(o(new Swagger().setInfo(new Info().setLicense(new License().setName("Apache 2.0")))));
		when(req.getOperationSwagger()).thenReturn(oe());
		assertNotNull(new RequestSwaggerVar().resolve(sessionWith(req), "license"));
	}

	@Test void k08_requestSwaggerVar_resolve_contact() throws Exception {
		var req = mock(RestRequest.class);
		when(req.getSwagger()).thenReturn(o(new Swagger().setInfo(new Info().setContact(new Contact().setName("Admin")))));
		when(req.getOperationSwagger()).thenReturn(oe());
		assertNotNull(new RequestSwaggerVar().resolve(sessionWith(req), "contact"));
	}

	@Test void k09_requestSwaggerVar_resolve_siteName() throws Exception {
		var req = mock(RestRequest.class);
		when(req.getSwagger()).thenReturn(o(new Swagger().setInfo(new Info().setSiteName("MySite"))));
		when(req.getOperationSwagger()).thenReturn(oe());
		assertEquals("MySite", new RequestSwaggerVar().resolve(sessionWith(req), "siteName"));
	}

	@Test void k10_requestSwaggerVar_resolve_termsOfService() throws Exception {
		var req = mock(RestRequest.class);
		when(req.getSwagger()).thenReturn(o(new Swagger().setInfo(new Info().setTermsOfService("TOS"))));
		when(req.getOperationSwagger()).thenReturn(oe());
		assertEquals("TOS", new RequestSwaggerVar().resolve(sessionWith(req), "termsOfService"));
	}

	@Test void k11_requestSwaggerVar_resolve_operationDescription() throws Exception {
		var req = mock(RestRequest.class);
		when(req.getSwagger()).thenReturn(oe());
		when(req.getOperationSwagger()).thenReturn(o(new Operation().setDescription("Op desc")));
		assertEquals("Op desc", new RequestSwaggerVar().resolve(sessionWith(req), "operationDescription"));
	}

	@Test void k12_requestSwaggerVar_resolve_operationSummary() throws Exception {
		var req = mock(RestRequest.class);
		when(req.getSwagger()).thenReturn(oe());
		when(req.getOperationSwagger()).thenReturn(o(new Operation().setSummary("Op summary")));
		assertEquals("Op summary", new RequestSwaggerVar().resolve(sessionWith(req), "operationSummary"));
	}

	@Test void k13_requestSwaggerVar_resolve_unknownKeyReturnsNull() throws Exception {
		var req = mock(RestRequest.class);
		when(req.getSwagger()).thenReturn(oe());
		when(req.getOperationSwagger()).thenReturn(oe());
		assertNull(new RequestSwaggerVar().resolve(sessionWith(req), "unknownKey"));
	}

	@Test void k14_requestSwaggerVar_resolve_noSwaggerReturnsNullForTitle() throws Exception {
		var req = mock(RestRequest.class);
		when(req.getSwagger()).thenReturn(oe());
		when(req.getOperationSwagger()).thenReturn(oe());
		assertNull(new RequestSwaggerVar().resolve(sessionWith(req), "title"));
	}

	@Test void k15_requestSwaggerVar_resolve_externalDocs() throws Exception {
		var req = mock(RestRequest.class);
		var extDocs = new ExternalDocumentation().setDescription("See more");
		when(req.getSwagger()).thenReturn(o(new Swagger().setExternalDocs(extDocs)));
		when(req.getOperationSwagger()).thenReturn(oe());
		assertNotNull(new RequestSwaggerVar().resolve(sessionWith(req), "externalDocs"));
	}

	@Test void k16_requestSwaggerVar_resolve_tags() throws Exception {
		var req = mock(RestRequest.class);
		var swaggerTag = new org.apache.juneau.bean.swagger.Tag().setName("foo");
		when(req.getSwagger()).thenReturn(o(new Swagger().setTags(swaggerTag)));
		when(req.getOperationSwagger()).thenReturn(oe());
		assertNotNull(new RequestSwaggerVar().resolve(sessionWith(req), "tags"));
	}
}
