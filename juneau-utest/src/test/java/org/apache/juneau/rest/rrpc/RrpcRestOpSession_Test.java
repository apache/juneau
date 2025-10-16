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
package org.apache.juneau.rest.rrpc;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.junit.jupiter.api.*;

class RrpcRestOpSession_Test extends TestBase {

	@Test void a01_status_fluentChaining() {
		// Test that status() returns RrpcRestOpSession for fluent chaining
		// Note: We can't easily create a full RrpcRestOpSession without a full REST context,
		// so this test verifies that the method signature is correct via compilation.
		// The actual functionality is tested through integration tests.

		// Verify the method exists with correct return type via reflection
		try {
			var method = RrpcRestOpSession.class.getMethod("status", StatusLine.class);
			assertEquals(RrpcRestOpSession.class, method.getReturnType());
		} catch (Exception e) {
			fail("Method status(StatusLine) should exist and return RrpcRestOpSession");
		}
	}

	@Test void a02_finish_fluentChaining() {
		// Test that finish() returns RrpcRestOpSession for fluent chaining
		// Verify the method exists with correct return type via reflection
		try {
			var method = RrpcRestOpSession.class.getMethod("finish");
			assertEquals(RrpcRestOpSession.class, method.getReturnType());
		} catch (Exception e) {
			fail("Method finish() should exist and return RrpcRestOpSession");
		}
	}
}