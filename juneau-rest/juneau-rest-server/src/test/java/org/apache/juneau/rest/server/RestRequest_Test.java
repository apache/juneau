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
package org.apache.juneau.rest.server;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/**
 * Tests for the {@code content} URL-parameter body-override method restriction in {@link RestRequest}, exercised
 * via the package-private {@link RestRequest#isContentParamMethod(String)} helper.
 *
 * @since 10.0.0
 */
class RestRequest_Test {

	@Test void a01_put_isAllowed() {
		assertTrue(RestRequest.isContentParamMethod("PUT"));
		assertTrue(RestRequest.isContentParamMethod("put"));
	}

	@Test void a02_post_isAllowed() {
		assertTrue(RestRequest.isContentParamMethod("POST"));
		assertTrue(RestRequest.isContentParamMethod("post"));
	}

	@Test void a03_get_isRejected() {
		assertFalse(RestRequest.isContentParamMethod("GET"));
	}

	@Test void a04_otherMethods_areRejected() {
		assertFalse(RestRequest.isContentParamMethod("DELETE"));
		assertFalse(RestRequest.isContentParamMethod("HEAD"));
		assertFalse(RestRequest.isContentParamMethod("OPTIONS"));
		assertFalse(RestRequest.isContentParamMethod("PATCH"));
	}

	@Test void a05_null_isRejected() {
		assertFalse(RestRequest.isContentParamMethod(null));
	}
}
