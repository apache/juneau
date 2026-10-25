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

// region stub
import static java.nio.charset.StandardCharsets.*;

import java.io.*;
import java.net.*;

import com.sun.net.httpserver.*;

/**
 * A stand-in for the real books API, on the JDK's built-in HTTP server.  Answers with fixed JSON, and echoes a
 * posted book back with id 3.
 */
public class BookStubServer {

	/**
	 * Serves the stand-in API on port 8080 until the process is stopped.
	 *
	 * @param args Unused.
	 * @throws IOException If the port cannot be bound.
	 */
	public static void main(String[] args) throws IOException {
		start(8080);
		System.out.println("Books API stand-in on http://localhost:8080/books");
	}

	/**
	 * Starts the server.
	 *
	 * @param port The port, or {@code 0} for any free port.
	 * @return The running server; call {@code stop(0)} to shut it down.
	 * @throws IOException If the port cannot be bound.
	 */
	public static HttpServer start(int port) throws IOException {
		var server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
		server.createContext("/books", x -> {
			var path = x.getRequestURI().getPath();
			var method = x.getRequestMethod();
			if (method.equals("GET") && path.equals("/books"))
				reply(x, 200, "[{\"id\":1,\"title\":\"Dune\",\"author\":\"Frank Herbert\"},"
					+ "{\"id\":2,\"title\":\"Emma\",\"author\":\"Jane Austen\"}]");
			else if (method.equals("GET") && path.equals("/books/1"))
				reply(x, 200, "{\"id\":1,\"title\":\"Dune\",\"author\":\"Frank Herbert\"}");
			else if (method.equals("POST") && path.equals("/books"))
				reply(x, 200, new String(x.getRequestBody().readAllBytes(), UTF_8).replace("\"id\":0", "\"id\":3"));
			else
				reply(x, 404, "{\"error\":\"Not found\"}");
		});
		server.start();
		return server;
	}

	private static void reply(HttpExchange x, int status, String json) throws IOException {
		var bytes = json.getBytes(UTF_8);
		x.getResponseHeaders().set("Content-Type", "application/json");
		x.sendResponseHeaders(status, bytes.length);
		try (var out = x.getResponseBody()) {
			out.write(bytes);
		}
	}
}
// endregion stub
