// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.junit.bct;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link PropertyNotFoundException}.
 */
class PropertyNotFoundException_Test extends TestBase {

	@Nested
	class A_construction extends TestBase {

		@Test
		void a01_messageConstructor() {
			String message = "Custom error message";
			PropertyNotFoundException ex = new PropertyNotFoundException(message);

			assertEquals(message, ex.getMessage());
			assertNull(ex.getCause());
		}

		@Test
		void a02_messageAndCauseConstructor() {
			String message = "Custom error message";
			RuntimeException cause = new RuntimeException("Root cause");
			PropertyNotFoundException ex = new PropertyNotFoundException(message, cause);

			assertEquals(message, ex.getMessage());
			assertEquals(cause, ex.getCause());
		}

		@Test
		void a03_propertyAndTypeConstructor() {
			String propertyName = "invalidProperty";
			Class<?> objectType = String.class;
			PropertyNotFoundException ex = new PropertyNotFoundException(propertyName, objectType);

			assertEquals("Property 'invalidProperty' not found on object of type String", ex.getMessage());
			assertNull(ex.getCause());
		}

		@Test
		void a04_propertyTypeAndCauseConstructor() {
			String propertyName = "missingField";
			Class<?> objectType = Integer.class;
			RuntimeException cause = new RuntimeException("Field not found");
			PropertyNotFoundException ex = new PropertyNotFoundException(propertyName, objectType, cause);

			assertEquals("Property 'missingField' not found on object of type Integer", ex.getMessage());
			assertEquals(cause, ex.getCause());
		}
	}

	@Nested
	class B_integration extends TestBase {

		@Test
		void b01_thrownByPropertyExtractor() {
			BeanConverter converter = BasicBeanConverter.DEFAULT;
			TestBean bean = new TestBean("test", 42);

			// This should throw PropertyNotFoundException
			PropertyNotFoundException ex = assertThrows(PropertyNotFoundException.class, () -> {
				converter.getNested(bean, Utils.tokenize("nonExistentProperty").get(0));
			});

			assertTrue(ex.getMessage().contains("nonExistentProperty"));
			assertTrue(ex.getMessage().contains("TestBean"));
		}

		@SuppressWarnings("cast")
		@Test
		void b02_exceptionHierarchy() {
			PropertyNotFoundException ex = new PropertyNotFoundException("test");

			// Should be a RuntimeException
			assertTrue(ex instanceof RuntimeException);
			assertTrue(ex instanceof Exception);
			assertTrue(ex instanceof Throwable);
		}
	}

	// Helper class for testing
	static class TestBean {
		final String name;
		final int value;

		TestBean(String name, int value) {
			this.name = name;
			this.value = value;
		}

		public String getName() { return name; }
		public int getValue() { return value; }
	}
}
