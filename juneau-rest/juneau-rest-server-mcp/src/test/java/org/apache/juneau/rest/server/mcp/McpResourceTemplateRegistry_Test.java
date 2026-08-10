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

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.inject.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link McpServerConfig}'s handler-based resource-template registry: registration,
 * descriptor-only listing convenience, fail-fast validation, and the direct-mutation revalidation gate.
 */
@SuppressWarnings({
	"java:S5976" // Each aNN/bNN/... test pins a distinct named registration/validation scenario as its own discoverable, individually-runnable test (per project SSLLC convention); collapsing similar-shaped groups into @ParameterizedTest would trade per-scenario failure clarity for a marginal LOC reduction.
})
class McpResourceTemplateRegistry_Test {

	private static final BeanStore CTX = new BasicBeanStore();

	private static McpResourceTemplateHandler handler(String uriTemplate) {
		return new McpResourceTemplateHandler() {
			@Override public McpResourceTemplateSpec descriptor() { return new McpResourceTemplateSpec().setUriTemplate(uriTemplate); }
			@Override public McpResourceOutcome read(String uri, Map<String,String> variables, BeanStore ctx) { return null; }
		};
	}

	/** A handler that counts {@link McpResourceTemplateHandler#descriptor()} invocations. */
	private static final class CountingHandler implements McpResourceTemplateHandler {
		private final McpResourceTemplateSpec spec;
		private int descriptorCalls;

		CountingHandler(String uriTemplate) {
			spec = new McpResourceTemplateSpec().setUriTemplate(uriTemplate);
		}

		@Override public McpResourceTemplateSpec descriptor() {
			descriptorCalls++;
			return spec;
		}

		@Override public McpResourceOutcome read(String uri, Map<String,String> variables, BeanStore ctx) {
			return null;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// A: handler descriptors are the sole listing/completion identity source; order/copy/null-clear semantics
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class A_registrationOrderCopyAndNull {

		@Test void a01_handlerOrderAndDescriptorIdentityArePreserved() {
			var a = handler("file:///a");
			var b = handler("file:///b");
			var config = new McpServerConfig().addResourceTemplate(a, b);
			assertEquals(List.of(a, b), config.getResourceTemplates());
			assertEquals(List.of("file:///a", "file:///b"), config.getResourceTemplates().stream()
				.map(h -> h.descriptor().getUriTemplate()).toList());
		}

		@Test void a02_setResourceTemplatesCopiesTheSuppliedList() {
			var a = handler("file:///a");
			var source = new ArrayList<>(List.of(a));
			var config = new McpServerConfig().setResourceTemplates(source);
			source.clear();
			assertEquals(1, config.getResourceTemplates().size());
			assertSame(a, config.getResourceTemplates().get(0));
		}

		@Test void a03_setResourceTemplatesNullClears() {
			var config = new McpServerConfig().addResourceTemplate(handler("file:///a"));
			config.setResourceTemplates(null);
			assertTrue(config.getResourceTemplates().isEmpty());
		}

		@Test void a04_setResourceTemplateSpecsCopiesAndNullClears() {
			var spec = new McpResourceTemplateSpec().setUriTemplate("file:///a").setName("a");
			var source = new ArrayList<>(List.of(spec));
			var config = new McpServerConfig().setResourceTemplateSpecs(source);
			source.clear();
			assertEquals(1, config.getResourceTemplates().size());
			assertSame(spec, config.getResourceTemplates().get(0).descriptor());

			config.setResourceTemplateSpecs(null);
			assertTrue(config.getResourceTemplates().isEmpty());
		}

		@Test void a05_descriptorVarargsRegistrationRemainsSourceCompatible() {
			var config = new McpServerConfig()
				.addResourceTemplate(
					new McpResourceTemplateSpec().setUriTemplate("file:///a").setName("a"),
					new McpResourceTemplateSpec().setUriTemplate("file:///b").setName("b"));
			assertEquals(List.of("a", "b"), config.getResourceTemplates().stream()
				.map(h -> h.descriptor().getName()).toList());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B: listing-only wrappers
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class B_listingOnlyWrapper {

		@Test void b01_wrapperReturnsOriginalDescriptorInstance() {
			var spec = new McpResourceTemplateSpec().setUriTemplate("file:///{x}").setName("n");
			var config = new McpServerConfig().addResourceTemplate(spec);
			assertSame(spec, config.getResourceTemplates().get(0).descriptor());
		}

		@Test void b02_wrapperReadAlwaysReturnsNull() {
			var spec = new McpResourceTemplateSpec().setUriTemplate("file:///{x}");
			var config = new McpServerConfig().addResourceTemplate(spec);
			var wrapped = config.getResourceTemplates().get(0);
			assertNull(wrapped.read("file:///v", Map.of("x", "v"), CTX));
		}

		@Test void b03_wrapperCompleterAlwaysReturnsNull() {
			var spec = new McpResourceTemplateSpec().setUriTemplate("file:///{x}");
			var config = new McpServerConfig().addResourceTemplate(spec);
			var wrapped = config.getResourceTemplates().get(0);
			assertNull(wrapped.completer("x"));
		}

		@Test void b04_setResourceTemplateSpecsProducesEquivalentListingOnlyWrappers() {
			var spec = new McpResourceTemplateSpec().setUriTemplate("file:///{x}");
			var config = new McpServerConfig().setResourceTemplateSpecs(List.of(spec));
			var wrapped = config.getResourceTemplates().get(0);
			assertSame(spec, wrapped.descriptor());
			assertNull(wrapped.read("file:///v", Map.of("x", "v"), CTX));
			assertNull(wrapped.completer("x"));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// C: fail-fast validation before publication
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class C_failFastValidation {

		@Test void c01_nullHandlerFailsNamingIndex() {
			assertThrowsWithMessage(IllegalArgumentException.class, "index 0",
				() -> new McpServerConfig().addResourceTemplate((McpResourceTemplateHandler) null));
		}

		@Test void c02_nullDescriptorFailsNamingIndex() {
			McpResourceTemplateHandler bad = new McpResourceTemplateHandler() {
				@Override public McpResourceTemplateSpec descriptor() { return null; }
				@Override public McpResourceOutcome read(String uri, Map<String,String> variables, BeanStore ctx) { return null; }
			};
			assertThrowsWithMessage(IllegalArgumentException.class, "index 0",
				() -> new McpServerConfig().addResourceTemplate(bad));
		}

		@Test void c03_blankUriTemplateFailsNamingIndex() {
			var bad = handler("   ");
			assertThrowsWithMessage(IllegalArgumentException.class, "index 0",
				() -> new McpServerConfig().addResourceTemplate(bad));
		}

		@Test void c04_nullUriTemplateFailsNamingIndex() {
			var bad = handler(null);
			assertThrowsWithMessage(IllegalArgumentException.class, "index 0",
				() -> new McpServerConfig().addResourceTemplate(bad));
		}

		@Test void c05_malformedTemplateFailsNamingIndexAndTemplate() {
			var bad = handler("file:///{unterminated");
			var ex = assertThrowsWithMessage(IllegalArgumentException.class, "index 0",
				() -> new McpServerConfig().addResourceTemplate(bad));
			assertContains("file:///{unterminated", ex.getMessage());
		}

		@Test void c06_duplicateVariableFailsNamingIndexAndTemplate() {
			var bad = handler("file:///{a}/{a}");
			var ex = assertThrowsWithMessage(IllegalArgumentException.class, "index 0",
				() -> new McpServerConfig().addResourceTemplate(bad));
			assertContains("file:///{a}/{a}", ex.getMessage());
		}

		@Test void c07_duplicateExactTemplateFailsNamingIndexAndTemplate() {
			var a = handler("file:///{name}");
			var b = handler("file:///{name}");
			var ex = assertThrowsWithMessage(IllegalArgumentException.class, "index 1",
				() -> new McpServerConfig().addResourceTemplate(a, b));
			assertContains("file:///{name}", ex.getMessage());
		}

		@Test void c08_failedValidationPreservesThePreviouslyPublishedRegistry() {
			var good = handler("file:///a");
			var config = new McpServerConfig().addResourceTemplate(good);
			assertThrows(IllegalArgumentException.class,
				() -> config.addResourceTemplate((McpResourceTemplateHandler) null));
			assertEquals(List.of(good), config.getResourceTemplates());
		}

		@Test void c09_setResourceTemplatesValidatesTheCompleteReplacementList() {
			var config = new McpServerConfig().addResourceTemplate(handler("file:///a"));
			var replacement = Arrays.asList(handler("file:///b"), null);
			assertThrows(IllegalArgumentException.class,
				() -> config.setResourceTemplates(replacement));
			// Failure must leave the previous registry untouched.
			assertEquals(List.of("file:///a"), config.getResourceTemplates().stream()
				.map(h -> h.descriptor().getUriTemplate()).toList());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// D: direct mutable-list corruption and stable-use recompilation avoidance
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class D_mutationDetectionAndStability {

		@Test void d01_directListMutationCorruptionIsCaughtOnFirstConsume() {
			var config = new McpServerConfig().addResourceTemplate(handler("file:///a"));
			// Bypass the validated add/set surface by mutating the live list directly.
			config.getResourceTemplates().add(null);
			assertThrowsWithMessage(IllegalArgumentException.class, "index 1", config::getResourceTemplates);
		}

		@Test void d02_directListMutationWithDuplicateTemplateIsCaughtOnFirstConsume() {
			var config = new McpServerConfig().addResourceTemplate(handler("file:///a"));
			config.getResourceTemplates().add(handler("file:///a"));
			assertThrowsWithMessage(IllegalArgumentException.class, "index 1", config::getResourceTemplates);
		}

		@Test void d03_stableRegistryDoesNotRecompilePerRequest() {
			var h = new CountingHandler("file:///a/{x}");
			var config = new McpServerConfig().addResourceTemplate(h);
			var callsAfterPublication = h.descriptorCalls;
			assertTrue(callsAfterPublication > 0, "publication must validate (and thereby compile) at least once");

			config.getResourceTemplates();
			config.getResourceTemplates();
			config.getResourceTemplates();

			assertEquals(callsAfterPublication, h.descriptorCalls,
				"stable repeated consumption must not re-validate/re-compile the unchanged registry");
		}

		@Test void d04_mutationAfterStableUseTriggersExactlyOneRevalidation() {
			var h1 = new CountingHandler("file:///a");
			var h2 = new CountingHandler("file:///b");
			var config = new McpServerConfig().addResourceTemplate(h1);
			config.getResourceTemplates();
			config.getResourceTemplates();
			var callsBeforeMutation = h1.descriptorCalls;

			config.getResourceTemplates().add(h2);
			config.getResourceTemplates();
			config.getResourceTemplates();

			assertEquals(callsBeforeMutation + 1, h1.descriptorCalls,
				"the mutation must trigger exactly one revalidation, not one per subsequent call");
			assertEquals(1, h2.descriptorCalls);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// E: valid non-reverse-matchable templates remain registered
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class E_nonMatchableTemplatesRemainRegistered {

		@Test void e01_explodeVarspecRegistersSuccessfully() {
			var config = new McpServerConfig().addResourceTemplate(handler("file:///{?tags*}"));
			assertEquals(1, config.getResourceTemplates().size());
			assertFalse(McpUriTemplateMatcher.compile("file:///{?tags*}").isReverseMatchable());
		}

		@Test void e02_multiVariableSimpleExpressionRegistersSuccessfully() {
			var config = new McpServerConfig().addResourceTemplate(handler("file:///{x,y}"));
			assertEquals(1, config.getResourceTemplates().size());
			assertFalse(McpUriTemplateMatcher.compile("file:///{x,y}").isReverseMatchable());
		}

		@Test void e03_prefixModifierRegistersSuccessfully() {
			var config = new McpServerConfig().addResourceTemplate(handler("file:///{var:3}"));
			assertEquals(1, config.getResourceTemplates().size());
			assertFalse(McpUriTemplateMatcher.compile("file:///{var:3}").isReverseMatchable());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// F: neutral completer lookup filters undeclared variables regardless of handler behavior
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class F_neutralCompleterLookup {

		private static final McpCompleter STUB = (request, ctx) -> McpCompletionResult.empty();

		@Test void f01_declaredVariableResolvesToHandlerCompleter() {
			McpResourceTemplateHandler h = new McpResourceTemplateHandler() {
				@Override public McpResourceTemplateSpec descriptor() { return new McpResourceTemplateSpec().setUriTemplate("file:///{name}"); }
				@Override public McpResourceOutcome read(String uri, Map<String,String> variables, BeanStore ctx) { return null; }
				@Override public McpCompleter completer(String variableName) { return "name".equals(variableName) ? STUB : null; }
			};
			assertSame(STUB, McpServerConfig.resourceTemplateCompleter(h, "name"));
		}

		@Test void f02_undeclaredVariableResolvesToNullEvenIfHandlerMisbehaves() {
			// A faulty handler that (incorrectly) returns a non-null completer for any variable name.
			McpResourceTemplateHandler faulty = new McpResourceTemplateHandler() {
				@Override public McpResourceTemplateSpec descriptor() { return new McpResourceTemplateSpec().setUriTemplate("file:///{name}"); }
				@Override public McpResourceOutcome read(String uri, Map<String,String> variables, BeanStore ctx) { return null; }
				@Override public McpCompleter completer(String variableName) { return STUB; }
			};
			assertSame(STUB, McpServerConfig.resourceTemplateCompleter(faulty, "name"));
			assertNull(McpServerConfig.resourceTemplateCompleter(faulty, "bogus"),
				"an undeclared variable name must never resolve to a completer, regardless of handler behavior");
		}

		@Test void f03_handlerWithNoCompleterResolvesToNullForDeclaredVariable() {
			var h = handler("file:///{name}");
			assertNull(McpServerConfig.resourceTemplateCompleter(h, "name"));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// G: coverage for the per-handler compiled McpUriTemplateMatcher cache
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class G_compiledMatcherCache {

		@SuppressWarnings("unchecked")
		private Map<McpResourceTemplateHandler,McpUriTemplateMatcher> compiledMatchers(McpServerConfig config) {
			try {
				var f = McpServerConfig.class.getDeclaredField("compiledResourceTemplateMatchers");
				f.setAccessible(true);
				return (Map<McpResourceTemplateHandler,McpUriTemplateMatcher>) f.get(config);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		}

		@Test void g01_registrationPopulatesACompiledMatcherPerHandler() {
			var a = handler("file:///a/{x}");
			var b = handler("file:///b/{y}");
			var config = new McpServerConfig().addResourceTemplate(a, b);
			var matchers = compiledMatchers(config);
			assertEquals(Set.of(a, b), matchers.keySet());
			assertEquals(List.of("x"), matchers.get(a).variableNames());
			assertEquals(List.of("y"), matchers.get(b).variableNames());
		}

		@Test void g02_resolveHasAnyCompleterAndTemplateCompleterReuseTheCachedMatcherWithoutRebuildingIt() {
			var h = handler("file:///a/{x}");
			var config = new McpServerConfig().addResourceTemplate(h);
			var matchers = compiledMatchers(config);
			var cached = matchers.get(h);
			assertNotNull(cached);

			config.resolveResourceTemplate("file:///a/1");
			config.hasAnyCompleter();
			config.templateCompleter("file:///a/{x}", "x");

			// None of the per-request read paths may rebuild the cache: same map, same compiled matcher instance.
			assertSame(matchers, compiledMatchers(config));
			assertSame(cached, compiledMatchers(config).get(h));
		}

		@Test void g03_reRegistrationRefreshesTheCacheRatherThanGoingStale() {
			var a = handler("file:///a/{x}");
			var config = new McpServerConfig().addResourceTemplate(a);
			var b = handler("file:///b/{y}");
			config.addResourceTemplate(b);

			var matchers = compiledMatchers(config);
			assertEquals(Set.of(a, b), matchers.keySet());

			// End-to-end: both templates remain correctly resolvable after the cache refresh.
			assertSame(a, config.resolveResourceTemplate("file:///a/1").handler());
			assertSame(b, config.resolveResourceTemplate("file:///b/1").handler());
		}

		@Test void g04_directListMutationRevalidationRefreshesTheMatcherCacheToo() {
			var a = handler("file:///a/{x}");
			var config = new McpServerConfig().addResourceTemplate(a);
			var b = handler("file:///b/{y}");
			// Bypass the validated add surface, mirroring D_mutationDetectionAndStability.
			config.getResourceTemplates().add(b);

			// First consumption after the direct mutation must revalidate and (re)compile, picking up b.
			var match = config.resolveResourceTemplate("file:///b/1");
			assertNotNull(match);
			assertSame(b, match.handler());
			assertEquals(Set.of(a, b), compiledMatchers(config).keySet());
		}
	}
}
