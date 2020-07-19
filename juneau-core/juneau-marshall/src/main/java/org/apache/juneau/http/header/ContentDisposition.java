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

import static org.apache.juneau.http.Constants.*;

import java.util.function.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Represents a parsed <l>Content-Disposition</l> HTTP request header.
 *
 * <p>
 * In a regular HTTP response, the Content-Disposition response header is a header indicating if the content is expected
 * to be displayed inline in the browser, that is, as a Web page or as part of a Web page, or as an attachment, that is
 * downloaded and saved locally.
 *
 * <h5 class='figure'>Example</h5>
 * <p class='bcode w800'>
 * 	Content-Disposition: form-data; name="fieldName"; filename="filename.jpg"
 * </p>
 *
 * <h5 class='topic'>RFC2616 Specification</h5>
 *
 * The Expect request-header field is used to indicate that particular server behaviors are required by the client.
 * <p class='bcode w800'>
 *	content-disposition = "Content-Disposition" ":"
 *  	disposition-type *( ";" disposition-parm )
 * 	disposition-type = "attachment" | disp-extension-token
 * 	disposition-parm = filename-parm | disp-extension-parm
 * 	filename-parm = "filename" "=" quoted-string
 *	disp-extension-token = token
 * 	disp-extension-parm = token "=" ( token | quoted-string )
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@doc RFC2616}
 * </ul>
 */
@Header("Content-Disposition")
public class ContentDisposition extends BasicStringRangeArrayHeader {

	private static final long serialVersionUID = 1L;

	private static final Cache<String,ContentDisposition> CACHE = new Cache<>(NOCACHE, CACHE_MAX_SIZE);

	/**
	 * Returns a parsed and cached header.
	 *
	 * @param value
	 * 	The header value.
	 * @return A cached {@link ContentDisposition} object.
	 */
	public static ContentDisposition of(String value) {
		if (value == null)
			return null;
		ContentDisposition x = CACHE.get(value);
		if (x == null)
			x = CACHE.put(value, new ContentDisposition(value));
		return x;
	}

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
	 * @return A new {@link ContentDisposition} object.
	 */
	public static ContentDisposition of(Object value) {
		if (value == null)
			return null;
		return new ContentDisposition(value);
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
	 * @return A new {@link ContentDisposition} object.
	 */
	public static ContentDisposition of(Supplier<?> value) {
		if (value == null)
			return null;
		return new ContentDisposition(value);
	}

	/**
	 * Constructor.
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
	public ContentDisposition(Object value) {
		super("Content-Disposition", value);
	}

	/**
	 * Constructor
	 *
	 * @param value
	 * 	The header value.
	 */
	public ContentDisposition(String value) {
		this((Object)value);
	}
}
