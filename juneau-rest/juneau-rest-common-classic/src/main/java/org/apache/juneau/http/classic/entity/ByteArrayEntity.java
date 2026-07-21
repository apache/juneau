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
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.classic.header.*;

/**
 * A repeatable entity that obtains its content from a byte array.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommon">juneau-rest-common Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S115", // Constants use UPPER_snakeCase naming convention
	"resource", // Resource management handled externally
})
public class ByteArrayEntity extends BasicHttpEntity<ByteArrayEntity> {

	// Argument name constants for assertArgNotNull
	private static final String ARG_out = "out";

	private static final byte[] EMPTY = {};

	/**
	 * Constructor.
	 */
	public ByteArrayEntity() {}

	/**
	 * Constructor.
	 *
	 * @param contentType The entity content type.  Can be <jk>null</jk>.
	 * @param contents The entity contents.  Can be <jk>null</jk>.
	 */
	public ByteArrayEntity(ContentType contentType, byte[] contents) {
		super(contentType, contents);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean being copied.  Must not be <jk>null</jk>.
	 */
	protected ByteArrayEntity(ByteArrayEntity copyFrom) {
		super(copyFrom);
	}

	@Override /* Overridden from AbstractHttpEntity */
	public byte[] asBytes() throws IOException {
		return cp(content());
	}

	@Override /* Overridden from AbstractHttpEntity */
	public String asString() throws IOException {
		return new String(content(), getCharset());
	}

	@Override /* Overridden from BasicHttpEntity */
	public ByteArrayEntity copy() {
		return new ByteArrayEntity(this);
	}

	@Override /* Overridden from HttpEntity */
	public InputStream getContent() throws IOException { return new ByteArrayInputStream(content()); }

	@Override /* Overridden from HttpEntity */
	public long getContentLength() { return isSupplied() ? super.getContentLength() : content().length; }

	@Override /* Overridden from HttpEntity */
	public boolean isRepeatable() { return true; }

	@Override /* Overridden from BasicHttpEntity */
	public ByteArrayEntity unmodifiable() {
		return this instanceof UnmodifiableBean ? this : new Unmodifiable(this);
	}

	@Override /* Overridden from HttpEntity */
	public void writeTo(OutputStream out) throws IOException {
		assertArgNotNull(ARG_out, out);
		out.write(content());
	}

	private byte[] content() {
		return contentOrElse(EMPTY);
	}

	/**
	 * Unmodifiable point-in-time snapshot of the enclosing {@link ByteArrayEntity}.
	 *
	 * <p>
	 * Its only behavioral override is {@link #modify(Runnable)}, which throws — because all mutation is funneled through
	 * {@code modify(...)}, this single override freezes the entire mutation surface.
	 */
	public static class Unmodifiable extends ByteArrayEntity implements UnmodifiableBean {

		/**
		 * Constructor.
		 *
		 * @param copyFrom The entity to snapshot.  Must not be <jk>null</jk>.
		 */
		@SuppressWarnings({
			"java:S1699" // Paradigm intentionally calls the overridable freeze() from the ctor to deep-freeze sub-beans.
		})
		protected Unmodifiable(ByteArrayEntity copyFrom) {
			super(copyFrom);
			freeze();
		}

		@Override /* Overridden from BasicHttpEntity */
		protected ByteArrayEntity modify(Runnable mutation) {
			throw uoex("Bean is unmodifiable.");
		}
	}
}
