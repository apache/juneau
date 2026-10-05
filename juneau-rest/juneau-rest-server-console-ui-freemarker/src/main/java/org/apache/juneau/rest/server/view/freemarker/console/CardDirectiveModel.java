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

import freemarker.core.*;
import freemarker.template.*;

/**
 * The {@code <@card>} FreeMarker directive: adds one entry to the page contract's {@code cards[]}.
 *
 * <p>
 * {@code type="html"} (the default) takes exactly one content source: a body (emitted as
 * {@code <template data-card="{id}">}), {@code template=} (reuses another card's template), or {@code src=} (fetched
 * same-origin by the shell). {@code type="datatables"} takes a table URL or a JSON5 catalog body and becomes the C1
 * bridge card. A card without {@code id=} gets {@code jc-card-N}. {@code <@card>} is only valid inside
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

	static final Set<String> ATTRS = Set.of("type", "id", "title", "src", "template");

	private static final Set<String> REMOVED_TYPES = Set.of("js", "json", "calendar");

	CardDirectiveModel() {}

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
		if (REMOVED_TYPES.contains(type))
			throw FtlAttrLists.reject(String.format(
				"<@card id='%s'> type='%s' was removed in 10.0.0; use type='html' with a <template>, or a registered card type.",
				authoredId, type));
		if (! (type.isEmpty() || eqa(type, "html", "datatables")))
			throw FtlAttrLists.reject(String.format("<@card> type= must be one of html|datatables; got '%s'.", type));

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
		if (eq(type, "datatables"))
			datatablesCard(cap, authoredId, title, src, template, markup);
		else
			htmlCard(cap, authoredId.isEmpty() ? cap.nextCardId() : authoredId, title, src, template, markup);
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
	 * The C1 datatables bridge (spec §4.6): a bare string body is a table URL; a JSON-object body is an author catalog
	 * lifted by {@link CardEnvelope#liftTable}. The shell mounts it through {@code JuneauViews.regions.mount}.
	 */
	private static void datatablesCard(PageCapture cap, String id, String title, String src, String template,
			String markup) throws TemplateModelException {
		if (id.isEmpty())
			throw FtlAttrLists.reject("<@card type=\"datatables\"> requires id=.");
		if (! (src.isEmpty() && template.isEmpty()))
			throw FtlAttrLists.reject(String.format(
				"<@card id='%s'> type='datatables' takes its table URL or catalog as the body; src= and template= are not allowed.", id));
		var trimmed = markup.trim();
		if (trimmed.isEmpty())
			throw FtlAttrLists.reject("<@card type=\"datatables\"> requires a table URL or catalog as its body.");
		var card = CardSpec.of("datatables", id);
		if (! title.isEmpty())
			card.title(title);
		if (trimmed.startsWith("{")) {
			var catalog = CardEnvelope.parse(trimmed);
			card.body("table", CardEnvelope.liftTable(id, catalog));
			var page = catalog.get("page");
			if (page != null)
				card.body("page", page);
		} else {
			card.body("table", trimmed);
		}
		cap.addCard(card, null);
	}
}
