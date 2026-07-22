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
package org.apache.juneau.http.classic.resource;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.http.UnmodifiableBean;
import org.apache.juneau.http.classic.entity.*;
import org.apache.juneau.http.classic.header.*;
import org.apache.juneau.test.assertions.*;

/**
 * A basic {@link HttpResource} implementation with additional features.
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
 * This is a self-typed (CRTP) root: a leaf declares <c>class StringResource <jk>extends</jk> BasicResource&lt;StringResource&gt;</c>
 * and inherits leaf-typed fluent setters with no covariant overrides.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommon">juneau-rest-common Basics</a>
 * </ul>
 *
 * @param <SELF> The self type for fluent setters.
 */
@BeanIgnore /* Use toString() to serialize */
@SuppressWarnings({
	"resource", // Depends on entity (streams); value equality not practical
	"java:S1206", // equals/hashCode not overridden; value equality not practical for this class
	"java:S119", // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
})
public abstract class BasicResource<SELF extends BasicResource<SELF>> implements HttpResource {
	BasicHttpEntity<?> entity;
	HeaderList headers = HeaderList.create();

	/**
	 * Constructor.
	 *
	 * @param entity The entity that makes up this resource content.  Must not be <jk>null</jk>.
	 */
	protected BasicResource(BasicHttpEntity<?> entity) {
		this.entity = entity;
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean being copied.  Must not be <jk>null</jk>.
	 */
	protected BasicResource(BasicResource<?> copyFrom) {
		this.entity = copyFrom.entity.copy();
		this.headers = copyFrom.headers.copy();
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * This is the constructor used when parsing an HTTP response.
	 *
	 * @param response The HTTP response to copy from.  Must not be <jk>null</jk>.
	 * @throws IOException Rethrown from {@link HttpEntity#getContent()}.
	 */
	protected BasicResource(HttpResponse response) throws IOException {
		this(new StreamEntity());
		copyFrom(response);
	}

	/**
	 * Appends the specified header to the end of the headers in this builder.
	 *
	 * <p>
	 * This is a no-op if either the name or value is <jk>null</jk>.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object.
	 */
	public SELF addHeader(String name, String value) {
		if (nn(name) && nn(value))
			headers.append(name, value);
		return self();
	}

	/**
	 * Appends the specified headers to the end of the headers in this builder.
	 *
	 * @param values The headers to set.  <jk>null</jk> headers and headers with <jk>null</jk> names or values are ignored.
	 * @return This object.
	 */
	public SELF addHeaders(Header...values) {
		for (var h : values) {
			if (nn(h)) {
				var n = h.getName();
				var v = h.getValue();
				if (ine(n)) {
					if (eqic(n, "content-type"))
						setContentType(v);
					else if (eqic(n, "content-encoding"))
						setContentEncoding(v);
					else if (eqic(n, "content-length"))
						setContentLength(Long.parseLong(v));
					else
						headers.append(h);
				}
			}
		}
		return self();
	}

	/**
	 * Converts the contents of the entity of this resource as a byte array.
	 *
	 * <p>
	 * Note that this may exhaust the content on non-repeatable, non-cached entities.
	 *
	 * @return The contents of this entity as a byte array.
	 * @throws IOException If a problem occurred while trying to read the content.
	 */
	public byte[] asBytes() throws IOException {
		return entity.asBytes();
	}

	/**
	 * Returns an assertion on the contents of the entity of this resource.
	 *
	 * <p>
	 * Note that this may exhaust the content on non-repeatable, non-cached entities.
	 *
	 * @return A new fluent assertion.
	 * @throws IOException If a problem occurred while trying to read the byte array.
	 */
	public FluentByteArrayAssertion<BasicResource<?>> assertBytes() throws IOException {
		return new FluentByteArrayAssertion<>(asBytes(), this);
	}

	/**
	 * Returns an assertion on the contents of the entity of this resource.
	 *
	 * <p>
	 * Note that this may exhaust the content on non-repeatable, non-cached entities.
	 *
	 * @return A new fluent assertion.
	 * @throws IOException If a problem occurred while trying to read the byte array.
	 */
	public FluentStringAssertion<BasicResource<?>> assertString() throws IOException {
		return new FluentStringAssertion<>(asString(), this);
	}

	/**
	 * Converts the contents of the entity of this resource as a string.
	 *
	 * <p>
	 * Note that this may exhaust the content on non-repeatable, non-cached entities.
	 *
	 * @return The contents of this entity as a string.
	 * @throws IOException If a problem occurred while trying to read the content.
	 */
	public String asString() throws IOException {
		return entity.asString();
	}

	@Override
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
	 * Copies the contents of the specified HTTP response to this builder.
	 *
	 * @param response The response to copy from.  Must not be <jk>null</jk>.
	 * @return This object.
	 * @throws IOException If content could not be retrieved.
	 */
	public SELF copyFrom(HttpResponse response) throws IOException {
		assertArgNotNull("response", response);
		addHeaders(response.getAllHeaders());
		setContent(response.getEntity().getContent());
		return self();
	}

	@Override /* Overridden from HttpEntity */
	public InputStream getContent() throws IOException, UnsupportedOperationException { return entity.getContent(); }

	@Override /* Overridden from HttpEntity */
	public Header getContentEncoding() { return entity.getContentEncoding(); }

	@Override /* Overridden from HttpEntity */
	public long getContentLength() { return entity.getContentLength(); }

	@Override /* Overridden from HttpEntity */
	public Header getContentType() { return entity.getContentType(); }

	/**
	 * Returns access to the underlying builder for the HTTP entity.
	 *
	 * @return The underlying builder for the HTTP entity.
	 */
	public HttpEntity getEntity() { return entity; }

	@Override /* Overridden from HttpResource */
	public HeaderList getHeaders() { return headers; }

	@Override /* Overridden from HttpEntity */
	public boolean isChunked() { return entity.isChunked(); }

	@Override /* Overridden from HttpEntity */
	public boolean isRepeatable() { return entity.isRepeatable(); }

	@Override /* Overridden from HttpEntity */
	public boolean isStreaming() { return entity.isStreaming(); }

	/**
	 * Returns <jk>true</jk> if this bean is unmodifiable.
	 *
	 * @return <jk>true</jk> if this bean is unmodifiable.
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
		entity.setCached();
		return self();
	}

	/**
	 * Sets the 'chunked' flag value to <jk>true</jk>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li>If the {@link HttpEntity#getContentLength()} method returns a negative value, the HttpClient code will always
	 * 		use chunked encoding.
	 * </ul>
	 *
	 * @return This object.
	 */
	public SELF setChunked() {
		entity.setChunked();
		return self();
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
		entity.setChunked(value);
		return self();
	}

	/**
	 * Sets the content on this entity bean.
	 *
	 * @param value The entity content, can be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF setContent(Object value) {
		entity.setContent(value);
		return self();
	}

	/**
	 * Sets the content on this entity bean from a supplier.
	 *
	 * <p>
	 * Repeatable entities such as {@link StringEntity} use this to allow the entity content to be resolved at
	 * serialization time.
	 *
	 * @param value The entity content, can be <jk>null</jk> (treated as a supplier that always returns <jk>null</jk>).
	 * @return This object.
	 */
	public SELF setContent(Supplier<?> value) {
		entity.setContent(value);
		return self();
	}

	/**
	 * Sets the content encoding header on this entity bean.
	 *
	 * @param value The new <c>Content-Encoding</c> header, or <jk>null</jk> to unset.
	 * @return This object.
	 */
	public SELF setContentEncoding(ContentEncoding value) {
		entity.setContentEncoding(value);
		return self();
	}

	/**
	 * Sets the content encoding header on this entity bean.
	 *
	 * @param value The new <c>Content-Encoding</c> header, or <jk>null</jk> to unset.
	 * @return This object.
	 */
	public SELF setContentEncoding(String value) {
		entity.setContentEncoding(value);
		return self();
	}

	/**
	 * Sets the content length on this entity bean.
	 *
	 * @param value The new <c>Content-Length</c> header value, or <c>-1</c> to unset.
	 * @return This object.
	 */
	public SELF setContentLength(long value) {
		entity.setContentLength(value);
		return self();
	}

	/**
	 * Sets the content type on this entity bean.
	 *
	 * @param value The new <c>Content-Type</c> header, or <jk>null</jk> to unset.
	 * @return This object.
	 */
	public SELF setContentType(ContentType value) {
		entity.setContentType(value);
		return self();
	}

	/**
	 * Sets the content type on this entity bean.
	 *
	 * @param value The new <c>Content-Type</c> header, or <jk>null</jk> to unset.
	 * @return This object.
	 */
	public SELF setContentType(String value) {
		entity.setContentType(value);
		return self();
	}

	/**
	 * Sets the specified header in this builder.
	 *
	 * <p>
	 * This is a no-op if either the name or value is <jk>null</jk>.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * @return This object.
	 */
	public SELF setHeader(String name, String value) {
		if (nn(name) && nn(value))
			headers.set(name, value);
		return self();
	}

	/**
	 * Sets the specified headers in this builder.
	 *
	 * @param values The headers to add.  <jk>null</jk> values are ignored.
	 * @return This object.
	 */
	public SELF setHeaders(Header...values) {
		for (var h : values) {
			if (nn(h)) {
				var n = h.getName();
				var v = h.getValue();
				if (ine(n)) {
					if (eqic(n, "content-type"))
						setContentType(v);
					else if (eqic(n, "content-encoding"))
						setContentEncoding(v);
					else if (eqic(n, "content-length"))
						setContentLength(Long.parseLong(v));
					else
						headers.set(h);
				}
			}
		}
		return self();
	}

	/**
	 * Sets the specified headers in this builder.
	 *
	 * @param value The new value.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public SELF setHeaders(HeaderList value) {
		return modify(() -> headers = value.copy());
	}

	/**
	 * Returns an unmodifiable snapshot of this bean.
	 *
	 * <p>
	 * D1 idempotency: if this bean is already an unmodifiable snapshot, it is returned as-is.
	 *
	 * @return An unmodifiable snapshot of this bean, or this bean if it is already unmodifiable.
	 */
	public abstract SELF unmodifiable();

	@Override /* Overridden from HttpEntity */
	public void writeTo(OutputStream outStream) throws IOException {
		entity.writeTo(outStream);
	}

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
	 * Single mutation funnel for direct-field state changes on this bean.
	 *
	 * <p>
	 * Each leaf's nested {@code Unmodifiable} snapshot overrides this method to throw.  Mutators that delegate to the
	 * entity or header sub-beans are frozen by the {@link #freeze()} deep-freeze of those sub-beans; this funnel
	 * additionally covers the direct-field {@link #setHeaders(HeaderList)} reassignment.
	 *
	 * @param mutation The state change to apply.
	 * @return This object.
	 */
	protected SELF modify(Runnable mutation) {
		mutation.run();
		return self();
	}

	/**
	 * Deep-freeze hook invoked from a leaf's nested {@code Unmodifiable} constructor after the snapshot copy completes.
	 *
	 * <p>
	 * The mutable sub-beans are frozen here by <b>direct field assignment</b> (never via a setter, which would route
	 * through the throwing {@link #modify(Runnable)} / frozen sub-beans and blow up during construction).
	 */
	protected void freeze() {
		entity = entity.unmodifiable();   // DIRECT field write — snapshot the copied entity.
		headers = headers.unmodifiable(); // DIRECT field write — snapshot the copied HeaderList.
	}
}