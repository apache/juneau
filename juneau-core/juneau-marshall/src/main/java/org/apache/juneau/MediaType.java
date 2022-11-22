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
import static org.apache.juneau.internal.ObjectUtils.*;

import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.http.message.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;


/**
 * Describes a single media type used in content negotiation between an HTTP client and server, as described in
 * Section 14.1 and 14.7 of RFC2616 (the HTTP/1.1 specification).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
@BeanIgnore
public class MediaType implements Comparable<MediaType>  {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Represents an empty media type object. */
	public static final MediaType EMPTY = new MediaType("/*");

	private static final Cache<String,MediaType> CACHE = Cache.of(String.class, MediaType.class).build();

	/** Reusable predefined media type */
	@SuppressWarnings("javadoc")
	public static final MediaType
		CSV = of("text/csv"),
		HTML = of("text/html"),
		JSON = of("application/json"),
		MSGPACK = of("octal/msgpack"),
		PLAIN = of("text/plain"),
		UON = of("text/uon"),
		URLENCODING = of("application/x-www-form-urlencoded"),
		XML = of("text/xml"),
		XMLSOAP = of("text/xml+soap"),

		RDF = of("text/xml+rdf"),
		RDFABBREV = of("text/xml+rdf+abbrev"),
		NTRIPLE = of("text/n-triple"),
		TURTLE = of("text/turtle"),
		N3 = of("text/n3")
	;

	/**
	 * Returns the media type for the specified string.
	 * The same media type strings always return the same objects so that these objects
	 * can be compared for equality using '=='.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		Spaces are replaced with <js>'+'</js> characters.
	 * 		This gets around the issue where passing media type strings with <js>'+'</js> as HTTP GET parameters
	 * 		get replaced with spaces by your browser.  Since spaces aren't supported by the spec, this
	 * 		is doesn't break anything.
	 * 	<li class='note'>
	 * 		Anything including and following the <js>';'</js> character is ignored (e.g. <js>";charset=X"</js>).
	 * </ul>
	 *
	 * @param value
	 * 	The media type string.
	 * 	Will be lowercased.
	 * 	Returns <jk>null</jk> if input is null or empty.
	 * @return A cached media type object.
	 */
	public static MediaType of(String value) {
		return value == null ? null : CACHE.get(value, ()->new MediaType(value));
	}

	/**
	 * Same as {@link #of(String)} but allows you to specify the parameters.
	 *
	 *
	 * @param value
	 * 	The media type string.
	 * 	Will be lowercased.
	 * 	Returns <jk>null</jk> if input is null or empty.
	 * @param parameters The media type parameters.  If <jk>null</jk>, they're pulled from the media type string.
	 * @return A new media type object, cached if parameters were not specified.
	 */
	public static MediaType of(String value, NameValuePair...parameters) {
		if (parameters.length == 0)
			return of(value);
		return isEmpty(value) ? null : new MediaType(value, parameters);
	}

	/**
	 * Same as {@link #of(String)} but allows you to construct an array of <c>MediaTypes</c> from an
	 * array of strings.
	 *
	 * @param values
	 * 	The media type strings.
	 * @return
	 * 	An array of <c>MediaType</c> objects.
	 * 	<br>Always the same length as the input string array.
	 */
	public static MediaType[] ofAll(String...values) {
		MediaType[] mt = new MediaType[values.length];
		for (int i = 0; i < values.length; i++)
			mt[i] = of(values[i]);
		return mt;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final String string;                          // The entire unparsed value.
	private final String mediaType;                      // The "type/subtype" portion of the media type..
	private final String type;                           // The media type (e.g. "text" for Accept, "utf-8" for Accept-Charset)
	private final String subType;                        // The media sub-type (e.g. "json" for Accept, not used for Accept-Charset)
	private final String[] subTypes;                     // The media sub-type (e.g. "json" for Accept, not used for Accept-Charset)
	private final String[] subTypesSorted;               // Same as subTypes, but sorted so that it can be used for comparison.
	private final boolean hasSubtypeMeta;                // The media subtype contains meta-character '*'.

	private final NameValuePair[] parameters;            // The media type parameters (e.g. "text/html;level=1").  Does not include q!

	/**
	 * Constructor.
	 *
	 * @param mt The media type string.
	 */
	public MediaType(String mt) {
		this(parse(mt));
	}

	/**
	 * Constructor.
	 *
	 * @param mt The media type string.
	 * @param parameters The media type parameters.  If <jk>null</jk>, they're pulled from the media type string.
	 */
	public MediaType(String mt, NameValuePair[] parameters) {
		this(parse(mt), parameters);
	}

	/**
	 * Constructor.
	 *
	 * @param e The parsed media type string.
	 */
	public MediaType(HeaderElement e) {
		this(e, null);
	}

	/**
	 * Constructor.
	 *
	 * @param e The parsed media type string.
	 * @param parameters Optional parameters.
	 */
	public MediaType(HeaderElement e, NameValuePair[] parameters) {
		mediaType = e.getName();

		if (parameters == null) {
			parameters = e.getParameters();
			for (int i = 0; i < parameters.length; i++) {
				if (parameters[i].getName().equals("q")) {
					parameters = Arrays.copyOfRange(parameters, 0, i);
					break;
				}
			}
		}
		for (int i = 0; i < parameters.length; i++)
			parameters[i] = new BasicNameValuePair(parameters[i].getName(), parameters[i].getValue());
		this.parameters = parameters;

		String x = mediaType.replace(' ', '+');
		int i = x.indexOf('/');
		type = (i == -1 ? x : x.substring(0, i));
		subType = (i == -1 ? "*" : x.substring(i+1));

		subTypes = StringUtils.split(subType, '+');
		subTypesSorted = Arrays.copyOf(subTypes, subTypes.length);
		Arrays.sort(this.subTypesSorted);
		hasSubtypeMeta = ArrayUtils.contains("*", this.subTypes);

		StringBuilder sb = new StringBuilder();
		sb.append(mediaType);
		for (NameValuePair p : parameters)
			sb.append(';').append(p.getName()).append('=').append(p.getValue());
		this.string = sb.toString();
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
		return ulist(subTypes);
	}

	/**
	 * Performs an action on the subtypes broken down by fragments delimited by <js>"'"</js>.
	 *
	 * @param action The action to perform.
	 * @return This object.
	 */
	public final MediaType forEachSubType(Consumer<String> action) {
		for (String s : subTypes)
			action.accept(s);
		return this;
	}

	/**
	 * Returns <jk>true</jk> if this media type subtype contains the <js>'*'</js> meta character.
	 *
	 * @return <jk>true</jk> if this media type subtype contains the <js>'*'</js> meta character.
	 */
	public final boolean isMetaSubtype() {
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
	 * 	<li>Exact matches (e.g. <js>"text/json"</js>/</js>"text/json"</js>) should match
	 * 		better than meta-character matches (e.g. <js>"text/*"</js>/</js>"text/json"</js>)
	 * 	<li>The comparison media type can have additional subtype tokens (e.g. <js>"text/json+foo"</js>)
	 * 		that will not prevent a match if the <c>allowExtraSubTypes</c> flag is set.
	 * 		The reverse is not true, e.g. the comparison media type must contain all subtype tokens found in the
	 * 		comparing media type.
	 * 		<ul>
	 * 			<li>We want the {@link JsonSerializer} (<js>"text/json"</js>) class to be able to handle requests for <js>"text/json+foo"</js>.
	 * 			<li>We want to make sure {@link org.apache.juneau.json.Json5Serializer} (<js>"text/json5"</js>) does not handle
	 * 				requests for <js>"text/json"</js>.
	 * 		</ul>
	 * 		More token matches should result in a higher match number.
	 * </ul>
	 *
	 * The formula is as follows for <c>type/subTypes</c>:
	 * <ul>
	 * 	<li>An exact match is <c>100,000</c>.
	 * 	<li>Add the following for type (assuming subtype match is &lt;0):
	 * 	<ul>
	 * 		<li><c>10,000</c> for an exact match (e.g. <js>"text"</js>==<js>"text"</js>).
	 * 		<li><c>5,000</c> for a meta match (e.g. <js>"*"</js>==<js>"text"</js>).
	 * 	</ul>
	 * 	<li>Add the following for subtype (assuming type match is &lt;0):
	 * 	<ul>
	 * 		<li><c>7,500</c> for an exact match (e.g. <js>"json+foo"</js>==<js>"json+foo"</js> or <js>"json+foo"</js>==<js>"foo+json"</js>)
	 * 		<li><c>100</c> for every subtype entry match (e.g. <js>"json"</js>/<js>"json+foo"</js>)
	 * 	</ul>
	 * </ul>
	 *
	 * @param o The media type to compare with.
	 * @param allowExtraSubTypes If <jk>true</jk>,
	 * @return <jk>true</jk> if the media types match.
	 */
	public final int match(MediaType o, boolean allowExtraSubTypes) {

		if (o == null)
			return -1;

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
				c += 0;
			else
				return 0;
		}
		for (String st2 : o.subTypes) {
			if ("*".equals(st2))
				c += 0;
			else if (ArrayUtils.contains(st2, subTypes))
				c += 100;
			else if (hasSubtypeMeta)
				c += 0;
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
	public List<NameValuePair> getParameters() {
		return ulist(parameters);
	}

	/**
	 * Performs an action on the additional parameters on this media type.
	 *
	 * @param action The action to perform.
	 * @return This object.
	 */
	public MediaType forEachParameter(Consumer<NameValuePair> action) {
		for (NameValuePair p : parameters)
			action.accept(p);
		return this;
	}

	/**
	 * Returns the additional parameter on this media type.
	 *
	 * @param name The additional parameter name.
	 * @return The parameter value, or <jk>null</jk> if not found.
	 */
	public String getParameter(String name) {
		for (NameValuePair p : parameters)
			if (eq(name, p.getName()))
				return p.getValue();
		return null;
	}

	/**
	 * Given a list of media types, returns the best match for this <c>Content-Type</c> header.
	 *
	 * <p>
	 * Note that fuzzy matching is allowed on the media types where the <c>Content-Types</c> header may
	 * contain additional subtype parts.
	 * <br>For example, given a <c>Content-Type</c> value of <js>"text/json+activity"</js>,
	 * the media type <js>"text/json"</js> will match if <js>"text/json+activity"</js> or <js>"text/activity+json"</js>
	 * isn't found.
	 * <br>The purpose for this is to allow parsers to match when artifacts such as <c>id</c> properties are
	 * present in the header.
	 *
	 * @param mediaTypes The media types to match against.
	 * @return The index into the array of the best match, or <c>-1</c> if no suitable matches could be found.
	 */
	public int match(List<MediaType> mediaTypes) {
		int matchQuant = 0, matchIndex = -1;

		for (int i = 0; i < mediaTypes.size(); i++) {
			MediaType mt = mediaTypes.get(i);
			int matchQuant2 = mt.match(this, true);
			if (matchQuant2 > matchQuant) {
				matchQuant = matchQuant2;
				matchIndex = i;
			}
		}
		return matchIndex;
	}

	private static HeaderElement parse(String value) {
		HeaderElement[] elements = BasicHeaderValueParser.parseElements(emptyIfNull(trim(value)), null);
		return (elements.length > 0 ? elements[0] : new BasicHeaderElement("", ""));
	}

	@Override /* Object */
	public String toString() {
		return string;
	}

	@Override /* Object */
	public int hashCode() {
		return string.hashCode();
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return (o instanceof MediaType) && eq(this, (MediaType)o, (x,y)->eq(x.string, y.string));
	}

	@Override
	public final int compareTo(MediaType o) {
		return toString().compareTo(o.toString());
	}
}
