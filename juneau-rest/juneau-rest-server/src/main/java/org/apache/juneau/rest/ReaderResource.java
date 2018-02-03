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
import org.apache.juneau.rest.response.*;
import org.apache.juneau.svl.*;

/**
 * Represents the contents of a text file with convenience methods for resolving SVL variables and adding
 * HTTP response headers.
 * 
 * <p>
 * This class is handled special by the {@link WritableHandler} class.
 * <br>This allows these objects to be returned as responses by REST methods.
 * 
 * <p>
 * <l>ReaderResources</l> are meant to be thread-safe and reusable objects.
 * <br>The contents of the request passed into the constructor are immediately converted to read-only strings.
 * 
 * <p>
 * Instances of this class can be built using {@link ReaderResourceBuilder}.
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../overview-summary.html#juneau-rest-server.ReaderResource">Overview &gt; juneau-rest-server &gt; ReaderResource</a>
 * </ul>
 */
public class ReaderResource implements Writable {

	private final MediaType mediaType;
	private final String[] contents;
	private final VarResolverSession varSession;
	private final Map<String,Object> headers;
	
	/**
	 * Creates a new instance of a {@link ReaderResourceBuilder}
	 * 
	 * @return A new instance of a {@link ReaderResourceBuilder}
	 */
	public static ReaderResourceBuilder create() {
		return new ReaderResourceBuilder();
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
	 * 	</ul>
	 * @throws IOException
	 */
	public ReaderResource(MediaType mediaType, Map<String,Object> headers, VarResolverSession varSession, Object...contents) throws IOException {
		this.mediaType = mediaType;
		this.varSession = varSession;

		this.headers = headers == null ? Collections.EMPTY_MAP : Collections.unmodifiableMap(new LinkedHashMap<>(headers));

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
	 * Get the HTTP response headers.
	 * 
	 * @return 
	 * 	The HTTP response headers.  
	 * 	<br>An unmodifiable map.  
	 * 	<br>Never <jk>null</jk>.
	 */
	public Map<String,Object> getHeaders() {
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
