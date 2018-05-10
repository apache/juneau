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
package org.apache.juneau.msgpack;

import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.junit.*;

/**
 * Tests the {@link MsgPackParser} class.
 */
public class MsgPackParserTest {
	
	//====================================================================================================
	// testStreamsAutoClose
	// Validates PARSER_autoCloseStreams.
	//====================================================================================================
	@Test
	public void testStreamsAutoClose() throws Exception {
		InputStreamParser p = MsgPackParser.DEFAULT.builder().autoCloseStreams().build();
		Object r;
		InputStream is;
		
		is = is("00 01");
		r = p.parse(is, Object.class);
		assertObjectEquals("0", r);
		try {
			r = p.parse(is, Object.class);
			fail("Exception expected");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("Stream is closed"));
		}
	}
		
	//====================================================================================================
	// testMultipleObjectsInStream
	// Validates that input streams are not closed so that we can read streams of POJOs.
	//====================================================================================================
	@Test
	public void testMultipleObjectsInStream() throws Exception {
		InputStreamParser p = MsgPackParser.DEFAULT;
		Object r;
		InputStream is;
		
		is = is("00 01");
		r = p.parse(is, Object.class);
		assertObjectEquals("0", r);
		r = p.parse(is, Object.class);
		assertObjectEquals("1", r);
		
		is = is("D1 00 80 D1 00 81");
		r = p.parse(is, Object.class);
		assertObjectEquals("128", r);
		r = p.parse(is, Object.class);
		assertObjectEquals("129", r);
		
		is = is("D2 00 00 80 00 D2 00 00 80 01");
		r = p.parse(is, Object.class);
		assertObjectEquals("32768", r);
		r = p.parse(is, Object.class);
		assertObjectEquals("32769", r);

		is = is("CA 00 00 00 00 CA 3F 80 00 00");
		r = p.parse(is, Object.class);
		assertObjectEquals("0.0", r);
		r = p.parse(is, Object.class);
		assertObjectEquals("1.0", r);

		is = is("CB 3F F0 00 00 00 00 00 00 CB BF F0 00 00 00 00 00 00");
		r = p.parse(is, Object.class);
		assertObjectEquals("1.0", r);
		r = p.parse(is, Object.class);
		assertObjectEquals("-1.0", r);

		is = is("A0 A0");
		r = p.parse(is, Object.class);
		assertObjectEquals("''", r);
		r = p.parse(is, Object.class);
		assertObjectEquals("''", r);
		
		is = is("BF 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 BF 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62");
		r = p.parse(is, Object.class);
		assertObjectEquals("'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'", r);
		r = p.parse(is, Object.class);
		assertObjectEquals("'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb'", r);

		is = is("D9 20 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 61 D9 20 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62 62");
		r = p.parse(is, Object.class);
		assertObjectEquals("'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'", r);
		r = p.parse(is, Object.class);
		assertObjectEquals("'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb'", r);

		is = is("90 90");
		r = p.parse(is, Object.class);
		assertObjectEquals("[]", r);
		r = p.parse(is, Object.class);
		assertObjectEquals("[]", r);

		is = is("91 01 91 02");
		r = p.parse(is, Object.class);
		assertObjectEquals("[1]", r);
		r = p.parse(is, Object.class);
		assertObjectEquals("[2]", r);

		is = is("80 80");
		r = p.parse(is, Object.class);
		assertObjectEquals("{}", r);
		r = p.parse(is, Object.class);
		assertObjectEquals("{}", r);

		is = is("81 A1 31 01 81 A1 31 02");
		r = p.parse(is, Object.class);
		assertObjectEquals("{'1':1}", r);
		r = p.parse(is, Object.class);
		assertObjectEquals("{'1':2}", r);
	}

	private InputStream is(String spacedHex) throws Exception {
		return new CloseableByteArrayInputStream(StringUtils.fromSpacedHex(spacedHex));
	}
}
