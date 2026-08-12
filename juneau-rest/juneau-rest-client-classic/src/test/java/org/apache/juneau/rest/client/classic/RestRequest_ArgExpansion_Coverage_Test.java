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
import java.net.*;
import java.util.*;

import org.apache.http.*;
import org.apache.juneau.http.classic.header.*;
import org.apache.juneau.http.classic.part.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Exercises the package-private {@code RestRequest#formDataArg}/{@code #headerArg}/{@code #pathArg}/
 * {@code #queryArg} dispatch helpers (invoked by {@link RestClient} while processing {@code @FormData}/
 * {@code @Header}/{@code @Path}/{@code @Query}-annotated remote-proxy method arguments) directly -- same-package
 * access, no visibility change needed. Each helper multiplexes on the runtime type of the supplied value
 * (castable single value, {@code PartList}/{@code HeaderList}, {@code Collection}, array, {@code Map}, bean, or
 * an unrecognized "custom" value) whenever the part/header name is empty, {@code "*"}, or the value is itself a
 * list/array type; otherwise it takes the single-value path. None of the {@code juneau-integration-tests} remote-
 * proxy suites exercise every combination, so this fills in the gaps directly.
 */
@SuppressWarnings({
	"resource" // req() returns a RestRequest whose ownership is transferred to the caller, who is responsible for closing it; some assigned-but-discarded 'r' locals on exception paths are the same already-tracked instance; Eclipse JDT's @Owning warning is by design.
})
class RestRequest_ArgExpansion_Coverage_Test {

	private static HttpServer server;
	private static int port;

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		port = server.getAddress().getPort();
		server.createContext("/echo", exchange -> {
			exchange.getRequestBody().readAllBytes();
			exchange.sendResponseHeaders(200, -1);
			exchange.close();
		});
		server.start();
	}

	@AfterAll
	static void stopServer() {
		if (server != null)
			server.stop(0);
	}

	private static String url() {
		return "http://localhost:" + port + "/echo";
	}

	private static RestRequest req() {
		try {
			return RestClient.create().build().get(url());
		} catch (RestCallException e) {
			throw new RuntimeException(e);
		}
	}

	/** Public getter qualifies this as a "bean" per {@code MarshallingSession#isBean(Object)}. */
	public static class NameBean {
		public String getFoo() { return "bar"; }
		public String getBaz() { return ""; }
	}

	// ==========================================================================
	// a - formDataArg
	// ==========================================================================

	@Test void a01_formDataArg_emptyName_castableSingleValue() {
		try (var r = req().formDataArg("", BasicPart.of("x", "1"), null, null, false)) {
			assertEquals("1", r.getFormData().get("x").orElseThrow().getValue());
		}
	}

	@Test void a02_formDataArg_starName_partList() {
		try (var r = req().formDataArg("*", PartList.of(BasicPart.of("y", "2")), null, null, false)) {
			assertEquals("2", r.getFormData().get("y").orElseThrow().getValue());
		}
	}

	@Test void a03_formDataArg_emptyName_collection() {
		try (var r = req().formDataArg("", List.of(BasicPart.of("z", "3")), null, null, false)) {
			assertEquals("3", r.getFormData().get("z").orElseThrow().getValue());
		}
	}

	@Test void a04_formDataArg_emptyName_array() {
		try (var r = req().formDataArg("", new NameValuePair[]{BasicPart.of("w", "4")}, null, null, false)) {
			assertEquals("4", r.getFormData().get("w").orElseThrow().getValue());
		}
	}

	@Test void a05_formDataArg_emptyName_map() {
		try (var r = req().formDataArg("", Map.of("m", "5"), null, null, false)) {
			assertEquals("5", r.getFormData().get("m").orElseThrow().getValue());
		}
	}

	@Test void a06_formDataArg_emptyName_bean() {
		try (var r = req().formDataArg("", new NameBean(), null, null, false)) {
			assertEquals("bar", r.getFormData().get("foo").orElseThrow().getValue());
		}
	}

	@Test void a07_formDataArg_emptyName_customValue_delegatesToFormDataCustom() {
		try (var r = req().formDataArg("", "foo=bar", null, null, false)) {
			// formDataCustom() sets the Content-Type header directly rather than touching the formData PartList.
			assertTrue(r.getFormData().isEmpty());
			assertEquals("application/x-www-form-urlencoded", r.getHeaders().getFirst("Content-Type").orElseThrow().getValue());
		}
	}

	@Test void a08_formDataArg_emptyName_skipIfEmpty_removesEmptyValueEntries() {
		try (var r = req().formDataArg("", List.of(BasicPart.of("e", "")), null, null, true)) {
			assertTrue(r.getFormData().get("e").isEmpty(), "Empty-valued entry should have been removed by skipIfEmpty");
		}
	}

	@Test void a09_formDataArg_singleValue_skipIfEmpty_emptyValue_isNoOp() {
		try (var r = req().formDataArg("k", "", null, null, true)) {
			assertTrue(r.getFormData().get("k").isEmpty());
		}
	}

	@Test void a10_formDataArg_singleValue_normal() {
		try (var r = req().formDataArg("k", "v", null, null, false)) {
			assertEquals("v", r.getFormData().get("k").orElseThrow().getValue());
		}
	}

	// ==========================================================================
	// b - headerArg
	// ==========================================================================

	@Test void b01_headerArg_emptyName_castableSingleValue() {
		try (var r = req().headerArg("", BasicHeader.of("X", "1"), null, null, false)) {
			assertEquals("1", r.getHeaders().getFirst("X").orElseThrow().getValue());
		}
	}

	@Test void b02_headerArg_starName_headerList() {
		try (var r = req().headerArg("*", HeaderList.of(BasicHeader.of("Y", "2")), null, null, false)) {
			assertEquals("2", r.getHeaders().getFirst("Y").orElseThrow().getValue());
		}
	}

	@Test void b03_headerArg_emptyName_collection() {
		try (var r = req().headerArg("", List.of(BasicHeader.of("Z", "3")), null, null, false)) {
			assertEquals("3", r.getHeaders().getFirst("Z").orElseThrow().getValue());
		}
	}

	@Test void b04_headerArg_emptyName_array() {
		try (var r = req().headerArg("", new Header[]{BasicHeader.of("W", "4")}, null, null, false)) {
			assertEquals("4", r.getHeaders().getFirst("W").orElseThrow().getValue());
		}
	}

	@Test void b05_headerArg_emptyName_map() {
		try (var r = req().headerArg("", Map.of("M", "5"), null, null, false)) {
			assertEquals("5", r.getHeaders().getFirst("M").orElseThrow().getValue());
		}
	}

	@Test void b06_headerArg_emptyName_bean() {
		try (var r = req().headerArg("", new NameBean(), null, null, false)) {
			assertEquals("bar", r.getHeaders().getFirst("foo").orElseThrow().getValue());
		}
	}

	@Test void b07_headerArg_emptyName_customValue_throwsConfigException() {
		try (var r = req()) {
			// Unlike formDataArg/queryArg, headerArg has no "custom" fallback for unrecognized value types.
			var e = assertThrows(RuntimeException.class, () -> r.headerArg("", "not-a-header", null, null, false));
			assertTrue(e.getMessage().contains("Invalid value type"), "Unexpected message: " + e.getMessage());
		}
	}

	@Test void b08_headerArg_emptyName_skipIfEmpty_removesEmptyValueEntries() {
		try (var r = req().headerArg("", List.of(BasicHeader.of("E", "")), null, null, true)) {
			assertTrue(r.getHeaders().getFirst("E").isEmpty(), "Empty-valued entry should have been removed by skipIfEmpty");
		}
	}

	@Test void b09_headerArg_singleValue_normal() {
		try (var r = req().headerArg("k", "v", null, null, false)) {
			assertEquals("v", r.getHeaders().getFirst("k").orElseThrow().getValue());
		}
	}

	// ==========================================================================
	// c - pathArg (no skipIfEmpty parameter, no "custom" fallback -- unrecognized types throw)
	// ==========================================================================

	@Test void c01_pathArg_emptyName_castableSingleValue() {
		try (var r = req().pathArg("", BasicPart.of("x", "1"), null, null)) {
			assertEquals("1", r.getPathData().get("x").orElseThrow().getValue());
		}
	}

	@Test void c02_pathArg_starName_partList() {
		try (var r = req().pathArg("*", PartList.of(BasicPart.of("y", "2")), null, null)) {
			assertEquals("2", r.getPathData().get("y").orElseThrow().getValue());
		}
	}

	@Test void c03_pathArg_emptyName_collection() {
		try (var r = req().pathArg("", List.of(BasicPart.of("z", "3")), null, null)) {
			assertEquals("3", r.getPathData().get("z").orElseThrow().getValue());
		}
	}

	@Test void c04_pathArg_emptyName_array() {
		try (var r = req().pathArg("", new NameValuePair[]{BasicPart.of("w", "4")}, null, null)) {
			assertEquals("4", r.getPathData().get("w").orElseThrow().getValue());
		}
	}

	@Test void c05_pathArg_emptyName_map() {
		try (var r = req().pathArg("", Map.of("m", "5"), null, null)) {
			assertEquals("5", r.getPathData().get("m").orElseThrow().getValue());
		}
	}

	@Test void c06_pathArg_emptyName_bean() {
		try (var r = req().pathArg("", new NameBean(), null, null)) {
			assertEquals("bar", r.getPathData().get("foo").orElseThrow().getValue());
		}
	}

	@Test void c07_pathArg_emptyName_customValue_throwsConfigException() {
		try (var r = req()) {
			var e = assertThrows(RuntimeException.class, () -> r.pathArg("", "not-a-part", null, null));
			assertTrue(e.getMessage().contains("Invalid value type"), "Unexpected message: " + e.getMessage());
		}
	}

	@Test void c08_pathArg_singleValue_normal() {
		try (var r = req().pathArg("k", "v", null, null)) {
			assertEquals("v", r.getPathData().get("k").orElseThrow().getValue());
		}
	}

	// ==========================================================================
	// d - queryArg
	// ==========================================================================

	@Test void d01_queryArg_emptyName_castableSingleValue() {
		try (var r = req().queryArg("", BasicPart.of("x", "1"), null, null, false)) {
			assertEquals("1", r.getQueryData().get("x").orElseThrow().getValue());
		}
	}

	@Test void d02_queryArg_starName_partList() {
		try (var r = req().queryArg("*", PartList.of(BasicPart.of("y", "2")), null, null, false)) {
			assertEquals("2", r.getQueryData().get("y").orElseThrow().getValue());
		}
	}

	@Test void d03_queryArg_emptyName_collection() {
		try (var r = req().queryArg("", List.of(BasicPart.of("z", "3")), null, null, false)) {
			assertEquals("3", r.getQueryData().get("z").orElseThrow().getValue());
		}
	}

	@Test void d04_queryArg_emptyName_array() {
		try (var r = req().queryArg("", new NameValuePair[]{BasicPart.of("w", "4")}, null, null, false)) {
			assertEquals("4", r.getQueryData().get("w").orElseThrow().getValue());
		}
	}

	@Test void d05_queryArg_emptyName_map() {
		try (var r = req().queryArg("", Map.of("m", "5"), null, null, false)) {
			assertEquals("5", r.getQueryData().get("m").orElseThrow().getValue());
		}
	}

	@Test void d06_queryArg_emptyName_bean() {
		try (var r = req().queryArg("", new NameBean(), null, null, false)) {
			assertEquals("bar", r.getQueryData().get("foo").orElseThrow().getValue());
		}
	}

	@Test void d07_queryArg_emptyName_customValue_delegatesToQueryCustom() throws Exception {
		try (var r = req().queryArg("", "foo=bar&baz=qux", null, null, false); var res = r.run()) {
			assertEquals(200, res.getStatusCode());
		}
		// Not asserted further here: queryCustom()'s effect on the URI is only realized once run() rebuilds it via
		// uriBuilder -- reaching this line without throwing already proves the "custom" dispatch branch executed.
	}

	@Test void d08_queryArg_emptyName_skipIfEmpty_removesEmptyValueEntries() {
		try (var r = req().queryArg("", List.of(BasicPart.of("e", "")), null, null, true)) {
			assertTrue(r.getQueryData().get("e").isEmpty(), "Empty-valued entry should have been removed by skipIfEmpty");
		}
	}

	@Test void d09_queryArg_singleValue_skipIfEmpty_emptyValue_isNoOp() {
		try (var r = req().queryArg("k", "", null, null, true)) {
			assertTrue(r.getQueryData().get("k").isEmpty());
		}
	}

	@Test void d10_queryArg_singleValue_normal() {
		try (var r = req().queryArg("k", "v", null, null, false)) {
			assertEquals("v", r.getQueryData().get("k").orElseThrow().getValue());
		}
	}
}
