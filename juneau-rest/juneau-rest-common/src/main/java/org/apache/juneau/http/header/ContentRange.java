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
 * Represents a parsed <l>Content-Range</l> HTTP response header.
 *
 * <p>
 * Where in a full body message this partial message belongs.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode'>
 * 	Content-Range: bytes 21010-47021/47022
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Content-Range entity-header is sent with a partial entity-body to specify where in the full entity-body the
 * partial body should be applied.
 * Range units are defined in section 3.12.
 * <p class='bcode'>
 * 	Content-Range = "Content-Range" ":" content-range-spec
 * 	content-range-spec      = byte-content-range-spec
 * 	byte-content-range-spec = bytes-unit SP
 * 	                          byte-range-resp-spec "/"
 * 	                          ( instance-length | "*" )
 * 	byte-range-resp-spec = (first-byte-pos "-" last-byte-pos)
 * 	                               | "*"
 * 	instance-length           = 1*DIGIT
 * </p>
 *
 * <p>
 * The header SHOULD indicate the total length of the full entity-body, unless this length is unknown or difficult to
 * determine.
 * The asterisk "*" character means that the instance-length is unknown at the time when the response was generated.
 *
 * <p>
 * Unlike byte-ranges-specifier values (see section 14.35.1), a byte- range-resp-spec MUST only specify one range, and
 * MUST contain absolute byte positions for both the first and last byte of the range.
 *
 * <p>
 * A byte-content-range-spec with a byte-range-resp-spec whose last- byte-pos value is less than its first-byte-pos
 * value, or whose instance-length value is less than or equal to its last-byte-pos value, is invalid.
 * The recipient of an invalid byte-content-range- spec MUST ignore it and any content transferred along with it.
 *
 * <p>
 * A server sending a response with status code 416 (Requested range not satisfiable) SHOULD include a Content-Range
 * field with a byte-range- resp-spec of "*".
 * The instance-length specifies the current length of the selected resource.
 * A response with status code 206 (Partial Content) MUST NOT include a Content-Range field with a byte-range-resp-spec
 * of "*".
 *
 * <p>
 * Examples of byte-content-range-spec values, assuming that the entity contains a total of 1234 bytes:
 * <p class='bcode'>
 * 	The first 500 bytes:
 * 	 bytes 0-499/1234
 * 	The second 500 bytes:
 * 	 bytes 500-999/1234
 * 	All except for the first 500 bytes:
 * 	 bytes 500-1233/1234
 * 	The last 500 bytes:
 * 	 bytes 734-1233/1234
 * </p>
 *
 * <p>
 * When an HTTP message includes the content of a single range (for example, a response to a request for a single range,
 * or to a request for a set of ranges that overlap without any holes), this content is transmitted with a Content-Range
 * header, and a Content-Length header showing the number of bytes actually transferred.
 * For example:
 * <p class='bcode'>
 * 	HTTP/1.1 206 Partial content
 * 	Date: Wed, 15 Nov 1995 06:25:24 GMT
 * 	Last-Modified: Wed, 15 Nov 1995 04:58:08 GMT
 * 	Content-Range: bytes 21010-47021/47022
 * 	Content-Length: 26012
 * 	Content-Type: image/gif
 * </p>
 *
 * <p>
 * When an HTTP message includes the content of multiple ranges (for example, a response to a request for multiple
 * non-overlapping ranges), these are transmitted as a multipart message.
 * The multipart media type used for this purpose is "multipart/byteranges" as defined in appendix 19.2.
 * See appendix 19.6.3 for a compatibility issue.
 *
 * <p>
 * A response to a request for a single range MUST NOT be sent using the multipart/byteranges media type.
 * A response to a request for multiple ranges, whose result is a single range, MAY be sent as a multipart/byteranges
 * media type with one part.
 * A client that cannot decode a multipart/byteranges message MUST NOT ask for multiple byte-ranges in a single request.
 *
 * <p>
 * When a client requests multiple byte-ranges in one request, the server SHOULD return them in the order that they
 * appeared in the request.
 *
 * <p>
 * If the server ignores a byte-range-spec because it is syntactically invalid, the server SHOULD treat the request as
 * if the invalid Range header field did not exist.
 * (Normally, this means return a 200 response containing the full entity).
 *
 * <p>
 * If the server receives a request (other than one including an If- Range request-header field) with an unsatisfiable
 * Range request- header field
 * (that is, all of whose byte-range-spec values have a first-byte-pos value greater than the current length of the
 * selected resource),
 * it SHOULD return a response code of 416 (Requested range not satisfiable) (section 10.4.17).
 *
 * <p>
 * Note: clients cannot depend on servers to send a 416 (Requested range not satisfiable) response instead of a 200 (OK)
 * response for
 * an unsatisfiable Range request-header, since not all servers implement this request-header.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * 	<li class='extlink'><a class="doclink" href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 *
 * @serial exclude
 */
@Header("Content-Range")
public class ContentRange extends BasicStringHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;
	private static final String NAME = "Content-Range";

	/**
	 * Static creator.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 */
	public static ContentRange of(String value) {
		return value == null ? null : new ContentRange(value);
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
	public static ContentRange of(Supplier<String> value) {
		return value == null ? null : new ContentRange(value);
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
	public ContentRange(String value) {
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
	public ContentRange(Supplier<String> value) {
		super(NAME, value);
	}
}
