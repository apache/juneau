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
package org.apache.juneau.http;

import java.time.*;
import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.header.Date;

/**
 * Standard predefined HTTP headers.
 */
public class HttpHeaders {

	//-----------------------------------------------------------------------------------------------------------------
	// Standard HTTP headers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new {@link Accept} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Accept accept(String value) {
		return Accept.of(value);
	}

	/**
	 * Creates a new {@link Accept} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Accept accept(Object value) {
		return Accept.of(value);
	}

	/**
	 * Creates a new {@link Accept} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Accept accept(Supplier<?> value) {
		return Accept.of(value);
	}

	/**
	 * Creates a new {@link AcceptCharset} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static AcceptCharset acceptCharset(String value) {
		return AcceptCharset.of(value);
	}

	/**
	 * Creates a new {@link AcceptCharset} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String} - Converted using {@link StringRanges#of(String)}.
	 * 		<li><c>StringRange[]</c> - Left as-is.
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static AcceptCharset acceptCharset(Object value) {
		return AcceptCharset.of(value);
	}

	/**
	 * Creates a new {@link AcceptCharset} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String} - Converted using {@link StringRanges#of(String)}.
	 * 		<li><c>StringRange[]</c> - Left as-is.
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static AcceptCharset acceptCharset(Supplier<?> value) {
		return AcceptCharset.of(value);
	}

	/**
	 * Creates a new {@link AcceptEncoding} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static AcceptEncoding acceptEncoding(String value) {
		return AcceptEncoding.of(value);
	}

	/**
	 * Creates a new {@link AcceptEncoding} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String} - Converted using {@link StringRanges#of(String)}.
	 * 		<li><c>StringRange[]</c> - Left as-is.
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static AcceptEncoding acceptEncoding(Object value) {
		return AcceptEncoding.of(value);
	}

	/**
	 * Creates a new {@link AcceptEncoding} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String} - Converted using {@link StringRanges#of(String)}.
	 * 		<li><c>StringRange[]</c> - Left as-is.
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static AcceptEncoding acceptEncoding(Supplier<?> value) {
		return AcceptEncoding.of(value);
	}

	/**
	 * Creates a new {@link AcceptLanguage} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static AcceptLanguage acceptLanguage(String value) {
		return AcceptLanguage.of(value);
	}

	/**
	 * Creates a new {@link AcceptLanguage} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String} - Converted using {@link StringRanges#of(String)}.
	 * 		<li><c>StringRange[]</c> - Left as-is.
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static AcceptLanguage acceptLanguage(Object value) {
		return AcceptLanguage.of(value);
	}

	/**
	 * Creates a new {@link AcceptLanguage} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String} - Converted using {@link StringRanges#of(String)}.
	 * 		<li><c>StringRange[]</c> - Left as-is.
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static AcceptLanguage acceptLanguage(Supplier<?> value) {
		return AcceptLanguage.of(value);
	}

	/**
	 * Creates a new {@link AcceptRanges} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static AcceptRanges acceptRanges(String value) {
		return AcceptRanges.of(value);
	}

	/**
	 * Creates a new {@link AcceptRanges} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static AcceptRanges acceptRanges(Object value) {
		return AcceptRanges.of(value);
	}

	/**
	 * Creates a new {@link AcceptRanges} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static AcceptRanges acceptRanges(Supplier<?> value) {
		return AcceptRanges.of(value);
	}

	/**
	 * Creates a new {@link Age} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Age age(String value) {
		return Age.of(value);
	}

	/**
	 * Creates a new {@link Age} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link Number} - Converted to an integer using {@link Number#intValue()}.
	 * 		<li>{@link String} - Parsed using {@link Integer#parseInt(String)}.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Age age(Object value) {
		return Age.of(value);
	}

	/**
	 * Creates a new {@link Age} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link Number} - Converted to an integer using {@link Number#intValue()}.
	 * 		<li>{@link String} - Parsed using {@link Integer#parseInt(String)}.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Age age(Supplier<?> value) {
		return Age.of(value);
	}

	/**
	 * Creates a new {@link Allow} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Allow allow(String value) {
		return Allow.of(value);
	}

	/**
	 * Creates a new {@link Allow} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited string.
	 * 		<li><c>String[]</c> - A pre-parsed value.
	 * 		<li>Any other array type - Converted to <c>String[]</c>.
	 * 		<li>Any {@link Collection} - Converted to <c>String[]</c>.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Allow allow(Object value) {
		return Allow.of(value);
	}

	/**
	 * Creates a new {@link Allow} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited string.
	 * 		<li><c>String[]</c> - A pre-parsed value.
	 * 		<li>Any other array type - Converted to <c>String[]</c>.
	 * 		<li>Any {@link Collection} - Converted to <c>String[]</c>.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Allow allow(Supplier<?> value) {
		return Allow.of(value);
	}

	/**
	 * Creates a new {@link Authorization} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Authorization authorization(String value) {
		return Authorization.of(value);
	}

	/**
	 * Creates a new {@link Authorization} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Authorization authorization(Object value) {
		return Authorization.of(value);
	}

	/**
	 * Creates a new {@link Authorization} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Authorization authorization(Supplier<?> value) {
		return Authorization.of(value);
	}

	/**
	 * Creates a new {@link CacheControl} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static CacheControl cacheControl(String value) {
		return CacheControl.of(value);
	}

	/**
	 * Creates a new {@link CacheControl} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static CacheControl cacheControl(Object value) {
		return CacheControl.of(value);
	}

	/**
	 * Creates a new {@link CacheControl} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static CacheControl cacheControl(Supplier<?> value) {
		return CacheControl.of(value);
	}

	/**
	 * Creates a new {@link ClientVersion} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ClientVersion clientVersion(String value) {
		return ClientVersion.of(value);
	}

	/**
	 * Creates a new {@link ClientVersion} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ClientVersion clientVersion(Object value) {
		return ClientVersion.of(value);
	}

	/**
	 * Creates a new {@link ClientVersion} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ClientVersion clientVersion(Supplier<?> value) {
		return ClientVersion.of(value);
	}

	/**
	 * Creates a new {@link Connection} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Connection connection(String value) {
		return Connection.of(value);
	}

	/**
	 * Creates a new {@link Connection} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Connection connection(Object value) {
		return Connection.of(value);
	}

	/**
	 * Creates a new {@link Connection} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Connection connection(Supplier<?> value) {
		return Connection.of(value);
	}

	/**
	 * Creates a new {@link ContentDisposition} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ContentDisposition contentDisposition(String value) {
		return ContentDisposition.of(value);
	}

	/**
	 * Creates a new {@link ContentDisposition} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ContentDisposition contentDisposition(Object value) {
		return ContentDisposition.of(value);
	}

	/**
	 * Creates a new {@link ContentDisposition} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ContentDisposition contentDisposition(Supplier<?> value) {
		return ContentDisposition.of(value);
	}

	/**
	 * Creates a new {@link ContentEncoding} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ContentEncoding contentEncoding(String value) {
		return ContentEncoding.of(value);
	}

	/**
	 * Creates a new {@link ContentEncoding} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ContentEncoding contentEncoding(Object value) {
		return ContentEncoding.of(value);
	}

	/**
	 * Creates a new {@link ContentEncoding} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ContentEncoding contentEncoding(Supplier<?> value) {
		return ContentEncoding.of(value);
	}

	/**
	 * Creates a new {@link ContentLanguage} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ContentLanguage contentLanguage(String value) {
		return ContentLanguage.of(value);
	}

	/**
	 * Creates a new {@link ContentLanguage} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited string.
	 * 		<li><c>String[]</c> - A pre-parsed value.
	 * 		<li>Any other array type - Converted to <c>String[]</c>.
	 * 		<li>Any {@link Collection} - Converted to <c>String[]</c>.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ContentLanguage contentLanguage(Object value) {
		return ContentLanguage.of(value);
	}

	/**
	 * Creates a new {@link ContentLanguage} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited string.
	 * 		<li><c>String[]</c> - A pre-parsed value.
	 * 		<li>Any other array type - Converted to <c>String[]</c>.
	 * 		<li>Any {@link Collection} - Converted to <c>String[]</c>.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ContentLanguage contentLanguage(Supplier<?> value) {
		return ContentLanguage.of(value);
	}

	/**
	 * Creates a new {@link ContentLength} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ContentLength contentLength(String value) {
		return ContentLength.of(value);
	}

	/**
	 * Creates a new {@link ContentLength} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link Number} - Converted to a long using {@link Number#longValue()}.
	 * 		<li>{@link String} - Parsed using {@link Long#parseLong(String)}.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ContentLength contentLength(Object value) {
		return ContentLength.of(value);
	}

	/**
	 * Creates a new {@link ContentLength} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link Number} - Converted to a long using {@link Number#longValue()}.
	 * 		<li>{@link String} - Parsed using {@link Long#parseLong(String)}.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ContentLength contentLength(Supplier<?> value) {
		return ContentLength.of(value);
	}

	/**
	 * Creates a new {@link ContentLocation} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ContentLocation contentLocation(String value) {
		return ContentLocation.of(value);
	}

	/**
	 * Creates a new {@link ContentLocation} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ContentLocation contentLocation(Object value) {
		return ContentLocation.of(value);
	}

	/**
	 * Creates a new {@link ContentLocation} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ContentLocation contentLocation(Supplier<?> value) {
		return ContentLocation.of(value);
	}

	/**
	 * Creates a new {@link ContentRange} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ContentRange contentRange(String value) {
		return ContentRange.of(value);
	}

	/**
	 * Creates a new {@link ContentRange} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ContentRange contentRange(Object value) {
		return ContentRange.of(value);
	}

	/**
	 * Creates a new {@link ContentRange} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ContentRange contentRange(Supplier<?> value) {
		return ContentRange.of(value);
	}

	/**
	 * Creates a new {@link ContentType} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ContentType contentType(String value) {
		return ContentType.of(value);
	}

	/**
	 * Creates a new {@link ContentType} header.
	 *
	 * @param value The header value.
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ContentType contentType(MediaType value) {
		return ContentType.of(value);
	}

	/**
	 * Creates a new {@link ContentType} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String} - Converted using {@link MediaType#of(String)}.
	 * 		<li>{@link MediaType}.
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ContentType contentType(Object value) {
		return ContentType.of(value);
	}

	/**
	 * Creates a new {@link ContentType} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String} - Converted using {@link MediaType#of(String)}.
	 * 		<li>{@link MediaType}.
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ContentType contentType(Supplier<?> value) {
		return ContentType.of(value);
	}

	/**
	 * Creates a new {@link Date} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Date date(String value) {
		return Date.of(value);
	}

	/**
	 * Creates a new {@link Date} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - An RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 		<li>{@link ZonedDateTime}
	 * 		<li>{@link Calendar}
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Date date(Object value) {
		return Date.of(value);
	}

	/**
	 * Creates a new {@link Date} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - An RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 		<li>{@link ZonedDateTime}
	 * 		<li>{@link Calendar}
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Date date(Supplier<?> value) {
		return Date.of(value);
	}

	/**
	 * Creates a new {@link ETag} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ETag eTag(String value) {
		return ETag.of(value);
	}

	/**
	 * Creates a new {@link ETag} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ETag eTag(Object value) {
		return ETag.of(value);
	}

	/**
	 * Creates a new {@link ETag} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ETag eTag(Supplier<?> value) {
		return ETag.of(value);
	}

	/**
	 * Creates a new {@link Expect} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Expect expect(String value) {
		return Expect.of(value);
	}

	/**
	 * Creates a new {@link Expect} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Expect expect(Object value) {
		return Expect.of(value);
	}

	/**
	 * Creates a new {@link Expect} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Expect expect(Supplier<?> value) {
		return Expect.of(value);
	}

	/**
	 * Creates a new {@link Expires} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Expires expires(String value) {
		return Expires.of(value);
	}

	/**
	 * Creates a new {@link Expires} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - An RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 		<li>{@link ZonedDateTime}
	 * 		<li>{@link Calendar}
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Expires expires(Object value) {
		return Expires.of(value);
	}

	/**
	 * Creates a new {@link Expires} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - An RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 		<li>{@link ZonedDateTime}
	 * 		<li>{@link Calendar}
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Expires expires(Supplier<?> value) {
		return Expires.of(value);
	}

	/**
	 * Creates a new {@link Forwarded} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Forwarded forwarded(String value) {
		return Forwarded.of(value);
	}

	/**
	 * Creates a new {@link Forwarded} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Forwarded forwarded(Object value) {
		return Forwarded.of(value);
	}

	/**
	 * Creates a new {@link Forwarded} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Forwarded forwarded(Supplier<?> value) {
		return Forwarded.of(value);
	}

	/**
	 * Creates a new {@link From} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static From from(String value) {
		return From.of(value);
	}

	/**
	 * Creates a new {@link From} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static From from(Object value) {
		return From.of(value);
	}

	/**
	 * Creates a new {@link From} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static From from(Supplier<?> value) {
		return From.of(value);
	}

	/**
	 * Creates a new {@link Host} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Host host(String value) {
		return Host.of(value);
	}

	/**
	 * Creates a new {@link Host} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Host host(Object value) {
		return Host.of(value);
	}

	/**
	 * Creates a new {@link Host} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Host host(Supplier<?> value) {
		return Host.of(value);
	}

	/**
	 * Creates a new {@link IfMatch} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static IfMatch ifMatch(String value) {
		return IfMatch.of(value);
	}

	/**
	 * Creates a new {@link IfMatch} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited list of entity validator values (e.g. <js>"\"xyzzy\", \"r2d2xxxx\", \"c3piozzzz\""</js>).
	 * 		<li>A collection or array of {@link EntityTag} objects.
	 * 		<li>A collection or array of anything else - Converted to Strings.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static IfMatch ifMatch(Object value) {
		return IfMatch.of(value);
	}

	/**
	 * Creates a new {@link IfMatch} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited list of entity validator values (e.g. <js>"\"xyzzy\", \"r2d2xxxx\", \"c3piozzzz\""</js>).
	 * 		<li>A collection or array of {@link EntityTag} objects.
	 * 		<li>A collection or array of anything else - Converted to Strings.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static IfMatch ifMatch(Supplier<?> value) {
		return IfMatch.of(value);
	}

	/**
	 * Creates a new {@link IfModifiedSince} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static IfModifiedSince ifModifiedSince(String value) {
		return IfModifiedSince.of(value);
	}

	/**
	 * Creates a new {@link IfModifiedSince} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - An RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 		<li>{@link ZonedDateTime}
	 * 		<li>{@link Calendar}
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static IfModifiedSince ifModifiedSince(Object value) {
		return IfModifiedSince.of(value);
	}

	/**
	 * Creates a new {@link IfModifiedSince} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - An RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 		<li>{@link ZonedDateTime}
	 * 		<li>{@link Calendar}
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static IfModifiedSince ifModifiedSince(Supplier<?> value) {
		return IfModifiedSince.of(value);
	}

	/**
	 * Creates a new {@link IfNoneMatch} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static IfNoneMatch ifNoneMatch(String value) {
		return IfNoneMatch.of(value);
	}

	/**
	 * Creates a new {@link IfNoneMatch} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited list of entity validator values (e.g. <js>"\"xyzzy\", \"r2d2xxxx\", \"c3piozzzz\""</js>).
	 * 		<li>A collection or array of {@link EntityTag} objects.
	 * 		<li>A collection or array of anything else - Converted to Strings.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static IfNoneMatch ifNoneMatch(Object value) {
		return IfNoneMatch.of(value);
	}

	/**
	 * Creates a new {@link IfNoneMatch} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited list of entity validator values (e.g. <js>"\"xyzzy\", \"r2d2xxxx\", \"c3piozzzz\""</js>).
	 * 		<li>A collection or array of {@link EntityTag} objects.
	 * 		<li>A collection or array of anything else - Converted to Strings.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static IfNoneMatch ifNoneMatch(Supplier<?> value) {
		return IfNoneMatch.of(value);
	}

	/**
	 * Creates a new {@link IfRange} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static IfRange ifRange(String value) {
		return IfRange.of(value);
	}

	/**
	 * Creates a new {@link IfRange} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static IfRange ifRange(Object value) {
		return IfRange.of(value);
	}

	/**
	 * Creates a new {@link IfRange} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static IfRange ifRange(Supplier<?> value) {
		return IfRange.of(value);
	}

	/**
	 * Creates a new {@link IfUnmodifiedSince} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static IfUnmodifiedSince ifUnmodifiedSince(String value) {
		return IfUnmodifiedSince.of(value);
	}

	/**
	 * Creates a new {@link IfUnmodifiedSince} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - An RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 		<li>{@link ZonedDateTime}
	 * 		<li>{@link Calendar}
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static IfUnmodifiedSince ifUnmodifiedSince(Object value) {
		return IfUnmodifiedSince.of(value);
	}

	/**
	 * Creates a new {@link IfUnmodifiedSince} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - An RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 		<li>{@link ZonedDateTime}
	 * 		<li>{@link Calendar}
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static IfUnmodifiedSince ifUnmodifiedSince(Supplier<?> value) {
		return IfUnmodifiedSince.of(value);
	}

	/**
	 * Creates a new {@link LastModified} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static LastModified lastModified(String value) {
		return LastModified.of(value);
	}

	/**
	 * Creates a new {@link LastModified} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - An RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 		<li>{@link ZonedDateTime}
	 * 		<li>{@link Calendar}
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static LastModified lastModified(Object value) {
		return LastModified.of(value);
	}

	/**
	 * Creates a new {@link LastModified} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - An RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 		<li>{@link ZonedDateTime}
	 * 		<li>{@link Calendar}
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static LastModified lastModified(Supplier<?> value) {
		return LastModified.of(value);
	}

	/**
	 * Creates a new {@link Referer} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Location location(String value) {
		return Location.of(value);
	}

	/**
	 * Creates a new {@link Referer} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Location location(Object value) {
		return Location.of(value);
	}

	/**
	 * Creates a new {@link Location} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Location location(Supplier<?> value) {
		return Location.of(value);
	}

	/**
	 * Creates a new {@link MaxForwards} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static MaxForwards maxForwards(String value) {
		return MaxForwards.of(value);
	}

	/**
	 * Creates a new {@link MaxForwards} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link Number} - Converted to an integer using {@link Number#intValue()}.
	 * 		<li>{@link String} - Parsed using {@link Integer#parseInt(String)}.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static MaxForwards maxForwards(Object value) {
		return MaxForwards.of(value);
	}

	/**
	 * Creates a new {@link MaxForwards} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link Number} - Converted to an integer using {@link Number#intValue()}.
	 * 		<li>{@link String} - Parsed using {@link Integer#parseInt(String)}.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static MaxForwards maxForwards(Supplier<?> value) {
		return MaxForwards.of(value);
	}

	/**
	 * Creates a new {@link NoTrace} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static NoTrace noTrace(String value) {
		return NoTrace.of(value);
	}

	/**
	 * Creates a new {@link NoTrace} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static NoTrace noTrace(Object value) {
		return NoTrace.of(value);
	}

	/**
	 * Creates a new {@link NoTrace} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static NoTrace noTrace(Supplier<?> value) {
		return NoTrace.of(value);
	}

	/**
	 * Creates a new {@link Origin} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Origin origin(String value) {
		return Origin.of(value);
	}

	/**
	 * Creates a new {@link Origin} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Origin origin(Object value) {
		return Origin.of(value);
	}

	/**
	 * Creates a new {@link Origin} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Origin origin(Supplier<?> value) {
		return Origin.of(value);
	}

	/**
	 * Creates a new {@link Pragma} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Pragma pragma(String value) {
		return Pragma.of(value);
	}

	/**
	 * Creates a new {@link Pragma} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Pragma pragma(Object value) {
		return Pragma.of(value);
	}

	/**
	 * Creates a new {@link Pragma} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Pragma pragma(Supplier<?> value) {
		return Pragma.of(value);
	}

	/**
	 * Creates a new {@link ProxyAuthenticate} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ProxyAuthenticate proxyAuthenticate(String value) {
		return ProxyAuthenticate.of(value);
	}

	/**
	 * Creates a new {@link ProxyAuthenticate} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ProxyAuthenticate proxyAuthenticate(Object value) {
		return ProxyAuthenticate.of(value);
	}

	/**
	 * Creates a new {@link ProxyAuthenticate} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ProxyAuthenticate proxyAuthenticate(Supplier<?> value) {
		return ProxyAuthenticate.of(value);
	}

	/**
	 * Creates a new {@link ProxyAuthorization} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ProxyAuthorization proxyAuthorization(String value) {
		return ProxyAuthorization.of(value);
	}

	/**
	 * Creates a new {@link ProxyAuthorization} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ProxyAuthorization proxyAuthorization(Object value) {
		return ProxyAuthorization.of(value);
	}

	/**
	 * Creates a new {@link ProxyAuthorization} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static ProxyAuthorization proxyAuthorization(Supplier<?> value) {
		return ProxyAuthorization.of(value);
	}

	/**
	 * Creates a new {@link Range} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Range range(String value) {
		return Range.of(value);
	}

	/**
	 * Creates a new {@link Range} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Range range(Object value) {
		return Range.of(value);
	}

	/**
	 * Creates a new {@link Range} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Range range(Supplier<?> value) {
		return Range.of(value);
	}

	/**
	 * Creates a new {@link Referer} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Referer referer(String value) {
		return Referer.of(value);
	}

	/**
	 * Creates a new {@link Referer} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Referer referer(Object value) {
		return Referer.of(value);
	}

	/**
	 * Creates a new {@link Referer} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Referer referer(Supplier<?> value) {
		return Referer.of(value);
	}

	/**
	 * Creates a new {@link RetryAfter} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static RetryAfter retryAfter(String value) {
		return RetryAfter.of(value);
	}

	/**
	 * Creates a new {@link RetryAfter} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static RetryAfter retryAfter(Object value) {
		return RetryAfter.of(value);
	}

	/**
	 * Creates a new {@link RetryAfter} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static RetryAfter retryAfter(Supplier<?> value) {
		return RetryAfter.of(value);
	}

	/**
	 * Creates a new {@link Server} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Server server(String value) {
		return Server.of(value);
	}

	/**
	 * Creates a new {@link Server} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Server server(Object value) {
		return Server.of(value);
	}

	/**
	 * Creates a new {@link Server} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Server server(Supplier<?> value) {
		return Server.of(value);
	}

	/**
	 * Creates a new {@link TE} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static TE te(String value) {
		return TE.of(value);
	}

	/**
	 * Creates a new {@link TE} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String} - Converted using {@link StringRanges#of(String)}.
	 * 		<li><c>StringRange[]</c> - Left as-is.
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static TE te(Object value) {
		return TE.of(value);
	}

	/**
	 * Creates a new {@link TE} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String} - Converted using {@link StringRanges#of(String)}.
	 * 		<li><c>StringRange[]</c> - Left as-is.
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static TE te(Supplier<?> value) {
		return TE.of(value);
	}

	/**
	 * Creates a new {@link Trailer} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Trailer trailer(String value) {
		return Trailer.of(value);
	}

	/**
	 * Creates a new {@link Trailer} header.
	 *
	 * @param value The header value.
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Trailer trailer(Object value) {
		return Trailer.of(value);
	}

	/**
	 * Creates a new {@link Trailer} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Trailer trailer(Supplier<?> value) {
		return Trailer.of(value);
	}

	/**
	 * Creates a new {@link TransferEncoding} header.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static TransferEncoding transferEncoding(String value) {
		return TransferEncoding.of(value);
	}

	/**
	 * Creates a new {@link TransferEncoding} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static TransferEncoding transferEncoding(Object value) {
		return TransferEncoding.of(value);
	}

	/**
	 * Creates a new {@link TransferEncoding} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static TransferEncoding transferEncoding(Supplier<?> value) {
		return TransferEncoding.of(value);
	}

	/**
	 * Creates a new {@link Upgrade} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Upgrade upgrade(String value) {
		return Upgrade.of(value);
	}

	/**
	 * Creates a new {@link Upgrade} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited string.
	 * 		<li><c>String[]</c> - A pre-parsed value.
	 * 		<li>Any other array type - Converted to <c>String[]</c>.
	 * 		<li>Any {@link Collection} - Converted to <c>String[]</c>.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Upgrade upgrade(Object value) {
		return Upgrade.of(value);
	}

	/**
	 * Creates a new {@link Upgrade} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited string.
	 * 		<li><c>String[]</c> - A pre-parsed value.
	 * 		<li>Any other array type - Converted to <c>String[]</c>.
	 * 		<li>Any {@link Collection} - Converted to <c>String[]</c>.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Upgrade upgrade(Supplier<?> value) {
		return Upgrade.of(value);
	}

	/**
	 * Creates a new {@link UserAgent} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static UserAgent userAgent(String value) {
		return UserAgent.of(value);
	}

	/**
	 * Creates a new {@link UserAgent} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static UserAgent userAgent(Object value) {
		return UserAgent.of(value);
	}

	/**
	 * Creates a new {@link UserAgent} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static UserAgent userAgent(Supplier<?> value) {
		return UserAgent.of(value);
	}

	/**
	 * Creates a new {@link Vary} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Vary vary(String value) {
		return Vary.of(value);
	}

	/**
	 * Creates a new {@link Vary} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Vary vary(Object value) {
		return Vary.of(value);
	}

	/**
	 * Creates a new {@link Vary} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Vary vary(Supplier<?> value) {
		return Vary.of(value);
	}

	/**
	 * Creates a new {@link Via} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Via via(String value) {
		return Via.of(value);
	}

	/**
	 * Creates a new {@link Via} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited string.
	 * 		<li><c>String[]</c> - A pre-parsed value.
	 * 		<li>Any other array type - Converted to <c>String[]</c>.
	 * 		<li>Any {@link Collection} - Converted to <c>String[]</c>.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Via via(Object value) {
		return Via.of(value);
	}

	/**
	 * Creates a new {@link Via} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited string.
	 * 		<li><c>String[]</c> - A pre-parsed value.
	 * 		<li>Any other array type - Converted to <c>String[]</c>.
	 * 		<li>Any {@link Collection} - Converted to <c>String[]</c>.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Via via(Supplier<?> value) {
		return Via.of(value);
	}

	/**
	 * Creates a new {@link Warning} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Warning warning(String value) {
		return Warning.of(value);
	}

	/**
	 * Creates a new {@link Warning} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Warning warning(Object value) {
		return Warning.of(value);
	}

	/**
	 * Creates a new {@link Warning} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Warning warning(Supplier<?> value) {
		return Warning.of(value);
	}

	/**
	 * Creates a new {@link WwwAuthenticate} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static WwwAuthenticate wwwAuthenticate(String value) {
		return WwwAuthenticate.of(value);
	}

	/**
	 * Creates a new {@link WwwAuthenticate} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static WwwAuthenticate wwwAuthenticate(Object value) {
		return WwwAuthenticate.of(value);
	}

	/**
	 * Creates a new {@link WwwAuthenticate} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static WwwAuthenticate wwwAuthenticate(Supplier<?> value) {
		return WwwAuthenticate.of(value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Custom headers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new {@link BasicBooleanHeader} header.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicBooleanHeader booleanHeader(String name, String value) {
		return BasicBooleanHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicBooleanHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link Boolean} - As-is.
	 * 		<li>{@link String} - Parsed using {@link Boolean#parseBoolean(String)}.
	 * 		<li>Anything else - Converted to <c>String</c> and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicBooleanHeader booleanHeader(String name, Object value) {
		return BasicBooleanHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicBooleanHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link Boolean} - As-is.
	 * 		<li>{@link String} - Parsed using {@link Boolean#parseBoolean(String)}.
	 * 		<li>Anything else - Converted to <c>String</c> and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicBooleanHeader booleanHeader(String name, Supplier<?> value) {
		return BasicBooleanHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicCsvArrayHeader} header.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicCsvArrayHeader csvArrayHeader(String name, String value) {
		return BasicCsvArrayHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicCsvArrayHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited string.
	 * 		<li><c>String[]</c> - A pre-parsed value.
	 * 		<li>Any other array type - Converted to <c>String[]</c>.
	 * 		<li>Any {@link Collection} - Converted to <c>String[]</c>.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicCsvArrayHeader csvArrayHeader(String name, Object value) {
		return BasicCsvArrayHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicCsvArrayHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited string.
	 * 		<li><c>String[]</c> - A pre-parsed value.
	 * 		<li>Any other array type - Converted to <c>String[]</c>.
	 * 		<li>Any {@link Collection} - Converted to <c>String[]</c>.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicCsvArrayHeader csvArrayHeader(String name, Supplier<?> value) {
		return BasicCsvArrayHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicDateHeader} header.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicDateHeader dateHeader(String name, String value) {
		return BasicDateHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicDateHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - An RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 		<li>{@link ZonedDateTime}
	 * 		<li>{@link Calendar}
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicDateHeader dateHeader(String name, Object value) {
		return BasicDateHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicDateHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - An RFC-1123 formated string (e.g. <js>"Sat, 29 Oct 1994 19:43:31 GMT"</js>).
	 * 		<li>{@link ZonedDateTime}
	 * 		<li>{@link Calendar}
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicDateHeader dateHeader(String name, Supplier<?> value) {
		return BasicDateHeader.of(name, value);
	}

	/**
	 * Creates a new {@link Debug} header.
	 *
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Debug debug(String value) {
		return Debug.of(value);
	}

	/**
	 * Creates a new {@link Debug} header.
	 *
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link Boolean}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()}.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Debug debug(Object value) {
		return Debug.of(value);
	}

	/**
	 * Creates a new {@link Debug} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link Boolean}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()}.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static Debug debug(Supplier<?> value) {
		return Debug.of(value);
	}

	/**
	 * Creates a new {@link BasicEntityTagArrayHeader} header.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicEntityTagArrayHeader entityTagArrayHeader(String name, String value) {
		return BasicEntityTagArrayHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicEntityTagArrayHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited list of entity validator values (e.g. <js>"\"xyzzy\", \"r2d2xxxx\", \"c3piozzzz\""</js>).
	 * 		<li>A collection or array of {@link EntityTag} objects.
	 * 		<li>A collection or array of anything else - Converted to Strings.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicEntityTagArrayHeader entityTagArrayHeader(String name, Object value) {
		return BasicEntityTagArrayHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicEntityTagArrayHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited list of entity validator values (e.g. <js>"\"xyzzy\", \"r2d2xxxx\", \"c3piozzzz\""</js>).
	 * 		<li>A collection or array of {@link EntityTag} objects.
	 * 		<li>A collection or array of anything else - Converted to Strings.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicEntityTagArrayHeader entityTagArrayHeader(String name, Supplier<?> value) {
		return BasicEntityTagArrayHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicEntityTagHeader} header.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicEntityTagHeader entityTagHeader(String name, String value) {
		return BasicEntityTagHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicEntityTagHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited list of entity validator values (e.g. <js>"\"xyzzy\", \"r2d2xxxx\", \"c3piozzzz\""</js>).
	 * 		<li>A collection or array of {@link EntityTag} objects.
	 * 		<li>A collection or array of anything else - Converted to Strings.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicEntityTagHeader entityTagHeader(String name, Object value) {
		return BasicEntityTagHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicEntityTagHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li><c>String</c> - A comma-delimited list of entity validator values (e.g. <js>"\"xyzzy\", \"r2d2xxxx\", \"c3piozzzz\""</js>).
	 * 		<li>A collection or array of {@link EntityTag} objects.
	 * 		<li>A collection or array of anything else - Converted to Strings.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicEntityTagHeader entityTagHeader(String name, Supplier<?> value) {
		return BasicEntityTagHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicIntegerHeader} header.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicIntegerHeader integerHeader(String name, String value) {
		return BasicIntegerHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicIntegerHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link Number} - Converted to an integer using {@link Number#intValue()}.
	 * 		<li>{@link String} - Parsed using {@link Integer#parseInt(String)}.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicIntegerHeader integerHeader(String name, Object value) {
		return BasicIntegerHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicIntegerHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link Number} - Converted to an integer using {@link Number#intValue()}.
	 * 		<li>{@link String} - Parsed using {@link Integer#parseInt(String)}.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicIntegerHeader integerHeader(String name, Supplier<?> value) {
		return BasicIntegerHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicLongHeader} header.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicLongHeader longHeader(String name, String value) {
		return BasicLongHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicLongHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link Number} - Converted to a long using {@link Number#longValue()}.
	 * 		<li>{@link String} - Parsed using {@link Long#parseLong(String)}.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicLongHeader longHeader(String name, Object value) {
		return BasicLongHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicLongHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link Number} - Converted to a long using {@link Number#longValue()}.
	 * 		<li>{@link String} - Parsed using {@link Long#parseLong(String)}.
	 * 		<li>Anything else - Converted to <c>String</c>.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicLongHeader longHeader(String name, Supplier<?> value) {
		return BasicLongHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicMediaRangeArrayHeader} header.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicMediaRangeArrayHeader mediaRangeArrayHeader(String name, String value) {
		return BasicMediaRangeArrayHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicMediaRangeArrayHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicMediaRangeArrayHeader mediaRangeArrayHeader(String name, Object value) {
		return BasicMediaRangeArrayHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicMediaRangeArrayHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicMediaRangeArrayHeader mediaRangeArrayHeader(String name, Supplier<?> value) {
		return BasicMediaRangeArrayHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicMediaTypeHeader} header.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicMediaTypeHeader mediaTypeHeader(String name, String value) {
		return BasicMediaTypeHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicMediaTypeHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicMediaTypeHeader mediaTypeHeader(String name, Object value) {
		return BasicMediaTypeHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicMediaTypeHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicMediaTypeHeader mediaTypeHeader(String name, Supplier<?> value) {
		return BasicMediaTypeHeader.of(name, value);
	}

	/**
	 * Creates a {@link BasicHeader} from a name/value pair string (e.g. <js>"Foo: bar"</js>)
	 *
	 * @param pair The pair string.
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicHeader basicHeader(String pair) {
		return BasicHeader.ofPair(pair);
	}

	/**
	 * Creates a new {@link BasicHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()}.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicHeader basicHeader(String name, Object value) {
		return BasicHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()}.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicHeader basicHeader(String name, Supplier<?> value) {
		return BasicHeader.of(name, value);
	}

	/**
	 * Creates a new {@link SerializedHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any POJO.
	 * @return A new header bean.
	 */
	public static SerializedHeader serializedHeader(String name, Object value) {
		return SerializedHeader.of(name, value);
	}

	/**
	 * Creates a new {@link SerializedHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any POJO.
	 * @return A new header bean.
	 */
	public static SerializedHeader serializedHeader(String name, Supplier<?> value) {
		return SerializedHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicStringHeader} header.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicStringHeader stringHeader(String name, String value) {
		return BasicStringHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicStringHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()}.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicStringHeader stringHeader(String name, Object value) {
		return BasicStringHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicStringHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()}.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicStringHeader stringHeader(String name, Supplier<?> value) {
		return BasicStringHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicStringRangeArrayHeader} header.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicStringRangeArrayHeader stringRangeArrayHeader(String name, String value) {
		return BasicStringRangeArrayHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicStringRangeArrayHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The parameter value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String} - Converted using {@link StringRanges#of(String)}.
	 * 		<li>{@link StringRanges} - Left as-is.
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicStringRangeArrayHeader stringRangeArrayHeader(String name, Object value) {
		return BasicStringRangeArrayHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicStringRangeArrayHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String} - Converted using {@link StringRanges#of(String)}.
	 * 		<li>{@link StringRanges} - Left as-is.
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicStringRangeArrayHeader stringRangeArrayHeader(String name, Supplier<?> value) {
		return BasicStringRangeArrayHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicUriHeader} header.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return A new (possibly cached) header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicUriHeader uriHeader(String name, String value) {
		return BasicUriHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicUriHeader} header.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicUriHeader uriHeader(String name, Object value) {
		return BasicUriHeader.of(name, value);
	}

	/**
	 * Creates a new {@link BasicUriHeader} header with a delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link Header#getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value supplier.
	 * 	<br>Can be a supplier of any of the following:
	 * 	<ul>
	 * 		<li>{@link String}
	 * 		<li>Anything else - Converted to <c>String</c> using {@link Object#toString()} and then parsed.
	 * 	</ul>
	 * @return A new header bean, or <jk>null</jk> if the value was <jk>null</jk>.
	 */
	public static BasicUriHeader uriHeader(String name, Supplier<?> value) {
		return BasicUriHeader.of(name, value);
	}
}
