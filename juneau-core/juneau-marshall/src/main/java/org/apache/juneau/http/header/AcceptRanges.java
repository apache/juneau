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
 * Represents a parsed <l>Accept-Range</l> HTTP response header.
 *
 * <p>
 * What partial content range types this server supports via byte serving.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Accept-Ranges: bytes
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Accept-Ranges response-header field allows the server to indicate its acceptance of range requests for a
 * resource:
 * <p class='bcode w800'>
 * 	Accept-Ranges     = "Accept-Ranges" ":" acceptable-ranges
 * 	acceptable-ranges = 1#range-unit | "none"
 * </p>
 *
 * <p>
 * Origin servers that accept byte-range requests MAY send...
 * <p class='bcode w800'>
 * 	Accept-Ranges: bytes
 * </p>
 * <p>
 * ...but are not required to do so.
 *
 * <p>
 * Clients MAY generate byte-range requests without having received this header for the resource involved.
 *
 * <p>
 * Range units are defined in section 3.12.
 *
 * <p>
 * Servers that do not accept any kind of range request for a resource MAY send...
 * <p class='bcode w800'>
 * 	Accept-Ranges: none
 * </p>
 * <p>
 * ...to advise the client not to attempt a range request.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc RFC2616}
 * </ul>
 */
@Header("Accept-Ranges")
public class AcceptRanges extends BasicStringHeader {

	private static final long serialVersionUID = 1L;

	/**
	 * Convenience creator.
	 *
	 * @param value
	 * 	The parameter value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> then parsed.
	 * 	</ul>
	 * @return A new {@link AcceptRanges} object.
	 */
	public static AcceptRanges of(Object value) {
		if (value == null)
			return null;
		return new AcceptRanges(value);
	}

	/**
	 * Convenience creator using supplier.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param value
	 * 	The parameter value supplier.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> then parsed.
	 * 	</ul>
	 * @return A new {@link AcceptRanges} object.
	 */
	public static AcceptRanges of(Supplier<?> value) {
		if (value == null)
			return null;
		return new AcceptRanges(value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The parameter value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> then parsed.
	 * 		<li>A {@link Supplier} of anything on this list.
	 * 	</ul>
	 */
	public AcceptRanges(Object value) {
		super("Accept-Ranges", value);
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The parameter value.
	 */
	public AcceptRanges(String value) {
		super("Accept-Ranges", value);
	}
}
