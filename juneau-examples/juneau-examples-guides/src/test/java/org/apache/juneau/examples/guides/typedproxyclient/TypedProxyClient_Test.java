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
package org.apache.juneau.examples.guides.typedproxyclient;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.examples.guides.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Backs the "Call an existing REST API with a typed proxy" guide ({@code pages/guides/04.TypedProxyClient.md}).
 *
 * <p>
 * Runs {@link BookClient} against {@link BookStubServer} on a free port and compares what it prints with the page's
 * "Run it" output.
 */
class TypedProxyClient_Test extends TestBase {

	private static HttpServer server;
	private static String root;

	@BeforeAll
	static void startStub() throws IOException {
		server = BookStubServer.start(0);
		root = "http://localhost:" + server.getAddress().getPort();
	}

	@AfterAll
	static void stopStub() {
		server.stop(0);
	}

	@Test
	void a01_clientOutput() throws IOException {
		var buf = new ByteArrayOutputStream();
		try (var out = new PrintStream(buf, true, UTF_8)) {
			BookClient.run(root, out);
		}
		var actual = buf.toString(UTF_8).replace("\r\n", "\n").strip();
		assertEquals(GuideRegion.read("TypedProxyClient/run-output.txt", "client-output"), actual);
	}
}
