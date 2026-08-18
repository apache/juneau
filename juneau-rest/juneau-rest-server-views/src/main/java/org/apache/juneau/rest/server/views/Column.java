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

import org.apache.juneau.commons.bean.*;

/**
 * A single column descriptor in the {@code VIEW_META} wire contract (design doc §6.4).
 *
 * <p>
 * Mirrors the DataTables column shape (the DataTables-native terms {@code orderable}/{@code searchable}/
 * {@code className} are kept) and adds a named-renderer reference ({@link #render}) plus a declarative
 * {@code {property}} URL template ({@link #href}).  Optional fields ({@code name}, {@code href}, {@code className})
 * are omitted from the wire when unset.
 *
 * <p>
 * The reserved catalog/View-Settings fields ({@code format}, {@code description}, {@code pinned},
 * {@code defaultVisible}) are <b>not</b> exposed by this MVP builder and are therefore omitted from the serialized
 * contract (design doc §6.4 reserved stubs).
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link ViewDef}
 * 	<li class='jc'>{@link Render}
 * </ul>
 *
 * @since 10.0.0
 */
@BeanType(properties="data,name,title,orderable,searchable,render,href,className")
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
}
