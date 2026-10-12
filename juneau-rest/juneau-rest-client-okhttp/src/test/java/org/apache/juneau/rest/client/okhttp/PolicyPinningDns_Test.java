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
package org.apache.juneau.rest.client.okhttp;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.util.*;

import org.junit.jupiter.api.*;

/**
 * Tests for {@link PolicyPinningDns}.
 */
class PolicyPinningDns_Test {

	@Test
	void a01_publicIpLiteral_pinnedToThatAddress() throws Exception {
		assertEquals(List.of(InetAddress.getByName("8.8.8.8")), PolicyPinningDns.INSTANCE.lookup("8.8.8.8"));
	}

	@Test
	void a02_loopbackIpLiteral_refused() {
		var ex = assertThrows(IllegalArgumentException.class, () -> PolicyPinningDns.INSTANCE.lookup("127.0.0.1"));
		assertTrue(ex.getMessage().contains("No allowed address for host '127.0.0.1'"), ex.getMessage());
	}

	@Test
	void a03_localhost_refused() {
		var ex = assertThrows(IllegalArgumentException.class, () -> PolicyPinningDns.INSTANCE.lookup("localhost"));
		assertTrue(ex.getMessage().contains("No allowed address for host 'localhost'"), ex.getMessage());
	}
}
