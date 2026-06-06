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
package org.apache.juneau.microservice.console;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.config.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.testing.annotations.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link ConfigCommand} {@code execute()} branches.
 *
 * <p>The {@code config} command is exercised through {@link Microservice#executeCommand(String, String, Object...)}
 * which captures the command's PrintWriter output as a String — this is the same harness that the live console
 * thread uses, so the tests cover the real production path without needing a {@code System.in}/{@code System.out}
 * mock.
 */
@JettyMicroserviceTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfigCommand_Test extends TestBase {

	private Microservice microservice;

	@BeforeAll
	void bootMicroservice() throws Exception {
		// Build a config seeded with two known keys so we can exercise get/set/remove paths.
		var config = Config.create().build();
		config.set("Section/foo", "bar");
		config.set("Section/baz", "qux");

		// consoleEnabled(true) is needed for the builder's consoleCommands to be registered into the
		// command map. The supplied Scanner/PrintWriter satisfy the System.console() fallback path so
		// the constructor doesn't try to grab the real STDIN/STDOUT.
		microservice = Microservice.create()
			.config(config)
			.consoleEnabled(true)
			.console(new Scanner(""), new PrintWriter(new StringWriter()))
			.consoleCommands(new ConfigCommand())
			.build();
	}

	@AfterAll
	void stopMicroservice() throws Exception {
		if (microservice != null)
			microservice.stop();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// 'config get <key>' branch.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_get_existingKey_printsValue() {
		var out = microservice.executeCommand("config", "", "get", "Section/foo");
		assertTrue(out.contains("bar"), () -> "Expected value 'bar' in output, got: " + out);
	}

	@Test void a02_get_unknownKey_printsKeyNotFound() {
		var out = microservice.executeCommand("config", "", "get", "Section/missing");
		// Resource bundle KeyNotFound message: "key ''{0}'' is not found in current configuration"
		assertTrue(out.contains("not found"), () -> "Expected 'not found' in output, got: " + out);
		assertTrue(out.contains("Section/missing"), () -> "Expected key name in output, got: " + out);
	}

	@Test void a03_get_tooManyArguments_printsTooManyArguments() {
		var out = microservice.executeCommand("config", "", "get", "Section/foo", "extra");
		assertTrue(out.contains("Too many arguments"), () -> "Expected 'Too many arguments' in output, got: " + out);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// 'config set <key> <value>' branch.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_set_validKeyValue_printsConfigSet() {
		var out = microservice.executeCommand("config", "", "set", "Section/newKey", "newValue");
		assertTrue(out.contains("Configuration updated"), () -> "Expected 'Configuration updated' in output, got: " + out);
		// And the underlying config really did get updated.
		assertEquals("newValue", microservice.getConfig().get("Section/newKey").orElse(null));
	}

	@Test void b02_set_missingValue_printsInvalidArguments() {
		// 'config set key' is size==3 (less than 4) → InvalidArguments branch.
		var out = microservice.executeCommand("config", "", "set", "Section/foo");
		assertTrue(out.contains("Invalid or missing arguments"), () -> "Expected 'Invalid or missing arguments' in output, got: " + out);
	}

	@Test void b03_set_tooManyArguments_printsTooManyArguments() {
		// 'config set key value extra' is size==5 → TooManyArguments branch.
		var out = microservice.executeCommand("config", "", "set", "Section/foo", "value", "extra");
		assertTrue(out.contains("Too many arguments"), () -> "Expected 'Too many arguments' in output, got: " + out);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// 'config remove <key>' branch.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_remove_existingKey_printsConfigRemove() {
		// Seed a key first so we can remove it without polluting other tests.
		microservice.getConfig().set("Section/removable", "tmp");
		var out = microservice.executeCommand("config", "", "remove", "Section/removable");
		assertTrue(out.contains("was removed"), () -> "Expected 'was removed' in output, got: " + out);
		assertTrue(out.contains("Section/removable"), () -> "Expected key name in output, got: " + out);
	}

	@Test void c02_remove_unknownKey_printsKeyNotFound() {
		var out = microservice.executeCommand("config", "", "remove", "Section/doesNotExist");
		assertTrue(out.contains("not found"), () -> "Expected 'not found' in output, got: " + out);
	}

	@Test void c03_remove_tooManyArguments_printsTooManyArguments() {
		var out = microservice.executeCommand("config", "", "remove", "Section/foo", "extra");
		assertTrue(out.contains("Too many arguments"), () -> "Expected 'Too many arguments' in output, got: " + out);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Unknown option / not enough args.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_unknownOption_printsInvalidArguments() {
		var out = microservice.executeCommand("config", "", "frobnicate", "Section/foo");
		assertTrue(out.contains("Invalid or missing arguments"), () -> "Expected 'Invalid or missing arguments' in output, got: " + out);
	}

	@Test void d02_noArgs_printsInvalidArguments() {
		// Just 'config' (size==1) hits the outer-else InvalidArguments branch.
		var out = microservice.executeCommand("config", "");
		assertTrue(out.contains("Invalid or missing arguments"), () -> "Expected 'Invalid or missing arguments' in output, got: " + out);
	}

	@Test void d03_singleArg_printsInvalidArguments() {
		// 'config get' (size==2, still ≤2) hits the outer-else InvalidArguments branch.
		var out = microservice.executeCommand("config", "", "get");
		assertTrue(out.contains("Invalid or missing arguments"), () -> "Expected 'Invalid or missing arguments' in output, got: " + out);
	}

	@Test void d04_nullInput_doesNotThrow() {
		// Regression: Microservice.executeCommand documents `input` "Can be null" but previously
		// passed null straight to `new Scanner(null)`, throwing NPE. Should be tolerant of null.
		var out = microservice.executeCommand("config", null, "get", "Section/foo");
		assertTrue(out.contains("bar"), () -> "Expected 'bar' in output, got: " + out);
	}
}
