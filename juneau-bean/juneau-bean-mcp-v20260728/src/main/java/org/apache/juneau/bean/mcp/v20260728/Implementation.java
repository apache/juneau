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
 * MCP {@code Implementation} object ({@code BaseMetadata} + {@code Icons} per the schema): required
 * {@code name} / {@code version}, plus optional {@code title}, {@code description}, and ordered {@code icons}.
 */
@Marshalled
public class Implementation {

	private String name;
	private String version;
	private String title;
	private String description;
	private List<Icon> icons;

	/**
	 * Implementation name.
	 *
	 * @return The name, or {@code null} if not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the implementation name.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Implementation setName(String value) {
		name = value;
		return this;
	}

	/**
	 * Implementation version.
	 *
	 * @return The version, or {@code null} if not set.
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Sets the implementation version.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Implementation setVersion(String value) {
		version = value;
		return this;
	}

	/**
	 * Human-readable display title, distinct from the machine-readable {@code name}.
	 *
	 * @return The title, or {@code null} if not set.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the display title.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Implementation setTitle(String value) {
		title = value;
		return this;
	}

	/**
	 * Human-readable description.
	 *
	 * @return The description, or {@code null} if not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Implementation setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Ordered icon descriptors.
	 *
	 * @return The icons list, or {@code null} if not set.
	 */
	public List<Icon> getIcons() {
		return u(icons);
	}

	/**
	 * Sets the icon descriptors.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Implementation setIcons(List<Icon> value) {
		icons = value;
		return this;
	}

	/**
	 * Sets the icon descriptors.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Implementation setIcons(Icon...value) {
		icons = list(value);
		return this;
	}

	/**
	 * Appends to the icon descriptors.
	 *
	 * @param value The values to append.
	 * @return This object (for method chaining).
	 */
	public Implementation addIcons(Icon...value) {
		if (icons == null)
			icons = list();
		Collections.addAll(icons, value);
		return this;
	}

	/**
	 * Appends to the icon descriptors.
	 *
	 * @param value The values to append.
	 * @return This object (for method chaining).
	 */
	public Implementation addIcons(Collection<Icon> value) {
		if (icons == null)
			icons = list();
		icons.addAll(value);
		return this;
	}
}
