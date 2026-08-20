/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  The ASF licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.  See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.server.views;

import static org.apache.juneau.commons.utils.Shorts.*;

/**
 * One field slot in a {@link DetailSection}.
 *
 * <p>
 * Values are filled from the expand GET JSON {@code fields} map.  The default {@link Format#TEXT} paints with
 * {@code textContent} only.  {@link Format#MARKDOWN} stamps {@code data-juneau-field-format="markdown"} on the
 * slot; the runtime copies allowlisted nodes from a {@code DOMParser} document and never assigns
 * {@code innerHTML}.  The expand JSON value for a markdown field is the HTML produced by a sanitizing markdown
 * renderer (see {@code juneau-rest-server-views-markdown}); it is not the raw markdown source.
 *
 * <p>
 * This does not bump {@link RowDetailDef#CONTRACT_VERSION}: the expand envelope is unchanged, the format
 * attribute is additive, and a TEXT-only consumer still paints unknown attributes via {@code textContent}.
 *
 * @since 10.0.0
 */
public class DetailField {

	/**
	 * How the expand-JSON scalar is painted into the slot.
	 *
	 * <p>
	 * Each constant carries the lowercase token emitted on {@code data-juneau-field-format}.  {@link #TEXT} is
	 * omitted from the template (the default).
	 */
	public enum Format {

		/** Paint with {@code textContent}.  The default. */
		TEXT("text"),

		/**
		 * Treat the expand-JSON value as sanitizing-markdown HTML and copy allowlisted nodes into the slot.
		 * Never {@code innerHTML}.
		 */
		MARKDOWN("markdown");

		private final String wire;

		Format(String wire) {
			this.wire = wire;
		}

		/**
		 * Returns the lowercase wire token for this format.
		 *
		 * @return The wire token (e.g. <c>"markdown"</c>).
		 */
		public String wire() {
			return wire;
		}
	}

	/** The key into the expand JSON {@code fields} map.  Unique across the whole {@link RowDetailDef}. */
	public String data;

	/** The label shown above the value slot. */
	public String title;

	/** How the slot is painted.  <jk>null</jk> means {@link Format#TEXT}. */
	public Format format;

	/**
	 * Creates a field bound to the specified expand-JSON key.
	 *
	 * @param data The {@code fields} map key.  Must not be <jk>null</jk> or blank.
	 * @return A new {@link DetailField}.
	 */
	public static DetailField of(String data) {
		if (data == null || data.isBlank())
			throw iaex("DetailField data must not be null or blank.");
		var f = new DetailField();
		f.data = data;
		return f;
	}

	/**
	 * Sets the label shown above the value slot.
	 *
	 * @param value The label.  May be <jk>null</jk> (the {@link #data} key is used as a fallback at emit time).
	 * 	An empty string suppresses the label (used for a full-width markdown body under a section title).
	 * @return This object.
	 */
	public DetailField title(String value) {
		title = value;
		return this;
	}

	/**
	 * Sets how the expand-JSON scalar is painted into the slot.
	 *
	 * @param value The format.  <jk>null</jk> means {@link Format#TEXT}.
	 * @return This object.
	 */
	public DetailField format(Format value) {
		format = value;
		return this;
	}
}
