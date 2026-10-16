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
 * The outcome of {@link RequestBoundary#check(jakarta.servlet.http.HttpServletRequest)}: either allowed, or a
 * rejection carrying the reason, the HTTP status to answer with, and a message safe to return to the caller.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BoundaryReason}
 * 	<li class='jc'>{@link RequestBoundaryFilter}
 * </ul>
 *
 * @since 10.0.0
 */
public final class BoundaryResult {

	/** The outcome of a request that passed every applicable check. */
	public static final BoundaryResult ALLOWED = new BoundaryResult(null, 0, null);

	private final BoundaryReason reason;
	private final int status;
	private final String message;

	BoundaryResult(BoundaryReason reason, int status, String message) {
		this.reason = reason;
		this.status = status;
		this.message = message;
	}

	/**
	 * Whether the request passed.
	 *
	 * @return <jk>true</jk> if the request may proceed.
	 */
	public boolean isAllowed() { return reason == null; }

	/**
	 * Why the request was rejected.
	 *
	 * @return The rejection reason, or <jk>null</jk> when {@link #isAllowed()} is <jk>true</jk>.
	 */
	public BoundaryReason reason() { return reason; }

	/**
	 * The HTTP status to answer a rejected request with.
	 *
	 * @return The status code, or {@code 0} when {@link #isAllowed()} is <jk>true</jk>.
	 */
	public int status() { return status; }

	/**
	 * A message describing the rejection.
	 *
	 * <p>
	 * Names the header at fault and what was expected of it.  It never echoes a value the request presented, and never
	 * reveals the expected CSRF token.
	 *
	 * @return The message, or <jk>null</jk> when {@link #isAllowed()} is <jk>true</jk>.
	 */
	public String message() { return message; }

	@Override /* Object */
	public String toString() { return isAllowed() ? "ALLOWED" : reason + "(" + status + "): " + message; }
}
