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
package org.apache.juneau.marshall.yaml;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Additional coverage-focused tests for {@link YamlParserSession}, targeting branches not already exercised
 * by {@link YamlParserSession_Test}:
 *  - {@code readAnything}'s {@code isArray()||isArgs()} arms for both flow-sequence ('[') and block-sequence
 *    ('-') dispatch, reached only via {@link YamlParser#readArgs}.
 *  - {@code isWhitespace}'s tab branch inside the flow-mapping state machine.
 *  - {@code skipToEndOfLine}'s bare-CR line terminator.
 *  - {@code readIntoBeanMapFlow}'s bean-type-property-name skip (a {@code _type} key inside {@code {...}}).
 */
class YamlParserSessionCoverage2_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// a0x - readArgs(): isArgs() arms of readAnything's '[' and '-' dispatch, and the corresponding
	// type.getArg(argIndex++) ternary inside readFlowSequence/readBlockSequence.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_readArgs_flowSequence() throws Exception {
		// Top-level type IS the ARGS ClassMeta itself; '[' input drives readAnything's isArray()||isArgs()
		// arm (isArgs() true) which delegates to readFlowSequence, whose own type.isArgs() ternary picks
		// type.getArg(argIndex++) for each element in turn.
		var session = YamlParser.DEFAULT.getSession();
		var args = session.readArgs("[1, hello]", new java.lang.reflect.Type[] { int.class, String.class });
		assertNotNull(args);
		assertEquals(2, args.length);
		assertEquals(1, args[0]);
		assertEquals("hello", args[1]);
	}

	@Test void a02_readArgs_blockSequence() throws Exception {
		// Same as a01 but via block-sequence ('-') syntax, exercising readAnything's '-' dispatch isArgs()
		// arm and readBlockSequence's own type.getArg(argIndex++) ternary.
		var session = YamlParser.DEFAULT.getSession();
		var args = session.readArgs("- 1\n- hello", new java.lang.reflect.Type[] { int.class, String.class });
		assertNotNull(args);
		assertEquals(2, args.length);
		assertEquals(1, args[0]);
		assertEquals("hello", args[1]);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b0x - isWhitespace: tab character inside the flow-mapping state machine (S4's "looking for value" state).
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_flowMapping_tabBeforeValue() throws Exception {
		var m = YamlParser.DEFAULT.read("{a:\t1}", JsonMap.class);
		assertEquals("1", m.getString("a"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// c0x - skipToEndOfLine: a comment line terminated by a bare '\r' (old-Mac style), not '\n'.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_commentLine_bareCarriageReturnTerminator() throws Exception {
		var m = YamlParser.DEFAULT.read("a: 1\r# comment\rb: 2", JsonMap.class);
		assertEquals("1", m.getString("a"));
		assertEquals("2", m.getString("b"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// d0x - readIntoBeanMapFlow: a "_type" key inside a '{...}' bean mapping is skipped (not treated as an
	// unknown/settable property), exercising the currAttr.equals(getBeanTypePropertyName(...)) true branch.
	//------------------------------------------------------------------------------------------------------------------

	public static class D_Bean {
		public String name;
	}

	@Test void d01_flowBeanMapping_typePropertySkipped() throws Exception {
		var b = YamlParser.DEFAULT.read("{_type: 'org.apache.juneau.marshall.yaml.YamlParserSessionCoverage2_Test$D_Bean', name: Bob}", D_Bean.class);
		assertNotNull(b);
		assertEquals("Bob", b.name);
	}

	//------------------------------------------------------------------------------------------------------------------
	// e0x - readIntoBeanMapBlock: unknown property with a non-null value (isNullBlockValue false), distinct
	// from the null-value variants already covered in YamlParserSession_Test's d0x group.
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_blockBeanMapping_unknownPropertyWithValue() throws Exception {
		var p = YamlParser.create().ignoreUnknownBeanProperties().build();
		var b = p.read("name: Bob\nbogus: 1", D_Bean.class);
		assertNotNull(b);
		assertEquals("Bob", b.name);
	}
}
