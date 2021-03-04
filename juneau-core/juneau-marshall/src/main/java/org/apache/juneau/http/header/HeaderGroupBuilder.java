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
package org.apache.juneau.http.header;

import java.util.*;

import org.apache.http.*;
import org.apache.juneau.internal.*;

/**
 * Builder for {@link HeaderGroup} objects.
 */
@FluentSetters
public class HeaderGroupBuilder {

	final List<Header> headers = new ArrayList<>();

	/**
	 * Constructor.
	 */
	public HeaderGroupBuilder() {}

	/**
	 * Creates a new {@link HeaderGroup} bean based on the contents of this builder.
	 *
	 * @return A new {@link HeaderGroup} bean.
	 */
	public HeaderGroup build() {
		return new HeaderGroup(this);
	}

	/**
	 * Removes any headers already in this builder.
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderGroupBuilder clear() {
		headers.clear();
		return this;
	}

	/**
	 * Adds the specified header to the end of the headers in this builder.
	 *
	 * @param value The header to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderGroupBuilder add(Header value) {
		if (value != null)
			headers.add(value);
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
	public HeaderGroupBuilder add(String name, String value) {
		headers.add(new BasicHeader(name, value));
		return this;
	}

	/**
	 * Adds the specified headers to the end of the headers in this builder.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderGroupBuilder add(Header...values) {
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
	public HeaderGroupBuilder add(List<Header> values) {
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
	public HeaderGroupBuilder remove(Header value) {
		if (value != null)
			headers.remove(value);
		return this;
	}

	/**
	 * Removes the specified headers from this builder.
	 *
	 * @param values The headers to remove.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderGroupBuilder remove(Header...values) {
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
	public HeaderGroupBuilder remove(List<Header> values) {
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
	public HeaderGroupBuilder remove(String name) {
		for (int i = 0; i < headers.size(); i++) /* See HTTPCORE-361 */
			if (headers.get(i).getName().equalsIgnoreCase(name))
				headers.remove(i--);
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
	public HeaderGroupBuilder update(Header value) {
		if (value == null)
			return this;

		for (int i = 0; i < headers.size(); i++) {
			Header x = headers.get(i);
			if (x.getName().equalsIgnoreCase(value.getName())) {
				headers.set(i, value);
				return this;
			}
		}

		headers.add(value);
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
	public HeaderGroupBuilder update(Header...values) {
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
	public HeaderGroupBuilder update(List<Header> values) {
		for (int i = 0; i < values.size(); i++) /* See HTTPCORE-361 */
			update(values.get(i));
		return this;
	}

	/**
	 * Sets all of the headers contained within this group overriding any existing headers.
	 *
	 * <p>
	 * The headers are added in the order in which they appear in the array.
	 *
	 * @param values The headers to set
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderGroupBuilder set(Header...values) {
		clear();
		Collections.addAll(headers, values);
		return this;
	}

	/**
	 * Sets all of the headers contained within this group overriding any existing headers.
	 *
	 * <p>
	 * The headers are added in the order in which they appear in the list.
	 *
	 * @param values The headers to set
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderGroupBuilder set(List<Header> values) {
		clear();
		headers.addAll(values);
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
	public HeaderGroupBuilder appendUnique(Header...values) {
		for (Header h : values) {
			boolean replaced = false;
			for (ListIterator<Header> li = headers.listIterator(); li.hasNext();) {
				Header h2 = li.next();
				if (h2.getName().equalsIgnoreCase(h.getName())) {
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
	public HeaderGroupBuilder appendUnique(Collection<Header> values) {
		for (Header h : values) {
			boolean replaced = false;
			for (ListIterator<Header> li = headers.listIterator(); li.hasNext();) {
				Header h2 = li.next();
				if (h2.getName().equalsIgnoreCase(h.getName())) {
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
