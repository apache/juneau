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

import java.util.function.*;

import org.apache.juneau.http.annotation.*;

/**
 * Represents a parsed <l>Warning</l> HTTP request/response header.
 *
 * <p>
 * A general warning about possible problems with the entity body.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Warning: 199 Miscellaneous warning
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Warning general-header field is used to carry additional information about the status or transformation of a
 * message which might not be reflected in the message.
 * This information is typically used to warn about a possible lack of semantic transparency from caching operations
 * or transformations applied to the entity body of the message.
 *
 * <p>
 * Warning headers are sent with responses using:
 * <p class='bcode'>
 * 	Warning    = "Warning" ":" 1#warning-value
 * 	warning-value = warn-code SP warn-agent SP warn-text
 * 	                                      [SP warn-date]
 * 	warn-code  = 3DIGIT
 * 	warn-agent = ( host [ ":" port ] ) | pseudonym
 * 	                ; the name or pseudonym of the server adding
 * 	                ; the Warning header, for use in debugging
 * 	warn-text  = quoted-string
 * 	warn-date  = &lt;"&gt; HTTP-date &lt;"&gt;
 * </p>
 *
 * <p>
 * A response MAY carry more than one Warning header.
 *
 * <p>
 * The warn-text SHOULD be in a natural language and character set that is most likely to be intelligible to the human
 * user receiving the response.
 * This decision MAY be based on any available knowledge, such as the location of the cache or user, the
 * Accept-Language field in a request, the Content-Language field in a response, etc.
 * The default language is English and the default character set is ISO-8859-1.
 *
 * <p>
 * If a character set other than ISO-8859-1 is used, it MUST be encoded in the warn-text using the method described in
 * RFC 2047.
 *
 * <p>
 * Warning headers can in general be applied to any message, however some specific warn-codes are specific to caches
 * and can only be applied to response messages.
 * New Warning headers SHOULD be added after any existing Warning headers.
 * A cache MUST NOT delete any Warning header that it received with a message.
 * However, if a cache successfully validates a cache entry, it SHOULD remove any Warning headers previously attached
 * to that entry except as specified for specific Warning codes.
 * It MUST then add any Warning headers received in the validating response.
 * In other words, Warning headers are those that would be attached to the most recent relevant response.
 *
 * <p>
 * When multiple Warning headers are attached to a response, the user agent ought to inform the user of as many of them
 * as possible, in the order that they appear in the response.
 * If it is not possible to inform the user of all of the warnings, the user agent SHOULD follow these heuristics:
 * <ul>
 * 	<li>Warnings that appear early in the response take priority over those appearing later in the response.
 * 	<li>Warnings in the user's preferred character set take priority over warnings in other character sets but with
 * identical warn-codes and warn-agents.
 * </ul>
 *
 * <p>
 * Systems that generate multiple Warning headers SHOULD order them with this user agent behavior in mind.
 *
 * <p>
 * Requirements for the behavior of caches with respect to Warnings are stated in section 13.1.2.
 *
 * <p>
 * This is a list of the currently-defined warn-codes, each with a recommended warn-text in English, and a description
 * of its meaning.
 * <ul>
 * 	<li>110 Response is stale MUST be included whenever the returned response is stale.
 * 	<li>111 Revalidation failed MUST be included if a cache returns a stale response because an attempt to revalidate
 * 		the response failed, due to an inability to reach the server.
 * 	<li>112 Disconnected operation SHOULD be included if the cache is intentionally disconnected from the rest of the
 * 		network for a period of time.
 * 	<li>113 Heuristic expiration MUST be included if the cache heuristically chose a freshness lifetime greater than
 * 		24 hours and the response's age is greater than 24 hours.
 * 	<li>199 Miscellaneous warning The warning text MAY include arbitrary information to be presented to a human user,
 * 		or logged. A system receiving this warning MUST NOT take any automated action, besides presenting the warning
 * 		to the user.
 * 	<li>214 Transformation applied MUST be added by an intermediate cache or proxy if it applies any transformation
 * 		changing the content-coding (as specified in the Content-Encoding header) or media-type (as specified in the
 * 		Content-Type header) of the response, or the entity-body of the response, unless this Warning code already
 * 		appears in the response.
 * 	<li>299 Miscellaneous persistent warning The warning text MAY include arbitrary information to be presented to a
 * 		human user, or logged. A system receiving this warning MUST NOT take any automated action.
 * </ul>
 *
 * <p>
 * If an implementation sends a message with one or more Warning headers whose version is HTTP/1.0 or lower, then the
 * sender MUST include in each warning-value a warn-date that matches the date in the response.
 *
 * <p>
 * If an implementation receives a message with a warning-value that includes a warn-date, and that warn-date is
 * different from the Date value in the response, then that warning-value MUST be deleted from the message before
 * storing, forwarding, or using it.
 * (This prevents bad consequences of naive caching of Warning header fields.)
 * If all of the warning-values are deleted for this reason, the Warning header MUST be deleted as well.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("Warning")
public class Warning extends BasicStringHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "Warning";

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static Warning of(String value) {
		return value == null ? null : new Warning(value);
	}

	/**
	 * Static creator with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static Warning of(Supplier<String> value) {
		return value == null ? null : new Warning(value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public Warning(String value) {
		super(NAME, value);
	}

	/**
	 * Constructor with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param value
	 * 	The supplier of the header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public Warning(Supplier<String> value) {
		super(NAME, value);
	}
}
