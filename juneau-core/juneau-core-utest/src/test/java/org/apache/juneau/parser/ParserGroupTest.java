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
package org.apache.juneau.parser;

import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ParserGroupTest {

	//====================================================================================================
	// Test parser group matching
	//====================================================================================================
	@Test
	public void testParserGroupMatching() throws Exception {

		ParserGroup g = ParserGroup.create().append(Parser1.class, Parser2.class, Parser3.class).build();
		assertInstanceOf(Parser1.class, g.getParser("text/foo"));
		assertInstanceOf(Parser1.class, g.getParser("text/foo_a"));
		assertInstanceOf(Parser1.class, g.getParser("text/foo_a+xxx"));
		assertInstanceOf(Parser1.class, g.getParser("text/xxx+foo_a"));
		assertInstanceOf(Parser2.class, g.getParser("text/foo+bar"));
		assertInstanceOf(Parser2.class, g.getParser("text/foo+bar_a"));
		assertInstanceOf(Parser2.class, g.getParser("text/bar+foo"));
		assertInstanceOf(Parser2.class, g.getParser("text/bar+foo+xxx"));
		assertInstanceOf(Parser3.class, g.getParser("text/baz"));
		assertInstanceOf(Parser3.class, g.getParser("text/baz_a"));
		assertInstanceOf(Parser3.class, g.getParser("text/baz+yyy"));
		assertInstanceOf(Parser3.class, g.getParser("text/baz_a+yyy"));
		assertInstanceOf(Parser3.class, g.getParser("text/yyy+baz"));
		assertInstanceOf(Parser3.class, g.getParser("text/yyy+baz_a"));
	}


	public static class Parser1 extends JsonParser {
		public Parser1(PropertyStore ps) {
			super(ps, "text/foo", "text/foo_a");
		}
	}

	public static class Parser2 extends JsonParser {
		public Parser2(PropertyStore ps) {
			super(ps, "text/foo+bar", "text/foo+bar_a");
		}
	}

	public static class Parser3 extends JsonParser {
		public Parser3(PropertyStore ps) {
			super(ps, "text/baz", "text/baz_a");
		}
	}

	//====================================================================================================
	// Test inheritence
	//====================================================================================================
	@Test
	public void testInheritence() throws Exception {
		ParserGroupBuilder gb = null;
		ParserGroup g = null;

		gb = ParserGroup.create().append(P1.class, P2.class);
		g = gb.build();
		assertObjectEquals("['text/1','text/2','text/2a']", g.getSupportedMediaTypes());

		gb = g.builder().append(P3.class, P4.class);
		g = gb.build();
		assertObjectEquals("['text/3','text/4','text/4a','text/1','text/2','text/2a']", g.getSupportedMediaTypes());

		gb = g.builder().append(P5.class);
		g = gb.build();
		assertObjectEquals("['text/5','text/3','text/4','text/4a','text/1','text/2','text/2a']", g.getSupportedMediaTypes());
	}

	public static class P1 extends JsonParser {
		public P1(PropertyStore ps) {
			super(ps, "text/1");
		}
	}

	public static class P2 extends JsonParser {
		public P2(PropertyStore ps) {
			super(ps, "text/2", "text/2a");
		}
	}

	public static class P3 extends JsonParser {
		public P3(PropertyStore ps) {
			super(ps, "text/3");
		}
	}

	public static class P4 extends JsonParser {
		public P4(PropertyStore ps) {
			super(ps, "text/4", "text/4a");
		}
	}

	public static class P5 extends JsonParser {
		public P5(PropertyStore ps) {
			super(ps, "text/5");
		}
	}
}
