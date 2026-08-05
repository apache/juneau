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
package org.apache.juneau.rest.client.mcp.auth;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

import com.nimbusds.openid.connect.sdk.rp.*;

/**
 * Tests for {@link McpApplicationType} (SEP-837 {@code application_type} enum + Nimbus mapping).
 *
 * @since 10.0.0
 */
class McpApplicationType_Test extends TestBase {

	@Test void a01_valuesExist() {
		assertEquals(2, McpApplicationType.values().length);
		assertNotNull(McpApplicationType.valueOf("NATIVE"));
		assertNotNull(McpApplicationType.valueOf("WEB"));
	}

	@Test void a02_toNimbusMapsNative() {
		assertEquals(ApplicationType.NATIVE, McpApplicationType.NATIVE.toNimbus());
	}

	@Test void a03_toNimbusMapsWeb() {
		assertEquals(ApplicationType.WEB, McpApplicationType.WEB.toNimbus());
	}
}
