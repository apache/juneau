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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.rest.server.console.*;
import org.apache.juneau.rest.server.views.*;

import freemarker.core.*;
import freemarker.template.*;

/**
 * The {@code <@card>} FreeMarker directive: adds one entry to the page contract's {@code cards[]}.
 *
 * <p>
 * {@code html} (the default) and {@code run-view} are built in. {@code datatables}, {@code console-output} and every other
 * well-formed type name is dispatched through the host's {@link CardTypeRegistry}: a registered
 * {@link CardTypeHandler} validates it, and an unregistered type falls back to the generic passthrough. Every type other
 * than {@code html} requires {@code id=}.
 *
 * <p>
 * {@code type="html"} (the default) takes exactly one content source: a body (emitted as
 * {@code <template data-card="{id}">}), {@code template=} (reuses another card's template), or {@code src=} (fetched
 * same-origin by the shell). {@code type="datatables"} takes {@code src=} or a JSON5 catalog body and becomes the C1
 * bridge card. {@code type="console-output"} takes a JSON5 {@code {contractVersion: '1', output: {...}}} body and
 * becomes the console-output bridge card (see {@link ConsoleOutputDef}). {@code type="run-view"} takes a JSON5
 * {@code {contractVersion: '1', runView: {...}}} body and becomes the run-view bridge card (see {@link RunViewDef}). A card without {@code id=} gets {@code jc-card-N}. {@code <@card>} is only valid inside
 * {@code <@page>}; markup around it becomes {@code jc-seg-N} segments, so the authored order is kept.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	&lt;@page tab="releases"&gt;
 * 	  &lt;@card id="intro" title="Releases"&gt;&lt;p&gt;All releases.&lt;/p&gt;&lt;/@card&gt;
 * 	  &lt;@card type="datatables" id="releases"&gt;
 * 	    { dataUrl: '/rest/releases/data', columns: [ { key: 'name', label: 'Name' } ] }
 * 	  &lt;/@card&gt;
 * 	&lt;/@page&gt;
 * </p>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Duplicated literals read more clearly inline than as constants
})
public final class CardDirectiveModel implements TemplateDirectiveModel {

	/** The shared-variable name this directive registers under. */
	public static final String NAME = "card";

	static final Set<String> ATTRS = Set.of("type", "id", "title", "src", "template", "requires");

	private static final Set<String> REMOVED_TYPES = Set.of("js", "json", "calendar");

	private static final Pattern TYPE_RE = Pattern.compile("^[a-z][a-z0-9-]{0,31}$");

	/** Contract base keys copied onto {@link CardSpec}'s own setters rather than {@link CardSpec#body}. */
	private static final Set<String> BASE_KEYS = Set.of("id", "type", "title", "src", "template");

	private final boolean devMode;
	private final CardRequirements requirements;
	private final CardTypeRegistry cardTypes;

	CardDirectiveModel(boolean devMode, CardRequirements requirements, CardTypeRegistry cardTypes) {
		this.devMode = devMode;
		this.requirements = requirements;
		this.cardTypes = cardTypes;
	}

	@Override
	@SuppressWarnings({
		"unchecked" // FreeMarker's raw params Map is String-keyed by contract.
	})
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		Map<String, TemplateModel> p = params;
		if (p.containsKey("format"))
			throw FtlAttrLists.reject("<@card> uses type= only; format= is not a valid attribute.");
		FtlAttrLists.rejectUnknown(p, NAME, ATTRS);

		var type = FtlAttrLists.scalar(p, "type");
		var authoredId = FtlAttrLists.scalar(p, "id");
		var requires = FtlAttrLists.list(p, NAME, "requires");
		if (REMOVED_TYPES.contains(type))
			throw FtlAttrLists.reject(String.format(
				"<@card id='%s'> type='%s' was removed in 10.0.0; use type='html' with a <template>, or a registered card type.",
				authoredId, type));
		if (! type.isEmpty() && ! TYPE_RE.matcher(type).matches())
			throw FtlAttrLists.reject(String.format("<@card id='%s'> type='%s' must match ^[a-z][a-z0-9-]{0,31}$.", authoredId, type));

		var cap = PageCapture.get(env);
		if (cap == null || ! cap.pageOpen)
			throw FtlAttrLists.reject("<@card> must be nested inside <@page>.");
		if (cap.cardOpen)
			throw FtlAttrLists.reject("<@card> cannot be nested inside another <@card>.");
		if (! authoredId.isEmpty())
			FtlAttrLists.checkId(NAME, authoredId);

		// Markup before this card becomes its own segment, so it stays ahead of the card.
		cap.flushSegment();

		String markup;
		cap.cardOpen = true;
		try {
			markup = PageCapture.render(body);
		} finally {
			cap.cardOpen = false;
		}

		var title = FtlAttrLists.scalar(p, "title");
		var src = FtlAttrLists.scalar(p, "src");
		var template = FtlAttrLists.scalar(p, "template");
		var cardType = type.isEmpty() ? "html" : type;
		var id = authoredId.isEmpty() && eq(cardType, "html") ? cap.nextCardId() : authoredId;
		if (eq(cardType, "run-view"))
			runViewCard(cap, id, title, src, template, markup);
		else if (eq(cardType, "html"))
			htmlCard(cap, id, title, src, template, markup);
		else
			registryCard(cap, cardType, id, title, src, template, markup, env.getCurrentTemplate().getName());
		recordRequirements(cap, cardType, id, requires);
	}

	// The one place a card's packs are recorded; <@page> resolves them after its body.
	private void recordRequirements(PageCapture cap, String type, String id, List<String> requires) throws TemplateModelException {
		try {
			cap.require(requirements.forCard(type, id, requires));
		} catch (IllegalArgumentException e) {
			throw FtlAttrLists.reject(e.getMessage());
		}
	}

	private static void htmlCard(PageCapture cap, String id, String title, String src, String template, String markup)
			throws TemplateModelException {
		var hasBody = ! markup.isBlank();
		var sources = (hasBody ? 1 : 0) + (src.isEmpty() ? 0 : 1) + (template.isEmpty() ? 0 : 1);
		if (sources == 0)
			throw FtlAttrLists.reject(String.format("<@card id='%s'> type='html' requires a body or src=.", id));
		if (sources > 1)
			throw FtlAttrLists.reject(String.format(
				"<@card id='%s'> type='html' takes exactly one of a body, template= or src=.", id));
		var card = CardSpec.html(id);
		if (! title.isEmpty())
			card.title(title);
		if (! src.isEmpty())
			card.src(src);
		if (! template.isEmpty())
			card.template(template);
		cap.addCard(card, hasBody ? markup : null);
	}

	/**
	 * Every type other than {@code html} and {@code run-view}: validated and lifted by the host's
	 * {@link CardTypeRegistry} (a registered handler such as {@code datatables} or {@code console-output}, or the
	 * generic passthrough for an unregistered type).
	 */
	private void registryCard(PageCapture cap, String type, String id, String title, String src, String template,
			String markup, String ftlName) throws TemplateModelException {
		if (id.isEmpty())
			throw FtlAttrLists.reject(String.format("<@card type=\"%s\"> requires id=.", type));
		var hasBody = ! markup.isBlank();
		if (eq(type, "console-output")) {
			if (! (src.isEmpty() && template.isEmpty()))
				throw FtlAttrLists.reject(String.format(
					"<@card id='%s'> type='console-output' takes its options as the body; src= and template= are not allowed.", id));
		} else if (eq(type, "datatables")) {
			if (! template.isEmpty())
				throw FtlAttrLists.reject(String.format(
					"<@card id='%s'> type='datatables' takes src= or a JSON5 body; template= is not allowed.", id));
		} else if ((hasBody ? 1 : 0) + (src.isEmpty() ? 0 : 1) + (template.isEmpty() ? 0 : 1) > 1) {
			throw FtlAttrLists.reject(String.format(
				"<@card id='%s'> type='%s' takes at most one of a body, template= or src=.", id, type));
		}
		var captured = new boolean[1];
		var source = CardSource.create(type, id).templateSink((cid, m) -> captured[0] = true);
		if (! title.isEmpty())
			source.title(title);
		if (! src.isEmpty())
			source.src(src);
		if (! template.isEmpty())
			source.template(template);
		if (hasBody)
			source.body(markup);
		JsonMap card;
		try {
			card = cardTypes.toCard(source.build());
			if (eq(type, "datatables") && card.get("table") instanceof JsonMap t && t.get("view") instanceof Map)
				DatatablesCardType.reconcileViewVersion(id, t, ftlName, devMode);
		} catch (IllegalArgumentException e) {
			throw FtlAttrLists.reject(e.getMessage());
		}
		cap.addCard(toCardSpec(card), captured[0] ? markup : null);
	}

	/** Converts the flat {@link CardTypeRegistry#toCard} result into the {@link CardSpec} {@link PageCapture} wants. */
	private static CardSpec toCardSpec(JsonMap card) {
		var spec = CardSpec.of((String)card.get("type"), (String)card.get("id"));
		if (card.get("title") instanceof String v)
			spec.title(v);
		if (card.get("src") instanceof String v)
			spec.src(v);
		if (card.get("template") instanceof String v)
			spec.template(v);
		for (var e : card.entrySet())
			if (! BASE_KEYS.contains(e.getKey()))
				spec.body(e.getKey(), e.getValue());
		return spec;
	}

	private static final Set<String> RUN_VIEW_BODY_KEYS = new LinkedHashSet<>(List.of("contractVersion", "runView"));

	/**
	 * The run-view bridge: a JSON5 {@code {contractVersion: '1', runView: {...}}} body whose {@code runView} keys
	 * are exactly {@link RunViewDef#KEYS}, validated through {@link RunViewDef} so an unsafe URL fails the
	 * render. The shell mounts it through {@code JuneauViews.runView.mount}.
	 */
	@SuppressWarnings({
		"unchecked" // JSON5 object keys are always strings.
	})
	private static void runViewCard(PageCapture cap, String id, String title, String src, String template,
			String markup) throws TemplateModelException {
		if (id.isEmpty())
			throw FtlAttrLists.reject("<@card type=\"run-view\"> requires id=.");
		if (! (src.isEmpty() && template.isEmpty()))
			throw FtlAttrLists.reject(String.format(
				"<@card id='%s'> type='run-view' takes its options as the body; src= and template= are not allowed.", id));
		var trimmed = markup.trim();
		if (! trimmed.startsWith("{"))
			throw FtlAttrLists.reject(String.format(
				"<@card id='%s'> type='run-view' requires a JSON5 body { contractVersion: '1', runView: {...} }.", id));
		JsonMap envelope;
		try {
			envelope = Json5Parser.DEFAULT.read(trimmed, JsonMap.class);
		} catch (org.apache.juneau.marshall.parser.ParseException ex) {
			throw FtlAttrLists.reject("Card JSON5 is invalid: " + ex.getMessage());
		}
		for (var k : envelope.keySet())
			if (! RUN_VIEW_BODY_KEYS.contains(k))
				throw FtlAttrLists.reject(String.format(
					"<@card id='%s'> type='run-view' unknown key '%s'; allowed: contractVersion, runView.", id, k));
		var version = envelope.get("contractVersion");
		if (! "1".equals(version))
			throw FtlAttrLists.reject(String.format(
				"<@card id='%s'> type='run-view' requires contractVersion: '1'; got '%s'.", id, version));
		if (! (envelope.get("runView") instanceof Map<?,?> runView))
			throw FtlAttrLists.reject(String.format("<@card id='%s'> type='run-view' requires a runView object.", id));
		var def = RunViewDef.fromMap(id, (Map<String,?>)runView).validate();
		var card = CardSpec.of("run-view", id);
		if (! title.isEmpty())
			card.title(title);
		card.body("runView", def.toMap());
		cap.addCard(card, null);
	}
}
