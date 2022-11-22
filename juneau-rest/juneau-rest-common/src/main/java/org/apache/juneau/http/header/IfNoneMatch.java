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
 * Represents a parsed <l>If-None-Match</l> HTTP request header.
 *
 * <p>
 * Allows a 304 Not Modified to be returned if content is unchanged.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	If-None-Match: "737060cd8c284d8af7ad3082f209582d"
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The If-None-Match request-header field is used with a method to make it conditional.
 * A client that has one or more entities previously obtained from the resource can verify that none of those entities
 * is current by including a list of their associated entity tags in the If-None-Match header field.
 * The purpose of this feature is to allow efficient updates of cached information with a minimum amount of transaction
 * overhead.
 * It is also used to prevent a method (e.g. PUT) from inadvertently modifying an existing resource when the client
 * believes that the resource does not exist.
 *
 * <p>
 * As a special case, the value "*" matches any current entity of the resource.
 *
 * <p class='bcode'>
 * 	If-None-Match = "If-None-Match" ":" ( "*" | 1#entity-tag )
 * </p>
 *
 * <p>
 * If any of the entity tags match the entity tag of the entity that would have been returned in the response to a
 * similar GET request (without the If-None-Match header) on that resource, or if "*" is given
 * and any current entity exists for that resource, then the server MUST NOT perform the requested method, unless
 * required to do so because the resource's modification date fails to match that supplied in an If-Modified-Since
 * header field in the request.
 * Instead, if the request method was GET or HEAD, the server SHOULD respond with a 304 (Not Modified) response,
 * including the cache- related header fields (particularly ETag) of one of the entities that matched.
 * For all other request methods, the server MUST respond with a status of 412 (Precondition Failed).
 *
 * <p>
 * See section 13.3.3 for rules on how to determine if two entities tags match.
 * The weak comparison function can only be used with GET or HEAD requests.
 *
 * <p>
 * If none of the entity tags match, then the server MAY perform the requested method as if the If-None-Match header
 * field did not exist, but MUST also ignore any If-Modified-Since header field(s) in the request.
 * That is, if no entity tags match, then the server MUST NOT return a 304 (Not Modified) response.
 *
 * <p>
 * If the request would, without the If-None-Match header field, result in anything other than a 2xx or 304 status,
 * then the If-None-Match header MUST be ignored.
 * (See section 13.3.4 for a discussion of server behavior when both If-Modified-Since and If-None-Match appear in the
 * same request.)
 *
 * <p>
 * The meaning of "If-None-Match: *" is that the method MUST NOT be performed if the representation selected by the
 * origin server (or by a cache, possibly using the Vary mechanism, see section 14.44) exists, and SHOULD be performed
 * if the representation does not exist.
 * This feature is intended to be useful in preventing races between PUT operations.
 *
 * <p>
 * Examples:
 * <p class='bcode'>
 * 	If-None-Match: "xyzzy"
 * 	If-None-Match: W/"xyzzy"
 * 	If-None-Match: "xyzzy", "r2d2xxxx", "c3piozzzz"
 * 	If-None-Match: W/"xyzzy", W/"r2d2xxxx", W/"c3piozzzz"
 * 	If-None-Match: *
 * </p>
 *
 * <p>
 * The result of a request having both an If-None-Match header field and either an If-Match or an If-Unmodified-Since
 * header fields is undefined by this specification.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("If-None-Match")
public class IfNoneMatch extends BasicEntityTagsHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "If-None-Match";

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be a comma-delimited list of entity validator values (e.g. <js>"\"xyzzy\", \"r2d2xxxx\", \"c3piozzzz\""</js>).
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static IfNoneMatch of(String value) {
		return value == null ? null : new IfNoneMatch(value);
	}

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static IfNoneMatch of(EntityTags value) {
		return value == null ? null : new IfNoneMatch(value);
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
	public static IfNoneMatch of(Supplier<EntityTags> value) {
		return value == null ? null : new IfNoneMatch(value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be a comma-delimited list of entity validator values (e.g. <js>"\"xyzzy\", \"r2d2xxxx\", \"c3piozzzz\""</js>).
	 * 	<br>Can be <jk>null</jk>.
	 */
	public IfNoneMatch(String value) {
		super(NAME, value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public IfNoneMatch(EntityTags value) {
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
	public IfNoneMatch(Supplier<EntityTags> value) {
		super(NAME, value);
	}
}
