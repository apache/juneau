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
 * A single column descriptor in the {@code VIEW_META} wire contract (design doc §6.4).
 *
 * <p>
 * Mirrors the DataTables column shape (the DataTables-native terms {@code orderable}/{@code searchable}/
 * {@code className} are kept) and adds a named-renderer reference ({@link #render}) plus a declarative
 * {@code {property}} URL template ({@link #href}).  Optional fields ({@code name}, {@code href}, {@code className})
 * are omitted from the wire when unset, as are the column-configurator fields ({@link #pinned},
 * {@link #defaultVisible}, {@link #formats}) &mdash; each is a nullable wrapper type left <jk>null</jk> by default
 * so the serializer drops it, rather than a primitive that would emit on every column.
 *
 * <p>
 * The reserved catalog/View-Settings fields ({@code format}, {@code description}) remain <b>not</b> exposed by
 * this MVP builder and are therefore omitted from the serialized contract (design doc §6.4 reserved stubs); the
 * <b>selected</b> format for a column (which id from {@link #formats} is active) is persistence-only and never a
 * wire field here.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link ViewDef}
 * 	<li class='jc'>{@link Render}
 * </ul>
 *
 * @since 10.0.0
 */
@BeanType(properties="data,name,title,orderable,searchable,render,href,className,pinned,defaultVisible,formats")
@SuppressWarnings("java:S1845") // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
public class Column {

	/** The bean-property / JSON key this column reads from each row. */
	public String data;

	/** Optional stable DataTables column {@code name}; defaults to {@link #data} when omitted. */
	public String name;

	/** The header text. */
	public String title;

	/** Whether the column is orderable (DataTables-native term).  Defaults to <jk>true</jk>. */
	public boolean orderable = true;

	/** Whether the column is searchable.  Defaults to <jk>true</jk>. */
	public boolean searchable = true;

	/** The named cell-renderer reference. */
	public Render render;

	/** Optional declarative <c>{property}</c> URL template consumed by the {@code linked} renderer. */
	public String href;

	/** Optional static CSS class on the cell (DataTables-native {@code className}). */
	public String className;

	/**
	 * Whether this column is always visible and un-hideable in the column chooser (its checkbox renders disabled).
	 * A pinned column may still be freely reordered.  Omitted from the wire when unset (not pinned).
	 */
	public Boolean pinned;

	/**
	 * Whether this column is in the initial visible set when no saved view overrides it.  Absent/<jk>null</jk> is
	 * treated as <jk>true</jk> by the client.  Omitted from the wire when unset.
	 */
	public Boolean defaultVisible;

	/**
	 * The selectable P3 renderer ids (e.g. {@code "date"}, {@code "datetime"}, {@code "ts-zulu"}) offered by the
	 * column chooser's per-column format dropdown.  Absent/<jk>null</jk> means no dropdown; the column always
	 * renders with its fixed {@link #render}.  The chooser-selected format itself is persistence-only, never a
	 * wire field on this bean.  Omitted from the wire when unset.
	 */
	public List<String> formats;

	/**
	 * Creates a column bound to the specified data key, with {@code orderable}/{@code searchable} defaulting to
	 * <jk>true</jk>.
	 *
	 * @param data The bean-property / JSON key.  Must not be <jk>null</jk>.
	 * @return A new {@link Column}.
	 */
	public static Column of(String data) {
		var c = new Column();
		c.data = data;
		return c;
	}

	/**
	 * Sets the stable DataTables column name.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public Column name(String value) {
		name = value;
		return this;
	}

	/**
	 * Sets the header text.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public Column title(String value) {
		title = value;
		return this;
	}

	/**
	 * Sets whether the column is orderable.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public Column orderable(boolean value) {
		orderable = value;
		return this;
	}

	/**
	 * Sets whether the column is searchable.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public Column searchable(boolean value) {
		searchable = value;
		return this;
	}

	/**
	 * Sets the named cell-renderer using the <c>"id:field"</c> string sugar (see {@link Render#parse(String)}).
	 *
	 * @param value The render-id string.  E.g. <c>"tag:status"</c> or <c>"date"</c>.
	 * @return This object.
	 */
	public Column render(String value) {
		render = Render.parse(value);
		return this;
	}

	/**
	 * Sets the named cell-renderer to a pre-built {@link Render} (canonical form).
	 *
	 * @param value The renderer reference.
	 * @return This object.
	 */
	public Column render(Render value) {
		render = value;
		return this;
	}

	/**
	 * Sets the declarative <c>{property}</c> URL template.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public Column href(String value) {
		href = value;
		return this;
	}

	/**
	 * Sets the static CSS class on the cell.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public Column className(String value) {
		className = value;
		return this;
	}

	/**
	 * Marks this column as always-visible and un-hideable in the column chooser (still freely reorderable).
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public Column pinned(boolean value) {
		pinned = value;
		return this;
	}

	/**
	 * Sets whether this column is in the initial visible set when no saved view overrides it.
	 *
	 * @param value The new value.  Absent/<jk>null</jk> is treated as <jk>true</jk> by the client.
	 * @return This object.
	 */
	public Column defaultVisible(boolean value) {
		defaultVisible = value;
		return this;
	}

	/**
	 * Sets the selectable P3 renderer ids offered by the column chooser's per-column format dropdown.
	 *
	 * @param value The renderer ids.  Can be <jk>null</jk>/omitted to unset (no dropdown).
	 * @return This object.
	 */
	public Column formats(String...value) {
		formats = l(value);
		return this;
	}
}
