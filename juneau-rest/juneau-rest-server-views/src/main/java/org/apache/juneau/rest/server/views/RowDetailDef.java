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

import org.apache.juneau.rest.server.widgets.*;

/**
 * The row-details expander definition: named sections, an expand GET endpoint, and optional per-section
 * {@link ActionBar}s.
 *
 * <p>
 * Structure is emitted as a {@code <template data-juneau-row-detail>} sibling of the view table; field values
 * arrive via a same-origin GET.  {@link DetailField.Format#TEXT} (the default) paints with {@code textContent};
 * {@link DetailField.Format#MARKDOWN} copies allowlisted nodes from a {@code DOMParser} document and never
 * assigns {@code innerHTML}.  This type is Java-only &mdash; it is not part of the {@code VIEW_META} JSON sidecar.
 *
 * @since 10.0.0
 */
public class RowDetailDef {

	/** The frozen contract version for the expand GET envelope and the stamped {@code data-juneau-detail-contract}. */
	public static final String CONTRACT_VERSION = "1";

	/** Same-origin path template; {@code {id}} is substituted with {@code encodeURIComponent(rowId)}. */
	public String endpoint;

	/** The named sections, in display order.  At least one is required. */
	public List<DetailSection> sections;

	/**
	 * App-approved custom renderer ids allowed on {@link DetailField#render} in addition to
	 * {@link SinkRenderAllowlist#BUILTIN_IDS}.  Blank entries fail {@link #validate(List)}.
	 */
	public Set<String> allowedCustomRenderers;

	/**
	 * Creates an empty row-detail definition.
	 *
	 * @return A new {@link RowDetailDef}.
	 */
	public static RowDetailDef create() {
		return new RowDetailDef();
	}

	/**
	 * Sets the expand GET path template.
	 *
	 * @param value A same-origin path template containing {@code {id}}.  Must not be <jk>null</jk> or blank.
	 * @return This object.
	 */
	public RowDetailDef endpoint(String value) {
		endpoint = value;
		return this;
	}

	/**
	 * Sets the named sections.
	 *
	 * @param value The sections, in display order.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public RowDetailDef sections(DetailSection...value) {
		sections = l(value);
		return this;
	}

	/**
	 * Opts in custom (non-built-in) renderer ids for {@link DetailField#render}.
	 *
	 * <p>
	 * Opt-in is id permission only: the custom renderer's HTML still goes through the closed
	 * {@code fillRenderSlot} copier.  Opting in a built-in id is a no-op.
	 *
	 * @param value Custom renderer ids.  Must not contain blank entries.
	 * @return This object.
	 */
	public RowDetailDef allowCustomRenderers(String...value) {
		allowedCustomRenderers = st(value);
		return this;
	}

	/**
	 * Fail-closed bean validation, including {@link ActionRef} existence against the enclosing view's action
	 * catalog.
	 *
	 * @param rowActions The enclosing {@link ViewDef#rowActions}, or <jk>null</jk> (any {@link ActionRef} then
	 * 	fails).
	 * @throws IllegalArgumentException If this definition is not well-formed.
	 */
	public void validate(List<RowAction> rowActions) {
		validate(rowActions, null);
	}

	/**
	 * Fail-closed bean validation, additionally validating nested {@link DetailSection#table}s and enforcing that a
	 * nested view id neither duplicates another nested view id nor collides with the enclosing view id.
	 *
	 * @param rowActions The enclosing {@link ViewDef#rowActions}, or <jk>null</jk> (any {@link ActionRef} then
	 * 	fails).
	 * @param enclosingViewId The enclosing {@link ViewDef#id}, or <jk>null</jk> to skip the parent-id collision check.
	 * @throws IllegalArgumentException If this definition is not well-formed.
	 */
	public void validate(List<RowAction> rowActions, String enclosingViewId) {
		if (endpoint == null || endpoint.isBlank())
			throw iaex("RowDetailDef endpoint must not be null or blank.");
		if (!endpoint.contains("{id}"))
			throw iaex("RowDetailDef endpoint must contain '{id}'.");
		if (!isSafeDetailEndpoint(endpoint))
			throw iaex("RowDetailDef endpoint must be a same-origin path template (no absolute URL, '..', or scheme): %s",
				endpoint);
		if (sections == null || sections.isEmpty())
			throw iaex("RowDetailDef must declare at least one section.");
		if (allowedCustomRenderers != null) {
			for (var id : allowedCustomRenderers) {
				if (id == null || id.isBlank())
					throw iaex("allowCustomRenderers entry must not be blank.");
			}
		}

		var actionIds = new HashSet<String>();
		if (rowActions != null)
			for (var a : rowActions)
				if (a != null && a.id != null)
					actionIds.add(a.id);

		var sectionIds = new HashSet<String>();
		var fieldKeys = new HashSet<String>();
		var nestedViewIds = new HashSet<String>();
		for (var s : sections) {
			if (s == null)
				throw iaex("RowDetailDef section must not be null.");
			if (s.id == null || s.id.isBlank())
				throw iaex("DetailSection id must not be null or blank.");
			if (!sectionIds.add(s.id))
				throw iaex("RowDetailDef duplicate section id '%s'.", s.id);
			if (s.columns < 1)
				throw iaex("DetailSection '%s' columns must be >= 1.", s.id);
			if (s.fields != null) {
				for (var f : s.fields) {
					if (f == null)
						throw iaex("DetailSection '%s' field must not be null.", s.id);
					if (f.data == null || f.data.isBlank())
						throw iaex("DetailSection '%s' field data must not be null or blank.", s.id);
					if (!fieldKeys.add(f.data))
						throw iaex("RowDetailDef duplicate field data key '%s'.", f.data);
					if (f.render != null) {
						if (f.render.id == null || f.render.id.isBlank())
							throw iaex("DetailField '%s' render id must not be null or blank.", f.data);
						if (f.format != null && f.format != DetailField.Format.TEXT)
							throw iaex("DetailField '%s' cannot set both render and a non-TEXT format.", f.data);
						SinkRenderAllowlist.assertAllowed(f.render.id, allowedCustomRenderers);
					}
				}
			}
			if (s.actions != null) {
				s.actions.validate();
				if (s.actions.items != null) {
					for (var item : s.actions.items) {
						if (item instanceof ActionRef ar) {
							if (!actionIds.contains(ar.id))
								throw iaex("ActionRef '%s' is not declared on the enclosing view's rowActions.", ar.id);
						}
					}
				}
			}
			if (s.table != null) {
				s.table.validate();
				var nid = s.table.view.id;
				if (enclosingViewId != null && enclosingViewId.equals(nid))
					throw iaex("DetailSection '%s' nested table view id '%s' collides with the enclosing view id.", s.id, nid);
				if (!nestedViewIds.add(nid))
					throw iaex("RowDetailDef duplicate nested table view id '%s'.", nid);
			}
		}
	}

	/**
	 * Whether {@code endpoint} is a same-origin path template: no {@code ://} , no {@code //} prefix, no scheme
	 * colon-before-slash, and no {@code ..} path segments.
	 *
	 * @param endpoint The candidate template.  May be <jk>null</jk>.
	 * @return <jk>true</jk> if the string is a same-origin path template.
	 */
	public static boolean isSafeDetailEndpoint(String endpoint) {
		if (endpoint == null || endpoint.isBlank())
			return false;
		if (endpoint.contains("://"))
			return false;
		if (endpoint.startsWith("//"))
			return false;
		var colon = endpoint.indexOf(':');
		var slash = endpoint.indexOf('/');
		if (colon >= 0 && (slash < 0 || colon < slash))
			return false;
		for (var seg : endpoint.split("/", -1)) {
			if ("..".equals(seg))
				return false;
		}
		return true;
	}
}
