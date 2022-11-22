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
 * Represents a parsed <l>ETag</l> HTTP response header.
 *
 * <p>
 * An identifier for a specific version of a resource, often a message digest.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	ETag: "737060cd8c284d8af7ad3082f209582d"
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The ETag response-header field provides the current value of the entity tag for the requested variant.
 * The headers used with entity tags are described in sections 14.24, 14.26 and 14.44.
 * The entity tag MAY be used for comparison with other entities from the same resource (see section 13.3.3).
 *
 * <p class='bcode'>
 * 	ETag = "ETag" ":" entity-tag
 * </p>
 *
 * <p>
 * Examples:
 * <p class='bcode'>
 * 	ETag: "xyzzy"
 * 	ETag: W/"xyzzy"
 * 	ETag: ""
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("ETag")
public class ETag extends BasicEntityTagHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "ETag";

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be an entity tag value (e.g. <js>"\"xyzzy\""</js>).
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static ETag of(String value) {
		return value == null ? null : new ETag(value);
	}

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static ETag of(EntityTag value) {
		return value == null ? null : new ETag(value);
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
	public static ETag of(Supplier<EntityTag> value) {
		return value == null ? null : new ETag(value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be an entity tag value (e.g. <js>"\"xyzzy\""</js>).
	 * 	<br>Can be <jk>null</jk>.
	 */
	public ETag(String value) {
		super(NAME, value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public ETag(EntityTag value) {
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
	public ETag(Supplier<EntityTag> value) {
		super(NAME, value);
	}
}
