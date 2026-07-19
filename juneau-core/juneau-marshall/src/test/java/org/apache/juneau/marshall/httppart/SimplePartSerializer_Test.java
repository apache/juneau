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
package org.apache.juneau.marshall.httppart;

import static org.apache.juneau.commons.httppart.HttpPartType.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class SimplePartSerializer_Test extends TestBase {

	@Test void a01_default_serializesString() {
		var session = SimplePartSerializer.DEFAULT.getPartSession();
		assertEquals("hello", session.write(QUERY, null, "hello"));
	}

	@Test void a02_default_serializesInteger() {
		var session = SimplePartSerializer.DEFAULT.getPartSession();
		assertEquals("42", session.write(HEADER, null, 42));
	}

	@Test void a03_default_serializesNull() {
		var session = SimplePartSerializer.DEFAULT.getPartSession();
		assertNull(session.write(QUERY, null, null));
	}

	@Test void a04_create_build_returnsSameInstance() {
		var s1 = SimplePartSerializer.create().build();
		var s2 = SimplePartSerializer.create().build();
		assertSame(s1, s2);
	}

	@Test void a05_copy_roundtrip() {
		var s = SimplePartSerializer.create().copy().build();
		assertNotNull(s.getPartSession());
	}
}
