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
 * Category of headers that consist of simple comma-delimited lists of strings with q-values.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Accept-Encoding: compress;q=0.5, gzip;q=1.0
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc ExtRFC2616}
 * </ul>
 */
public class BasicStringRangeArrayHeader extends BasicHeader {

	private static final long serialVersionUID = 1L;

	/**
	 * Convenience creator.
	 *
	 * @param value
	 * 	The parameter value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String} - Converted using {@link StringRanges#of(String)}.
	 * 		<li>{@link StringRanges} - Left as-is.
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new {@link BasicLongHeader} object.
	 */
	public static BasicStringRangeArrayHeader of(String name, Object value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicStringRangeArrayHeader(name, value);
	}

	/**
	 * Convenience creator using supplier.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param value
	 * 	The parameter value supplier.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String} - Converted using {@link StringRanges#of(String)}.
	 * 		<li>{@link StringRanges} - Left as-is.
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new {@link BasicLongHeader} object.
	 */
	public static BasicStringRangeArrayHeader of(String name, Supplier<?> value) {
		if (isEmpty(name) || value == null)
			return null;
		return new BasicStringRangeArrayHeader(name, value);
	}

	private StringRanges parsed;

	/**
	 * Constructor.
	 *
	 * @param name The parameter name.
	 * @param value
	 * 	The parameter value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String} - Converted using {@link StringRanges#of(String)}.
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 		<li>A {@link Supplier} of anything on this list.
	 * 	</ul>
	 */
	public BasicStringRangeArrayHeader(String name, Object value) {
		super(name, value);
		if (! isSupplier(value))
			parsed = parse();
	}

	@Override /* Header */
	public String getValue() {
		Object o = getRawValue();
		if (o == null)
			return null;
		return stringify(asRanges().orElse(StringRanges.EMPTY));
	}

	/**
	 * Returns the list of the types ranges that make up this header.
	 *
	 * <p>
	 * The types ranges in the list are sorted by their q-value in descending order.
	 *
	 * @return An unmodifiable list of type ranges, or {@link Optional#empty()} if the value is <jk>null</jk>
	 */
	public Optional<StringRanges> asRanges() {
		return ofNullable(parse());
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
	 * See {@doc https://www.w3.org/TR/activitypub/#retrieving-objects ActivityPub / Retrieving Objects}
	 *
	 * @param names The names to match against.
	 * @return The index into the array of the best match, or <c>-1</c> if no suitable matches could be found.
	 */
	public int match(List<String> names) {
		return asRanges().orElse(StringRanges.EMPTY).match(names);
	}

	/**
	 * Returns the {@link MediaRange} at the specified index.
	 *
	 * @param index The index position of the media range.
	 * @return The {@link MediaRange} at the specified index or <jk>null</jk> if the index is out of range.
	 */
	public StringRange getRange(int index) {
		return asRanges().orElse(StringRanges.EMPTY).getRange(index);
	}

	/**
	 * Returns the string ranges that make up this object.
	 *
	 * @return The string ranges that make up this object.
	 */
	public List<StringRange> getRanges() {
		return asRanges().orElse(StringRanges.EMPTY).getRanges();
	}

	private StringRanges parse() {
		if (parsed != null)
			return parsed;
		Object o = getRawValue();
		if (o == null)
			return null;
		return StringRanges.of(o.toString());
	}
}