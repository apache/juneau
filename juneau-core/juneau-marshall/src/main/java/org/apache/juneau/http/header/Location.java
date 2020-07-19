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
 * Represents a parsed <l>Location</l> HTTP response header.
 *
 * <p>
 * Used in redirection, or when a new resource has been created.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
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
 * <p class='bcode w800'>
 * 	Location       = "Location" ":" absoluteURI
 * </p>
 *
 * <p>
 * An example is:
 * <p class='bcode w800'>
 * 	Location: http://www.w3.org/pub/WWW/People.html
 * </p>
 *
 * <p>
 * Note: The Content-Location header field (section 14.14) differs from Location in that the Content-Location identifies
 * the original location of the entity enclosed in the request.
 * It is therefore possible for a response to contain header fields for both Location and Content-Location.
 * Also see section 13.10 for cache requirements of some methods.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc RFC2616}
 * </ul>
 */
@Header("Location")
public class Location extends BasicUriHeader {

	private static final long serialVersionUID = 1L;

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
	 * @return A new {@link Location} object.
	 */
	public static Location of(Object value) {
		if (value == null)
			return null;
		return new Location(value);
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
	 * @return A new {@link Location} object.
	 */
	public static Location of(Supplier<?> value) {
		if (value == null)
			return null;
		return new Location(value);
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
	public Location(Object value) {
		super("Location", value);
	}

	/**
	 * Constructor
	 *
	 * @param value
	 * 	The header value.
	 */
	public Location(String value) {
		this((Object)value);
	}
}
