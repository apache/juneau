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
package org.apache.juneau.rest.server.filter;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.util.function.*;

import org.apache.juneau.http.*;
import org.apache.juneau.rest.server.*;

import jakarta.servlet.http.*;

/**
 * Thin configuration façade over the always-on request-id correlation resolver.
 *
 * <p>
 * <b>The correlation lifecycle is now owned by the always-on resolver built into {@link RestSession} at session-build
 * time</b> &mdash; it mints or honors the {@code X-Request-Id} correlation id for <i>every</i> request (including 404 /
 * early-error paths that never reach a {@code @RestStartCall} hook), stashes it under
 * {@link RestServerConstants#REQUEST_ID}, echoes it on the response, and opens the {@code requestId} log-context scope.
 * The real tuning knobs live on {@link RequestIdSettings} (resolved via {@link RestContext#getRequestIdSettings()}).
 *
 * <p>
 * This filter is retained for source compatibility.  Its {@link #apply(HttpServletRequest, HttpServletResponse) apply}
 * method is now <b>idempotent</b>: it reads the already-resolved id back through the {@link RestSession#fromRequest
 * session-handle seam} and re-echoes it &mdash; it never double-mints.  Its per-instance builder knobs
 * ({@link Builder#idSupplier idSupplier}, {@link Builder#validator validator}, {@link Builder#attributeKey attributeKey})
 * are <b>documented no-ops</b>: configure {@link RequestIdSettings} instead.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link RequestIdSettings}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerLoggingAndDebugging">Logging / Debugging</a>
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Duplicate string literals are HTTP header names (e.g. X-Request-Id); intentional
})
public class RequestIdFilter {

	/** Standard request and response header name for the request id. */
	public static final String HEADER_REQUEST_ID = RequestIdConstants.HEADER;

	/**
	 * Legacy default validator pattern, retained for source compatibility.
	 *
	 * @deprecated The resolver now defaults to sanitize-and-accept (see {@link RequestIdSettings}); this pattern is no
	 * 	longer applied.
	 */
	@Deprecated
	public static final String DEFAULT_VALIDATOR_PATTERN = "^[A-Za-z0-9-_]{1,128}$";

	/**
	 * Constructor.
	 *
	 * @param b The builder configuring this filter.  Must not be <jk>null</jk>.
	 */
	protected RequestIdFilter(Builder b) {
		assertArgNotNull("builder", b);
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
	 * Re-echoes the correlation id already resolved by the always-on {@link RestSession} resolver.
	 *
	 * <p>
	 * Idempotent façade: the id was already minted/honored/echoed at session build.  This reads it back through the
	 * {@link RestSession#fromRequest session-handle seam} (falling back to the {@link RestServerConstants#REQUEST_ID}
	 * attribute) and re-echoes it on the response &mdash; it never double-mints.
	 *
	 * @param req The servlet request.  Must not be <jk>null</jk>.
	 * @param res The servlet response.  Must not be <jk>null</jk>.
	 * @return The resolved request id, or <jk>null</jk> if none has been resolved (e.g. invoked outside a Juneau call).
	 */
	public String apply(HttpServletRequest req, HttpServletResponse res) {
		assertArgNotNull("req", req);
		assertArgNotNull("res", res);
		var session = RestSession.fromRequest(req);
		String id = session != null ? session.getRequestId() : null;
		if (id == null && req.getAttribute(RestServerConstants.REQUEST_ID) instanceof String s && ! s.isEmpty())
			id = s;
		if (id != null)
			res.setHeader(HEADER_REQUEST_ID, id);
		return id;
	}

	/**
	 * Builder for {@link RequestIdFilter}.
	 *
	 * <p>
	 * The tuning methods below are <b>documented no-ops</b> retained for source compatibility.  They still reject a
	 * <jk>null</jk> argument (so existing null-validation contracts hold) but no longer affect resolution &mdash;
	 * configure {@link RequestIdSettings} instead.
	 */
	public static class Builder {

		/**
		 * Constructor.
		 */
		protected Builder() {}

		/**
		 * <b>No-op</b> (retained for source compatibility).  Configure {@link RequestIdSettings.Builder#idSupplier} instead.
		 *
		 * @param value The supplier.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder idSupplier(Supplier<String> value) {
			assertArgNotNull("value", value);
			return this;
		}

		/**
		 * <b>No-op</b> (retained for source compatibility).  Configure {@link RequestIdSettings.Builder#validator} instead.
		 *
		 * @param value The predicate.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder validator(Predicate<String> value) {
			assertArgNotNull("value", value);
			return this;
		}

		/**
		 * <b>No-op</b> (retained for source compatibility).  Configure {@link RequestIdSettings.Builder#attributeKey} instead.
		 *
		 * @param value The attribute key.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder attributeKey(String value) {
			assertArgNotNull("value", value);
			if (value.isBlank())
				throw new IllegalArgumentException("Argument 'value' must not be blank.");
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
