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

import static org.apache.juneau.internal.StringUtils.*;
import static java.util.Optional.*;

import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.json.*;

/**
 * Category of headers that consist of a single parameterized string value.
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Content-Type: application/json;charset=utf-8
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc TODO}
 * 	<li class='extlink'>{@doc ext.RFC2616}
 * 	<li class='extlink'>{@source}
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
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static BasicMediaTypeHeader of(String name, String value) {
		return value == null || isEmpty(name) ? null : new BasicMediaTypeHeader(name, value);
	}

	/**
	 * Static creator.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the name is <jk>null</jk> or empty or the value is <jk>null</jk>.
	 */
	public static BasicMediaTypeHeader of(String name, MediaType value) {
		return value == null || isEmpty(name) ? null : new BasicMediaTypeHeader(name, value);
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
	 */
	public BasicMediaTypeHeader(String name, MediaType value) {
		super(name, serialize(value));
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
	 */
	public BasicMediaTypeHeader(String name, Supplier<MediaType> value) {
		super(name, (String)null);
		this.value = null;
		this.supplier = value;
	}

	@Override /* Header */
	public String getValue() {
		if (supplier != null)
			return serialize(supplier.get());
		return super.getValue();
	}

	/**
	 * Returns this header as a {@link MediaType} object.
	 *
	 * @return This header as a {@link MediaType} object, or {@link Optional#empty()} if the value is <jk>null</jk>
	 */
	public Optional<MediaType> asMediaType() {
		if (supplier != null)
			return ofNullable(supplier.get());
		return ofNullable(value);
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
			int matchQuant2 = mt.match(asMediaType().orElse(MediaType.EMPTY), true);
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
		return asMediaType().orElse(MediaType.EMPTY).getType();
	}

	/**
	 * Returns the <js>'subType'</js> fragment of the <js>'type/subType'</js> string.
	 *
	 * @return The media subtype.
	 */
	public final String getSubType() {
		return asMediaType().orElse(MediaType.EMPTY).getSubType();
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
		return asMediaType().orElse(MediaType.EMPTY).hasSubType(st);
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
		return asMediaType().orElse(MediaType.EMPTY).getSubTypes();
	}

	/**
	 * Returns <jk>true</jk> if this media type contains the <js>'*'</js> meta character.
	 *
	 * @return <jk>true</jk> if this media type contains the <js>'*'</js> meta character.
	 */
	public final boolean isMetaSubtype() {
		return asMediaType().orElse(MediaType.EMPTY).isMetaSubtype();
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
	 * 			<li>We want to make sure {@link org.apache.juneau.json.SimpleJsonSerializer} (<js>"text/json+simple"</js>) does not handle
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
		return asMediaType().orElse(MediaType.EMPTY).match(o, allowExtraSubTypes);
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
		return asMediaType().orElse(MediaType.EMPTY).getParameters();
	}

	/**
	 * Returns a parameterized value of the header.
	 *
	 * <p class='bcode w800'>
	 * 	ContentType ct = ContentType.<jsm>of</jsm>(<js>"application/json;charset=foo"</js>);
	 * 	assertEquals(<js>"foo"</js>, ct.getParameter(<js>"charset"</js>);
	 * </p>
	 *
	 * @param name The header name.
	 * @return The header value, or <jk>null</jk> if the parameter is not present.
	 */
	public String getParameter(String name) {
		return asMediaType().orElse(MediaType.EMPTY).getParameter(name);
	}

	private static String serialize(MediaType value) {
		return stringify(value);
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
}
