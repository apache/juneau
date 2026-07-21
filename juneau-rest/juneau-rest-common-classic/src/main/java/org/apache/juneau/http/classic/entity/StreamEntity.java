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
import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.classic.header.*;

/**
 * A streamed, non-repeatable entity that obtains its content from an {@link InputStream}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommon">juneau-rest-common Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S115", // Constants use UPPER_snakeCase naming convention
	"resource" // Resource management handled externally
})
public class StreamEntity extends BasicHttpEntity<StreamEntity> {

	// Argument name constants for assertArgNotNull
	private static final String ARG_out = "out";

	private byte[] byteCache;
	private String stringCache;

	/**
	 * Constructor.
	 */
	public StreamEntity() {}

	/**
	 * Constructor.
	 *
	 * @param contentType The entity content type.  Can be <jk>null</jk>.
	 * @param content The entity contents.  Can be <jk>null</jk>.
	 */
	public StreamEntity(ContentType contentType, InputStream content) {
		super(contentType, content);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean being copied.  Must not be <jk>null</jk>.
	 */
	protected StreamEntity(StreamEntity copyFrom) {
		super(copyFrom);
	}

	@Override /* Overridden from AbstractHttpEntity */
	public byte[] asBytes() throws IOException {
		if (isCached() && byteCache == null) {
			try (InputStream is = content()) {
				byteCache = readBytes(is, getMaxLength());
			}
		}
		if (nn(byteCache))
			return cp(byteCache);
		try (InputStream is = content()) {
			return readBytes(is, getMaxLength());
		}
	}

	@Override /* Overridden from AbstractHttpEntity */
	public String asString() throws IOException {
		if (isCached() && stringCache == null)
			stringCache = read(content(), getCharset());
		if (nn(stringCache))
			return stringCache;
		return read(content());
	}

	@Override /* Overridden from BasicHttpEntity */
	public StreamEntity copy() {
		return new StreamEntity(this);
	}

	@Override /* Overridden from HttpEntity */
	public InputStream getContent() throws IOException {
		if (isCached())
			return new ByteArrayInputStream(asBytes());
		return content();
	}

	@Override /* Overridden from HttpEntity */
	public long getContentLength() {
		if (isCached())
			return asSafeBytes().length;
		return super.getContentLength();
	}

	@Override /* Overridden from HttpEntity */
	public boolean isRepeatable() { return isCached(); }

	@Override /* Overridden from HttpEntity */
	public boolean isStreaming() { return ! isCached(); }

	@Override /* Overridden from BasicHttpEntity */
	public StreamEntity unmodifiable() {
		return this instanceof UnmodifiableBean ? this : new Unmodifiable(this);
	}

	/**
	 * Writes bytes from the {@code InputStream} this entity was constructed
	 * with to an {@code OutputStream}.  The content length
	 * determines how many bytes are written.  If the length is unknown ({@code -1}), the
	 * stream will be completely consumed (to the end of the stream).
	 */
	@Override
	public void writeTo(OutputStream out) throws IOException {
		assertArgNotNull(ARG_out, out);

		if (isCached()) {
			out.write(asBytes());
		} else {
			try (InputStream is = getContent()) {
				pipe(is, out, getMaxLength());
			}
		}
	}

	@Override /* Overridden from Object */
	public boolean equals(Object o) {
		// Leaf-type gate: keeps this entity from comparing equal to a different leaf with equal base content, while
		// preserving D3 (StreamEntity.Unmodifiable IS-A StreamEntity, so bean.equals(bean.unmodifiable()) still holds).
		// byteCache/stringCache are memoized derivations of the inherited content/charset and are excluded from equality.
		return o instanceof StreamEntity && super.equals(o);
	}

	@Override /* Overridden from Object */
	public int hashCode() {
		// Fold in the leaf type so distinct leaves land in different buckets; stable across StreamEntity and its
		// Unmodifiable snapshot (both report StreamEntity.class), keeping the equals/hashCode contract intact.
		return h(StreamEntity.class, super.hashCode());
	}

	private InputStream content() {
		return Objects.requireNonNull(contentOrElse((InputStream)null), "Input stream is null.");
	}

	/**
	 * Unmodifiable point-in-time snapshot of the enclosing {@link StreamEntity}.
	 *
	 * <p>
	 * Its only behavioral override is {@link #modify(Runnable)}, which throws — because all mutation is funneled through
	 * {@code modify(...)}, this single override freezes the entire mutation surface.
	 */
	public static class Unmodifiable extends StreamEntity implements UnmodifiableBean {

		/**
		 * Constructor.
		 *
		 * @param copyFrom The entity to snapshot.  Must not be <jk>null</jk>.
		 */
		@SuppressWarnings({
			"java:S1699" // Paradigm intentionally calls the overridable freeze() from the ctor to deep-freeze sub-beans.
		})
		protected Unmodifiable(StreamEntity copyFrom) {
			super(copyFrom);
			freeze();
		}

		@Override /* Overridden from BasicHttpEntity */
		protected StreamEntity modify(Runnable mutation) {
			throw uoex("Bean is unmodifiable.");
		}
	}
}
