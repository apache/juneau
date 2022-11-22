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

import static org.apache.juneau.common.internal.ArgUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ConsumerUtils.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.http.*;
import org.apache.http.util.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.http.HttpHeaders;
import org.apache.juneau.internal.*;
import org.apache.juneau.svl.*;

/**
 * A simple list of HTTP headers with various convenience methods.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bjava'>
 * 	HeaderList <jv>headers</jv> = HeaderList
 * 		.<jsm>create</jsm>()
 * 		.append(Accept.<jsm>of</jsm>(<js>"text/xml"</js>))
 * 		.append(<js>"Content-Type"</js>, ()-&gt;<jsm>getDynamicContentTypeFromSomewhere</jsm>());
 * </p>
 *
 * <p>
 * Convenience creators are provided for creating lists with minimal code:
 * <p class='bjava'>
 * 	HeaderList <jv>headers</jv> = HeaderList.<jsm>of</jsm>(Accept.<jsf>TEXT_XML</jsf>, ContentType.<jsf>TEXT_XML</jsf>);
 * </p>
 *
 * <p>
 * Static methods are provided on {@link HttpHeaders} to further simplify creation of header lists.
 * <p class='bjava'>
 * 	<jk>import static</jk> org.apache.juneau.http.HttpHeaders.*;
 *
 * 	HeaderList <jv>headers</jv> = <jsm>headerList</jsm>(<jsm>accept</jsm>(<js>"text/xml"</js>), <jsm>contentType</jsm>(<js>"text/xml"</js>));
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * </ul>
 */
@FluentSetters
public class HeaderList extends ControlledArrayList<Header> {

	private static final long serialVersionUID = 1L;

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Represents no header list in annotations. */
	public static final class Void extends HeaderList {
		private static final long serialVersionUID = 1L;
	}

	/**
	 * Instantiates a new list.
	 *
	 * @return A new list.
	 */
	public static HeaderList create() {
		return new HeaderList();
	}

	/**
	 * Creates a new {@link HeaderList} initialized with the specified headers.
	 *
	 * @param headers
	 * 	The headers to add to the list.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br><jk>null</jk> entries are ignored.
	 * @return A new unmodifiable instance, never <jk>null</jk>.
	 */
	public static HeaderList of(List<Header> headers) {
		return new HeaderList().append(headers);
	}

	/**
	 * Creates a new {@link HeaderList} initialized with the specified headers.
	 *
	 * @param headers
	 * 	The headers to add to the list.
	 * 	<br><jk>null</jk> entries are ignored.
	 * @return A new unmodifiable instance, never <jk>null</jk>.
	 */
	public static HeaderList of(Header...headers) {
		return new HeaderList().append(headers);
	}

	/**
	 * Creates a new {@link HeaderList} initialized with the specified name/value pairs.
	 *
	 * <h5 class='figure'>Example</h5>
	 * <p class='bjava'>
	 * 	HeaderList <jv>headers</jv> = HeaderList.<jsm>ofPairs</jsm>(<js>"Accept"</js>, <js>"text/xml"</js>, <js>"Content-Type"</js>, <js>"text/xml"</js>);
	 * </p>
	 *
	 * @param pairs
	 * 	Initial list of pairs.
	 * 	<br>Must be an even number of parameters representing key/value pairs.
	 * @throws RuntimeException If odd number of parameters were specified.
	 * @return A new instance.
	 */
	public static HeaderList ofPairs(String...pairs) {
		HeaderList x = new HeaderList();
		if (pairs == null)
			pairs = new String[0];
		if (pairs.length % 2 != 0)
			throw new RuntimeException("Odd number of parameters passed into HeaderList.ofPairs()");
		for (int i = 0; i < pairs.length; i+=2)
			x.add(BasicHeader.of(pairs[i], pairs[i+1]));
		return x;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private VarResolver varResolver;
	boolean caseSensitive;

	/**
	 * Constructor.
	 */
	public HeaderList() {
		super(false);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy.
	 */
	protected HeaderList(HeaderList copyFrom) {
		super(false, copyFrom);
		caseSensitive = copyFrom.caseSensitive;
	}

	/**
	 * Makes a copy of this list.
	 *
	 * @return A new copy of this list.
	 */
	public HeaderList copy() {
		return new HeaderList(this);
	}

	/**
	 * Adds a collection of default headers.
	 *
	 * <p>
	 * Default headers are set if they're not already in the list.
	 *
	 * @param headers The list of default headers.
	 * @return This object.
	 */
	public HeaderList setDefault(List<Header> headers) {
		if (headers != null)
			headers.stream().filter(x -> x != null && ! contains(x.getName())).forEach(x -> set(x));
		return this;
	}

	/**
	 * Replaces the first occurrence of the headers with the same name.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object.
	 */
	public HeaderList setDefault(String name, Object value) {
		return setDefault(createPart(name, value));
	}

	/**
	 * Replaces the first occurrence of the headers with the same name.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object.
	 */
	public HeaderList setDefault(String name, Supplier<?> value) {
		return setDefault(createPart(name, value));
	}

	/**
	 * Makes a copy of this list of headers and adds a collection of default headers.
	 *
	 * <p>
	 * Default headers are set if they're not already in the list.
	 *
	 * @param headers The list of default headers.
	 * @return A new list, or the same list if the headers were empty.
	 */
	public HeaderList setDefault(Header...headers) {
		if (headers != null)
			setDefault(Arrays.asList(headers));
		return this;
	}


	//-------------------------------------------------------------------------------------------------------------
	// Properties
	//-------------------------------------------------------------------------------------------------------------

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
	 * @return This object.
	 */
	public HeaderList resolving() {
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
	 * @return This object.
	 */
	public HeaderList resolving(VarResolver varResolver) {
		assertModifiable();
		this.varResolver = varResolver;
		return this;
	}

	/**
	 * Specifies that the headers in this list should be treated as case-sensitive.
	 *
	 * <p>
	 * The default behavior is case-insensitive.
	 *
	 * @param value The new value for this setting.
	 * @return This object.
	 */
	public HeaderList caseSensitive(boolean value) {
		assertModifiable();
		caseSensitive = value;
		return this;
	}

	/**
	 * Adds the specified header to the end of the headers in this list.
	 *
	 * @param value The header to add.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public HeaderList append(Header value) {
		if (value != null)
			add(value);
		return this;
	}

	/**
	 * Appends the specified header to the end of this list.
	 *
	 * <p>
	 * The header is added as a {@link BasicHeader}.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object.
	 */
	public HeaderList append(String name, Object value) {
		return append(createPart(name, value));
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
	 * @return This object.
	 */
	public HeaderList append(String name, Supplier<?> value) {
		return append(createPart(name, value));
	}

	/**
	 * Adds the specified headers to the end of the headers in this list.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public HeaderList append(Header...values) {
		if (values != null)
			for (int i = 0; i < values.length; i++)
				if (values[i] != null)
					append(values[i]);
		return this;
	}

	/**
	 * Adds the specified headers to the end of the headers in this list.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public HeaderList append(List<Header> values) {
		if (values != null)
			values.forEach(x -> append(x));
		return this;
	}

	/**
	 * Adds the specified header to the beginning of the headers in this list.
	 *
	 * @param value The header to add.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public HeaderList prepend(Header value) {
		if (value != null)
			add(0, value);
		return this;
	}

	/**
	 * Appends the specified header to the beginning of this list.
	 *
	 * <p>
	 * The header is added as a {@link BasicHeader}.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object.
	 */
	public HeaderList prepend(String name, Object value) {
		return prepend(createPart(name, value));
	}

	/**
	 * Appends the specified header to the beginning of this list using a value supplier.
	 *
	 * <p>
	 * The header is added as a {@link BasicHeader}.
	 *
	 * <p>
	 * Value is re-evaluated on each call to {@link BasicHeader#getValue()}.
	 *
	 * @param name The header name.
	 * @param value The header value supplier.
	 * @return This object.
	 */
	public HeaderList prepend(String name, Supplier<?> value) {
		return prepend(createPart(name, value));
	}

	/**
	 * Adds the specified headers to the beginning of the headers in this list.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public HeaderList prepend(Header...values) {
		if (values != null)
			prepend(alist(values));
		return this;
	}

	/**
	 * Adds the specified headers to the beginning of the headers in this list.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public HeaderList prepend(List<Header> values) {
		if (values != null)
			addAll(0, values);
		return this;
	}

	/**
	 * Removes the specified header from this list.
	 *
	 * @param value The header to remove.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public HeaderList remove(Header value) {
		if (value != null)
			removeIf(x -> eq(x.getName(), value.getName()) && eq(x.getValue(), value.getValue()));
		return this;
	}

	/**
	 * Removes the specified headers from this list.
	 *
	 * @param values The headers to remove.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public HeaderList remove(Header...values) {
		for (int i = 0; i < values.length; i++)
			remove(values[i]);
		return this;
	}

	/**
	 * Removes the specified headers from this list.
	 *
	 * @param values The headers to remove.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public HeaderList remove(List<Header> values) {
		if (values != null)
			values.forEach(x -> remove(x));
		return this;
	}

	/**
	 * Removes the header with the specified name from this list.
	 *
	 * @param name The header name.
	 * @return This object.
	 */
	@FluentSetter
	public HeaderList remove(String name) {
		removeIf(x -> eq(x.getName(), name));
		return this;
	}

	/**
	 * Removes the header with the specified name from this list.
	 *
	 * @param names The header name.
	 * @return This object.
	 */
	@FluentSetter
	public HeaderList remove(String...names) {
		if (names != null)
			for (int i = 0; i < names.length; i++)
				remove(names[i]);
		return this;
	}

	/**
	 * Removes all headers from this list.
	 *
	 * @return This object.
	 */
	public HeaderList removeAll() {
		clear();
		return this;
	}

	/**
	 * Adds or replaces the header(s) with the same name.
	 *
	 * <p>
	 * If no header with the same name is found the given header is added to the end of the list.
	 *
	 * @param value The headers to replace.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public HeaderList set(Header value) {
		if (value != null) {
			boolean replaced = false;
			for (int i = 0, j = size(); i < j; i++) {
				Header x = get(i);
				if (eq(x.getName(), value.getName())) {
					if (replaced) {
						remove(i);
						j--;
					} else {
						set(i, value);
						replaced = true;
					}
				}
			}

			if (! replaced)
				add(value);
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
	 * @return This object.
	 */
	@FluentSetter
	public HeaderList set(Header...values) {
		if (values != null)
			set(alist(values));
		return this;
	}

	/**
	 * Replaces the first occurrence of the headers with the same name.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object.
	 */
	public HeaderList set(String name, Object value) {
		return set(createPart(name, value));
	}

	/**
	 * Replaces the first occurrence of the headers with the same name.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object.
	 */
	public HeaderList set(String name, Supplier<?> value) {
		return set(createPart(name, value));
	}

	/**
	 * Replaces the first occurrence of the headers with the same name.
	 *
	 * <p>
	 * If no header with the same name is found the given header is added to the end of the list.
	 *
	 * @param values The headers to replace.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	@FluentSetter
	public HeaderList set(List<Header> values) {

		if (values != null) {
			for (int i1 = 0, j1 = values.size(); i1 < j1; i1++) {
				Header h = values.get(i1);
				if (h != null) {
					for (int i2 = 0, j2 = size(); i2 < j2; i2++) {
						Header x = get(i2);
						if (eq(x.getName(), h.getName())) {
							remove(i2);
							j2--;
						}
					}
				}
			}

			for (int i = 0, j = values.size(); i < j; i++) {
				Header x = values.get(i);
				if (x != null) {
					add(x);
				}
			}
		}

		return this;
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
		for (int i = 0; i < size(); i++) {
			Header x = get(i);
			if (eq(x.getName(), name))
				return optional(x);
		}
		return empty();
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
		for (int i = size() - 1; i >= 0; i--) {
			Header x = get(i);
			if (eq(x.getName(), name))
				return optional(x);
		}
		return empty();
	}

	/**
	 * Gets a header representing all of the header values with the given name.
	 *
	 * <p>
	 * If more that one header with the given name exists the values will be combined with <js>", "</js> as per
	 * <a href='https://tools.ietf.org/html/rfc2616#section-4.2'>RFC 2616 Section 4.2</a>.
	 *
	 * @param name The header name.
	 * @return A header with a condensed value, or {@link Optional#empty()} if no headers by the given name are present
	 */
	public Optional<Header> get(String name) {

		Header first = null;
		List<Header> rest = null;
		for (Header x : this) {
			if (eq(x.getName(), name)) {
				if (first == null)
					first = x;
				else {
					if (rest == null)
						rest = list();
					rest.add(x);
				}
			}
		}

		if (first == null)
			return empty();

		if (rest == null)
			return optional(first);

		CharArrayBuffer sb = new CharArrayBuffer(128);
		sb.append(first.getValue());
		for (int i = 0; i < rest.size(); i++) {
			sb.append(", ");
			sb.append(rest.get(i).getValue());
		}

		return optional(new BasicHeader(name, sb.toString()));
	}

	/**
	 * Gets a header representing all of the header values with the given name.
	 *
	 * <p>
	 * If more that one header with the given name exists the values will be combined with <js>", "</js> as per
	 * <a href='https://tools.ietf.org/html/rfc2616#section-4.2'>RFC 2616 Section 4.2</a>.
	 *
	 * <p>
	 * The implementation class must have a public constructor taking in one of the following argument lists:
	 * <ul>
	 * 	<li><c>X(String <jv>value</jv>)</c>
	 * 	<li><c>X(Object <jv>value</jv>)</c>
	 * 	<li><c>X(String <jv>name</jv>, String <jv>value</jv>)</c>
	 * 	<li><c>X(String <jv>name</jv>, Object <jv>value</jv>)</c>
	 * </ul>
	 *
	 * <h5 class='figure'>Example</h5>
	 * <p class='bjava'>
	 * 	BasicIntegerHeader <jv>age</jv> = headerList.get(<js>"Age"</js>, BasicIntegerHeader.<jk>class</jk>);
	 * </p>
	 *
	 * @param <T> The header implementation class.
	 * @param name The header name.
	 * @param type The header implementation class.
	 * @return A header with a condensed value or <jk>null</jk> if no headers by the given name are present
	 */
	public <T> Optional<T> get(String name, Class<T> type) {

		Header first = null;
		List<Header> rest = null;
		for (Header x : this) {
			if (eq(x.getName(), name)) {
				if (first == null)
					first = x;
				else {
					if (rest == null)
						rest = list();
					rest.add(x);
				}
			}
		}

		if (first == null)
			return empty();

		if (rest == null)
			return optional(HeaderBeanMeta.of(type).construct(name, first.getValue()));

		CharArrayBuffer sb = new CharArrayBuffer(128);
		sb.append(first.getValue());
		for (int i = 0; i < rest.size(); i++) {
			sb.append(", ");
			sb.append(rest.get(i).getValue());
		}

		return optional(HeaderBeanMeta.of(type).construct(name, sb.toString()));
	}

	/**
	 * Gets a header representing all of the header values with the given name.
	 *
	 * <p>
	 * Same as {@link #get(String, Class)} but the header name is pulled from the {@link org.apache.juneau.http.annotation.Header#name()} or
	 * 	{@link org.apache.juneau.http.annotation.Header#value()} annotations.
	 *
	 * <h5 class='figure'>Example</h5>
	 * <p class='bjava'>
	 * 	Age <jv>age</jv> = headerList.get(Age.<jk>class</jk>);
	 * </p>
	 *
	 * @param <T> The return type.
	 * @param type The header implementation class.
	 * @return A header with a condensed value or <jk>null</jk> if no headers by the given name are present
	 */
	public <T> Optional<T> get(Class<T> type) {
		assertArgNotNull("type", type);

		String name = HeaderBeanMeta.of(type).getSchema().getName();
		assertArg(name != null, "Header name could not be found on bean type ''{0}''", type.getName());

		return get(name, type);
	}

	/**
	 * Gets all of the headers.
	 *
	 * <p>
	 * The returned array maintains the relative order in which the headers were added.
	 * Each call creates a new array not backed by this list.
	 *
	 * <p>
	 * As a general rule, it's more efficient to use the other methods with consumers to
	 * get headers.
	 *
	 * @return An array containing all headers, never <jk>null</jk>.
	 */
	public Header[] getAll() {
		return stream().toArray(Header[]::new);
	}

	/**
	 * Gets all of the headers with the given name.
	 *
	 * <p>
	 * The returned array maintains the relative order in which the headers were added.
	 * Header name comparison is case insensitive.
	 * Headers with null values are ignored.
	 * Each call creates a new array not backed by this list.
	 *
	 * <p>
	 * As a general rule, it's more efficient to use the other methods with consumers to
	 * get headers.
	 *
	 * @param name The header name.
	 *
	 * @return An array containing all matching headers, never <jk>null</jk>.
	 */
	public Header[] getAll(String name) {
		return stream().filter(x -> eq(x.getName(), name)).toArray(Header[]::new);
	}

	/**
	 * Performs an action on the values for all matching headers in this list.
	 *
	 * @param filter A predicate to apply to each element to determine if it should be included.  Can be <jk>null</jk>.
	 * @param action An action to perform on each element.
	 * @return This object.
	 */
	public HeaderList forEachValue(Predicate<Header> filter, Consumer<String> action) {
		return forEach(filter, x -> action.accept(x.getValue()));
	}

	/**
	 * Performs an action on the values of all matching headers in this list.
	 *
	 * @param name The header name.
	 * @param action An action to perform on each element.
	 * @return This object.
	 */
	public HeaderList forEachValue(String name, Consumer<String> action) {
		return forEach(name, x -> action.accept(x.getValue()));
	}

	/**
	 * Returns all the string values for all headers with the specified name.
	 *
	 * @param name The header name.
	 * @return An array containing all values.  Never <jk>null</jk>.
	 */
	public String[] getValues(String name) {
		return stream().filter(x -> eq(x.getName(), name)).map(x -> x.getValue()).toArray(String[]::new);
	}

	/**
	 * Tests if headers with the given name are contained within this list.
	 *
	 * <p>
	 * Header name comparison is case insensitive.
	 *
	 * @param name The header name.
	 * @return <jk>true</jk> if at least one header with the name is present.
	 */
	public boolean contains(String name) {
		return stream().anyMatch(x -> eq(x.getName(), name));
	}

	/**
	 * Returns an iterator over this list of headers.
	 *
	 * @return A new iterator over this list of headers.
	 */
	public HeaderIterator headerIterator() {
		return new BasicHeaderIterator(toArray(new Header[0]), null, caseSensitive);
	}

	/**
	 * Returns an iterator over the headers with a given name in this list.
	 *
	 * @param name The name of the headers over which to iterate, or <jk>null</jk> for all headers
	 *
	 * @return A new iterator over the matching headers in this list.
	 */
	public HeaderIterator headerIterator(String name) {
		return new BasicHeaderIterator(getAll(name), name, caseSensitive);
	}

	/**
	 * Performs an action on all headers with the specified name in this list.
	 *
	 * <p>
	 * This is the preferred method for iterating over headers as it does not involve
	 * creation or copy of lists/arrays.
	 *
	 * @param name The header name.
	 * @param action An action to perform on each element.
	 * @return This object.
	 */
	public HeaderList forEach(String name, Consumer<Header> action) {
		return forEach(x -> eq(name, x.getName()), action);
	}

	/**
	 * Performs an action on all matching headers in this list.
	 *
	 * <p>
	 * This is the preferred method for iterating over headers as it does not involve
	 * creation or copy of lists/arrays.
	 *
	 * @param filter A predicate to apply to each element to determine if it should be included.  Can be <jk>null</jk>.
	 * @param action An action to perform on each element.
	 * @return This object.
	 */
	public HeaderList forEach(Predicate<Header> filter, Consumer<Header> action) {
		forEach(x -> consume(filter, action, x));
		return this;
	}

	/**
	 * Returns a stream of the headers in this list with the specified name.
	 *
	 * <p>
	 * This does not involve a copy of the underlying array of <c>Header</c> objects so should perform well.
	 *
	 * @param name The header name.
	 * @return This object.
	 */
	public Stream<Header> stream(String name) {
		return stream().filter(x->eq(name, x.getName()));
	}

	//-------------------------------------------------------------------------------------------------------------
	// Other methods
	//-------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new header out of the specified name/value pair.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return A new header.
	 */
	private Header createPart(String name, Object value) {
		boolean isResolving = varResolver != null;

		if (value instanceof Supplier<?>) {
			Supplier<?> value2 = (Supplier<?>)value;
			return isResolving ? new BasicHeader(name, resolver(value2)) : new BasicHeader(name, value2);
		}
		return isResolving ? new BasicHeader(name, resolver(value)) : new BasicHeader(name, value);
	}

	private Supplier<Object> resolver(Object input) {
		return ()->varResolver.resolve(stringify(unwrap(input)));
	}

	private Object unwrap(Object o) {
		while (o instanceof Supplier)
			o = ((Supplier<?>)o).get();
		return o;
	}

	private boolean eq(String s1, String s2) {
		return caseSensitive ? StringUtils.eq(s1, s2) : StringUtils.eqic(s1, s2);
	}

	@Override /* Object */
	public String toString() {
		return "[" + join(this, ", ") + "]";
	}

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.collections.ControlledArrayList */
	public HeaderList setUnmodifiable() {
		super.setUnmodifiable();
		return this;
	}

	// </FluentSetters>
}