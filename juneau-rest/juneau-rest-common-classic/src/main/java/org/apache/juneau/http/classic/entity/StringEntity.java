/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.http.classic.entity;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;

import org.apache.juneau.commons.io.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.classic.header.*;

/**
 * A self contained, repeatable entity that obtains its content from a {@link String}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommon">juneau-rest-common Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S115", // Constants use UPPER_snakeCase naming convention
	"resource", // Resource management handled externally
})
public class StringEntity extends BasicHttpEntity<StringEntity> {

	// Argument name constants for assertArgNotNull
	private static final String ARG_out = "out";

	private static final String EMPTY = "";
	private byte[] byteCache;

	/**
	 * Constructor.
	 */
	public StringEntity() {}

	/**
	 * Constructor.
	 *
	 * @param contentType The entity content type.  Can be <jk>null</jk>.
	 * @param content The entity contents.  Can be <jk>null</jk>.
	 */
	public StringEntity(ContentType contentType, String content) {
		super(contentType, content);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean being copied.  Must not be <jk>null</jk>.
	 */
	protected StringEntity(StringEntity copyFrom) {
		super(copyFrom);
	}

	@Override /* Overridden from AbstractHttpEntity */
	public byte[] asBytes() throws IOException {
		if (isCached() && byteCache == null)
			byteCache = content().getBytes(getCharset());
		if (nn(byteCache))
			return cp(byteCache);
		return content().getBytes(getCharset());
	}

	@Override /* Overridden from AbstractHttpEntity */
	public String asString() throws IOException {
		return content();
	}

	@Override /* Overridden from BasicHttpEntity */
	public StringEntity copy() {
		return new StringEntity(this);
	}

	@Override /* Overridden from HttpEntity */
	public InputStream getContent() throws IOException {
		if (isCached())
			return new ByteArrayInputStream(asBytes());
		return new ReaderInputStream(new StringReader(content()), getCharset());
	}

	@Override /* Overridden from HttpEntity */
	public long getContentLength() {
		if (isCached())
			return asSafeBytes().length;
		long l = super.getContentLength();
		if (l != -1 || isSupplied())
			return l;
		String s = content();
		if (getCharset() == UTF8)
			for (var i = 0; i < s.length(); i++)
				if (s.charAt(i) > 127)
					return -1;
		return s.length();
	}

	@Override /* Overridden from HttpEntity */
	public boolean isRepeatable() { return true; }

	@Override /* Overridden from HttpEntity */
	public boolean isStreaming() { return false; }

	@Override /* Overridden from BasicHttpEntity */
	public StringEntity unmodifiable() {
		return this instanceof UnmodifiableBean ? this : new Unmodifiable(this);
	}

	@Override /* Overridden from HttpEntity */
	public void writeTo(OutputStream out) throws IOException {
		assertArgNotNull(ARG_out, out);
		if (isCached()) {
			out.write(asBytes());
		} else {
			var osw = new OutputStreamWriter(out, getCharset());
			osw.write(content());
			osw.flush();
		}
	}

	private String content() {
		return contentOrElse(EMPTY);
	}

	/**
	 * Unmodifiable point-in-time snapshot of the enclosing {@link StringEntity}.
	 *
	 * <p>
	 * Its only behavioral override is {@link #modify(Runnable)}, which throws — because all mutation is funneled through
	 * {@code modify(...)}, this single override freezes the entire mutation surface.
	 */
	public static class Unmodifiable extends StringEntity implements UnmodifiableBean {

		/**
		 * Constructor.
		 *
		 * @param copyFrom The entity to snapshot.  Must not be <jk>null</jk>.
		 */
		@SuppressWarnings({
			"java:S1699" // Paradigm intentionally calls the overridable freeze() from the ctor to deep-freeze sub-beans.
		})
		protected Unmodifiable(StringEntity copyFrom) {
			super(copyFrom);
			freeze();
		}

		@Override /* Overridden from BasicHttpEntity */
		protected StringEntity modify(Runnable mutation) {
			throw uoex("Bean is unmodifiable.");
		}
	}
}
