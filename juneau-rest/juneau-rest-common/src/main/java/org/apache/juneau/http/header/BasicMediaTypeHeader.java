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

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.json.*;

/**
 * Category of headers that consist of a single parameterized string value.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Content-Type: application/json;charset=utf-8
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
public class BasicMediaTypeHeader extends BasicStringHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;

	/**
	 * Static creator.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link MediaType#of(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public static BasicMediaTypeHeader of(String name, String value) {
		return value == null ? null : new BasicMediaTypeHeader(name, value);
	}

	/**
	 * Static creator.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public static BasicMediaTypeHeader of(String name, MediaType value) {
		return value == null ? null : new BasicMediaTypeHeader(name, value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final MediaType value;
	private final Supplier<MediaType> supplier;

	/**
	 * Constructor.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link MediaType#of(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public BasicMediaTypeHeader(String name, String value) {
		super(name, value);
		this.value = parse(value);
		this.supplier = null;
	}

	/**
	 * Constructor.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public BasicMediaTypeHeader(String name, MediaType value) {
		super(name, stringify(value));
		this.value = value;
		this.supplier = null;
	}

	/**
	 * Constructor with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public BasicMediaTypeHeader(String name, Supplier<MediaType> value) {
		super(name, (String)null);
		this.value = null;
		this.supplier = value;
	}

	@Override /* Header */
	public String getValue() {
		return stringify(value());
	}

	/**
	 * Returns the header value as a {@link MediaType} wrapped in an {@link Optional}.
	 *
	 * @return The header value as a {@link MediaType} wrapped in an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<MediaType> asMediaType() {
		return optional(value());
	}

	/**
	 * Returns the header value as a {@link MediaType}.
	 *
	 * @return The header value as a {@link MediaType}.  Can be <jk>null</jk>.
	 */
	public MediaType toMediaType() {
		return value();
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
			int matchQuant2 = mt.match(orElse(MediaType.EMPTY), true);
			if (matchQuant2 > matchQuant) {
				matchQuant = matchQuant2;
				matchIndex = i;
			}
		}
		return matchIndex;
	}

	/**
	 * Returns the <js>'type'</js> fragment of the <js>'type/subType'</js> string.
	 *
	 * @return The media type.
	 */
	public final String getType() {
		return orElse(MediaType.EMPTY).getType();
	}

	/**
	 * Returns the <js>'subType'</js> fragment of the <js>'type/subType'</js> string.
	 *
	 * @return The media subtype.
	 */
	public final String getSubType() {
		return orElse(MediaType.EMPTY).getSubType();
	}

	/**
	 * Returns <jk>true</jk> if the subtype contains the specified <js>'+'</js> delimited subtype value.
	 *
	 * @param value
	 * 	The subtype string.
	 * 	Case is ignored.
	 * @return <jk>true</jk> if the subtype contains the specified subtype string.
	 */
	public final boolean hasSubType(String value) {
		return orElse(MediaType.EMPTY).hasSubType(value);
	}

	/**
	 * Returns the subtypes broken down by fragments delimited by <js>"'"</js>.
	 *
	 * <P>
	 * For example, the media type <js>"text/foo+bar"</js> will return a list of
	 * <code>[<js>'foo'</js>,<js>'bar'</js>]</code>
	 *
	 * @return An unmodifiable list of subtype fragments.  Can be <jk>null</jk>.
	 */
	public final List<String> getSubTypes() {
		return orElse(MediaType.EMPTY).getSubTypes();
	}

	/**
	 * Returns <jk>true</jk> if this media type contains the <js>'*'</js> meta character.
	 *
	 * @return <jk>true</jk> if this media type contains the <js>'*'</js> meta character.
	 */
	public final boolean isMetaSubtype() {
		return orElse(MediaType.EMPTY).isMetaSubtype();
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
		return orElse(MediaType.EMPTY).match(o, allowExtraSubTypes);
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
		return orElse(MediaType.EMPTY).getParameters();
	}

	/**
	 * Returns a parameterized value of the header.
	 *
	 * <p class='bjava'>
	 * 	ContentType <jv>contentType</jv> = ContentType.<jsm>of</jsm>(<js>"application/json;charset=foo"</js>);
	 * 	<jsm>assertEquals</jsm>(<js>"foo"</js>, <jv>contentType</jv>.getParameter(<js>"charset"</js>);
	 * </p>
	 *
	 * @param name The header name.
	 * @return The header value, or <jk>null</jk> if the parameter is not present.
	 */
	public String getParameter(String name) {
		return orElse(MediaType.EMPTY).getParameter(name);
	}

	/**
	 * Return the value if present, otherwise return <c>other</c>.
	 *
	 * <p>
	 * This is a shortened form for calling <c>asMediaType().orElse(<jv>other</jv>)</c>.
	 *
	 * @param other The value to be returned if there is no value present, can be <jk>null</jk>.
	 * @return The value, if present, otherwise <c>other</c>.
	 */
	public MediaType orElse(MediaType other) {
		MediaType x = value();
		return x != null ? x : other;
	}

	private MediaType parse(String value) {
		// If this happens to be a multi-value, use the last value.
		if (value != null) {
			int i = value.indexOf(',');
			if (i != -1)
				value = value.substring(i+1);
		}
		return MediaType.of(value);
	}

	private MediaType value() {
		if (supplier != null)
			return supplier.get();
		return value;
	}
}
