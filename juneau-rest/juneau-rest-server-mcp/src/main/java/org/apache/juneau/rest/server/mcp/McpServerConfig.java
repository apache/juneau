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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.commons.inject.*;

/**
 * Aggregate configuration consumed by the bound {@link McpRevision}.
 *
 * <p>
 * Applications register a single {@link McpServerConfig} (typically as a bean in their {@code RestContext}
 * bean store) listing the tools, prompts, resources, and resource templates to expose, plus optional server
 * metadata and a pagination strategy.
 *
 * <p>
 * This type is revision-neutral: it holds no field typed with any protocol revision's wire beans, and
 * in particular holds no capabilities field in any form. The protocol version is owned by
 * {@link McpRevision#protocolVersion()}, and an explicit capabilities advertisement is owned by a
 * revision-specific hook on that revision's servlet or endpoint mixin.
 */
public class McpServerConfig {

	private String name;
	private String version;
	private String instructions;
	private List<McpToolHandler> tools = l();
	private List<McpPromptHandler> prompts = l();
	private List<McpResourceHandler> resources = l();
	private List<McpResourceTemplateHandler> resourceTemplates = l();

	@SuppressWarnings({
		"java:S3077" // Safe-publication snapshot only: written solely via publishResourceTemplates()/ensureResourceTemplatesValid() after the resourceTemplates write it guards, so a racing re-validation is at worst redundant (idempotent), never corrupting.
	})
	private volatile List<McpResourceTemplateHandler> validatedResourceTemplatesSnapshot = List.of();
	@SuppressWarnings({
		"java:S3077" // Safe-publication snapshot only: written in lockstep with validatedResourceTemplatesSnapshot (same guard), so a racing re-validation is at worst a redundant recompile, never a corrupt/mismatched read.
	})
	private volatile Map<McpResourceTemplateHandler, McpUriTemplateMatcher> compiledResourceTemplateMatchers = Map.of();
	private McpCursor cursor = McpCursor.SINGLE_PAGE;

	/**
	 * Server name reported in {@code initialize}.
	 *
	 * <p>
	 * When both this and {@link #getVersion()} are {@code null}, the bound revision substitutes its
	 * own default server identity.
	 *
	 * @return The name, or {@code null} if not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the server name.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpServerConfig setName(String value) {
		name = value;
		return this;
	}

	/**
	 * Server version reported in {@code initialize}.
	 *
	 * @return The version, or {@code null} if not set.
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Sets the server version.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpServerConfig setVersion(String value) {
		version = value;
		return this;
	}

	/**
	 * Optional instructions surfaced via {@code initialize}.
	 *
	 * @return The instructions, or {@code null} if not set.
	 */
	public String getInstructions() {
		return instructions;
	}

	/**
	 * Sets initialize instructions.
	 *
	 * @param instructions The new value.
	 * @return This object (for method chaining).
	 */
	public McpServerConfig setInstructions(String instructions) {
		this.instructions = instructions;
		return this;
	}

	/**
	 * Registered tool handlers.
	 *
	 * @return Mutable list of handlers. Never {@code null}.
	 */
	public List<McpToolHandler> getTools() {
		return tools;
	}

	/**
	 * Sets the tool handler list.
	 *
	 * @param tools The new value (or {@code null} to clear).
	 * @return This object (for method chaining).
	 */
	public McpServerConfig setTools(List<McpToolHandler> tools) {
		this.tools = tools == null ? l() : new ArrayList<>(tools);
		return this;
	}

	/**
	 * Convenience: append one or more tool handlers.
	 *
	 * @param handlers Handlers to add.
	 * @return This object (for method chaining).
	 */
	public McpServerConfig addTool(McpToolHandler... handlers) {
		Collections.addAll(this.tools, handlers);
		return this;
	}

	/**
	 * Convenience: adapt and append a single typed tool handler.
	 *
	 * <p>
	 * The handler is bridged into a raw {@link McpToolHandler} via
	 * {@link McpTypedHandlers#adaptTool(McpTypedToolHandler)} before it is stored. This overload is intentionally
	 * singular: a typed varargs overload would make {@code addTool()} ambiguous. To register multiple typed
	 * handlers, use chained {@code addTool(...)} calls, or adapt them explicitly with
	 * {@link McpTypedHandlers#adaptTool(McpTypedToolHandler)} and pass the results to
	 * {@link #addTool(McpToolHandler...)}.
	 *
	 * @param handler The typed handler to adapt and add.
	 * @return This object (for method chaining).
	 */
	public McpServerConfig addTool(McpTypedToolHandler<?,?> handler) {
		tools.add(McpTypedHandlers.adaptTool(handler));
		return this;
	}

	/**
	 * Registered prompt handlers.
	 *
	 * @return Mutable list of handlers. Never {@code null}.
	 */
	public List<McpPromptHandler> getPrompts() {
		return prompts;
	}

	/**
	 * Sets the prompt handler list.
	 *
	 * <p>
	 * The complete resulting registry is validated before publication; on failure, the previously published
	 * registry is left unchanged. See {@link #addPrompt(McpPromptHandler...)} for the validated conditions.
	 *
	 * @param prompts The new value (or {@code null} to clear).
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If the resulting registry would be invalid.
	 */
	public McpServerConfig setPrompts(List<McpPromptHandler> prompts) {
		var candidate = prompts == null ? new ArrayList<McpPromptHandler>() : new ArrayList<>(prompts);
		validatePrompts(candidate);
		this.prompts = candidate;
		return this;
	}

	/**
	 * Convenience: append one or more prompt handlers.
	 *
	 * <p>
	 * The complete resulting registry - the handlers already registered plus {@code handlers} - is validated
	 * before publication; on failure, the previously published registry is left unchanged. Fails fast with
	 * {@link IllegalArgumentException} for a single prompt whose declared {@link McpPromptArgument} list either:
	 * <ul>
	 * 	<li>repeats a non-{@code null} argument name; or
	 * 	<li>attaches a non-{@code null} {@link McpPromptArgument#getCompleter() completer} to a {@code null} or
	 * 		blank argument name.
	 * </ul>
	 *
	 * @param handlers Handlers to add.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If the resulting registry would be invalid.
	 */
	public McpServerConfig addPrompt(McpPromptHandler... handlers) {
		var candidate = new ArrayList<>(this.prompts);
		Collections.addAll(candidate, handlers);
		validatePrompts(candidate);
		this.prompts = candidate;
		return this;
	}

	/**
	 * Validates one candidate prompt registry in full, throwing {@link IllegalArgumentException} on the first
	 * invalid entry, so a misconfigured prompt argument is caught at registration time rather than during
	 * {@code completion/complete} dispatch. See {@link #addPrompt(McpPromptHandler...)} for the validated
	 * conditions.
	 *
	 * <p>
	 * A {@code null} handler, a handler whose {@link McpPromptHandler#descriptor()} returns {@code null}, or a
	 * descriptor with {@code null} {@link McpPromptSpec#getArguments() arguments} carries nothing to validate
	 * here and is silently skipped; those are not this validation's concern.
	 */
	@SuppressWarnings({
		"java:S135", // Three sequential null/absent-field skips are simple, independent guard clauses, not branching logic worth extracting.
		"java:S3776" // Cognitive complexity from the guard-clause density is inherent to exhaustively validating a nested registry; standing project policy is to suppress rather than refactor for this metric alone.
	})
	private static void validatePrompts(List<McpPromptHandler> candidate) {
		for (var i = 0; i < candidate.size(); i++) {
			var handler = candidate.get(i);
			if (handler == null)
				continue;
			var descriptor = handler.descriptor();
			if (descriptor == null)
				continue;
			var arguments = descriptor.getArguments();
			if (arguments == null)
				continue;
			var seenNames = new HashSet<String>();
			for (var j = 0; j < arguments.size(); j++) {
				var argument = arguments.get(j);
				if (argument == null)
					continue;
				var name = argument.getName();
				if (argument.getCompleter() != null && (name == null || name.isBlank()))
					throw iaex(
						"Invalid prompt registration at index %s (prompt '%s'), argument index %s: a completer must not be attached to a null or blank argument name",
						i, descriptor.getName(), j);
				if (name != null && ! seenNames.add(name))
					throw iaex("Invalid prompt registration at index %s (prompt '%s'): duplicate argument name '%s'",
						i, descriptor.getName(), name);
			}
		}
	}

	/**
	 * Registered resource handlers.
	 *
	 * @return Mutable list of handlers. Never {@code null}.
	 */
	public List<McpResourceHandler> getResources() {
		return resources;
	}

	/**
	 * Sets the resource handler list.
	 *
	 * @param resources The new value (or {@code null} to clear).
	 * @return This object (for method chaining).
	 */
	public McpServerConfig setResources(List<McpResourceHandler> resources) {
		this.resources = resources == null ? l() : new ArrayList<>(resources);
		return this;
	}

	/**
	 * Convenience: append one or more resource handlers.
	 *
	 * @param handlers Handlers to add.
	 * @return This object (for method chaining).
	 */
	public McpServerConfig addResource(McpResourceHandler... handlers) {
		Collections.addAll(this.resources, handlers);
		return this;
	}

	/**
	 * Registered resource-template handlers.
	 *
	 * <p>
	 * Every read of this property (re)validates the current registry if the list has been mutated directly
	 * since the last validated {@link #setResourceTemplates(List)}, {@link #addResourceTemplate(McpResourceTemplateHandler...)},
	 * {@link #addResourceTemplate(McpResourceTemplateSpec...)}, or {@link #setResourceTemplateSpecs(List)} call,
	 * so direct-list corruption is caught on first consumption rather than silently propagating to dispatch.
	 * A registry that has not been mutated since its last successful validation is returned without
	 * recompiling its templates.
	 *
	 * @return Mutable list of handlers. Never {@code null}.
	 * @throws IllegalArgumentException If the list was mutated directly into an invalid state (see
	 * 	{@link #addResourceTemplate(McpResourceTemplateHandler...)} for the validated conditions).
	 */
	public List<McpResourceTemplateHandler> getResourceTemplates() {
		ensureResourceTemplatesValid();
		return resourceTemplates;
	}

	/**
	 * Sets the resource-template handler list.
	 *
	 * <p>
	 * The complete resulting registry is validated before publication; on failure, the previously published
	 * registry is left unchanged. See {@link #addResourceTemplate(McpResourceTemplateHandler...)} for the
	 * validated conditions.
	 *
	 * @param value The new value (or {@code null} to clear).
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If the resulting registry would be invalid.
	 */
	public McpServerConfig setResourceTemplates(List<McpResourceTemplateHandler> value) {
		var candidate = value == null ? new ArrayList<McpResourceTemplateHandler>() : new ArrayList<>(value);
		publishResourceTemplates(candidate);
		return this;
	}

	/**
	 * Sets the resource-template registry from bare descriptors, replacing each with a built-in listing-only
	 * handler equivalent to what {@link #addResourceTemplate(McpResourceTemplateSpec...)} wraps.
	 *
	 * <p>
	 * The complete resulting registry is validated before publication; on failure, the previously published
	 * registry is left unchanged.
	 *
	 * @param value The new descriptor list (or {@code null} to clear).
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If the resulting registry would be invalid.
	 */
	public McpServerConfig setResourceTemplateSpecs(List<McpResourceTemplateSpec> value) {
		return setResourceTemplates(value == null ? null : value.stream().map(McpServerConfig::listingOnlyTemplate).toList());
	}

	/**
	 * Convenience: append one or more resource-template handlers.
	 *
	 * <p>
	 * The complete resulting registry - the handlers already registered plus {@code values} - is validated
	 * before publication; on failure, the previously published registry is left unchanged. Fails fast with
	 * {@link IllegalArgumentException} naming the offending registration index and template for:
	 * <ul>
	 * 	<li>a {@code null} handler;
	 * 	<li>a handler whose {@link McpResourceTemplateHandler#descriptor()} returns {@code null};
	 * 	<li>a descriptor with a {@code null} or blank {@code uriTemplate};
	 * 	<li>a malformed {@code uriTemplate} (delegated to {@link McpUriTemplateMatcher#compile(String)}); and
	 * 	<li>an exact-duplicate {@code uriTemplate} string within the resulting registry.
	 * </ul>
	 * A syntactically valid {@code uriTemplate} outside {@link McpUriTemplateMatcher}'s reverse-matchable
	 * subset is a legal registration, not an error.
	 *
	 * @param values Handlers to add.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If the resulting registry would be invalid.
	 */
	public McpServerConfig addResourceTemplate(McpResourceTemplateHandler... values) {
		var candidate = new ArrayList<>(resourceTemplates);
		Collections.addAll(candidate, values);
		publishResourceTemplates(candidate);
		return this;
	}

	/**
	 * Convenience: append one or more resource-template descriptors as built-in listing-only handlers.
	 *
	 * <p>
	 * Each descriptor is wrapped in a handler whose {@link McpResourceTemplateHandler#descriptor()} returns
	 * that same descriptor instance, whose {@link McpResourceTemplateHandler#read(String, Map, BeanStore) read}
	 * always returns {@code null}, and whose {@link McpResourceTemplateHandler#completer(String) completer}
	 * always returns {@code null}. The resulting registry is validated exactly as in
	 * {@link #addResourceTemplate(McpResourceTemplateHandler...)}.
	 *
	 * @param values Descriptors to add.
	 * @return This object (for method chaining).
	 * @throws IllegalArgumentException If the resulting registry would be invalid.
	 */
	public McpServerConfig addResourceTemplate(McpResourceTemplateSpec... values) {
		var wrapped = new McpResourceTemplateHandler[values.length];
		for (var i = 0; i < values.length; i++)
			wrapped[i] = listingOnlyTemplate(values[i]);
		return addResourceTemplate(wrapped);
	}

	private static McpResourceTemplateHandler listingOnlyTemplate(McpResourceTemplateSpec spec) {
		return new McpResourceTemplateHandler() {
			@Override public McpResourceTemplateSpec descriptor() { return spec; }
			@Override public McpResourceOutcome read(String uri, Map<String,String> variables, BeanStore ctx) { return null; }
		};
	}

	/**
	 * Validates {@code candidate} and, on success, publishes it as the current registry and its already-valid
	 * snapshot (so the very next {@link #getResourceTemplates()} call does not repeat the validation just
	 * performed here). On failure the previously published registry is left untouched.
	 */
	private void publishResourceTemplates(List<McpResourceTemplateHandler> candidate) {
		var compiled = validateResourceTemplates(candidate);
		resourceTemplates = candidate;
		validatedResourceTemplatesSnapshot = List.copyOf(candidate);
		compiledResourceTemplateMatchers = compiled;
	}

	/**
	 * Revalidates the live {@link #resourceTemplates} list if it differs from the last validated snapshot,
	 * catching mutation performed directly through the mutable list returned by {@link #getResourceTemplates()}.
	 * A registry that is unchanged since its last successful validation is left alone: templates are not
	 * recompiled on every call.
	 */
	private void ensureResourceTemplatesValid() {
		var current = resourceTemplates;
		if (validatedResourceTemplatesSnapshot.equals(current))
			return;
		var compiled = validateResourceTemplates(current);
		validatedResourceTemplatesSnapshot = List.copyOf(current);
		compiledResourceTemplateMatchers = compiled;
	}

	/**
	 * Validates one candidate resource-template registry in full, throwing {@link IllegalArgumentException}
	 * on the first invalid entry. See {@link #addResourceTemplate(McpResourceTemplateHandler...)} for the
	 * validated conditions.
	 *
	 * <p>
	 * As a side benefit of validation already compiling every entry's {@link McpUriTemplateMatcher} once,
	 * this returns an identity-keyed map from each validated handler to its compiled matcher, so callers can
	 * publish it as {@link #compiledResourceTemplateMatchers} and reuse the compiled matcher on the
	 * per-request paths ({@link #resolveResourceTemplate(String)}, {@link #hasAnyCompleter()},
	 * {@link #templateCompleter(String, String)}) instead of recompiling from the {@code uriTemplate} string
	 * on every call.
	 */
	private static Map<McpResourceTemplateHandler, McpUriTemplateMatcher> validateResourceTemplates(List<McpResourceTemplateHandler> candidate) {
		var seenTemplates = new HashSet<String>();
		var compiled = new IdentityHashMap<McpResourceTemplateHandler, McpUriTemplateMatcher>();
		for (var i = 0; i < candidate.size(); i++) {
			var handler = candidate.get(i);
			if (handler == null)
				throw iaex("Invalid resource-template registration at index %s: handler must not be null", i);
			var descriptor = handler.descriptor();
			if (descriptor == null)
				throw iaex("Invalid resource-template registration at index %s: descriptor() must not return null", i);
			var uriTemplate = descriptor.getUriTemplate();
			if (uriTemplate == null || uriTemplate.isBlank())
				throw iaex("Invalid resource-template registration at index %s (template '%s'): uriTemplate must not be null or blank",
					i, uriTemplate);
			McpUriTemplateMatcher matcher;
			try {
				matcher = McpUriTemplateMatcher.compile(uriTemplate);
			} catch (IllegalArgumentException e) {
				throw iaex(e, "Invalid resource-template registration at index %s (template '%s'): %s", i, uriTemplate, e.getMessage());
			}
			if (! seenTemplates.add(uriTemplate))
				throw iaex("Invalid resource-template registration at index %s (template '%s'): duplicate uriTemplate", i, uriTemplate);
			compiled.put(handler, matcher);
		}
		return Collections.unmodifiableMap(compiled);
	}

	/**
	 * Resolves the completer for one declared variable of a registered resource-template handler, enforcing
	 * in one neutral place - rather than in each dated adapter - that an undeclared variable name never
	 * receives a completer even if the handler itself misbehaves and returns one.
	 *
	 * @param handler The resource-template handler. Must not be {@code null}, and its
	 * 	{@link McpResourceTemplateHandler#descriptor()}.{@code uriTemplate} must be well-formed (as enforced by
	 * 	registration validation).
	 * @param variableName The variable name from a {@code completion/complete} request.
	 * @return The handler's completer for that variable, or {@code null} if the variable is not declared by
	 * 	the template or the handler supplies no completer for it.
	 */
	static McpCompleter resourceTemplateCompleter(McpResourceTemplateHandler handler, String variableName) {
		return resourceTemplateCompleter(handler, variableName, McpUriTemplateMatcher.compile(handler.descriptor().getUriTemplate()));
	}

	/**
	 * Same contract as {@link #resourceTemplateCompleter(McpResourceTemplateHandler, String)}, but takes an
	 * already-compiled {@code matcher} (typically the one cached in {@link #compiledResourceTemplateMatchers}
	 * for a registered handler) instead of recompiling the template from its {@code uriTemplate} string.
	 */
	static McpCompleter resourceTemplateCompleter(McpResourceTemplateHandler handler, String variableName, McpUriTemplateMatcher matcher) {
		var declared = matcher.variableNames();
		return declared.contains(variableName) ? handler.completer(variableName) : null;
	}

	/**
	 * Resolves the concrete {@code resources/read} request URI {@code uri} against the registered
	 * resource-template registry, selecting the single winning template handler using the deterministic
	 * specificity ranking shared identically by both dated adapters (spec &sect;7).
	 *
	 * <p>
	 * A candidate is every reverse-matchable ({@link McpUriTemplateMatcher#isReverseMatchable()}) template
	 * handler whose {@link McpUriTemplateMatcher#match(String)} succeeds against {@code uri}; a
	 * non-reverse-matchable or non-matching template handler is never a candidate. Successful candidates are
	 * ranked, highest priority first, by exactly:
	 * <ol>
	 * 	<li>greater {@link McpUriTemplateMatcher#literalOctetCount() literalOctetCount};
	 * 	<li>then fewer {@link McpUriTemplateMatcher#variableCount() variableCount}; and
	 * 	<li>then earlier registration index (candidates are scanned in registration order and a later
	 * 		candidate only displaces the current winner on a strictly better key, so the first-registered
	 * 		candidate wins a complete tie).
	 * </ol>
	 * No other tie-breaker - operator type, capture width, descriptor name/title, or match-time value
	 * length - is consulted.
	 *
	 * <p>
	 * This method does not consult exact {@link McpResourceHandler} registrations. Exact lookup is
	 * unconditionally higher precedence than any template match; callers must perform exact lookup first
	 * and invoke this method only on an exact miss.
	 *
	 * @param uri The concrete request URI. Can be {@code null}, in which case this method returns
	 * 	{@code null}.
	 * @return The winning match (handler plus its immutable, insertion-ordered decoded variable map), or
	 * 	{@code null} if no registered reverse-matchable template matches {@code uri}.
	 */
	@SuppressWarnings({
		"java:S135", // Three sequential null/non-matching skips are simple, independent guard clauses over one candidate loop, not branching logic worth extracting.
		"java:S3776" // Cognitive complexity from the guard-clause density plus the ranking comparison is inherent to this method's documented deterministic tie-break contract; standing project policy is to suppress rather than refactor for this metric alone.
	})
	public ResourceTemplateMatch resolveResourceTemplate(String uri) {
		if (uri == null)
			return null;
		McpResourceTemplateHandler winner = null;
		Map<String,String> winnerVariables = null;
		McpUriTemplateMatcher winnerMatcher = null;
		var templates = getResourceTemplates();
		var matchers = compiledResourceTemplateMatchers;
		for (var handler : templates) {
			if (handler == null)
				continue;
			var descriptor = handler.descriptor();
			if (descriptor == null)
				continue;
			var matcher = matchers.get(handler);
			if (matcher == null)
				matcher = McpUriTemplateMatcher.compile(descriptor.getUriTemplate());
			if (! matcher.isReverseMatchable())
				continue;
			var variables = matcher.match(uri);
			if (variables == null)
				continue;
			if (winner == null
					|| matcher.literalOctetCount() > winnerMatcher.literalOctetCount()
					|| (matcher.literalOctetCount() == winnerMatcher.literalOctetCount()
						&& matcher.variableCount() < winnerMatcher.variableCount())) {
				winner = handler;
				winnerVariables = variables;
				winnerMatcher = matcher;
			}
		}
		return winner == null ? null : new ResourceTemplateMatch(winner, winnerVariables);
	}

	/**
	 * The result of {@link #resolveResourceTemplate(String)}: the winning resource-template handler paired
	 * with the variables decoded from the concrete request URI.
	 *
	 * @param handler The winning handler. Never {@code null}.
	 * @param variables The immutable, insertion-ordered map of the handler's declared variables to their
	 * 	decoded values, captured from the request URI. Never {@code null}.
	 */
	public record ResourceTemplateMatch(McpResourceTemplateHandler handler, Map<String,String> variables) {}

	/**
	 * Resolves the completer registered for one declared argument of a registered prompt, by exact prompt name
	 * and exact argument name.
	 *
	 * <p>
	 * This is one half of the neutral cross-target completion lookup shared identically by both dated
	 * adapters; see {@link #templateCompleter(String, String)} for the resource-template half and
	 * {@link #completer(McpCompletionRef, String)} for the unified entry point.
	 *
	 * @param promptName The exact prompt name to look up. Can be {@code null}, in which case this method
	 * 	returns {@code null}.
	 * @param argumentName The exact argument name to look up. Can be {@code null}, in which case this method
	 * 	returns {@code null}.
	 * @return The argument's completer, or {@code null} if the prompt is unknown, the argument is undeclared,
	 * 	or the argument declares no completer. Never invokes the completer.
	 */
	@SuppressWarnings({
		"java:S135" // Two sequential null/name-mismatch skips are simple, independent guard clauses over one lookup loop, not branching logic worth extracting.
	})
	public McpCompleter promptCompleter(String promptName, String argumentName) {
		if (promptName == null || argumentName == null)
			return null;
		for (var handler : getPrompts()) {
			if (handler == null)
				continue;
			var descriptor = handler.descriptor();
			if (descriptor == null || ! promptName.equals(descriptor.getName()))
				continue;
			var arguments = descriptor.getArguments();
			return arguments == null ? null : completerForArgument(arguments, argumentName);
		}
		return null;
	}

	/**
	 * Scans one prompt's declared arguments for the exact-named argument's completer, extracted from
	 * {@link #promptCompleter(String, String)} to keep that method's cognitive complexity within the
	 * project's guard-clause-density threshold.
	 */
	private static McpCompleter completerForArgument(List<McpPromptArgument> arguments, String argumentName) {
		for (var argument : arguments)
			if (argument != null && argumentName.equals(argument.getName()))
				return argument.getCompleter();
		return null;
	}

	/**
	 * Resolves the completer registered for one declared variable of a registered resource-template, by exact
	 * registered {@code uriTemplate} string and exact variable name.
	 *
	 * <p>
	 * Matching is against the exact registered template string, not a reverse-match against a concrete URI: a
	 * {@code completion/complete} reference names the declaration being completed, never a concrete/expanded
	 * URI. This is one half of the neutral cross-target completion lookup shared identically by both dated
	 * adapters; see {@link #promptCompleter(String, String)} for the prompt half and
	 * {@link #completer(McpCompletionRef, String)} for the unified entry point. Delegates the "variable must be
	 * declared by the template" check to {@link #resourceTemplateCompleter(McpResourceTemplateHandler, String)}
	 * rather than re-parsing.
	 *
	 * @param uriTemplate The exact registered {@code uriTemplate} to look up. Can be {@code null}, in which
	 * 	case this method returns {@code null}.
	 * @param variableName The exact variable name to look up. Can be {@code null}, in which case this method
	 * 	returns {@code null}.
	 * @return The variable's completer, or {@code null} if the template is unregistered, the variable is
	 * 	undeclared, or the handler declares no completer for it. Never invokes the completer.
	 */
	@SuppressWarnings({
		"java:S135" // Two sequential null/template-mismatch skips are simple, independent guard clauses over one lookup loop, not branching logic worth extracting.
	})
	public McpCompleter templateCompleter(String uriTemplate, String variableName) {
		if (uriTemplate == null || variableName == null)
			return null;
		var templates = getResourceTemplates();
		var matchers = compiledResourceTemplateMatchers;
		for (var handler : templates) {
			if (handler == null)
				continue;
			var descriptor = handler.descriptor();
			if (descriptor == null || ! uriTemplate.equals(descriptor.getUriTemplate()))
				continue;
			var matcher = matchers.get(handler);
			return matcher != null ? resourceTemplateCompleter(handler, variableName, matcher) : resourceTemplateCompleter(handler, variableName);
		}
		return null;
	}

	/**
	 * Unified neutral completion lookup: resolves the completer for a {@code completion/complete} target,
	 * dispatching by {@link McpCompletionRef.Kind} to {@link #promptCompleter(String, String)} or
	 * {@link #templateCompleter(String, String)}.
	 *
	 * <p>
	 * Both dated adapters call this same method for both target kinds, so template/prompt resolution policy is
	 * defined exactly once, in this neutral module.
	 *
	 * @param ref The completion reference identifying the exact prompt name or registered template string.
	 * 	Can be {@code null}, in which case this method returns {@code null}.
	 * @param argumentName The exact argument/variable name to look up. Can be {@code null}, in which case this
	 * 	method returns {@code null}.
	 * @return The resolved completer, or {@code null} if {@code ref} is {@code null}/has no {@link
	 * 	McpCompletionRef#getKind() kind}, or if the target-specific lookup finds none. Never invokes the
	 * 	completer.
	 */
	public McpCompleter completer(McpCompletionRef ref, String argumentName) {
		if (ref == null || ref.getKind() == null)
			return null;
		if (ref.getKind() == McpCompletionRef.Kind.PROMPT)
			return promptCompleter(ref.getTarget(), argumentName);
		return templateCompleter(ref.getTarget(), argumentName);
	}

	/**
	 * Reports whether {@code completions} capability should be advertised: whether at least one registered
	 * prompt argument or resource-template variable currently declares a non-{@code null} completer.
	 *
	 * <p>
	 * This never invokes a completer; it only detects registration, so both dated adapters can use it for
	 * auto-derived capability advertisement without side effects.
	 *
	 * @return {@code true} if at least one prompt-argument completer or resource-template variable completer
	 * 	is registered.
	 */
	@SuppressWarnings({
		"java:S135", // Two sequential null-skips per loop, across two independent scans (prompts then templates), are simple guard clauses, not branching logic worth extracting.
		"java:S3776" // Cognitive complexity from scanning two registries with guard clauses is inherent to this method's "any completer anywhere" contract; standing project policy is to suppress rather than refactor for this metric alone.
	})
	public boolean hasAnyCompleter() {
		for (var handler : getPrompts()) {
			if (handler == null)
				continue;
			var descriptor = handler.descriptor();
			var arguments = descriptor == null ? null : descriptor.getArguments();
			if (arguments == null)
				continue;
			for (var argument : arguments)
				if (argument != null && argument.getCompleter() != null)
					return true;
		}
		var templates = getResourceTemplates();
		var matchers = compiledResourceTemplateMatchers;
		for (var handler : templates) {
			if (handler == null)
				continue;
			var descriptor = handler.descriptor();
			var uriTemplate = descriptor == null ? null : descriptor.getUriTemplate();
			if (uriTemplate == null)
				continue;
			var matcher = matchers.get(handler);
			var variableNames = matcher != null ? matcher.variableNames() : McpUriTemplateMatcher.compile(uriTemplate).variableNames();
			for (var variableName : variableNames)
				if (handler.completer(variableName) != null)
					return true;
		}
		return false;
	}

	/**
	 * Pagination strategy for {@code list} dispatchers (tools / prompts / resources / resource templates).
	 *
	 * @return The cursor. Never {@code null} (defaults to {@link McpCursor#SINGLE_PAGE}).
	 */
	public McpCursor getCursor() {
		return cursor;
	}

	/**
	 * Sets the pagination strategy.
	 *
	 * @param cursor The new value (or {@code null} to reset to {@link McpCursor#SINGLE_PAGE}).
	 * @return This object (for method chaining).
	 */
	public McpServerConfig setCursor(McpCursor cursor) {
		this.cursor = cursor == null ? McpCursor.SINGLE_PAGE : cursor;
		return this;
	}
}
