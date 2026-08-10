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
package org.apache.juneau.rest;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Verifies the guard used by the safe-redirect helper: only relative paths and allow-listed hosts are treated
 * as safe redirect targets, so a redirect target derived from request input can't send a user to an arbitrary
 * external site.
 */
class RestResponse_SafeRedirect_Test extends TestBase {

	@Test void a01_relativePathsAreSafe() {
		assertTrue(RestResponse.isSafeRedirectUri("/app/page"));
		assertTrue(RestResponse.isSafeRedirectUri("page"));
		assertTrue(RestResponse.isSafeRedirectUri(""));
		assertTrue(RestResponse.isSafeRedirectUri(null));
	}

	@Test void a02_protocolRelativeIsNotSafeWithoutAllowedHost() {
		assertFalse(RestResponse.isSafeRedirectUri("//evil.example"));
	}

	@Test void a03_absoluteCrossOriginRejected() {
		assertFalse(RestResponse.isSafeRedirectUri("https://evil.example/steal"));
	}

	@Test void a04_absoluteAllowedHostAccepted() {
		assertTrue(RestResponse.isSafeRedirectUri("https://good.example/next", "good.example"));
		assertTrue(RestResponse.isSafeRedirectUri("https://GOOD.example/next", "good.example"));
	}

	@Test void a05_absoluteNonAllowedHostRejected() {
		assertFalse(RestResponse.isSafeRedirectUri("https://evil.example/steal", "good.example"));
	}
}
