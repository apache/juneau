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
package org.apache.juneau.commons.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link Console}.
 */
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
		Console.out("test{0}", 123);
		var output = outCapture.toString();
		assertTrue(output.contains("test123"), output);
	}

	@Test
	void a03_out_withMultipleArguments() {
		Console.out("test{0} {1} {2}", "a", "b", "c");
		var output = outCapture.toString();
		assertTrue(output.contains("testa b c"), output);
	}

	@Test
	void a04_out_withNullArgument() {
		Console.out("test{0}", (Object)null);
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
	void a07_out_withMessageFormatPattern() {
		Console.out("Value: {0,number}", 12345);
		var output = outCapture.toString();
		assertTrue(output.contains("Value:"), output);
		// MessageFormat may add locale-specific formatting (e.g., commas), so check for digits
		assertTrue(output.matches("(?s).*Value:.*[0-9].*"), 
			"Output should contain 'Value:' and at least one digit. Actual: " + output);
	}

	@Test
	void a08_out_withSpecialCharacters() {
		Console.out("test{0}", "a\nb\tc");
		var output = outCapture.toString();
		assertTrue(output.contains("testa"), output);
	}

	@Test
	void a09_out_withNumberFormatting() {
		Console.out("Number: {0,number,integer}", 1234);
		var output = outCapture.toString();
		assertTrue(output.contains("Number:"), output);
		// MessageFormat may add locale-specific formatting (e.g., commas), so check for digits
		assertTrue(output.matches("(?s).*Number:.*[0-9].*"), 
			"Output should contain 'Number:' and at least one digit. Actual: " + output);
	}

	@Test
	void a10_out_withDateFormatting() {
		Console.out("Date: {0,date}", new java.util.Date(0));
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
		Console.err("test{0}", 123);
		var output = errCapture.toString();
		assertTrue(output.contains("test123"), output);
	}

	@Test
	void b03_err_withMultipleArguments() {
		Console.err("test{0} {1} {2}", "a", "b", "c");
		var output = errCapture.toString();
		assertTrue(output.contains("testa b c"), output);
	}

	@Test
	void b04_err_withNullArgument() {
		Console.err("test{0}", (Object)null);
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
	void b07_err_withMessageFormatPattern() {
		Console.err("Error: {0,number}", 12345);
		var output = errCapture.toString();
		assertTrue(output.contains("Error:"), output);
		// MessageFormat may add locale-specific formatting (e.g., commas), so check for digits
		assertTrue(output.matches("(?s).*Error:.*[0-9].*"), 
			"Output should contain 'Error:' and at least one digit. Actual: " + output);
	}

	@Test
	void b08_err_withSpecialCharacters() {
		Console.err("test{0}", "a\nb\tc");
		var output = errCapture.toString();
		assertTrue(output.contains("testa"), output);
	}

	@Test
	void b09_err_withNumberFormatting() {
		Console.err("Number: {0,number,integer}", 1234);
		var output = errCapture.toString();
		assertTrue(output.contains("Number:"), output);
		// MessageFormat may add locale-specific formatting (e.g., commas), so check for digits
		assertTrue(output.matches("(?s).*Number:.*[0-9].*"), 
			"Output should contain 'Number:' and at least one digit. Actual: " + output);
	}

	@Test
	void b10_err_withDateFormatting() {
		Console.err("Date: {0,date}", new java.util.Date(0));
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
		Console.out("User {0} has {1} items worth {2,number,currency}", "John", 5, 99.99);
		var output = outCapture.toString();
		assertTrue(output.contains("User"), output);
		assertTrue(output.contains("John"), output);
		assertTrue(output.contains("5"), output);
	}
}

