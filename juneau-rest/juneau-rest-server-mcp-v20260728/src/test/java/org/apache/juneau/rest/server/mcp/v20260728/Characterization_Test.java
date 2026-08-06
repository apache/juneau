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
package org.apache.juneau.rest.server.mcp.v20260728;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.function.*;

import javax.crypto.*;
import javax.crypto.spec.*;

import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.tracing.TraceContextCarrier;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.mcp.*;
import org.apache.juneau.rest.server.tracing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Wire-compatibility characterization fixtures for the {@code 2026-07-28} MCP JSON-RPC endpoint.
 *
 * <p>
 * These fixtures encode <em>current</em> behavior. A fixture body must never be edited to
 * accommodate a code change: if replay fails, the code change is wrong.
 *
 * <p>
 * Regenerate the {@code *.response.json} files (only ever against known-good code) with:
 * <p>
 * {@code mvn test -Drat.skip=true -pl juneau-rest/juneau-rest-server-mcp-v20260728 -Dtest=Characterization_Test -Djuneau.mcp.characterization.write=true}
 */
@SuppressWarnings({
	"resource" // MockRestClient is a Closeable test helper; lifetime is bounded by the test method.
})
class Characterization_Test {

	private static final Path DIR = Paths.get("src/test/resources/characterization");
	private static final boolean WRITE = Boolean.getBoolean("juneau.mcp.characterization.write");

	// --- fixture servlets -------------------------------------------------------------------

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F_Empty extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() { return new McpServerConfig(); }
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F_Full extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig()
				.addTool(tool("echo", McpSchema.of(JsonMap.of("type", "object", "required", List.of("text"))), a -> McpToolOutcome.text(String.valueOf(a.get("text")))))
				.addPrompt(prompt("greet", a -> new McpPromptOutcome().setDescription("d").setMessages(List.of(
					new McpPromptMessage().setRole(McpRole.USER).setContent(McpContentBlock.text("hi " + a.get("who")))))))
				.addResource(resource("file:///a", u -> new McpResourceOutcome().setContents(List.of(
					McpResourceContents.text(u, "text/plain", "body")))))
				.addResourceTemplate(new McpResourceTemplateSpec()
					.setUriTemplate("file:///{name}")
					.setName("templated")
					.setTitle("Template title")
					.setDescription("template description")
					.setMimeType("text/plain"));
		}
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F_Cache extends F_Full {
		private static final long serialVersionUID = 1L;

		@Override protected McpOptions createMcpOptions() {
			return new McpOptions().cache(c -> c
				.setDefaultHint(new McpCacheHint().setTtlMs(30000))
				.setToolsList(new McpCacheHint().setTtlMs(5000).setCacheScope(McpCacheScope.PRIVATE))
				.setPromptsList(new McpCacheHint().setTtlMs(0).setCacheScope(McpCacheScope.PUBLIC))
				.setResourceTemplatesList(new McpCacheHint().setTtlMs(60000).setCacheScope(McpCacheScope.PRIVATE))
				.setResourcesRead(new McpCacheHint().setTtlMs(2000))
				.addResourceReadOverride("file:///a",
					new McpCacheHint().setTtlMs(1000).setCacheScope(McpCacheScope.PRIVATE)));
		}
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F_Throw extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig()
				.addTool(tool("echo", null, a -> { throw new RuntimeException("handler failed"); }));
		}
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F_Schema extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			var schema = McpSchema.of(JsonMap.of(
				"type", "object",
				"required", List.of("text"),
				"properties", JsonMap.of("text", JsonMap.of("$ref", "#/$defs/text")),
				"$defs", JsonMap.of("text", JsonMap.of("type", "string")),
				"$id", "https://example.com/schemas/echo-input",
				"$schema", "https://json-schema.org/draft/2020-12/schema",
				"$comment", "input schema",
				"allOf", List.of(JsonMap.of("type", "object")),
				"oneOf", List.of(JsonMap.of("required", List.of("text"))),
				"if", JsonMap.of("properties", JsonMap.of("text", JsonMap.of("type", "string"))),
				"else", JsonMap.of()));
			return new McpServerConfig().addTool(tool("echo", schema, a -> new McpToolOutcome()));
		}
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F_Structured extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().addTool(new McpTypedToolHandler<JsonMap,Object>() {
				@Override public McpToolSpec descriptor() { return new McpToolSpec().setName("echo"); }
				@Override public java.lang.reflect.Type argumentType() { return JsonMap.class; }
				@Override public java.lang.reflect.Type resultType() { return Object.class; }
				@Override public Object call(JsonMap arguments, BeanStore ctx) { return arguments.get("value"); }
			});
		}
	}

	/**
	 * Backs every {@code TEMPLATE-*} fixture: one exact resource that always outranks any template match,
	 * plus resource-template registrations (in listing order) exercising decoded-scalar capture,
	 * reserved/unencoded capture, a listing-only non-winning two-variable template, and literal-prefix
	 * specificity. {@code TEMPLATE-read-unknown} deliberately reuses this template-registered servlet
	 * (rather than {@link F_Empty}) so its "no match" outcome proves a real miss against a populated
	 * registry, not a trivially empty one. No cache config is set, so C2 read-hint precedence resolves to
	 * "no hint" for every concrete URI here, exactly like every other cache-config-free fixture servlet;
	 * dedicated coverage of concrete-URI cache-hint precedence over template-backed reads lives in
	 * {@code McpCachePrecedence_Test#b04_readHintAppliedToTemplateBackedOutcomeByOriginalConcreteUri}.
	 */
	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F_Template extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig()
				.addResource(resource("file:///a", u -> new McpResourceOutcome().setContents(List.of(
					McpResourceContents.text(u, "text/plain", "exact-a")))))
				.addResourceTemplate(
					template("file:///{name}", "simple", "Simple template", "Captures one decoded path segment",
						(uri, vars) -> new McpResourceOutcome().setContents(List.of(
							McpResourceContents.text(uri, "text/plain", "name=" + vars.get("name"))))),
					template("file:///r/{+name}", "reserved", "Reserved template", "Captures unencoded path segments",
						(uri, vars) -> new McpResourceOutcome().setContents(List.of(
							McpResourceContents.text(uri, "text/plain", "name=" + vars.get("name"))))))
				.addResourceTemplate(new McpResourceTemplateSpec()
					.setUriTemplate("file:///{a}/{b}").setName("twovar").setTitle("Two variable template")
					.setDescription("Two single-segment variables").setMimeType("text/plain"))
				.addResourceTemplate(
					template("file:///fixed/{name}", "fixed", "Fixed-prefix template", "Fixed literal prefix with one variable",
						(uri, vars) -> new McpResourceOutcome().setContents(List.of(
							McpResourceContents.text(uri, "text/plain", "fixed name=" + vars.get("name"))))));
		}
	}

	/**
	 * Backs every {@code COMPLETE-*} fixture: a prompt whose two declared arguments each carry a
	 * deterministic completer (one ignoring context, one consuming it), and a resource-template variable
	 * completer whose current-value branches deterministically reproduce the small and the 101-value/capped
	 * cases from one registration.
	 */
	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F_Complete extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			var greet = new McpPromptSpec().setName("greet").setDescription("pd")
				.setArguments(List.of(
					new McpPromptArgument().setName("who")
						.setCompleter((request, ctx) -> new McpCompletionResult().setValues(List.of("Bob", "Alice", "Bob"))),
					new McpPromptArgument().setName("style")
						.setCompleter((request, ctx) -> {
							var greeting = request.getContextArguments().getOrDefault("greeting", "");
							return new McpCompletionResult().setValues(List.of(greeting + " Alice", greeting + " Bob"));
						})));
			return new McpServerConfig()
				.addPrompt(new McpPromptHandler() {
					@Override public McpPromptSpec descriptor() { return greet; }
					@Override public McpPromptOutcome get(Map<String,Object> arguments, BeanStore ctx) { return new McpPromptOutcome(); }
				})
				.addResourceTemplate(new McpResourceTemplateHandler() {
					@Override public McpResourceTemplateSpec descriptor() {
						return new McpResourceTemplateSpec().setUriTemplate("file:///{name}").setName("simple").setMimeType("text/plain");
					}
					@Override public McpResourceOutcome read(String uri, Map<String,String> variables, BeanStore ctx) { return null; }
					@Override public McpCompleter completer(String variableName) {
						if (! "name".equals(variableName))
							return null;
						return (request, ctx) -> {
							if ("cap".equals(request.getValue())) {
								var values = new ArrayList<String>();
								for (var i = 0; i < 101; i++)
									values.add("item" + i);
								return new McpCompletionResult().setValues(values).setTotal(101);
							}
							return new McpCompletionResult().setValues(List.of("alpha", "beta"));
						};
					}
				});
		}
	}

	/**
	 * Backs every {@code TRACE-*} fixture: one {@code echo} tool plus a deterministic in-memory
	 * {@link TracerHook} that consumes the real {@code 2026-07-28} {@link TraceContextCarrier}/
	 * {@link TraceOperation} the module publishes (see {@link McpRevision#TRACE_CONTEXT_EXTRACTOR}),
	 * so real dispatch exercises {@code params._meta}-vs-HTTP-header precedence and the
	 * {@code result._meta} echo without any OpenTelemetry import in this module.
	 */
	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F_Traced extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().setName("test").setVersion("1.0.0")
				.addTool(tool("echo", null, a -> McpToolOutcome.text(String.valueOf(a.get("text")))));
		}
		@Bean
		public TracerHook tracer() {
			return DeterministicTracer.INSTANCE;
		}
	}

	/**
	 * Deterministic, allocation-trivial {@link TracerHook}: renders the exact remote-parent value the
	 * real composite carrier resolved (already {@code params._meta}-over-HTTP-header precedence,
	 * applied by {@link McpRevision}'s {@code TraceContextExtractor}), or a fixed root {@code traceparent}
	 * literal when the carrier recognizes no remote parent at all. Every rendered value is therefore
	 * produced by executing this hook against the real carrier, never hand-invented per fixture.
	 */
	private static final class DeterministicTracer implements TracerHook {
		static final TracerHook INSTANCE = new DeterministicTracer();
		static final String ROOT_TRACEPARENT = "00-00000000000000000000000000000c-000000000000000c-01";

		@Override public Scope startSpan(RestRequest request) {
			return startSpan(request, null, TraceOperation.DEFAULT);
		}

		@Override public Scope startSpan(RestRequest request, TraceContextCarrier carrier, TraceOperation operation) {
			var traceparent = carrier == null ? null : carrier.get(RequestMeta.KEY_TRACEPARENT);
			request.setAttribute(TraceContextResponseProcessor.ATTR_TRACEPARENT, traceparent != null ? traceparent : ROOT_TRACEPARENT);
			var tracestate = carrier == null ? null : carrier.get(RequestMeta.KEY_TRACESTATE);
			if (tracestate != null)
				request.setAttribute(TraceContextResponseProcessor.ATTR_TRACESTATE, tracestate);
			var baggage = carrier == null ? null : carrier.get(RequestMeta.KEY_BAGGAGE);
			if (baggage != null)
				request.setAttribute(TraceContextResponseProcessor.ATTR_BAGGAGE, baggage);
			return new Scope() {
				@Override public void setStatusCode(int statusCode) { }
				@Override public void setError(Throwable error) { }
				@Override public void close() { }
			};
		}
	}

	/**
	 * Backs every {@code MRTR-*} fixture (SEP-2322 Multi-Round-Trip Requests): one {@code ask} tool that pauses
	 * (throws {@link McpInputRequiredSignal}) on a first-round call, and on RESUME either completes (echoing the
	 * client's {@code inputResponses}) or pauses a second time &mdash; branched on the decoded continuation so a
	 * single handler drives the complete / pause-again / max-rounds fixtures.
	 *
	 * <p>
	 * Wires a <b>deterministic, fixed-key</b> {@link FixedKeyGcmCodec} instead of the production ephemeral-key
	 * {@link AeadRequestStateCodec}, so a {@code requestState} the harness seals for a RESUME fixture (see
	 * {@link #mrtrToken(String)}) is unsealable by this servlet across a fresh process &mdash; a property the
	 * random-per-process production key deliberately does <b>not</b> have. The opaque token minted <em>into</em> a
	 * PAUSE response still embeds a wall-clock expiry, so responses are compared with the token value redacted (see
	 * {@link #normalizeRequestState(String)}); the fixtures pin the wire <em>shape</em>, never the opaque bytes.
	 */
	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F_Mrtr extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().addTool(new McpToolHandler() {
				@Override public McpToolSpec descriptor() { return new McpToolSpec().setName("ask").setDescription("desc:ask"); }
				@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) {
					var resume = ctx.getBean(McpMrtrResumeContext.class);
					if (resume.isEmpty())
						throw new McpInputRequiredSignal(Map.of("q1", Map.of("type", "elicitation")), "cont-1");
					if ("pause-again".equals(resume.get().continuation()))
						throw new McpInputRequiredSignal(Map.of("q2", Map.of("type", "elicitation")), "cont-2");
					return McpToolOutcome.text("resumed:" + resume.get().inputResponses().get("q1"));
				}
			});
		}
		@Override protected McpOptions createMcpOptions() {
			return new McpOptions().mrtr(m -> m.setCodec(new FixedKeyGcmCodec()));
		}
	}

	/**
	 * Backs every {@code ELICIT-*} fixture (SEP-2322 elicitation, built on the same PAUSE/RESUME mechanism
	 * {@link F_Mrtr} exercises generically). Unlike {@link F_Mrtr}'s placeholder {@code Map.of("type","elicitation")},
	 * this servlet's tools pause and resume through the real typed path ({@link ElicitationRequests}/
	 * {@link ElicitationResponses}, {@link ElicitRequest}/{@link ElicitResult}/{@link ElicitSchema}), so these
	 * fixtures characterize the actual elicitation wire shape, not just the generic MRTR envelope.
	 */
	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F_Elicit extends McpRestServlet {
		private static final long serialVersionUID = 1L;

		private static ElicitRequest confirmQuestion() {
			return new ElicitRequest()
				.setMessage("Proceed with deletion?")
				.setRequestedSchema(ElicitSchema.create().booleanField("confirm").title("Confirm").build());
		}

		@Override protected McpServerConfig createMcpConfig() {
			return new McpServerConfig()
				.addTool(new McpToolHandler() {
					@Override public McpToolSpec descriptor() { return new McpToolSpec().setName("confirm").setDescription("desc:confirm"); }
					@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) {
						var resume = ctx.getBean(McpMrtrResumeContext.class);
						if (resume.isEmpty())
							throw ElicitationRequests.of("confirm", confirmQuestion(), "cont-1");
						var answer = ElicitationResponses.get(resume.get(), "confirm");
						return McpToolOutcome.text("action:" + answer.getAction());
					}
				})
				.addTool(new McpToolHandler() {
					@Override public McpToolSpec descriptor() { return new McpToolSpec().setName("confirmTwo").setDescription("desc:confirmTwo"); }
					@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) {
						var resume = ctx.getBean(McpMrtrResumeContext.class);
						if (resume.isEmpty()) {
							var requests = new LinkedHashMap<String,ElicitRequest>();
							requests.put("confirm", confirmQuestion());
							requests.put("reason", new ElicitRequest().setMessage("Why?")
								.setRequestedSchema(ElicitSchema.create().stringField("reason").build()));
							throw ElicitationRequests.of(requests, "cont-multi");
						}
						var answers = ElicitationResponses.all(resume.get());
						return McpToolOutcome.text("confirm:" + answers.get("confirm").getAction()
							+ ",reason:" + answers.get("reason").getAction());
					}
				});
		}

		@Override protected McpOptions createMcpOptions() {
			return new McpOptions().mrtr(m -> m.setCodec(new FixedKeyGcmCodec()));
		}
	}

	/**
	 * Test-only deterministic {@link RequestStateCodec}: AES-256-GCM with a <b>hardcoded</b> key and nonce.
	 *
	 * <p>
	 * Unlike the production {@link AeadRequestStateCodec} (random per-process key + random per-seal nonce), this
	 * codec produces identical ciphertext for identical {@code (state, aad)} inputs and can unseal a token sealed by
	 * any other instance &mdash; exactly the reproducibility a committed characterization fixture needs. It is still
	 * genuinely tamper-evident (the GCM auth tag rejects the {@code MRTR-tampered-request-state} corruption) and
	 * honors the AAD binding. Fixed-nonce AES-GCM is catastrophically insecure for real traffic; it is used here
	 * <em>solely</em> because determinism, not confidentiality, is the goal in a fixture.
	 */
	static final class FixedKeyGcmCodec implements RequestStateCodec {
		private static final SecretKey KEY = new SecretKeySpec(new byte[32], "AES");
		private static final byte[] NONCE = new byte[12];
		private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();

		@Override public String seal(McpRequestState state, String aad, Principal principal) {
			try {
				var cipher = Cipher.getInstance("AES/GCM/NoPadding");
				cipher.init(Cipher.ENCRYPT_MODE, KEY, new GCMParameterSpec(128, NONCE));
				cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
				var ciphertext = cipher.doFinal(Json.of(state).getBytes(StandardCharsets.UTF_8));
				return B64.encodeToString(NONCE) + "." + B64.encodeToString(ciphertext);
			} catch (Exception e) {
				throw new RuntimeException("Fixture codec seal failed", e);
			}
		}

		@Override public Optional<McpRequestState> unseal(String token, String aad, Principal principal) {
			try {
				var parts = token.split("\\.", 2);
				var nonce = Base64.getUrlDecoder().decode(parts[0]);
				var ciphertext = Base64.getUrlDecoder().decode(parts[1]);
				var cipher = Cipher.getInstance("AES/GCM/NoPadding");
				cipher.init(Cipher.DECRYPT_MODE, KEY, new GCMParameterSpec(128, nonce));
				cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
				return Optional.of(Json.to(new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8), McpRequestState.class));
			} catch (@SuppressWarnings("unused") Exception e) {
				return Optional.empty();
			}
		}
	}

	// --- fixture handler factories ---------------------------------------------------------

	private static McpToolHandler tool(String name, McpSchema schema, Function<Map<String,Object>,McpToolOutcome> fn) {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() { return new McpToolSpec().setName(name).setDescription("desc:" + name).setInputSchema(schema); }
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) { return fn.apply(arguments); }
		};
	}

	private static McpPromptHandler prompt(String name, Function<Map<String,Object>,McpPromptOutcome> fn) {
		return new McpPromptHandler() {
			@Override public McpPromptSpec descriptor() { return new McpPromptSpec().setName(name).setDescription("pd"); }
			@Override public McpPromptOutcome get(Map<String,Object> arguments, BeanStore ctx) { return fn.apply(arguments); }
		};
	}

	private static McpResourceHandler resource(String uri, Function<String,McpResourceOutcome> fn) {
		return new McpResourceHandler() {
			@Override public McpResourceSpec descriptor() { return new McpResourceSpec().setUri(uri).setName("a").setMimeType("text/plain"); }
			@Override public McpResourceOutcome read(String u, BeanStore ctx) { return fn.apply(u); }
		};
	}

	private static McpResourceTemplateHandler template(String uriTemplate, String name, String title, String description,
			BiFunction<String,Map<String,String>,McpResourceOutcome> fn) {
		return new McpResourceTemplateHandler() {
			@Override public McpResourceTemplateSpec descriptor() {
				return new McpResourceTemplateSpec().setUriTemplate(uriTemplate).setName(name).setTitle(title)
					.setDescription(description).setMimeType("text/plain");
			}
			@Override public McpResourceOutcome read(String uri, Map<String,String> variables, BeanStore ctx) { return fn.apply(uri, variables); }
		};
	}

	/**
	 * Maps each fixture to the servlet whose tool/prompt/resource registrations it needs.
	 *
	 * <p>
	 * Matched per literal fixture name, not by prefix: {@code ERROR-missing-tool} needs {@code echo}
	 * <em>unregistered</em> while {@code ERROR-handler-failure} needs it registered and throwing, so
	 * both share the {@code ERROR-} prefix but resolve to different servlets.
	 */
	private static Class<?> servletFor(String fixture) {
		return switch (fixture) {
			case "ERROR-handler-failure" -> F_Throw.class;
			case "SCHEMA-draft-2020-12" -> F_Schema.class;
			case "FULL-tools-list", "FULL-tools-call", "FULL-prompts-list", "FULL-prompts-get",
				"FULL-resources-list", "FULL-resources-read", "FULL-resource-templates-list",
				"HEADER-valid-named", "STATELESS-repeat" -> F_Full.class;
			case "CACHE-tools-list", "CACHE-prompts-list", "CACHE-resources-list",
				"CACHE-resource-templates-list", "CACHE-resources-read" -> F_Cache.class;
			case "STRUCTURED-object", "STRUCTURED-array", "STRUCTURED-string", "STRUCTURED-boolean", "STRUCTURED-null" -> F_Structured.class;
			case "TEMPLATE-simple-read", "TEMPLATE-reserved-read", "TEMPLATE-exact-precedence",
				"TEMPLATE-most-specific", "TEMPLATE-read-unknown" -> F_Template.class;
			case "COMPLETE-prompt", "COMPLETE-resource-template", "COMPLETE-context",
				"COMPLETE-empty-unknown", "COMPLETE-capped" -> F_Complete.class;
			case "TRACE-meta-parent", "TRACE-http-fallback", "TRACE-meta-wins",
				"TRACE-tracestate-baggage", "TRACE-response-echo", "TRACE-jsonrpc-error" -> F_Traced.class;
			case "MRTR-input-required-response", "MRTR-resume-complete", "MRTR-resume-input-required-again",
				"MRTR-tampered-request-state", "MRTR-expired-request-state", "MRTR-unsupported-capability",
				"MRTR-max-rounds-exceeded" -> F_Mrtr.class;
			case "ELICIT-single-request-pause", "ELICIT-multi-request-pause", "ELICIT-resume-accept-complete",
				"ELICIT-resume-decline", "ELICIT-resume-cancel" -> F_Elicit.class;
			default -> F_Empty.class;
		};
	}

	// --- MRTR requestState sealing/redaction ------------------------------------------------

	/** Placeholder in an {@code MRTR-*} resume fixture's committed request body, replaced at replay time. */
	private static final String TOKEN_PLACEHOLDER = "__REQUEST_STATE__";

	// Fixed expiry constants embedded in a sealed requestState so a RESUME fixture's token is reproducible
	// (never "now + ttl"): far-future for a still-valid token, a fixed past instant for the expired fixture.
	private static final long FAR_FUTURE_MS = 32503680000000L; // ~ year 3000
	private static final long PAST_MS = 1000L;

	// Mirror of McpRevision#aad(method, target): the sealed AAD binds the operation target (tool name / resource
	// uri) in addition to method+version, so every fixture token below must seal under the same target its
	// resume request names or the GCM tag check fails on unseal.
	private static String aad(String method, String target) {
		return method + '\u0000' + McpProtocol.VERSION_2026_07_28 + '\u0000' + (target == null ? "" : target);
	}

	/**
	 * The {@code requestState} the harness seals (with {@link FixedKeyGcmCodec}, matching {@link F_Mrtr}'s codec)
	 * for each RESUME-family fixture. Substituted into the committed request in place of {@link #TOKEN_PLACEHOLDER}
	 * so the token stays framework-owned/opaque while the request pins the surrounding wire shape.
	 *
	 * <p>
	 * {@code MRTR-resume-complete} and {@code MRTR-resume-input-required-again} have byte-identical committed
	 * request files by design: the behavioral difference between the two fixtures lives entirely inside the sealed
	 * opaque token's continuation value ({@code "complete-me"} vs {@code "pause-again"}), not in anything visible
	 * on the wire request body.
	 *
	 * <p>
	 * The plan's original "commit the corrupted requestState as a frozen literal" constraint for
	 * {@code MRTR-tampered-request-state} was consciously superseded: because {@link FixedKeyGcmCodec} makes
	 * {@code seal(...)} fully deterministic (fixed key, fixed nonce), regenerating the tampered token on every run
	 * via {@code tamper(codec.seal(...))} is equally reproducible and avoids hand-maintaining opaque ciphertext.
	 */
	// None of the committed request bodies below carry an "arguments" member, so every sealed fixture token
	// must carry the same empty-arguments sentinel hash the dispatcher derives from the (absent) resume request
	// arguments -- otherwise every RESUME-family fixture that reaches the handler would newly fail with the
	// argument-hash mismatch check.
	private static final String NO_ARGS_HASH = McpRevision.argumentsHash(Map.of());

	private static String mrtrToken(String fixture) {
		var codec = new FixedKeyGcmCodec();
		return switch (fixture) {
			case "MRTR-resume-complete" -> codec.seal(new McpRequestState("complete-me", "tools/call", 1, FAR_FUTURE_MS, "jti-complete", NO_ARGS_HASH), aad("tools/call", "ask"));
			case "MRTR-resume-input-required-again" -> codec.seal(new McpRequestState("pause-again", "tools/call", 1, FAR_FUTURE_MS, "jti-pause-again", NO_ARGS_HASH), aad("tools/call", "ask"));
			case "MRTR-expired-request-state" -> codec.seal(new McpRequestState("cont-1", "tools/call", 1, PAST_MS, "jti-expired", NO_ARGS_HASH), aad("tools/call", "ask"));
			case "MRTR-max-rounds-exceeded" -> codec.seal(new McpRequestState("cont-1", "tools/call", McpMrtrConfig.DEFAULT_MAX_ROUNDS, FAR_FUTURE_MS, "jti-max-rounds", NO_ARGS_HASH), aad("tools/call", "ask"));
			case "MRTR-tampered-request-state" -> tamper(codec.seal(new McpRequestState("cont-1", "tools/call", 1, FAR_FUTURE_MS, "jti-tampered", NO_ARGS_HASH), aad("tools/call", "ask")));
			case "ELICIT-resume-accept-complete", "ELICIT-resume-decline", "ELICIT-resume-cancel" ->
				codec.seal(new McpRequestState("cont-1", "tools/call", 1, FAR_FUTURE_MS, "jti-elicit", NO_ARGS_HASH), aad("tools/call", "confirm"));
			default -> throw new IllegalArgumentException("No MRTR token mapping for fixture: " + fixture);
		};
	}

	/** Flips one ciphertext byte of a valid token so its GCM tag fails verification (the tamper fixture). */
	private static String tamper(String token) {
		var parts = token.split("\\.", 2);
		var ciphertext = Base64.getUrlDecoder().decode(parts[1]);
		ciphertext[0] ^= 1;
		return parts[0] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(ciphertext);
	}

	/**
	 * Redacts the opaque {@code requestState} token value in a response body to a fixed placeholder before
	 * comparison. The token is per-process-nondeterministic ciphertext (production uses a random key and embeds a
	 * wall-clock expiry), so pinning its exact bytes is neither possible nor meaningful; the fixture pins its
	 * presence and the surrounding wire shape instead.
	 */
	private static String normalizeRequestState(String responseBody) {
		return responseBody.replaceAll("\"requestState\":\"[^\"]+\"", "\"requestState\":\"<sealed>\"");
	}

	// --- replay ----------------------------------------------------------------------------

	static List<String> fixtures() throws Exception {
		try (var s = Files.list(DIR)) {
			return s.map(x -> x.getFileName().toString())
				.filter(x -> x.endsWith(".request.json"))
				.map(x -> x.substring(0, x.length() - ".request.json".length()))
				.sorted()
				.toList();
		}
	}

	@ParameterizedTest
	@MethodSource("fixtures")
	void a01_wireIsUnchanged(String fixture) throws Exception {
		var actual = normalizeRequestState(replayHttp(fixture));
		var expected = DIR.resolve(fixture + ".response.json");
		if (WRITE) {
			Files.writeString(expected, actual);
			return;
		}
		assertEquals(Files.readString(expected), actual,
			() -> fixture + ": WIRE FORMAT CHANGED. Do not update the fixture — fix the code.");
	}

	/**
	 * The opaque {@code requestState} minted into {@code MRTR-resume-input-required-again}'s second PAUSE is
	 * redacted for {@code a01}'s byte comparison, so its embedded round counter is verified here instead: unseal
	 * the live token with the same fixed-key codec {@link F_Mrtr} uses and assert the round advanced to 2 (plan
	 * Phase 4 Task 12 decode-and-check).
	 */
	@Test
	void a02_resumeInputRequiredAgain_sealedRoundIsTwo() throws Exception {
		var raw = replayHttp("MRTR-resume-input-required-again");
		var envelope = Json.to(raw, JsonMap.class);
		var result = (Map<?,?>) envelope.get("result");
		var token = (String) result.get("requestState");
		var state = new FixedKeyGcmCodec().unseal(token, aad("tools/call", "ask")).orElseThrow();
		assertEquals(2, state.round());
		assertEquals("cont-2", state.continuation());
	}

	private String replayHttp(String fixture) throws Exception {
		var requestBody = Files.readString(DIR.resolve(fixture + ".request.json")).strip();
		if (requestBody.contains(TOKEN_PLACEHOLDER))
			requestBody = requestBody.replace(TOKEN_PLACEHOLDER, mrtrToken(fixture));
		var headers = loadHeaders(fixture);
		var client = MockRestClient.create(servletFor(fixture)).json()
			.contentType("application/json").accept("application/json").ignoreErrors().build();
		var req = client.post("/").contentString(requestBody);
		headers.forEach(req::header);
		var res = req.run();
		assertEquals(200, res.getStatusCode(), () -> fixture + ": HTTP status changed");
		return res.getContent().asString();
	}

	private static Map<String,String> loadHeaders(String fixture) throws IOException {
		var props = new Properties();
		try (var in = Files.newBufferedReader(DIR.resolve(fixture + ".headers.properties"))) {
			props.load(in);
		}
		var m = new LinkedHashMap<String,String>();
		for (var name : props.stringPropertyNames())
			m.put(name, props.getProperty(name));
		return m;
	}
}
