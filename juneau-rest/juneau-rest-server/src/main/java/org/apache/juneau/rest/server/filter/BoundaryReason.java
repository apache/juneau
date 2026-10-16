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

/**
 * Why a {@link RequestBoundary} refused a request.
 *
 * <p>
 * Enumerated rather than collapsed into a single "forbidden" so the application can render an accurate reason and log
 * an actionable one.  Distinguishing missing from mismatched leaks nothing a cross-origin caller does not already know
 * about its own request.
 *
 * <p>
 * The constant names are wire codes: {@link RequestBoundaryFilter} sends them in the
 * {@link RequestBoundaryFilter#REJECTION_HEADER} response header and in the rejection body's {@code reason}, and page
 * scripts match on them.  A name, once shipped, does not change.  Each constant notes the status a boundary answers
 * it with; the status travels on the {@link BoundaryResult}, not on this enum.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BoundaryResult}
 * 	<li class='jc'>{@link RequestBoundary}
 * </ul>
 *
 * @since 10.0.0
 */
public enum BoundaryReason {

	/**
	 * The {@code Host} header was absent or was not the configured authority.
	 *
	 * <p>
	 * Answered with 421 Misdirected Request, on every request.  Emitted by {@link LoopbackBoundary}, which pins one
	 * authority.
	 */
	HOST_MISMATCH,

	/** The {@code Host} header was absent.  Answered with 400 Bad Request. */
	HOST_MISSING,

	/** The {@code Host} header could not be parsed as a host and optional port.  Answered with 400 Bad Request. */
	HOST_MALFORMED,

	/** The {@code Host} header parsed, but is not one of the configured allowed hosts.  Answered with 421 Misdirected Request. */
	HOST_NOT_ALLOWED,

	/** A state-changing request carried no {@code Origin} header.  Answered with 403 Forbidden. */
	ORIGIN_MISSING,

	/**
	 * A state-changing request carried the literal {@code Origin} value {@code null}, which browsers send for opaque
	 * origins such as a sandboxed frame.  Answered with 403 Forbidden.
	 */
	ORIGIN_NULL,

	/** A state-changing request's {@code Origin} was not this server's origin.  Answered with 403 Forbidden. */
	ORIGIN_MISMATCH,

	/**
	 * A state-changing request's {@code Sec-Fetch-Site} was present and was not {@code same-origin}.  Answered with
	 * 403 Forbidden.
	 */
	FETCH_SITE_NOT_SAME_ORIGIN,

	/**
	 * A state-changing request with no {@code Origin} and no fetch metadata was one a cross-origin page could send
	 * without a preflight.  Answered with 403 Forbidden.
	 */
	SIMPLE_REQUEST_WITHOUT_ORIGIN,

	/** A state-changing request's content type was not {@code application/json}.  Answered with 415 Unsupported Media Type. */
	CONTENT_TYPE_NOT_JSON,

	/** A state-changing request carried no CSRF token header.  Answered with 403 Forbidden. */
	CSRF_TOKEN_MISSING,

	/** A state-changing request's CSRF token was not the expected token.  Answered with 403 Forbidden. */
	CSRF_TOKEN_MISMATCH
}
