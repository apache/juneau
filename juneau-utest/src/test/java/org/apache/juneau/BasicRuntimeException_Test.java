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
package org.apache.juneau;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.objecttools.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.client.remote.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.junit.jupiter.api.*;

/**
 * Tests for fluent setters on BasicRuntimeException subclasses.
 */
class BasicRuntimeException_Test extends TestBase {

	@Test void a02_ClassMetaRuntimeException_fluentSetters() {
		var x = new ClassMetaRuntimeException("Original message");

		// Test setMessage returns same instance for fluent chaining
		assertSame(x, x.setMessage("New message"));
		assertEquals("New message", x.getMessage());

		// Test setMessage with args
		assertSame(x, x.setMessage("Message {0}", "arg1"));
		assertEquals("Message arg1", x.getMessage());
	}

	@Test void a03_ConfigException_fluentSetters() {
		var x = new ConfigException("Original message");

		// Test setMessage returns same instance for fluent chaining
		assertSame(x, x.setMessage("New message"));
		assertEquals("New message", x.getMessage());

		// Test setMessage with args
		assertSame(x, x.setMessage("Message {0}", "arg1"));
		assertEquals("Message arg1", x.getMessage());
	}

	@Test void a04_ContextRuntimeException_fluentSetters() {
		var x = new ContextRuntimeException("Original message");

		// Test setMessage returns same instance for fluent chaining
		assertSame(x, x.setMessage("New message"));
		assertEquals("New message", x.getMessage());

		// Test setMessage with args
		assertSame(x, x.setMessage("Message {0}", "arg1"));
		assertEquals("Message arg1", x.getMessage());
	}

//	@Test void a05_ExecutableException_fluentSetters() {
//		ExecutableException x = new ExecutableException("Original message");
//
//		// Test setMessage returns same instance for fluent chaining
//		assertSame(x, x.setMessage("New message"));
//		assertEquals("New message", x.getMessage());
//
//		// Test setMessage with args
//		assertSame(x, x.setMessage("Message {0}", "arg1"));
//		assertEquals("Message arg1", x.getMessage());
//	}

	@Test void a06_InvalidAnnotationException_fluentSetters() {
		var x = new InvalidAnnotationException("Original message");

		// Test setMessage returns same instance for fluent chaining
		assertSame(x, x.setMessage("New message"));
		assertEquals("New message", x.getMessage());

		// Test setMessage with args
		assertSame(x, x.setMessage("Message {0}", "arg1"));
		assertEquals("Message arg1", x.getMessage());
	}

	@Test void a07_InvalidDataConversionException_fluentSetters() {
		var x = new InvalidDataConversionException(null, "Original message");

		// Test setMessage returns same instance for fluent chaining
		assertSame(x, x.setMessage("New message"));
		assertEquals("New message", x.getMessage());

		// Test setMessage with args
		assertSame(x, x.setMessage("Message {0}", "arg1"));
		assertEquals("Message arg1", x.getMessage());
	}

	@Test void a08_ObjectRestException_fluentSetters() {
		var x = new ObjectRestException(404, "Original message");

		// Test setMessage returns same instance for fluent chaining
		assertSame(x, x.setMessage("New message"));
		assertEquals("New message", x.getMessage());

		// Test setMessage with args
		assertSame(x, x.setMessage("Message {0}", "arg1"));
		assertEquals("Message arg1", x.getMessage());
	}

	@Test void a09_ParseException_fluentSetters() {
		var x = new ParseException("Original message");

		// Test setMessage returns same instance for fluent chaining
		assertSame(x, x.setMessage("New message"));
		assertEquals("New message", x.getMessage());

		// Test setMessage with args
		assertSame(x, x.setMessage("Message {0}", "arg1"));
		assertEquals("Message arg1", x.getMessage());
	}

	@Test void a10_PatternException_fluentSetters() {
		var x = new PatternException("Original message");

		// Test setMessage returns same instance for fluent chaining
		assertSame(x, x.setMessage("New message"));
		assertEquals("New message", x.getMessage());

		// Test setMessage with args
		assertSame(x, x.setMessage("Message {0}", "arg1"));
		assertEquals("Message arg1", x.getMessage());
	}

	@Test void a11_RemoteMetadataException_fluentSetters() {
		var x = new RemoteMetadataException((Throwable)null, "Original message");

		// Test setMessage returns same instance for fluent chaining
		assertSame(x, x.setMessage("New message"));
		assertTrue(x.getMessage().contains("New message"));

		// Test setMessage with args
		assertSame(x, x.setMessage("Message {0}", "arg1"));
		assertTrue(x.getMessage().contains("Message arg1"));
	}

	@Test void a12_SerializeException_fluentSetters() {
		var x = new SerializeException("Original message");

		// Test setMessage returns same instance for fluent chaining
		assertSame(x, x.setMessage("New message"));
		assertEquals("New message", x.getMessage());

		// Test setMessage with args
		assertSame(x, x.setMessage("Message {0}", "arg1"));
		assertEquals("Message arg1", x.getMessage());
	}

	@Test void a13_VarResolverException_fluentSetters() {
		var x = new VarResolverException("Original message");

		// Test setMessage returns same instance for fluent chaining
		assertSame(x, x.setMessage("New message"));
		assertEquals("New message", x.getMessage());

		// Test setMessage with args
		assertSame(x, x.setMessage("Message {0}", "arg1"));
		assertEquals("Message arg1", x.getMessage());
	}
}