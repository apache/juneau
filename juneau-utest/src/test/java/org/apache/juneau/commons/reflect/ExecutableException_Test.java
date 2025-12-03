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
package org.apache.juneau.commons.reflect;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ExecutableException_Test extends TestBase {

	public static class TestClass {
		public void method() throws Exception {
			throw new RuntimeException("test");
		}
	}

	//====================================================================================================
	// ExecutableException(String, Object...)
	//====================================================================================================
	@Test
	void a001_constructor_message() {
		ExecutableException e = new ExecutableException("Test message");
		assertEquals("Test message", e.getMessage());
		assertNull(e.getCause());
	}

	//====================================================================================================
	// ExecutableException(String, Object...) with args
	//====================================================================================================
	@Test
	void a002_constructor_messageWithArgs() {
		ExecutableException e = new ExecutableException("Test {0} message {1}", "arg1", "arg2");
		assertTrue(e.getMessage().contains("arg1"));
		assertTrue(e.getMessage().contains("arg2"));
		assertNull(e.getCause());
	}

	//====================================================================================================
	// ExecutableException(Throwable)
	//====================================================================================================
	@Test
	void a003_constructor_cause() {
		Throwable cause = new RuntimeException("cause");
		ExecutableException e = new ExecutableException(cause);
		assertSame(cause, e.getCause());
	}

	//====================================================================================================
	// ExecutableException(Throwable, String, Object...)
	//====================================================================================================
	@Test
	void a004_constructor_causeAndMessage() {
		Throwable cause = new RuntimeException("cause");
		ExecutableException e = new ExecutableException(cause, "Test {0}", "arg");
		assertSame(cause, e.getCause());
		assertTrue(e.getMessage().contains("arg"));
	}

	//====================================================================================================
	// getTargetException()
	//====================================================================================================
	@Test
	void a005_getTargetException() throws Exception {
		// With InvocationTargetException
		RuntimeException targetException = new RuntimeException("target");
		InvocationTargetException ite = new InvocationTargetException(targetException);
		ExecutableException e = new ExecutableException(ite);
		assertSame(targetException, e.getTargetException());
		
		// With other exception
		IllegalArgumentException iae = new IllegalArgumentException("test");
		ExecutableException e2 = new ExecutableException(iae);
		assertSame(iae, e2.getTargetException());
		
		// With no cause
		ExecutableException e3 = new ExecutableException("message");
		assertNull(e3.getTargetException());
	}

	//====================================================================================================
	// unwrap()
	//====================================================================================================
	@Test
	void a006_unwrap() {
		// With cause
		Throwable cause = new RuntimeException("cause");
		ExecutableException e = new ExecutableException(cause);
		assertSame(cause, e.unwrap());
		
		// Without cause
		ExecutableException e2 = new ExecutableException("message");
		assertSame(e2, e2.unwrap());
	}
}


