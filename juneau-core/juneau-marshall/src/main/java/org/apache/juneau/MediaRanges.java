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
package org.apache.juneau;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.http.message.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.internal.*;

/**
 * A parsed <c>Accept</c> or similar header value.
 *
 * <p>
 * The returned media ranges are sorted such that the most acceptable media is available at ordinal position
 * <js>'0'</js>, and the least acceptable at position n-1.
 *
 * <p>
 * The syntax expected to be found in the referenced <c>value</c> complies with the syntax described in
 * RFC2616, Section 14.1, as described below:
 * <p class='bcode'>
 * 	Accept         = "Accept" ":"
 * 	                  #( media-range [ accept-params ] )
 *
 * 	media-range    = ( "*\/*"
 * 	                  | ( type "/" "*" )
 * 	                  | ( type "/" subtype )
 * 	                  ) *( ";" parameter )
 * 	accept-params  = ";" "q" "=" qvalue *( accept-extension )
 * 	accept-extension = ";" token [ "=" ( token | quoted-string ) ]
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
@BeanIgnore
public class MediaRanges {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Represents an empty media ranges object. */
	public static final MediaRanges EMPTY = new MediaRanges("");

	private static final Cache<String,MediaRanges> CACHE = Cache.of(String.class, MediaRanges.class).build();

	private final MediaRange[] ranges;
	private final String string;

	/**
	 * Returns a parsed <c>Accept</c> header value.
	 *
	 * @param value The raw <c>Accept</c> header value.
	 * @return A parsed <c>Accept</c> header value.
	 */
	public static MediaRanges of(String value) {
		return isEmpty(value) ? EMPTY : CACHE.get(value, ()->new MediaRanges(value));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param value The <c>Accept</c> header value.
	 */
	public MediaRanges(String value) {
		this(parse(value));
	}

	/**
	 * Constructor.
	 *
	 * @param e The parsed header value.
	 */
	public MediaRanges(HeaderElement[] e) {

		ranges = new MediaRange[e.length];
		for (int i = 0; i < e.length; i++)
			ranges[i] = new MediaRange(e[i]);
		Arrays.sort(ranges, RANGE_COMPARATOR);

		this.string = ranges.length == 1 ? ranges[0].toString() : StringUtils.join(ranges, ',');
	}

	/**
	 * Compares two MediaRanges for equality.
	 *
	 * <p>
	 * The values are first compared according to <c>qValue</c> values.
	 * Should those values be equal, the <c>type</c> is then lexicographically compared (case-insensitive) in
	 * ascending order, with the <js>"*"</js> type demoted last in that order.
	 * <c>MediaRanges</c> with the same type but different sub-types are compared - a more specific subtype is
	 * promoted over the 'wildcard' subtype.
	 * <c>MediaRanges</c> with the same types but with extensions are promoted over those same types with no
	 * extensions.
	 */
	private static final Comparator<MediaRange> RANGE_COMPARATOR = new Comparator<>() {
		@Override
		public int compare(MediaRange o1, MediaRange o2) {
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
		if (string.isEmpty() || mediaTypes == null)
			return -1;

		int matchQuant = 0, matchIndex = -1;
		float q = 0f;

		// Media ranges are ordered by 'q'.
		// So we only need to search until we've found a match.
		for (MediaRange mr : ranges) {
			float q2 = mr.getQValue();

			if (q2 < q || q2 == 0)
				break;

			for (int i = 0; i < mediaTypes.size(); i++) {
				MediaType mt = mediaTypes.get(i);
				int matchQuant2 = mr.match(mt, false);

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
	public MediaRange getRange(int index) {
		if (index < 0 || index >= ranges.length)
			return null;
		return ranges[index];
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

		for (MediaRange mr : ranges)
			if (mr.getQValue() > 0 && mr.getSubTypes().indexOf(part) >= 0)
				return true;

		return false;
	}

	/**
	 * Returns the media ranges that make up this object.
	 *
	 * @return The media ranges that make up this object.
	 */
	public List<MediaRange> toList() {
		return ulist(ranges);
	}

	/**
	 * Performs an action on the media ranges that make up this object.
	 *
	 * @param action The action to perform.
	 * @return This object.
	 */
	public MediaRanges forEachRange(Consumer<MediaRange> action) {
		for (MediaRange r : ranges)
			action.accept(r);
		return this;
	}

	private static HeaderElement[] parse(String value) {
		return BasicHeaderValueParser.parseElements(emptyIfNull(trim(value)), null);
	}

	@Override /* Object */
	public String toString() {
		return string;
	}
}
