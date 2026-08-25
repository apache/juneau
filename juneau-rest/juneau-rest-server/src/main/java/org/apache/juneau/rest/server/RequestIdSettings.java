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
package org.apache.juneau.rest.server;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.util.function.*;

import org.apache.juneau.commons.utils.*;

/**
 * Configuration for the always-on request-id correlation resolver run at {@link RestSession} build time.
 *
 * <p>
 * This is the public write-surface for tuning how the framework mints or honors the {@code X-Request-Id} correlation id
 * for every request &mdash; including 404 / early-error paths that never reach a {@code @RestStartCall} hook.  Register a
 * configured instance as a bean, or return one from a {@code @Bean} factory method on the resource; the resolver picks it
 * up via {@link RestContext#getRequestIdSettings()}.
 *
 * <h5 class='topic'>Resolution behavior</h5>
 * <ol>
 * 	<li>If the request already carries a non-empty id under {@link #getAttributeKey() attributeKey} (idempotent re-entry
 * 		or a parent filter), that value is honored unchanged.
 * 	<li>Otherwise, if an incoming {@code X-Request-Id} header is present, it is
 * 		{@linkplain org.apache.juneau.http.DebugTextSanitizer#sanitize(String, int) sanitized} and length-capped to
 * 		{@link org.apache.juneau.http.RequestIdConstants#MAX_LEN}; the sanitized value is honored when it is non-empty,
 * 		not truncated, and accepted by {@link #getValidator() validator}.
 * 	<li>Otherwise a fresh id is minted via {@link #getIdSupplier() idSupplier}.
 * </ol>
 *
 * <h5 class='topic'>Default validator &mdash; sanitize-and-accept</h5>
 * <p>
 * Unlike a reject-and-remint regex, the default validator {@linkplain Builder#validator(Predicate) accepts} any candidate
 * that survives sanitization within the length cap.  The sanitizer already neutralizes CR/LF/control-character
 * header-injection payloads (escaping them to inert, ASCII-safe {@code \\uXXXX}), so the default honors a cleaned form
 * rather than discarding a slightly-dirty id.  Supply a stricter validator (e.g. UUID-only) to opt into reject-and-remint.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerLoggingAndDebugging">Logging / Debugging</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class RequestIdSettings {

	private static final String ARG_VALUE = "value";

	private final Predicate<String> validator;
	private final String attributeKey;
	private final Supplier<String> idSupplier;

	/**
	 * Constructor.
	 *
	 * @param b The builder configuring these settings.  Must not be <jk>null</jk>.
	 */
	protected RequestIdSettings(Builder b) {
		assertArgNotNull("builder", b);
		this.validator = b.validator;
		this.attributeKey = b.attributeKey;
		this.idSupplier = b.idSupplier;
	}

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Returns the predicate applied to a sanitized incoming id to decide whether to honor it.
	 *
	 * @return The validator predicate.  Never <jk>null</jk>.
	 */
	public Predicate<String> getValidator() { return validator; }

	/**
	 * Returns the servlet-request attribute key under which the resolved id is stashed.
	 *
	 * @return The attribute key.  Never <jk>null</jk>.
	 */
	public String getAttributeKey() { return attributeKey; }

	/**
	 * Returns the supplier used to mint a fresh id when none is honored from the request.
	 *
	 * @return The id supplier.  Never <jk>null</jk>.
	 */
	public Supplier<String> getIdSupplier() { return idSupplier; }

	/**
	 * Builder for {@link RequestIdSettings}.
	 */
	public static class Builder {

		Predicate<String> validator = s -> true;
		String attributeKey = RestServerConstants.REQUEST_ID;
		Supplier<String> idSupplier = Uuid7::createString;

		/**
		 * Constructor.
		 */
		protected Builder() {}

		/**
		 * Sets the predicate applied to a <b>sanitized</b> incoming {@code X-Request-Id} value.
		 *
		 * <p>
		 * Defaults to accept-all (sanitize-and-accept): any candidate that survives sanitization within
		 * {@link org.apache.juneau.http.RequestIdConstants#MAX_LEN} is honored.  Supply a stricter predicate to
		 * reject-and-remint values that do not match a required shape.
		 *
		 * @param value The predicate, tested against the sanitized candidate.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder validator(Predicate<String> value) {
			assertArgNotNull(ARG_VALUE, value);
			validator = value;
			return this;
		}

		/**
		 * Overrides the servlet-request attribute key under which the resolved id is stashed.
		 *
		 * <p>
		 * Defaults to {@link RestServerConstants#REQUEST_ID}.  Override only when coexisting with a third-party
		 * filter that publishes the id under a different key.
		 *
		 * @param value The attribute key.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder attributeKey(String value) {
			assertArgNotNull(ARG_VALUE, value);
			if (value.isBlank())
				throw new IllegalArgumentException("Argument 'value' must not be blank.");
			attributeKey = value;
			return this;
		}

		/**
		 * Sets the supplier used to mint a new id when none is honored from the request.
		 *
		 * <p>
		 * Defaults to a time-ordered {@linkplain Uuid7 RFC&nbsp;9562 version-7 UUID}.  Swap in a smaller / shorter id
		 * scheme when payload size matters.
		 *
		 * @param value The supplier.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder idSupplier(Supplier<String> value) {
			assertArgNotNull(ARG_VALUE, value);
			idSupplier = value;
			return this;
		}

		/**
		 * Builds the settings.
		 *
		 * @return A new {@link RequestIdSettings}.
		 */
		public RequestIdSettings build() {
			return new RequestIdSettings(this);
		}
	}
}
