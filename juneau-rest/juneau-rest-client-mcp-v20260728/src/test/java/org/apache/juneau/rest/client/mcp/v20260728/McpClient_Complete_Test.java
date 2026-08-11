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
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.rest.client.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"resource" // Mock HttpTransport lambda is a short-lived test fixture whose client is already closed via try-with-resources.
})
class McpClient_Complete_Test extends TestBase {

	@Test
	void a01_complete_serializesRefTypeDiscriminator() throws Exception {
		var seenBody = new AtomicReference<String>();
		HttpTransport transport = tReq -> {
			try {
				var out = new ByteArrayOutputStream();
				tReq.getBody().writeTo(out);
				seenBody.set(out.toString(StandardCharsets.UTF_8));
			} catch (IOException e) {
				throw new TransportException(e.getMessage(), e);
			}
			return TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "application/json")
				.body(new ByteArrayInputStream("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{}}".getBytes(StandardCharsets.UTF_8)))
				.build();
		};
		try (var c = McpClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			c.complete(new PromptReference().setName("p"), "argName", "argVal", null);
		}

		assertTrue(seenBody.get().contains("\"method\":\"completion/complete\""));
		assertTrue(seenBody.get().contains("\"type\":\"ref/prompt\""));
		assertTrue(seenBody.get().contains("\"name\":\"p\""));
		assertTrue(seenBody.get().contains("\"argName\""));
		assertTrue(seenBody.get().contains("\"argVal\""));
	}
}
