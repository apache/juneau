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

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;


/**
 * Describes a single media type used in content negotiation between an HTTP client and server, as described in
 * Section 14.1 and 14.7 of RFC2616 (the HTTP/1.1 specification).
 *
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='jp'><a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.http</a>
 * 	<li class='extlink'><a class='doclink' href='https://www.w3.org/Protocols/rfc2616/rfc2616.html'>
 * 		Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
@BeanIgnore
@SuppressWarnings("unchecked")
public class MediaType implements Comparable<MediaType> {

	private static final boolean nocache = Boolean.getBoolean("juneau.nocache");
	private static final ConcurrentHashMap<String,MediaType> cache = new ConcurrentHashMap<String,MediaType>();

	/** Reusable predefined media type */
	@SuppressWarnings("javadoc")
	public static final MediaType
		CSV = forString("text/csv"),
		HTML = forString("text/html"),
		JSON = forString("application/json"),
		MSGPACK = forString("octal/msgpack"),
		PLAIN = forString("text/plain"),
		UON = forString("text/uon"),
		URLENCODING = forString("application/x-www-form-urlencoded"),
		XML = forString("text/xml"),
		XMLSOAP = forString("text/xml+soap"),

		RDF = forString("text/xml+rdf"),
		RDFABBREV = forString("text/xml+rdf+abbrev"),
		NTRIPLE = forString("text/n-triple"),
		TURTLE = forString("text/turtle"),
		N3 = forString("text/n3")
	;

	private final String mediaType;
	private final String type;								     // The media type (e.g. "text" for Accept, "utf-8" for Accept-Charset)
	private final String subType;                        // The media sub-type (e.g. "json" for Accept, not used for Accept-Charset)
	private final String[] subTypes;                     // The media sub-type (e.g. "json" for Accept, not used for Accept-Charset)
	private final List<String> subTypesList;             // The media sub-type (e.g. "json" for Accept, not used for Accept-Charset)
	private final Map<String,Set<String>> parameters;    // The media type parameters (e.g. "text/html;level=1").  Does not include q!


	/**
	 * Returns the media type for the specified string.
	 * The same media type strings always return the same objects so that these objects
	 * can be compared for equality using '=='.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>Spaces are replaced with <js>'+'</js> characters.
	 * 		This gets around the issue where passing media type strings with <js>'+'</js> as HTTP GET parameters
	 * 		get replaced with spaces by your browser.  Since spaces aren't supported by the spec, this
	 * 		is doesn't break anything.
	 * 	<li>Anything including and following the <js>';'</js> character is ignored (e.g. <js>";charset=X"</js>).
	 * </ul>
	 *
	 * @param s The media type string.  Will be lowercased.
	 * 	<br>Returns <jk>null</jk> if input is null.
	 * @return A cached media type object.
	 */
	public static MediaType forString(String s) {
		if (s == null)
			return null;
		MediaType mt = cache.get(s);
		if (mt == null) {
			mt = new MediaType(s);
			if (nocache)
				return mt;
			cache.putIfAbsent(s, mt);
		}
		return cache.get(s);
	}

	MediaType(String mt) {
		Builder b = new Builder(mt);
		this.mediaType = b.mediaType;
		this.type = b.type;
		this.subType = b.subType;
		this.subTypes = b.subTypes;
		this.subTypesList = Collections.unmodifiableList(Arrays.asList(subTypes));
		this.parameters = (b.parameters == null ? Collections.EMPTY_MAP : Collections.unmodifiableMap(b.parameters));
	}

	private static class Builder {
		private String mediaType, type, subType;
		private String[] subTypes;
		private Map<String,Set<String>> parameters;

		private Builder(String mt) {
			mt = mt.trim();

			int i = mt.indexOf(';');
			if (i == -1) {
				this.parameters = Collections.EMPTY_MAP;
			} else {
				this.parameters = new TreeMap<String,Set<String>>();
				String[] tokens = mt.substring(i+1).split(";");

				for (int j = 0; j < tokens.length; j++) {
					String[] parm = tokens[j].split("=");
					if (parm.length == 2) {
						String k = parm[0].trim(), v = parm[1].trim();
						if (! parameters.containsKey(k))
							parameters.put(k, new TreeSet<String>());
						parameters.get(k).add(v);
					}
				}

				mt = mt.substring(0, i);
			}

			this.mediaType = mt;
			if (mt != null) {
				mt = mt.replace(' ', '+');
				i = mt.indexOf('/');
				type = (i == -1 ? mt : mt.substring(0, i));
				subType = (i == -1 ? "*" : mt.substring(i+1));
			}
			this.subTypes = StringUtils.split(subType, '+');
		}
	}

	/**
	 * Returns the <js>'type'</js> fragment of the <js>'type/subType'</js> string.
	 *
	 * @return The media type.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Returns the <js>'subType'</js> fragment of the <js>'type/subType'</js> string.
	 *
	 * @return The media subtype.
	 */
	public String getSubType() {
		return subType;
	}

	/**
	 * Returns the subtypes broken down by fragments delimited by <js>"'"</js>.
	 * For example, the media type <js>"text/foo+bar"</js> will return a list of
	 * <code>[<js>'foo'</js>,<js>'bar'</js>]</code>
	 *
	 * @return An unmodifiable list of subtype fragments.  Never <jk>null</jk>.
	 */
	public List<String> getSubTypes() {
		return subTypesList;
	}

	/**
	 * Returns <jk>true</jk> if this media type is a match for the specified media type.
	 * <p>
	 * Matches if any of the following is true:
	 * <ul>
	 * 	<li>Both type and subtype are the same.
	 * 	<li>One or both types are <js>'*'</js> and the subtypes are the same.
	 * 	<li>One or both subtypes are <js>'*'</js> and the types are the same.
	 * 	<li>Either is <js>'*\/*'</js>.
	 * </ul>
	 *
	 * @param o The media type to compare with.
	 * @return <jk>true</jk> if the media types match.
	 */
	public final boolean matches(MediaType o) {
		return match(o) > 0;
	}

	/**
	 * Returns a match metric against the specified media type where a larger number represents a better match.
	 * <p>
	 * <ul>
	 * 	<li>Exact matches (e.g. <js>"text/json"<js>/</js>"text/json"</js>) should match
	 * 		better than meta-character matches (e.g. <js>"text/*"<js>/</js>"text/json"</js>)
	 * 	<li>The comparison media type can have additional subtype tokens (e.g. <js>"text/json+foo"</js>)
	 * 		that will not prevent a match.  The reverse is not true, e.g. the comparison media type
	 * 		must contain all subtype tokens found in the comparing media type.
	 * 		<ul>
	 * 			<li>We want the {@link JsonSerializer} (<js>"text/json"</js>) class to be able to handle requests for <js>"text/json+foo"</js>.
	 * 			<li>We want to make sure {@link org.apache.juneau.json.JsonSerializer.Simple} (<js>"text/json+simple"</js>) does not handle
	 * 				requests for <js>"text/json"</js>.
	 * 		</ul>
	 * 		More token matches should result in a higher match number.
	 * </ul>
	 *
	 * @param o The media type to compare with.
	 * @return <jk>true</jk> if the media types match.
	 */
	public final int match(MediaType o) {

		// Perfect match
		if (this == o || (type.equals(o.type) && subType.equals(o.subType)))
			return Integer.MAX_VALUE;

		int c1 = 0, c2 = 0;

		if (type.equals(o.type))
			c1 += 10000;
		else if ("*".equals(type) || "*".equals(o.type))
			c1 += 5000;

		if (c1 == 0)
			return 0;

		// Give type slightly higher comparison value than subtype simply for deterministic results.
		if (subType.equals(o.subType))
			return c1 + 9999;

		int c3 = 0;

		for (String st1 : subTypes) {
			if ("*".equals(st1))
				c1++;
			else if (ArrayUtils.contains(st1, o.subTypes))
				c1 += 100;
			else if (ArrayUtils.contains("*", o.subTypes))
				c1 += 10;
			else
				return 0;
		}

		return c1 + c2 + c3;
	}

	/**
	 * Returns the additional parameters on this media type.
	 * <p>
	 * For example, given the media type string <js>"text/html;level=1"</js>, will return a map
	 * with the single entry <code>{level:[<js>'1'</js>]}</code>.
	 *
	 * @return The map of additional parameters, or an empty map if there are no parameters.
	 */
	public Map<String,Set<String>> getParameters() {
		return parameters;
	}

	@Override /* Object */
	public String toString() {
		if (parameters.isEmpty())
			return mediaType;
		StringBuilder sb = new StringBuilder(mediaType);
		for (Map.Entry<String,Set<String>> e : parameters.entrySet())
			for (String value : e.getValue())
				sb.append(';').append(e.getKey()).append('=').append(value);
		return sb.toString();
	}

	@Override /* Object */
	public int hashCode() {
		return mediaType.hashCode();
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return this == o;
	}

	@Override
	public int compareTo(MediaType o) {
		return mediaType.compareTo(o.mediaType);
	}
}
