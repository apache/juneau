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

import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;


/**
 * Describes a single media type used in content negotiation between an HTTP client and server, as described in
 * Section 14.1 and 14.7 of RFC2616 (the HTTP/1.1 specification).
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='extlink'><a class='doclink' href='https://www.w3.org/Protocols/rfc2616/rfc2616.html'>Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
@BeanIgnore
public class MediaType implements Comparable<MediaType> {

	private static final boolean nocache = Boolean.getBoolean("juneau.nocache");
	private static final ConcurrentHashMap<String,MediaType> cache = new ConcurrentHashMap<>();

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
	private final String[] subTypesSorted;               // Same as subTypes, but sorted so that it can be used for comparison.
	private final List<String> subTypesList;             // The media sub-type (e.g. "json" for Accept, not used for Accept-Charset)
	private final Map<String,Set<String>> parameters;    // The media type parameters (e.g. "text/html;level=1").  Does not include q!
	private final boolean hasSubtypeMeta;                // The media subtype contains meta-character '*'.

	/**
	 * Returns the media type for the specified string.
	 * The same media type strings always return the same objects so that these objects
	 * can be compared for equality using '=='.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Spaces are replaced with <js>'+'</js> characters.
	 * 		This gets around the issue where passing media type strings with <js>'+'</js> as HTTP GET parameters
	 * 		get replaced with spaces by your browser.  Since spaces aren't supported by the spec, this
	 * 		is doesn't break anything.
	 * 	<li>
	 * 		Anything including and following the <js>';'</js> character is ignored (e.g. <js>";charset=X"</js>).
	 * </ul>
	 * 
	 * @param s
	 * 	The media type string.
	 * 	Will be lowercased.
	 * 	Returns <jk>null</jk> if input is null or empty.
	 * @return A cached media type object.
	 */
	public static MediaType forString(String s) {
		if (isEmpty(s))
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

	/**
	 * Same as {@link #forString(String)} but allows you to construct an array of <code>MediaTypes</code> from an
	 * array of strings.
	 * 
	 * @param s
	 * 	The media type strings.
	 * @return
	 * 	An array of <code>MediaType</code> objects.
	 * 	<br>Always the same length as the input string array.
	 */
	public static MediaType[] forStrings(String...s) {
		MediaType[] mt = new MediaType[s.length];
		for (int i = 0; i < s.length; i++)
			mt[i] = forString(s[i]);
		return mt;
	}

	MediaType(String mt) {
		Builder b = new Builder(mt);
		this.mediaType = b.mediaType;
		this.type = b.type;
		this.subType = b.subType;
		this.subTypes = b.subTypes;
		this.subTypesSorted = b.subTypesSorted;
		this.subTypesList = Collections.unmodifiableList(Arrays.asList(subTypes));
		this.parameters = (b.parameters == null ? Collections.EMPTY_MAP : Collections.unmodifiableMap(b.parameters));
		this.hasSubtypeMeta = b.hasSubtypeMeta;
	}

	static final class Builder {
		String mediaType, type, subType;
		String[] subTypes, subTypesSorted;
		Map<String,Set<String>> parameters;
		boolean hasSubtypeMeta;

		Builder(String mt) {
			mt = mt.trim();

			int i = mt.indexOf(';');
			if (i == -1) {
				this.parameters = Collections.EMPTY_MAP;
			} else {
				this.parameters = new TreeMap<>();
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
			this.subTypesSorted = Arrays.copyOf(subTypes, subTypes.length);
			Arrays.sort(this.subTypesSorted);
			hasSubtypeMeta = ArrayUtils.contains("*", this.subTypes);
		}
	}

	/**
	 * Returns the <js>'type'</js> fragment of the <js>'type/subType'</js> string.
	 * 
	 * @return The media type.
	 */
	public final String getType() {
		return type;
	}

	/**
	 * Returns the <js>'subType'</js> fragment of the <js>'type/subType'</js> string.
	 * 
	 * @return The media subtype.
	 */
	public final String getSubType() {
		return subType;
	}

	/**
	 * Returns <jk>true</jk> if the subtype contains the specified <js>'+'</js> delimited subtype value.
	 * 
	 * @param st
	 * 	The subtype string.
	 * 	Case is ignored.
	 * @return <jk>true</jk> if the subtype contains the specified subtype string.
	 */
	public final boolean hasSubType(String st) {
		if (st != null)
			for (String s : subTypes)
				if (st.equalsIgnoreCase(s))
					return true;
		return false;
	}

	/**
	 * Returns the subtypes broken down by fragments delimited by <js>"'"</js>.
	 * 
	 * <P>
	 * For example, the media type <js>"text/foo+bar"</js> will return a list of
	 * <code>[<js>'foo'</js>,<js>'bar'</js>]</code>
	 * 
	 * @return An unmodifiable list of subtype fragments.  Never <jk>null</jk>.
	 */
	public final List<String> getSubTypes() {
		return subTypesList;
	}

	/**
	 * Returns <jk>true</jk> if this media type contains the <js>'*'</js> meta character.
	 * 
	 * @return <jk>true</jk> if this media type contains the <js>'*'</js> meta character.
	 */
	public final boolean isMeta() {
		return hasSubtypeMeta;
	}

	/**
	 * Returns a match metric against the specified media type where a larger number represents a better match.
	 * 
	 * <p>
	 * This media type can contain <js>'*'</js> metacharacters.
	 * <br>The comparison media type must not.
	 * 
	 * <ul>
	 * 	<li>Exact matches (e.g. <js>"text/json"<js>/</js>"text/json"</js>) should match
	 * 		better than meta-character matches (e.g. <js>"text/*"<js>/</js>"text/json"</js>)
	 * 	<li>The comparison media type can have additional subtype tokens (e.g. <js>"text/json+foo"</js>)
	 * 		that will not prevent a match if the <code>allowExtraSubTypes</code> flag is set.
	 * 		The reverse is not true, e.g. the comparison media type must contain all subtype tokens found in the
	 * 		comparing media type.
	 * 		<ul>
	 * 			<li>We want the {@link JsonSerializer} (<js>"text/json"</js>) class to be able to handle requests for <js>"text/json+foo"</js>.
	 * 			<li>We want to make sure {@link org.apache.juneau.json.JsonSerializer.Simple} (<js>"text/json+simple"</js>) does not handle
	 * 				requests for <js>"text/json"</js>.
	 * 		</ul>
	 * 		More token matches should result in a higher match number.
	 * </ul>
	 * 
	 * The formula is as follows for <code>type/subTypes</code>:
	 * <ul>
	 * 	<li>An exact match is <code>100,000</code>.
	 * 	<li>Add the following for type (assuming subtype match is &lt;0):
	 * 	<ul>
	 * 		<li><code>10,000</code> for an exact match (e.g. <js>"text"</js>==<js>"text"</js>).
	 * 		<li><code>5,000</code> for a meta match (e.g. <js>"*"</js>==<js>"text"</js>).
	 * 	</ul>
	 * 	<li>Add the following for subtype (assuming type match is &lt;0):
	 * 	<ul>
	 * 		<li><code>7,500</code> for an exact match (e.g. <js>"json+foo"</js>==<js>"json+foo"</js> or <js>"json+foo"</js>==<js>"foo+json"</js>)
	 * 		<li><code>100</code> for every subtype entry match (e.g. <js>"json"</js>/<js>"json+foo"</js>)
	 * 		<li><code>10</code> for a subtype entry meta match (e.g. <js>"*"</js>/<js>"json"</js> or <js>"json+*"</js>/<js>"json+foo"</js>)
	 * 	</ul>
	 * </ul>
	 * 
	 * @param o The media type to compare with.
	 * @param allowExtraSubTypes If <jk>true</jk>,
	 * @return <jk>true</jk> if the media types match.
	 */
	public final int match(MediaType o, boolean allowExtraSubTypes) {

		// Perfect match
		if (this == o || (type.equals(o.type) && subType.equals(o.subType)))
			return 100000;

		int c = 0;

		if (type.equals(o.type))
			c += 10000;
		else if ("*".equals(type) || "*".equals(o.type))
			c += 5000;

		if (c == 0)
			return 0;

		// Subtypes match but are ordered different
		if (ArrayUtils.equals(subTypesSorted, o.subTypesSorted))
			return c + 7500;

		for (String st1 : subTypes) {
			if ("*".equals(st1))
				c += 0;
			else if (ArrayUtils.contains(st1, o.subTypes))
				c += 100;
			else if (o.hasSubtypeMeta)
				c += 10;
			else
				return 0;
		}
		for (String st2 : o.subTypes) {
			if ("*".equals(st2))
				c += 0;
			else if (ArrayUtils.contains(st2, subTypes))
				c += 100;
			else if (hasSubtypeMeta)
				c += 10;
			else if (! allowExtraSubTypes)
				return 0;
			else
				c += 10;
		}

		return c;
	}

	/**
	 * Returns the additional parameters on this media type.
	 * 
	 * <p>
	 * For example, given the media type string <js>"text/html;level=1"</js>, will return a map
	 * with the single entry <code>{level:[<js>'1'</js>]}</code>.
	 * 
	 * @return The map of additional parameters, or an empty map if there are no parameters.
	 */
	public final Map<String,Set<String>> getParameters() {
		return parameters;
	}

	@Override /* Object */
	public final String toString() {
		if (parameters.isEmpty())
			return mediaType;
		StringBuilder sb = new StringBuilder(mediaType);
		for (Map.Entry<String,Set<String>> e : parameters.entrySet())
			for (String value : e.getValue())
				sb.append(';').append(e.getKey()).append('=').append(value);
		return sb.toString();
	}

	@Override /* Object */
	public final int hashCode() {
		return mediaType.hashCode();
	}

	@Override /* Object */
	public final boolean equals(Object o) {
		return this == o;
	}

	@Override
	public final int compareTo(MediaType o) {
		return mediaType.compareTo(o.mediaType);
	}
}
