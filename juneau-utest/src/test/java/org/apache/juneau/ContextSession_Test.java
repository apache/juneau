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

import org.junit.jupiter.api.*;

class ContextSession_Test extends TestBase {

	//====================================================================================================
	// ContextSession.Builder.property() - Lines 142-149
	//====================================================================================================

	@Test void a01_property_addProperty() {
		// Test line 147: adding a property with non-null value
		var session = BeanContext.DEFAULT.createSession()
			.property("key1", "value1")
			.property("key2", 123)
			.build();
		var props = session.getSessionProperties();
		assertEquals("value1", props.get("key1"));
		assertEquals(123, props.get("key2"));
	}

	@Test void a02_property_removeProperty() {
		// Test line 145: removing a property by setting value to null
		var session = BeanContext.DEFAULT.createSession()
			.property("key1", "value1")
			.property("key2", "value2")
			.property("key1", null)  // Remove key1
			.build();
		var props = session.getSessionProperties();
		assertFalse(props.containsKey("key1"));
		assertEquals("value2", props.get("key2"));
	}

	@Test void a03_property_nullKey() {
		// Test line 142: assertArgNotNull on key
		var session = BeanContext.DEFAULT.createSession();
		assertThrows(IllegalArgumentException.class, () -> {
			session.property(null, "value");
		});
	}

	//====================================================================================================
	// ContextSession constructor - Line 185
	//====================================================================================================

	@Test void b01_unmodifiableSession_emptyProperties() {
		// Test line 185: unmodifiable session with empty properties should use Collections.emptyMap()
		// Note: The actual implementation may wrap empty maps differently, so we test the behavior
		var session = BeanContext.DEFAULT.createSession()
			.unmodifiable()
			.build();
		var props = session.getSessionProperties();
		// Test line 185: if properties are empty, should use Collections.emptyMap() or equivalent
		// The key test is that it's unmodifiable and empty
		assertTrue(props.isEmpty());
		assertThrows(UnsupportedOperationException.class, () -> {
			props.put("key", "value");
		});
	}

	@Test void b02_unmodifiableSession_withProperties() {
		// Test line 185: unmodifiable session with properties should use unmodifiable map
		var session = BeanContext.DEFAULT.createSession()
			.property("key1", "value1")
			.property("key2", "value2")
			.unmodifiable()
			.build();
		var props = session.getSessionProperties();
		assertEquals("value1", props.get("key1"));
		assertEquals("value2", props.get("key2"));
		// Verify it's unmodifiable
		assertThrows(UnsupportedOperationException.class, () -> {
			props.put("key3", "value3");
		});
	}

	//====================================================================================================
	// ContextSession.addWarning() - Line 201
	//====================================================================================================

	@Test void c01_addWarning_unmodifiableSession() {
		// Test line 201: addWarning should return early if session is unmodifiable
		var session = BeanContext.DEFAULT.createSession()
			.unmodifiable()
			.build();
		// Should not throw exception, just return early
		session.addWarning("Test warning");
		assertTrue(session.getWarnings().isEmpty());
	}

	@Test void c02_addWarning_modifiableSession() {
		// Test that addWarning works on modifiable sessions
		var session = BeanContext.DEFAULT.createSession()
			.build();
		session.addWarning("Test warning");
		var warnings = session.getWarnings();
		assertFalse(warnings.isEmpty());
		assertTrue(warnings.get(0).contains("Test warning"));
	}

	//====================================================================================================
	// ContextSession.getContext() - Line 220
	//====================================================================================================

	@Test void d01_getContext() {
		// Test line 220: getContext() returns the context that created the session
		var context = BeanContext.DEFAULT;
		var session = context.createSession().build();
		assertSame(context, session.getContext());
	}
}

