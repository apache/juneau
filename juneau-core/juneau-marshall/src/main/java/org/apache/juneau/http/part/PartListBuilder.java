// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.http.part;

import java.util.*;

import org.apache.juneau.internal.*;

/**
 * Builder for {@link PartList} objects.
 */
@FluentSetters
public class PartListBuilder {

	final List<Part> parts = new ArrayList<>();

	/**
	 * Constructor.
	 */
	public PartListBuilder() {}

	/**
	 * Creates a new {@link PartList} bean based on the contents of this builder.
	 *
	 * @return A new {@link PartList} bean.
	 */
	public PartList build() {
		return new PartList(this);
	}

	/**
	 * Removes any headers already in this builder.
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder clear() {
		parts.clear();
		return this;
	}

	/**
	 * Adds the specified header to the end of the headers in this builder.
	 *
	 * @param value The header to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder add(Part value) {
		if (value != null)
			parts.add(value);
		return this;
	}

	/**
	 * Adds the specified header to the end of the headers in this builder.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder add(String name, String value) {
		parts.add(new BasicPart(name, value));
		return this;
	}

	/**
	 * Adds the specified headers to the end of the headers in this builder.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder add(Part...values) {
		for (int i = 0; i < values.length; i++) /* See HTTPCORE-361 */
			add(values[i]);
		return this;
	}

	/**
	 * Adds the specified headers to the end of the headers in this builder.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder add(List<Part> values) {
		for (int i = 0; i < values.size(); i++) /* See HTTPCORE-361 */
			add(values.get(i));
		return this;
	}

	/**
	 * Removes the specified header from this builder.
	 *
	 * @param value The header to remove.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder remove(Part value) {
		if (value != null)
			parts.remove(value);
		return this;
	}

	/**
	 * Removes the specified headers from this builder.
	 *
	 * @param values The headers to remove.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder remove(Part...values) {
		for (int i = 0; i < values.length; i++) /* See HTTPCORE-361 */
			remove(values[i]);
		return this;
	}

	/**
	 * Removes the specified headers from this builder.
	 *
	 * @param values The headers to remove.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder remove(List<Part> values) {
		for (int i = 0; i < values.size(); i++) /* See HTTPCORE-361 */
			remove(values.get(i));
		return this;
	}

	/**
	 * Removes the header with the specified name from this builder.
	 *
	 * @param name The header name.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder remove(String name) {
		for (int i = 0; i < parts.size(); i++) /* See HTTPCORE-361 */
			if (parts.get(i).getName().equals(name))
				parts.remove(i--);
		return null;
	}

	/**
	 * Replaces the first occurrence of the header with the same name.
	 *
	 * <p>
	 * If no header with the same name is found the given header is added to the end of the list.
	 *
	 * @param value The headers to replace.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder update(Part value) {
		if (value == null)
			return this;

		for (int i = 0; i < parts.size(); i++) {
			Part x = parts.get(i);
			if (x.getName().equals(value.getName())) {
				parts.set(i, value);
				return this;
			}
		}

		parts.add(value);
		return this;
	}

	/**
	 * Replaces the first occurrence of the headers with the same name.
	 *
	 * <p>
	 * If no header with the same name is found the given header is added to the end of the list.
	 *
	 * @param values The headers to replace.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder update(Part...values) {
		for (int i = 0; i < values.length; i++) /* See HTTPCORE-361 */
			update(values[i]);
		return this;
	}

	/**
	 * Replaces the first occurrence of the headers with the same name.
	 *
	 * <p>
	 * If no header with the same name is found the given header is added to the end of the list.
	 *
	 * @param values The headers to replace.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder update(List<Part> values) {
		for (int i = 0; i < values.size(); i++) /* See HTTPCORE-361 */
			update(values.get(i));
		return this;
	}

	/**
	 * Sets all of the headers contained within this list overriding any existing headers.
	 *
	 * <p>
	 * The headers are added in the order in which they appear in the array.
	 *
	 * @param values The headers to set
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder set(Part...values) {
		clear();
		Collections.addAll(parts, values);
		return this;
	}

	/**
	 * Sets all of the headers contained within this list overriding any existing headers.
	 *
	 * <p>
	 * The headers are added in the order in which they appear in the list.
	 *
	 * @param values The headers to set
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public PartListBuilder set(List<Part> values) {
		clear();
		parts.addAll(values);
		return this;
	}

	/**
	 * Appends or replaces the header values in this list.
	 *
	 * <p>
	 * If the header already exists in this list, it will be replaced with the new value.
	 * Otherwise it will be appended to the end of this list.
	 *
	 * @param values The values to append or replace in this list.
	 * @return This object (for method chaining).
	 */
	public PartListBuilder appendUnique(Part...values) {
		for (Part h : values) {
			boolean replaced = false;
			for (ListIterator<Part> li = parts.listIterator(); li.hasNext();) {
				Part h2 = li.next();
				if (h2.getName().equals(h.getName())) {
					li.set(h);
					replaced = true;
					break;
				}
			}
			if (! replaced)
				add(h);
		}
		return this;
	}


	/**
	 * Appends or replaces the header values in this list.
	 *
	 * <p>
	 * If the header already exists in this list, it will be replaced with the new value.
	 * Otherwise it will be appended to the end of this list.
	 *
	 * @param values The values to append or replace in this list.
	 * @return This object (for method chaining).
	 */
	public PartListBuilder appendUnique(Collection<Part> values) {
		for (Part h : values) {
			boolean replaced = false;
			for (ListIterator<Part> li = parts.listIterator(); li.hasNext();) {
				Part h2 = li.next();
				if (h2.getName().equals(h.getName())) {
					li.set(h);
					replaced = true;
					break;
				}
			}
			if (! replaced)
				add(h);
		}
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>
}
