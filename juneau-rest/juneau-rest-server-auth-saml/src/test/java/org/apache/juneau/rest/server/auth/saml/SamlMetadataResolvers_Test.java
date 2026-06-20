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
package org.apache.juneau.rest.server.auth.saml;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Tests for {@link SamlMetadataResolvers} — null-arg guards, missing file, and the path overload.
 *
 * <p>
 * The live HTTP and happy-path file paths require OpenSAML to be fully initialized with a real
 * metadata XML document; those are tested in the validator integration tests.  This class covers
 * the defensive guard branches that execute before any OpenSAML call.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S5778"  // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
})
class SamlMetadataResolvers_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// A: file(File) — null and missing-file guards.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_file_nullArg_throws() {
		var ex = assertThrows(IllegalArgumentException.class, () -> SamlMetadataResolvers.file((File) null));
		assertTrue(ex.getMessage().contains("null"));
	}

	@Test void a02_file_nonExistentFile_throwsFileNotFoundException() {
		var f = new File("/non-existent/path/metadata.xml");
		assertThrows(FileNotFoundException.class, () -> SamlMetadataResolvers.file(f));
	}

	@Test void a03_file_directory_throwsFileNotFoundException() throws Exception {
		var dir = Files.createTempDirectory("smr-test").toFile();
		dir.deleteOnExit();
		assertThrows(FileNotFoundException.class, () -> SamlMetadataResolvers.file(dir));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: url(String) — null guard.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_url_nullArg_throws() {
		var ex = assertThrows(IllegalArgumentException.class, () -> SamlMetadataResolvers.url(null));
		assertTrue(ex.getMessage().contains("null"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: file(Path) — null guard and delegation.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_filePath_nullArg_throws() {
		var ex = assertThrows(IllegalArgumentException.class, () -> SamlMetadataResolvers.file((Path) null));
		assertTrue(ex.getMessage().contains("null"));
	}

	@Test void c02_filePath_nonExistentPath_throwsFileNotFoundException() {
		var p = Path.of("/non-existent/path/metadata.xml");
		assertThrows(FileNotFoundException.class, () -> SamlMetadataResolvers.file(p));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: url(String) — HTTP server returns non-2xx → IOException (SamlMetadataResolvers.java line 125)
	// -----------------------------------------------------------------------------------------------------------------

	@SuppressWarnings({
		"resource" // HttpServer held as local fixture; stopped in finally block
	})
	@Test void d01_url_nonSuccessStatus_throwsIOException() throws Exception {
		var server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/metadata", ex -> {
			var body = "Not Found".getBytes(StandardCharsets.UTF_8);
			ex.sendResponseHeaders(404, body.length);
			try (var os = ex.getResponseBody()) {
				os.write(body);
			}
		});
		server.start();
		try {
			var url = "http://127.0.0.1:" + server.getAddress().getPort() + "/metadata";
			assertThrows(IOException.class, () -> SamlMetadataResolvers.url(url));
		} finally {
			server.stop(0);
		}
	}
}
