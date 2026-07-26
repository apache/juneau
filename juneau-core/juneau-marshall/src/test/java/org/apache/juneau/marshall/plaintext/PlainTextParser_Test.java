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
package org.apache.juneau.marshall.plaintext;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link PlainTextParser}.
 *
 * <p>
 * {@link PlainTextParserSession#doRead(org.apache.juneau.marshall.parser.ParserPipe, ClassMeta)} is a thin
 * wrapper delegating straight to {@code convertToType(...)}; there was no test in this module exercising the
 * class directly, so this file covers both the happy path and the type-conversion error path.
 */
class PlainTextParser_Test extends TestBase {

	@Test void a01_readString() throws Exception {
		assertEquals("foo", PlainTextParser.DEFAULT.read("foo", String.class));
	}

	@Test void a02_readInteger() throws Exception {
		assertEquals(123, PlainTextParser.DEFAULT.read("123", Integer.class));
	}

	@Test void a03_readEmptyString() throws Exception {
		assertEquals("", PlainTextParser.DEFAULT.read("", String.class));
	}

	//====================================================================================================
	// Malformed input -- convertToType(...) error path for an incompatible target type
	//====================================================================================================

	@Test void b01_unparsableNumberThrows() {
		assertThrows(Exception.class, ()->PlainTextParser.DEFAULT.read("not-a-number", Integer.class));
	}
}
