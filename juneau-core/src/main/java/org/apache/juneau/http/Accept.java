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

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.internal.*;

/**
 * Represents a parsed <code>Accept:</code> HTTP header.
 * <p>
 * The formal RFC2616 header field definition is as follows:
 * <p class='bcode'>
 * 	14.1 Accept
 *
 * 	The Accept request-header field can be used to specify certain media
 * 	types which are acceptable for the response. Accept headers can be
 * 	used to indicate that the request is specifically limited to a small
 * 	set of desired types, as in the case of a request for an in-line
 * 	image.
 *
 * 	 Accept         = "Accept" ":
 * 							#( media-range [ accept-params ] )
 *
 * 	 media-range    = ( "* /*"
 * 							| ( type "/" "*" )
 * 							| ( type "/" subtype )
 * 							) *( ";" parameter )
 * 	 accept-params  = ";" "q" "=" qvalue *( accept-extension )
 * 	 accept-extension = ";" token [ "=" ( token | quoted-string ) ]
 *
 * 	The asterisk "*" character is used to group media types into ranges,
 * 	with "* /*" indicating all media types and "type/*" indicating all
 * 	subtypes of that type. The media-range MAY include media type
 * 	parameters that are applicable to that range.
 *
 * 	Each media-range MAY be followed by one or more accept-params,
 * 	beginning with the "q" parameter for indicating a relative quality
 * 	factor. The first "q" parameter (if any) separates the media-range
 * 	parameter(s) from the accept-params. Quality factors allow the user
 * 	or user agent to indicate the relative degree of preference for that
 * 	media-range, using the qvalue scale from 0 to 1 (section 3.9). The
 * 	default value is q=1.
 *
 * 	Note: Use of the "q" parameter name to separate media type
 * 	parameters from Accept extension parameters is due to historical
 * 	practice. Although this prevents any media type parameter named
 * 	"q" from being used with a media range, such an event is believed
 * 	to be unlikely given the lack of any "q" parameters in the IANA
 * 	media type registry and the rare usage of any media type
 * 	parameters in Accept. Future media types are discouraged from
 * 	registering any parameter named "q".
 *
 * 	The example
 *
 * 		Accept: audio/*; q=0.2, audio/basic
 *
 * 	SHOULD be interpreted as "I prefer audio/basic, but send me any audio
 * 	type if it is the best available after an 80% mark-down in quality."
 *
 * 	If no Accept header field is present, then it is assumed that the
 * 	client accepts all media types. If an Accept header field is present,
 * 	and if the server cannot send a response which is acceptable
 * 	according to the combined Accept field value, then the server SHOULD
 * 	send a 406 (not acceptable) response.
 *
 * 	A more elaborate example is
 *
 * 	    Accept: text/plain; q=0.5, text/html,
 * 	            text/x-dvi; q=0.8, text/x-c
 *
 * 	Verbally, this would be interpreted as "text/html and text/x-c are
 * 	the preferred media types, but if they do not exist, then send the
 * 	text/x-dvi entity, and if that does not exist, send the text/plain
 * 	entity."
 *
 * 	Media ranges can be overridden by more specific media ranges or
 * 	specific media types. If more than one media range applies to a given
 * 	type, the most specific reference has precedence. For example,
 *
 * 	    Accept: text/ *, text/html, text/html;level=1, * /*
 *
 * 	have the following precedence:
 *
 * 	    1) text/html;level=1
 * 	    2) text/html
 * 	    3) text/*
 * 	    4) * /*
 *
 * 	The media type quality factor associated with a given type is
 * 	determined by finding the media range with the highest precedence
 * 	which matches that type. For example,
 *
 * 	    Accept: text/*;q=0.3, text/html;q=0.7, text/html;level=1,
 * 	            text/html;level=2;q=0.4, * /*;q=0.5
 *
 * 	would cause the following values to be associated:
 *
 * 	    text/html;level=1         = 1
 * 	    text/html                 = 0.7
 * 	    text/plain                = 0.3
 * 	    image/jpeg                = 0.5
 * 	    text/html;level=2         = 0.4
 * 	    text/html;level=3         = 0.7
 *
 * 	   Note: A user agent might be provided with a default set of quality
 * 	   values for certain media ranges. However, unless the user agent is
 * 	   a closed system which cannot interact with other rendering agents,
 * 	   this default set ought to be configurable by the user.
 * </p>
 */
public final class Accept {

	private static final boolean nocache = Boolean.getBoolean("juneau.nocache");
	private static final ConcurrentHashMap<String,Accept> cache = new ConcurrentHashMap<String,Accept>();

	private final MediaTypeRange[] mediaRanges;
	private final List<MediaTypeRange> mediaRangesList;

	/**
	 * Returns a parsed <code>Accept</code> header.
	 *
	 * @param s The <code>Accept</code> header string.
	 * @return The parsed <code>Accept</code> header.
	 */
	public static Accept forString(String s) {
		if (s == null)
			s = "null";
		Accept a = cache.get(s);
		if (a == null) {
			a = new Accept(s);
			if (nocache)
				return a;
			cache.putIfAbsent(s, a);
		}
		return cache.get(s);
	}

	private Accept(String raw) {
		this.mediaRanges = MediaTypeRange.parse(raw);
		this.mediaRangesList = Collections.unmodifiableList(Arrays.asList(mediaRanges));
	}

	/**
	 * Returns the list of the media ranges that make up this header.
	 * <p>
	 * The media ranges in the list are sorted by their q-value in descending order.
	 *
	 * @return An unmodifiable list of media ranges.
	 */
	public List<MediaTypeRange> getMediaRanges() {
		return mediaRangesList;
	}

	/**
	 * Given a list of media types, returns the best match for this <code>Accept</code> header.
	 * <p>
	 * Note that fuzzy matching is allowed on the media types where the <code>Accept</code> header may
	 * contain additional subtype parts.
	 * <br>For example, given identical q-values and an <code>Accept</code> value of <js>"text/json+activity"</js>,
	 * the media type <js>"text/json"</js> will match if <js>"text/json+activity"</js> or <js>"text/activity+json"</js>
	 * isn't found.
	 * <br>The purpose for this is to allow serializers to match when artifacts such as <code>id</code> properties are present
	 * in the header.
	 * <p>
	 * See <a class='doclink' href='https://www.w3.org/TR/activitypub/#retrieving-objects'>ActivityPub / Retrieving Objects</a>
	 * <p>
	 *
	 * @param mediaTypes The media types to match against.
	 * @return The index into the array of the best match, or <code>-1</code> if no suitable matches could be found.
	 */
	public int findMatch(MediaType[] mediaTypes) {
		int matchQuant = 0, matchIndex = -1;
		float q = 0f;

		// Media ranges are ordered by 'q'.
		// So we only need to search until we've found a match.
		for (MediaTypeRange mr : mediaRanges) {
			float q2 = mr.getQValue();

			if (q2 < q || q2 == 0)
				break;

			for (int i = 0; i < mediaTypes.length; i++) {
				MediaType mt = mediaTypes[i];
				int matchQuant2 = mt.match(mr.getMediaType());
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
	 * Convenience method for searching through all of the subtypes of all the media ranges in this header
	 * for the presence of a subtype fragment.
	 * <p>
	 * For example, given the header <js>"text/json+activity"</js>, calling <code>hasSubtypePart(<js>"activity"</js>)</code> returns <jk>true</jk>.
	 *
	 * @param part The media type subtype fragment.
	 * @return <jk>true</jk> if subtype fragement exists.
	 */
	public boolean hasSubtypePart(String part) {

		for (MediaTypeRange mr : this.mediaRanges)
			if (mr.getQValue() > 0 && mr.getMediaType().getSubTypes().indexOf(part) >= 0)
				return true;

		return false;
	}

	@Override /* Object */
	public String toString() {
		return StringUtils.join(mediaRanges, ',');
	}
}
