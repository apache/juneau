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

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.commons.inject.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for the shared, dated-import-free completion domain: {@link McpCompletionRef},
 * {@link McpCompletionRequest}, {@link McpCompleter}, and {@link McpCompletionResult}.
 */
class McpCompletionDomain_Test {

	private static final BeanStore CTX = new BasicBeanStore();

	//-----------------------------------------------------------------------------------------------------------------
	// A: McpCompletionRef
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class A_completionRef {

		@Test void a01_kindEnumIsExactlyPromptAndResource() {
			assertEquals(List.of("PROMPT", "RESOURCE"),
				Stream.of(McpCompletionRef.Kind.values()).map(Enum::name).toList());
		}

		@Test void a02_promptFactoryPreservesExactTarget() {
			var ref = McpCompletionRef.prompt("greet");
			assertEquals(McpCompletionRef.Kind.PROMPT, ref.getKind());
			assertEquals("greet", ref.getTarget());
		}

		@Test void a03_resourceFactoryPreservesExactTarget() {
			var ref = McpCompletionRef.resource("file:///{name}");
			assertEquals(McpCompletionRef.Kind.RESOURCE, ref.getKind());
			assertEquals("file:///{name}", ref.getTarget());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B: McpCompletionRequest
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class B_completionRequest {

		@Test void b01_carriesRefArgumentNameAndValue() {
			var ref = McpCompletionRef.prompt("greet");
			var req = new McpCompletionRequest().setRef(ref).setArgumentName("name").setValue("jo");
			assertSame(ref, req.getRef());
			assertEquals("name", req.getArgumentName());
			assertEquals("jo", req.getValue());
		}

		@Test void b02_omittedContextNormalizesToEmptyMap() {
			var req = new McpCompletionRequest();
			assertNotNull(req.getContextArguments());
			assertTrue(req.getContextArguments().isEmpty());
		}

		@Test void b03_explicitNullContextNormalizesToEmptyMap() {
			var req = new McpCompletionRequest().setContextArguments(null);
			assertNotNull(req.getContextArguments());
			assertTrue(req.getContextArguments().isEmpty());
		}

		@Test void b04_contextIsImmutable() {
			var req = new McpCompletionRequest().setContextArguments(Map.of("a", "1"));
			assertThrows(UnsupportedOperationException.class, () -> req.getContextArguments().put("b", "2"));
		}

		@Test void b05_contextPreservesInsertionOrderIndependentOfSourceMap() {
			var source = new LinkedHashMap<String,String>();
			source.put("z", "1");
			source.put("a", "2");
			source.put("m", "3");
			var req = new McpCompletionRequest().setContextArguments(source);
			assertEquals(List.of("z", "a", "m"), List.copyOf(req.getContextArguments().keySet()));
			// Mutating the caller's source map afterward must not affect the stored copy.
			source.put("extra", "4");
			assertEquals(3, req.getContextArguments().size());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// C: McpCompleter
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class C_completer {

		@Test void c01_isFunctionalInterfaceWithSingleAbstractMethod() {
			assertTrue(McpCompleter.class.isInterface());
			assertNotNull(McpCompleter.class.getAnnotation(FunctionalInterface.class));
			var abstractMethods = Stream.of(McpCompleter.class.getMethods())
				.filter(m -> Modifier.isAbstract(m.getModifiers()))
				.toList();
			assertEquals(1, abstractMethods.size());
			assertEquals("complete", abstractMethods.get(0).getName());
		}

		@Test void c02_invokedWithRequestAndBeanStore() {
			McpCompleter completer = (request, ctx) -> {
				assertSame(CTX, ctx);
				return new McpCompletionResult().setValues(List.of(request.getValue() + "-suffix"));
			};
			var req = new McpCompletionRequest().setValue("pre");
			var result = completer.complete(req, CTX);
			assertEquals(List.of("pre-suffix"), result.getValues());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// D: McpCompletionResult normalization - values
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class D_normalizeValues {

		@Test void d01_nullValuesNormalizeToEmptyList() {
			var n = McpCompletionResult.normalize(new McpCompletionResult());
			assertNotNull(n.values());
			assertTrue(n.values().isEmpty());
		}

		@Test void d02_orderIsPreserved() {
			var raw = new McpCompletionResult().setValues(List.of("c", "a", "b"));
			var n = McpCompletionResult.normalize(raw);
			assertEquals(List.of("c", "a", "b"), n.values());
		}

		@Test void d03_duplicatesArePreserved() {
			var raw = new McpCompletionResult().setValues(List.of("a", "a", "b", "a"));
			var n = McpCompletionResult.normalize(raw);
			assertEquals(List.of("a", "a", "b", "a"), n.values());
		}

		@Test void d04_hundredValuesSurviveUnchanged() {
			var values = IntStream.range(0, 100).mapToObj(i -> "v" + i).toList();
			var raw = new McpCompletionResult().setValues(values).setHasMore(false);
			var n = McpCompletionResult.normalize(raw);
			assertEquals(100, n.values().size());
			assertEquals(values, n.values());
			assertEquals(Boolean.FALSE, n.hasMore());
		}

		@Test void d05_hundredAndOneValuesTruncateToFirstHundredAndForceHasMore() {
			var values = IntStream.range(0, 101).mapToObj(i -> "v" + i).toList();
			var raw = new McpCompletionResult().setValues(values).setHasMore(false);
			var n = McpCompletionResult.normalize(raw);
			assertEquals(100, n.values().size());
			assertEquals(values.subList(0, 100), n.values());
			assertEquals(Boolean.TRUE, n.hasMore());
		}

		@Test void d06_normalizedValuesAreImmutable() {
			var raw = new McpCompletionResult().setValues(new ArrayList<>(List.of("a", "b")));
			var n = McpCompletionResult.normalize(raw);
			assertThrows(UnsupportedOperationException.class, () -> n.values().add("c"));
		}

		@Test void d07_nullElementIsInternalFailure() {
			var raw = new McpCompletionResult().setValues(Arrays.asList("a", null, "b"));
			var ex = assertThrows(McpException.class, () -> McpCompletionResult.normalize(raw));
			assertEquals(McpCompletionResult.CODE_INTERNAL_ERROR, ex.getCode());
			assertEquals(-32603, ex.getCode());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// E: McpCompletionResult normalization - total / hasMore
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class E_normalizeTotalAndHasMore {

		@Test void e01_nonNegativeTotalIsPreserved() {
			var raw = new McpCompletionResult().setValues(List.of("a")).setTotal(42);
			var n = McpCompletionResult.normalize(raw);
			assertEquals(42, n.total());
		}

		@Test void e02_zeroTotalIsPreserved() {
			var raw = new McpCompletionResult().setValues(List.of()).setTotal(0);
			var n = McpCompletionResult.normalize(raw);
			assertEquals(0, n.total());
		}

		@Test void e03_negativeTotalIsInternalFailure() {
			var raw = new McpCompletionResult().setValues(List.of("a")).setTotal(-1);
			var ex = assertThrows(McpException.class, () -> McpCompletionResult.normalize(raw));
			assertEquals(-32603, ex.getCode());
		}

		@Test void e04_nullableHasMoreSurvivesUnchangedWithoutTruncation() {
			var raw1 = new McpCompletionResult().setValues(List.of("a"));
			assertNull(McpCompletionResult.normalize(raw1).hasMore());

			var raw2 = new McpCompletionResult().setValues(List.of("a")).setHasMore(true);
			assertEquals(Boolean.TRUE, McpCompletionResult.normalize(raw2).hasMore());

			var raw3 = new McpCompletionResult().setValues(List.of("a")).setHasMore(false);
			assertEquals(Boolean.FALSE, McpCompletionResult.normalize(raw3).hasMore());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// F: null completer result and empty()
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class F_nullResultAndEmpty {

		@Test void f01_nullCompleterResultIsInternalFailure() {
			var ex = assertThrows(McpException.class, () -> McpCompletionResult.normalize(null));
			assertEquals(-32603, ex.getCode());
		}

		@Test void f02_emptyProducesEmptyValuesNoTotalNoHasMore() {
			var n = McpCompletionResult.normalize(McpCompletionResult.empty());
			assertTrue(n.values().isEmpty());
			assertNull(n.total());
			assertNull(n.hasMore());
		}

		@Test void f03_emptyValuesListIsSetDirectly() {
			assertEquals(List.of(), McpCompletionResult.empty().getValues());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// G: neutrality - no dated bean/adapter import anywhere in the completion domain sources
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class G_neutrality {

		private static final List<Class<?>> DOMAIN_TYPES =
			List.of(McpCompleter.class, McpCompletionRef.class, McpCompletionRequest.class, McpCompletionResult.class);

		@Test void g01_noDatedImportInSourceFiles() throws IOException {
			var srcDir = Paths.get("src/main/java/org/apache/juneau/rest/server/mcp");
			for (var type : DOMAIN_TYPES) {
				var file = srcDir.resolve(type.getSimpleName() + ".java");
				assertTrue(Files.exists(file), () -> "Missing source file: " + file);
				var text = Files.readString(file);
				assertFalse(text.contains("org.apache.juneau.bean.mcp.v20"),
					() -> type.getSimpleName() + " must not import a dated MCP bean package");
				assertFalse(text.contains("org.apache.juneau.rest.server.mcp.v20"),
					() -> type.getSimpleName() + " must not import a dated MCP adapter package");
			}
		}

		@Test void g02_noDatedTypeReachableViaReflection() {
			for (var type : DOMAIN_TYPES) {
				for (var member : allReferencedTypes(type)) {
					var name = member.getName();
					assertFalse(name.contains(".bean.mcp.v20"), () -> type + " reaches dated bean type " + name);
					assertFalse(name.contains(".rest.server.mcp.v20"), () -> type + " reaches dated adapter type " + name);
				}
			}
		}

		private static List<Class<?>> allReferencedTypes(Class<?> type) {
			var out = new ArrayList<Class<?>>();
			for (var field : type.getDeclaredFields())
				out.add(field.getType());
			for (var method : type.getDeclaredMethods()) {
				out.add(method.getReturnType());
				out.addAll(List.of(method.getParameterTypes()));
			}
			for (var ctor : type.getDeclaredConstructors())
				out.addAll(List.of(ctor.getParameterTypes()));
			return out;
		}
	}
}
