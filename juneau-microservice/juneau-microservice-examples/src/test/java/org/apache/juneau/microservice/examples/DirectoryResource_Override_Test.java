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
package org.apache.juneau.microservice.examples;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;

import org.apache.juneau.*;
import org.apache.juneau.config.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

/**
 * Pins the {@code ?method=} overrides {@link DirectoryResource} accepts with its own allow-list, which the other
 * DirectoryResource tests replace with {@code "*"}.
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class DirectoryResource_Override_Test extends TestBase {

	@TempDir
	static Path tempDir;

	static Path rootDir;

	@BeforeAll
	static void setup() throws Exception {
		rootDir = tempDir.resolve("root");
		Files.createDirectories(rootDir);
	}

	@BeforeEach
	void writeFile() throws Exception {
		Files.writeString(rootDir.resolve("inside.txt"), "INSIDE_ROOT");
	}

	static Config buildConfig() {
		var cfg = Config.create().memStore().build();
		cfg.set("DirectoryResource." + "rootDir.s", rootDir.toString());
		cfg.set("DirectoryResource." + "allowViews.b", "true");
		cfg.set("DirectoryResource." + "allowUploads.b", "true");
		cfg.set("DirectoryResource." + "allowDeletes.b", "true");
		return cfg;
	}

	/** Keeps the allow-list declared on {@link DirectoryResource}. */
	public static class DefaultDirResource extends DirectoryResource {
		private static final long serialVersionUID = 1L;
		public DefaultDirResource() { super(buildConfig()); }
	}

	/** Admits every override. */
	@Rest(allowedMethodParams="*")
	public static class StarDirResource extends DirectoryResource {
		private static final long serialVersionUID = 1L;
		public StarDirResource() { super(buildConfig()); }
	}

	private static MockRestClient buildClient(Class<?> c) {
		return MockRestClient.create(c).disableRedirectHandling().ignoreErrors().noTrace().build();
	}

	@Test void a01_viewLink_dispatches() throws Exception {
		buildClient(DefaultDirResource.class).get("/inside.txt?method=VIEW").run()
			.assertStatus(200)
			.assertContent().is("INSIDE_ROOT");
	}

	@Test void a02_downloadLink_dispatches() throws Exception {
		buildClient(DefaultDirResource.class).get("/inside.txt?method=DOWNLOAD").run()
			.assertStatus(200)
			.assertContent().is("INSIDE_ROOT");
	}

	@Test void a03_deleteNotAdmitted_plainGet() throws Exception {
		buildClient(DefaultDirResource.class).get("/inside.txt?method=DELETE").run().assertStatus(200);
		assertTrue(Files.exists(rootDir.resolve("inside.txt")));
	}

	@Test void a04_deleteAdmitted_refused() throws Exception {
		buildClient(StarDirResource.class).get("/inside.txt?method=DELETE").run().assertStatus(405);
		assertTrue(Files.exists(rootDir.resolve("inside.txt")));
		// Not vacuous: the same request on a real DELETE line removes the file.
		buildClient(StarDirResource.class).delete("/inside.txt").run().assertStatus(303);
		assertFalse(Files.exists(rootDir.resolve("inside.txt")));
	}
}
