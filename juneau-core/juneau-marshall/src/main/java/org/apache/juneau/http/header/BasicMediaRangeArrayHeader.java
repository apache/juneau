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
import static java.util.Optional.*;

import java.util.*;
import java.util.function.*;

/**
 * Category of headers that consist of multiple parameterized string values.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Accept: application/json;q=0.9,text/xml;q=0.1
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc ExtRFC2616}
 * </ul>
*/
public class BasicMediaRangeArrayHeader extends BasicStringHeader {

	private static final long serialVersionUID = 1L;

	/**
	 * Convenience creator.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new {@link BasicMediaRangeArrayHeader} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicMediaRangeArrayHeader of(String name, Object value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicMediaRangeArrayHeader(name, value);
	}

	/**
	 * Convenience creator using supplier.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new {@link BasicMediaRangeArrayHeader} object, or <jk>null</jk> if the name or value is <jk>null</jk>.
	 */
	public static BasicMediaRangeArrayHeader of(String name, Supplier<?> value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicMediaRangeArrayHeader(name, value);
	}

	private MediaRanges parsed;

	/**
	 * Constructor
	 *
	 * @param name The header name.
	 * @param value
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 		<li>A {@link Supplier} of anything on this list.
	 * 	</ul>
	 */
	public BasicMediaRangeArrayHeader(String name, Object value) {
		super(name, value);
		if (! isSupplier(value))
			parsed = parse();
	}

	/**
	 * Returns this header as a {@link MediaRanges} object.
	 *
	 * @return This header as a {@link MediaRanges} object, or {@link Optional#empty()} if the value is <jk>null</jk>
	 */
	public Optional<MediaRanges> asMediaRanges() {
		return ofNullable(parse());
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
	 * See {@doc https://www.w3.org/TR/activitypub/#retrieving-objects ActivityPub / Retrieving Objects}
	 *
	 * @param mediaTypes The media types to match against.
	 * @return The index into the array of the best match, or <c>-1</c> if no suitable matches could be found.
	 */
	public int match(List<? extends MediaType> mediaTypes) {
		return asMediaRanges().orElse(MediaRanges.EMPTY).match(mediaTypes);
	}

	/**
	 * Returns the {@link MediaRange} at the specified index.
	 *
	 * @param index The index position of the media range.
	 * @return The {@link MediaRange} at the specified index or <jk>null</jk> if the index is out of range.
	 */
	public MediaRange getRange(int index) {
		return asMediaRanges().orElse(MediaRanges.EMPTY).getRange(index);
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
		return asMediaRanges().orElse(MediaRanges.EMPTY).hasSubtypePart(part);
	}

	/**
	 * Returns the media ranges that make up this object.
	 *
	 * @return The media ranges that make up this object.
	 */
	public List<MediaRange> getRanges() {
		return asMediaRanges().orElse(MediaRanges.EMPTY).getRanges();
	}

	@Override /* Header */
	public String getValue() {
		Object o = getRawValue();
		if (o == null)
			return null;
		return stringify(asMediaRanges().orElse(MediaRanges.EMPTY));
	}

	private MediaRanges parse() {
		if (parsed != null)
			return parsed;
		Object o = getRawValue();
		if (o == null)
			return null;
		if (o instanceof MediaRanges)
			return (MediaRanges)o;
		return MediaRanges.of(o.toString());
	}
}
