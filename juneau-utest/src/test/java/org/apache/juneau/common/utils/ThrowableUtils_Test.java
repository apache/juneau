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
package org.apache.juneau.common.utils;

import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.sql.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ThrowableUtils_Test extends TestBase {

	//====================================================================================================
	// findCause(Throwable, Class)
	//====================================================================================================
	@Test
	void a01_findCause() {
		var rootCause = new IOException("root");
		var middleCause = new RuntimeException("middle", rootCause);
		var topException = new Exception("top", middleCause);

		// Find IOException
		assertTrue(findCause(topException, IOException.class).isPresent());
		assertEquals("root", findCause(topException, IOException.class).get().getMessage());

		// Find RuntimeException
		assertTrue(findCause(topException, RuntimeException.class).isPresent());
		assertEquals("middle", findCause(topException, RuntimeException.class).get().getMessage());

		// Find Exception (returns itself)
		assertTrue(findCause(topException, Exception.class).isPresent());
		assertEquals("top", findCause(topException, Exception.class).get().getMessage());

		// Not found
		assertFalse(findCause(topException, IllegalArgumentException.class).isPresent());

		// Null exception
		assertFalse(findCause(null, IOException.class).isPresent());
	}

	@Test
	void a02_findCause_noCause() {
		var ex = new Exception("test");
		
		// Find itself
		assertTrue(findCause(ex, Exception.class).isPresent());
		assertEquals("test", findCause(ex, Exception.class).get().getMessage());

		// Not found
		assertFalse(findCause(ex, IOException.class).isPresent());
	}

	@Test
	void a03_findCause_longChain() {
		Throwable cause = new IllegalStateException("root");
		for (int i = 0; i < 10; i++) {
			cause = new RuntimeException("level" + i, cause);
		}

		// Should find the root cause
		assertTrue(findCause(cause, IllegalStateException.class).isPresent());
		assertEquals("root", findCause(cause, IllegalStateException.class).get().getMessage());
	}

	//====================================================================================================
	// unsupportedOp(String, Object...)
	//====================================================================================================
	@Test
	void b01_unsupportedOp_noArgs() {
		UnsupportedOperationException ex = unsupportedOp("Operation not supported");
		assertEquals("Operation not supported", ex.getMessage());
		assertInstanceOf(UnsupportedOperationException.class, ex);
	}

	@Test
	void b02_unsupportedOp_withArgs() {
		UnsupportedOperationException ex = unsupportedOp("Operation {0} is not supported for type {1}", "delete", "User");
		assertEquals("Operation delete is not supported for type User", ex.getMessage());
	}

	@Test
	void b03_unsupportedOp_emptyArgs() {
		UnsupportedOperationException ex = unsupportedOp("No formatting");
		assertEquals("No formatting", ex.getMessage());
	}

	@Test
	void b04_unsupportedOp_throwable() {
		UnsupportedOperationException ex = unsupportedOp("Cannot modify {0}", "immutable list");
		assertThrows(UnsupportedOperationException.class, () -> {
			throw ex;
		});
	}

	//====================================================================================================
	// ioException(String, Object...)
	//====================================================================================================
	@Test
	void c01_ioException_noArgs() {
		IOException ex = ioex("File not found");
		assertEquals("File not found", ex.getMessage());
		assertInstanceOf(IOException.class, ex);
	}

	@Test
	void c02_ioException_withArgs() {
		IOException ex = ioex("Failed to read file {0} at line {1}", "/tmp/test.txt", 42);
		assertEquals("Failed to read file /tmp/test.txt at line 42", ex.getMessage());
	}

	@Test
	void c03_ioException_emptyArgs() {
		IOException ex = ioex("No formatting");
		assertEquals("No formatting", ex.getMessage());
	}

	@Test
	void c04_ioException_throwable() {
		IOException ex = ioex("Cannot write to {0}", "readonly.txt");
		assertThrows(IOException.class, () -> {
			throw ex;
		});
	}

	@Test
	void c05_ioException_multipleArgs() {
		IOException ex = ioex("Error at position {0}:{1} in file {2}", 10, 25, "data.csv");
		assertEquals("Error at position 10:25 in file data.csv", ex.getMessage());
	}

	//====================================================================================================
	// Exception methods with causes
	//====================================================================================================
	@Test
	void d01_illegalArg_withCause() {
		var cause = new IOException("root cause");
		IllegalArgumentException ex = illegalArg(cause, "Invalid parameter {0}", "userId");
		
		assertEquals("Invalid parameter userId", ex.getMessage());
		assertSame(cause, ex.getCause());
		assertEquals("root cause", ex.getCause().getMessage());
	}

	@Test
	void d02_runtimeException_withCause() {
		var cause = new SQLException("Database error");
		RuntimeException ex = rex(cause, "Failed to process {0} at {1}", "user", "login");
		
		assertEquals("Failed to process user at login", ex.getMessage());
		assertSame(cause, ex.getCause());
		assertEquals("Database error", ex.getCause().getMessage());
	}

	@Test
	void d03_unsupportedOp_withCause() {
		var cause = new IllegalStateException("Locked");
		UnsupportedOperationException ex = unsupportedOp(cause, "Cannot {0} on {1}", "delete", "immutable collection");
		
		assertEquals("Cannot delete on immutable collection", ex.getMessage());
		assertSame(cause, ex.getCause());
		assertEquals("Locked", ex.getCause().getMessage());
	}

	@Test
	void d04_ioException_withCause() {
		var cause = new FileNotFoundException("config.xml");
		IOException ex = ioex(cause, "Failed to load {0}", "configuration");
		
		assertEquals("Failed to load configuration", ex.getMessage());
		assertSame(cause, ex.getCause());
		assertEquals("config.xml", ex.getCause().getMessage());
	}

	@Test
	void d05_runtimeException_withCause_noArgs() {
		var cause = new NullPointerException("value was null");
		RuntimeException ex = rex(cause, "Processing failed");
		
		assertEquals("Processing failed", ex.getMessage());
		assertSame(cause, ex.getCause());
	}

	@Test
	void d06_ioException_withCause_multipleArgs() {
		var cause = new SocketException("Connection reset");
		IOException ex = ioex(cause, "Network error at {0}:{1} for host {2}", "192.168.1.1", "8080", "server");
		
		assertEquals("Network error at 192.168.1.1:8080 for host server", ex.getMessage());
		assertSame(cause, ex.getCause());
	}

	@Test
	void d07_exception_withCause_chaining() {
		var rootCause = new IOException("disk full");
		RuntimeException wrappedException = rex(rootCause, "Cannot write file {0}", "output.txt");
		IllegalArgumentException topException = illegalArg(wrappedException, "Invalid operation");
		
		assertEquals("Invalid operation", topException.getMessage());
		assertEquals("Cannot write file output.txt", topException.getCause().getMessage());
		assertEquals("disk full", topException.getCause().getCause().getMessage());
	}
}

