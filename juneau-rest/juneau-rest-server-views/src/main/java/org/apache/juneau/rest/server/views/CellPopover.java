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
 * Author-declared per-cell popover attached to a {@link Render}.
 *
 * <p>
 * Omitted from the {@code VIEW_META} wire when <jk>null</jk>.  The client opens it on click/Enter/Space and
 * fills it from the DataTables row already on the client &mdash; no fetch, no {@code innerHTML}.
 *
 * @since 10.0.0
 */
@BeanType(properties="title,fields")
@SuppressWarnings({
	"java:S1845" // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
})
public class CellPopover {

	/** Optional dialog title.  Also used as the trigger's accessible name when non-blank. */
	public String title;

	/** Ordered fields.  At least one is required. */
	public List<PopoverField> fields;

	/**
	 * Creates a popover with the specified fields.
	 *
	 * @param fields The fields, in display order.
	 * @return A new {@link CellPopover}.
	 */
	public static CellPopover of(PopoverField...fields) {
		var p = new CellPopover();
		p.fields = l(fields);
		return p;
	}

	/**
	 * Sets the optional dialog title.
	 *
	 * @param value The title.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public CellPopover title(String value) {
		title = value;
		return this;
	}

	/**
	 * Sets the ordered fields.
	 *
	 * @param value The fields, in display order.
	 * @return This object.
	 */
	public CellPopover fields(PopoverField...value) {
		fields = l(value);
		return this;
	}

	/**
	 * Fail-closed bean validation.
	 *
	 * @throws IllegalArgumentException If this popover is not well-formed.
	 */
	public void validate() {
		if (fields == null || fields.isEmpty())
			throw iaex("CellPopover must declare at least one field.");
		var keys = new HashSet<String>();
		for (var f : fields) {
			if (f == null)
				throw iaex("CellPopover field must not be null.");
			if (f.data == null || f.data.isBlank())
				throw iaex("PopoverField data must not be null or blank.");
			if (!keys.add(f.data))
				throw iaex("CellPopover duplicate field data key '%s'.", f.data);
			if (f.render != null) {
				if (f.render.id == null || f.render.id.isBlank())
					throw iaex("PopoverField render id must not be null or blank.");
				SinkRenderAllowlist.assertPopoverAllowed(f.render.id);
			}
		}
	}
}
