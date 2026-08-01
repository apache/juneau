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
package org.apache.juneau.bean.mcp.v20260728;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.marshall.*;

/**
 * MCP icon descriptor (the schema's {@code Icons.icons} entry shape), used by {@link Implementation}.
 */
@Marshalled
public class Icon {

	private String src;
	private String mimeType;
	private List<String> sizes;
	private Theme theme;

	/**
	 * Icon source location (typically a URI).
	 *
	 * <p>
	 * Required by the schema; not validated by this DTO.
	 *
	 * @return The source, or {@code null} if not set.
	 */
	public String getSrc() {
		return src;
	}

	/**
	 * Sets the icon source location.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Icon setSrc(String value) {
		src = value;
		return this;
	}

	/**
	 * Icon MIME type.
	 *
	 * @return The MIME type, or {@code null} if not set.
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * Sets the icon MIME type.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Icon setMimeType(String value) {
		mimeType = value;
		return this;
	}

	/**
	 * Ordered list of available sizes (for example {@code "48x48"} or {@code "any"}).
	 *
	 * @return The sizes list, or {@code null} if not set.
	 */
	public List<String> getSizes() {
		return u(sizes);
	}

	/**
	 * Sets the ordered list of available sizes.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Icon setSizes(List<String> value) {
		sizes = value;
		return this;
	}

	/**
	 * Sets the ordered list of available sizes.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Icon setSizes(String...value) {
		sizes = list(value);
		return this;
	}

	/**
	 * Appends to the ordered list of available sizes.
	 *
	 * @param value The values to append.
	 * @return This object (for method chaining).
	 */
	public Icon addSizes(String...value) {
		if (sizes == null)
			sizes = list();
		Collections.addAll(sizes, value);
		return this;
	}

	/**
	 * Appends to the ordered list of available sizes.
	 *
	 * @param value The values to append.
	 * @return This object (for method chaining).
	 */
	public Icon addSizes(Collection<String> value) {
		if (sizes == null)
			sizes = list();
		sizes.addAll(value);
		return this;
	}

	/**
	 * Color-scheme theme this icon is suited for.
	 *
	 * @return The theme, or {@code null} if not set.
	 */
	public Theme getTheme() {
		return theme;
	}

	/**
	 * Sets the color-scheme theme.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Icon setTheme(Theme value) {
		theme = value;
		return this;
	}

	/**
	 * Icon color-scheme hint, restricted to the schema's two legal values (MCP wire uses lowercase strings).
	 */
	public enum Theme {

		/** Icon is suited for light backgrounds. */
		LIGHT("light"),

		/** Icon is suited for dark backgrounds. */
		DARK("dark");

		private final String wire;

		Theme(String wire) {
			this.wire = wire;
		}

		/**
		 * Wire token for JSON payloads.
		 *
		 * @return Lowercase MCP icon-theme string.
		 */
		public String toWire() {
			return wire;
		}

		@Override /* Object */
		public String toString() {
			return wire;
		}
	}
}
