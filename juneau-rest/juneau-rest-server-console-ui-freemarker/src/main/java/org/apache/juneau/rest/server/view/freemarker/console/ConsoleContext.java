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

/**
 * Per-render document-shell context shared between {@code <@console>} and its nested slot directives
 * ({@code <@head>}, {@code <@scripts>}, {@code <@brand>}, {@code <@actions>}, {@code <@title>},
 * {@code <@footer>}, {@code <@body>}) and {@code <@theme>}, via
 * {@link freemarker.core.Environment#setCustomState(Object, Object)}.
 *
 * <p>
 * {@code <@console>} installs a fresh instance before rendering its body into a throwaway buffer. During that
 * pass each slot directive <b>captures</b> its rendered body into the matching field here and emits nothing,
 * {@code <@theme>} captures the resolved stock-pack URL plus any FTL-constructed override block, and the
 * positional {@code <@navigation>} / {@code <@main>} directives write their markup into the buffer in author
 * order. {@code <@console>} then emits the full document from the fixed head cascade plus these captured slots.
 *
 * <p>
 * The {@code <@head>} and {@code <@scripts>} slots each accept an optional {@code phase=} that steers the capture
 * into a distinct cascade position: {@code <@head phase="before-page-css">} lands app CSS <b>before</b> the page-local
 * {@code pageCss} (so a page {@code css=} rule still wins), and {@code <@scripts phase="after-toolkit">} lands scripts
 * <b>between</b> the toolkit JS and {@code pageInit} (an ordering some consoles require). Without {@code phase=} the
 * two slots keep their trailing positions (head end / body end).
 *
 * @since 10.0.0
 */
final class ConsoleContext {

	/** Identity key for {@code Environment} custom-state storage. */
	static final Object KEY = new Object();

	/** Slot names, each also the shared-variable name the capturing directive registers under. */
	static final String HEAD = "head";
	static final String SCRIPTS = "scripts";
	static final String BRAND = "brand";
	static final String ACTIONS = "actions";
	static final String TITLE = "title";
	static final String FOOTER = "footer";
	static final String BODY = "body";
	static final String MAIN = "main";

	/** {@code <@head phase>} value that lands the captured markup before the page-local {@code pageCss}. */
	static final String PHASE_BEFORE_PAGE_CSS = "before-page-css";

	/** {@code <@scripts phase>} value that lands the captured markup between the toolkit JS and {@code pageInit}. */
	static final String PHASE_AFTER_TOOLKIT = "after-toolkit";

	/**
	 * Sentinel {@code <@main/>} writes into the {@code <@console>} body buffer at its author position, so
	 * {@code <@console>} can split the buffer there: everything before is the pre-main chrome region (nav + any
	 * author markup above {@code <@main/>}) that an opt-in {@code .jc-chrome} sticky wrapper encloses; everything
	 * after is emitted below the {@code <main>}. An inert HTML-comment token keeps it from colliding with authored
	 * markup. Never reaches the wire &mdash; {@code <@console>} replaces it with {@link #mainMarkup}.
	 */
	static final String MAIN_PLACEHOLDER = "<!--jc-main-slot-->";

	/** Chrome-level {@code <@head>} pass-through, emitted after the fixed head cascade. Null if the slot was not authored. */
	String head;

	/** Chrome-level {@code <@head phase="before-page-css">} pass-through, emitted after the theme pack but before {@code pageCss}. */
	String headBeforePageCss;

	/** Chrome-level {@code <@scripts>} pass-through, emitted at end of body after toolkit JS / {@code pageInit}. */
	String scripts;

	/** Chrome-level {@code <@scripts phase="after-toolkit">} pass-through, emitted after the toolkit JS but before {@code pageInit}. */
	String scriptsAfterToolkit;

	/** {@code <@brand>} region markup (replaces only the default {@code icon=}/{@code brand=} brand region). */
	String brand;

	/** {@code <@actions>} region markup (header actions: help/avatar/similar). */
	String actions;

	/** {@code <@title>} raw header markup - when present, replaces the whole default header. */
	String title;

	/** {@code <@footer>} region markup - the only footer. */
	String footer;

	/** {@code <@body>} captured raw attribute text spliced onto the emitted {@code <body>} tag (e.g. a runtime data-attribute switch). */
	String bodyAttrs;

	/** Resolved stock theme-pack URL, set by a nested {@code <@theme>} element (wins over the {@code theme=} attribute). */
	String themePackUrl;

	/** FTL-constructed {@code ThemePack} override block (leaves escaped + aliases verbatim), or null if {@code <@theme>} declared no tokens. */
	String themeOverrideBlock;

	/** {@code <main class="jc-main">…</main>} markup built by {@code <@main/>}; substituted for {@link #MAIN_PLACEHOLDER}. */
	String mainMarkup;

	ConsoleContext() {}

	/**
	 * Stores a captured slot body under its slot name and (for {@code head}/{@code scripts}) its {@code phase=};
	 * the slot names are the {@code <@console>} sub-directive names.
	 */
	void setSlot(String slot, String phase, String value) {
		switch (slot) {
			case HEAD -> { if (PHASE_BEFORE_PAGE_CSS.equals(phase)) headBeforePageCss = value; else head = value; }
			case SCRIPTS -> { if (PHASE_AFTER_TOOLKIT.equals(phase)) scriptsAfterToolkit = value; else scripts = value; }
			case BRAND -> brand = value;
			case ACTIONS -> actions = value;
			case TITLE -> title = value;
			case FOOTER -> footer = value;
			case BODY -> bodyAttrs = value;
			default -> throw new IllegalArgumentException("Unknown console slot: " + slot);  // HTT: slot names are the directive's own registration constants.
		}
	}
}
