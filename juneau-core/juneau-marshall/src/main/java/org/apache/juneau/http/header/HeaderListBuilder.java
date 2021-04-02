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
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.svl.*;

/**
 * Builder for {@link HeaderList} objects.
 * {@review}
 */
@FluentSetters
@NotThreadSafe
public class HeaderListBuilder {

	final List<Header> headers;
	private VarResolver varResolver;
	boolean caseSensitive = false;

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
		headers = new ArrayList<>(copyFrom.headers.length);
		for (int i = 0; i < copyFrom.headers.length; i++)
			headers.add(copyFrom.headers[i]);
		caseSensitive = copyFrom.caseSensitive;
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
	 * 	<li class='jm'>{@link #append(String, Object) append(String,Object)}
	 * 	<li class='jm'>{@link #append(String, Supplier) append(String,Supplier&lt;?&gt;)}
	 * 	<li class='jm'>{@link #prepend(String, Object) prepend(String,Object)}
	 * 	<li class='jm'>{@link #prepend(String, Supplier) prepend(String,Supplier&lt;?&gt;)}
	 * 	<li class='jm'>{@link #set(String, Object) set(String,Object)}
	 * 	<li class='jm'>{@link #set(String, Supplier) set(String,Supplier&lt;?&gt;)}
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
	 * 	<li class='jm'>{@link #append(String, Object) append(String,Object)}
	 * 	<li class='jm'>{@link #append(String, Supplier) append(String,Supplier&lt;?&gt;)}
	 * 	<li class='jm'>{@link #prepend(String, Object) prepend(String,Object)}
	 * 	<li class='jm'>{@link #prepend(String, Supplier) prepend(String,Supplier&lt;?&gt;)}
	 * 	<li class='jm'>{@link #set(String, Object) set(String,Object)}
	 * 	<li class='jm'>{@link #set(String, Supplier) set(String,Supplier&lt;?&gt;)}
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
	 * Specifies that the headers in this builder should be treated as case-sensitive.
	 *
	 * <p>
	 * The default behavior is case-insensitive.
	 *
	 * @return This object (for method chaining).
	 */
	public HeaderListBuilder caseSensitive() {
		this.caseSensitive = true;
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
	 * Adds the specified headers to the end of the headers in this builder.
	 *
	 * @param value The header to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderListBuilder append(HeaderList value) {
		if (value != null)
			for (Header h : value.headers)
				append(h);
		return this;
	}

	/**
	 * Adds the specified header to the end of the headers in this builder.
	 *
	 * @param value The header to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderListBuilder append(Header value) {
		if (value != null)
			headers.add(value);
		return this;
	}

	/**
	 * Appends the specified header to the end of this builder.
	 *
	 * <p>
	 * The header is added as a {@link BasicHeader}.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object (for method chaining).
	 */
	public HeaderListBuilder append(String name, Object value) {
		return append(header(name, value));
	}

	/**
	 * Appends the specified header to the end of this builder using a value supplier.
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
	public HeaderListBuilder append(String name, Supplier<?> value) {
		return append(header(name, value));
	}

	/**
	 * Adds the specified headers to the end of the headers in this builder.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderListBuilder append(Header...values) {
		if (values != null)
			for (int i = 0; i < values.length; i++) /* See HTTPCORE-361 */
				append(values[i]);
		return this;
	}

	/**
	 * Adds the specified headers to the end of the headers in this builder.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderListBuilder append(List<Header> values) {
		if (values != null)
			for (int i = 0, j = values.size(); i < j; i++) /* See HTTPCORE-361 */
				append(values.get(i));
		return this;
	}

	/**
	 * Adds the specified header to the beginning of the headers in this builder.
	 *
	 * @param value The header to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderListBuilder prepend(HeaderList value) {
		if (value != null)
			headers.addAll(0, Arrays.asList(value.headers));
		return this;
	}

	/**
	 * Adds the specified header to the beginning of the headers in this builder.
	 *
	 * @param value The header to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderListBuilder prepend(Header value) {
		if (value != null)
			headers.add(0, value);
		return this;
	}

	/**
	 * Appends the specified header to the beginning of this builder.
	 *
	 * <p>
	 * The header is added as a {@link BasicHeader}.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object (for method chaining).
	 */
	public HeaderListBuilder prepend(String name, Object value) {
		return prepend(header(name, value));
	}

	/**
	 * Appends the specified header to the beginning of this builder using a value supplier.
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
	public HeaderListBuilder prepend(String name, Supplier<?> value) {
		return prepend(header(name, value));
	}

	/**
	 * Adds the specified headers to the beginning of the headers in this builder.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderListBuilder prepend(Header...values) {
		if (values != null)
			prepend(Arrays.asList(values));
		return this;
	}

	/**
	 * Adds the specified headers to the beginning of the headers in this builder.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderListBuilder prepend(List<Header> values) {
		if (values != null)
			headers.addAll(0, values);
		return this;
	}

	/**
	 * Removes the specified header from this builder.
	 *
	 * @param value The header to remove.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderListBuilder remove(HeaderList value) {
		if (value != null)
			for (int i = 0; i < value.headers.length; i++) /* See HTTPCORE-361 */
				remove(value.headers[i]);
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
		for (int i = 0, j = values.size(); i < j; i++) /* See HTTPCORE-361 */
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
			if (eq(headers.get(i).getName(), name))
				headers.remove(i--);
		return this;
	}

	/**
	 * Removes the header with the specified name from this builder.
	 *
	 * @param names The header name.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderListBuilder remove(String...names) {
		if (names != null)
			for (int i = 0; i < names.length; i++) /* See HTTPCORE-361 */
				remove(names[i]);
		return this;
	}

	/**
	 * Adds or replaces the header(s) with the same name.
	 *
	 * <p>
	 * If no header with the same name is found the given header is added to the end of the list.
	 *
	 * @param value The headers to replace.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderListBuilder set(Header value) {
		if (value != null) {
			boolean replaced = false;
			for (int i = 0, j = headers.size(); i < j; i++) {
				Header x = headers.get(i);
				if (eq(x.getName(), value.getName())) {
					if (replaced) {
						headers.remove(i);
						j--;
					} else {
						headers.set(i, value);
						replaced = true;
					}
				}
			}

			if (! replaced)
				headers.add(value);
		}

		return this;
	}

	/**
	 * Adds or replaces the header(s) with the same name.
	 *
	 * <p>
	 * If no header with the same name is found the given header is added to the end of the list.
	 *
	 * @param values The headers to replace.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderListBuilder set(Header...values) {
		if (values != null)
			set(Arrays.asList(values));
		return this;
	}

	/**
	 * Replaces the first occurrence of the headers with the same name.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object (for method chaining).
	 */
	public HeaderListBuilder set(String name, Object value) {
		return set(header(name, value));
	}

	/**
	 * Replaces the first occurrence of the headers with the same name.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object (for method chaining).
	 */
	public HeaderListBuilder set(String name, Supplier<?> value) {
		return set(header(name, value));
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
	public HeaderListBuilder set(List<Header> values) {

		if (values != null) {
			for (int k = 0, m = values.size(); k < m; k++) {
				Header h = values.get(k);
				if (h != null) {
					for (int i = 0, j = headers.size(); i < j; i++) {
						Header x = headers.get(i);
						if (eq(x.getName(), h.getName())) {
							headers.remove(i);
							j--;
						}
					}
				}
			}

			for (Header v : values) {
				if (v != null) {
					headers.add(v);
				}
			}
		}

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
	public HeaderListBuilder set(HeaderList values) {
		return set(values.headers);
	}

	private boolean isResolving() {
		return varResolver != null;
	}

	private Supplier<Object> resolver(Object input) {
		return ()->varResolver.resolve(stringify(unwrap(input)));
	}

	private Object unwrap(Object o) {
		while (o instanceof Supplier)
			o = ((Supplier<?>)o).get();
		return o;
	}

	private Header header(String name, Object value) {
		return isResolving() ? new BasicHeader(name, resolver(value)) : new BasicHeader(name, value);
	}

	private Header header(String name, Supplier<?> value) {
		return isResolving() ? new BasicHeader(name, resolver(value)) : new BasicHeader(name, value);
	}

	private boolean eq(String s1, String s2) {
		return caseSensitive ? StringUtils.eq(s1, s2) : StringUtils.eqic(s1, s2);
	}
	// <FluentSetters>

	// </FluentSetters>
}
