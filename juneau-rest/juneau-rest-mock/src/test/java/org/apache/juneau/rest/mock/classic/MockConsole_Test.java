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
package org.apache.juneau.rest.mock.classic;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"resource" // MockConsole instances are lightweight in-memory PrintStream test fixtures backed by a byte array; nothing external to leak.
})
class MockConsole_Test extends TestBase {

	@Test void a01_create() {
		assertNotNull(MockConsole.create());
		assertNotNull(new MockConsole());
	}

	@Test void a02_print_toString_assertContents() {
		var console = MockConsole.create();
		console.print("Hello World");
		assertEquals("Hello World", console.toString());
		console.assertContents().is("Hello World");
	}

	@Test void a03_assertSize() {
		var console = MockConsole.create();
		console.assertSize().is(0);
		console.print("abcde");
		console.assertSize().is(5);
	}

	@Test void a04_reset() {
		var console = MockConsole.create();
		console.print("some text");
		console.assertSize().isGt(0);
		console.reset();
		console.assertSize().is(0);
		assertEquals("", console.toString());
	}
}
