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
package org.apache.juneau.rest.client.mcp.v20260728;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.rest.client.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"resource" // Mock HttpTransport lambda is a short-lived test fixture whose client is already closed via try-with-resources.
})
class McpClient_ErrorHandling_Test {

	@Test
	void a01_jsonRpcError_throwsMcpException() throws Exception {
		HttpTransport transport = tReq -> {
			var json = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"error\":{\"code\":-32601,\"message\":\"Method not found\"}}";
			return TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "application/json")
				.body(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))
				.build();
		};
		try (var c = McpClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var ex = assertThrows(McpException.class, c::ping);
			assertEquals(-32601, ex.getCode());
			assertTrue(ex.getMessage().contains("Method not found"));
		}
	}
}
