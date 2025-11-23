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
package org.apache.juneau.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.junit.jupiter.api.*;

class ParserReaderTest extends TestBase {

	//====================================================================================================
	// test
	//====================================================================================================
	@Test void a01_test() throws Exception {
		try (var r = new ParserReader(new ParserPipe("abc123"))) {
			assertEquals('a', r.read());
			r.unread();
			assertEquals('a', r.read());
			assertEquals('b', r.read());
			r.unread();
			assertEquals("bc", r.read(2));
			assertEquals('1', r.read());
			r.unread();
			r.read();
			assertEquals('2', r.peek());
			assertEquals('2', r.peek());
			assertEquals('2', r.read());
			assertEquals('3', r.read());
			assertEquals(-1, r.read());
			assertEquals(-1, r.read());
		}
	}
}