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
 * Represents a parsed <l>Content-Location</l> HTTP response header.
 *
 * <p>
 * An alternate location for the returned data.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Content-Location: /index.htm
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Content-Location entity-header field MAY be used to supply the resource location for the entity enclosed in the
 * message when that entity is accessible from a location separate from the requested resource's URI.
 * A server SHOULD provide a Content-Location for the variant corresponding to the response entity; especially in the
 * case where a resource has multiple entities associated with it, and those entities actually have separate locations
 * by which they might be individually accessed, the server SHOULD provide a Content-Location for the particular variant
 * which is returned.
 * <p class='bcode'>
 * 	Content-Location = "Content-Location" ":"
 * 	                   ( absoluteURI | relativeURI )
 * </p>
 *
 * <p>
 * The value of Content-Location also defines the base URI for the entity.
 *
 * <p>
 * The Content-Location value is not a replacement for the original requested URI; it is only a statement of the
 * location of the resource corresponding to this particular entity at the time of the request.
 * Future requests MAY specify the Content-Location URI as the request- URI if the desire is to identify the source of
 * that particular entity.
 *
 * <p>
 * A cache cannot assume that an entity with a Content-Location different from the URI used to retrieve it can be used
 * to respond to later requests on that Content-Location URI.
 * However, the Content- Location can be used to differentiate between multiple entities retrieved from a single
 * requested resource, as described in section 13.6.
 *
 * <p>
 * If the Content-Location is a relative URI, the relative URI is interpreted relative to the Request-URI.
 *
 * <p>
 * The meaning of the Content-Location header in PUT or POST requests is undefined; servers are free to ignore it in
 * those cases.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("Content-Location")
public class ContentLocation extends BasicUriHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "Content-Location";

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Must be parsable by {@link URI#create(String)}.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static ContentLocation of(String value) {
		return value == null ? null : new ContentLocation(value);
	}

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static ContentLocation of(URI value) {
		return value == null ? null : new ContentLocation(value);
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
	public static ContentLocation of(Supplier<URI> value) {
		return value == null ? null : new ContentLocation(value);
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
	public ContentLocation(String value) {
		super(NAME, value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public ContentLocation(URI value) {
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
	public ContentLocation(Supplier<URI> value) {
		super(NAME, value);
	}
}
