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
import org.apache.juneau.commons.runtime.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.testing.annotations.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link HelpCommand} {@code execute()} branches.
 *
 * <p>The {@code help} command is exercised through {@link Microservice#executeCommand(String, String, Object...)}
 * which captures the command's PrintWriter output as a String — this lets us assert against the exact text the
 * console thread would print without spinning up a real STDIN/STDOUT.
 */
@JettyMicroserviceTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HelpCommand_Test extends TestBase {

	/**
	 * A minimal command with NO description / synopsis / examples / info — exercises the {@code nn(...)} false
	 * branches in {@link HelpCommand} (so we don't print SYNOPSIS / DESCRIPTION / EXAMPLES blocks for it).
	 *
	 * <p>The default {@link ConsoleCommand#getSynopsis()} returns {@link #getName()}, which IS non-null, so to
	 * really exercise the synopsis-null branch we override it to return null.
	 */
	static class BareCommand extends ConsoleCommand {
		@Override public boolean execute(Scanner in, PrintWriter out, Args args) { return false; }
		@Override public String getName() { return "bare"; }
		@Override public String getSynopsis() { return null; }
	}

	private Microservice microservice;

	@BeforeAll
	void bootMicroservice() throws Exception {
		// consoleEnabled(true) is needed for the builder's consoleCommands to be registered into the
		// command map. The supplied Scanner/PrintWriter satisfy the System.console() fallback path so
		// the constructor doesn't try to grab the real STDIN/STDOUT.
		microservice = Microservice.create()
			.consoleEnabled(true)
			.console(new Scanner(""), new PrintWriter(new StringWriter()))
			.consoleCommands(new HelpCommand(), new ExitCommand(), new RestartCommand(), new BareCommand())
			.build();
	}

	@AfterAll
	void stopMicroservice() throws Exception {
		if (microservice != null)
			microservice.stop();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// 'help' (no argument) — list-all-commands branch.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_help_noArgs_printsListOfAvailableCommands() {
		var out = microservice.executeCommand("help", "");
		assertTrue(out.contains("List of available commands"), () -> "Expected list header in output, got: " + out);
	}

	@Test void a02_help_noArgs_includesEachRegisteredCommand() {
		var out = microservice.executeCommand("help", "");
		// Each registered command's name should appear in the listing.
		assertTrue(out.contains("help"), () -> "Expected 'help' name in output, got: " + out);
		assertTrue(out.contains("exit"), () -> "Expected 'exit' name in output, got: " + out);
		assertTrue(out.contains("restart"), () -> "Expected 'restart' name in output, got: " + out);
		assertTrue(out.contains("bare"), () -> "Expected 'bare' name in output, got: " + out);
	}

	@Test void a03_help_noArgs_includesCommandInfo() {
		var out = microservice.executeCommand("help", "");
		// HelpCommand.info / ExitCommand.info / RestartCommand.info come from the message bundle.
		assertTrue(out.contains("Commands help"), () -> "Expected 'Commands help' info text in output, got: " + out);
		assertTrue(out.contains("Shut down service"), () -> "Expected 'Shut down service' info text in output, got: " + out);
		assertTrue(out.contains("Restarts service"), () -> "Expected 'Restarts service' info text in output, got: " + out);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// 'help <command>' — detail-for-named-command branch (full bundle: NAME / SYNOPSIS / DESCRIPTION / EXAMPLES).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_help_namedCommand_printsNameSection() {
		var out = microservice.executeCommand("help", "", "help");
		assertTrue(out.contains("NAME"), () -> "Expected NAME header in output, got: " + out);
		assertTrue(out.contains("help"), () -> "Expected command name in output, got: " + out);
	}

	@Test void b02_help_namedCommand_printsSynopsisSection() {
		// HelpCommand has a non-null synopsis ("help [command]") so the SYNOPSIS branch fires.
		var out = microservice.executeCommand("help", "", "help");
		assertTrue(out.contains("SYNOPSIS"), () -> "Expected SYNOPSIS header in output, got: " + out);
		assertTrue(out.contains("help [command]"), () -> "Expected synopsis text in output, got: " + out);
	}

	@Test void b03_help_namedCommand_printsDescriptionSection() {
		var out = microservice.executeCommand("help", "", "help");
		assertTrue(out.contains("DESCRIPTION"), () -> "Expected DESCRIPTION header in output, got: " + out);
		// Description in the bundle starts with "When called without arguments,..."
		assertTrue(out.contains("When called without arguments"), () -> "Expected description text in output, got: " + out);
	}

	@Test void b04_help_namedCommand_printsExamplesSection() {
		// HelpCommand has non-null examples so the EXAMPLES branch fires.
		var out = microservice.executeCommand("help", "", "help");
		assertTrue(out.contains("EXAMPLES"), () -> "Expected EXAMPLES header in output, got: " + out);
	}

	@Test void b05_help_namedCommand_includesInfoOnNameLine() {
		// info != null path: NAME line is "<name> -- <info>"
		var out = microservice.executeCommand("help", "", "help");
		assertTrue(out.contains(" -- "), () -> "Expected ' -- ' separator on NAME line, got: " + out);
		assertTrue(out.contains("Commands help"), () -> "Expected info text on NAME line, got: " + out);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// 'help <command>' — detail for a command with NO description/examples/synopsis/info (null branches).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_help_bareCommand_omitsSynopsisSection() {
		// BareCommand.getSynopsis() returns null → SYNOPSIS section is NOT printed.
		var out = microservice.executeCommand("help", "", "bare");
		assertFalse(out.contains("SYNOPSIS"), () -> "SYNOPSIS section should not appear for a command with null synopsis, got: " + out);
	}

	@Test void c02_help_bareCommand_omitsDescriptionSection() {
		// BareCommand.getDescription() defaults to null → DESCRIPTION section is NOT printed.
		var out = microservice.executeCommand("help", "", "bare");
		assertFalse(out.contains("DESCRIPTION"), () -> "DESCRIPTION section should not appear for a command with null description, got: " + out);
	}

	@Test void c03_help_bareCommand_omitsExamplesSection() {
		// BareCommand.getExamples() defaults to null → EXAMPLES section is NOT printed.
		var out = microservice.executeCommand("help", "", "bare");
		assertFalse(out.contains("EXAMPLES"), () -> "EXAMPLES section should not appear for a command with null examples, got: " + out);
	}

	@Test void c04_help_bareCommand_omitsInfoSeparatorOnNameLine() {
		// BareCommand.getInfo() defaults to null → no " -- <info>" suffix on NAME line.
		var out = microservice.executeCommand("help", "", "bare");
		// The NAME header still prints, and the bare command's name still prints, but no " -- " trailer.
		assertTrue(out.contains("NAME"), () -> "Expected NAME header in output, got: " + out);
		assertTrue(out.contains("bare"), () -> "Expected 'bare' name in output, got: " + out);
		// The output must not contain "bare -- " since info is null.
		assertFalse(out.contains("bare -- "), () -> "Did not expect ' -- ' separator after 'bare' name when info is null, got: " + out);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// 'help <unknown>' — command-not-found branch.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_help_unknownCommand_printsCommandNotFound() {
		var out = microservice.executeCommand("help", "", "doesNotExist");
		assertTrue(out.contains("Command not found"), () -> "Expected 'Command not found' in output, got: " + out);
	}
}
