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

import static org.apache.juneau.TestUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.junit.*;

@SuppressWarnings({"javadoc"})
public class ParserGroupTest {

	//====================================================================================================
	// Test parser group matching
	//====================================================================================================
	@Test
	public void testParserGroupMatching() throws Exception {

		ParserGroup g = new ParserGroupBuilder().append(Parser1.class, Parser2.class, Parser3.class).build();
		assertType(Parser1.class, g.getParser("text/foo"));
		assertType(Parser1.class, g.getParser("text/foo_a"));
		assertType(Parser1.class, g.getParser("text/foo_a+xxx"));
		assertType(Parser1.class, g.getParser("text/xxx+foo_a"));
		assertType(Parser2.class, g.getParser("text/foo+bar"));
		assertType(Parser2.class, g.getParser("text/foo+bar_a"));
		assertType(Parser2.class, g.getParser("text/bar+foo"));
		assertType(Parser2.class, g.getParser("text/bar+foo+xxx"));
		assertType(Parser3.class, g.getParser("text/baz"));
		assertType(Parser3.class, g.getParser("text/baz_a"));
		assertType(Parser3.class, g.getParser("text/baz+yyy"));
		assertType(Parser3.class, g.getParser("text/baz_a+yyy"));
		assertType(Parser3.class, g.getParser("text/yyy+baz"));
		assertType(Parser3.class, g.getParser("text/yyy+baz_a"));
	}


	public static class Parser1 extends JsonParser {
		public Parser1(PropertyStore propertyStore) {
			super(propertyStore, "text/foo", "text/foo_a");
		}
	}

	public static class Parser2 extends JsonParser {
		public Parser2(PropertyStore propertyStore) {
			super(propertyStore, "text/foo+bar", "text/foo+bar_a");
		}
	}

	public static class Parser3 extends JsonParser {
		public Parser3(PropertyStore propertyStore) {
			super(propertyStore, "text/baz", "text/baz_a");
		}
	}

	//====================================================================================================
	// Test inheritence
	//====================================================================================================
	@Test
	public void testInheritence() throws Exception {
		ParserGroupBuilder gb = null;
		ParserGroup g = null;

		gb = new ParserGroupBuilder().append(P1.class, P2.class);
		g = gb.build();
		assertObjectEquals("['text/1','text/2','text/2a']", g.getSupportedMediaTypes());

		gb = new ParserGroupBuilder(g).append(P3.class, P4.class);
		g = gb.build();
		assertObjectEquals("['text/3','text/4','text/4a','text/1','text/2','text/2a']", g.getSupportedMediaTypes());

		gb = new ParserGroupBuilder(g).append(P5.class);
		g = gb.build();
		assertObjectEquals("['text/5','text/3','text/4','text/4a','text/1','text/2','text/2a']", g.getSupportedMediaTypes());
	}

	public static class P1 extends JsonParser {
		public P1(PropertyStore propertyStore) {
			super(propertyStore, "text/1");
		}
	}

	public static class P2 extends JsonParser {
		public P2(PropertyStore propertyStore) {
			super(propertyStore, "text/2", "text/2a");
		}
	}

	public static class P3 extends JsonParser {
		public P3(PropertyStore propertyStore) {
			super(propertyStore, "text/3");
		}
	}

	public static class P4 extends JsonParser {
		public P4(PropertyStore propertyStore) {
			super(propertyStore, "text/4", "text/4a");
		}
	}

	public static class P5 extends JsonParser {
		public P5(PropertyStore propertyStore) {
			super(propertyStore, "text/5");
		}
	}
}
