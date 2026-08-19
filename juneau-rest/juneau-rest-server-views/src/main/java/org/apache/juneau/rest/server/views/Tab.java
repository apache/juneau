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
package org.apache.juneau.rest.server.views;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.commons.bean.*;

/**
 * A single top-level tab within a {@link PageDef}, referencing one child {@link ViewDef}, an ordered list of
 * {@link Subtab}, raw panel {@link #content}, or {@link #content} paired with {@link #subtabs} as a preamble
 * (design doc §"Bean model"; TODO-420).
 *
 * <p>
 * A {@link Tab} carries a stable {@code id} (the second hash segment, {@code #pageId/<tabId>/...}) and {@code label}
 * (the tab-bar button text), plus a panel body governed by the following matrix &mdash; enforced by
 * {@link #validate()} (called from {@link PageDef#build()}, not by the individual setters, so builder call order
 * stays unconstrained):
 *
 * <h5 class='section'>Panel-body matrix</h5>
 * <p>
 * <b>{@code Tab = {view} | {subtabs} | {content} | {content+subtabs}}</b> &mdash; exactly one of {@link #view} /
 * {@link #subtabs} / {@link #content}, <b>except</b> that {@link #content} may co-occur with {@link #subtabs}: in
 * that combination {@link #content} is a preamble rendered above the sub-tab bar, inside this tab's outer panel
 * (visible for every sub-tab, no new routing attribute &mdash; see {@link PageTable}'s panel markup contract). This
 * is deliberately not a clean three-way exclusive-or; {@link #view} never combines with anything, but
 * {@link #content} and {@link #subtabs} do.
 *
 * <h5 class='section'>{@code content}'s ownership contract: template engine, trusted / first-party content only</h5>
 * <p>
 * See {@link #content} for the full contract. In short: this framework is a template engine on this path &mdash;
 * the <b>caller pre-sanitizes</b>, the framework emits {@link #content} <b>verbatim</b>, unescaped. It exists for
 * first-party, trusted prose (the FG-2 docs-page use case), never for live/remote/attacker-influenceable data.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link PageDef}
 * 	<li class='jc'>{@link Subtab}
 * 	<li class='jc'>{@link PageTable}
 * </ul>
 *
 * @since 10.0.0
 */
@BeanType(properties="id,label,view,subtabs")
@SuppressWarnings("java:S1845") // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
public class Tab {

	/** The stable tab id (the second hash segment), unique across the page. */
	public String id;

	/** The tab-bar button text. */
	public String label;

	/** The referenced child view (mutually exclusive with {@link #subtabs} and {@link #content}). */
	public ViewDef view;

	/** The ordered sub-tabs (mutually exclusive with {@link #view}; may co-occur with {@link #content}). */
	public List<Subtab> subtabs;

	/**
	 * Raw panel-body markup for this tab (mutually exclusive with {@link #view}; may co-occur with {@link #subtabs}
	 * as a preamble rendered above the sub-tab bar &mdash; see the panel-body matrix in this class's javadoc).
	 * Emitted <b>verbatim</b> &mdash; unescaped &mdash; by {@link PageTable#of(PageDef)} via the html5
	 * {@code rawText(...)} primitive (a plain {@code String} child would instead be entity-escaped and would break
	 * markup-bearing prose).
	 *
	 * <h5 class='section'>Ownership contract: template engine, trusted / first-party content only</h5>
	 * <p>
	 * This framework is a <b>template engine</b> on this path: <b>the caller is responsible for sanitizing
	 * {@code content} before setting it; the framework performs no sanitization and emits the string exactly as
	 * given.</b> Nothing in this framework neutralizes {@code <script>}, {@code <style>}, inline event handlers, or
	 * {@code url(...)} sinks in this value &mdash; {@link org.apache.juneau.commons.utils.StringUtils#escapeForScript
	 * StringUtils.escapeForScript} is JSON-in/JSON-out and is the wrong tool for an HTML body, and no CSS/script
	 * scanner in this framework inspects consumer-supplied markup.
	 *
	 * <p>
	 * Accordingly, {@code content} MUST carry <b>trusted, first-party content only</b> &mdash; markup the
	 * application itself authored or rendered from a trusted, non-attacker-controlled source (e.g. hand-written
	 * page prose, or HTML rendered from a first-party markdown document). {@code content} MUST NOT carry
	 * live/remote/attacker-influenceable data (a request parameter, a third-party API response, any value derived
	 * from something an untrusted party can influence). Pouring such data into this sink is stored XSS in the
	 * page's own trusted origin. A write-path confirmation/detail body built from live data must use a typed/escaped
	 * path instead (an html5 bean tree, or {@code escapeHtml}), never this field &mdash; a build-gating scanner
	 * enforces that separation for this framework's own sources (see the {@code RawContentSinkScanner} test-only
	 * guard in this module's test tree).
	 */
	public String content;

	/**
	 * Starts a new {@link Tab} builder with the specified stable id and display label.
	 *
	 * @param id The stable tab id.  Must not be <jk>null</jk> or blank.
	 * @param label The tab-bar button text.
	 * @return A new mutable {@link Tab} to chain builder calls on.
	 */
	public static Tab create(String id, String label) {
		if (id == null || id.isBlank())
			throw iaex("Tab id must not be null or blank.");
		var t = new Tab();
		t.id = id;
		t.label = label;
		return t;
	}

	/**
	 * Sets the referenced child view (a leaf tab with no sub-tabs).
	 *
	 * @param value The built child view.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Tab view(ViewDef value) {
		view = value;
		return this;
	}

	/**
	 * Sets the ordered sub-tabs.
	 *
	 * @param value The sub-tabs, in display order.
	 * @return This object.
	 */
	public Tab subtabs(Subtab...value) {
		subtabs = l(value);
		return this;
	}

	/**
	 * Sets the raw panel-body markup (see {@link #content} for the full ownership contract &mdash; trusted /
	 * first-party content only, emitted verbatim).
	 *
	 * @param value The raw markup.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Tab content(String value) {
		content = value;
		return this;
	}

	/**
	 * Validates this tab in isolation (design doc §"Bean model" validation rules; TODO-420 widens this from a
	 * three-way exclusive-or to the panel-body matrix documented in this class's javadoc):
	 * {@code {view} | {subtabs} | {content} | {content+subtabs}}. {@link #view} is exclusive of everything else;
	 * {@link #content} and {@link #subtabs} may co-occur (the preamble case). Every declared sub-tab is also
	 * validated in isolation (see {@link Subtab#validate()}), and sub-tab ids must be unique <b>within this tab</b>
	 * (a sub-tab id may safely recur under a different tab &mdash; the hash's {@code tabId} segment already
	 * disambiguates the parent before {@code subtabId} is ever resolved).
	 *
	 * @throws IllegalArgumentException On any rule violation.
	 */
	void validate() {
		var hasView = view != null;
		var hasSubtabs = subtabs != null && !subtabs.isEmpty();
		var hasContent = content != null;
		if (hasView) {
			if (hasSubtabs || hasContent)
				throw iaex("Tab '%s' must declare exactly one of view, subtabs, or content "
					+ "(view cannot combine with subtabs or content).", id);
		} else if (!hasSubtabs && !hasContent) {
			throw iaex("Tab '%s' must declare exactly one of view, subtabs, or content.", id);
		}
		if (hasSubtabs) {
			var ids = new HashSet<String>();
			for (var s : subtabs) {
				s.validate();
				if (!ids.add(s.id))
					throw iaex("Tab '%s': duplicate subtab id '%s'.", id, s.id);
			}
		}
	}
}
