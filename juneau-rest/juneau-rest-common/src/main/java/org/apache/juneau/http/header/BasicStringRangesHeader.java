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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;

/**
 * Category of headers that consist of simple comma-delimited lists of strings with q-values.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Accept-Encoding: compress;q=0.5, gzip;q=1.0
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
public class BasicStringRangesHeader extends BasicHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;

	/**
	 * Static creator.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link StringRanges#of(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public static BasicStringRangesHeader of(String name, String value) {
		return value == null ? null : new BasicStringRangesHeader(name, value);
	}

	/**
	 * Static creator.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public static BasicStringRangesHeader of(String name, StringRanges value) {
		return value == null ? null : new BasicStringRangesHeader(name, value);
	}

	/**
	 * Static creator with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public static BasicStringRangesHeader of(String name, Supplier<StringRanges> value) {
		return value == null ? null : new BasicStringRangesHeader(name, value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final String stringValue;
	private final StringRanges value;
	private final Supplier<StringRanges> supplier;

	/**
	 * Constructor.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link StringRanges#of(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public BasicStringRangesHeader(String name, String value) {
		super(name, value);
		this.stringValue = value;
		this.value = StringRanges.of(value);
		this.supplier = null;
	}

	/**
	 * Constructor.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public BasicStringRangesHeader(String name, StringRanges value) {
		super(name, stringify(value));
		this.stringValue = null;
		this.value = value;
		this.supplier = null;
	}

	/**
	 * Constructor with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public BasicStringRangesHeader(String name, Supplier<StringRanges> value) {
		super(name, null);
		this.stringValue = null;
		this.value = null;
		this.supplier = value;
	}

	@Override /* Header */
	public String getValue() {
		return stringValue != null ? stringValue : stringify(value());
	}

	/**
	 * Returns the header value as a {@link StringRanges} wrapped in an {@link Optional}.
	 *
	 * @return The header value as a {@link StringRanges} wrapped in an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<StringRanges> asStringRanges() {
		return optional(value());
	}

	/**
	 * Returns the header value as a {@link StringRanges}.
	 *
	 * @return The header value as a {@link StringRanges}.  Can be <jk>null</jk>.
	 */
	public StringRanges toStringRanges() {
		return value();
	}

	/**
	 * Given a list of media types, returns the best match for this string range header.
	 *
	 * <p>
	 * Note that fuzzy matching is allowed on the media types where the string range header may
	 * contain additional subtype parts.
	 * <br>For example, given identical q-values and an string range value of <js>"text/json+activity"</js>,
	 * the media type <js>"text/json"</js> will match if <js>"text/json+activity"</js> or <js>"text/activity+json"</js>
	 * isn't found.
	 * <br>The purpose for this is to allow serializers to match when artifacts such as <c>id</c> properties are
	 * present in the header.
	 *
	 * <p>
	 * See <a class="doclink" href="https://www.w3.org/TR/activitypub/#retrieving-objects">ActivityPub / Retrieving Objects</a>
	 *
	 * @param names The names to match against.
	 * @return The index into the array of the best match, or <c>-1</c> if no suitable matches could be found.
	 */
	public int match(List<String> names) {
		StringRanges x = value();
		return x == null ? -1 : x.match(names);
	}

	/**
	 * Returns the {@link MediaRange} at the specified index.
	 *
	 * @param index The index position of the media range.
	 * @return The {@link MediaRange} at the specified index or <jk>null</jk> if the index is out of range.
	 */
	public StringRange getRange(int index) {
		StringRanges x = value();
		return x == null ? null : x.getRange(index);
	}

	/**
	 * Return the value if present, otherwise return <c>other</c>.
	 *
	 * <p>
	 * This is a shortened form for calling <c>asArray().orElse(<jv>other</jv>)</c>.
	 *
	 * @param other The value to be returned if there is no value present, can be <jk>null</jk>.
	 * @return The value, if present, otherwise <c>other</c>.
	 */
	public StringRanges orElse(StringRanges other) {
		StringRanges x = value();
		return x != null ? x : other;
	}

	private StringRanges value() {
		if (supplier != null)
			return supplier.get();
		return value;
	}
}