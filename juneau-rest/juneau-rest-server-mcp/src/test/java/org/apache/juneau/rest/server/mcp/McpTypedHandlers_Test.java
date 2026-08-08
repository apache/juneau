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
package org.apache.juneau.rest.server.mcp;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for typed schema derivation, argument binding, one-pass canonicalization, and registration.
 */
class McpTypedHandlers_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// Nested test types
	//-----------------------------------------------------------------------------------------------------------------

	public static class Args {
		public String value;
	}

	public static class Nested {
		public String v;
	}

	public static class Result {
		public Nested nested = new Nested();
	}

	public static class Types {
		public List<Result> results;
	}

	public static class CountingResult {
		private final AtomicInteger counter;

		public CountingResult(AtomicInteger counter) {
			this.counter = counter;
		}

		public String getValue() {
			counter.incrementAndGet();
			return "ok";
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helpers
	//-----------------------------------------------------------------------------------------------------------------

	private static <A,R> McpTypedToolHandler<A,R> handler(Type argType, Type resultType, Function<A,R> fn) {
		return new McpTypedToolHandler<>() {
			@Override public McpToolSpec descriptor() {
				return new McpToolSpec().setName("typed").setDescription("typed result");
			}
			@Override public Type argumentType() {
				return argType;
			}
			@Override public Type resultType() {
				return resultType;
			}
			@Override public R call(A arguments, BeanStore ctx) {
				return fn.apply(arguments);
			}
		};
	}

	private static void assertStructuredAndMirror(Object expected) {
		var a = McpTypedHandlers.adaptTool(handler(Args.class, Object.class, x -> expected));
		var b = a.call(Map.of(), new BasicBeanStore());
		if (expected == null) {
			assertNull(b.getStructuredContent());
			assertEquals("null", b.getContent().get(0).text());
		} else {
			assertEquals(Json.of(b.getStructuredContent()), b.getContent().get(0).text());
		}
	}

	private static Map<String,Object> nest(int depth) {
		Map<String,Object> a = new LinkedHashMap<>();
		for (var i = 1; i < depth; i++) {
			var b = new LinkedHashMap<String,Object>();
			b.put("child", a);
			a = b;
		}
		return a;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Derivation / contract tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_derivesAndCachesBothSchemasIncludingDefs() {
		var a = McpTypedHandlers.adaptTool(handler(Args.class, Result.class, x -> new Result()));
		var b = a.descriptor();
		assertEquals("typed", b.getName());
		assertContains("\"type\":\"object\"", Json.of(b.getInputSchema().toJsonMap()));
		assertContains("\"type\":\"object\"", Json.of(b.getOutputSchema().toJsonMap()));
		assertContains("\"$defs\"", Json.of(b.getOutputSchema().toJsonMap()));
		assertSame(b, a.descriptor());
	}

	@Test void a02_parameterizedResultTypeIsAuthoritative() throws Exception {
		Type a = Types.class.getDeclaredField("results").getGenericType();
		var b = McpTypedHandlers.adaptTool(handler(Args.class, a, x -> List.of(new Result())));
		assertContains("\"type\":\"array\"", Json.of(b.descriptor().getOutputSchema().toJsonMap()));
	}

	@Test void a03_singularConfigOverloadStoresRawAdapter() {
		var a = new McpServerConfig().addTool(handler(Args.class, Result.class, x -> new Result()));
		assertEquals(1, a.getTools().size());
		assertInstanceOf(McpToolHandler.class, a.getTools().get(0));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Behavior tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_bindsArgumentsAndCanonicalizesOnce() {
		var a = new AtomicInteger();
		var b = McpTypedHandlers.adaptTool(handler(Args.class, CountingResult.class, x -> new CountingResult(a)));
		var c = b.call(Map.of("value", "ok"), new BasicBeanStore());
		assertEquals(1, a.get());
		assertEquals("{\"value\":\"ok\"}", Json.of(c.getStructuredContent()));
		assertEquals("{\"value\":\"ok\"}", c.getContent().get(0).text());
		assertFalse(Json.of(c.getStructuredContent()).contains("_type"));
	}

	@Test void b02_allJsonCategoriesAndFragmentUrisRemainExact() {
		for (var a : List.of(JsonMap.of("uri", "#/$defs/x"), JsonList.of(1, 2), "text", true))
			assertStructuredAndMirror(a);
		assertStructuredAndMirror(null);
	}

	@Test void b03_mirroringCanBeDisabled() {
		var a = new McpTypedToolHandler<Args,Result>() {
			@Override public McpToolSpec descriptor() {
				return new McpToolSpec().setName("typed");
			}
			@Override public Type argumentType() {
				return Args.class;
			}
			@Override public Type resultType() {
				return Result.class;
			}
			@Override public Result call(Args arguments, BeanStore ctx) {
				return new Result();
			}
			@Override public boolean mirrorStructuredContentAsText() {
				return false;
			}
		};
		var b = McpTypedHandlers.adaptTool(a).call(Map.of(), new BasicBeanStore());
		assertNull(b.getContent());
		assertNotNull(b.getStructuredContent());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Error tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_bindingFailureIsInvalidParams() {
		var a = McpTypedHandlers.adaptTool(handler(Args.class, Result.class, x -> new Result()));
		Map<String,Object> arguments = Map.of("value", JsonMap.of());
		var ctx = new BasicBeanStore();
		var b = assertThrows(McpException.class, () -> a.call(arguments, ctx));
		assertEquals(-32602, b.getCode());
		assertContains(Args.class.getTypeName(), b.getMessage());
	}

	@Test void c02_resultCanonicalizationAndBoundsAreInternalErrors() {
		var a = McpTypedHandlers.adaptTool(handler(Args.class, Object.class, x -> nest(65)));
		Map<String,Object> arguments = Map.of();
		var b = assertThrows(McpException.class, () -> a.call(arguments, new BasicBeanStore()));
		assertEquals(-32603, b.getCode());
		assertContains("structuredContent", b.getMessage());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// tool(...) lambda-friendly factory
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_toolFactoryDeclaresInOneExpressionAndRoundTripsSchemaAndCall() {
		var a = McpTypedHandlers.tool("typed", "typed result", Args.class, Result.class,
			(args, ctx) -> new Result());
		var b = a.descriptor();
		assertEquals("typed", b.getName());
		assertEquals("typed result", b.getDescription());
		assertContains("\"type\":\"object\"", Json.of(b.getInputSchema().toJsonMap()));
		assertContains("\"$defs\"", Json.of(b.getOutputSchema().toJsonMap()));
		var c = a.call(Map.of("value", "ok"), new BasicBeanStore());
		assertEquals("{\"nested\":{}}", Json.of(c.getStructuredContent()));
		assertEquals(Json.of(c.getStructuredContent()), c.getContent().get(0).text());
	}

	@Test void d02_toolFactoryDelegatesArgumentsAndContextToTheLambda() {
		var seen = new AtomicReference<Args>();
		var seenCtx = new AtomicReference<BeanStore>();
		var a = McpTypedHandlers.tool("typed", null, Args.class, Result.class, (args, ctx) -> {
			seen.set(args);
			seenCtx.set(ctx);
			return new Result();
		});
		var ctx = new BasicBeanStore();
		a.call(Map.of("value", "ok"), ctx);
		assertEquals("ok", seen.get().value);
		assertSame(ctx, seenCtx.get());
	}

	@Test void d03_toolFactoryRejectsNullRequiredArguments() {
		assertThrows(IllegalArgumentException.class, () -> McpTypedHandlers.tool(null, "d", Args.class, Result.class, (args, ctx) -> new Result()));
		assertThrows(IllegalArgumentException.class, () -> McpTypedHandlers.tool("typed", "d", null, Result.class, (args, ctx) -> new Result()));
		assertThrows(IllegalArgumentException.class, () -> McpTypedHandlers.tool("typed", "d", Args.class, null, (args, ctx) -> new Result()));
		assertThrows(IllegalArgumentException.class, () -> McpTypedHandlers.tool("typed", "d", Args.class, Result.class, null));
	}
}
