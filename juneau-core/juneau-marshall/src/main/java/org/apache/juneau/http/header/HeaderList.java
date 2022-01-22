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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.http.*;
import org.apache.http.util.*;
import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.http.HttpHeaders;
import org.apache.juneau.internal.*;
import org.apache.juneau.svl.*;

/**
 * An immutable list of HTTP headers.
 * {@review}
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	HeaderList <jv>headers</jv> = HeaderList
 * 		.<jsm>create</jsm>()
 * 		.append(Accept.<jsm>of</jsm>("text/xml"))
 * 		.append(<js>"Content-Type"</js>, ()-><jsm>getDynamicContentTypeFromSomewhere</jsm>())
 * 		.build();
 * </p>
 *
 * <p>
 * Convenience creators are provided for creating lists with minimal code:
 * <p class='bcode w800'>
 * 	HeaderList <jv>headers</jv> = HeaderList.<jsm>of</jsm>(Accept.<jsf>TEXT_XML</jsf>, ContentType.<jsf>TEXT_XML</jsf>);
 * </p>
 *
 * <p>
 * Header lists are immutable, but can be appended to using the {@link #copy()} method:
 * <p class='bcode w800'>
 * 	headers = headers
 * 		.copy()
 * 		.append(AcceptEncoding.<jsm>of</jsm>(<js>"identity"</js>))
 * 		.build();
 * </p>
 *
 * <p>
 * Static methods are provided on {@link HttpHeaders} to further simplify creation of header lists.
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.http.HttpHeaders.*;
 *
 * 	HeaderList <jv>headers</jv> = <jsm>headerList</jsm>(<jsm>accept</jsm>(<js>"text/xml"</js>), <jsm>contentType</jsm>(<js>"text/xml"<js>));
 * </p>
 *
 * <p>
 * The builder class supports setting default header values (i.e. add a header to the list if it isn't otherwise in the list).
 * Note that this is different from simply setting a value twice as using default values will not overwrite existing
 * headers.
 * <br>The following example notes the distinction:
 *
 * <p class='bcode w800'>
 * 	<jv>headers</jv> = HeaderList
 * 		.<jsm>create</jsm>()
 * 		.set(Accept.<jsf>TEXT_PLAIN</jsf>)
 * 		.set(Accept.<jsf>TEXT_XML</jsf>)
 * 		.build();
 * 	<jsm>assertObject</jsm>(<jv>headers</jv>).isString(<js>"[Accept: text/xml]"</js>);
 *
 * 	<jv>headers</jv> = HeaderList
 * 		.create()
 * 		.set(Accept.<jsf>TEXT_PLAIN</jsf>)
 * 		.setDefault(Accept.<jsf>TEXT_XML</jsf>)
 * 		.build();
 * 	<jsm>assertObject</jsm>(<jv>headers</jv>).isString(<js>"[Accept: text/plain]"</js>);
 * </p>
 *
 * <p>
 * Various methods are provided for iterating over the headers in this list to avoid array copies.
 * <ul class='javatree'>
 * 	<li class='jm'>{@link #forEach(Consumer)} / {@link #forEach(String,Consumer)} - Use consumers to process headers.
 * 	<li class='jm'>{@link #iterator()} / {@link #iterator(String)} - Use an {@link HeaderIterator} to process headers.
 * 	<li class='jm'>{@link #stream()} / {@link #stream(String)} - Use a stream.
 * </ul>
 * <p>
 * In general, try to use these over the {@link #getAll()}/{@link #getAll(String)} methods that require array copies.
 *
 * <p>
 * The {@link #get(String)} method is special in that it will collapse multiple headers with the same name into
 * a single comma-delimited list (see <a href='https://tools.ietf.org/html/rfc2616#section-4.2'>RFC 2616 Section 4.2</a> for rules).
 *
 * <p>
 * The {@link #get(Class)} and {@link #get(String, Class)} methods are provided for working with {@link org.apache.juneau.http.annotation.Header}-annotated
 * beans.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	ContentType <jv>contentType</jv> = <jv>headers</jv>.get(ContentType.<jk>class</jk>);
 * </p>
 *
 * <p>
 * By default, header names are treated as case-insensitive.  This can be changed using the {@link HeaderList.Builder#caseSensitive()}
 * method.
 *
 * <p>
 * A {@link VarResolver} can be associated with this builder to create header values with embedded variables that
 * are resolved at runtime.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	<jc>// Create a header list with dynamically-resolving values pulled from a system property.</jc>
 *
 * 	System.<jsm>setProperty</jsm>(<js>"foo"</js>, <js>"bar"</js>);
 *
 * 	HeaderList <jv>headers</jv> = HeaderList
 * 		.<jsm>create</jsm>()
 * 		.resolving()
 * 		.append(<js>"X1"</js>, <js>"$S{foo}"</js>)
 * 		.append(<js>"X2"</js>, ()-><js>"$S{foo}"</js>)
 * 		.build();
 *
 * 	<jsm>assertObject</jsm>(<jv>headers</jv>).isString(<js>"[X1: bar, X2: bar]"</js>);
 * </p>
 *
 * <p>
 * The {@link HeaderList} object can be extended to defined pre-packaged lists of headers which can be used in various
 * annotations throughout the framework.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	<jc>// A predefined list of headers.</jc>
 * 	<jk>public class</jk> MyHeaderList <jk>extends</jk> HeaderList {
 * 		<jk>public</jk> MyHeaderList() {
 * 			<jk>super</jk>(Accept.<jsf>TEXT_XML</jsf>, ContentType.<jsf>TEXT_XML</jsf>);
 * 		}
 * 	}
 *
 * 	<jc>// Use it on a remote proxy to add headers on all requests.</jc>
 *  <ja>@Remote</ja>(path=<js>"/petstore"</js>, headerList=MyHeaderList.<jk>class</jk>)
 * 	<jk>public interface</jk> PetStore {
 *
 * 		<ja>@RemotePost</ja>(<js>"/pets"</js>)
 * 		Pet addPet(
 * 			<ja>@Body</ja> CreatePet <jv>createPet</jv>,
 * 			<ja>@Header</ja>(<js>"E-Tag"</js>) UUID <jv>etag</jv>,
 * 			<ja>@Query</ja>(<js>"debug"</js>) <jk>boolean</jk> <jv>debug</jv>
 * 		);
 * 	}
 * </p>
 *
 * <ul class='spaced-list'>
 * 	<li class='note'>This class is thread safe.
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jm.HttpParts}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class HeaderList {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final Header[] EMPTY_ARRAY = new Header[0];

	/** Represents no header supplier in annotations. */
	public static final class Null extends HeaderList {}

	/** Predefined instance. */
	public static final HeaderList EMPTY = new HeaderList();

	/**
	 * Instantiates a new builder for this bean.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
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
		return headers == null || headers.isEmpty() ? EMPTY : new HeaderList(headers);
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
		return headers == null || headers.length == 0 ? EMPTY : new HeaderList(headers);
	}

	/**
	 * Creates a new {@link HeaderList} initialized with the specified name/value pairs.
	 *
	 * <h5 class='figure'>Example</h5>
	 * <p class='bcode w800'>
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
		if (pairs == null || pairs.length == 0)
			return EMPTY;
		if (pairs.length % 2 != 0)
			throw runtimeException("Odd number of parameters passed into HeaderList.ofPairs()");
		ArrayBuilder<Header> b = ArrayBuilder.create(Header.class, pairs.length / 2, true);
		for (int i = 0; i < pairs.length; i+=2)
			b.add(BasicHeader.of(pairs[i], pairs[i+1]));
		return new HeaderList(b.toArray());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends BeanBuilder<HeaderList> {

		final List<Header> entries;
		List<Header> defaultEntries;
		private VarResolver varResolver;
		boolean caseSensitive;

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(HeaderList.class, BeanStore.INSTANCE);
			entries = new ArrayList<>();
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy.
		 */
		protected Builder(HeaderList copyFrom) {
			super(copyFrom.getClass(), BeanStore.INSTANCE);
			entries = new ArrayList<>(copyFrom.entries.length);
			for (int i = 0; i < copyFrom.entries.length; i++)
				entries.add(copyFrom.entries[i]);
			caseSensitive = copyFrom.caseSensitive;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			entries = new ArrayList<>(copyFrom.entries);
			defaultEntries = copyFrom.defaultEntries == null ? null : new ArrayList<>(copyFrom.defaultEntries);
			varResolver = copyFrom.varResolver;
			caseSensitive = copyFrom.caseSensitive;
		}

		@Override /* BeanBuilder */
		protected HeaderList buildDefault() {
			return entries.isEmpty() && defaultEntries == null ? EMPTY : new HeaderList(this);
		}

		/**
		 * Makes a copy of this builder.
		 *
		 * @return A new copy of this builder.
		 */
		public Builder copy() {
			return new Builder(this);
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
		 * 	<li class='jm'>{@link #setDefault(String, Object) set(String,Object)}
		 * 	<li class='jm'>{@link #setDefault(String, Supplier) set(String,Supplier&lt;?&gt;)}
		 * </ul>
		 *
		 * <p>
		 * Uses {@link VarResolver#DEFAULT} to resolve variables.
		 *
		 * @return This object.
		 */
		public Builder resolving() {
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
		 * @return This object.
		 */
		public Builder resolving(VarResolver varResolver) {
			this.varResolver = varResolver;
			return this;
		}

		/**
		 * Specifies that the headers in this builder should be treated as case-sensitive.
		 *
		 * <p>
		 * The default behavior is case-insensitive.
		 *
		 * @return This object.
		 */
		public Builder caseSensitive() {
			caseSensitive = true;
			return this;
		}

		/**
		 * Removes any headers already in this builder.
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder clear() {
			entries.clear();
			return this;
		}

		/**
		 * Adds the specified headers to the end of the headers in this builder.
		 *
		 * @param value The header to add.  <jk>null</jk> values are ignored.
		 * @return This object.
		 */
		@FluentSetter
		public Builder append(HeaderList value) {
			if (value != null)
				for (Header x : value.entries)
					append(x);
			return this;
		}

		/**
		 * Adds the specified header to the end of the headers in this builder.
		 *
		 * @param value The header to add.  <jk>null</jk> values are ignored.
		 * @return This object.
		 */
		@FluentSetter
		public Builder append(Header value) {
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
		 * @return This object.
		 */
		public Builder append(String name, Object value) {
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
		 * @return This object.
		 */
		public Builder append(String name, Supplier<?> value) {
			return append(createHeader(name, value));
		}

		/**
		 * Adds the specified headers to the end of the headers in this builder.
		 *
		 * @param values The headers to add.  <jk>null</jk> values are ignored.
		 * @return This object.
		 */
		@FluentSetter
		public Builder append(Header...values) {
			if (values != null)
				for (int i = 0; i < values.length; i++)
					append(values[i]);
			return this;
		}

		/**
		 * Adds the specified headers to the end of the headers in this builder.
		 *
		 * @param values The headers to add.  <jk>null</jk> values are ignored.
		 * @return This object.
		 */
		@FluentSetter
		public Builder append(List<Header> values) {
			if (values != null)
				for (int i = 0, j = values.size(); i < j; i++) /* See HTTPCORE-361 */
					append(values.get(i));
			return this;
		}

		/**
		 * Adds the specified header to the beginning of the headers in this builder.
		 *
		 * @param value The header to add.  <jk>null</jk> values are ignored.
		 * @return This object.
		 */
		@FluentSetter
		public Builder prepend(HeaderList value) {
			if (value != null)
				entries.addAll(0, Arrays.asList(value.entries));
			return this;
		}

		/**
		 * Adds the specified header to the beginning of the headers in this builder.
		 *
		 * @param value The header to add.  <jk>null</jk> values are ignored.
		 * @return This object.
		 */
		@FluentSetter
		public Builder prepend(Header value) {
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
		 * @return This object.
		 */
		public Builder prepend(String name, Object value) {
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
		 * @return This object.
		 */
		public Builder prepend(String name, Supplier<?> value) {
			return prepend(createHeader(name, value));
		}

		/**
		 * Adds the specified headers to the beginning of the headers in this builder.
		 *
		 * @param values The headers to add.  <jk>null</jk> values are ignored.
		 * @return This object.
		 */
		@FluentSetter
		public Builder prepend(Header...values) {
			if (values != null)
				prepend(Arrays.asList(values));
			return this;
		}

		/**
		 * Adds the specified headers to the beginning of the headers in this builder.
		 *
		 * @param values The headers to add.  <jk>null</jk> values are ignored.
		 * @return This object.
		 */
		@FluentSetter
		public Builder prepend(List<Header> values) {
			if (values != null)
				entries.addAll(0, values);
			return this;
		}

		/**
		 * Removes the specified header from this builder.
		 *
		 * @param value The header to remove.  <jk>null</jk> values are ignored.
		 * @return This object.
		 */
		@FluentSetter
		public Builder remove(HeaderList value) {
			if (value != null)
				for (int i = 0; i < value.entries.length; i++)
					remove(value.entries[i]);
			return this;
		}

		/**
		 * Removes the specified header from this builder.
		 *
		 * @param value The header to remove.  <jk>null</jk> values are ignored.
		 * @return This object.
		 */
		@FluentSetter
		public Builder remove(Header value) {
			if (value != null)
				entries.remove(value);
			return this;
		}

		/**
		 * Removes the specified headers from this builder.
		 *
		 * @param values The headers to remove.  <jk>null</jk> values are ignored.
		 * @return This object.
		 */
		@FluentSetter
		public Builder remove(Header...values) {
			for (int i = 0; i < values.length; i++)
				remove(values[i]);
			return this;
		}

		/**
		 * Removes the specified headers from this builder.
		 *
		 * @param values The headers to remove.  <jk>null</jk> values are ignored.
		 * @return This object.
		 */
		@FluentSetter
		public Builder remove(List<Header> values) {
			for (int i = 0, j = values.size(); i < j; i++) /* See HTTPCORE-361 */
				remove(values.get(i));
			return this;
		}

		/**
		 * Removes the header with the specified name from this builder.
		 *
		 * @param name The header name.
		 * @return This object.
		 */
		@FluentSetter
		public Builder remove(String name) {
			for (int i = 0; i < entries.size(); i++) /* See HTTPCORE-361 */
				if (eq(entries.get(i).getName(), name))
					entries.remove(i--);
			return this;
		}

		/**
		 * Removes the header with the specified name from this builder.
		 *
		 * @param names The header name.
		 * @return This object.
		 */
		@FluentSetter
		public Builder remove(String...names) {
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
		 * @return This object.
		 */
		@FluentSetter
		public Builder set(Header value) {
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
		 * @return This object.
		 */
		@FluentSetter
		public Builder set(Header...values) {
			if (values != null)
				set(Arrays.asList(values));
			return this;
		}

		/**
		 * Replaces the first occurrence of the headers with the same name.
		 *
		 * @param name The header name.
		 * @param value The header value.
		 * @return This object.
		 */
		public Builder set(String name, Object value) {
			return set(createHeader(name, value));
		}

		/**
		 * Replaces the first occurrence of the headers with the same name.
		 *
		 * @param name The header name.
		 * @param value The header value.
		 * @return This object.
		 */
		public Builder set(String name, Supplier<?> value) {
			return set(createHeader(name, value));
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
		public Builder set(List<Header> values) {

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
		 * @return This object.
		 */
		public Builder set(HeaderList values) {
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
		 * @return This object.
		 */
		@FluentSetter
		public Builder setDefault(Header value) {
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
		 * @return This object.
		 */
		@FluentSetter
		public Builder setDefault(Header...values) {
			if (values != null)
				setDefault(Arrays.asList(values));
			return this;
		}

		/**
		 * Sets a default value for a header.
		 *
		 * @param name The header name.
		 * @param value The header value.
		 * @return This object.
		 */
		public Builder setDefault(String name, Object value) {
			return setDefault(createHeader(name, value));
		}

		/**
		 * Sets a default value for a header.
		 *
		 * @param name The header name.
		 * @param value The header value.
		 * @return This object.
		 */
		public Builder setDefault(String name, Supplier<?> value) {
			return setDefault(createHeader(name, value));
		}

		/**
		 * Sets default values for one or more headers.
		 *
		 * <p>
		 * If no header with the same name is found the given header is added to the end of the list.
		 *
		 * @param values The default headers to set.  <jk>null</jk> values are ignored.
		 * @return This object.
		 */
		@FluentSetter
		public Builder setDefault(List<Header> values) {

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
		 * @return This object.
		 */
		public Builder setDefault(HeaderList values) {
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
		 * @return This object.
		 */
		@FluentSetter
		public Builder add(ListOperation flag, Header value) {
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
		 * @return This object.
		 */
		@FluentSetter
		public Builder add(ListOperation flag, Header...values) {
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
		 * @return This object.
		 */
		public Builder add(ListOperation flag, String name, Object value) {
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
		 * @return This object.
		 */
		public Builder add(ListOperation flag, String name, Supplier<?> value) {
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
		 * @return This object.
		 */
		@FluentSetter
		public Builder add(ListOperation flag, List<Header> values) {
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
		 * @return This object.
		 */
		public Builder add(ListOperation flag, HeaderList values) {
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
		 * @return This object.
		 */
		public Builder forEach(Consumer<Header> c) {
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
		 * @return This object.
		 */
		public Builder forEach(String name, Consumer<Header> c) {
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

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder impl(Object value) {
			super.impl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder type(Class<?> value) {
			super.type(value);
			return this;
		}

		// </FluentSetters>

		//-------------------------------------------------------------------------------------------------------------
		// Other methods
		//-------------------------------------------------------------------------------------------------------------

		@Override /* Object */
		public String toString() {
			return "[" + join(entries, ", ") + "]";
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	final Header[] entries;
	final boolean caseSensitive;

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this bean.
	 */
	public HeaderList(Builder builder) {
		if (builder.defaultEntries == null) {
			entries = builder.entries.toArray(new Header[builder.entries.size()]);
		} else {
			ArrayBuilder<Header> l = ArrayBuilder.create(Header.class, builder.entries.size() + builder.defaultEntries.size(), true);

			for (int i = 0, j = builder.entries.size(); i < j; i++)
				l.add(builder.entries.get(i));

			for (int i1 = 0, j1 = builder.defaultEntries.size(); i1 < j1; i1++) {
				Header x = builder.defaultEntries.get(i1);
				boolean exists = false;
				for (int i2 = 0, j2 = builder.entries.size(); i2 < j2 && ! exists; i2++)
					exists = eq(builder.entries.get(i2).getName(), x.getName());
				if (! exists)
					l.add(x);
			}

			entries = l.toArray();
		}
		this.caseSensitive = builder.caseSensitive;
	}

	/**
	 * Constructor.
	 *
	 * @param headers
	 * 	The headers to add to the list.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br><jk>null</jk> entries are ignored.
	 */
	protected HeaderList(List<Header> headers) {
		ArrayBuilder<Header> l = ArrayBuilder.create(Header.class, headers.size(), true);
		for (int i = 0, j = headers.size(); i < j; i++)
			l.add(headers.get(i));
		entries = l.toArray();
		caseSensitive = false;
	}

	/**
	 * Constructor.
	 *
	 * @param headers
	 * 	The headers to add to the list.
	 * 	<br><jk>null</jk> entries are ignored.
	 */
	protected HeaderList(Header...headers) {
		ArrayBuilder<Header> l = ArrayBuilder.create(Header.class, headers.length, true);
		for (int i = 0; i < headers.length; i++)
			l.add(headers[i]);
		entries = l.toArray();
		caseSensitive = false;
	}

	/**
	 * Default constructor.
	 */
	protected HeaderList() {
		entries = EMPTY_ARRAY;
		caseSensitive = false;
	}

	/**
	 * Returns a builder initialized with the contents of this bean.
	 *
	 * @return A new builder object.
	 */
	public Builder copy() {
		return new Builder(this);
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
		for (int i = 0; i < entries.length; i++) {
			Header x = entries[i];
			if (eq(x.getName(), name)) {
				if (first == null)
					first = x;
				else {
					if (rest == null)
						rest = new ArrayList<>();
					rest.add(x);
				}
			}
		}

		if (first == null)
			return Optional.empty();

		if (rest == null)
			return Optional.of(first);

		CharArrayBuffer sb = new CharArrayBuffer(128);
		sb.append(first.getValue());
		for (int i = 0; i < rest.size(); i++) {
			sb.append(", ");
			sb.append(rest.get(i).getValue());
		}

		return Optional.of(new BasicHeader(name, sb.toString()));
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
	 * <p class='bcode w800'>
	 * 	BasicIntegerHeader <jv>age</jv> = headerList.get(<js>"Age"</js>, BasicIntegerHeader.<jk>class</jk>);
	 * </p>
	 *
	 * @param name The header name.
	 * @param type The header implementation class.

	 * @return A header with a condensed value or <jk>null</jk> if no headers by the given name are present
	 */
	public <T> Optional<T> get(String name, Class<T> type) {

		Header first = null;
		List<Header> rest = null;
		for (int i = 0; i < entries.length; i++) {
			Header x = entries[i];
			if (eq(x.getName(), name)) {
				if (first == null)
					first = x;
				else {
					if (rest == null)
						rest = new ArrayList<>();
					rest.add(x);
				}
			}
		}

		if (first == null)
			return Optional.empty();

		if (rest == null)
			return Optional.of(HeaderBeanMeta.of(type).construct(name, first.getValue()));

		CharArrayBuffer sb = new CharArrayBuffer(128);
		sb.append(first.getValue());
		for (int i = 0; i < rest.size(); i++) {
			sb.append(", ");
			sb.append(rest.get(i).getValue());
		}

		return Optional.of(HeaderBeanMeta.of(type).construct(name, sb.toString()));
	}

	/**
	 * Gets a header representing all of the header values with the given name.
	 *
	 * <p>
	 * Same as {@link #get(String, Class)} but the header name is pulled from the {@link org.apache.juneau.http.annotation.Header#name()} or
	 * 	{@link org.apache.juneau.http.annotation.Header#value()} annotations.
	 *
	 * <h5 class='figure'>Example</h5>
	 * <p class='bcode w800'>
	 * 	Age <jv>age</jv> = headerList.get(Age.<jk>class</jk>);
	 * </p>
	 *
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
	 * Gets all of the headers with the given name.
	 *
	 * <p>
	 * The returned array maintains the relative order in which the headers were added.
	 *
	 * <p>
	 * Header name comparison is case insensitive.
	 *
	 * @param name The header name.
	 *
	 * @return An array containing all matching headers, or an empty array if none are found.
	 */
	public Header[] getAll(String name) {
		List<Header> l = null;
		for (int i = 0; i < entries.length; i++) {
			Header x = entries[i];
			if (eq(x.getName(), name)) {
				if (l == null)
					l = new ArrayList<>();
				l.add(x);
			}
		}
		return l == null ? EMPTY_ARRAY : l.toArray(new Header[l.size()]);
	}

	/**
	 * Returns the number of headers in this list.
	 *
	 * @return The number of headers in this list.
	 */
	public int size() {
		return entries.length;
	}

	/**
	 * Gets the first header with the given name.
	 *
	 * <p>
	 * Header name comparison is case insensitive.
	 *
	 * @param name The header name.
	 * @return The first matching header, or <jk>null</jk> if not found.
	 */
	public Optional<Header> getFirst(String name) {
		for (int i = 0; i < entries.length; i++) {
			Header x = entries[i];
			if (eq(x.getName(), name))
				return Optional.of(x);
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
	 * @return The last matching header, or <jk>null</jk> if not found.
	 */
	public Optional<Header> getLast(String name) {
		for (int i = entries.length - 1; i >= 0; i--) {
			Header x = entries[i];
			if (eq(x.getName(), name))
				return Optional.of(x);
		}
		return Optional.empty();
	}

	/**
	 * Gets all of the headers contained within this list.
	 *
	 * @return An array of all the headers in this list, or an empty array if no headers are present.
	 */
	public Header[] getAll() {
		return entries.length == 0 ? EMPTY_ARRAY : Arrays.copyOf(entries, entries.length);
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
		for (int i = 0; i < entries.length; i++) {
			Header x = entries[i];
			if (eq(x.getName(), name))
				return true;
		}
		return false;
	}

	/**
	 * Returns an iterator over this list of headers.
	 *
	 * @return A new iterator over this list of headers.
	 */
	public HeaderIterator iterator() {
		return new BasicHeaderIterator(entries, null, caseSensitive);
	}

	/**
	 * Returns an iterator over the headers with a given name in this list.
	 *
	 * @param name The name of the headers over which to iterate, or <jk>null</jk> for all headers
	 *
	 * @return A new iterator over the matching headers in this list.
	 */
	public HeaderIterator iterator(String name) {
		return new BasicHeaderIterator(entries, name, caseSensitive);
	}

	/**
	 * Performs an operation on the headers of this list.
	 *
	 * <p>
	 * This is the preferred method for iterating over headers as it does not involve
	 * creation or copy of lists/arrays.
	 *
	 * @param c The consumer.
	 * @return This object.
	 */
	public HeaderList forEach(Consumer<Header> c) {
		for (int i = 0; i < entries.length; i++)
			c.accept(entries[i]);
		return this;
	}

	/**
	 * Performs an operation on the headers with the specified name in this list.
	 *
	 * <p>
	 * This is the preferred method for iterating over headers as it does not involve
	 * creation or copy of lists/arrays.
	 *
	 * @param name The header name.
	 * @param c The consumer.
	 * @return This object.
	 */
	public HeaderList forEach(String name, Consumer<Header> c) {
		for (int i = 0; i < entries.length; i++)
			if (eq(name, entries[i].getName()))
				c.accept(entries[i]);
		return this;
	}

	/**
	 * Returns a stream of the headers in this list.
	 *
	 * <p>
	 * This does not involve a copy of the underlying array of <c>Header</c> objects so should perform well.
	 *
	 * @return This object.
	 */
	public Stream<Header> stream() {
		return Arrays.stream(entries);
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
		return Arrays.stream(entries).filter(x->eq(name, x.getName()));
	}

	private boolean eq(String s1, String s2) {
		return StringUtils.eq(!caseSensitive, s1, s2);
	}

	@Override /* Object */
	public String toString() {
		return Arrays.asList(entries).toString();
	}
}
