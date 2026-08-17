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
package org.apache.juneau.http;

/**
 * Shared constants for the {@code X-Request-Id} correlation-id contract.
 *
 * <p>
 * Homed in the security-neutral {@code org.apache.juneau.http} package ({@code juneau-rest-common}) so both the server
 * ({@code juneau-rest-server}'s always-on session-build resolver and {@code RequestIdFilter}) and the client
 * ({@code juneau-rest-client}'s auto-send interceptor and debug formatter) reference one source of truth without a
 * package cycle.  The header name is intentionally not duplicated as a bare literal in either module.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerLoggingAndDebugging">Logging / Debugging</a>
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S115" // Constant mirrors an external protocol literal (the X-Request-Id HTTP header name).
})
public final class RequestIdConstants {

	/** The canonical request/response correlation-id header name: {@code "X-Request-Id"}. */
	public static final String HEADER = "X-Request-Id";

	/**
	 * The maximum sanitized character length of an honored correlation id.
	 *
	 * <p>
	 * Generous versus a 36-character UUID, roomy enough for common non-UUID trace-id shapes, and still firmly bounded.
	 * A client-supplied id whose {@link DebugTextSanitizer#sanitize(String, int) sanitized} form would exceed this cap
	 * is discarded in favor of a freshly minted id rather than echoed truncated.
	 */
	public static final int MAX_LEN = 128;

	private RequestIdConstants() {}
}
