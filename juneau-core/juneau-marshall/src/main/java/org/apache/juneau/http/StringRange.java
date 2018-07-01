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

import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;
import java.util.Map.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Represents a single value in a comma-delimited header value that optionally contains a quality metric for
 * comparison and extension parameters.
 *
 * <p>
 * Similar in concept to {@link MediaTypeRange} except instead of media types (e.g. <js>"text/json"</js>),
 * it's a simple type (e.g. <js>"iso-8601"</js>).
 *
 * <p>
 * An example of a type range is a value in an <code>Accept-Encoding</code> header.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='extlink'><a class='doclink' href='https://www.w3.org/Protocols/rfc2616/rfc2616.html'>Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
@BeanIgnore
public final class StringRange implements Comparable<StringRange>  {

	private static final StringRange[] DEFAULT = new StringRange[]{new StringRange("*")};

	private final String type;
	private final Float qValue;
	private final Map<String,Set<String>> extensions;

	/**
	 * Parses a header such as an <code>Accept-Encoding</code> header value into an array of type ranges.
	 *
	 * <p>
	 * The syntax expected to be found in the referenced <code>value</code> complies with the syntax described in
	 * RFC2616, Section 14.1, as described below:
	 * <p class='bcode'>
	 * 	Accept-Encoding  = "Accept-Encoding" ":"
	 * 	                   1#( codings [ ";" "q" "=" qvalue ] )
	 * 	codings          = ( content-coding | "*" )
	 * </p>
	 *
	 * <p>
	 * Examples of its use are:
	 * <p class='bcode'>
	 * 	Accept-Encoding: compress, gzip
	 * 	Accept-Encoding:
	 * 	Accept-Encoding: *
	 * 	Accept-Encoding: compress;q=0.5, gzip;q=1.0
	 * 	Accept-Encoding: gzip;q=1.0, identity; q=0.5, *;q=0
	 * </p>
	 *
	 * @param value
	 * 	The value to parse.
	 * 	If <jk>null</jk> or empty, returns a single <code>TypeRange</code> is returned that represents all types.
	 * @return
	 * 	The type ranges described by the string.
	 * 	<br>The ranges are sorted such that the most acceptable type is available at ordinal position <js>'0'</js>, and
	 * 	the least acceptable at position n-1.
	 */
	public static StringRange[] parse(String value) {

		if (value == null || value.length() == 0)
			return DEFAULT;

		if (value.indexOf(',') == -1)
			return new StringRange[]{new StringRange(value)};

		Set<StringRange> ranges = new TreeSet<>();

		for (String r : StringUtils.split(value)) {
			r = r.trim();

			if (r.isEmpty())
				continue;

			ranges.add(new StringRange(r));
		}

		return ranges.toArray(new StringRange[ranges.size()]);
	}

	private StringRange(String token) {
		Builder b = new Builder(token);
		this.type = b.type;
		this.qValue = b.qValue;
		this.extensions = unmodifiableMap(b.extensions);
	}

	static final class Builder {
		String type;
		Float qValue = 1f;
		Map<String,Set<String>> extensions;

		Builder(String token) {

			token = token.trim();

			int i = token.indexOf(";q=");

			if (i == -1) {
				type = token;
				return;
			}

			type = token.substring(0, i);

			String[] tokens = token.substring(i+1).split(";");

			// Only the type of the range is specified
			if (tokens.length > 0) {
				boolean isInExtensions = false;
				for (int j = 0; j < tokens.length; j++) {
					String[] parm = tokens[j].split("=");
					if (parm.length == 2) {
						String k = parm[0], v = parm[1];
						if (isInExtensions) {
							if (extensions == null)
								extensions = new TreeMap<>();
							if (! extensions.containsKey(k))
								extensions.put(k, new TreeSet<String>());
							extensions.get(k).add(v);
						} else if (k.equals("q")) {
							qValue = new Float(v);
							isInExtensions = true;
						}
					}
				}
			}
		}
	}

	/**
	 * Returns the type enclosed by this type range.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <ul>
	 * 	<li><js>"compress"</js>
	 * 	<li><js>"gzip"</js>
	 * 	<li><js>"*"</js>
	 * </ul>
	 *
	 * @return The type of this type range, lowercased, never <jk>null</jk>.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Returns the <js>'q'</js> (quality) value for this type, as described in Section 3.9 of RFC2616.
	 *
	 * <p>
	 * The quality value is a float between <code>0.0</code> (unacceptable) and <code>1.0</code> (most acceptable).
	 *
	 * <p>
	 * If 'q' value doesn't make sense for the context (e.g. this range was extracted from a <js>"content-*"</js>
	 * header, as opposed to <js>"accept-*"</js> header, its value will always be <js>"1"</js>.
	 *
	 * @return The 'q' value for this type, never <jk>null</jk>.
	 */
	public Float getQValue() {
		return qValue;
	}

	/**
	 * Returns the optional set of custom extensions defined for this type.
	 *
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
	 *
	 * <p>
	 * The literal text generated will be all lowercase.
	 *
	 * @return A media range suitable for use as an Accept header value, never <code>null</code>.
	 */
	@Override /* Object */
	public String toString() {
		StringBuffer sb = new StringBuffer().append(type);

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
	 * Returns <jk>true</jk> if the specified object is also a <code>MediaType</code>, and has the same qValue, type,
	 * parameters, and extensions.
	 *
	 * @return <jk>true</jk> if object is equivalent.
	 */
	@Override /* Object */
	public boolean equals(Object o) {

		if (o == null || !(o instanceof StringRange))
			return false;

		if (this == o)
			return true;

		StringRange o2 = (StringRange) o;
		return qValue.equals(o2.qValue)
			&& type.equals(o2.type)
			&& extensions.equals(o2.extensions);
	}

	/**
	 * Returns a hash based on this instance's <code>media-type</code>.
	 *
	 * @return A hash based on this instance's <code>media-type</code>.
	 */
	@Override /* Object */
	public int hashCode() {
		return type.hashCode();
	}

	/**
	 * Compares two MediaRanges for equality.
	 *
	 * <p>
	 * The values are first compared according to <code>qValue</code> values.
	 * Should those values be equal, the <code>type</code> is then lexicographically compared (case-insensitive) in
	 * ascending order, with the <js>"*"</js> type demoted last in that order.
	 * <code>TypeRanges</code> with the same types but with extensions are promoted over those same types with no
	 * extensions.
	 *
	 * @param o The range to compare to.  Never <jk>null</jk>.
	 */
	@Override /* Comparable */
	public int compareTo(StringRange o) {

		// Compare q-values.
		int qCompare = Float.compare(o.qValue, qValue);
		if (qCompare != 0)
			return qCompare;

		// Compare media-types.
		// Note that '*' comes alphabetically before letters, so just do a reverse-alphabetical comparison.
		int i = o.type.toString().compareTo(type.toString());
		return i;
	}

	/**
	 * Checks if the specified type matches this range.
	 *
	 * <p>
	 * The type will match this range if the range type string is the same or <js>"*"</js>.
	 *
	 * @param type The type to match against this range.
	 * @return <jk>true</jk> if the specified type matches this range.
	 */
	public boolean matches(String type) {
		if (qValue == 0)
			return false;
		return this.type.equals(type) || this.type.equals("*");
	}
}
