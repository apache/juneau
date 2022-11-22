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
 * Represents a parsed <l>Range</l> HTTP request header.
 *
 * <p>
 * Request only part of an entity. Bytes are numbered from 0.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Range: bytes=500-999
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * Since all HTTP entities are represented in HTTP messages as sequences of bytes, the concept of a byte range is
 * meaningful for any HTTP entity.
 * (However, not all clients and servers need to support byte- range operations.)
 *
 * <p>
 * Byte range specifications in HTTP apply to the sequence of bytes in the entity-body (not necessarily the same as the
 * message-body).
 *
 * <p>
 * A byte range operation MAY specify a single range of bytes, or a set of ranges within a single entity.
 * <p class='bcode'>
 * 	ranges-specifier = byte-ranges-specifier
 * 	byte-ranges-specifier = bytes-unit "=" byte-range-set
 * 	byte-range-set  = 1#( byte-range-spec | suffix-byte-range-spec )
 * 	byte-range-spec = first-byte-pos "-" [last-byte-pos]
 * 	first-byte-pos  = 1*DIGIT
 * 	last-byte-pos   = 1*DIGIT
 * </p>
 *
 * <p>
 * The first-byte-pos value in a byte-range-spec gives the byte-offset of the first byte in a range.
 * The last-byte-pos value gives the byte-offset of the last byte in the range; that is, the byte positions specified
 * are inclusive.
 * Byte offsets start at zero.
 *
 * <p>
 * If the last-byte-pos value is present, it MUST be greater than or equal to the first-byte-pos in that
 * byte-range-spec, or the byte- range-spec is syntactically invalid.
 * The recipient of a byte-range- set that includes one or more syntactically invalid byte-range-spec values MUST
 * ignore the header field that includes that byte-range-set.
 *
 * <p>
 * If the last-byte-pos value is absent, or if the value is greater than or equal to the current length of the
 * entity-body, last-byte-pos is taken to be equal to one less than the current length of the entity-body in bytes.
 *
 * <p>
 * By its choice of last-byte-pos, a client can limit the number of bytes retrieved without knowing the size of the
 * entity.
 * <p class='bcode'>
 * 	suffix-byte-range-spec = "-" suffix-length
 * 	suffix-length = 1*DIGIT
 * </p>
 *
 * <p>
 * A suffix-byte-range-spec is used to specify the suffix of the entity-body, of a length given by the suffix-length
 * value.
 * (That is, this form specifies the last N bytes of an entity-body.)
 * If the entity is shorter than the specified suffix-length, the entire entity-body is used.
 *
 * <p>
 * If a syntactically valid byte-range-set includes at least one byte- range-spec whose first-byte-pos is less than the
 * current length of the entity-body, or at least one suffix-byte-range-spec with a non-zero suffix-length, then the
 * byte-range-set is satisfiable.
 * Otherwise, the byte-range-set is unsatisfiable.
 * If the byte-range-set is unsatisfiable, the server SHOULD return a response with a status of 416 (Requested range
 * not satisfiable).
 * Otherwise, the server SHOULD return a response with a status of 206 (Partial Content) containing the satisfiable
 * ranges of the entity-body.
 *
 * <p>
 * Examples of byte-ranges-specifier values (assuming an entity-body of length 10000):
 * <p class='bcode'>
 * 	- The first 500 bytes (byte offsets 0-499, inclusive):  bytes=0-499
 * 	- The second 500 bytes (byte offsets 500-999, inclusive):  bytes=500-999
 * 	- The final 500 bytes (byte offsets 9500-9999, inclusive):  bytes=-500
 * 	- Or bytes=9500-
 * 	- The first and last bytes only (bytes 0 and 9999):  bytes=0-0,-1
 * 	- Several legal but not canonical specifications of the second 500 bytes (byte offsets 500-999, inclusive):
 * 	   bytes=500-600,601-999
 * 	   bytes=500-700,601-999
 * </p>
 *
 * <p>
 * HTTP retrieval requests using conditional or unconditional GET methods MAY request one or more sub-ranges of the
 * entity, instead of the entire entity, using the Range request header, which applies to the entity returned as the
 * result of the request:
 *
 * <p class='bcode'>
 * 	Range = "Range" ":" ranges-specifier
 * </p>
 *
 * <p>
 * A server MAY ignore the Range header.
 * However, HTTP/1.1 origin servers and intermediate caches ought to support byte ranges when possible, since Range
 * supports efficient recovery from partially failed transfers, and supports efficient partial retrieval of large
 * entities.
 *
 * <p>
 * If the server supports the Range header and the specified range or ranges are appropriate for the entity:
 * <ul>
 * 	<li>The presence of a Range header in an unconditional GET modifies what is returned if the GET is otherwise
 * 		successful.
 * 		In other words, the response carries a status code of 206 (Partial Content) instead of 200 (OK).
 * 	<li>The presence of a Range header in a conditional GET (a request using one or both of If-Modified-Since and
 * 		If-None-Match, or one or both of If-Unmodified-Since and If-Match) modifies what is returned if the GET is
 * 		otherwise successful and the condition is true. It does not affect the 304 (Not Modified) response returned if
 * 		the conditional is false.
 * </ul>
 *
 * <p>
 * In some cases, it might be more appropriate to use the If-Range header (see section 14.27) in addition to the Range
 * header.
 *
 * <p>
 * If a proxy that supports ranges receives a Range request, forwards the request to an inbound server, and receives an
 * entire entity in reply, it SHOULD only return the requested range to its client.
 * It SHOULD store the entire received response in its cache if that is consistent with its cache allocation policies.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("Range")
public class Range extends BasicStringHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "Range";

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static Range of(String value) {
		return value == null ? null : new Range(value);
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
	public static Range of(Supplier<String> value) {
		return value == null ? null : new Range(value);
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
	public Range(String value) {
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
	public Range(Supplier<String> value) {
		super(NAME, value);
	}
}
