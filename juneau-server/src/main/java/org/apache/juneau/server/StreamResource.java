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
package org.apache.juneau.server;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.server.response.*;

/**
 * Represents the contents of a byte stream file with convenience methods for adding HTTP response headers.
 * <p>
 * This class is handled special by the {@link StreamableHandler} class.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class StreamResource implements Streamable {

	private byte[] contents;
	private String mediaType;
	private Map<String,String> headers = new LinkedHashMap<String,String>();

	/**
	 * Constructor.
	 * Create a stream resource from a byte array.
	 *
	 * @param contents The resource contents.
	 * @param mediaType The resource media type.
	 */
	public StreamResource(byte[] contents, String mediaType) {
		this.contents = contents;
		this.mediaType = mediaType;
	}

	/**
	 * Constructor.
	 * Create a stream resource from an <code>InputStream</code>.
	 * Contents of stream will be loaded into a reusable byte array.
	 *
	 * @param contents The resource contents.
	 * @param mediaType The resource media type.
	 * @throws IOException
	 */
	public StreamResource(InputStream contents, String mediaType) throws IOException {
		this.contents = IOUtils.readBytes(contents, 1024);
		this.mediaType = mediaType;
	}

	/**
	 * Add an HTTP response header.
	 *
	 * @param name The header name.
	 * @param value The header value, converted to a string using {@link Object#toString()}.
	 * @return This object (for method chaining).
	 */
	public StreamResource setHeader(String name, Object value) {
		headers.put(name, value == null ? "" : value.toString());
		return this;
	}

	/**
	 * Get the HTTP response headers.
	 *
	 * @return The HTTP response headers.  Never <jk>null</jk>.
	 */
	public Map<String,String> getHeaders() {
		return headers;
	}

	@Override /* Streamable */
	public void streamTo(OutputStream os) throws IOException {
		os.write(contents);
	}

	@Override /* Streamable */
	public String getMediaType() {
		return mediaType;
	}
}
