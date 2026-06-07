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
package org.apache.juneau.config;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.config.event.*;
import org.junit.jupiter.api.*;

class ConfigEvents_Test extends TestBase {

	@Test void a01_isKeyModified_true() {
		var events = new ConfigEvents();
		events.add(ConfigEvent.setEntry("test.cfg", "S", "key1", "val", null, null, null));
		assertTrue(events.isKeyModified("S", "key1"));
	}

	@Test void a02_isKeyModified_wrongKey() {
		var events = new ConfigEvents();
		events.add(ConfigEvent.setEntry("test.cfg", "S", "key1", "val", null, null, null));
		assertFalse(events.isKeyModified("S", "key2"));
	}

	@Test void a03_isKeyModified_wrongSection() {
		var events = new ConfigEvents();
		events.add(ConfigEvent.setEntry("test.cfg", "S", "key1", "val", null, null, null));
		assertFalse(events.isKeyModified("T", "key1"));
	}

	@Test void a04_isKeyModified_emptyList() {
		var events = new ConfigEvents();
		assertFalse(events.isKeyModified("S", "key1"));
	}

	@Test void a05_isSectionModified_true() {
		var events = new ConfigEvents();
		events.add(ConfigEvent.removeSection("test.cfg", "S"));
		assertTrue(events.isSectionModified("S"));
	}

	@Test void a06_isSectionModified_wrongSection() {
		var events = new ConfigEvents();
		events.add(ConfigEvent.removeSection("test.cfg", "S"));
		assertFalse(events.isSectionModified("T"));
	}

	@Test void a07_isSectionModified_emptyList() {
		var events = new ConfigEvents();
		assertFalse(events.isSectionModified("S"));
	}

	@Test void a08_isKeyModified_removeEntry() {
		var events = new ConfigEvents();
		events.add(ConfigEvent.removeEntry("test.cfg", "S", "key1"));
		assertTrue(events.isKeyModified("S", "key1"));
		assertFalse(events.isKeyModified("S", "key2"));
	}

	@Test void a09_isSectionModified_multipleEvents() {
		var events = new ConfigEvents();
		events.add(ConfigEvent.setEntry("test.cfg", "A", "k", "v", null, null, null));
		events.add(ConfigEvent.removeSection("test.cfg", "B"));
		assertTrue(events.isSectionModified("A"));
		assertTrue(events.isSectionModified("B"));
		assertFalse(events.isSectionModified("C"));
	}
}
