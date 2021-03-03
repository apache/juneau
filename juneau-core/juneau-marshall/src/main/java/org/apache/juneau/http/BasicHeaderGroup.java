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
package org.apache.juneau.http;

import static java.util.Optional.*;

import java.util.*;

import org.apache.http.*;
import org.apache.http.message.*;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.*;

/**
 * An unmodifiable group of HTTP headers.
 *
 * <p>
 * Similar to {@link HeaderGroup} but uses a builder-based approach for building header groups.
 */
public class BasicHeaderGroup {

	/** Predefined instance. */
	public static final BasicHeaderGroup INSTANCE = create().build();

	private static final Header[] EMPTY = new Header[] {};

	private final List<Header> headers;

	/**
	 * Instantiates a new builder for this bean.
	 *
	 * @return A new builder.
	 */
	public static BasicHeaderGroupBuilder create() {
		return new BasicHeaderGroupBuilder();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this bean.
	 */
	public BasicHeaderGroup(BasicHeaderGroupBuilder builder) {
		this.headers = new ArrayList<>(builder.headers);
	}

	/**
	 * Returns a builder initialized with the contents of this bean.
	 *
	 * @return A new builder object.
	 */
	public BasicHeaderGroupBuilder builder() {
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
	public Optional<Header> getCondensedHeader(final String name) {
		Header[] hdrs = getHeaders(name);

		if (hdrs.length == 0)
			return empty();

		if (hdrs.length == 1)
			return of(hdrs[0]);

		CharArrayBuffer valueBuffer = new CharArrayBuffer(128);
		valueBuffer.append(hdrs[0].getValue());
		for (int i = 1; i < hdrs.length; i++) {
			valueBuffer.append(", ");
			valueBuffer.append(hdrs[i].getValue());
		}

		return of(new BasicHeader(name.toLowerCase(Locale.ROOT), valueBuffer.toString()));
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
	public Header[] getHeaders(final String name) {
		// HTTPCORE-361 : we don't use the for-each syntax, i.e.
		//	 for (Header header : headers)
		// as that creates an Iterator that needs to be garbage-collected
		List<Header> l = null;
		for (int i = 0; i < headers.size(); i++) {
			Header x = headers.get(i);
			if (x.getName().equalsIgnoreCase(name)) {
				if (l == null)
					l = new ArrayList<>();
				l.add(x);
			}
		}
		return l != null ? l.toArray(new Header[l.size()]) : EMPTY;
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
	public Header getFirstHeader(String name) {
		// HTTPCORE-361 : we don't use the for-each syntax, i.e.
		//	 for (Header header : headers)
		// as that creates an Iterator that needs to be garbage-collected
		for (int i = 0; i < headers.size(); i++) {
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
	public Header getLastHeader(String name) {
		for (int i = headers.size() - 1; i >= 0; i--) {
			Header x = headers.get(i);
			if (x.getName().equalsIgnoreCase(name))
				return x;
		}
		return null;
	}

	/**
	 * Gets all of the headers contained within this group.
	 *
	 * @return An array containing all the headers within this group, or an empty array if no headers are present.
	 */
	public Header[] getAllHeaders() {
		return headers.toArray(new Header[headers.size()]);
	}

	/**
	 * Tests if headers with the given name are contained within this group.
	 *
	 * <p>
	 * Header name comparison is case insensitive.
	 *
	 * @param name The header name.
	 * @return <jk>true</jk> if at least one header with the name is present.
	 */
	public boolean containsHeader(String name) {
		// HTTPCORE-361 : we don't use the for-each syntax, i.e.
		//	 for (Header header : headers)
		// as that creates an Iterator that needs to be garbage-collected
		for (int i = 0; i < headers.size(); i++) {
			Header x = headers.get(i);
			if (x.getName().equalsIgnoreCase(name))
				return true;
		}
		return false;
	}

	/**
	 * Returns an iterator over this group of headers.
	 *
	 * @return A new iterator over this group of headers.
	 */
	public HeaderIterator iterator() {
		return new BasicListHeaderIterator(this.headers, null);
	}

	/**
	 * Returns an iterator over the headers with a given name in this group.
	 *
	 * @param name The name of the headers over which to iterate, or <jk>null</jk> for all headers
	 *
	 * @return A new iterator over the matching headers in this group.
	 */
	public HeaderIterator iterator(String name) {
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
	public BasicHeaderGroup copy() {
		return builder().build();
	}

	@Override /* Object */
	public String toString() {
		return headers.toString();
	}
}
