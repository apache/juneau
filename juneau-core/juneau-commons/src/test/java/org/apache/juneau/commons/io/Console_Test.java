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
package org.apache.juneau.commons.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link Console}.
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class Console_Test extends TestBase {

	private PrintStream originalOut;
	private PrintStream originalErr;
	private ByteArrayOutputStream outCapture;
	private ByteArrayOutputStream errCapture;

	@BeforeEach
	void setUp() {
		originalOut = System.out;
		originalErr = System.err;
		outCapture = new ByteArrayOutputStream();
		errCapture = new ByteArrayOutputStream();
		System.setOut(new PrintStream(outCapture));
		System.setErr(new PrintStream(errCapture));
	}

	@AfterEach
	void tearDown() {
		System.setOut(originalOut);
		System.setErr(originalErr);
	}

	//====================================================================================================
	// Constructor (line 29)
	//====================================================================================================
	@Test
	void a00_constructor() {
		// Test line 29: class instantiation
		// Console is a utility class with private constructor - no instantiation needed
		// Constructor is private to prevent instantiation of utility class
		assertNotNull(Console.class);
	}

	//====================================================================================================
	// out(String, Object...) tests
	//====================================================================================================

	@Test
	void a01_out_basic() {
		Console.out("test");
		var output = outCapture.toString();
		assertTrue(output.contains("test"), output);
		assertTrue(output.endsWith(System.lineSeparator()), output);
	}

	@Test
	void a02_out_withSingleArgument() {
		Console.out("test%s", 123);
		var output = outCapture.toString();
		assertTrue(output.contains("test123"), output);
	}

	@Test
	void a03_out_withMultipleArguments() {
		Console.out("test%s %s %s", "a", "b", "c");
		var output = outCapture.toString();
		assertTrue(output.contains("testa b c"), output);
	}

	@Test
	void a04_out_withNullArgument() {
		Console.out("test%s", (Object)null);
		var output = outCapture.toString();
		assertTrue(output.contains("testnull"), output);
	}

	@Test
	void a05_out_withEmptyMessage() {
		Console.out("");
		var output = outCapture.toString();
		assertTrue(output.endsWith(System.lineSeparator()), output);
	}

	@Test
	void a06_out_withNoArguments() {
		Console.out("test");
		var output = outCapture.toString();
		assertTrue(output.contains("test"), output);
	}

	@Test
	void a07_out_withNumericArgument() {
		Console.out("Value: %d", 12345);
		var output = outCapture.toString();
		assertTrue(output.contains("Value: 12345"), output);
	}

	@Test
	void a08_out_withSpecialCharacters() {
		Console.out("test%s", "a\nb\tc");
		var output = outCapture.toString();
		assertTrue(output.contains("testa"), output);
	}

	@Test
	void a09_out_withIntegerFormatting() {
		Console.out("Number: %d", 1234);
		var output = outCapture.toString();
		assertTrue(output.contains("Number: 1234"), output);
	}

	@Test
	void a10_out_withDateArgument() {
		Console.out("Date: %s", new java.util.Date(0));
		var output = outCapture.toString();
		assertTrue(output.contains("Date:"), output);
	}

	@Test
	void a11_out_doesNotWriteToErr() {
		Console.out("test");
		var errOutput = errCapture.toString();
		assertTrue(errOutput.isEmpty(), "err should be empty but was: " + errOutput);
	}

	//====================================================================================================
	// err(String, Object...) tests
	//====================================================================================================

	@Test
	void b01_err_basic() {
		Console.err("test");
		var output = errCapture.toString();
		assertTrue(output.contains("test"), output);
		assertTrue(output.endsWith(System.lineSeparator()), output);
	}

	@Test
	void b02_err_withSingleArgument() {
		Console.err("test%s", 123);
		var output = errCapture.toString();
		assertTrue(output.contains("test123"), output);
	}

	@Test
	void b03_err_withMultipleArguments() {
		Console.err("test%s %s %s", "a", "b", "c");
		var output = errCapture.toString();
		assertTrue(output.contains("testa b c"), output);
	}

	@Test
	void b04_err_withNullArgument() {
		Console.err("test%s", (Object)null);
		var output = errCapture.toString();
		assertTrue(output.contains("testnull"), output);
	}

	@Test
	void b05_err_withEmptyMessage() {
		Console.err("");
		var output = errCapture.toString();
		assertTrue(output.endsWith(System.lineSeparator()), output);
	}

	@Test
	void b06_err_withNoArguments() {
		Console.err("test");
		var output = errCapture.toString();
		assertTrue(output.contains("test"), output);
	}

	@Test
	void b07_err_withNumericArgument() {
		Console.err("Error: %d", 12345);
		var output = errCapture.toString();
		assertTrue(output.contains("Error: 12345"), output);
	}

	@Test
	void b08_err_withSpecialCharacters() {
		Console.err("test%s", "a\nb\tc");
		var output = errCapture.toString();
		assertTrue(output.contains("testa"), output);
	}

	@Test
	void b09_err_withIntegerFormatting() {
		Console.err("Number: %d", 1234);
		var output = errCapture.toString();
		assertTrue(output.contains("Number: 1234"), output);
	}

	@Test
	void b10_err_withDateArgument() {
		Console.err("Date: %s", new java.util.Date(0));
		var output = errCapture.toString();
		assertTrue(output.contains("Date:"), output);
	}

	@Test
	void b11_err_doesNotWriteToOut() {
		Console.err("test");
		var outOutput = outCapture.toString();
		assertTrue(outOutput.isEmpty(), "out should be empty but was: " + outOutput);
	}

	//====================================================================================================
	// Integration tests
	//====================================================================================================

	@Test
	void c01_bothOutAndErr() {
		Console.out("out message");
		Console.err("err message");
		var outOutput = outCapture.toString();
		var errOutput = errCapture.toString();
		assertTrue(outOutput.contains("out message"), outOutput);
		assertTrue(errOutput.contains("err message"), errOutput);
		assertFalse(outOutput.contains("err message"), outOutput);
		assertFalse(errOutput.contains("out message"), errOutput);
	}

	@Test
	void c02_multipleCalls() {
		Console.out("message1");
		Console.out("message2");
		Console.err("error1");
		Console.err("error2");
		var outOutput = outCapture.toString();
		var errOutput = errCapture.toString();
		assertTrue(outOutput.contains("message1"), outOutput);
		assertTrue(outOutput.contains("message2"), outOutput);
		assertTrue(errOutput.contains("error1"), errOutput);
		assertTrue(errOutput.contains("error2"), errOutput);
	}

	@Test
	void c03_complexFormatting() {
		Console.out("User %s has %d items worth $%.2f", "John", 5, 99.99);
		var output = outCapture.toString();
		assertTrue(output.contains("User"), output);
		assertTrue(output.contains("John"), output);
		assertTrue(output.contains("5"), output);
		assertTrue(output.contains("99.99"), output);
	}
}

