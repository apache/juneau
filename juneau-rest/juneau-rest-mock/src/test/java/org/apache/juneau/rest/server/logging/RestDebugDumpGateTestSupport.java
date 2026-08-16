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
package org.apache.juneau.rest.server.logging;

/**
 * Test-only bridge to the package-private body-dump gate seam on {@link BasicRestDebugFormatter}.
 *
 * <p>
 * Lives in the {@code org.apache.juneau.rest.server.logging} package (as a split-package test helper in the
 * {@code juneau-rest-mock} test sources) purely so the mock-module end-to-end debug tests can force both gate states
 * without mutating the process environment and without a system-property fallback. It exists only in test sources, so
 * application code cannot reach the seam through it.
 *
 * @since 10.0.0
 */
public final class RestDebugDumpGateTestSupport {

	private RestDebugDumpGateTestSupport() {}

	/** Forces the body-dump gate on. */
	public static void forceOn() {
		BasicRestDebugFormatter.resetAllowDumpBodiesForTest(Boolean.TRUE);
	}

	/** Forces the body-dump gate off. */
	public static void forceOff() {
		BasicRestDebugFormatter.resetAllowDumpBodiesForTest(Boolean.FALSE);
	}

	/** Clears the forced state so the next resolution re-reads the environment once. */
	public static void reset() {
		BasicRestDebugFormatter.resetAllowDumpBodiesForTest(null);
	}
}
