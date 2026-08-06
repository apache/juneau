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
package org.apache.juneau.examples.mcp;

import java.util.*;

import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.sse.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.mcp.*;
import org.apache.juneau.rest.server.mcp.v20260728.*;

/**
 * A complete, runnable MCP <c>2026-07-28</c> server exposing every major surface of the protocol
 * over one tiny, coherent scenario: an in-memory {@link NoteStore notes} service.
 *
 * <p>
 * This is the reference implementation to copy. Everything the server exposes is registered in
 * {@link #createMcpConfig()} below, and every handler is a small anonymous class that closes over a
 * single shared {@link NoteStore}. What each surface demonstrates:
 *
 * <ul>
 * 	<li><b>tool</b> &mdash; {@code publishNote(title, body)} stores a note and then <i>pushes</i> a
 * 		{@code resources/updated} change to any subscribers (see the subscription bullet).
 * 	<li><b>tool + elicitation / MRTR</b> &mdash; {@code deleteNote(title)} returns an
 * 		{@code input_required} pause asking the caller to confirm, then resumes to actually delete.
 * 		This is one of the framework's two headline features.
 * 	<li><b>prompt + completion</b> &mdash; {@code summarize} renders a prompt message from a stored
 * 		note; its {@code title} argument offers {@code completion/complete} suggestions from existing
 * 		note titles.
 * 	<li><b>resource</b> &mdash; the fixed {@code note:///index} resource lists all note titles.
 * 	<li><b>resource template + completion</b> &mdash; {@code note:///{title}} reads one note by title;
 * 		its {@code title} variable also offers completions.
 * 	<li><b>subscription</b> &mdash; {@code subscriptions/listen} (SEP-2575 held-open SSE) delivers the
 * 		{@code resources/updated} change {@code publishNote} fires. The other headline feature.
 * </ul>
 *
 * <p>
 * The two overridable factory hooks are the whole story:
 * <ul>
 * 	<li>{@link #createMcpConfig()} &mdash; <i>what</i> the server exposes (tools/prompts/resources).
 * 	<li>{@link #createMcpOptions()} &mdash; <i>how</i> it behaves (advertised capabilities, etc.).
 * </ul>
 *
 * <p>
 * The {@link Rest @Rest} annotation must register {@link SseSerializer} (alongside the JSON
 * serializer) so a real servlet container can satisfy the {@code Accept: text/event-stream}
 * negotiation the subscription stream requires; the base {@link McpRestServlet} wires only JSON.
 *
 * @serial exclude
 */
@Rest(serializers = {JsonSerializer.class, SseSerializer.class}, parsers = JsonParser.class, defaultAccept = "application/json")
public class ExampleMcpServer extends McpRestServlet {

	private static final long serialVersionUID = 1L;

	// The entire domain state. Handlers below close over this instance, so there is no DI ceremony to
	// follow: the server object owns its notes, and every MCP surface is a thin view over them.
	private final transient NoteStore notes = new NoteStore();

	/**
	 * Declares everything this server exposes. This one method is the "table of contents" for the demo.
	 */
	@Override
	protected McpServerConfig createMcpConfig() {
		return new McpServerConfig()
			.setName("juneau-notes-example")
			.setVersion("1.0.0")
			.addTool(publishNoteTool(notes))
			.addTool(deleteNoteTool(notes))
			.addPrompt(summarizePrompt(notes))
			.addResource(noteIndexResource(notes))
			.addResourceTemplate(noteTemplate(notes));
	}

	/**
	 * Advertises capabilities and instructions. Explicit here so the {@code server/discover} handshake
	 * truthfully reports everything this server supports &mdash; notably {@code resources.subscribe},
	 * which pairs with the {@code subscriptions/listen} demo.
	 *
	 * <p>
	 * On revision {@code 2026-07-28}, {@code server/discover}'s {@code instructions} field is sourced from
	 * this {@link McpOptions}, <b>not</b> {@link McpServerConfig#setInstructions}, which only feeds the
	 * legacy v1 {@code initialize} handshake.
	 */
	@Override
	protected McpOptions createMcpOptions() {
		return new McpOptions()
			.setInstructions("A tiny notes service. Use 'publishNote' to add notes, read them via the "
				+ "'note:///{title}' resource template, subscribe for change notifications, and try "
				+ "'deleteNote' to see an elicitation (confirm) round-trip.")
			.setCapabilities(new ServerCapabilities()
				.setTools(new ToolCapability().setListChanged(true))
				.setPrompts(new PromptCapability().setListChanged(true))
				.setResources(new ResourceCapability().setSubscribe(true).setListChanged(true))
				.setCompletions(new CompletionCapability()));
	}

	// =================================================================================================
	// Tools
	// =================================================================================================

	/**
	 * A plain tool that mutates server state and then notifies subscribers.
	 *
	 * <p>
	 * The {@code resourceUpdated(...)} call is the server-push half of the subscription feature: any client
	 * currently {@code listen}ing for changes to this note's URI receives an {@code onResourceUpdated} frame.
	 * The {@link McpSubscriptions} SPI is resolved from the per-request {@link BeanStore}.
	 */
	private static McpToolHandler publishNoteTool(NoteStore notes) {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() {
				return new McpToolSpec().setName("publishNote")
					.setDescription("Stores a note under the given title and notifies subscribers of the change.");
			}
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) {
				var title = String.valueOf(arguments.getOrDefault("title", ""));
				var body = String.valueOf(arguments.getOrDefault("body", ""));
				notes.put(title, body);
				ctx.getBean(McpSubscriptions.class).ifPresent(s -> s.resourceUpdated(NoteStore.uriFor(title)));
				return McpToolOutcome.text("Stored note '" + title + "' (" + body.length() + " chars).");
			}
		};
	}

	/**
	 * A tool that pauses for confirmation before acting &mdash; the elicitation / Multi-Round-Trip-Request
	 * (MRTR, SEP-2322) flow.
	 *
	 * <p>
	 * On the first call there is no resume context, so the handler <i>throws</i> an {@code input_required}
	 * signal built by {@link ElicitationRequests#of(String, ElicitRequest, Object)}: it carries one question
	 * (id {@code "confirmDelete"}, asking for a boolean {@code confirm} field) plus a continuation token
	 * ({@code "delete:<title>"}) that the framework seals and echoes back. The id and the field name are
	 * deliberately distinct (rather than both {@code "confirm"}) so the two-level {@code (id, field)} lookup
	 * below is legible. When the client answers, the framework re-invokes this same handler with a populated
	 * {@link McpMrtrResumeContext}; the handler reads the answer and the continuation and finishes.
	 *
	 * <p>
	 * Note the title is recovered from the continuation on resume rather than from the arguments: carrying
	 * per-pause state through the continuation is the intended MRTR pattern.
	 *
	 * <p>
	 * The framework explicitly allows a resume with zero elicitation answers (a client that never asked the
	 * user, or a transport that dropped the answer), so the continuation and the {@code confirm} answer are
	 * both treated as possibly missing below &mdash; never dereferenced directly. {@link McpMrtrResumeContext#continuationAsString()}
	 * and {@link ElicitationResponses#getBoolean} are the decline/omission-safe accessors that make that
	 * possible without hand-rolled null checks.
	 */
	private static McpToolHandler deleteNoteTool(NoteStore notes) {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() {
				return new McpToolSpec().setName("deleteNote")
					.setDescription("Deletes a note, after asking the caller to confirm (demonstrates elicitation).");
			}
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) {
				var resume = ctx.getBean(McpMrtrResumeContext.class);
				if (resume.isEmpty()) {
					// PAUSE: ask the caller to confirm. Execution stops here and control returns to the client.
					var title = String.valueOf(arguments.getOrDefault("title", ""));
					var question = new ElicitRequest()
						.setMessage("Really delete note '" + title + "'?")
						.setRequestedSchema(ElicitSchema.create().booleanField("confirm").title("Confirm deletion").build());
					throw ElicitationRequests.of("confirmDelete", question, "delete:" + title);
				}
				// RESUME: the caller answered (or didn't). Recover state from the continuation; a missing
				// continuation or a missing/declined "confirm" answer both cleanly mean "cancelled".
				var rc = resume.get();
				var continuation = rc.continuationAsString();
				var title = continuation != null && continuation.startsWith("delete:")
					? continuation.substring("delete:".length()) : "";
				if (ElicitationResponses.getBoolean(rc, "confirmDelete", "confirm")) {
					var removed = notes.remove(title);
					return McpToolOutcome.text(removed
						? "Deleted note '" + title + "'."
						: "There was no note titled '" + title + "'.");
				}
				return McpToolOutcome.text("Deletion of '" + title + "' was cancelled.");
			}
		};
	}

	// =================================================================================================
	// Prompt
	// =================================================================================================

	/**
	 * A prompt whose {@code title} argument declares a completer, so a client's {@code completion/complete}
	 * request for that argument is answered with matching existing note titles.
	 */
	private static McpPromptHandler summarizePrompt(NoteStore notes) {
		var spec = new McpPromptSpec()
			.setName("summarize")
			.setDescription("Builds a prompt asking a model to summarize a stored note.")
			.setArguments(List.of(new McpPromptArgument()
				.setName("title")
				.setDescription("The title of the note to summarize.")
				.setRequired(true)
				.setCompleter(noteTitleCompleter(notes))));
		return new McpPromptHandler() {
			@Override public McpPromptSpec descriptor() { return spec; }
			@Override public McpPromptOutcome get(Map<String,Object> arguments, BeanStore ctx) {
				var title = String.valueOf(arguments.getOrDefault("title", ""));
				var body = notes.get(title);
				var text = body == null
					? "There is no note titled '" + title + "'."
					: "Summarize the following note titled '" + title + "':\n\n" + body;
				var message = new McpPromptMessage().setRole(McpRole.USER).setContent(McpContentBlock.text(text));
				return new McpPromptOutcome()
					.setDescription("Summary prompt for note '" + title + "'.")
					.setMessages(List.of(message));
			}
		};
	}

	// =================================================================================================
	// Resources
	// =================================================================================================

	/**
	 * A fixed (non-templated) resource that lists all current note titles.
	 */
	private static McpResourceHandler noteIndexResource(NoteStore notes) {
		var spec = new McpResourceSpec()
			.setUri(NoteStore.SCHEME + "index")
			.setName("note-index")
			.setDescription("A plain-text list of all note titles.")
			.setMimeType("text/plain");
		return new McpResourceHandler() {
			@Override public McpResourceSpec descriptor() { return spec; }
			@Override public McpResourceOutcome read(String uri, BeanStore ctx) {
				var titles = notes.titles();
				var text = titles.isEmpty() ? "(no notes yet)" : String.join("\n", titles);
				return new McpResourceOutcome().setContents(List.of(McpResourceContents.text(uri, "text/plain", text)));
			}
		};
	}

	/**
	 * A resource template ({@code note:///{title}}) that reads one note by title, and offers completions
	 * for its {@code title} variable.
	 *
	 * <p>
	 * An exact resource registration (here, {@code note:///index}) always beats a matching template, so
	 * {@code note:///index} reads the index above while {@code note:///groceries} routes here.
	 */
	private static McpResourceTemplateHandler noteTemplate(NoteStore notes) {
		var spec = new McpResourceTemplateSpec()
			.setUriTemplate(NoteStore.SCHEME + "{title}")
			.setName("note")
			.setDescription("Reads a single note by title.")
			.setMimeType("text/plain");
		var completer = noteTitleCompleter(notes);
		return new McpResourceTemplateHandler() {
			@Override public McpResourceTemplateSpec descriptor() { return spec; }
			@Override public McpResourceOutcome read(String uri, Map<String,String> variables, BeanStore ctx) {
				var title = variables.get("title");
				var body = notes.get(title);
				var text = body == null ? "(no note titled '" + title + "')" : body;
				return new McpResourceOutcome().setContents(List.of(McpResourceContents.text(uri, "text/plain", text)));
			}
			@Override public McpCompleter completer(String variableName) {
				return "title".equals(variableName) ? completer : null;
			}
		};
	}

	// =================================================================================================
	// Shared completion logic
	// =================================================================================================

	/**
	 * A completer that suggests existing note titles beginning with the caller's partial input. Used by both
	 * the prompt argument and the resource-template variable, so one implementation drives every
	 * {@code completion/complete} response in this server.
	 */
	private static McpCompleter noteTitleCompleter(NoteStore notes) {
		return (request, ctx) -> {
			var prefix = request.getValue();
			var matches = notes.titles().stream().filter(t -> t.startsWith(prefix)).toList();
			return new McpCompletionResult().setValues(matches);
		};
	}
}
