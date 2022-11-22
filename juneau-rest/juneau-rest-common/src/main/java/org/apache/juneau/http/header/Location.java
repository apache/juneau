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

import java.net.*;
import java.util.function.*;

import org.apache.juneau.http.annotation.*;

/**
 * Represents a parsed <l>Location</l> HTTP response header.
 *
 * <p>
 * Used in redirection, or when a new resource has been created.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Location: http://www.w3.org/pub/WWW/People.html
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Location response-header field is used to redirect the recipient to a location other than the Request-URI for
 * completion of the request or identification of a new resource.
 * For 201 (Created) responses, the Location is that of the new resource which was created by the request.
 * For 3xx responses, the location SHOULD indicate the server's preferred URI for automatic redirection to the resource.
 * The field value consists of a single absolute URI.
 *
 * <p class='bcode'>
 * 	Location       = "Location" ":" absoluteURI
 * </p>
 *
 * <p>
 * An example is:
 * <p class='bcode'>
 * 	Location: http://www.w3.org/pub/WWW/People.html
 * </p>
 *
 * <p>
 * Note: The Content-Location header field (section 14.14) differs from Location in that the Content-Location identifies
 * the original location of the entity enclosed in the request.
 * It is therefore possible for a response to contain header fields for both Location and Content-Location.
 * Also see section 13.10 for cache requirements of some methods.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("Location")
public class Location extends BasicUriHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "Location";

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link URI#create(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static Location of(String value) {
		return value == null ? null : new Location(value);
	}

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static Location of(URI value) {
		return value == null ? null : new Location(value);
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
	public static Location of(Supplier<URI> value) {
		return value == null ? null : new Location(value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link URI#create(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public Location(String value) {
		super(NAME, value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public Location(URI value) {
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
	public Location(Supplier<URI> value) {
		super(NAME, value);
	}
}
