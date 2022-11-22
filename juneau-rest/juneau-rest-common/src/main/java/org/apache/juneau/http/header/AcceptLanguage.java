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

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Represents a parsed <l>Accept-Language</l> HTTP request header.
 *
 * <p>
 * List of acceptable human languages for response.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Accept-Language: en-US
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Accept-Language request-header field is similar to Accept, but restricts the set of natural languages that are
 * preferred as a response to the request.
 * Language tags are defined in section 3.10.
 *
 * <p class='bcode'>
 * 	Accept-Language = "Accept-Language" ":"
 * 	                  1#( language-range [ ";" "q" "=" qvalue ] )
 * 	language-range  = ( ( 1*8ALPHA *( "-" 1*8ALPHA ) ) | "*" )
 * </p>
 *
 * <p>
 * Each language-range MAY be given an associated quality value which represents an estimate of the user's preference
 * for the languages specified by that range.
 * The quality value defaults to "q=1".
 * For example...
 * <p class='bcode'>
 * 	Accept-Language: da, en-gb;q=0.8, en;q=0.7
 * </p>
 * <p>
 * ...would mean: "I prefer Danish, but will accept British English and other types of English."
 *
 * <p>
 * A language-range matches a language-tag if it exactly equals the tag, or if it exactly equals a prefix of the tag
 * such that the first tag character following the prefix is "-".
 *
 * <p>
 * The special range "*", if present in the Accept-Language field, matches every tag not matched by any other range
 * present in the Accept-Language field.
 *
 * <p>
 * Note: This use of a prefix matching rule does not imply that language tags are assigned to languages in such a way
 * that it is always true that if a user understands a language with a certain
 * tag, then this user will also understand all languages with tags for which this tag is a prefix.
 * The prefix rule simply allows the use of prefix tags if this is the case.
 *
 * <p>
 * The language quality factor assigned to a language-tag by the Accept-Language field is the quality value of the
 * longest language- range in the field that matches the language-tag.
 *
 * <p>
 * If no language- range in the field matches the tag, the language quality factor assigned is 0.
 *
 * <p>
 * If no Accept-Language header is present in the request, the server SHOULD assume that all languages are equally
 * acceptable.
 *
 * <p>
 * If an Accept-Language header is present, then all languages which are assigned a quality factor greater than 0 are
 * acceptable.
 *
 * <p>
 * It might be contrary to the privacy expectations of the user to send an Accept-Language header with the complete
 * linguistic preferences of the user in every request.
 * For a discussion of this issue, see section 15.1.4.
 *
 * <p>
 * As intelligibility is highly dependent on the individual user, it is recommended that client applications make the
 * choice of linguistic preference available to the user.
 * If the choice is not made available, then the Accept-Language header field MUST NOT be given in the request.
 *
 * <p>
 * Note: When making the choice of linguistic preference available to the user, we remind implementors of the fact that
 * users are not familiar with the details of language matching as described above, and should provide appropriate
 * guidance.
 * As an example, users might assume that on selecting "en-gb", they will be served any kind of English document if
 * British English is not available.
 * A user agent might suggest in such a case to add "en" to get the best matching behavior.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("Accept-Language")
public class AcceptLanguage extends BasicStringRangesHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "Accept-Language";

	private static final Cache<String,AcceptLanguage> CACHE = Cache.of(String.class, AcceptLanguage.class).build();

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link StringRanges#of(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static AcceptLanguage of(String value) {
		return value == null ? null : CACHE.get(value, ()->new AcceptLanguage(value));
	}

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static AcceptLanguage of(StringRanges value) {
		return value == null ? null : new AcceptLanguage(value);
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
	public static AcceptLanguage of(Supplier<StringRanges> value) {
		return value == null ? null : new AcceptLanguage(value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link StringRanges#of(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public AcceptLanguage(String value) {
		super(NAME, value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public AcceptLanguage(StringRanges value) {
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
	public AcceptLanguage(Supplier<StringRanges> value) {
		super(NAME, value);
	}
}
