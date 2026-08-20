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
 * One labelled value in a {@link CellPopover}, bound to a top-level row-data key.
 *
 * <p>
 * Optional {@link #render} is restricted to the text-shaped built-ins ({@code date}, {@code bool},
 * {@code decimal}, {@code datetime}, {@code ts-zulu}).  HTML-shaped renderers are rejected at
 * {@link CellPopover#validate()}.
 *
 * @since 10.0.0
 */
@BeanType(properties="data,title,render")
@SuppressWarnings({
	"java:S1845" // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
})
public class PopoverField {

	/** The key into the DataTables row.  Must not be dotted. */
	public String data;

	/** Optional label shown beside the value. */
	public String title;

	/** Optional text-shaped renderer.  <jk>null</jk> paints {@code String(value)} via {@code textContent}. */
	public Render render;

	/**
	 * Creates a field bound to the specified row-data key.
	 *
	 * @param data The row key.  Must not be <jk>null</jk> or blank.
	 * @return A new {@link PopoverField}.
	 */
	public static PopoverField of(String data) {
		if (data == null || data.isBlank())
			throw iaex("PopoverField data must not be null or blank.");
		var f = new PopoverField();
		f.data = data;
		return f;
	}

	/**
	 * Sets the label shown beside the value.
	 *
	 * @param value The label.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public PopoverField title(String value) {
		title = value;
		return this;
	}

	/**
	 * Sets the named renderer using the <c>"id:field"</c> string sugar (see {@link Render#parse(String)}).
	 *
	 * @param value The render-id string.  Must not be <jk>null</jk> or blank.
	 * @return This object.
	 */
	public PopoverField render(String value) {
		render = Render.parse(value);
		return this;
	}

	/**
	 * Sets the named renderer to a pre-built {@link Render}.
	 *
	 * @param value The renderer reference.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public PopoverField render(Render value) {
		render = value;
		return this;
	}
}
