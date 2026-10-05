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
 * The capture-only console slot directives: {@code <@head>}, {@code <@scripts>}, {@code <@brand>},
 * {@code <@actions>}, {@code <@title>}, {@code <@footer>}, and {@code <@body>}. One parameterized instance is
 * registered per slot name (its {@link PageCapture} field), so the seven directives share this one class.
 *
 * <p>
 * Each is authored inside {@code <@console>}. During the console's body pass this directive <b>captures</b> its
 * rendered body into the matching {@link PageCapture} field and emits <b>nothing</b> at the author's position;
 * {@code <@console>} then places each captured slot into the fixed document at its correct spot (chrome-level
 * {@code <@head>}/{@code <@scripts>} in the head/end-of-body pass-throughs; {@code <@brand>}/{@code <@actions>}/
 * {@code <@title>} in the header; {@code <@footer>} as the single footer; {@code <@body>} spliced onto the
 * {@code <body>} tag). A slot authored outside a {@code <@console>} (no {@link PageCapture} in scope) is
 * rejected fail-closed.
 *
 * <h5 class='section'>{@code phase=} (head/scripts only):</h5>
 * <p>
 * {@code <@head>} and {@code <@scripts>} accept an optional {@code phase=} that steers the capture to a distinct
 * cascade position: {@code <@head phase="before-page-css">} lands app CSS <b>before</b> the page-local
 * {@code <@page css=>} links (e.g. {@code a.css}), so a page {@code css=} rule still wins, and
 * {@code <@scripts phase="after-toolkit">} lands scripts <b>between</b> the toolkit JS and the page-local
 * {@code <@page init=>} scripts (e.g. {@code one.js}). Any other slot rejects {@code phase=}; an
 * unrecognized value is rejected fail-closed.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Duplicated literals read more clearly inline than as constants
})
public final class ConsoleSlotDirectiveModel implements TemplateDirectiveModel {

	/** Slot names; each is also the shared-variable name the directive registers under. */
	static final String HEAD = "head";
	static final String SCRIPTS = "scripts";
	static final String BRAND = "brand";
	static final String ACTIONS = "actions";
	static final String TITLE = "title";
	static final String FOOTER = "footer";
	static final String BODY = "body";

	/** Every slot name, in registration order. */
	static final List<String> SLOT_NAMES = List.of(HEAD, SCRIPTS, BRAND, ACTIONS, TITLE, FOOTER, BODY);

	/** {@code <@head phase>} value that lands the markup before the page-local {@code css=} links. */
	static final String PHASE_BEFORE_PAGE_CSS = "before-page-css";

	/** {@code <@scripts phase>} value that lands the markup between the toolkit JS and {@code init=}. */
	static final String PHASE_AFTER_TOOLKIT = "after-toolkit";

	private static final String PHASE = "phase";
	private static final String TEXT = "text";

	private final String slot;

	/**
	 * @param slot One of the slot-name constants; also the shared-variable name this directive is registered under.
	 */
	ConsoleSlotDirectiveModel(String slot) {
		this.slot = slot;
	}

	/** @return The attributes this slot accepts (read by {@link ConsoleTemplateValidator}). */
	Set<String> attrs() {
		if (HEAD.equals(slot) || SCRIPTS.equals(slot))
			return Set.of(PHASE);
		if (FOOTER.equals(slot))
			return Set.of(TEXT);
		return Set.of();
	}

	@Override
	@SuppressWarnings({
		"java:S3776", // Validation and slot dispatch read best as one straight-line method.
		"unchecked" // FreeMarker's raw params Map is String-keyed by contract.
	})
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		Map<String, TemplateModel> p = params;
		if (p.containsKey("format"))
			throw FtlAttrLists.reject("<@" + slot + "> has no format= attribute.");
		FtlAttrLists.rejectUnknown(p, slot, attrs());

		var phase = FtlAttrLists.scalar(p, PHASE);
		if (! phase.isEmpty()) {
			var ok = (HEAD.equals(slot) && eq(phase, PHASE_BEFORE_PAGE_CSS))
				|| (SCRIPTS.equals(slot) && eq(phase, PHASE_AFTER_TOOLKIT));
			if (! ok)
				throw FtlAttrLists.reject("<@" + slot + "> unknown phase '" + phase + "'.");
		}

		var cap = PageCapture.get(env);
		if (cap == null || ! cap.consoleOpen)
			throw FtlAttrLists.reject(String.format("<@%s> must be nested inside <@console>.", slot));

		var markup = PageCapture.render(body);
		switch (slot) {
			case HEAD -> {
				if (phase.isEmpty())
					cap.head = markup;
				else
					cap.headBeforePageCss = markup;
			}
			case SCRIPTS -> {
				if (phase.isEmpty())
					cap.scripts = markup;
				else
					cap.scriptsAfterToolkit = markup;
			}
			case BODY -> cap.bodyAttrs = markup;
			case BRAND -> cap.addSlot("header", "brand", markup);
			case ACTIONS -> cap.addSlot("header", "actions", markup);
			case TITLE -> cap.addSlot("header", "replace", markup);
			case FOOTER -> footer(cap, FtlAttrLists.scalar(p, TEXT), markup);
			default -> throw new IllegalStateException(slot);
		}
	}

	private static void footer(PageCapture cap, String text, String markup) throws TemplateModelException {
		if (! text.isEmpty()) {
			if (! markup.isBlank())
				throw FtlAttrLists.reject(String.format(
					"<@footer text='%s'> also has a body; use text= for plain text or the body for markup, not both.", text));
			cap.footer(f -> f.text(text));
		} else if (! markup.isBlank()) {
			cap.addSlot("footer", "content", markup);
		}
	}
}
