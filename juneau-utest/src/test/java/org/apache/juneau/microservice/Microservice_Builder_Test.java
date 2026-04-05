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
package org.apache.juneau.microservice;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.config.*;
import org.apache.juneau.microservice.console.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link Microservice.Builder} methods.
 */
class Microservice_Builder_Test extends TestBase {

	@Test void a01_create() {
		var b = Microservice.create();
		assertNotNull(b);
	}

	@Test void a02_getInstance_noInstance() {
		// getInstance() returns null if no microservice is running
		// Don't rely on value being null since another test might have set it
		assertDoesNotThrow(() -> Microservice.getInstance());
	}

	@Test void a03_builder_args_array() {
		var b = Microservice.create().args("--port", "8080");
		assertNotNull(b.args);
		assertDoesNotThrow(() -> b.args.get("port"));
	}

	@Test void a04_builder_args_object() {
		var args = new Args(new String[]{"--host", "localhost"});
		var b = Microservice.create().args(args);
		assertSame(args, b.args);
	}

	@Test void a05_builder_configName() {
		assertBean(Microservice.create().configName("myapp.cfg"), "configName", "myapp.cfg");
	}

	@Test void a06_builder_consoleEnabled() {
		assertBean(Microservice.create().consoleEnabled(true), "consoleEnabled", "true");
		assertBean(Microservice.create().consoleEnabled(false), "consoleEnabled", "false");
	}

	@Test void a07_builder_consoleCommands_instances() {
		var cmd = new ExitCommand();
		var b = Microservice.create().consoleCommands(cmd);
		assertTrue(b.consoleCommands.contains(cmd));
	}

	@SuppressWarnings({
		"unchecked" // Generic array created for varargs parameter of Class<? extends ConsoleCommand>
	})
	@Test void a08_builder_consoleCommands_classes() throws Exception {
		var b = Microservice.create().consoleCommands(HelpCommand.class);
		assertFalse(b.consoleCommands.isEmpty());
		assertEquals("help", b.consoleCommands.get(0).getName());
	}

	@Test void a09_builder_listener() {
		var listener = new BasicMicroserviceListener();
		var b = Microservice.create().listener(listener);
		assertSame(listener, b.listener);
	}

	@Test void a10_builder_logConfig() {
		var lc = LogConfig.create().logFile("app.log");
		var b = Microservice.create().logConfig(lc);
		assertSame(lc, b.logConfig);
	}

	@Test void a11_builder_logger() {
		var logger = Logger.getLogger("test.logger");
		var b = Microservice.create().logger(logger);
		assertSame(logger, b.logger);
	}

	@Test void a12_builder_console() {
		var reader = new java.util.Scanner(System.in);
		var writer = new PrintWriter(System.out);
		var b = Microservice.create().console(reader, writer);
		assertSame(reader, b.consoleReader);
		assertSame(writer, b.consoleWriter);
	}

	@Test void a13_builder_workingDir_file() {
		var dir = new File("/tmp");
		var b = Microservice.create().workingDir(dir);
		assertSame(dir, b.workingDir);
	}

	@Test void a14_builder_workingDir_string() {
		assertBean(Microservice.create().workingDir("/tmp").workingDir, "path", "/tmp");
	}

	@Test void a15_builder_copy() {
		var b = Microservice.create()
			.configName("test.cfg")
			.consoleEnabled(true);
		var copy = b.copy();
		assertNotSame(b, copy);
		assertBean(copy, "configName,consoleEnabled", "test.cfg,true");
	}

	@Test void a16_builder_resolveFile_absolutePath() {
		assertBean(Microservice.create().resolveFile("/absolute/path/to/file.cfg"), "absolute,path", "true,/absolute/path/to/file.cfg");
	}

	@Test void a17_builder_resolveFile_relativePath_noWorkingDir() {
		assertBean(Microservice.create().resolveFile("relative/path.cfg"), "absolute,path", "false,relative/path.cfg");
	}

	@Test void a18_builder_resolveFile_relativePath_withWorkingDir() {
		var b = Microservice.create().workingDir("/tmp");
		var f = b.resolveFile("relative/path.cfg");
		assertTrue(f.getPath().contains("relative/path.cfg"));
	}

	@Test void a19_builder_config() {
		var config = Config.create().build();
		var b = Microservice.create().config(config);
		assertSame(config, b.config);
	}

	@Test void a20_builder_manifest_null() throws Exception {
		var b = Microservice.create().manifest((Object)null);
		assertNull(b.manifest);
	}

	@Test void a21_builder_manifest_class() throws Exception {
		var b = Microservice.create().manifest(Microservice.class);
		// ManifestFile created from class - just verify no exception
		assertNotNull(b);
	}

	@Test void a22_builder_manifest_invalidType() {
		assertThrows(RuntimeException.class, () -> Microservice.create().manifest(42));
	}

	@Test void a23_builder_copyWithLogConfig() throws Exception {
		var lc = LogConfig.create().logFile("test.log").count(3);
		var b = Microservice.create().logConfig(lc);
		var copy = b.copy();
		assertNotSame(lc, copy.logConfig);
		assertBean(copy.logConfig, "logFile,count", "test.log,3");
	}

	@Test void a24_builder_copyWithNullLogConfig() {
		var b = Microservice.create(); // logConfig is null
		var copy = b.copy();
		assertNull(copy.logConfig);
	}
}
