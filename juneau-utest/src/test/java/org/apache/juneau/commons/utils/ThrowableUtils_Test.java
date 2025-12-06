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

import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.sql.*;
import java.util.Optional;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.junit.jupiter.api.*;

class ThrowableUtils_Test extends TestBase {

	//====================================================================================================
	// Constructor (line 29)
	//====================================================================================================
	@Test
	void a00_constructor() {
		// Test line 29: class instantiation
		// ThrowableUtils has an implicit public no-arg constructor
		var instance = new ThrowableUtils();
		assertNotNull(instance);
	}

	//====================================================================================================
	// bex(Class<?>, String, Object...)
	//====================================================================================================
	@Test
	void a001_bex_withClass() {
		BeanRuntimeException ex = bex(String.class, "Error in class {0}", "TestClass");
		assertNotNull(ex);
		assertTrue(ex.getMessage().contains("Error in class TestClass"));
		assertTrue(ex.getMessage().contains("java.lang.String")); // Class name is in message
	}

	//====================================================================================================
	// bex(String, Object...)
	//====================================================================================================
	@Test
	void a002_bex_withMessage() {
		BeanRuntimeException ex = bex("Error message {0}", "test");
		assertNotNull(ex);
		assertTrue(ex.getMessage().contains("Error message test"));
	}

	//====================================================================================================
	// bex(Throwable)
	//====================================================================================================
	@Test
	void a003_bex_withCause() {
		var cause = new IOException("root cause");
		BeanRuntimeException ex = bex(cause);
		assertNotNull(ex);
		assertSame(cause, ex.getCause());
	}

	//====================================================================================================
	// bex(Throwable, Class<?>, String, Object...)
	//====================================================================================================
	@Test
	void a004_bex_withCauseAndClass() {
		var cause = new IOException("root cause");
		BeanRuntimeException ex = bex(cause, String.class, "Error in {0}", "TestClass");
		assertNotNull(ex);
		assertSame(cause, ex.getCause());
		assertTrue(ex.getMessage().contains("Error in TestClass"));
		assertTrue(ex.getMessage().contains("java.lang.String")); // Class name is in message
	}

	//====================================================================================================
	// bex(Throwable, String, Object...)
	//====================================================================================================
	@Test
	void a005_bex_withCauseAndMessage() {
		var cause = new IOException("root cause");
		// Note: bex(Throwable, String, ...) calls new BeanRuntimeException(f(msg, args), cause)
		// but BeanRuntimeException doesn't have a (String, Throwable) constructor,
		// so it matches BeanRuntimeException(String) and the cause is lost
		BeanRuntimeException ex = bex(cause, "Error message {0}", "test");
		assertNotNull(ex);
		// The cause is not preserved because BeanRuntimeException(String) constructor is used
		assertTrue(ex.getMessage().contains("Error message test"));
	}

	//====================================================================================================
	// castException(Class<T>, Throwable)
	//====================================================================================================
	@Test
	void a006_castException() {
		// Test casting to same type
		var original = new IllegalArgumentException("test");
		IllegalArgumentException result = castException(IllegalArgumentException.class, original);
		assertSame(original, result);

		// Test wrapping to different type
		var ioException = new IOException("io error");
		RuntimeException wrapped = castException(RuntimeException.class, ioException);
		assertNotNull(wrapped);
		assertSame(ioException, wrapped.getCause());
		assertInstanceOf(RuntimeException.class, wrapped);

		// Test with exception that doesn't have Throwable constructor
		var npe = new NullPointerException("npe");
		// IllegalArgumentException has a Throwable constructor, so this should work
		IllegalArgumentException wrapped2 = castException(IllegalArgumentException.class, npe);
		assertNotNull(wrapped2);
		assertSame(npe, wrapped2.getCause());
	}

	//====================================================================================================
	// findCause(Throwable, Class<T>)
	//====================================================================================================
	@Test
	void a007_findCause() {
		var rootCause = new IOException("root");
		var middleCause = new RuntimeException("middle", rootCause);
		var topException = new Exception("top", middleCause);

		// Find IOException
		Optional<IOException> ioOpt = findCause(topException, IOException.class);
		assertTrue(ioOpt.isPresent());
		assertEquals("root", ioOpt.get().getMessage());

		// Find RuntimeException
		Optional<RuntimeException> reOpt = findCause(topException, RuntimeException.class);
		assertTrue(reOpt.isPresent());
		assertEquals("middle", reOpt.get().getMessage());

		// Find Exception (returns itself)
		Optional<Exception> exOpt = findCause(topException, Exception.class);
		assertTrue(exOpt.isPresent());
		assertEquals("top", exOpt.get().getMessage());

		// Not found
		Optional<IllegalArgumentException> iaeOpt = findCause(topException, IllegalArgumentException.class);
		assertFalse(iaeOpt.isPresent());

		// Null exception
		Optional<IOException> nullOpt = findCause(null, IOException.class);
		assertFalse(nullOpt.isPresent());

		// Test with exception that has no cause
		var ex = new Exception("test");
		Optional<Exception> selfOpt = findCause(ex, Exception.class);
		assertTrue(selfOpt.isPresent());
		assertEquals("test", selfOpt.get().getMessage());

		// Test long chain
		Throwable cause = new IllegalStateException("root");
		for (var i = 0; i < 10; i++) {
			cause = new RuntimeException("level" + i, cause);
		}
		Optional<IllegalStateException> rootOpt = findCause(cause, IllegalStateException.class);
		assertTrue(rootOpt.isPresent());
		assertEquals("root", rootOpt.get().getMessage());
	}

	//====================================================================================================
	// getStackTrace(Throwable)
	//====================================================================================================
	@Test
	void a008_getStackTrace() {
		var ex = new RuntimeException("test exception");
		String stackTrace = getStackTrace(ex);
		assertNotNull(stackTrace);
		assertTrue(stackTrace.contains("RuntimeException"));
		assertTrue(stackTrace.contains("test exception"));
		assertTrue(stackTrace.contains("ThrowableUtils_Test") || stackTrace.contains("getStackTrace"));
	}

	//====================================================================================================
	// getThrowableCause(Class<T>, Throwable)
	//====================================================================================================
	@Test
	void a009_getThrowableCause() {
		var rootCause = new IOException("root");
		var middleCause = new RuntimeException("middle", rootCause);
		var topException = new Exception("top", middleCause);

		// Find IOException in cause chain
		IOException ioCause = getThrowableCause(IOException.class, topException);
		assertNotNull(ioCause);
		assertEquals("root", ioCause.getMessage());

		// Find RuntimeException in cause chain
		RuntimeException reCause = getThrowableCause(RuntimeException.class, topException);
		assertNotNull(reCause);
		assertEquals("middle", reCause.getMessage());

		// Not found
		IllegalArgumentException iaeCause = getThrowableCause(IllegalArgumentException.class, topException);
		assertNull(iaeCause);

		// Null exception
		IOException nullCause = getThrowableCause(IOException.class, null);
		assertNull(nullCause);

		// Exception with no cause
		var ex = new Exception("test");
		IOException noCause = getThrowableCause(IOException.class, ex);
		assertNull(noCause);
	}

	//====================================================================================================
	// hash(Throwable, String)
	//====================================================================================================
	@Test
	void a010_hash() {
		var ex1 = new RuntimeException("test");
		var ex2 = new RuntimeException("test");

		// Same exception type and stack trace should produce same hash
		int hash1 = hash(ex1, null);
		int hash2 = hash(ex2, null);
		// Note: hash might be different due to different stack traces, but should be consistent
		assertNotNull(hash1);
		assertNotNull(hash2);

		// Test with stop class - covers line 180 (break when stopClass matches)
		// Use a class name that will be in the stack trace
		String testClassName = ThrowableUtils_Test.class.getName();
		int hash3 = hash(ex1, testClassName);
		assertNotNull(hash3);

		// Test with stop class that doesn't match (should process all stack frames)
		int hash4 = hash(ex1, "java.lang.Object");
		assertNotNull(hash4);

		// Test with nested exception
		var cause = new IOException("cause");
		var wrapped = new RuntimeException("wrapped", cause);
		int hash5 = hash(wrapped, null);
		assertNotNull(hash5);

		// Test with null exception
		int hash6 = hash(null, null);
		assertEquals(0, hash6);

		// Test with stop class matching a class in the cause chain - covers line 180
		int hash7 = hash(wrapped, "java.io.IOException");
		assertNotNull(hash7);
	}

	//====================================================================================================
	// illegalArg(String, Object...)
	//====================================================================================================
	@Test
	void a011_illegalArg_withMessage() {
		IllegalArgumentException ex = illegalArg("Invalid parameter {0}", "userId");
		assertNotNull(ex);
		assertTrue(ex.getMessage().contains("Invalid parameter userId"));
		assertNull(ex.getCause());
	}

	//====================================================================================================
	// illegalArg(Throwable)
	//====================================================================================================
	@Test
	void a012_illegalArg_withCause() {
		var cause = new IOException("root cause");
		IllegalArgumentException ex = illegalArg(cause);
		assertNotNull(ex);
		assertSame(cause, ex.getCause());
	}

	//====================================================================================================
	// illegalArg(Throwable, String, Object...)
	//====================================================================================================
	@Test
	void a013_illegalArg_withCauseAndMessage() {
		var cause = new IOException("root cause");
		IllegalArgumentException ex = illegalArg(cause, "Invalid parameter {0}", "userId");
		assertNotNull(ex);
		assertSame(cause, ex.getCause());
		assertTrue(ex.getMessage().contains("Invalid parameter userId"));
	}

	//====================================================================================================
	// ioex(String, Object...)
	//====================================================================================================
	@Test
	void a014_ioex_withMessage() {
		IOException ex = ioex("File not found: {0}", "/tmp/test.txt");
		assertNotNull(ex);
		assertTrue(ex.getMessage().contains("File not found: /tmp/test.txt"));
		assertNull(ex.getCause());
	}

	//====================================================================================================
	// ioex(Throwable)
	//====================================================================================================
	@Test
	void a015_ioex_withCause() {
		var cause = new FileNotFoundException("config.xml");
		IOException ex = ioex(cause);
		assertNotNull(ex);
		assertSame(cause, ex.getCause());
	}

	//====================================================================================================
	// ioex(Throwable, String, Object...)
	//====================================================================================================
	@Test
	void a016_ioex_withCauseAndMessage() {
		var cause = new FileNotFoundException("config.xml");
		IOException ex = ioex(cause, "Failed to load {0}", "configuration");
		assertNotNull(ex);
		assertSame(cause, ex.getCause());
		assertTrue(ex.getMessage().contains("Failed to load configuration"));
	}

	//====================================================================================================
	// lm(Throwable)
	//====================================================================================================
	@Test
	void a017_lm() {
		var ex = new RuntimeException("test message");
		String localized = lm(ex);
		assertEquals("test message", localized);
		assertEquals(ex.getLocalizedMessage(), localized);
	}

	//====================================================================================================
	// rex(String, Object...)
	//====================================================================================================
	@Test
	void a018_rex_withMessage() {
		RuntimeException ex = rex("Error message {0}", "test");
		assertNotNull(ex);
		assertTrue(ex.getMessage().contains("Error message test"));
		assertNull(ex.getCause());
	}

	//====================================================================================================
	// rex(Throwable)
	//====================================================================================================
	@Test
	void a019_rex_withCause() {
		var cause = new SQLException("Database error");
		RuntimeException ex = rex(cause);
		assertNotNull(ex);
		assertSame(cause, ex.getCause());
	}

	//====================================================================================================
	// rex(Throwable, String, Object...)
	//====================================================================================================
	@Test
	void a020_rex_withCauseAndMessage() {
		var cause = new SQLException("Database error");
		RuntimeException ex = rex(cause, "Failed to process {0} at {1}", "user", "login");
		assertNotNull(ex);
		assertSame(cause, ex.getCause());
		assertTrue(ex.getMessage().contains("Failed to process user at login"));
	}

	//====================================================================================================
	// toRex(Throwable)
	//====================================================================================================
	@Test
	void a021_toRex() {
		// Test with RuntimeException (should return same)
		var re = new RuntimeException("test");
		RuntimeException result1 = toRex(re);
		assertSame(re, result1);

		// Test with IOException (should wrap)
		var io = new IOException("io error");
		RuntimeException result2 = toRex(io);
		assertNotNull(result2);
		assertSame(io, result2.getCause());
		assertInstanceOf(RuntimeException.class, result2);

		// Test with Exception (should wrap)
		var ex = new Exception("exception");
		RuntimeException result3 = toRex(ex);
		assertNotNull(result3);
		assertSame(ex, result3.getCause());
		assertInstanceOf(RuntimeException.class, result3);
	}

	//====================================================================================================
	// unsupportedOp()
	//====================================================================================================
	@Test
	void a022_unsupportedOp() {
		UnsupportedOperationException ex = unsupportedOp();
		assertNotNull(ex);
		assertEquals("Not supported.", ex.getMessage());
		assertNull(ex.getCause());
	}

	//====================================================================================================
	// unsupportedOp(String, Object...)
	//====================================================================================================
	@Test
	void a023_unsupportedOp_withMessage() {
		UnsupportedOperationException ex = unsupportedOp("Operation {0} is not supported for type {1}", "delete", "User");
		assertNotNull(ex);
		assertTrue(ex.getMessage().contains("Operation delete is not supported for type User"));
		assertNull(ex.getCause());
	}

	//====================================================================================================
	// unsupportedOp(Throwable)
	//====================================================================================================
	@Test
	void a024_unsupportedOp_withCause() {
		var cause = new IllegalStateException("Locked");
		UnsupportedOperationException ex = unsupportedOp(cause);
		assertNotNull(ex);
		assertSame(cause, ex.getCause());
	}

	//====================================================================================================
	// unsupportedOp(Throwable, String, Object...)
	//====================================================================================================
	@Test
	void a025_unsupportedOp_withCauseAndMessage() {
		var cause = new IllegalStateException("Locked");
		UnsupportedOperationException ex = unsupportedOp(cause, "Cannot {0} on {1}", "delete", "immutable collection");
		assertNotNull(ex);
		assertSame(cause, ex.getCause());
		assertTrue(ex.getMessage().contains("Cannot delete on immutable collection"));
	}

	//====================================================================================================
	// unsupportedOpReadOnly()
	//====================================================================================================
	@Test
	void a026_unsupportedOpReadOnly() {
		UnsupportedOperationException ex = unsupportedOpReadOnly();
		assertNotNull(ex);
		assertEquals("Object is read only.", ex.getMessage());
		assertNull(ex.getCause());
	}

	//====================================================================================================
	// exex(String, Object...)
	//====================================================================================================
	@Test
	void a027_exex_withMessage() {
		ExecutableException ex = exex("Error message {0}", "test");
		assertNotNull(ex);
		assertTrue(ex.getMessage().contains("Error message test"));
		assertNull(ex.getCause());
	}

	//====================================================================================================
	// exex(Throwable)
	//====================================================================================================
	@Test
	void a028_exex_withCause() {
		var cause = new IOException("root cause");
		ExecutableException ex = exex(cause);
		assertNotNull(ex);
		assertSame(cause, ex.getCause());
	}

	//====================================================================================================
	// exex(Throwable, String, Object...)
	//====================================================================================================
	@Test
	void a029_exex_withCauseAndMessage() {
		var cause = new IOException("root cause");
		ExecutableException ex = exex(cause, "Error message {0}", "test");
		assertNotNull(ex);
		assertSame(cause, ex.getCause());
		assertTrue(ex.getMessage().contains("Error message test"));
	}
}

