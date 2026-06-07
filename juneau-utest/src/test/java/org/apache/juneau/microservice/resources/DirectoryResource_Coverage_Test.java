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
package org.apache.juneau.microservice.resources;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;

import org.apache.juneau.*;
import org.apache.juneau.config.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

/**
 * Coverage tests for {@link DirectoryResource} that exercise paths not covered by the existing
 * {@code DirectoryResource_Action_Test} (Action class fluent setters), {@code DirectoryResource_PathTraversal_Test}
 * (path-traversal hardening), and {@code MicroserviceResources_Test} (basic happy path).
 *
 * <p>
 * Targets the {@code FileResource} action-list branches (allowViews on/off, allowDeletes on/off, directories
 * always advertise no actions), the {@code PUT} (upload) operation including the disabled branch, the
 * {@code DELETE} delete-tree path, the {@code VIEW}/{@code DOWNLOAD} disabled branches, and the not-found
 * branches inside the disabled-views error path.
 */
class DirectoryResource_Coverage_Test extends TestBase {

	@TempDir
	static Path tempDir;

	static Path dirRoot;

	@BeforeAll
	static void setup() throws Exception {
		dirRoot = tempDir.resolve("dir-root");
		Files.createDirectories(dirRoot);
		Files.writeString(dirRoot.resolve("hello.txt"), "world");
		var sub = dirRoot.resolve("sub");
		Files.createDirectories(sub);
		Files.writeString(sub.resolve("nested.txt"), "nested-content");
	}

	@Rest(allowedMethodParams="*")
	public static class TestDirResource extends DirectoryResource {
		private static final long serialVersionUID = 1L;

		public TestDirResource() { super(buildConfig(true, true, true)); }

		static Config buildConfig(boolean views, boolean uploads, boolean deletes) {
			var cfg = Config.create().memStore().build();
			cfg.set(DIRECTORY_RESOURCE_rootDir, dirRoot.toString());
			cfg.set(DIRECTORY_RESOURCE_allowViews, String.valueOf(views));
			cfg.set(DIRECTORY_RESOURCE_allowUploads, String.valueOf(uploads));
			cfg.set(DIRECTORY_RESOURCE_allowDeletes, String.valueOf(deletes));
			return cfg;
		}
	}

	/** Read-only resource — views allowed, uploads/deletes disabled. */
	@Rest(allowedMethodParams="*")
	public static class ReadOnlyDirResource extends DirectoryResource {
		private static final long serialVersionUID = 1L;
		public ReadOnlyDirResource() { super(TestDirResource.buildConfig(true, false, false)); }
	}

	/** All-disabled resource — views/uploads/deletes all off. */
	@Rest(allowedMethodParams="*")
	public static class AllDisabledDirResource extends DirectoryResource {
		private static final long serialVersionUID = 1L;
		public AllDisabledDirResource() { super(TestDirResource.buildConfig(false, false, false)); }
	}

	private static MockRestClient buildClient(Class<?> resourceClass) {
		return MockRestClient.create(resourceClass)
			.disableRedirectHandling()
			.ignoreErrors()
			.noTrace()
			.build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// A. FileResource bean state from a constructed resource.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_constructor_readsRootDirFromConfig() {
		var r = new ReadOnlyDirResource();
		assertEquals(dirRoot.toFile(), r.getRootDir());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B. View / download disabled branches (allowViews=false).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_view_disabled_returns405() throws Exception {
		try (var c = buildClient(AllDisabledDirResource.class)) {
			c.request("VIEW", "/hello.txt").run().assertStatus(405);
		}
	}

	@Test void b02_download_disabled_returns405() throws Exception {
		try (var c = buildClient(AllDisabledDirResource.class)) {
			c.request("DOWNLOAD", "/hello.txt").run().assertStatus(405);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// C. Delete branches.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_delete_disabled_returns405() throws Exception {
		try (var c = buildClient(ReadOnlyDirResource.class)) {
			c.delete("/hello.txt").run().assertStatus(405);
		}
	}

	@Test void c02_delete_existing_file_returnsRedirect() throws Exception {
		var f = dirRoot.resolve("transient.txt");
		Files.writeString(f, "x");
		try (var c = buildClient(TestDirResource.class)) {
			c.delete("/transient.txt").run().assertStatus(303);
			assertFalse(Files.exists(f));
		}
	}

	@Test void c03_delete_directory_recurses() throws Exception {
		var d = dirRoot.resolve("trans-dir");
		Files.createDirectories(d);
		Files.writeString(d.resolve("a.txt"), "a");
		Files.writeString(d.resolve("b.txt"), "b");
		try (var c = buildClient(TestDirResource.class)) {
			c.delete("/trans-dir").run().assertStatus(303);
			assertFalse(Files.exists(d), "Directory and its contents must be deleted recursively");
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// D. Upload (PUT) branches.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_put_disabled_returns405() throws Exception {
		try (var c = buildClient(ReadOnlyDirResource.class)) {
			c.put("/uploaded.txt", "data").run().assertStatus(405);
		}
	}

	@Test void d02_put_creates_new_file() throws Exception {
		try (var c = buildClient(TestDirResource.class)) {
			c.put("/uploaded.txt", "uploaded-content").run().assertStatus(303);
			var f = dirRoot.resolve("uploaded.txt");
			assertTrue(Files.exists(f));
			assertEquals("uploaded-content", Files.readString(f));
			Files.deleteIfExists(f);
		}
	}

	@Test void d03_put_overwrites_existing_file() throws Exception {
		var f = dirRoot.resolve("existing.txt");
		Files.writeString(f, "original");
		try (var c = buildClient(TestDirResource.class)) {
			c.put("/existing.txt", "replaced").run().assertStatus(303);
			assertEquals("replaced", Files.readString(f));
		} finally {
			Files.deleteIfExists(f);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// E. FileResource action listing — exercise allowViews/allowDeletes branches.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_actionListing_allowed_includesViewDownload_butNotDelete() throws Exception {
		// The ReadOnlyDirResource has views=true but deletes=false, so the listing should expose
		// "view" / "download" actions but not "delete".
		try (var c = buildClient(ReadOnlyDirResource.class)) {
			var resp = c.get("/").run();
			resp.assertStatus(200);
			var body = resp.getContent().asString();
			assertTrue(body.contains("hello.txt"));
			assertTrue(body.contains("VIEW"));
			assertTrue(body.contains("DOWNLOAD"));
			assertFalse(body.contains("?method=DELETE"),
				"DELETE action must not appear when allowDeletes=false");
		}
	}

	@Test void e02_actionListing_allDisabled_doesNotIncludeFileActions() throws Exception {
		try (var c = buildClient(AllDisabledDirResource.class)) {
			var resp = c.get("/").run();
			resp.assertStatus(200);
			var body = resp.getContent().asString();
			assertFalse(body.contains("?method=VIEW"),
				"VIEW action must not appear when allowViews=false");
			assertFalse(body.contains("?method=DOWNLOAD"),
				"DOWNLOAD action must not appear when allowViews=false");
			assertFalse(body.contains("?method=DELETE"),
				"DELETE action must not appear when allowDeletes=false");
		}
	}

	@Test void e03_actionListing_fullyEnabled_includesAllActions() throws Exception {
		try (var c = buildClient(TestDirResource.class)) {
			var resp = c.get("/").run();
			resp.assertStatus(200);
			var body = resp.getContent().asString();
			assertTrue(body.contains("?method=VIEW"));
			assertTrue(body.contains("?method=DOWNLOAD"));
			assertTrue(body.contains("?method=DELETE"));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// F. View not-found branches.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_view_notFound_returns404() throws Exception {
		try (var c = buildClient(TestDirResource.class)) {
			c.request("VIEW", "/nope.txt").run().assertStatus(404);
		}
	}

	@Test void f02_download_notFound_returns404() throws Exception {
		try (var c = buildClient(TestDirResource.class)) {
			c.request("DOWNLOAD", "/nope.txt").run().assertStatus(404);
		}
	}

	@Test void f03_get_notFound_returns404() throws Exception {
		try (var c = buildClient(TestDirResource.class)) {
			c.get("/nope-dir").run().assertStatus(404);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// G. Subdirectory listing exercises path-prefixing branch in FileResource.getFiles().
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g01_subdirectory_listing_includesNestedFiles() throws Exception {
		try (var c = buildClient(TestDirResource.class)) {
			var resp = c.get("/sub").run();
			resp.assertStatus(200);
			assertTrue(resp.getContent().asString().contains("nested.txt"));
		}
	}

	@Test void g02_subdirectory_view_ofNestedFile() throws Exception {
		try (var c = buildClient(TestDirResource.class)) {
			c.request("VIEW", "/sub/nested.txt").run()
				.assertStatus(200)
				.assertContent().is("nested-content");
		}
	}
}
