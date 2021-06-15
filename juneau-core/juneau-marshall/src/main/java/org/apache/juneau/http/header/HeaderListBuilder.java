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

import static org.apache.juneau.internal.ExceptionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.*;
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

	final List<Header> entries;
	List<Header> defaultEntries;
	private VarResolver varResolver;
	boolean caseSensitive;

	/** Predefined instance. */
	private static final HeaderList EMPTY = new HeaderList();

	/**
	 * Constructor.
	 */
	public HeaderListBuilder() {
		entries = new ArrayList<>();
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.
	 */
	protected HeaderListBuilder(HeaderList copyFrom) {
		entries = new ArrayList<>(copyFrom.entries.length);
		for (int i = 0; i < copyFrom.entries.length; i++)
			entries.add(copyFrom.entries[i]);
		caseSensitive = copyFrom.caseSensitive;
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.
	 */
	protected HeaderListBuilder(HeaderListBuilder copyFrom) {
		entries = new ArrayList<>(copyFrom.entries);
		defaultEntries = copyFrom.defaultEntries == null ? null : new ArrayList<>(copyFrom.defaultEntries);
		varResolver = copyFrom.varResolver;
		caseSensitive = copyFrom.caseSensitive;
	}

	/**
	 * Creates a modifiable copy of this builder.
	 *
	 * @return A shallow copy of this builder.
	 */
	public HeaderListBuilder copy() {
		return new HeaderListBuilder(this);
	}

	/**
	 * Creates a new {@link HeaderList} bean based on the contents of this builder.
	 *
	 * @return A new {@link HeaderList} bean.
	 */
	public HeaderList build() {
		return entries.isEmpty() && defaultEntries == null ? EMPTY : new HeaderList(this);
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
	 * 	<li class='jm'>{@link #setDefault(String, Object) set(String,Object)}
	 * 	<li class='jm'>{@link #setDefault(String, Supplier) set(String,Supplier&lt;?&gt;)}
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
	 * 	<li class='jm'>{@link #setDefault(String, Object) set(String,Object)}
	 * 	<li class='jm'>{@link #setDefault(String, Supplier) set(String,Supplier&lt;?&gt;)}
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
		caseSensitive = true;
		return this;
	}

	/**
	 * Removes any headers already in this builder.
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderListBuilder clear() {
		entries.clear();
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
			for (Header x : value.entries)
				append(x);
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
			entries.add(value);
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
		return append(createHeader(name, value));
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
		return append(createHeader(name, value));
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
			for (int i = 0; i < values.length; i++)
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
			entries.addAll(0, Arrays.asList(value.entries));
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
			entries.add(0, value);
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
		return prepend(createHeader(name, value));
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
		return prepend(createHeader(name, value));
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
			entries.addAll(0, values);
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
			for (int i = 0; i < value.entries.length; i++)
				remove(value.entries[i]);
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
			entries.remove(value);
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
		for (int i = 0; i < values.length; i++)
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
		for (int i = 0; i < entries.size(); i++) /* See HTTPCORE-361 */
			if (eq(entries.get(i).getName(), name))
				entries.remove(i--);
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
			for (int i = 0; i < names.length; i++)
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
			for (int i = 0, j = entries.size(); i < j; i++) {
				Header x = entries.get(i);
				if (eq(x.getName(), value.getName())) {
					if (replaced) {
						entries.remove(i);
						j--;
					} else {
						entries.set(i, value);
						replaced = true;
					}
				}
			}

			if (! replaced)
				entries.add(value);
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
		return set(createHeader(name, value));
	}

	/**
	 * Replaces the first occurrence of the headers with the same name.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object (for method chaining).
	 */
	public HeaderListBuilder set(String name, Supplier<?> value) {
		return set(createHeader(name, value));
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
			for (int i1 = 0, j1 = values.size(); i1 < j1; i1++) {
				Header h = values.get(i1);
				if (h != null) {
					for (int i2 = 0, j2 = entries.size(); i2 < j2; i2++) {
						Header x = entries.get(i2);
						if (eq(x.getName(), h.getName())) {
							entries.remove(i2);
							j2--;
						}
					}
				}
			}

			for (int i = 0, j = values.size(); i < j; i++) {
				Header x = values.get(i);
				if (x != null) {
					entries.add(x);
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
		if (values != null)
			set(values.entries);
		return this;
	}

	/**
	 * Sets a default value for a header.
	 *
	 * <p>
	 * If no header with the same name is found, the given header is added to the end of the list.
	 *
	 * @param value The default header to set.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderListBuilder setDefault(Header value) {
		if (value != null) {
			boolean replaced = false;
			if (defaultEntries == null)
				defaultEntries = new ArrayList<>();
			for (int i = 0, j = defaultEntries.size(); i < j; i++) {
				Header x = defaultEntries.get(i);
				if (eq(x.getName(), value.getName())) {
					if (replaced) {
						defaultEntries.remove(i);
						j--;
					} else {
						defaultEntries.set(i, value);
						replaced = true;
					}
				}
			}

			if (! replaced)
				defaultEntries.add(value);
		}

		return this;
	}

	/**
	 * Sets default values for one or more headers.
	 *
	 * <p>
	 * If no header with the same name is found the given header is added to the end of the list.
	 *
	 * @param values The default headers to set.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderListBuilder setDefault(Header...values) {
		if (values != null)
			setDefault(Arrays.asList(values));
		return this;
	}

	/**
	 * Sets a default value for a header.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object (for method chaining).
	 */
	public HeaderListBuilder setDefault(String name, Object value) {
		return setDefault(createHeader(name, value));
	}

	/**
	 * Sets a default value for a header.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object (for method chaining).
	 */
	public HeaderListBuilder setDefault(String name, Supplier<?> value) {
		return setDefault(createHeader(name, value));
	}

	/**
	 * Sets default values for one or more headers.
	 *
	 * <p>
	 * If no header with the same name is found the given header is added to the end of the list.
	 *
	 * @param values The default headers to set.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderListBuilder setDefault(List<Header> values) {

		if (values != null) {
			if (defaultEntries == null)
				defaultEntries = new ArrayList<>();
			for (int i1 = 0, j1 = values.size(); i1 < j1; i1++) {
				Header h = values.get(i1);
				if (h != null) {
					for (int i2 = 0, j2 = defaultEntries.size(); i2 < j2; i2++) {
						Header x = defaultEntries.get(i2);
						if (eq(x.getName(), h.getName())) {
							defaultEntries.remove(i2);
							j2--;
						}
					}
				}
			}

			for (Header v : values) {
				if (v != null) {
					defaultEntries.add(v);
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
	 * @param values The default headers to set.  <jk>null</jk> values are ignored.
	 * @return This object (for method chaining).
	 */
	public HeaderListBuilder setDefault(HeaderList values) {
		if (values != null)
			setDefault(values.entries);
		return this;
	}

	/**
	 * Adds the specify header to this list.
	 *
	 * @param flag
	 * 	What to do with the header.
	 * 	<br>Possible values:
	 * 	<ul>
	 * 		<li>{@link ListOperation#APPEND APPEND} - Calls {@link #append(Header)}.
	 * 		<li>{@link ListOperation#PREPEND PREEND} - Calls {@link #prepend(Header)}.
	 * 		<li>{@link ListOperation#SET REPLACE} - Calls {@link #set(Header)}.
	 * 		<li>{@link ListOperation#DEFAULT DEFAULT} - Calls {@link #setDefault(Header)}.
	 * 	</ul>
	 * @param value The header to add.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderListBuilder add(ListOperation flag, Header value) {
		if (flag == ListOperation.APPEND)
			return append(value);
		if (flag == ListOperation.PREPEND)
			return prepend(value);
		if (flag == ListOperation.SET)
			return set(value);
		if (flag == ListOperation.DEFAULT)
			return setDefault(value);
		throw runtimeException("Invalid value specified for flag parameter on add(flag,value) method: {0}", flag);
	}

	/**
	 * Adds the specified headers to this list.
	 *
	 * @param flag
	 * 	What to do with the header.
	 * 	<br>Possible values:
	 * 	<ul>
	 * 		<li>{@link ListOperation#APPEND APPEND} - Calls {@link #append(Header[])}.
	 * 		<li>{@link ListOperation#PREPEND PREEND} - Calls {@link #prepend(Header[])}.
	 * 		<li>{@link ListOperation#SET REPLACE} - Calls {@link #set(Header[])}.
	 * 		<li>{@link ListOperation#DEFAULT DEFAULT} - Calls {@link #setDefault(Header[])}.
	 * 	</ul>
	 * @param values The headers to add.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderListBuilder add(ListOperation flag, Header...values) {
		if (flag == ListOperation.APPEND)
			return append(values);
		if (flag == ListOperation.PREPEND)
			return prepend(values);
		if (flag == ListOperation.SET)
			return set(values);
		if (flag == ListOperation.DEFAULT)
			return setDefault(values);
		throw runtimeException("Invalid value specified for flag parameter on add(flag,values) method: {0}", flag);
	}

	/**
	 * Adds the specified header to this list.
	 *
	 * @param flag
	 * 	What to do with the header.
	 * 	<br>Possible values:
	 * 	<ul>
	 * 		<li>{@link ListOperation#APPEND APPEND} - Calls {@link #append(String,Object)}.
	 * 		<li>{@link ListOperation#PREPEND PREEND} - Calls {@link #prepend(String,Object)}.
	 * 		<li>{@link ListOperation#SET REPLACE} - Calls {@link #set(String,Object)}.
	 * 		<li>{@link ListOperation#DEFAULT DEFAULT} - Calls {@link #setDefault(String,Object)}.
	 * 	</ul>
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object (for method chaining).
	 */
	public HeaderListBuilder add(ListOperation flag, String name, Object value) {
		if (flag == ListOperation.APPEND)
			return append(name, value);
		if (flag == ListOperation.PREPEND)
			return prepend(name, value);
		if (flag == ListOperation.SET)
			return set(name, value);
		if (flag == ListOperation.DEFAULT)
			return setDefault(name, value);
		throw runtimeException("Invalid value specified for flag parameter on add(flag,name,value) method: {0}", flag);
	}

	/**
	 * Adds the specified header to this list.
	 *
	 * @param flag
	 * 	What to do with the header.
	 * 	<br>Possible values:
	 * 	<ul>
	 * 		<li>{@link ListOperation#APPEND APPEND} - Calls {@link #append(String,Supplier)}.
	 * 		<li>{@link ListOperation#PREPEND PREEND} - Calls {@link #prepend(String,Supplier)}.
	 * 		<li>{@link ListOperation#SET REPLACE} - Calls {@link #set(String,Supplier)}.
	 * 		<li>{@link ListOperation#DEFAULT DEFAULT} - Calls {@link #setDefault(String,Supplier)}.
	 * 	</ul>
	 * @param name The header name.
	 * @param value The header value supplier.
	 * @return This object (for method chaining).
	 */
	public HeaderListBuilder add(ListOperation flag, String name, Supplier<?> value) {
		if (flag == ListOperation.APPEND)
			return append(name, value);
		if (flag == ListOperation.PREPEND)
			return prepend(name, value);
		if (flag == ListOperation.SET)
			return set(name, value);
		if (flag == ListOperation.DEFAULT)
			return setDefault(name, value);
		throw runtimeException("Invalid value specified for flag parameter on add(flag,name,value) method: {0}", flag);
	}

	/**
	 * Adds the specified headers to this list.
	 *
	 * @param flag
	 * 	What to do with the header.
	 * 	<br>Possible values:
	 * 	<ul>
	 * 		<li>{@link ListOperation#APPEND APPEND} - Calls {@link #append(String,Supplier)}.
	 * 		<li>{@link ListOperation#PREPEND PREEND} - Calls {@link #prepend(String,Supplier)}.
	 * 		<li>{@link ListOperation#SET REPLACE} - Calls {@link #set(String,Supplier)}.
	 * 		<li>{@link ListOperation#DEFAULT DEFAULT} - Calls {@link #setDefault(String,Supplier)}.
	 * 	</ul>
	 * @param values The headers to add.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HeaderListBuilder add(ListOperation flag, List<Header> values) {
		if (flag == ListOperation.APPEND)
			return append(values);
		if (flag == ListOperation.PREPEND)
			return prepend(values);
		if (flag == ListOperation.SET)
			return set(values);
		if (flag == ListOperation.DEFAULT)
			return setDefault(values);
		throw runtimeException("Invalid value specified for flag parameter on add(flag,values) method: {0}", flag);
	}

	/**
	 * Adds the specified headers to this list.
	 *
	 * @param flag
	 * 	What to do with the header.
	 * 	<br>Possible values:
	 * 	<ul>
	 * 		<li>{@link ListOperation#APPEND APPEND} - Calls {@link #append(HeaderList)}.
	 * 		<li>{@link ListOperation#PREPEND PREEND} - Calls {@link #prepend(HeaderList)}.
	 * 		<li>{@link ListOperation#SET REPLACE} - Calls {@link #set(HeaderList)}.
	 * 		<li>{@link ListOperation#DEFAULT DEFAULT} - Calls {@link #setDefault(HeaderList)}.
	 * 	</ul>
	 * @param values The headers to add.
	 * @return This object (for method chaining).
	 */
	public HeaderListBuilder add(ListOperation flag, HeaderList values) {
		if (flag == ListOperation.APPEND)
			return append(values);
		if (flag == ListOperation.PREPEND)
			return prepend(values);
		if (flag == ListOperation.SET)
			return set(values);
		if (flag == ListOperation.DEFAULT)
			return setDefault(values);
		throw runtimeException("Invalid value specified for flag parameter on add(flag,values) method: {0}", flag);
	}

	/**
	 * Performs an operation on the headers of this list.
	 *
	 * <p>
	 * This is the preferred method for iterating over parts as it does not involve
	 * creation or copy of lists/arrays.
	 *
	 * @param c The consumer.
	 * @return This object (for method chaining).
	 */
	public HeaderListBuilder forEach(Consumer<Header> c) {
		for (int i = 0, j = entries.size(); i < j; i++)
			c.accept(entries.get(i));
		return this;
	}

	/**
	 * Performs an operation on the headers with the specified name in this list.
	 *
	 * <p>
	 * This is the preferred method for iterating over parts as it does not involve
	 * creation or copy of lists/arrays.
	 *
	 * @param name The part name.
	 * @param c The consumer.
	 * @return This object (for method chaining).
	 */
	public HeaderListBuilder forEach(String name, Consumer<Header> c) {
		for (int i = 0, j = entries.size(); i < j; i++) {
			Header x = entries.get(i);
			if (eq(name, x.getName()))
				c.accept(x);
		}
		return this;
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

	/**
	 * Creates a new header out of the specified name/value pair.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return A new header.
	 */
	public Header createHeader(String name, Object value) {
		if (value instanceof Supplier<?>) {
			Supplier<?> value2 = (Supplier<?>)value;
			return isResolving() ? new BasicHeader(name, resolver(value2)) : new BasicHeader(name, value2);
		}
		return isResolving() ? new BasicHeader(name, resolver(value)) : new BasicHeader(name, value);
	}

	private boolean eq(String s1, String s2) {
		return caseSensitive ? StringUtils.eq(s1, s2) : StringUtils.eqic(s1, s2);
	}

	/**
	 * Gets the first header with the given name.
	 *
	 * <p>
	 * Header name comparison is case insensitive.
	 *
	 * @param name The header name.
	 * @return The first matching header, or {@link Optional#empty()} if not found.
	 */
	public Optional<Header> getFirst(String name) {
		for (int i = 0; i < entries.size(); i++) {
			Header x = entries.get(i);
			if (eq(x.getName(), name))
				return Optional.of(x);
		}
		if (defaultEntries != null) {
			for (int i = 0; i < defaultEntries.size(); i++) {
				Header x = defaultEntries.get(i);
				if (eq(x.getName(), name))
					return Optional.of(x);
			}
		}
		return Optional.empty();
	}

	/**
	 * Gets the last header with the given name.
	 *
	 * <p>
	 * Header name comparison is case insensitive.
	 *
	 * @param name The header name.
	 * @return The last matching header, or {@link Optional#empty()} if not found.
	 */
	public Optional<Header> getLast(String name) {
		for (int i = entries.size() - 1; i >= 0; i--) {
			Header x = entries.get(i);
			if (eq(x.getName(), name))
				return Optional.of(x);
		}
		if (defaultEntries != null) {
			for (int i = defaultEntries.size() - 1; i >= 0; i--) {
				Header x = defaultEntries.get(i);
				if (eq(x.getName(), name))
					return Optional.of(x);
			}
		}
		return Optional.empty();
	}

	@Override /* Object */
	public String toString() {
		return "[" + join(entries, ", ") + "]";
	}

	// <FluentSetters>

	// </FluentSetters>
}
