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
package org.apache.juneau.transforms;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.swaps.*;
import org.junit.jupiter.api.*;

/**
 * Tests the {@link StackTraceElementSwap} class.
 */
class StackTraceElementSwap_Test extends TestBase {

	private String write(StackTraceElement ste) {
		return Json5.of(ste);
	}

	private StackTraceElement read(String in) throws Exception {
		return Json5.DEFAULT.read(in, StackTraceElement.class);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------
	@Test void basicTests() throws Exception {
		var ste = new StackTraceElement("foo.bar.Baz", "qux", "Baz.java", 123);

		assertEquals("'foo.bar.Baz.qux(Baz.java:123)'", write(ste));
		assertEquals("'foo.bar.Baz.qux(Baz.java:123)'", write(read("'foo.bar.Baz.qux(Baz.java:123)'")));

		ste = new StackTraceElement("foo.bar.Baz", "qux", "Baz.java", -2);
		assertEquals("'foo.bar.Baz.qux(Native Method)'", write(ste));
		assertEquals("'foo.bar.Baz.qux(Native Method)'", write(read("'foo.bar.Baz.qux(Native Method)'")));

		ste = new StackTraceElement("foo.bar.Baz", "qux", null, 0);
		assertEquals("'foo.bar.Baz.qux(Unknown Source)'", write(ste));
		assertEquals("'foo.bar.Baz.qux(Unknown Source)'", write(read("'foo.bar.Baz.qux(Unknown Source)'")));

		ste = new StackTraceElement("foo.bar.Baz", "qux", "Baz.java", -1);
		assertEquals("'foo.bar.Baz.qux(Baz.java)'", write(ste));
		assertEquals("'foo.bar.Baz.qux(Baz.java)'", write(read("'foo.bar.Baz.qux(Baz.java)'")));
	}
}