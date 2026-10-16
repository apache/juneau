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
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.config.*;
import org.apache.juneau.microservice.examples.LogsResource.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

/**
 * Coverage tests for {@link LogsResource} that exercise paths not covered by the existing
 * {@code LogsResource_Action_Test} (Action class fluent setters) and {@code LogsResource_PathTraversal_Test}
 * (path-traversal hardening).
 *
 * <p>
 * Targets the {@link FileResource} bean (size, name, type, last-modified, file-filter behavior, action
 * link generation, child enumeration), the {@code GET /view/*} highlight branch (HTML severity-coloring path),
 * the {@code GET /parse/*} operation (filtered log parser output), and the {@code DELETE} disabled branch.
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class LogsResource_Coverage_Test extends TestBase {

	@TempDir
	static Path tempDir;

	static Path logRoot;

	@BeforeAll
	static void setup() throws Exception {
		logRoot = tempDir.resolve("logs-root");
		Files.createDirectories(logRoot);
		// A log file with one entry of every severity color the highlighter cares about.
		Files.writeString(logRoot.resolve("multi.log"),
				"[2024.01.01 10:00:00 INFO] info-line%n".replace("%n", "\n")
				+ "[2024.01.01 10:00:01 WARNING] warn-line\n"
				+ "[2024.01.01 10:00:02 SEVERE] severe-line\n"
				+ "[2024.01.01 10:00:03 FINE] fine-line\n"
				+ "[2024.01.01 10:00:04 CONFIG] config-line\n"
				+ "[2024.01.01 10:00:05 FINEST] finest-line\n");
		Files.writeString(logRoot.resolve("plain.log"), "[2024.01.01 11:00:00 INFO] hello\n");
		// Non-.log files must be filtered out of the FileResource children listing.
		Files.writeString(logRoot.resolve("README.txt"), "ignored");
		// Directory inside log root - should be visible in the listing.
		var sub = logRoot.resolve("sub");
		Files.createDirectories(sub);
		Files.writeString(sub.resolve("nested.log"), "[2024.01.01 12:00:00 INFO] nested\n");
		// A directory whose name is one of the reserved action prefixes.
		var view = logRoot.resolve("view");
		Files.createDirectories(view);
		Files.writeString(view.resolve("inner.log"), "[2024.01.01 12:30:00 INFO] inner\n");
	}

	/** Mounted at root so the test URLs don't need a /logs prefix. */
	@Rest
	public static class TestLogsResource extends LogsResource {
		private static final long serialVersionUID = 1L;
	}

	// Tests intentionally leave resources open; try-with-resources would obscure the test intent.
	private static MockRestClient buildClient(boolean allowDeletes) {
		var cfg = Config.create().memStore().build();
		cfg.set("Logging/logDir", logRoot.toString());
		cfg.set("Logging/allowDeletes", String.valueOf(allowDeletes));
		// Format/date-format/useStackTraceHashes branches in @RestInit:
		cfg.set("Logging/format", "[{date} {level}] {msg}%n");
		cfg.set("Logging/dateFormat", "yyyy.MM.dd hh:mm:ss");
		cfg.set("Logging/useStackTraceHashes", "false");
		var overlay = new BasicBeanStore().addBean(Config.class, cfg);
		return MockRestClient.create(TestLogsResource.class)
			.overridingBeanStore(overlay)
			.disableRedirectHandling()
			.ignoreErrors()
			.noTrace()
			.build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// A. FileResource bean - size, name, type, last-modified, action listing, child-files filter.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_fileResource_size_forRegularFile() {
		var f = logRoot.resolve("plain.log").toFile();
		var fr = new FileResource(f, "plain.log", false);
		assertEquals(f.length(), fr.getSize());
	}

	@Test void a02_fileResource_size_forDirectory_returnsCount() {
		var fr = new FileResource(logRoot.toFile(), "", false);
		// Directory size is the number of entries in listFiles() — not the byte size.
		assertEquals(logRoot.toFile().listFiles().length, fr.getSize());
	}

	@Test void a03_fileResource_type_file() {
		var fr = new FileResource(logRoot.resolve("plain.log").toFile(), "plain.log", false);
		assertEquals("file", fr.getType());
	}

	@Test void a04_fileResource_type_directory() {
		var fr = new FileResource(logRoot.toFile(), "", false);
		assertEquals("dir", fr.getType());
	}

	@Test void a05_fileResource_name_isLinkString() {
		var fr = new FileResource(logRoot.resolve("plain.log").toFile(), "plain.log", false);
		var name = fr.getName();
		assertNotNull(name);
		assertEquals("plain.log", name.getName());
	}

	@Test void a06_fileResource_lastModified_returnsDate() {
		var fr = new FileResource(logRoot.resolve("plain.log").toFile(), "plain.log", false);
		assertNotNull(fr.getLastModified());
	}

	@Test void a07_fileResource_actions_areGetRoutes_withNoDelete() {
		var fr = new FileResource(logRoot.resolve("plain.log").toFile(), "plain.log", false);
		var actions = fr.getActions().stream().collect(Collectors.toMap(LinkString::getName, a -> a.getUri().toString()));
		assertEquals("servlet:/view/plain.log", actions.get("view"));
		assertEquals("servlet:/view/plain.log?highlight=true", actions.get("highlighted"));
		assertEquals("servlet:/parse/plain.log", actions.get("parsed"));
		assertEquals("servlet:/download/plain.log", actions.get("download"));
		assertEquals(4, actions.size(), "No delete action, and nothing else");
	}

	@Test void a08_fileResource_actions_neverCarryMethodParam() {
		var fr = new FileResource(logRoot.resolve("plain.log").toFile(), "plain.log", false);
		for (var a : fr.getActions())
			assertFalse(a.getUri().toString().contains("method="), () -> "Action link must not use ?method=: " + a.getUri());
	}

	@Test void a09_fileResource_actions_emptyForDirectory() {
		var fr = new FileResource(logRoot.toFile(), "", true);
		assertTrue(fr.getActions().isEmpty(), "Directories must not advertise file-only actions");
	}

	@Test void a10_fileResource_files_returnsNullForFile() {
		var fr = new FileResource(logRoot.resolve("plain.log").toFile(), "plain.log", true);
		assertNull(fr.getFiles());
	}

	@Test void a11_fileResource_files_returnsNullWhenNotIncludingChildren() {
		var fr = new FileResource(logRoot.toFile(), "", /*includeChildren*/false);
		assertNull(fr.getFiles());
	}

	@Test void a12_fileResource_files_filtersToLogFilesAndDirs() {
		var fr = new FileResource(logRoot.toFile(), "", true);
		var children = fr.getFiles();
		assertNotNull(children);
		var childNames = children.stream().map(c -> c.getName().getName()).toList();
		// .log files and the sub-directory must be present; README.txt must be filtered out.
		assertTrue(childNames.contains("plain.log"));
		assertTrue(childNames.contains("multi.log"));
		assertTrue(childNames.contains("sub"));
		assertFalse(childNames.contains("README.txt"), "Non-.log files must be filtered by FILE_FILTER");
	}

	@Test void a13_fileResource_files_nullPath_doesNotPrefix() {
		// path=null branch in getFiles(): the constructed child path uses bare urlEncoded name.
		var fr = new FileResource(logRoot.toFile(), null, true);
		assertNotNull(fr.getFiles());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B. End-to-end REST: view with highlight, parse, DELETE-disabled, GET listing for nested directory.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_view_highlight_writesHtmlWithSeverityColors() throws Exception {
		try (var c = buildClient(true)) {
			var resp = c.get("/view/multi.log?highlight=true").run();
			resp.assertStatus(200);
			var body = resp.getContent().asString();
			// Highlighting wraps each entry in a <span style='color:...'> with one of the documented colors.
			assertTrue(body.contains("<html>"), () -> "expected HTML wrapper, got: " + body);
			assertTrue(body.contains("color:") || body.contains("color :"),
				() -> "highlight=true output should contain CSS color spans, got: " + body);
		}
	}
	@Test void b02_view_highlight_emptyFile_writesEmptyMarker() throws Exception {
		// Filter out everything via a severity that doesn't match any line.
		try (var c = buildClient(true)) {
			var resp = c.get("/view/plain.log?highlight=true&severity=NOPE").run();
			resp.assertStatus(200);
			var body = resp.getContent().asString();
			assertTrue(body.contains("[EMPTY]") || body.contains("EMPTY"),
				() -> "Empty highlight result must contain the [EMPTY] marker, got: " + body);
		}
	}
	@Test void b03_view_plain_noFilters_streamsRawReader() throws Exception {
		try (var c = buildClient(true)) {
			var resp = c.get("/view/plain.log").run();
			resp.assertStatus(200);
			assertTrue(resp.getContent().asString().contains("hello"));
		}
	}
	@Test void b04_view_plain_withFilters_usesParser() throws Exception {
		// Passing thread/start/end/loggers params triggers the filter branch in getReader(...)
		try (var c = buildClient(true)) {
			var resp = c.get("/view/multi.log?thread=NoSuchThread").run();
			resp.assertStatus(200);
		}
	}
	@Test void b05_parse_returnsParsedEntries() throws Exception {
		try (var c = buildClient(true)) {
			var resp = c.get("/parse/plain.log").run();
			resp.assertStatus(200);
		}
	}
	@Test void b06_delete_disabled_returns405() throws Exception {
		try (var c = buildClient(/*allowDeletes*/false)) {
			c.delete("/plain.log").run().assertStatus(405);
		}
	}
	@Test void b07_get_directoryListing_includesChildLogs() throws Exception {
		try (var c = buildClient(true)) {
			var resp = c.get("/").run();
			resp.assertStatus(200);
			var body = resp.getContent().asString();
			assertTrue(body.contains("plain.log"));
			assertTrue(body.contains("multi.log"));
			assertTrue(body.contains("sub"));
			// README.txt is filtered.
			assertFalse(body.contains("README.txt"), "Non-.log files must be filtered out of the listing");
		}
	}
	@Test void b08_get_subdirectoryListing() throws Exception {
		try (var c = buildClient(true)) {
			var resp = c.get("/sub").run();
			resp.assertStatus(200);
			var body = resp.getContent().asString();
			assertTrue(body.contains("nested.log"));
		}
	}
	@Test void b09_view_dateFilter_startEnd() throws Exception {
		// Exercise the start/end branches of the date-filter parsing in viewFile().
		try (var c = buildClient(true)) {
			c.get("/view/multi.log?start=2024-01-01&end=2024-12-31").run().assertStatus(200);
		}
	}
	@Test void b10_parse_dateFilter_startEnd() throws Exception {
		// Same date-filter branches in viewParsedEntries().
		try (var c = buildClient(true)) {
			c.get("/parse/multi.log?start=2024-01-01&end=2024-12-31").run().assertStatus(200);
		}
	}
	@Test void b11_view_loggers_severity_filters() throws Exception {
		try (var c = buildClient(true)) {
			c.get("/view/multi.log?loggers=Foo&severity=INFO&severity=WARNING").run().assertStatus(200);
		}
	}
	@Test void b12_download_returnsFileContents() throws Exception {
		try (var c = buildClient(true)) {
			var resp = c.get("/download/plain.log").run();
			resp.assertStatus(200);
			assertTrue(resp.getContent().asString().contains("hello"));
		}
	}
	@Test void b13_download_notFound_returns404() throws Exception {
		try (var c = buildClient(true)) {
			c.get("/download/nope.log").run().assertStatus(404);
		}
	}
	@Test void b14_delete_existing_log_returnsRedirect() throws Exception {
		// Create then delete a file via the REST surface to exercise deleteFile() with allowDeletes=true.
		var toDelete = logRoot.resolve("delete-me.log");
		Files.writeString(toDelete, "[2024.01.01 13:00:00 INFO] doomed\n");
		try (var c = buildClient(true)) {
			c.delete("/delete-me.log").run().assertStatus(303);
			assertFalse(Files.exists(toDelete));
		}
	}
	@Test void b15_actionOnDirectory_returns404() throws Exception {
		try (var c = buildClient(true)) {
			c.get("/view/sub").run().assertStatus(404);
			c.get("/parse/sub").run().assertStatus(404);
			c.get("/download/sub").run().assertStatus(404);
		}
	}
	@Test void b16_view_nestedFile() throws Exception {
		try (var c = buildClient(true)) {
			var resp = c.get("/view/sub/nested.log").run();
			resp.assertStatus(200);
			assertTrue(resp.getContent().asString().contains("nested"));
		}
	}
	@Test void b17_reservedPrefixDirectory_isNotListableButItsFilesAreReachable() throws Exception {
		try (var c = buildClient(true)) {
			c.get("/view/view").run().assertStatus(404);
			var resp = c.get("/view/view/inner.log").run();
			resp.assertStatus(200);
			assertTrue(resp.getContent().asString().contains("inner"));
		}
	}
	@Test void b18_delete_reservesNoPrefix() throws Exception {
		var doomed = logRoot.resolve("view").resolve("doomed.log");
		Files.writeString(doomed, "[2024.01.01 13:30:00 INFO] doomed\n");
		try (var c = buildClient(true)) {
			c.delete("/view/doomed.log").run().assertStatus(303);
			assertFalse(Files.exists(doomed), "DELETE /view/doomed.log deletes the file in the view directory");
			assertTrue(Files.exists(logRoot.resolve("view").resolve("inner.log")));
		}
	}
}
