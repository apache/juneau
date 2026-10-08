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
package org.apache.juneau.rest.server.views;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.views.ConsoleOutputLine.*;
import org.junit.jupiter.api.*;

class ConsoleOutputLine_Level_Test extends TestBase {

	@Test void a01_severityNumbers() {
		assertEquals(17, Level.SEVERE.severityNumber());
		assertEquals(13, Level.WARNING.severityNumber());
		assertEquals(9, Level.INFO.severityNumber());
		assertEquals(5, Level.FINE.severityNumber());
	}

	@Test void a02_defaultStyles() {
		assertEquals(Style.ERROR, Level.SEVERE.defaultStyle());
		assertEquals(Style.WARN, Level.WARNING.defaultStyle());
		assertNull(Level.INFO.defaultStyle());
		assertEquals(Style.MUTED, Level.FINE.defaultStyle());
	}

	@Test void a03_ofJul() {
		assertEquals(Level.SEVERE, Level.of(java.util.logging.Level.SEVERE));
		assertEquals(Level.WARNING, Level.of(java.util.logging.Level.WARNING));
		assertEquals(Level.INFO, Level.of(java.util.logging.Level.INFO));
		assertEquals(Level.INFO, Level.of(java.util.logging.Level.CONFIG));
		assertEquals(Level.FINE, Level.of(java.util.logging.Level.FINE));
		assertEquals(Level.FINE, Level.of(java.util.logging.Level.FINER));
		assertEquals(Level.FINE, Level.of(java.util.logging.Level.FINEST));
		assertEquals(Level.INFO, Level.of(null));
	}

	@Test void a04_styleWire() {
		assertEquals("success", Style.SUCCESS.wire());
		assertEquals("accent", Style.ACCENT.wire());
	}
}
