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

import static java.util.Collections.*;
import static org.apache.juneau.internal.StringUtils.*;
import static java.util.Arrays.*;

import java.util.*;

import org.apache.http.*;
import org.apache.http.message.*;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.*;
import org.apache.juneau.*;

/**
 * An unmodifiable list of HTTP headers.
 *
 * <p>
 * Similar to {@link HeaderGroup} but uses a builder-based approach for building header lists.
 */
public class HeaderList implements Iterable<Header> {

	/** Represents no header supplier in annotations. */
	public static final class Null extends HeaderList {}

	/** Predefined instance. */
	public static final HeaderList EMPTY = new HeaderList();

	private final List<Header> headers;

	/**
	 * Instantiates a new builder for this bean.
	 *
	 * @return A new builder.
	 */
	public static HeaderListBuilder create() {
		return new HeaderListBuilder();
	}

	/**
	 * Creates a new {@link HeaderList} initialized with the specified headers.
	 *
	 * @param headers The headers to add to the list.  Can be <jk>null</jk>.  <jk>null</jk> entries are ignored.
	 * @return A new unmodifiable instance, never <jk>null</jk>.
	 */
	public static HeaderList of(List<Header> headers) {
		return headers == null || headers.isEmpty() ? EMPTY : new HeaderList(headers);
	}

	/**
	 * Creates a new {@link HeaderList} initialized with the specified headers.
	 *
	 * @param headers The headers to add to the list.  <jk>null</jk> entries are ignored.
	 * @return A new unmodifiable instance, never <jk>null</jk>.
	 */
	public static HeaderList of(Header...headers) {
		return headers == null || headers.length == 0 ? EMPTY : new HeaderList(asList(headers));
	}

	/**
	 * Creates a new {@link HeaderList} initialized with the specified name/value pairs.
	 *
	 * @param pairs
	 * 	Initial list of pairs.
	 * 	<br>Must be an even number of parameters representing key/value pairs.
	 * @throws RuntimeException If odd number of parameters were specified.
	 * @return A new instance.
	 */
	public static HeaderList ofPairs(Object...pairs) {
		if (pairs.length == 0)
			return EMPTY;
		if (pairs.length % 2 != 0)
			throw new BasicRuntimeException("Odd number of parameters passed into HeaderList.ofPairs()");
		HeaderListBuilder b = create();
		for (int i = 0; i < pairs.length; i+=2)
			b.add(stringify(pairs[i]), pairs[i+1]);
		return new HeaderList(b);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this bean.
	 */
	public HeaderList(HeaderListBuilder builder) {
		this.headers = unmodifiableList(new ArrayList<>(builder.headers));
	}

	/**
	 * Constructor.
	 *
	 * @param headers The initial list of headers.  <jk>null</jk> entries are ignored.
	 */
	protected HeaderList(List<Header> headers) {
		if (headers == null || headers.isEmpty())
			this.headers = emptyList();
		else {
			List<Header> l = new ArrayList<>();
			for (int i = 0; i < headers.size(); i++) {
				Header x = headers.get(i);
				if (x != null)
					l.add(x);
			}
			this.headers = unmodifiableList(l);
		}
	}

	/**
	 * Default constructor.
	 */
	protected HeaderList() {
		this.headers = emptyList();
	}

	/**
	 * Returns a builder initialized with the contents of this bean.
	 *
	 * @return A new builder object.
	 */
	public HeaderListBuilder builder() {
		return create().add(headers);
	}

	/**
	 * Gets a header representing all of the header values with the given name.
	 *
	 * <p>
	 * If more that one header with the given name exists the values will be
	 * combined with a "," as per RFC 2616.
	 *
	 * <p>Header name comparison is case insensitive.
	 *
	 * @param name The header name.
	 * @return A header with a condensed value or <jk>null</jk> if no headers by the given name are present
	 */
	public Header getCondensed(String name) {
		List<Header> hdrs = get(name);

		if (hdrs.isEmpty())
			return null;

		if (hdrs.size() == 1)
			return hdrs.get(0);

		CharArrayBuffer sb = new CharArrayBuffer(128);
		sb.append(hdrs.get(0).getValue());
		for (int i = 1; i < hdrs.size(); i++) {
			sb.append(", ");
			sb.append(hdrs.get(i).getValue());
		}

		return new BasicHeader(name.toLowerCase(Locale.ROOT), sb.toString());
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
	public List<Header> get(String name) {
		List<Header> l = null;
		for (int i = 0; i < headers.size(); i++) {  // See HTTPCORE-361
			Header x = headers.get(i);
			if (x.getName().equalsIgnoreCase(name)) {
				if (l == null)
					l = new ArrayList<>();
				l.add(x);
			}
		}
		return l == null ? emptyList() : l;
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
	public Header getFirst(String name) {
		for (int i = 0; i < headers.size(); i++) {  // See HTTPCORE-361
			Header x = headers.get(i);
			if (x.getName().equalsIgnoreCase(name))
				return x;
		}
		return null;
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
	public Header getLast(String name) {
		for (int i = headers.size() - 1; i >= 0; i--) {
			Header x = headers.get(i);
			if (x.getName().equalsIgnoreCase(name))
				return x;
		}
		return null;
	}

	/**
	 * Gets all of the headers contained within this list.
	 *
	 * @return An unmodifiable list of all the headers in this list, or an empty list if no headers are present.
	 */
	public List<Header> getAll() {
		return headers;
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
		for (int i = 0; i < headers.size(); i++) {  // See HTTPCORE-361
			Header x = headers.get(i);
			if (x.getName().equalsIgnoreCase(name))
				return true;
		}
		return false;
	}

	/**
	 * Returns an iterator over this list of headers.
	 *
	 * @return A new iterator over this list of headers.
	 */
	public HeaderIterator headerIterator() {
		return new BasicListHeaderIterator(headers, null);
	}

	/**
	 * Returns an iterator over the headers with a given name in this list.
	 *
	 * @param name The name of the headers over which to iterate, or <jk>null</jk> for all headers
	 *
	 * @return A new iterator over the matching headers in this list.
	 */
	public HeaderIterator headerIterator(String name) {
		return new BasicListHeaderIterator(headers, name);
	}

	/**
	 * Returns a copy of this object.
	 *
	 * <p>
	 * Individual header values are not cloned.
	 *
	 * @return A copy of this object.
	 */
	public HeaderList copy() {
		return builder().build();
	}

	@Override /* Iterable */
	public Iterator<Header> iterator() {
		return getAll().iterator();
	}

	@Override /* Object */
	public String toString() {
		return headers.toString();
	}
}
