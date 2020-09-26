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

import static org.apache.juneau.http.Constants.*;

import java.util.function.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Represents a parsed <l>Accept</l> HTTP request header.
 *
 * <p>
 * Content-Types that are acceptable for the response.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Accept: text/plain
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Accept request-header field can be used to specify certain media types which are acceptable for the response.
 * Accept headers can be used to indicate that the request is specifically limited to a small set of desired types, as
 * in the case of a request for an in-line image.
 *
 * <p class='bcode w800'>
 * 	 Accept         = "Accept" ":
 * 							#( media-range [ accept-params ] )
 *
 * 	 media-range    = ( "* /*"
 * 							| ( type "/" "*" )
 * 							| ( type "/" subtype )
 * 							) *( ";" parameter )
 * 	 accept-params  = ";" "q" "=" qvalue *( accept-extension )
 * 	 accept-extension = ";" token [ "=" ( token | quoted-string ) ]
 * </p>
 *
 * <p>
 * The asterisk "*" character is used to group media types into ranges, with "* /*" indicating all media types and
 * "type/*" indicating all subtypes of that type.
 * The media-range MAY include media type parameters that are applicable to that range.
 *
 * <p>
 * Each media-range MAY be followed by one or more accept-params, beginning with the "q" parameter for indicating a
 * relative quality factor.
 * The first "q" parameter (if any) separates the media-range parameter(s) from the accept-params.
 * Quality factors allow the user or user agent to indicate the relative degree of preference for that media-range,
 * using the qvalue scale from 0 to 1 (section 3.9).
 * The default value is q=1.
 *
 * <p>
 * Note: Use of the "q" parameter name to separate media type parameters from Accept extension parameters is due to
 * historical practice.
 * Although this prevents any media type parameter named "q" from being used with a media range, such an event is
 * believed to be unlikely given the lack of any "q" parameters in the IANA
 * media type registry and the rare usage of any media type parameters in Accept.
 * Future media types are discouraged from registering any parameter named "q".
 *
 * <p>
 * The example
 * <p class='bcode w800'>
 * 	Accept: audio/*; q=0.2, audio/basic
 * </p>
 * <p>
 * SHOULD be interpreted as "I prefer audio/basic, but send me any audio type if it is the best available after an 80%
 * mark-down in quality."
 *
 * <p>
 * If no Accept header field is present, then it is assumed that the client accepts all media types.
 *
 * <p>
 * If an Accept header field is present, and if the server cannot send a response which is acceptable according to the
 * combined Accept field value, then the server SHOULD send a 406 (not acceptable) response.
 *
 * <p>
 * A more elaborate example is
 * <p class='bcode w800'>
 * 	Accept: text/plain; q=0.5, text/html,
 * 	        text/x-dvi; q=0.8, text/x-c
 * </p>
 *
 * <p>
 * Verbally, this would be interpreted as "text/html and text/x-c are the preferred media types, but if they do not
 * exist, then send the
 * text/x-dvi entity, and if that does not exist, send the text/plain entity."
 *
 * <p>
 * Media ranges can be overridden by more specific media ranges or specific media types.
 * If more than one media range applies to a given type, the most specific reference has precedence.
 * For example,
 * <p class='bcode w800'>
 * 	Accept: text/ *, text/html, text/html;level=1, * /*
 * </p>
 * <p>
 * have the following precedence:
 * <ol>
 * 	<li>text/html;level=1
 * 	<li>text/html
 * 	<li>text/*
 * 	<li>* /*
 * </ol>
 *
 * <p>
 * The media type quality factor associated with a given type is determined by finding the media range with the highest
 * precedence which matches that type.
 * For example,
 * <p class='bcode w800'>
 * 	Accept: text/*;q=0.3, text/html;q=0.7, text/html;level=1,
 * 	        text/html;level=2;q=0.4, * /*;q=0.5
 * </p>
 * <p>
 * would cause the following values to be associated:
 * <p class='bcode w800'>
 * 	text/html;level=1         = 1
 * 	text/html                 = 0.7
 * 	text/plain                = 0.3
 * 	image/jpeg                = 0.5
 * 	text/html;level=2         = 0.4
 * 	text/html;level=3         = 0.7
 * </p>
 *
 * <p>
 * Note: A user agent might be provided with a default set of quality values for certain media ranges.
 * However, unless the user agent is a closed system which cannot interact with other rendering agents, this default
 * set ought to be configurable by the user.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc ExtRFC2616}
 * </ul>
 */
@Header("Accept")
public class Accept extends BasicMediaRangeArrayHeader {

	private static final long serialVersionUID = 1L;

	private static final Cache<String,Accept> CACHE = new Cache<>(NOCACHE, CACHE_MAX_SIZE);

	/**
	 * Returns a parsed and cached header.
	 *
	 * @param value
	 * 	The header value.
	 * @return A cached {@link AcceptCharset} object.
	 */
	public static Accept of(String value) {
		if (value == null)
			return null;
		Accept x = CACHE.get(value);
		if (x == null)
			x = CACHE.put(value, new Accept(value));
		return x;
	}

	/**
	 * Convenience creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> then parsed.
	 * 	</ul>
	 * @return A new {@link Accept} object.
	 */
	public static Accept of(Object value) {
		if (value == null)
			return null;
		return new Accept(value);
	}

	/**
	 * Convenience creator using supplier.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> then parsed.
	 * 	</ul>
	 * @return A new {@link Accept} object.
	 */
	public static Accept of(Supplier<?> value) {
		if (value == null)
			return null;
		return new Accept(value);
	}

	/**
	 * Constructor
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> then parsed.
	 * 		<li>A {@link Supplier} of anything on this list.
	 * 	</ul>
	 */
	public Accept(Object value) {
		super("Accept", value);
	}

	/**
	 * Constructor
	 *
	 * @param value
	 * 	The header value.
	 */
	public Accept(String value) {
		this((Object)value);
	}
}
