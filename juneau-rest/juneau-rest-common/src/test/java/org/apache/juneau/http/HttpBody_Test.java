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

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the {@link HttpBody} default methods.
 */
class HttpBody_Test extends TestBase {

	// Minimal implementation that overrides nothing but the required abstract methods, to exercise the
	// interface's default method bodies (getContentLength()/isRepeatable()) rather than any subclass override.
	private static final class MinimalBody implements HttpBody {
		@Override public String getContentType() { return null; }
		@Override public void writeTo(OutputStream out) {}
	}

	@Test void a01_getContentLength_defaultIsUnknown() {
		assertEquals(-1, new MinimalBody().getContentLength());
	}

	@Test void a02_isRepeatable_defaultIsFalse() {
		assertFalse(new MinimalBody().isRepeatable());
	}
}
