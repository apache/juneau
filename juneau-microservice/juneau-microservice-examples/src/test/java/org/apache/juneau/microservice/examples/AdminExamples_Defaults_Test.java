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

import java.io.*;
import java.nio.file.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.config.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

/**
 * Tests the safe defaults of the admin examples: deletes off, and a dedicated directory instead of the working directory.
 */
@SuppressWarnings({
	"resource" // MockRestClient is an in-memory test double that holds no OS resource.
})
class AdminExamples_Defaults_Test extends TestBase {

	@TempDir
	static Path tempDir;

	@Rest
	public static class TestLogsResource extends LogsResource {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_logsDeleteIsOffByDefault() throws Exception {
		var logDir = tempDir.resolve("logs-default");
		Files.createDirectories(logDir);
		var f = logDir.resolve("keep.log");
		Files.writeString(f, "[2024.01.01 10:00:00 INFO] keep\n");
		var cfg = Config.create().name("AdminExamples_Defaults_Test_logs.cfg").memStore().build();
		cfg.set("Logging/logDir", logDir.toString());
		var overlay = new BasicBeanStore().addBean(Config.class, cfg);
		var c = MockRestClient.create(TestLogsResource.class).overridingBeanStore(overlay).disableRedirectHandling().ignoreErrors().noTrace().build();
		c.delete("/keep.log").run().assertStatus(405);
		assertTrue(Files.exists(f));
	}

	@Test void a02_directoryRootDefaultsToDedicatedDirectory() {
		var d = new DirectoryResource(Config.create().name("AdminExamples_Defaults_Test_dir.cfg").memStore().build());
		assertEquals(new File("shared-files"), d.getRootDir());
	}
}
