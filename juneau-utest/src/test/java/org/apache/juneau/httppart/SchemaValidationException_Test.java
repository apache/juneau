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
package org.apache.juneau.httppart;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class SchemaValidationException_Test extends TestBase {

	@Test void a01_basic() {
		var x = new SchemaValidationException("Test message");
		assertNotNull(x);
		assertTrue(x.getMessage().contains("Test message"));
	}

	@Test void a02_withArgs() {
		var x = new SchemaValidationException("Test {0} {1}", "foo", "bar");
		assertTrue(x.getMessage().contains("Test foo bar"));
	}

	@Test void a03_setMessage_fluentChaining() {
		var x = new SchemaValidationException("Original");

		// Test that setMessage returns SchemaValidationException for fluent chaining
		SchemaValidationException result = x.setMessage("New message");
		assertSame(x, result);
		assertInstanceOf(SchemaValidationException.class, result);
		assertTrue(result.getMessage().contains("New message"));
	}

	@Test void a04_getRootCause_single() {
		var x = new SchemaValidationException("Root cause");

		// Test that getRootCause returns SchemaValidationException (not ParseException)
		SchemaValidationException root = x.getRootCause();
		assertSame(x, root);
		assertInstanceOf(SchemaValidationException.class, root);
	}

	@Test void a05_getRootCause_nested() {
		var cause = new SchemaValidationException("Cause");
		var middle = new SchemaValidationException("Middle");
		middle.initCause(cause);
		var top = new SchemaValidationException("Top");
		top.initCause(middle);

		// Test that getRootCause returns the deepest SchemaValidationException
		SchemaValidationException root = top.getRootCause();
		assertSame(cause, root);
		assertInstanceOf(SchemaValidationException.class, root);
		assertTrue(root.getMessage().contains("Cause"));
	}

	@Test void a06_fluentChaining() {
		// Test multiple fluent calls can be chained
		var x = new SchemaValidationException("Initial")
			.setMessage("Updated message");

		assertTrue(x.getMessage().contains("Updated message"));
	}
}