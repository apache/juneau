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
 * Optional operator-supplied transform applied to request/response body text before it is written to a REST debug log.
 *
 * <p>
 * This SPI is <b>not</b> a default protection. Body dumping is off by default and is enabled only by the
 * {@code JUNEAU_REST_DEBUG_ALLOW_DUMP_BODIES} environment-variable master gate. A scrubber only ever runs once that gate
 * has already permitted dumping; it merely chooses <i>scrubbed-vs-raw</i> body text (e.g. mask fields, drop sections). A
 * configured scrubber never causes body content to be emitted while the gate is unset.
 *
 * <p>
 * The scrubber is <b>fail-closed</b>: if {@link #scrub(String, String)} throws or returns <jk>null</jk>, the formatter
 * emits a suppression placeholder instead of the body — it never falls back to the raw, unscrubbed body. A non-<jk>null</jk>
 * result is still sanitized (control characters escaped) and length-capped before it reaches the log line.
 *
 * <p>
 * Implementations must be <b>thread-safe</b> — the formatter may invoke {@link #scrub(String, String)} concurrently.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerLoggingAndDebugging">Logging / Debugging</a>
 * </ul>
 *
 * @since 10.0.0
 */
public interface RestDebugBodyScrubber {

	/**
	 * Transforms body text before it is logged.
	 *
	 * @param contentType The body's content type (may be <jk>null</jk> if unknown).
	 * @param body The raw (already byte-capped) body text.
	 * @return The scrubbed body text to log, or <jk>null</jk> to fail closed (the formatter emits a placeholder instead).
	 */
	String scrub(String contentType, String body);
}
