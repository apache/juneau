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
package org.apache.juneau.examples.guides.secureanendpoint;

// region stub
import java.io.*;
import java.net.*;
import java.nio.charset.*;

import com.sun.net.httpserver.*;

/**
 * Stands in for an OAuth 2.0 authorization server's introspection endpoint.
 * The token "opaque-good" is active with scope orders:read; every other token is inactive.
 */
public class TokenIntrospectionStub implements Closeable {

	private final HttpServer server;

	public TokenIntrospectionStub() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/introspect", exchange -> {
			var form = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			var body = form.contains("token=opaque-good")
				? "{\"active\":true,\"sub\":\"reporting-job\",\"scope\":\"orders:read\"}"
				: "{\"active\":false}";
			var bytes = body.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, bytes.length);
			try (var out = exchange.getResponseBody()) {
				out.write(bytes);
			}
		});
		server.start();
	}

	public URI endpoint() {
		return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/introspect");
	}

	@Override
	public void close() {
		server.stop(0);
	}
}
// endregion stub
