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
 * Category of headers that consist of multiple parameterized string values.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Accept: application/json;q=0.9,text/xml;q=0.1
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
public class BasicMediaRangesHeader extends BasicStringHeader {

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
	 * 	<br>Must be parsable by {@link MediaRanges#of(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public static BasicMediaRangesHeader of(String name, String value) {
		return value == null ? null : new BasicMediaRangesHeader(name, value);
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
	public static BasicMediaRangesHeader of(String name, MediaRanges value) {
		return value == null ? null : new BasicMediaRangesHeader(name, value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final String stringValue;
	private final MediaRanges value;
	private final Supplier<MediaRanges> supplier;

	/**
	 * Constructor.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link MediaRanges#of(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public BasicMediaRangesHeader(String name, String value) {
		super(name, value);
		this.stringValue = value;
		this.value = parse(value);
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
	public BasicMediaRangesHeader(String name, MediaRanges value) {
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
	public BasicMediaRangesHeader(String name, Supplier<MediaRanges> value) {
		super(name, (String)null);
		this.stringValue = null;
		this.value = null;
		this.supplier = value;
	}

	/**
	 * Returns the header value as a {@link MediaRanges} wrapped in an {@link Optional}.
	 *
	 * @return The header value as a {@link MediaRanges} wrapped in an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<MediaRanges> asMediaRanges() {
		return optional(value());
	}

	/**
	 * Returns the header value as a {@link MediaRanges}.
	 *
	 * @return The header value as a {@link MediaRanges}.  Can be <jk>null</jk>.
	 */
	public MediaRanges toMediaRanges() {
		return value();
	}

	/**
	 * Given a list of media types, returns the best match for this <c>Accept</c> header.
	 *
	 * <p>
	 * Note that fuzzy matching is allowed on the media types where the <c>Accept</c> header may
	 * contain additional subtype parts.
	 * <br>For example, given identical q-values and an <c>Accept</c> value of <js>"text/json+activity"</js>,
	 * the media type <js>"text/json"</js> will match if <js>"text/json+activity"</js> or <js>"text/activity+json"</js>
	 * isn't found.
	 * <br>The purpose for this is to allow serializers to match when artifacts such as <c>id</c> properties are
	 * present in the header.
	 *
	 * <p>
	 * See <a class="doclink" href="https://www.w3.org/TR/activitypub/#retrieving-objects">ActivityPub / Retrieving Objects</a>
	 *
	 * @param mediaTypes The media types to match against.
	 * @return The index into the array of the best match, or <c>-1</c> if no suitable matches could be found.
	 */
	public int match(List<? extends MediaType> mediaTypes) {
		MediaRanges x = value();
		return x == null ? -1 : x.match(mediaTypes);
	}

	/**
	 * Returns the {@link MediaRange} at the specified index.
	 *
	 * @param index The index position of the media range.
	 * @return The {@link MediaRange} at the specified index or <jk>null</jk> if the index is out of range.
	 */
	public MediaRange getRange(int index) {
		MediaRanges x = value();
		return x == null ? null : x.getRange(index);
	}

	/**
	 * Convenience method for searching through all of the subtypes of all the media ranges in this header for the
	 * presence of a subtype fragment.
	 *
	 * <p>
	 * For example, given the header <js>"text/json+activity"</js>, calling
	 * <code>hasSubtypePart(<js>"activity"</js>)</code> returns <jk>true</jk>.
	 *
	 * @param part The media type subtype fragment.
	 * @return <jk>true</jk> if subtype fragment exists.
	 */
	public boolean hasSubtypePart(String part) {
		MediaRanges x = value();
		return x == null ? false : x.hasSubtypePart(part);
	}

	@Override /* Header */
	public String getValue() {
		return stringValue != null ? stringValue : stringify(value());
	}

	/**
	 * Return the value if present, otherwise return <c>other</c>.
	 *
	 * <p>
	 * This is a shortened form for calling <c>asMediaRanges().orElse(<jv>other</jv>)</c>.
	 *
	 * @param other The value to be returned if there is no value present, can be <jk>null</jk>.
	 * @return The value, if present, otherwise <c>other</c>.
	 */
	public MediaRanges orElse(MediaRanges other) {
		MediaRanges x = value();
		return x != null ? x : other;
	}

	private MediaRanges parse(String value) {
		return value == null ? null : MediaRanges.of(value);
	}

	private MediaRanges value() {
		if (supplier != null)
			return supplier.get();
		return value;
	}
}
