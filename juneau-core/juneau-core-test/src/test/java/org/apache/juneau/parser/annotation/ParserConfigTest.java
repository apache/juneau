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
package org.apache.juneau.parser.annotation;

import static org.junit.Assert.*;
import static org.apache.juneau.parser.Parser.*;
import static org.apache.juneau.parser.InputStreamParser.*;
import static org.apache.juneau.parser.ReaderParser.*;

import java.util.function.*;

import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.utils.*;
import org.junit.*;

/**
 * Tests the @ParserConfig annotation.
 */
public class ParserConfigTest {

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = new Function<Object,String>() {
		@Override
		public String apply(Object t) {
			if (t == null)
				return null;
			if (t instanceof Class)
				return ((Class<?>)t).getSimpleName();
			return t.toString();
		}
	};

	static StringResolver sr = new StringResolver() {
		@Override
		public String resolve(String input) {
			if (input.startsWith("$"))
				input = input.substring(1);
			return input;
		}
	};

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	static class AA extends ParserListener {}

	@ParserConfig(
		autoCloseStreams="$true",
		binaryFormat="HEX",
		debugOutputLines="$1",
		fileCharset="$foo",
		inputStreamCharset="$foo",
		listener=AA.class,
		strict="$true",
		trimStrings="$true",
		unbuffered="$true"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test
	public void basicReaderParser() throws Exception {
		AnnotationsMap m = a.getAnnotationsMap();
		JsonParser x = JsonParser.create().applyAnnotations(m, sr).build();
		check("true", x.getProperty(PARSER_autoCloseStreams));
		check(null, x.getProperty(ISPARSER_binaryFormat));
		check("1", x.getProperty(PARSER_debugOutputLines));
		check("foo", x.getProperty(RPARSER_fileCharset));
		check("foo", x.getProperty(RPARSER_inputStreamCharset));
		check("AA", x.getProperty(PARSER_listener));
		check("true", x.getProperty(PARSER_strict));
		check("true", x.getProperty(PARSER_trimStrings));
		check("true", x.getProperty(PARSER_unbuffered));
	}

	@Test
	public void basicInputStreamParser() throws Exception {
		AnnotationsMap m = a.getAnnotationsMap();
		MsgPackParser x = MsgPackParser.create().applyAnnotations(m, sr).build();
		check("true", x.getProperty(PARSER_autoCloseStreams));
		check("HEX", x.getProperty(ISPARSER_binaryFormat));
		check("1", x.getProperty(PARSER_debugOutputLines));
		check(null, x.getProperty(RPARSER_fileCharset));
		check(null, x.getProperty(RPARSER_inputStreamCharset));
		check("AA", x.getProperty(PARSER_listener));
		check("true", x.getProperty(PARSER_strict));
		check("true", x.getProperty(PARSER_trimStrings));
		check("true", x.getProperty(PARSER_unbuffered));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@ParserConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test
	public void noValuesReaderParser() throws Exception {
		AnnotationsMap m = b.getAnnotationsMap();
		JsonParser x = JsonParser.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(PARSER_autoCloseStreams));
		check(null, x.getProperty(ISPARSER_binaryFormat));
		check(null, x.getProperty(PARSER_debugOutputLines));
		check(null, x.getProperty(RPARSER_fileCharset));
		check(null, x.getProperty(RPARSER_inputStreamCharset));
		check(null, x.getProperty(PARSER_listener));
		check(null, x.getProperty(PARSER_strict));
		check(null, x.getProperty(PARSER_trimStrings));
		check(null, x.getProperty(PARSER_unbuffered));
	}

	@Test
	public void noValuesInputStreamParser() throws Exception {
		AnnotationsMap m = b.getAnnotationsMap();
		MsgPackParser x = MsgPackParser.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(PARSER_autoCloseStreams));
		check(null, x.getProperty(ISPARSER_binaryFormat));
		check(null, x.getProperty(PARSER_debugOutputLines));
		check(null, x.getProperty(RPARSER_fileCharset));
		check(null, x.getProperty(RPARSER_inputStreamCharset));
		check(null, x.getProperty(PARSER_listener));
		check(null, x.getProperty(PARSER_strict));
		check(null, x.getProperty(PARSER_trimStrings));
		check(null, x.getProperty(PARSER_unbuffered));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test
	public void noAnnotationReaderParser() throws Exception {
		AnnotationsMap m = c.getAnnotationsMap();
		JsonParser x = JsonParser.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(PARSER_autoCloseStreams));
	}

	@Test
	public void noAnnotationInputStreamParser() throws Exception {
		AnnotationsMap m = c.getAnnotationsMap();
		MsgPackParser x = MsgPackParser.create().applyAnnotations(m, sr).build();
		check(null, x.getProperty(PARSER_autoCloseStreams));
	}
}
