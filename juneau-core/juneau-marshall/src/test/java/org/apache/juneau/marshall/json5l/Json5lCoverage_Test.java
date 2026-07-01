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
package org.apache.juneau.marshall.json5l;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

/**
 * Supplemental coverage tests for the {@code json5l} package — exercises edge branches not covered
 * by the primary behavioral tests.
 */
@SuppressWarnings({
	"unchecked", // Parser returns Object; casts in tests
	"resource"   // Token readers/pipes are short-lived test fixtures.
})
class Json5lCoverage_Test extends TestBase {

	@BeanType(properties = "name,age")
	public static class Person {
		public String name;
		public int age;

		public Person() {}
		public Person(String name, int age) {
			this.name = name;
			this.age = age;
		}
	}

	// =================================================================================
	// A. Parser session — doParse array/null branches
	// =================================================================================

	@Test
	void a01_parseToArrayType() throws Exception {
		var in = "{name:'Alice',age:30}\n{name:'Bob',age:25}";
		var arr = Json5lParser.DEFAULT.parse(in, Person[].class);
		assertBean(arr, "0{name,age},1{name,age}", "{Alice,30},{Bob,25}");
	}

	@Test
	void a02_singleTargetNoParseableLineReturnsNull() throws Exception {
		// Only comment/blank lines → the single-value loop falls through to return null.
		var p = Json5lParser.DEFAULT.parse("// just a comment\n\n", Person.class);
		assertNull(p);
	}

	@Test
	void a03_nullInputReturnsNull() throws Exception {
		assertNull(Json5lParser.DEFAULT.parse((String) null, Person.class));
	}

	// =================================================================================
	// B. isParseable — block-comment branch coverage
	// =================================================================================

	@Test
	void b01_blockCommentWithTrailingContentIsParseable() throws Exception {
		// `/* ... */ {a:1}` — closing */ is NOT at end of line, so the line IS parsed.
		var in = "/* lead */ {name:'Alice',age:30}";
		var list = (List<Person>) Json5lParser.DEFAULT.parse(in, List.class, Person.class);
		assertBean(list, "0{name,age}", "{Alice,30}");
	}

	@Test
	void b02_lineNotStartingWithCommentIsParseable() throws Exception {
		var in = "{name:'Alice',age:30}";
		var list = (List<Person>) Json5lParser.DEFAULT.parse(in, List.class, Person.class);
		assertBean(list, "0{name}", "{Alice}");
	}

	// =================================================================================
	// C. Token reader — single-arg constructor + read() delegation disabled
	// =================================================================================

	@Test
	void c01_singleArgConstructor() throws Exception {
		try (var pipe = new ParserPipe("{a:1}\n");
				var r = new Json5lTokenReader(pipe)) {
			assertNotNull(r.next());
		}
	}

	// =================================================================================
	// D. Serializer / builder — copy() paths
	// =================================================================================

	@Test
	void d01_serializerBuilderCopy() {
		var b = Json5lSerializer.create().json5Sugar();
		var copy = b.copy();
		assertTrue(copy.build().isJson5Sugar());
	}

	@Test
	void d02_serializerContextCopy() {
		var s = Json5lSerializer.create().json5Sugar().build();
		assertTrue(s.copy().build().isJson5Sugar());
	}

	@Test
	void d03_parserBuilderCopy() throws Exception {
		var p = Json5lParser.create().copy().build();
		var person = p.parse("{name:'Alice',age:30}", Person.class);
		assertBean(person, "name,age", "Alice,30");
	}

	@Test
	void d04_parserContextCopy() throws Exception {
		var p = Json5lParser.DEFAULT.copy().build();
		var person = p.parse("{name:'Bob',age:25}", Person.class);
		assertBean(person, "name,age", "Bob,25");
	}

	// =================================================================================
	// E. Serializer session — getJsonWriter when output is already a JsonWriter (sugar on)
	// =================================================================================

	@Test
	void e01_sugarSerializeViaStringRoundTrip() throws Exception {
		// Drives the getJsonWriter() sugar branch through serializeToString (which wraps a fresh writer).
		var s = Json5lSerializer.create().json5Sugar().build();
		var out = s.serialize(JsonMap.of("name", "Alice"));
		assertEquals("{name:'Alice'}", out.trim());
	}

	@Test
	void e02_strictSerializeViaStringUsesDoubleQuotes() throws Exception {
		// Drives the getJsonWriter() strict (non-sugar) branch.
		var out = Json5lSerializer.DEFAULT.serialize(JsonMap.of("name", "Alice"));
		assertEquals("{\"name\":\"Alice\"}", out.trim());
	}

	// =================================================================================
	// F. @Json5lConfig annotation appliers (no-op, but must be exercised)
	// =================================================================================

	@Json5lConfig(rank = 1)
	public static class F_Configured {}

	@Test
	void f01_serializerApply() {
		var s = Json5lSerializer.create().applyAnnotations(F_Configured.class).build();
		assertNotNull(s);
	}

	@Test
	void f02_parserApply() {
		var p = Json5lParser.create().applyAnnotations(F_Configured.class).build();
		assertNotNull(p);
	}

	// =================================================================================
	// G. isParseable — remaining branch coverage
	// =================================================================================

	@Test
	void g01_blankLinesOnlyBetweenRecords() throws Exception {
		// Blank line in the array loop hits the isEmpty()==true branch.
		var list = (List<Person>) Json5lParser.DEFAULT.parse("{name:'A'}\n   \n{name:'B'}", List.class, Person.class);
		assertBean(list, "0{name},1{name}", "{A},{B}");
	}

	@Test
	void g02_lineCommentInArrayLoopSkipped() throws Exception {
		// `//`-prefixed line in the array loop hits the startsWith("//")==true branch.
		var list = (List<Person>) Json5lParser.DEFAULT.parse("{name:'A'}\n// skip\n{name:'B'}", List.class, Person.class);
		assertBean(list, "0{name},1{name}", "{A},{B}");
	}

	@Test
	void g03_blockCommentWithTextBeforeCloseIsParseable() throws Exception {
		// `/* */ x` style: indexOf("*/") != end so the line is parsed (the &&-chain's last branch).
		var list = (List<Person>) Json5lParser.DEFAULT.parse("/* a */{name:'A'}", List.class, Person.class);
		assertBean(list, "0{name}", "{A}");
	}
}
