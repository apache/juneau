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
 * Verifies that the {@code &content=} query-parameter override is only honored for the request methods that
 * carry a body ({@code PUT}/{@code POST}), so a body can't be injected through the URL on a bodyless method.
 */
class RestRequest_ContentParam_Test extends TestBase {

	@Test void a01_bodyMethodsAllowed() {
		assertTrue(RestRequest.isContentParamMethod("PUT"));
		assertTrue(RestRequest.isContentParamMethod("POST"));
	}

	@Test void a02_caseInsensitive() {
		assertTrue(RestRequest.isContentParamMethod("put"));
		assertTrue(RestRequest.isContentParamMethod("Post"));
	}

	@Test void a03_bodylessMethodsRejected() {
		assertFalse(RestRequest.isContentParamMethod("GET"));
		assertFalse(RestRequest.isContentParamMethod("DELETE"));
		assertFalse(RestRequest.isContentParamMethod("HEAD"));
		assertFalse(RestRequest.isContentParamMethod("OPTIONS"));
	}
}
