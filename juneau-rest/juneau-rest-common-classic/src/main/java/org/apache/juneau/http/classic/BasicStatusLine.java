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
package org.apache.juneau.http.classic;

import static org.apache.juneau.commons.utils.ObjectUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.http.*;
import org.apache.http.impl.*;
import org.apache.http.message.*;
import org.apache.juneau.http.*;

/**
 * A basic implementation of the {@link StatusLine} interface.
 *
 * <p>
 * Immutability is expressed with the "funnel + nested {@code Unmodifiable} snapshot" paradigm: every mutator routes
 * through the single protected {@link #modify(Runnable)} choke-point, and {@link #unmodifiable()} returns a
 * point-in-time snapshot of type {@link Unmodifiable} whose {@code modify(...)} override throws.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommon">juneau-rest-common Basics</a>
 * </ul>
 */
public class BasicStatusLine implements StatusLine {

	/**
	 * Instantiates a new instance of this bean.
	 *
	 * @return A new bean.
	 */
	public static BasicStatusLine create() {
		return new BasicStatusLine();
	}

	/**
	 * Instantiates a new instance of this bean.
	 *
	 * @param statusCode The initial status code.
	 * @param reasonPhrase The initial reason phrase.  Can be <jk>null</jk>.
	 * @return A new bean.
	 */
	public static BasicStatusLine create(int statusCode, String reasonPhrase) {
		return new BasicStatusLine().setStatusCode(statusCode).setReasonPhrase(reasonPhrase);
	}

	private static final ProtocolVersion DEFAULT_PROTOCOL_VERSION = new ProtocolVersion("HTTP", 1, 1);

	private ProtocolVersion protocolVersion = DEFAULT_PROTOCOL_VERSION;
	private int statusCode;
	private String reasonPhrase;
	private ReasonPhraseCatalog reasonPhraseCatalog;
	private Locale locale = Locale.getDefault();

	/**
	 * Constructor.
	 */
	public BasicStatusLine() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The status line being copied.  Must not be <jk>null</jk>.
	 */
	protected BasicStatusLine(BasicStatusLine copyFrom) {
		this.protocolVersion = copyFrom.protocolVersion;
		this.statusCode = copyFrom.statusCode;
		this.reasonPhrase = copyFrom.reasonPhrase;
		this.reasonPhraseCatalog = copyFrom.reasonPhraseCatalog;
		this.locale = copyFrom.locale;
	}

	/**
	 * Returns a copy of this bean.
	 *
	 * @return A copy of this bean.
	 */
	public BasicStatusLine copy() {
		return new BasicStatusLine(this);
	}

	/**
	 * Returns the locale of this status line.
	 *
	 * @return The locale of this status line.
	 */
	public Locale getLocale() { return locale; }

	@Override /* Overridden from StatusLine */
	public ProtocolVersion getProtocolVersion() { return protocolVersion; }

	@Override /* Overridden from StatusLine */
	public String getReasonPhrase() {
		if (reasonPhrase == null) {
			ReasonPhraseCatalog rfc = coalesce(reasonPhraseCatalog, EnglishReasonPhraseCatalog.INSTANCE);
			return rfc.getReason(statusCode, locale);
		}
		return reasonPhrase;
	}

	@Override /* Overridden from StatusLine */
	public int getStatusCode() { return statusCode; }

	/**
	 * Sets the locale used to retrieve reason phrases.
	 *
	 * <p>
	 * If not specified, uses {@link Locale#getDefault()}.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public BasicStatusLine setLocale(Locale value) {
		return modify(() -> locale = value);
	}

	/**
	 * Sets the protocol version on the status line.
	 *
	 * <p>
	 * If not specified, <js>"HTTP/1.1"</js> will be used.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public BasicStatusLine setProtocolVersion(ProtocolVersion value) {
		return modify(() -> protocolVersion = value);
	}

	/**
	 * Sets the reason phrase on the status line.
	 *
	 * <p>
	 * If not specified, the reason phrase will be retrieved from the reason phrase catalog
	 * using the locale on this bean.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public BasicStatusLine setReasonPhrase(String value) {
		return modify(() -> reasonPhrase = value);
	}

	/**
	 * Sets the reason phrase catalog used to retrieve reason phrases.
	 *
	 * <p>
	 * If not specified, uses {@link EnglishReasonPhraseCatalog}.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public BasicStatusLine setReasonPhraseCatalog(ReasonPhraseCatalog value) {
		return modify(() -> reasonPhraseCatalog = value);
	}

	/**
	 * Sets the status code on the status line.
	 *
	 * <p>
	 * If not specified, <c>0</c> will be used.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public BasicStatusLine setStatusCode(int value) {
		return modify(() -> statusCode = value);
	}

	/**
	 * Returns an unmodifiable snapshot of this bean.
	 *
	 * <p>
	 * The returned instance is a point-in-time copy of type {@link Unmodifiable}; any attempt to set a property on it
	 * throws an {@link UnsupportedOperationException}.  This method is idempotent: if this bean is already unmodifiable
	 * it returns itself rather than taking another snapshot.  The receiver is left unchanged (and still modifiable
	 * unless it was already an {@link Unmodifiable}).
	 *
	 * @return An unmodifiable snapshot of this bean, or this bean if it is already unmodifiable.
	 */
	public BasicStatusLine unmodifiable() {
		return this instanceof UnmodifiableBean ? this : new Unmodifiable(this);
	}

	/**
	 * Returns whether this bean is unmodifiable.
	 *
	 * @return <jk>true</jk> if this bean is an {@link Unmodifiable} snapshot.
	 */
	public boolean isUnmodifiable() {
		return this instanceof UnmodifiableBean;
	}

	@Override /* Overridden from Object */
	public String toString() {
		return BasicLineFormatter.INSTANCE.formatStatusLine(null, this).toString();
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof BasicStatusLine other && eq(this, other, (x, y) ->
			eq(x.protocolVersion, y.protocolVersion) && x.statusCode == y.statusCode
			&& eq(x.reasonPhrase, y.reasonPhrase) && eq(x.locale, y.locale));
	}

	@Override
	public int hashCode() {
		return h(protocolVersion, statusCode, reasonPhrase, locale);
	}

	/**
	 * Single mutation funnel — the only choke-point through which all state changes on this bean flow.
	 *
	 * <p>
	 * Every mutator routes through this method.  The {@link Unmodifiable} snapshot overrides only this method to
	 * throw, which freezes the entire mutation surface with a single override.
	 *
	 * @param mutation The state change to apply.
	 * @return This object.
	 */
	protected BasicStatusLine modify(Runnable mutation) {
		mutation.run();
		return this;
	}

	/**
	 * Deep-freeze hook invoked from the {@link Unmodifiable} constructor after the snapshot copy completes.
	 *
	 * <p>
	 * This is a no-op for {@link BasicStatusLine}: all of its fields are immutable value types (protocol version,
	 * status code, reason phrase, locale, reason-phrase catalog) with no mutable sub-beans to deep-freeze.  It exists
	 * as the paradigm's override point for families whose beans hold mutable sub-beans, which must be frozen here by
	 * <b>direct field assignment</b> (never via a setter, which would route through the throwing {@link #modify(Runnable)}).
	 */
	protected void freeze() {
		// No mutable sub-beans to freeze.
	}

	/**
	 * An unmodifiable point-in-time snapshot of a {@link BasicStatusLine}.
	 *
	 * <p>
	 * Its only behavioral override is {@link #modify(Runnable)}, which throws — because all mutation is funneled
	 * through {@code modify(...)}, this single override freezes the entire mutation surface.
	 */
	public static class Unmodifiable extends BasicStatusLine implements UnmodifiableBean {

		/**
		 * Constructor.
		 *
		 * @param copyFrom The status line to snapshot.  Must not be <jk>null</jk>.
		 */
		@SuppressWarnings({
			"java:S1699" // Paradigm intentionally calls the overridable freeze() from the ctor to deep-freeze sub-beans.
		})
		protected Unmodifiable(BasicStatusLine copyFrom) {
			super(copyFrom);
			freeze();
		}

		@Override /* Overridden from BasicStatusLine */
		protected BasicStatusLine modify(Runnable mutation) {
			throw uoex("Bean is unmodifiable.");
		}
	}
}