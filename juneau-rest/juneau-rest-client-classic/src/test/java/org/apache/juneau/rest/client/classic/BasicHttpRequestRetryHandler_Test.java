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
package org.apache.juneau.rest.client.classic;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.http.client.protocol.*;
import org.apache.http.message.*;
import org.apache.http.protocol.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@link BasicHttpRequestRetryHandler}, in particular the retry-interval sleep and its
 * interrupted-sleep branch.
 */
class BasicHttpRequestRetryHandler_Test {

	private final HttpContext context = newContext();

	private static HttpContext newContext() {
		var c = HttpClientContext.create();
		c.setAttribute(HttpCoreContext.HTTP_REQUEST, new BasicHttpRequest("GET", "/"));
		return c;
	}

	@Test void a01_retryInterval_zero_skipsSleep() {
		var h = new BasicHttpRequestRetryHandler(3, 0, true);
		assertTrue(h.retryRequest(new IOException("x"), 1, context));
	}

	@Test void a02_retryInterval_positive_sleeps() {
		var h = new BasicHttpRequestRetryHandler(3, 1, true);
		assertTrue(h.retryRequest(new IOException("x"), 1, context));
	}

	@Test void a03_retryInterval_positive_interruptedSleep_setsInterruptFlagAndContinues() {
		var h = new BasicHttpRequestRetryHandler(3, 5000, true);
		Thread.currentThread().interrupt();
		try {
			assertTrue(h.retryRequest(new IOException("x"), 1, context));
			assertTrue(Thread.currentThread().isInterrupted(), "Interrupt status should have been restored");
		} finally {
			// Clear the interrupt flag so it doesn't leak into other tests.
			Thread.interrupted();
		}
	}
}
