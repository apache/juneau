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
package org.apache.juneau.rest.filter;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.util.*;
import java.util.function.*;
import java.util.regex.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

import jakarta.servlet.http.*;

/**
 * Per-request id mint-or-honor filter that stashes the id on the servlet request and echoes it on the response.
 *
 * <p>
 * Designed to be invoked from a {@link RestStartCall @RestStartCall} method so the id is available to every
 * downstream component — the call logger, observability layers, and any application code that reads
 * {@code req.getAttribute(RestServerConstants.REQUEST_ID)}.
 *
 * <h5 class='topic'>Behavior</h5>
 *
 * <ul>
 * 	<li>If the incoming request carries an {@code X-Request-Id} header and the value matches the configured
 * 		{@linkplain Builder#validator(Predicate) validator}, that value is honored.
 * 	<li>Otherwise (header absent, blank, or rejected by the validator), a fresh id is minted via the configured
 * 		{@linkplain Builder#idSupplier(Supplier) supplier} (default: {@link UUID#randomUUID()}).
 * 	<li>The chosen id is stashed on the underlying servlet request under the
 * 		{@link org.apache.juneau.rest.RestServerConstants#REQUEST_ID REQUEST_ID} attribute key.
 * 	<li>The chosen id is echoed on the response as {@code X-Request-Id}.
 * </ul>
 *
 * <h5 class='topic'>Default validator</h5>
 *
 * <p>
 * The default validator is {@code ^[A-Za-z0-9-_]{1,128}$}, which accepts UUIDs and the bulk of distributed-tracing
 * id schemes (W3C Trace Context, OpenTelemetry, Datadog, etc.) while rejecting whitespace, control characters,
 * header-injection payloads, and oversized strings.  Customize via {@link Builder#validator(Predicate)}.
 *
 * <h5 class='topic'>Example usage</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> ApiResource <jk>extends</jk> BasicRestServlet {
 *
 * 		<jk>private static final</jk> RequestIdFilter <jsf>REQUEST_ID</jsf> = RequestIdFilter.<jsm>create</jsm>().build();
 *
 * 		<ja>@RestStartCall</ja>
 * 		<jk>public void</jk> stampRequestId(HttpServletRequest <jv>req</jv>, HttpServletResponse <jv>res</jv>) {
 * 			<jsf>REQUEST_ID</jsf>.apply(<jv>req</jv>, <jv>res</jv>);
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerRateLimitAndRequestId">REST Server — Rate-Limiting and Request-Id Propagation</a>
 * </ul>
 *
 * @since 9.5.0
 */
public class RequestIdFilter {

	/** Standard request and response header name for the request id. */
	public static final String HEADER_REQUEST_ID = "X-Request-Id";

	/** Default validator pattern.  Matches UUIDs and most distributed-tracing id schemes. */
	public static final String DEFAULT_VALIDATOR_PATTERN = "^[A-Za-z0-9-_]{1,128}$";

	private final Supplier<String> idSupplier;
	private final Predicate<String> validator;
	private final String attributeKey;

	/**
	 * Constructor.
	 *
	 * @param b The builder configuring this filter.  Must not be <jk>null</jk>.
	 */
	protected RequestIdFilter(Builder b) {
		assertArgNotNull("builder", b);
		this.idSupplier = b.idSupplier != null ? b.idSupplier : () -> UUID.randomUUID().toString();
		this.validator = b.validator != null ? b.validator : Pattern.compile(DEFAULT_VALIDATOR_PATTERN).asPredicate();
		this.attributeKey = b.attributeKey;
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
	 * Mints or honors a request id, stashes it on the request, and echoes it on the response.
	 *
	 * <p>
	 * Idempotent: if the attribute is already set on the servlet request (for example by a parent filter), the
	 * existing value is honored and re-echoed.
	 *
	 * @param req The servlet request.  Must not be <jk>null</jk>.
	 * @param res The servlet response.  Must not be <jk>null</jk>.
	 * @return The resolved request id.  Never <jk>null</jk>.
	 */
	public String apply(HttpServletRequest req, HttpServletResponse res) {
		assertArgNotNull("req", req);
		assertArgNotNull("res", res);
		var existing = req.getAttribute(attributeKey);
		if (existing instanceof String s && ! s.isEmpty()) {
			res.setHeader(HEADER_REQUEST_ID, s);
			return s;
		}
		var incoming = req.getHeader(HEADER_REQUEST_ID);
		var id = (incoming != null && validator.test(incoming)) ? incoming : idSupplier.get();
		req.setAttribute(attributeKey, id);
		res.setHeader(HEADER_REQUEST_ID, id);
		return id;
	}

	/**
	 * Builder for {@link RequestIdFilter}.
	 */
	public static class Builder {

		Supplier<String> idSupplier;
		Predicate<String> validator;
		String attributeKey = org.apache.juneau.rest.RestServerConstants.REQUEST_ID;

		/**
		 * Constructor.
		 */
		protected Builder() {}

		/**
		 * Sets the supplier used to mint a new id when none is honored from the request.
		 *
		 * <p>
		 * Default is {@link UUID#randomUUID()}.  Swap in a smaller / shorter id scheme (e.g. a 16-byte
		 * base32 token) when payload size matters.
		 *
		 * @param value The supplier.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder idSupplier(Supplier<String> value) {
			assertArgNotNull("value", value);
			idSupplier = value;
			return this;
		}

		/**
		 * Sets the predicate used to validate an incoming {@code X-Request-Id} header.
		 *
		 * <p>
		 * Default is {@code Pattern.compile("^[A-Za-z0-9-_]{1,128}$").asPredicate()} — accepts UUIDs and most
		 * distributed-tracing id schemes while rejecting whitespace, control characters, and oversize values.
		 * Values that fail validation are discarded and a fresh id is minted.
		 *
		 * @param value The predicate.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder validator(Predicate<String> value) {
			assertArgNotNull("value", value);
			validator = value;
			return this;
		}

		/**
		 * Overrides the servlet-request attribute key under which the id is stashed.
		 *
		 * <p>
		 * Defaults to {@link org.apache.juneau.rest.RestServerConstants#REQUEST_ID}.  Override only when
		 * coexisting with a third-party filter that publishes the id under a different key.
		 *
		 * @param value The attribute key.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder attributeKey(String value) {
			assertArgNotNull("value", value);
			if (value.isBlank())
				throw new IllegalArgumentException("Argument 'value' must not be blank.");
			attributeKey = value;
			return this;
		}

		/**
		 * Builds the filter.
		 *
		 * @return A new {@link RequestIdFilter}.
		 */
		public RequestIdFilter build() {
			return new RequestIdFilter(this);
		}
	}
}
