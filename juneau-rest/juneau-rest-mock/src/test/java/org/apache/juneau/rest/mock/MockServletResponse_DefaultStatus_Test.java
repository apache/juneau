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
package org.apache.juneau.rest.mock;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/**
 * Pins {@link MockServletResponse}'s initial status to the servlet-spec default of {@code 200}, matching a real
 * servlet container, so the mock cannot hide behavior that only appears under a real container.
 *
 * @since 10.0.0
 */
class MockServletResponse_DefaultStatus_Test {

	@Test void a01_initialStatus_is200() {
		assertEquals(200, MockServletResponse.create().getStatus());
	}

	@Test void a02_setStatus_overridesDefault() {
		var res = MockServletResponse.create();
		res.setStatus(404);
		assertEquals(404, res.getStatus());
	}
}
