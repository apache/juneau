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
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link ConsoleCommand} and its concrete subclasses.
 */
class ConsoleCommand_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// ConsoleCommand abstract class - default method behavior
	//-----------------------------------------------------------------------------------------------------------------

	static class MinimalCommand extends ConsoleCommand {
		@Override
		public boolean execute(Scanner in, PrintWriter out, Args args) { return false; }
		@Override
		public String getName() { return "minimal"; }
	}

	@Test void a01_consoleCommand_defaultDescription() {
		assertNull(new MinimalCommand().getDescription());
	}

	@Test void a02_consoleCommand_defaultExamples() {
		assertNull(new MinimalCommand().getExamples());
	}

	@Test void a03_consoleCommand_defaultInfo() {
		assertNull(new MinimalCommand().getInfo());
	}

	@Test void a04_consoleCommand_defaultSynopsis() {
		// Default synopsis returns getName()
		var cmd = new MinimalCommand();
		assertEquals("minimal", cmd.getSynopsis());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// ExitCommand
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_exitCommand_getName() {
		assertEquals("exit", new ExitCommand().getName());
	}

	@Test void b02_exitCommand_getInfo() {
		var info = new ExitCommand().getInfo();
		assertNotNull(info);
		assertFalse(info.isEmpty());
	}

	@Test void b03_exitCommand_getDescription() {
		var desc = new ExitCommand().getDescription();
		assertNotNull(desc);
		assertFalse(desc.isEmpty());
	}

	@Test void b04_exitCommand_getSynopsis() {
		// Default synopsis is the command name
		assertEquals("exit", new ExitCommand().getSynopsis());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RestartCommand
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_restartCommand_getName() {
		assertEquals("restart", new RestartCommand().getName());
	}

	@Test void c02_restartCommand_getInfo() {
		var info = new RestartCommand().getInfo();
		assertNotNull(info);
		assertFalse(info.isEmpty());
	}

	@Test void c03_restartCommand_getDescription() {
		var desc = new RestartCommand().getDescription();
		assertNotNull(desc);
		assertFalse(desc.isEmpty());
	}

	@Test void c04_restartCommand_getSynopsis() {
		assertEquals("restart", new RestartCommand().getSynopsis());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HelpCommand
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_helpCommand_getName() {
		assertEquals("help", new HelpCommand().getName());
	}

	@Test void d02_helpCommand_getInfo() {
		var info = new HelpCommand().getInfo();
		assertNotNull(info);
		assertFalse(info.isEmpty());
	}

	@Test void d03_helpCommand_getDescription() {
		var desc = new HelpCommand().getDescription();
		assertNotNull(desc);
		assertFalse(desc.isEmpty());
	}

	@Test void d04_helpCommand_getExamples() {
		var examples = new HelpCommand().getExamples();
		assertNotNull(examples);
		assertFalse(examples.isEmpty());
	}

	@Test void d05_helpCommand_getSynopsis() {
		assertEquals("help [command]", new HelpCommand().getSynopsis());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// ConfigCommand
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_configCommand_getName() {
		assertEquals("config", new ConfigCommand().getName());
	}

	@Test void e02_configCommand_getInfo() {
		var info = new ConfigCommand().getInfo();
		assertNotNull(info);
		assertFalse(info.isEmpty());
	}

	@Test void e03_configCommand_getDescription() {
		var desc = new ConfigCommand().getDescription();
		assertNotNull(desc);
		assertFalse(desc.isEmpty());
	}

	@Test void e04_configCommand_getSynopsis() {
		var synopsis = new ConfigCommand().getSynopsis();
		assertNotNull(synopsis);
		assertTrue(synopsis.contains("config"));
	}
}
