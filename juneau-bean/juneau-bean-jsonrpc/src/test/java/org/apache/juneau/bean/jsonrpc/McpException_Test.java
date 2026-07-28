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
package org.apache.juneau.bean.jsonrpc;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link McpException} and its {@link McpException#toJsonRpcError()} mapping.
 */
class McpException_Test {

	@Test
	void a01_threeArgConstructor_mapsToJsonRpcError() {
		var a = new McpException(-32000, "Tool failed", JsonMap.of("tool", "t1"));
		var b = a.toJsonRpcError();
		assertEquals(-32000, b.getCode());
		assertEquals("Tool failed", b.getMessage());
		assertNotNull(b.getData());
	}

	@Test
	void a02_twoArgConstructorAndSetters() {
		var a = new McpException(1, "m");
		assertNull(a.getData());
		a.setCode(2).setData(JsonMap.of("a", 1));
		assertEquals(2, a.getCode());
		assertNotNull(a.getData());
		var b = a.toJsonRpcError();
		assertEquals(2, b.getCode());
		assertEquals("m", b.getMessage());
	}

	@Test
	void a03_defaultData_isNull() {
		var a = new McpException(-1, "x");
		assertNull(a.getData());
		assertEquals(-1, a.getCode());
		assertString("x", a.getMessage());
	}

	@Test
	void a04_withData_roundTripsThroughJsonRpcError() {
		var a = new McpException(-1, "x", "data");
		assertString("data", a.getData());
		var b = a.toJsonRpcError();
		assertEquals(-1, b.getCode());
		assertString("x", b.getMessage());
		assertString("data", b.getData());
	}
}
