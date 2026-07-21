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

import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.nio.charset.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.http.UnmodifiableBean;
import org.apache.juneau.http.classic.header.*;
import org.apache.juneau.test.assertions.*;

/**
 * A basic {@link org.apache.http.HttpEntity} implementation with additional features.
 *
 * Provides the following features:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Caching.
 * 	<li>
 * 		Fluent setters.
 * 	<li>
 * 		Fluent assertions.
 * 	<li>
 * 		Externally-supplied/dynamic content.
 * </ul>
 *
 * <p>
 * Immutability is expressed with the "funnel + nested {@code Unmodifiable} snapshot" paradigm: every fluent setter
 * routes through the single protected {@link #modify(Runnable)} choke-point, and each concrete leaf gets a nested
 * {@code X.Unmodifiable} whose only behavioral override is a throwing {@code modify(...)}.  A leaf's
 * {@link #unmodifiable()} factory returns a point-in-time snapshot of type {@code X.Unmodifiable}; any attempt to set a
 * property on it throws an {@link UnsupportedOperationException}.
 *
 * <p>
 * This is a self-typed (CRTP) root: a leaf declares <c>class StringEntity <jk>extends</jk> BasicHttpEntity&lt;StringEntity&gt;</c>
 * and inherits leaf-typed fluent setters with no covariant overrides.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>Beans are not thread safe unless they're marked as unmodifiable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommon">juneau-rest-common Basics</a>
 * </ul>
 *
 * @param <SELF> The self type for fluent setters.
 */
@BeanIgnore

@SuppressWarnings({
	"resource", // Content may be streams; only reference equality is used for the content field.
	"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
})
public abstract class BasicHttpEntity<SELF extends BasicHttpEntity<SELF>> implements HttpEntity {

	/**
	 * An empty HttpEntity.
	 */
	@SuppressWarnings({
		"java:S2390" // Convenience constant, not a static-init cycle: ByteArrayEntity()/unmodifiable() touch no static state of either class (only per-instance fields), so nothing here observes a not-yet-initialized value.
	})
	public static final BasicHttpEntity<?> EMPTY = new ByteArrayEntity().unmodifiable();

	private boolean cached;
	private boolean chunked;
	private Object content;
	private Supplier<?> contentSupplier;
	private ContentType contentType;
	private ContentEncoding contentEncoding;
	private Charset charset;
	private long contentLength = -1;
	private int maxLength = -1;

	/**
	 * Constructor.
	 */
	protected BasicHttpEntity() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean being copied.  Must not be <jk>null</jk>.
	 */
	protected BasicHttpEntity(BasicHttpEntity<?> copyFrom) {
		this.cached = copyFrom.cached;
		this.chunked = copyFrom.chunked;
		this.content = copyFrom.content;
		this.contentSupplier = copyFrom.contentSupplier;
		this.contentType = copyFrom.contentType;
		this.contentEncoding = copyFrom.contentEncoding;
		this.contentLength = copyFrom.contentLength;
		this.charset = copyFrom.charset;
		this.maxLength = copyFrom.maxLength;
	}

	/**
	 * Constructor.
	 *
	 * @param contentType The entity content type.  Can be <jk>null</jk>.
	 * @param content The entity content.  Can be <jk>null</jk>.
	 */
	protected BasicHttpEntity(ContentType contentType, Object content) {
		this.contentType = contentType;
		this.content = content;
	}

	/**
	 * Converts the contents of this entity as a byte array.
	 *
	 * <p>
	 * Note that this may exhaust the content on non-repeatable, non-cached entities.
	 *
	 * @return The contents of this entity as a byte array.
	 * @throws IOException If a problem occurred while trying to read the content.
	 */
	public byte[] asBytes() throws IOException {
		return readBytes(getContent());
	}

	/**
	 * Returns an assertion on the contents of this entity.
	 *
	 * <p>
	 * Note that this may exhaust the content on non-repeatable, non-cached entities.
	 *
	 * @return A new fluent assertion.
	 * @throws IOException If a problem occurred while trying to read the byte array.
	 */
	public FluentByteArrayAssertion<BasicHttpEntity<?>> assertBytes() throws IOException {
		return new FluentByteArrayAssertion<>(asBytes(), this);
	}

	/**
	 * Returns an assertion on the contents of this entity.
	 *
	 * <p>
	 * Note that this may exhaust the content on non-repeatable, non-cached entities.
	 *
	 * @return A new fluent assertion.
	 * @throws IOException If a problem occurred while trying to read the byte array.
	 */
	public FluentStringAssertion<BasicHttpEntity<?>> assertString() throws IOException {
		return new FluentStringAssertion<>(asString(), this);
	}

	/**
	 * Converts the contents of this entity as a string.
	 *
	 * <p>
	 * Note that this may exhaust the content on non-repeatable, non-cached entities.
	 *
	 * @return The contents of this entity as a string.
	 * @throws IOException If a problem occurred while trying to read the content.
	 */
	public String asString() throws IOException {
		return read(getContent());
	}

	@Override /* Overridden from HttpEntity */
	public void consumeContent() throws IOException {
		// No-op: Intentional empty implementation for optional interface method
	}

	/**
	 * Creates a builder for this class initialized with the contents of this bean.
	 *
	 * <p>
	 * Allows you to create a modifiable copy of this bean.
	 *
	 * @return A new builder bean.
	 */
	public abstract SELF copy();

	/**
	 * Returns the charset to use when converting to and from stream-based resources.
	 *
	 * @return The charset to use when converting to and from stream-based resources.
	 */
	public Charset getCharset() { return charset == null ? UTF8 : charset; }

	@Override /* Overridden from HttpEntity */
	public InputStream getContent() throws IOException, UnsupportedOperationException { return EMPTY_INPUT_STREAM; }

	@Override /* Overridden from HttpEntity */
	public Header getContentEncoding() { return contentEncoding; }

	@Override /* Overridden from HttpEntity */
	public long getContentLength() { return contentLength; }

	@Override /* Overridden from HttpEntity */
	public Header getContentType() { return contentType; }

	/**
	 * Returns the maximum number of bytes to read or write to and from stream-based resources.
	 *
	 * @return The maximum number of bytes to read or write to and from stream-based resources.
	 */
	public int getMaxLength() { return maxLength; }

	/**
	 * Returns <jk>true</jk> if this entity is cached in-memory.
	 *
	 * @return <jk>true</jk> if this entity is cached in-memory.
	 */
	public boolean isCached() { return cached; }

	@Override /* Overridden from HttpEntity */
	public boolean isChunked() { return chunked; }

	@Override /* Overridden from HttpEntity */
	public boolean isRepeatable() { return false; }

	@Override /* Overridden from HttpEntity */
	public boolean isStreaming() { return false; }

	/**
	 * Returns <jk>true</jk> if this bean is unmodifiable.
	 *
	 * @return <jk>true</jk> if this bean is an {@link UnmodifiableBean} snapshot.
	 */
	public boolean isUnmodifiable() { return this instanceof UnmodifiableBean; }

	/**
	 * Specifies that the contents of this resource should be cached into an internal byte array so that it can
	 * be read multiple times.
	 *
	 * @return This object.
	 * @throws IOException If entity could not be read into memory.
	 */
	public SELF setCached() throws IOException {
		return modify(() -> cached = true);
	}

	/**
	 * Specifies the charset to use when converting to and from stream-based resources.
	 *
	 * @param value The new value.  If <jk>null</jk>, <c>UTF-8</c> is assumed.
	 * @return This object.
	 */
	public SELF setCharset(Charset value) {
		return modify(() -> charset = value);
	}

	/**
	 * Sets the 'chunked' flag value to <jk>true</jk>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>If the {@link HttpEntity#getContentLength()} method returns a negative value, the HttpClient code will always
	 * 		use chunked encoding.
	 * </ul>
	 *
	 * @return This object.
	 */
	public SELF setChunked() {
		return setChunked(true);
	}

	/**
	 * Sets the 'chunked' flag value.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>If the {@link HttpEntity#getContentLength()} method returns a negative value, the HttpClient code will always
	 * 		use chunked encoding.
	 * </ul>
	 *
	 * @param value The new value for this flag.
	 * @return This object.
	 */
	public SELF setChunked(boolean value) {
		return modify(() -> chunked = value);
	}

	/**
	 * Sets the content on this entity bean.
	 *
	 * @param value The entity content, can be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF setContent(Object value) {
		return modify(() -> content = value);
	}

	/**
	 * Sets the content on this entity bean from a supplier.
	 *
	 * <p>
	 * Repeatable entities such as {@link StringEntity} use this to allow the entity content to be resolved at
	 * serialization time.
	 *
	 * @param value The entity content, can be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF setContent(Supplier<?> value) {
		return modify(() -> contentSupplier = value == null ? () -> null : value);
	}

	/**
	 * Sets the content encoding header on this entity bean.
	 *
	 * @param value The new <c>Content-Encoding</c> header, or <jk>null</jk> to unset.
	 * @return This object.
	 */
	public SELF setContentEncoding(ContentEncoding value) {
		return modify(() -> contentEncoding = value);
	}

	/**
	 * Sets the content encoding header on this entity bean.
	 *
	 * @param value The new <c>Content-Encoding</c> header, or <jk>null</jk> to unset.
	 * @return This object.
	 */
	public SELF setContentEncoding(String value) {
		return setContentEncoding(ContentEncoding.of(value));
	}

	/**
	 * Sets the content length on this entity bean.
	 *
	 * @param value The new <c>Content-Length</c> header value, or <c>-1</c> to unset.
	 * @return This object.
	 */
	public SELF setContentLength(long value) {
		return modify(() -> contentLength = value);
	}

	/**
	 * Sets the content type on this entity bean.
	 *
	 * @param value The new <c>Content-Type</c> header, or <jk>null</jk> to unset.
	 * @return This object.
	 */
	public SELF setContentType(ContentType value) {
		return modify(() -> contentType = value);
	}

	/**
	 * Sets the content type on this entity bean.
	 *
	 * @param value The new <c>Content-Type</c> header, or <jk>null</jk> to unset.
	 * @return This object.
	 */
	public SELF setContentType(String value) {
		return setContentType(ContentType.of(value));
	}

	/**
	 * Specifies the maximum number of bytes to read or write to and from stream-based resources.
	 *
	 * <p>
	 * Implementation is not universal.
	 *
	 * @param value The new value.  The default is <c>-1</c> which means read everything.
	 * @return This object.
	 */
	public SELF setMaxLength(int value) {
		return modify(() -> maxLength = value);
	}

	/**
	 * Returns an unmodifiable snapshot of this bean.
	 *
	 * <p>
	 * The returned instance is a point-in-time copy of the leaf's nested {@code Unmodifiable} type; any attempt to set a
	 * property on it throws an {@link UnsupportedOperationException}.  This method is idempotent: if this bean is already
	 * unmodifiable it returns itself rather than taking another snapshot.  The receiver is left unchanged (and still
	 * modifiable unless it was already unmodifiable).
	 *
	 * @return An unmodifiable snapshot of this bean, or this bean if it is already unmodifiable.
	 */
	public abstract SELF unmodifiable();

	@Override /* Overridden from HttpEntity */
	public void writeTo(OutputStream outStream) throws IOException {
		// No-op: Intentional empty implementation for optional interface method
	}

	/**
	 * Compares the shared base content of two entities.
	 *
	 * <p>
	 * This base implementation compares only the fields declared on {@link BasicHttpEntity} and deliberately accepts any
	 * {@code BasicHttpEntity} subtype so that concrete leaves can delegate to it via {@code super.equals(...)}.  It does
	 * <b>not</b> gate on the concrete leaf type; each concrete leaf overrides {@code equals} to first apply an
	 * {@code instanceof <ThatLeaf>} gate (so two unrelated leaves with equal base content are not equal) and then calls
	 * {@code super.equals(...)} to compare the inherited content.  Using {@code instanceof <ThatLeaf>} (rather than
	 * {@code getClass() ==}) is what preserves the immutability invariant that {@code bean.equals(bean.unmodifiable())}
	 * holds, since each {@code X.Unmodifiable} snapshot is a subclass of its leaf {@code X}.
	 */
	@Override /* Overridden from Object */
	public boolean equals(Object o) {
		return o instanceof BasicHttpEntity<?> other && eq(this, other, (x, y) ->
			eq(x.cached, y.cached) && eq(x.chunked, y.chunked) && eq(x.content, y.content)
			&& eq(x.contentSupplier, y.contentSupplier) && eq(x.contentType, y.contentType)
			&& eq(x.contentEncoding, y.contentEncoding) && eq(x.charset, y.charset)
			&& eq(x.contentLength, y.contentLength) && eq(x.maxLength, y.maxLength));
	}

	@Override /* Overridden from Object */
	public int hashCode() {
		return h(cached, chunked, content, contentSupplier, contentType, contentEncoding, charset, contentLength, maxLength);
	}

	/**
	 * Same as {@link #asBytes()} but wraps {@link IOException IOExceptions} inside a {@link RuntimeException}.
	 *
	 * @return The contents of this entity as a byte array.
	 */
	protected byte[] asSafeBytes() {
		try {
			return asBytes();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Returns the content of this entity.
	 *
	 * @param <T> The value type.
	 * @param def The default value if <jk>null</jk>.
	 * @return The content object.
	 */
	@SuppressWarnings({
		"unchecked" // Type erasure requires cast for content
	})
	protected <T> T contentOrElse(T def) {
		Object o = content;
		if (o == null && nn(contentSupplier))
			o = contentSupplier.get();
		return (o == null ? def : (T)o);
	}

	/**
	 * Returns <jk>true</jk> if the contents of this entity are provided through an external supplier.
	 *
	 * <p>
	 * Externally supplied content generally means the content length cannot be reliably determined
	 * based on the content.
	 *
	 * @return <jk>true</jk> if the contents of this entity are provided through an external supplier.
	 */
	protected boolean isSupplied() { return nn(contentSupplier); }

	/**
	 * Returns this object cast to the self type.
	 *
	 * @return This object.
	 */
	@SuppressWarnings({
		"unchecked" // CRTP self-type cast is safe: SELF is bound to the concrete leaf type.
	})
	protected final SELF self() { return (SELF) this; }

	/**
	 * Single mutation funnel — the only choke-point through which all state changes on this bean flow.
	 *
	 * <p>
	 * Every fluent setter routes through this method.  The nested {@code Unmodifiable} snapshot overrides only this
	 * method to throw, which freezes the entire mutation surface with a single override.
	 *
	 * @param mutation The state change to apply.
	 * @return This object.
	 */
	protected SELF modify(Runnable mutation) {
		mutation.run();
		return self();
	}

	/**
	 * Deep-freeze hook invoked from the nested {@code Unmodifiable} constructor after the snapshot copy completes.
	 *
	 * <p>
	 * Entities hold no mutable sub-beans (the content-type / content-encoding headers are immutable value objects, and
	 * the content buffer follows the copy-constructor's reference-copy behavior), so this is a documented no-op — the
	 * single throwing {@link #modify(Runnable)} override on the nested {@code Unmodifiable} already freezes the entire
	 * mutation surface.  A leaf that adds its own mutable sub-bean would override this method (calling
	 * {@code super.freeze()} first).
	 */
	protected void freeze() {
		// No-op: entities have no mutable sub-beans to deep-freeze.
	}
}
