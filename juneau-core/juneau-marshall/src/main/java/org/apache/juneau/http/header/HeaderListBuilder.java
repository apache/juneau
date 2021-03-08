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

import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.svl.*;

/**
 * Builder for {@link HeaderList} objects.
 */
@FluentSetters
public class HeaderListBuilder {

	final List<Header> headers;
	private volatile VarResolver varResolver;

	/** Predefined instance. */
	private static final HeaderList EMPTY = new HeaderList();

	/**
	 * Constructor.
	 */
	public HeaderListBuilder() {
		headers = new ArrayList<>();
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.
	 */
	public HeaderListBuilder(HeaderList copyFrom) {
		headers = new ArrayList<>(copyFrom.headers);
	}

	/**
	 * Creates a new {@link HeaderList} bean based on the contents of this builder.
	 *
	 * @return A new {@link HeaderList} bean.
	 */
	public HeaderList build() {
		return headers.isEmpty() ? EMPTY : new HeaderList(this);
	}

	/**
	 * Allows header values to contain SVL variables.
	 *
	 * <p>
	 * Resolves variables in header values when using the following methods:
	 * <ul>
	 * 	<li class='jm'>{@link #add(String, Object) add(String,Object)}
	 * 	<li class='jm'>{@link #add(String, Supplier) add(String,Supplier&lt;?&gt;)}
	 * 	<li class='jm'>{@link #add(String, Object, HttpPartSerializerSession, HttpPartSchema, boolean) add(String,Object,HttpPartSerializerSession,HttpPartSchema,boolean)}
	 * </ul>
	 *
	 * <p>
	 * Uses {@link VarResolver#DEFAULT} to resolve variables.
	 *
	 * @return This object (for method chaining).
	 */
	public HeaderListBuilder resolving() {
		return resolving(VarResolver.DEFAULT);
	}

	/**
	 * Allows header values to contain SVL variables.
	 *
	 * <p>
	 * Resolves variables in header values when using the following methods:
	 * <ul>
	 * 	<li class='jm'>{@link #add(String, Object) add(String,Object)}
	 * 	<li class='jm'>{@link #add(String, Supplier) add(String,Supplier&lt;?&gt;)}
	 * 	<li class='jm'>{@link #add(String, Object, HttpPartSerializerSession, HttpPartSchema, boolean) add(String,Object,HttpPartSerializerSession,HttpPartSchema,boolean)}
	 * </ul>
	 *
	 * @param varResolver The variable resolver to use for resolving variables.
	 * @return This object (for method chaining).
	 */
	public HeaderListBuilder resolving(VarResolver varResolver) {
		this.varResolver = varResolver;
		return this;
	}

	/**
	 * Removes any headers already in this builder.
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderListBuilder clear() {
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
	public HeaderListBuilder add(Header value) {
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
	public HeaderListBuilder add(String name, String value) {
		Header x = isResolving() ? new BasicHeader(name, resolver(value)) : new BasicHeader(name, value);
		return add(x);
	}

	/**
	 * Appends the specified header to the end of this list.
	 *
	 * <p>
	 * The header is added as a {@link BasicHeader}.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object (for method chaining).
	 */
	public HeaderListBuilder add(String name, Object value) {
		Header x = isResolving() ? new BasicHeader(name, resolver(value)) : new BasicHeader(name, value);
		return add(x);
	}

	/**
	 * Appends the specified header to the end of this list using a value supplier.
	 *
	 * <p>
	 * The header is added as a {@link BasicHeader}.
	 *
	 * <p>
	 * Value is re-evaluated on each call to {@link BasicHeader#getValue()}.
	 *
	 * @param name The header name.
	 * @param value The header value supplier.
	 * @return This object (for method chaining).
	 */
	public HeaderListBuilder add(String name, Supplier<?> value) {
		Header x = isResolving() ? new BasicHeader(name, resolver(value)) : new BasicHeader(name, value);
		return add(x);
	}

	/**
	 * Appends the specified header to the end of this list.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @param serializer
	 * 	The serializer to use for serializing the value to a string value.
	 * @param schema
	 * 	The schema object that defines the format of the output.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the parser.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * @param skipIfEmpty If value is a blank string, the value should return as <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public HeaderListBuilder add(String name, Object value, HttpPartSerializerSession serializer, HttpPartSchema schema, boolean skipIfEmpty) {
		Header x = isResolving() ? new SerializedHeader(name, resolver(value), serializer, schema, skipIfEmpty) : new SerializedHeader(name, value, serializer, schema, skipIfEmpty);
		return add(x);
	}

	/**
	 * Adds the specified headers to the end of the headers in this builder.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderListBuilder add(Header...values) {
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
	public HeaderListBuilder add(List<Header> values) {
		for (int i = 0; i < values.size(); i++) /* See HTTPCORE-361 */
			add(values.get(i));
		return this;
	}

	/**
	 * Adds the specified headers to the end of the headers in this builder.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HeaderListBuilder add(HeaderList values) {
		if (values != null)
			add(values.getAll());
		return this;
	}

	/**
	 * Removes the specified header from this builder.
	 *
	 * @param value The header to remove.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderListBuilder remove(Header value) {
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
	public HeaderListBuilder remove(Header...values) {
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
	public HeaderListBuilder remove(List<Header> values) {
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
	public HeaderListBuilder remove(String name) {
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
	public HeaderListBuilder update(Header value) {
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
	public HeaderListBuilder update(Header...values) {
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
	public HeaderListBuilder update(List<Header> values) {
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
	public HeaderListBuilder set(Header...values) {
		clear();
		Collections.addAll(headers, values);
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
	public HeaderListBuilder set(List<Header> values) {
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
	public HeaderListBuilder appendUnique(Header...values) {
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
	public HeaderListBuilder appendUnique(Collection<Header> values) {
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

	private boolean isResolving() {
		return varResolver != null;
	}

	private Supplier<Object> resolver(Object input) {
		return ()->(varResolver == null ? unwrap(input) : varResolver.resolve(stringify(unwrap(input))));
	}

	private Object unwrap(Object o) {
		while (o instanceof Supplier)
			o = ((Supplier<?>)o).get();
		return o;
	}

	// <FluentSetters>

	// </FluentSetters>
}
