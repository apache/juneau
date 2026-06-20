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
package org.apache.juneau.rest.server.auth.saml;

import org.opensaml.core.config.*;

/**
 * One-shot OpenSAML bootstrap helper.
 *
 * <p>
 * OpenSAML's {@link InitializationService#initialize()} registers global XMLObject providers, unmarshallers,
 * and signature/encryption support.  It is a global, idempotent-but-not-thread-safe operation; the safe
 * pattern is double-checked locking from a single shared entry point.  Every class in this package that
 * needs to unmarshal or build OpenSAML objects calls {@link #ensureInitialized()} in its constructor.
 *
 * @since 10.0.0
 */
final class OpenSamlBootstrap {

	private static volatile boolean initialized;
	private static final Object LOCK = new Object();

	private OpenSamlBootstrap() {}

	/**
	 * Initializes the OpenSAML global registry the first time it is called.  Subsequent calls are no-ops.
	 *
	 * @throws IllegalStateException If OpenSAML initialization fails.
	 */
	static void ensureInitialized() {
		if (initialized)
			return;
		synchronized (LOCK) {
			if (initialized) // HTT: inner DCL check; unreachable in single-threaded tests (concurrent first-load race)
				return;
			try {
				InitializationService.initialize();
				initialized = true;
			} catch (Exception e) {
				throw new IllegalStateException("OpenSAML initialization failed", e);
			}
		}
	}
}
