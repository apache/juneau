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
package org.apache.juneau.rest;

import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.response.*;
import org.apache.juneau.svl.*;

/**
 * Represents the contents of a text file with convenience methods for resolving {@link Parameter} variables and adding
 * HTTP response headers.
 *
 * <p>
 * This class is handled special by the {@link WritableHandler} class.
 */
public class ReaderResource implements Writable {

	private final MediaType mediaType;
	private final String[] contents;
	private final VarResolverSession varSession;
	private final Map<String,String> headers;

	/**
	 * Constructor.
	 *
	 * @param mediaType The HTTP media type.
	 * @param contents
	 * 	The contents of this resource.
	 * 	<br>If multiple contents are specified, the results will be concatenated.
	 * 	<br>Contents can be any of the following:
	 * 	<ul>
	 * 		<li><code>CharSequence</code>
	 * 		<li><code>Reader</code>
	 * 		<li><code>File</code>
	 * 	</ul>
	 * @throws IOException
	 */
	protected ReaderResource(MediaType mediaType, Object...contents) throws IOException {
		this(mediaType, null, null, contents);
	}

	/**
	 * Constructor.
	 *
	 * @param mediaType The resource media type.
	 * @param headers The HTTP response headers for this streamed resource.
	 * @param varSession Optional variable resolver for resolving variables in the string.
	 * @param contents
	 * 	The resource contents.
	 * 	<br>If multiple contents are specified, the results will be concatenated.
	 * 	<br>Contents can be any of the following:
	 * 	<ul>
	 * 		<li><code>InputStream</code>
	 * 		<li><code>Reader</code> - Converted to UTF-8 bytes.
	 * 		<li><code>File</code>
	 * 		<li><code>CharSequence</code> - Converted to UTF-8 bytes.
	 *		</ul>
	 * @throws IOException
	 */
	public ReaderResource(MediaType mediaType, Map<String,String> headers, VarResolverSession varSession, Object...contents) throws IOException {
		this.mediaType = mediaType;
		this.varSession = varSession;

		Map<String,String> m = new LinkedHashMap<String,String>();
		if (headers != null)
			for (Map.Entry<String,String> e : headers.entrySet())
				m.put(e.getKey(), StringUtils.toString(e.getValue()));
		this.headers = Collections.unmodifiableMap(m);

		this.contents = new String[contents.length];
		for (int i = 0; i < contents.length; i++) {
			Object c = contents[i];
			if (c == null)
				this.contents[i] = "";
			else if (c instanceof InputStream)
				this.contents[i] = read((InputStream)c);
			else if (c instanceof File)
				this.contents[i] = read((File)c);
			else if (c instanceof Reader)
				this.contents[i] = read((Reader)c);
			else if (c instanceof CharSequence)
				this.contents[i] = ((CharSequence)c).toString();
			else
				throw new IOException("Invalid class type passed to ReaderResource: " + c.getClass().getName());
		}
	}

	/**
	 * Builder class for constructing {@link ReaderResource} objects.
	 */
	@SuppressWarnings("hiding")
	public static class Builder {
		ArrayList<Object> contents = new ArrayList<Object>();
		MediaType mediaType;
		VarResolverSession varResolver;
		Map<String,String> headers = new LinkedHashMap<String,String>();

		/**
		 * Specifies the resource media type string.
		 *
		 * @param mediaType The resource media type string.
		 * @return This object (for method chaining).
		 */
		public Builder mediaType(String mediaType) {
			this.mediaType = MediaType.forString(mediaType);
			return this;
		}

		/**
		 * Specifies the resource media type string.
		 *
		 * @param mediaType The resource media type string.
		 * @return This object (for method chaining).
		 */
		public Builder mediaType(MediaType mediaType) {
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
		 *		</ul>
		 * @return This object (for method chaining).
		 */
		public Builder contents(Object...contents) {
			this.contents.addAll(Arrays.asList(contents));
			return this;
		}

		/**
		 * Specifies an HTTP response header value.
		 *
		 * @param name The HTTP header name.
		 * @param value
		 * 	The HTTP header value.
		 * 	Will be converted to a <code>String</code> using {@link Object#toString()}.
		 * @return This object (for method chaining).
		 */
		public Builder header(String name, Object value) {
			this.headers.put(name, StringUtils.toString(value));
			return this;
		}

		/**
		 * Specifies HTTP response header values.
		 *
		 * @param headers
		 * 	The HTTP headers.
		 * 	Values will be converted to <code>Strings</code> using {@link Object#toString()}.
		 * @return This object (for method chaining).
		 */
		public Builder headers(Map<String,Object> headers) {
			for (Map.Entry<String,Object> e : headers.entrySet())
				header(e.getKey(), e.getValue());
			return this;
		}

		/**
		 * Specifies the variable resolver to use for this resource.
		 *
		 * @param varResolver The variable resolver.
		 * @return This object (for method chaining).
		 */
		public Builder varResolver(VarResolverSession varResolver) {
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

	/**
	 * Get the HTTP response headers.
	 *
	 * @return The HTTP response headers.
	 */
	public Map<String,String> getHeaders() {
		return headers;
	}

	@Override /* Writeable */
	public void writeTo(Writer w) throws IOException {
		for (String s : contents) {
			if (varSession != null)
				varSession.resolveTo(s, w);
			else
				w.write(s);
		}
	}

	@Override /* Writeable */
	public MediaType getMediaType() {
		return mediaType;
	}

	@Override /* Object */
	public String toString() {
		if (contents.length == 1 && varSession == null)
			return contents[0];
		StringWriter sw = new StringWriter();
		for (String s : contents) {
			if (varSession != null)
				return varSession.resolve(s);
			sw.write(s);
		}
		return sw.toString();
	}

	/**
	 * Same as {@link #toString()} but strips comments from the text before returning it.
	 *
	 * <p>
	 * Supports stripping comments from the following media types: HTML, XHTML, XML, JSON, Javascript, CSS.
	 *
	 * @return The resource contents stripped of any comments.
	 */
	public String toCommentStrippedString() {
		String s = toString();
		String subType = mediaType.getSubType();
		if ("html".equals(subType) || "xhtml".equals(subType) || "xml".equals(subType))
			s = s.replaceAll("(?s)<!--(.*?)-->\\s*", "");
		else if ("json".equals(subType) || "javascript".equals(subType) || "css".equals(subType))
			s = s.replaceAll("(?s)\\/\\*(.*?)\\*\\/\\s*", "");
		return s;
	}
}
