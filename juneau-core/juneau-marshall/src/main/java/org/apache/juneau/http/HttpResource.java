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

import java.util.*;

import org.apache.http.*;
import org.apache.http.Header;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;

/**
 * An extension of an {@link HttpEntity} that also includes arbitrary headers.
 */
@Response
public interface HttpResource extends HttpEntity {

	/**
	 * Returns the list of headers associated with this resource.
	 *
	 * @return The list of headers associated with this resource.
	 */
	@ResponseHeader("*")
	List<Header> getHeaders();

	/**
	 * Shortcut for calling {@link #chunked()} with <jk>true</jk>.
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResource chunked();

	/**
	 * Shortcut for calling {@link #contentType(Header)}.
	 *
	 * @param value The new <c>Content-Type</ header, or <jk>null</jk> to unset.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResource contentType(Header value);

	/**
	 * Shortcut for calling {@link #contentEncoding(Header)}.
	 *
	 * @param value The new <c>Content-Encoding</ header, or <jk>null</jk> to unset.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResource contentEncoding(Header value);

	/**
	 * Adds an arbitrary header to this resource.
	 *
	 * @param value The header.  Ignored if <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResource header(Header value);

	/**
	 * Sets the content of this resource.
	 *
	 * <p>
	 * Can be any of the following:
	 * 	<ul>
	 * 		<li><c>InputStream</c>
	 * 		<li><c>Reader</c> - Converted to UTF-8 bytes.
	 * 		<li><c>File</c>
	 * 		<li><c>CharSequence</c> - Converted to UTF-8 bytes.
	 * 		<li><c><jk>byte</jk>[]</c>.
	 * 	</ul>
	 * </ul>
	 *
	 * @param value The new contents of this resource.
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResource content(Object value);

	/**
	 * Specifies that the contents of this resource should be cached into an internal byte array so that it can
	 * be read multiple times.
	 *
	 * @return This object (for method chaining).
	 */
	@FluentSetter
	public HttpResource cache();
}
