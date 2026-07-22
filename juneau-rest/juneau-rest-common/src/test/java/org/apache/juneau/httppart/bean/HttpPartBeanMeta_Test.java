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
package org.apache.juneau.httppart.bean;

import static org.apache.juneau.commons.httppart.HttpPartType.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.httppart.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.http.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.marshall.uon.*;
import org.junit.jupiter.api.*;

/**
 * Tests for uncovered getter paths in RequestBeanPropertyMeta and ResponseBeanPropertyMeta.
 */
class HttpPartBeanMeta_Test {

	// --------------------------------------------------
	// RequestBeanPropertyMeta
	// --------------------------------------------------

	static class ReqInterface {
		@Query("q") public String getQuery() { return "q-value"; }
		@Header("h") public String getHeader() { return "h-value"; }
	}

	private RequestBeanPropertyMeta buildReqMeta(String methodName, HttpPartSerializer serializer, HttpPartParser parser) throws Exception {
		var m = MethodInfo.of(ReqInterface.class.getMethod(methodName));
		var b = RequestBeanPropertyMeta.create(QUERY, Query.class, m);
		return b.build(serializer, parser);
	}

	@Test
	void a01_getGetter_returnsMethod() throws Exception {
		var meta = buildReqMeta("getQuery", null, null);
		assertEquals("getQuery", meta.getGetter().getName());
	}

	@Test
	void a02_getPartType_returnsQueryType() throws Exception {
		var meta = buildReqMeta("getQuery", null, null);
		assertEquals(QUERY, meta.getPartType());
	}

	@Test
	void a03_getSchema_returnsNonNull() throws Exception {
		var meta = buildReqMeta("getQuery", null, null);
		assertNotNull(meta.getSchema());
		assertEquals("q", meta.getPartName());
	}

	@Test
	void a04_getParser_parserNull_returnsDefault() throws Exception {
		var meta = buildReqMeta("getQuery", null, null);
		var session = mock();
		assertSame(session, meta.getParser(session));
	}

	@Test
	void a05_getParser_parserNotNull_returnsParserSession() throws Exception {
		// Pass a real parser as the default; schema has no parser class, so meta.parser = the default parser
		var meta = buildReqMeta("getQuery", null, UonParser.DEFAULT);
		var result = meta.getParser(null);
		assertNotNull(result);
	}

	@Test
	void a06_getSerializer_noSchemaSerializer_returnsEmpty() throws Exception {
		var meta = buildReqMeta("getQuery", null, null);
		assertFalse(meta.getSerializer().isPresent());
	}

	// --------------------------------------------------
	// ResponseBeanPropertyMeta
	// --------------------------------------------------

	static class RespInterface {
		@Header("x-status") public int getStatus() { return 200; }
		@Content public String getBody() { return "body"; }
	}

	private ResponseBeanPropertyMeta buildRespMeta(String methodName) throws Exception {
		var m = MethodInfo.of(RespInterface.class.getMethod(methodName));
		var b = ResponseBeanPropertyMeta.create(HEADER, m);
		return b.build(oe(), oe());
	}

	@Test
	void b01_getGetter_returnsMethod() throws Exception {
		var meta = buildRespMeta("getStatus");
		assertEquals("getStatus", meta.getGetter().getName());
	}

	@Test
	void b02_getPartType_returnsHeaderType() throws Exception {
		var meta = buildRespMeta("getStatus");
		assertEquals(HEADER, meta.getPartType());
	}

	@Test
	void b03_getSchema_returnsNonNull() throws Exception {
		var meta = buildRespMeta("getStatus");
		assertNotNull(meta.getSchema());
	}

	@Test
	void b04_getPartName_withSchema_returnsName() throws Exception {
		var schema = HttpPartSchema.create().name("x-status").build();
		var m = MethodInfo.of(RespInterface.class.getMethod("getStatus"));
		var b = ResponseBeanPropertyMeta.create(HEADER, schema, m);
		var meta = b.build(oe(), oe());
		assertTrue(meta.getPartName().isPresent());
		assertEquals("x-status", meta.getPartName().orElseThrow());
	}

	@Test
	void b05_getParser_noSchemaParser_returnsEmpty() throws Exception {
		var meta = buildRespMeta("getStatus");
		assertFalse(meta.getParser().isPresent());
	}

	@Test
	void b06_getSerializer_noSchemaSerializer_returnsEmpty() throws Exception {
		var meta = buildRespMeta("getStatus");
		assertFalse(meta.getSerializer().isPresent());
	}

	@Test
	void b07_builderName_setsName() throws Exception {
		// ResponseBeanPropertyMeta.Builder.name() is a package-private setter — cover it here.
		var m = MethodInfo.of(RespInterface.class.getMethod("getStatus"));
		var b = ResponseBeanPropertyMeta.create(HEADER, m);
		b.name("overridden");   // exercises the name() setter (lines 54-56)
		var meta = b.build(oe(), oe());
		assertNotNull(meta);
	}

	// Minimal mock to avoid requiring full HttpPartParserSession instantiation
	private static HttpPartParserSession mock() {
		return new HttpPartParserSession() {
			@Override public <T> T read(HttpPartType partType, HttpPartSchema schema, String in, ClassMeta<T> type) { return null; }
		};
	}
}
