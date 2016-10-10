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

import java.util.*;
import java.util.Map.*;

/**
 * Describes a single type used in content negotiation between an HTTP client and server, as described in
 * Section 14.1 and 14.7 of RFC2616 (the HTTP/1.1 specification).
 */
public final class MediaRange implements Comparable<MediaRange>  {

	private final String type;								// The media type (e.g. "text" for Accept, "utf-8" for Accept-Charset)
	private final String subType;                   // The media sub-type (e.g. "json" for Accept, not used for Accept-Charset)
	private final Float qValue;
	private final Map<String,Set<String>> parameters, extensions;

	/**
	 * Returns the media type enclosed by this media range.
	 *
	 * <h6 class='topic'>Examples:</h6>
	 * <ul>
	 * 	<li><js>"text/html"</js>
	 * 	<li><js>"text/*"</js>
	 * 	<li><js>"*\/*"</js>
	 * </ul>
	 *
	 * @return The media type of this media range, lowercased, never <jk>null</jk>.
	 */
	public String getMediaType() {
		return type + "/" + subType;
	}

	/**
	 * Return just the type portion of this media range.
	 *
	 * @return The type portion of this media range.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Returns the <js>'q'</js> (quality) value for this type, as described in Section 3.9 of RFC2616.
	 * <p>
	 * The quality value is a float between <code>0.0</code> (unacceptable) and <code>1.0</code> (most acceptable).
	 * <p>
	 * If 'q' value doesn't make sense for the context (e.g. this range was extracted from a <js>"content-*"</js> header, as opposed to <js>"accept-*"</js>
	 * header, its value will always be <js>"1"</js>.
	 *
	 * @return The 'q' value for this type, never <jk>null</jk>.
	 */
	public Float getQValue() {
		return qValue;
	}

	/**
	 * Returns the optional set of parameters associated to the type as returned by {@link #getMediaType()}.
	 * <p>
	 * The parameters are those values as described in standardized MIME syntax.
	 * An example of such a parameter in string form might be <js>"level=1"</js>.
	 * <p>
	 * Values are lowercase and never <jk>null</jk>.
	 *
	 * @return The optional list of parameters, never <jk>null</jk>.
	 */
	public Map<String,Set<String>> getParameters() {
		return parameters;
	}

	/**
	 * Returns the optional set of custom extensions defined for this type.
	 * <p>
	 * Values are lowercase and never <jk>null</jk>.
	 *
	 * @return The optional list of extensions, never <jk>null</jk>.
	 */
	public Map<String,Set<String>> getExtensions() {
		return extensions;
	}

	/**
	 * Provides a string representation of this media range, suitable for use as an <code>Accept</code> header value.
	 * <p>
	 * The literal text generated will be all lowercase.
	 *
	 * @return A media range suitable for use as an Accept header value, never <code>null</code>.
	 */
	@Override /* Object */
	public String toString() {
		StringBuffer sb = new StringBuffer().append(type).append('/').append(subType);

		if (! parameters.isEmpty())
			for (Entry<String,Set<String>> e : parameters.entrySet()) {
				String k = e.getKey();
				for (String v : e.getValue())
					sb.append(';').append(k).append('=').append(v);
			}

		// '1' is equivalent to specifying no qValue. If there's no extensions, then we won't include a qValue.
		if (qValue.floatValue() == 1.0) {
			if (! extensions.isEmpty()) {
				sb.append(";q=").append(qValue);
				for (Entry<String,Set<String>> e : extensions.entrySet()) {
					String k = e.getKey();
					for (String v : e.getValue())
						sb.append(';').append(k).append('=').append(v);
				}
			}
		} else {
			sb.append(";q=").append(qValue);
			for (Entry<String,Set<String>> e : extensions.entrySet()) {
				String k = e.getKey();
				for (String v : e.getValue())
					sb.append(';').append(k).append('=').append(v);
			}
		}
		return sb.toString();
	}

	/**
	 * Returns <jk>true</jk> if the specified object is also a <code>MediaType</code>, and has the same qValue, type, parameters, and extensions.
	 *
	 * @return <jk>true</jk> if object is equivalent.
	 */
	@Override /* Object */
	public boolean equals(Object o) {

		if (o == null || !(o instanceof MediaRange))
			return false;

		if (this == o)
			return true;

		MediaRange o2 = (MediaRange) o;
		return qValue.equals(o2.qValue)
			&& type.equals(o2.type)
			&& subType.equals(o2.subType)
			&& parameters.equals(o2.parameters)
			&& extensions.equals(o2.extensions);
	}

	/**
	 * Returns a hash based on this instance's <code>media-type</code>.
	 *
	 * @return A hash based on this instance's <code>media-type</code>.
	 */
	@Override /* Object */
	public int hashCode() {
		return type.hashCode() + subType.hashCode();
	}

	/**
	 * Creates a <code>MediaRange</code> object with the referenced values.
	 *
	 * @param type The MIME type of this media range (e.g. <js>"application"</js> in <js>"application/json"</js>)
	 * @param subType The MIME subtype of this media range (e.g. <js>"json"</js> in <js>"application/json"</js>).
	 * @param parameters The optional parameters for this range.
	 * @param qValue The quality value of this range.  Must be between <code>0</code> and <code>1.0</code>.
	 * @param extensions The optional extensions to this quality value.
	 */
	private MediaRange(String type, String subType, Map<String,Set<String>> parameters, Float qValue, Map<String,Set<String>> extensions) {
		this.type = type;
		this.subType = subType;
		this.parameters = (parameters == null ? new TreeMap<String,Set<String>>() : parameters);
		this.extensions = (extensions == null ? new TreeMap<String,Set<String>>() : extensions);
		this.qValue = qValue;
	}

	/**
	 * Parses an <code>Accept</code> header value into an array of media ranges.
	 * <p>
	 * The returned media ranges are sorted such that the most acceptable media is available at ordinal position <js>'0'</js>, and the least acceptable at position n-1.
	 * <p>
	 * The syntax expected to be found in the referenced <code>value</code> complies with the syntax described in RFC2616, Section 14.1, as described below:
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
	 * This method can also be used on other headers such as <code>Accept-Charset</code> and <code>Accept-Encoding</code>...
	 * <p class='bcode'>
	 * 	Accept-Charset = "Accept-Charset" ":"
	 * 	1#( ( charset | "*" )[ ";" "q" "=" qvalue ] )
	 * </p>
	 *
	 * @param value The value to parse.  If <jk>null</jk> or empty, returns a single <code>MediaRange</code> is returned that represents all types.
	 * @return The media ranges described by the string.
	 * 	The ranges are sorted such that the most acceptable media is available at ordinal position <js>'0'</js>, and the least acceptable at position n-1.
	 */
	public static MediaRange[] parse(String value) {

		Set<MediaRange> ranges = new TreeSet<MediaRange>();

		if (value == null || value.length() == 0)
			return new MediaRange[]{new MediaRange("*", "*", null, 1f, null)};

		value = value.toLowerCase(Locale.ENGLISH);

		for (String r : value.trim().split("\\s*,\\s*")) {
			r = r.trim();

			if (r.isEmpty())
				continue;

			String[] tokens = r.split("\\s*;\\s*");

			tokens[0] = tokens[0].replace(' ', '+');

			// There is at least a type.
			String[] t = tokens[0].split("/");
			String type = t[0], subType = (t.length == 1 ? "*" : t[1]);

			// Only the type of the range is specified
			if (tokens.length == 1) {
				ranges.add(new MediaRange(type, subType, null, 1f, null));
				continue;
			}

			Float qValue = 1f;
			Map<String,Set<String>> params = new TreeMap<String,Set<String>>();
			Map<String,Set<String>> exts = new TreeMap<String,Set<String>>();

			boolean isInExtensions = false;
			for (int i = 1; i < tokens.length; i++) {
				String[] parm = tokens[i].split("\\s*=\\s*");
				if (parm.length == 2) {
					String k = parm[0], v = parm[1];
					if (isInExtensions) {
						if (! exts.containsKey(parm[0]))
							exts.put(parm[0], new TreeSet<String>());
						exts.get(parm[0]).add(parm[1]);
					} else if (k.equals("q")) {
						qValue = new Float(v);
						isInExtensions = true;
					} else /*(! isInExtensions)*/ {
						if (! params.containsKey(parm[0]))
							params.put(parm[0], new TreeSet<String>());
						params.get(parm[0]).add(parm[1]);
					}
				}
			}

			ranges.add(new MediaRange(type, subType, params, qValue, exts));
		}

		return ranges.toArray(new MediaRange[ranges.size()]);
	}

	/**
	 * Compares two MediaRanges for equality.
	 * <p>
	 * The values are first compared according to <code>qValue</code> values.
	 * Should those values be equal, the <code>type</code> is then lexicographically compared (case-insensitive) in ascending order,
	 * 	with the <js>"*"</js> type demoted last in that order.
	 * <code>MediaRanges</code> with the same type but different sub-types are compared - a more specific subtype is
	 * 	promoted over the 'wildcard' subtype.
	 * <code>MediaRanges</code> with the same types but with extensions are promoted over those same types with no extensions.
	 *
	 * @param o The range to compare to.  Never <jk>null</jk>.
	 */
	@Override /* Comparable */
	public int compareTo(MediaRange o) {

		// Compare q-values.
		int qCompare = Float.compare(o.qValue, qValue);
		if (qCompare != 0)
			return qCompare;

		// Compare media-types.
		// Note that '*' comes alphabetically before letters, so just do a reverse-alphabetical comparison.
		int i = o.type.compareTo(type);
		if (i == 0)
			i = o.subType.compareTo(subType);
		return i;
	}

	/**
	 * Returns <jk>true</jk> if the specified <code>MediaRange</code> matches this range.
	 * <p>
	 * This implies the types and subtypes are the same as or encompasses the other (e.g. <js>'application/xml'</js> and <js>'application/*'</js>).
	 *
	 * @param o The other media rage.
	 * @return <jk>true</jk> if the media ranges are the same or one encompasses the other.
	 */
	public boolean matches(MediaRange o) {
		if (this == o)
			return true;

		if (qValue == 0 || o.qValue == 0)
			return false;

		if (type.equals(o.type) || (type.equals("*")) || (o.type.equals("*")))
			if (subType.equals(o.subType) || subType.equals("*") || o.subType.equals("*"))
				return true;

		return false;
	}
}
