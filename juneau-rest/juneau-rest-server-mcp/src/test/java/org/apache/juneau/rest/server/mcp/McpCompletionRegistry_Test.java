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
 * Coverage for {@link McpServerConfig}'s neutral, cross-target completion lookup and prompt-argument
 * completer registration: the {@link McpPromptArgument#getCompleter() completer} property, prompt
 * registration validation, exact prompt/template lookup, and completion-capability detection.
 */
class McpCompletionRegistry_Test {

	private static final BeanStore CTX = new BasicBeanStore();

	private static final McpCompleter STUB = (request, ctx) -> McpCompletionResult.empty();

	private static McpPromptHandler prompt(String name, McpPromptArgument... arguments) {
		return new McpPromptHandler() {
			@Override public McpPromptSpec descriptor() {
				return new McpPromptSpec().setName(name).setArguments(arguments.length == 0 ? null : List.of(arguments));
			}
			@Override public McpPromptOutcome get(Map<String,Object> args, BeanStore ctx) { return new McpPromptOutcome(); }
		};
	}

	private static McpResourceTemplateHandler template(String uriTemplate, Map<String,McpCompleter> completers) {
		return new McpResourceTemplateHandler() {
			@Override public McpResourceTemplateSpec descriptor() { return new McpResourceTemplateSpec().setUriTemplate(uriTemplate); }
			@Override public McpResourceOutcome read(String uri, Map<String,String> variables, BeanStore ctx) { return null; }
			@Override public McpCompleter completer(String variableName) { return completers.get(variableName); }
		};
	}

	//-----------------------------------------------------------------------------------------------------------------
	// A: McpPromptArgument.completer - fluent getter/setter, existing fields preserved
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class A_promptArgumentCompleterField {

		@Test void a01_defaultsToNull() {
			assertNull(new McpPromptArgument().getCompleter());
		}

		@Test void a02_fluentGetterSetter() {
			var a = new McpPromptArgument().setCompleter(STUB);
			assertSame(STUB, a.getCompleter());
		}

		@Test void a03_setNullClears() {
			var a = new McpPromptArgument().setCompleter(STUB);
			a.setCompleter(null);
			assertNull(a.getCompleter());
		}

		@Test void a04_existingThreeFieldsPreservedAlongsideCompleter() {
			var a = new McpPromptArgument().setName("who").setDescription("d").setRequired(true).setCompleter(STUB);
			assertEquals("who", a.getName());
			assertEquals("d", a.getDescription());
			assertEquals(Boolean.TRUE, a.getRequired());
			assertSame(STUB, a.getCompleter());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B: prompt registration validation - fail fast before dispatch
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class B_promptRegistrationValidation {

		@Test void b01_duplicateNonNullArgumentNamesRejected_onAddPrompt() {
			var bad = prompt("greet", new McpPromptArgument().setName("name"), new McpPromptArgument().setName("name"));
			var ex = assertThrowsWithMessage(IllegalArgumentException.class, "duplicate argument name",
				() -> new McpServerConfig().addPrompt(bad));
			assertContains("greet", ex.getMessage());
			assertContains("name", ex.getMessage());
		}

		@Test void b02_duplicateNonNullArgumentNamesRejected_onSetPrompts() {
			var bad = prompt("greet", new McpPromptArgument().setName("name"), new McpPromptArgument().setName("name"));
			assertThrowsWithMessage(IllegalArgumentException.class, "duplicate argument name",
				() -> new McpServerConfig().setPrompts(List.of(bad)));
		}

		@Test void b03_multipleNullArgumentNamesAreNotDuplicates() {
			var ok = prompt("greet", new McpPromptArgument().setName(null), new McpPromptArgument().setName(null));
			assertDoesNotThrow(() -> new McpServerConfig().addPrompt(ok));
		}

		@Test void b04_completerOnNullArgumentNameRejected() {
			var bad = prompt("greet", new McpPromptArgument().setName(null).setCompleter(STUB));
			var ex = assertThrowsWithMessage(IllegalArgumentException.class,
				"completer must not be attached to a null or blank argument name",
				() -> new McpServerConfig().addPrompt(bad));
			assertContains("greet", ex.getMessage());
		}

		@Test void b05_completerOnBlankArgumentNameRejected() {
			var bad = prompt("greet", new McpPromptArgument().setName("   ").setCompleter(STUB));
			assertThrowsWithMessage(IllegalArgumentException.class,
				"completer must not be attached to a null or blank argument name",
				() -> new McpServerConfig().addPrompt(bad));
		}

		@Test void b06_completerOnNonBlankArgumentNameIsValid() {
			var ok = prompt("greet", new McpPromptArgument().setName("name").setCompleter(STUB));
			assertDoesNotThrow(() -> new McpServerConfig().addPrompt(ok));
		}

		@Test void b07_failedValidationPreservesThePreviouslyPublishedRegistry() {
			var good = prompt("p", new McpPromptArgument().setName("a"));
			var config = new McpServerConfig().addPrompt(good);
			var bad = prompt("q", new McpPromptArgument().setName("x"), new McpPromptArgument().setName("x"));
			assertThrows(IllegalArgumentException.class, () -> config.addPrompt(bad));
			assertEquals(List.of("p"), config.getPrompts().stream().map(h -> h.descriptor().getName()).toList());
		}

		@Test void b08_handlerWithNullDescriptorArgumentsIsSkipped() {
			var ok = prompt("bare");
			assertDoesNotThrow(() -> new McpServerConfig().addPrompt(ok));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// C: exact prompt/argument completer lookup
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class C_promptCompleterLookup {

		@Test void c01_exactPromptAndArgumentRoutesToCompleter() {
			var config = new McpServerConfig().addPrompt(prompt("greet", new McpPromptArgument().setName("name").setCompleter(STUB)));
			assertSame(STUB, config.promptCompleter("greet", "name"));
		}

		@Test void c02_unknownPromptResolvesToNullWithoutInvocation() {
			var config = new McpServerConfig().addPrompt(prompt("greet", new McpPromptArgument().setName("name").setCompleter(STUB)));
			assertNull(config.promptCompleter("unknown", "name"));
		}

		@Test void c03_unknownArgumentResolvesToNullWithoutInvocation() {
			var config = new McpServerConfig().addPrompt(prompt("greet", new McpPromptArgument().setName("name").setCompleter(STUB)));
			assertNull(config.promptCompleter("greet", "unknown"));
		}

		@Test void c04_knownArgumentWithoutCompleterResolvesToNull() {
			var config = new McpServerConfig().addPrompt(prompt("greet", new McpPromptArgument().setName("name")));
			assertNull(config.promptCompleter("greet", "name"));
		}

		@Test void c05_nullPromptOrArgumentNameResolvesToNull() {
			var config = new McpServerConfig().addPrompt(prompt("greet", new McpPromptArgument().setName("name").setCompleter(STUB)));
			assertNull(config.promptCompleter(null, "name"));
			assertNull(config.promptCompleter("greet", null));
		}

		@Test void c06_noPromptsRegisteredResolvesToNull() {
			assertNull(new McpServerConfig().promptCompleter("greet", "name"));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// D: exact template-string/declared-variable completer lookup
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class D_templateCompleterLookup {

		@Test void d01_exactTemplateAndDeclaredVariableRoutesToCompleter() {
			var config = new McpServerConfig().addResourceTemplate(template("file:///{name}", Map.of("name", STUB)));
			assertSame(STUB, config.templateCompleter("file:///{name}", "name"));
		}

		@Test void d02_unknownTemplateResolvesToNullWithoutInvocation() {
			var config = new McpServerConfig().addResourceTemplate(template("file:///{name}", Map.of("name", STUB)));
			assertNull(config.templateCompleter("file:///{other}", "name"));
		}

		@Test void d03_undeclaredVariableResolvesToNullEvenIfHandlerMisbehaves() {
			// A faulty handler that (incorrectly) returns a non-null completer for any variable name.
			McpResourceTemplateHandler faulty = new McpResourceTemplateHandler() {
				@Override public McpResourceTemplateSpec descriptor() { return new McpResourceTemplateSpec().setUriTemplate("file:///{name}"); }
				@Override public McpResourceOutcome read(String uri, Map<String,String> variables, BeanStore ctx) { return null; }
				@Override public McpCompleter completer(String variableName) { return STUB; }
			};
			var config = new McpServerConfig().addResourceTemplate(faulty);
			assertSame(STUB, config.templateCompleter("file:///{name}", "name"));
			assertNull(config.templateCompleter("file:///{name}", "bogus"));
		}

		@Test void d04_declaredVariableWithoutCompleterResolvesToNull() {
			var config = new McpServerConfig().addResourceTemplate(template("file:///{name}", Map.of()));
			assertNull(config.templateCompleter("file:///{name}", "name"));
		}

		@Test void d05_nullTemplateOrVariableNameResolvesToNull() {
			var config = new McpServerConfig().addResourceTemplate(template("file:///{name}", Map.of("name", STUB)));
			assertNull(config.templateCompleter(null, "name"));
			assertNull(config.templateCompleter("file:///{name}", null));
		}

		@Test void d06_concreteUriIsNotAnExactTemplateStringMatch() {
			var config = new McpServerConfig().addResourceTemplate(template("file:///{name}", Map.of("name", STUB)));
			// The lookup is exact-registered-template-string, not a reverse match against a concrete URI.
			assertNull(config.templateCompleter("file:///alice", "name"));
		}

		@Test void d07_nonReverseMatchableTemplateVariableRemainsCompletable() {
			var config = new McpServerConfig().addResourceTemplate(template("file:///{x,y}", Map.of("x", STUB)));
			assertFalse(McpUriTemplateMatcher.compile("file:///{x,y}").isReverseMatchable());
			assertSame(STUB, config.templateCompleter("file:///{x,y}", "x"));
		}

		@Test void d08_noTemplatesRegisteredResolvesToNull() {
			assertNull(new McpServerConfig().templateCompleter("file:///{name}", "name"));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// E: unified ref-based lookup
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class E_unifiedRefLookup {

		@Test void e01_promptKindDispatchesToPromptCompleter() {
			var config = new McpServerConfig().addPrompt(prompt("greet", new McpPromptArgument().setName("name").setCompleter(STUB)));
			assertSame(STUB, config.completer(McpCompletionRef.prompt("greet"), "name"));
		}

		@Test void e02_resourceKindDispatchesToTemplateCompleter() {
			var config = new McpServerConfig().addResourceTemplate(template("file:///{name}", Map.of("name", STUB)));
			assertSame(STUB, config.completer(McpCompletionRef.resource("file:///{name}"), "name"));
		}

		@Test void e03_nullRefResolvesToNull() {
			assertNull(new McpServerConfig().completer(null, "name"));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// F: context/BeanStore pass-through once a completer is resolved
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class F_contextPassThrough {

		@Test void f01_resolvedCompleterReceivesRequestAndBeanStoreUnchanged() {
			McpCompleter captor = (request, ctx) -> {
				assertSame(CTX, ctx);
				assertEquals("jo", request.getValue());
				assertEquals(Map.of("lang", "en"), request.getContextArguments());
				return new McpCompletionResult().setValues(List.of("john"));
			};
			var config = new McpServerConfig().addPrompt(prompt("greet", new McpPromptArgument().setName("name").setCompleter(captor)));

			var resolved = config.promptCompleter("greet", "name");
			var request = new McpCompletionRequest().setRef(McpCompletionRef.prompt("greet")).setArgumentName("name")
				.setValue("jo").setContextArguments(Map.of("lang", "en"));
			var result = resolved.complete(request, CTX);

			assertEquals(List.of("john"), result.getValues());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// G: completion-capability detection - registration only, never invokes
	//-----------------------------------------------------------------------------------------------------------------

	@Nested class G_capabilityDetection {

		@Test void g01_noRegistrationsHasNoCompleter() {
			assertFalse(new McpServerConfig().hasAnyCompleter());
		}

		@Test void g02_descriptorsWithoutCompletersHaveNoCompleter() {
			var config = new McpServerConfig()
				.addPrompt(prompt("p", new McpPromptArgument().setName("a")))
				.addResourceTemplate(template("file:///{x}", Map.of()));
			assertFalse(config.hasAnyCompleter());
		}

		@Test void g03_promptArgumentCompleterAloneIsDetected() {
			var config = new McpServerConfig().addPrompt(prompt("p", new McpPromptArgument().setName("a").setCompleter(STUB)));
			assertTrue(config.hasAnyCompleter());
		}

		@Test void g04_templateVariableCompleterAloneIsDetected() {
			var config = new McpServerConfig().addResourceTemplate(template("file:///{x}", Map.of("x", STUB)));
			assertTrue(config.hasAnyCompleter());
		}

		@Test void g05_detectionNeverInvokesTheCompleter() {
			var invoked = new boolean[1];
			McpCompleter tracking = (request, ctx) -> { invoked[0] = true; return McpCompletionResult.empty(); };
			var config = new McpServerConfig()
				.addPrompt(prompt("p", new McpPromptArgument().setName("a").setCompleter(tracking)))
				.addResourceTemplate(template("file:///{x}", Map.of("x", tracking)));
			assertTrue(config.hasAnyCompleter());
			assertFalse(invoked[0], "capability detection must never invoke a registered completer");
		}

		@Test void g06_undeclaredTemplateVariableCompleterIsNotDetected() {
			// A faulty handler that (incorrectly) returns a completer for an undeclared variable name only.
			McpResourceTemplateHandler faulty = new McpResourceTemplateHandler() {
				@Override public McpResourceTemplateSpec descriptor() { return new McpResourceTemplateSpec().setUriTemplate("file:///{name}"); }
				@Override public McpResourceOutcome read(String uri, Map<String,String> variables, BeanStore ctx) { return null; }
				@Override public McpCompleter completer(String variableName) { return "bogus".equals(variableName) ? STUB : null; }
			};
			var config = new McpServerConfig().addResourceTemplate(faulty);
			assertFalse(config.hasAnyCompleter());
		}
	}
}
