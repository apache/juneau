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

import org.apache.juneau.commons.bean.*;

/**
 * A single sub-tab within a {@link Tab} that has {@link Tab#subtabs}, referencing exactly one of a child
 * {@link ViewDef} or raw panel {@link #content} (design doc §"Bean model"; TODO-420).
 *
 * <p>
 * A {@link Subtab} carries a stable {@code id} (the third hash segment, {@code #pageId/tabId/<subtabId>}) and
 * {@code label} (the sub-tab bar button text), plus <b>exactly one</b> of {@link #view} or {@link #content}
 * (matrix: {@code Subtab = {view} | {content}}) &mdash; enforced by {@link #validate()}. Built via
 * {@link #create(String, String)} + {@link #view(ViewDef)}/{@link #content(String)}, mirroring the Phase B builder
 * ergonomics.
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
 * 	<li class='jc'>{@link Tab}
 * 	<li class='jc'>{@link PageTable}
 * </ul>
 *
 * @since 10.0.0
 */
@BeanType(properties="id,label,view")
@SuppressWarnings("java:S1845") // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
public class Subtab {

	/** The stable sub-tab id (the third hash segment). */
	public String id;

	/** The sub-tab bar button text. */
	public String label;

	/** The referenced child view (mutually exclusive with {@link #content}). */
	public ViewDef view;

	/**
	 * Raw panel-body markup for this sub-tab (mutually exclusive with {@link #view}).  Emitted <b>verbatim</b> by
	 * {@link PageTable#of(PageDef)} via the html5 {@code rawText(...)} primitive.
	 *
	 * <h5 class='section'>Ownership contract: template engine, trusted / first-party content only</h5>
	 * <p>
	 * Identical contract to {@link Tab#content}: the <b>caller sanitizes; the framework emits verbatim</b>.
	 * {@code content} MUST carry trusted, first-party content only, and MUST NOT carry
	 * live/remote/attacker-influenceable data. See {@link Tab#content} for the full threat-model writeup.
	 */
	public String content;

	/**
	 * Starts a new {@link Subtab} builder with the specified stable id and display label.
	 *
	 * @param id The stable sub-tab id.  Must not be <jk>null</jk> or blank.
	 * @param label The sub-tab bar button text.
	 * @return A new mutable {@link Subtab} to chain builder calls on.
	 */
	public static Subtab create(String id, String label) {
		if (id == null || id.isBlank())
			throw iaex("Subtab id must not be null or blank.");
		var s = new Subtab();
		s.id = id;
		s.label = label;
		return s;
	}

	/**
	 * Sets the referenced child view.
	 *
	 * @param value The built child view.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Subtab view(ViewDef value) {
		view = value;
		return this;
	}

	/**
	 * Sets the raw panel-body markup (see {@link #content} for the full ownership contract &mdash; trusted /
	 * first-party content only, emitted verbatim).
	 *
	 * @param value The raw markup.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public Subtab content(String value) {
		content = value;
		return this;
	}

	/**
	 * Validates this sub-tab in isolation (TODO-420): exactly one of {@link #view} or {@link #content} must be set
	 * (matrix: {@code Subtab = {view} | {content}}).
	 *
	 * @throws IllegalArgumentException On any rule violation.
	 */
	void validate() {
		var hasView = view != null;
		var hasContent = content != null;
		if (hasView == hasContent)
			throw iaex("Subtab '%s' must declare exactly one of view or content.", id);
	}
}
