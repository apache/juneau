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

import static org.apache.juneau.http.Constants.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;

import org.apache.http.*;
import org.apache.http.message.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;

/**
 * A parsed <c>Accept-Encoding</c> or similar header value.
 *
 * <p>
 * The returned ranges are sorted such that the most acceptable value is available at ordinal position
 * <js>'0'</js>, and the least acceptable at position n-1.
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Accept-Encoding request-header field is similar to Accept, but restricts the content-codings (section 3.5) that
 * are acceptable in the response.
 *
 * <p class='bcode w800'>
 * 	Accept-Encoding  = "Accept-Encoding" ":"
 * 	                   1#( codings [ ";" "q" "=" qvalue ] )
 * 	codings          = ( content-coding | "*" )
 * </p>
 *
 * <p>
 * Examples of its use are:
 * <p class='bcode w800'>
 * 	Accept-Encoding: compress, gzip
 * 	Accept-Encoding:
 * 	Accept-Encoding: *
 * 	Accept-Encoding: compress;q=0.5, gzip;q=1.0
 * 	Accept-Encoding: gzip;q=1.0, identity; q=0.5, *;q=0
 * </p>
 */
@BeanIgnore
public class StringRanges {

	/** Represents an empty string ranges object. */
	public static final StringRanges EMPTY = new StringRanges("");

	private static final Cache<String,StringRanges> CACHE = new Cache<>(NOCACHE, CACHE_MAX_SIZE);

	private final StringRange[] ranges;
	private final String string;

	/**
	 * Returns a parsed string range header value.
	 *
	 * @param value The raw string range header value.
	 * @return A parsed string range header value.
	 */
	public static StringRanges of(String value) {
		if (value == null || value.length() == 0)
			return EMPTY;

		StringRanges mr = CACHE.get(value);
		if (mr == null)
			mr = CACHE.put(value, new StringRanges(value));
		return mr;
	}

	/**
	 * Constructor.
	 *
	 * @param value The string range header value.
	 */
	public StringRanges(String value) {
		this(parse(value));
	}

	/**
	 * Constructor.
	 *
	 * @param e The parsed string range header value.
	 */
	public StringRanges(HeaderElement[] e) {

		List<StringRange> l = AList.create();
		for (HeaderElement e2 : e)
			l.add(new StringRange(e2));

		l.sort(RANGE_COMPARATOR);
		ranges = l.toArray(new StringRange[l.size()]);

		this.string = ranges.length == 1 ? ranges[0].toString() : StringUtils.join(l, ',');
	}

	/**
	 * Compares two StringRanges for equality.
	 *
	 * <p>
	 * The values are first compared according to <c>qValue</c> values.
	 * Should those values be equal, the <c>type</c> is then lexicographically compared (case-insensitive) in
	 * ascending order, with the <js>"*"</js> type demoted last in that order.
	 */
	private static final Comparator<StringRange> RANGE_COMPARATOR = new Comparator<StringRange>() {
		@Override
		public int compare(StringRange o1, StringRange o2) {
			// Compare q-values.
			int qCompare = Float.compare(o2.getQValue(), o1.getQValue());
			if (qCompare != 0)
				return qCompare;

			// Compare media-types.
			// Note that '*' comes alphabetically before letters, so just do a reverse-alphabetical comparison.
			int i = o2.toString().compareTo(o1.toString());
			return i;
		}
	};

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
		if (string.isEmpty())
			return -1;

		int matchQuant = 0, matchIndex = -1;
		float q = 0f;

		// Media ranges are ordered by 'q'.
		// So we only need to search until we've found a match.
		for (StringRange mr : ranges) {
			float q2 = mr.getQValue();

			if (q2 < q || q2 == 0)
				break;

			for (int i = 0; i < names.size(); i++) {
				String mt = names.get(i);
				int matchQuant2 = mr.match(mt);

				if (matchQuant2 > matchQuant) {
					matchIndex = i;
					matchQuant = matchQuant2;
					q = q2;
				}
			}
		}

		return matchIndex;
	}

	/**
	 * Returns the {@link MediaRange} at the specified index.
	 *
	 * @param index The index position of the media range.
	 * @return The {@link MediaRange} at the specified index or <jk>null</jk> if the index is out of range.
	 */
	public StringRange getRange(int index) {
		if (index < 0 || index >= ranges.length)
			return null;
		return ranges[index];
	}

	/**
	 * Returns the string ranges that make up this object.
	 *
	 * @return The string ranges that make up this object.
	 */
	public List<StringRange> getRanges() {
		return Collections.unmodifiableList(Arrays.asList(ranges));
	}

	private static HeaderElement[] parse(String value) {
		return BasicHeaderValueParser.parseElements(emptyIfNull(trim(value)), null);
	}

	@Override /* Object */
	public String toString() {
		return string;
	}
}
