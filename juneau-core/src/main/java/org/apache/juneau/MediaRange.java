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

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Describes a single type used in content negotiation between an HTTP client and server, as described in
 * Section 14.1 and 14.7 of RFC2616 (the HTTP/1.1 specification).
 */
@BeanIgnore
public final class MediaRange implements Comparable<MediaRange>  {

	private static final MediaRange[] DEFAULT = new MediaRange[]{new MediaRange("*/*")};

	private final MediaType mediaType;
	private final Float qValue;
	private final Map<String,Set<String>> parameters, extensions;

	/**
	 * Parses a media range fragement of an <code>Accept</code> header value into a single media range object..
	 * <p>
	 * The syntax expected to be found in the referenced <code>value</code> complies with the syntax described in RFC2616, Section 14.1, as described below:
	 * <p class='bcode'>
	 * 	media-range    = ( "*\/*"
	 * 	                  | ( type "/" "*" )
	 * 	                  | ( type "/" subtype )
	 * 	                  ) *( ";" parameter )
	 * 	accept-params  = ";" "q" "=" qvalue *( accept-extension )
	 * 	accept-extension = ";" token [ "=" ( token | quoted-string ) ]
	 * </p>
	 * @param mediaRangeFragment The media range fragement string.
	 */
	private MediaRange(String mediaRangeFragment) {

		String r = mediaRangeFragment;
		Float _qValue = 1f;
		MediaType _mediaType = null;
		Map<String,Set<String>> _parameters = null;
		Map<String,Set<String>> _extensions = null;

		r = r.trim();

		int i = r.indexOf(';');

		if (i == -1) {
			_mediaType = MediaType.forString(r);

		} else {

			_mediaType = MediaType.forString(r.substring(0, i));

			String[] tokens = r.substring(i+1).split(";");

			// Only the type of the range is specified
			if (tokens.length > 0) {

				boolean isInExtensions = false;
				for (int j = 0; j < tokens.length; j++) {
					String[] parm = tokens[j].split("=");
					if (parm.length == 2) {
						String k = parm[0], v = parm[1];
						if (isInExtensions) {
							if (_extensions == null)
								_extensions = new TreeMap<String,Set<String>>();
							if (! _extensions.containsKey(parm[0]))
								_extensions.put(parm[0], new TreeSet<String>());
							_extensions.get(parm[0]).add(parm[1]);
						} else if (k.equals("q")) {
							_qValue = new Float(v);
							isInExtensions = true;
						} else /*(! isInExtensions)*/ {
							if (_parameters == null)
								_parameters = new TreeMap<String,Set<String>>();
							if (! _parameters.containsKey(parm[0]))
								_parameters.put(parm[0], new TreeSet<String>());
							_parameters.get(parm[0]).add(parm[1]);
						}
					}
				}
			}
		}
		if (_parameters == null)
			_parameters = Collections.emptyMap();
		if (_extensions == null)
			_extensions = Collections.emptyMap();

		this.mediaType = _mediaType;
		this.parameters = _parameters;
		this.qValue = _qValue;
		this.extensions = _extensions;
	}

	/**
	 * Returns the media type enclosed by this media range.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <ul>
	 * 	<li><js>"text/html"</js>
	 * 	<li><js>"text/*"</js>
	 * 	<li><js>"*\/*"</js>
	 * </ul>
	 *
	 * @return The media type of this media range, lowercased, never <jk>null</jk>.
	 */
	public MediaType getMediaType() {
		return mediaType;
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
		StringBuffer sb = new StringBuffer().append(mediaType);

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
			&& mediaType.equals(o2.mediaType)
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
		return mediaType.hashCode();
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
	 * The ranges are sorted such that the most acceptable media is available at ordinal position <js>'0'</js>, and the least acceptable at position n-1.
	 */
	public static MediaRange[] parse(String value) {

		if (value == null || value.length() == 0)
			return DEFAULT;

		value = value.toLowerCase(Locale.ENGLISH);

		if (value.indexOf(',') == -1)
			return new MediaRange[]{new MediaRange(value)};

		Set<MediaRange> ranges = new TreeSet<MediaRange>();

		for (String r : StringUtils.split(value, ',')) {
			r = r.trim();

			if (r.isEmpty())
				continue;

			ranges.add(new MediaRange(r));
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
		int i = o.mediaType.toString().compareTo(mediaType.toString());
		return i;
	}

	/**
	 * Matches the specified media type against this range and returns a q-value
	 * between 0 and 1 indicating the quality of the match.
	 *
	 * @param o The media type to match against.
	 * @return A float between 0 and 1.  1 is a perfect match.  0 is no match at all.
	 */
	public float matches(MediaType o) {
		if (this.mediaType == o || mediaType.matches(o))
			return qValue;
		return 0;
	}
}
