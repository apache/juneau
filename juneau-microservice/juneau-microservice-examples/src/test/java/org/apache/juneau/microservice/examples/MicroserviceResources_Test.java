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
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.config.*;
import org.apache.juneau.microservice.examples.ShutdownResource.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

/**
 * Tests for {@link DirectoryResource}, {@link LogsResource}, and {@link ShutdownResource}
 * covering normal REST operations via {@link MockRestClient}.
 */
// Tests intentionally leave resources open; try-with-resources would obscure the test intent.
@SuppressWarnings({
	"resource"  // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class MicroserviceResources_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// DirectoryResource tests
	//-----------------------------------------------------------------------------------------------------------------

	@TempDir
	static Path dirTempDir;

	static Path dirRoot;

	/** Every exit the shutdown resource under test asks for; nothing here may stop the test JVM. */
	final BlockingQueue<Integer> exits = new LinkedBlockingQueue<>();

	@BeforeAll
	static void setup() throws Exception {
		setupDir();
		setupLogs();
	}

	static void setupDir() throws Exception {
		dirRoot = dirTempDir.resolve("dir-root");
		Files.createDirectories(dirRoot);
		Files.writeString(dirRoot.resolve("file1.txt"), "content1");
		Files.writeString(dirRoot.resolve("file2.txt"), "content2");
		var sub = dirRoot.resolve("subdir");
		Files.createDirectories(sub);
		Files.writeString(sub.resolve("nested.txt"), "nested-content");
	}

	@Rest(allowedMethodParams="*")
	public static class TestDirResource extends DirectoryResource {
		private static final long serialVersionUID = 1L;

		public TestDirResource() {
			super(buildConfig());
		}

		private static Config buildConfig() {
			var cfg = Config.create().memStore().build();
			cfg.set("DirectoryResource." + "rootDir.s", dirRoot.toString());
			cfg.set("DirectoryResource." + "allowViews.b", "true");
			cfg.set("DirectoryResource." + "allowUploads.b", "true");
			cfg.set("DirectoryResource." + "allowDeletes.b", "false");
			return cfg;
		}
	}

	private static MockRestClient buildDirClient() {
		return MockRestClient.buildLax(TestDirResource.class);
	}

	@Test void a01_directoryResource_getRootListing() throws Exception {
		try (var c = buildDirClient()) {
			var resp = c.get("/").run();
			resp.assertStatus(200);
			var body = resp.getContent().asString();
			assertTrue(body.contains("file1.txt"), "Root listing should contain file1.txt");
			assertTrue(body.contains("file2.txt"), "Root listing should contain file2.txt");
			assertTrue(body.contains("subdir"), "Root listing should contain subdir");
		}
	}

	@Test void a02_directoryResource_viewFile() throws Exception {
		try (var c = buildDirClient()) {
			c.request("VIEW", "/file1.txt").run()
				.assertStatus(200)
				.assertContent().is("content1");
		}
	}

	@Test void a03_directoryResource_downloadFile() throws Exception {
		try (var c = buildDirClient()) {
			c.request("DOWNLOAD", "/file1.txt").run()
				.assertStatus(200)
				.assertContent().is("content1");
		}
	}

	@Test void a04_directoryResource_getSubdirListing() throws Exception {
		try (var c = buildDirClient()) {
			var resp = c.get("/subdir").run();
			resp.assertStatus(200);
			var body = resp.getContent().asString();
			assertTrue(body.contains("nested.txt"), "Subdir listing should contain nested.txt");
		}
	}

	@Test void a05_directoryResource_viewNestedFile() throws Exception {
		try (var c = buildDirClient()) {
			c.request("VIEW", "/subdir/nested.txt").run()
				.assertStatus(200)
				.assertContent().is("nested-content");
		}
	}

	@Test void a06_directoryResource_viewNotFound() throws Exception {
		try (var c = buildDirClient()) {
			c.request("VIEW", "/nonexistent.txt").run()
				.assertStatus(404);
		}
	}

	@Test void a07_directoryResource_deleteDisabled() throws Exception {
		try (var c = buildDirClient()) {
			c.delete("/file1.txt").run()
				.assertStatus(405);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// LogsResource tests
	//-----------------------------------------------------------------------------------------------------------------

	@TempDir
	static Path logsTempDir;

	static Path logsRoot;

	static void setupLogs() throws Exception {
		logsRoot = logsTempDir.resolve("logs-root");
		Files.createDirectories(logsRoot);
		Files.writeString(logsRoot.resolve("app.log"), "2024.01.01 10:00:00 INFO Test log entry\n");
		Files.writeString(logsRoot.resolve("error.log"), "2024.01.01 11:00:00 SEVERE Error occurred\n");
	}

	@Rest
	public static class TestLogsResource extends LogsResource {
		private static final long serialVersionUID = 1L;
	}

	private static MockRestClient buildLogsClient() {
		var cfg = Config.create().memStore().build();
		cfg.set("Logging/logDir", logsRoot.toString());
		cfg.set("Logging/allowDeletes", "true");
		var overlay = new BasicBeanStore().addBean(Config.class, cfg);
		return MockRestClient.create(TestLogsResource.class)
			.overridingBeanStore(overlay)
			.disableRedirectHandling()
			.ignoreErrors()
			.noTrace()
			.build();
	}

	@Test void b01_logsResource_getRootListing() throws Exception {
		try (var c = buildLogsClient()) {
			var resp = c.get("/").run();
			resp.assertStatus(200);
			var body = resp.getContent().asString();
			assertTrue(body.contains("app.log"), "Log listing should contain app.log");
			assertTrue(body.contains("error.log"), "Log listing should contain error.log");
		}
	}

	@Test void b02_logsResource_viewFile() throws Exception {
		try (var c = buildLogsClient()) {
			c.get("/view/app.log").run()
				.assertStatus(200);
		}
	}

	@Test void b03_logsResource_downloadFile() throws Exception {
		try (var c = buildLogsClient()) {
			c.get("/download/app.log").run()
				.assertStatus(200);
		}
	}

	@Test void b04_logsResource_viewNotFound() throws Exception {
		try (var c = buildLogsClient()) {
			c.get("/view/nonexistent.log").run()
				.assertStatus(404);
		}
	}

	@Test void b05_logsResource_deleteFile() throws Exception {
		// Create a temp file to delete
		var toDelete = logsRoot.resolve("deleteme.log");
		Files.writeString(toDelete, "delete me");
		try (var c = buildLogsClient()) {
			c.delete("/deleteme.log").run()
				.assertStatus(303);
			assertFalse(Files.exists(toDelete), "File should be deleted");
		}
	}

	@Test void b06_logsResource_methodParamDoesNotDelete() throws Exception {
		// The resource no longer honours ?method=, so a GET claiming DELETE is a plain listing read.
		var keep = logsRoot.resolve("keep.log");
		Files.writeString(keep, "keep me");
		try (var c = buildLogsClient()) {
			c.get("/keep.log?method=DELETE").run()
				.assertStatus(200);
			assertTrue(Files.exists(keep), "GET ?method=DELETE must not delete the file");
		}
	}

	@Test void b07_logsResource_oldCustomMethodsRemoved() throws Exception {
		try (var c = buildLogsClient()) {
			c.request("VIEW", "/app.log").run().assertStatus(404);
			c.request("DOWNLOAD", "/app.log").run().assertStatus(404);
			c.request("PARSE", "/app.log").run().assertStatus(404);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// ShutdownResource tests
	//-----------------------------------------------------------------------------------------------------------------

	private MockRestClient buildShutdownClient() {
		return shutdownClient(new ShutdownResource(exits::add));
	}

	private static MockRestClient shutdownClient(ShutdownResource r) {
		return MockRestClient.create(r)
			.ignoreErrors()
			.noTrace()
			.build();
	}

	@Test void c01_shutdownResource_instantiation() {
		assertNotNull(new ShutdownResource());
	}

	@Test void c02_shutdownResource_shutdownIsMutatingPost() throws Exception {
		var method = ShutdownResource.class.getMethod("shutdown");
		assertEquals(String.class, method.getReturnType());
		assertNotNull(method.getAnnotation(Mutating.class), "shutdown() must be declared @Mutating");
		assertNotNull(method.getAnnotation(RestPost.class), "shutdown() must be bound to POST");
	}

	@Test void c03_shutdownResource_getNotAllowed() throws Exception {
		try (var c = buildShutdownClient()) {
			c.get("/").run().assertStatus(405);
			assertNull(exits.poll(1500, TimeUnit.MILLISECONDS), "GET must not schedule an exit");
		}
	}

	@Test void c04_shutdownResource_postExits() throws Exception {
		try (var c = buildShutdownClient()) {
			c.post("/", null).run()
				.assertStatus(200)
				.assertContent().isContains("OK");
			assertEquals(0, exits.poll(10, TimeUnit.SECONDS));
		}
	}

	@Test void c05_shutdownResource_nullExitIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> new ShutdownResource(null));
	}

	@Test void c06_shutdownResource_mutatingRefusesSafeBinding() throws Exception {
		// The startup check refuses shutdown() on any safe method, so a later rebinding to GET cannot boot.
		var m = ShutdownResource.class.getMethod("shutdown");
		assertThrows(RuntimeException.class, () -> MethodSafety.checkOperation("GET", m));
		assertDoesNotThrow(() -> MethodSafety.checkOperation("POST", m));
	}

	@Test void c07_shutdownResource_exitFromBeanStore() throws Exception {
		// A child resource is built by the bean instantiator, which takes the Exit the bean store holds.  Read back
		// rather than shut down, so a regression cannot stop the test JVM.
		Exit e = exits::add;
		var r = BeanInstantiator.of(ShutdownResource.class, new BasicBeanStore().addBean(Exit.class, e)).run();
		assertSame(e, exitOf(r));
		assertNotSame(e, exitOf(BeanInstantiator.of(ShutdownResource.class, new BasicBeanStore()).run()));
	}

	@SuppressWarnings({
		"java:S3011" // Reads the private exit back so the test can check which constructor ran without shutting down.
	})
	private static Object exitOf(ShutdownResource r) throws Exception {
		var f = ShutdownResource.class.getDeclaredField("exit");
		f.setAccessible(true);
		return f.get(r);
	}

	@Test void c08_shutdownResource_instancesDoNotShareAnExit() throws Exception {
		var other = new LinkedBlockingQueue<Integer>();
		try (var c = shutdownClient(new ShutdownResource(other::add))) {
			c.post("/", null).run().assertStatus(200);
			assertEquals(0, other.poll(10, TimeUnit.SECONDS));
			assertNull(exits.poll(1500, TimeUnit.MILLISECONDS), "Another instance's shutdown must not use this one's exit");
		}
	}
}
