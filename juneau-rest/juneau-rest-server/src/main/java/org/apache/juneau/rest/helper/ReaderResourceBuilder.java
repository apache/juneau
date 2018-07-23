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
package org.apache.juneau.rest.helper;

import java.io.*;
import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.svl.*;

/**
 * Builder class for constructing {@link ReaderResource} objects.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-server.RestMethod.ReaderResource">Overview &gt; juneau-rest-server &gt; @RestMethod &gt; ReaderResource</a>
 * </ul>
 */
public final class ReaderResourceBuilder {
	ArrayList<Object> contents = new ArrayList<>();
	MediaType mediaType;
	VarResolverSession varResolver;
	Map<String,Object> headers = new LinkedHashMap<>();

	/**
	 * Specifies the resource media type string.
	 *
	 * @param mediaType The resource media type string.
	 * @return This object (for method chaining).
	 */
	public ReaderResourceBuilder mediaType(String mediaType) {
		this.mediaType = MediaType.forString(mediaType);
		return this;
	}

	/**
	 * Specifies the resource media type string.
	 *
	 * @param mediaType The resource media type string.
	 * @return This object (for method chaining).
	 */
	public ReaderResourceBuilder mediaType(MediaType mediaType) {
		this.mediaType = mediaType;
		return this;
	}

	/**
	 * Specifies the contents for this resource.
	 *
	 * <p>
	 * This method can be called multiple times to add more content.
	 *
	 * @param contents
	 * 	The resource contents.
	 * 	<br>If multiple contents are specified, the results will be concatenated.
	 * 	<br>Contents can be any of the following:
	 * 	<ul>
	 * 		<li><code>InputStream</code>
	 * 		<li><code>Reader</code> - Converted to UTF-8 bytes.
	 * 		<li><code>File</code>
	 * 		<li><code>CharSequence</code> - Converted to UTF-8 bytes.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public ReaderResourceBuilder contents(Object...contents) {
		this.contents.addAll(Arrays.asList(contents));
		return this;
	}

	/**
	 * Specifies an HTTP response header value.
	 *
	 * @param name The HTTP header name.
	 * @param value
	 * 	The HTTP header value.
	 * 	<br>Will be converted to a <code>String</code> using {@link Object#toString()}.
	 * @return This object (for method chaining).
	 */
	public ReaderResourceBuilder header(String name, Object value) {
		this.headers.put(name, value);
		return this;
	}

	/**
	 * Specifies HTTP response header values.
	 *
	 * @param headers
	 * 	The HTTP headers.
	 * 	<br>Values will be converted to <code>Strings</code> using {@link Object#toString()}.
	 * @return This object (for method chaining).
	 */
	public ReaderResourceBuilder headers(Map<String,Object> headers) {
		this.headers.putAll(headers);
		return this;
	}

	/**
	 * Specifies the variable resolver to use for this resource.
	 *
	 * @param varResolver The variable resolver.
	 * @return This object (for method chaining).
	 */
	public ReaderResourceBuilder varResolver(VarResolverSession varResolver) {
		this.varResolver = varResolver;
		return this;
	}

	/**
	 * Create a new {@link ReaderResource} using values in this builder.
	 *
	 * @return A new immutable {@link ReaderResource} object.
	 * @throws IOException
	 */
	public ReaderResource build() throws IOException {
		return new ReaderResource(mediaType, headers, varResolver, contents.toArray());
	}
}