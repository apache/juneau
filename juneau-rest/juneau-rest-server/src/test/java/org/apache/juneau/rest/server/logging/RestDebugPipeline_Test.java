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

import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.*;

import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link RestDebugPipeline#resolveTier(Logger)} — cumulative tier derivation from a JUL level.
 *
 * @since 10.0.0
 */
class RestDebugPipeline_Test {

	private Logger newLogger(Level level) {
		var l = Logger.getAnonymousLogger();
		l.setLevel(level);
		return l;
	}

	@Test void a01_finest_resolvesFinest() {
		assertEquals(Level.FINEST, RestDebugPipeline.resolveTier(newLogger(Level.FINEST)));
	}

	@Test void a02_fine_resolvesFine() {
		assertEquals(Level.FINE, RestDebugPipeline.resolveTier(newLogger(Level.FINE)));
	}

	@Test void a03_finer_resolvesFine() {
		assertEquals(Level.FINE, RestDebugPipeline.resolveTier(newLogger(Level.FINER)));
	}

	@Test void a04_config_resolvesInfo() {
		assertEquals(Level.INFO, RestDebugPipeline.resolveTier(newLogger(Level.CONFIG)));
	}

	@Test void a05_info_resolvesInfo() {
		assertEquals(Level.INFO, RestDebugPipeline.resolveTier(newLogger(Level.INFO)));
	}

	@Test void a06_warning_resolvesNull() {
		assertNull(RestDebugPipeline.resolveTier(newLogger(Level.WARNING)));
	}

	@Test void a07_severe_resolvesNull() {
		assertNull(RestDebugPipeline.resolveTier(newLogger(Level.SEVERE)));
	}

	@Test void a08_off_resolvesNull() {
		assertNull(RestDebugPipeline.resolveTier(newLogger(Level.OFF)));
	}
}
