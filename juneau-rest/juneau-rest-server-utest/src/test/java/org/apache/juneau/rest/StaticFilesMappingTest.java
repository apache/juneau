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
package org.apache.juneau.rest;

import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.parser.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class StaticFilesMappingTest {

	private static Collection<StaticFileMapping> parse(String input) throws ParseException {
		return StaticFileMapping.parse(null, input);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Empty input.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_null() throws Exception {
		assertObjectEquals("[]", parse(null));
	}

	@Test
	public void a02_emptySpaces() throws Exception {
		assertObjectEquals("[]", parse("    "));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_basic2Part() throws Exception {
		assertObjectEquals("[{path:'foo',location:'bar'}]", parse("foo:bar"));
	}

	@Test
	public void b02_basic2Part_withSpaces() throws Exception {
		assertObjectEquals("[{path:'foo',location:'bar'}]", parse("  foo  :  bar  "));
	}

	@Test
	public void b03_basic3Part() throws Exception {
		assertObjectEquals(
			"[{path:'foo',location:'bar',responseHeaders:{baz:'qux'}}]",
			parse("foo:bar:{baz:'qux'}")
		);
	}

	@Test
	public void b04_basic3Part_withSpaces() throws Exception {
		assertObjectEquals(
			"[{path:'foo',location:'bar',responseHeaders:{baz:'qux'}}]",
			parse("  foo  :  bar  :  {  baz  :  'qux'  }  ")
		);
	}

	@Test
	public void b05_multipleHeaders() throws Exception {
		assertObjectEquals(
			"[{path:'foo',location:'bar',responseHeaders:{baz:'qux',qux:'quux'}}]",
			parse("foo:bar:{baz:'qux',qux:'quux'}")
		);
	}

	@Test
	public void b06_multipleHeaders_withSpaces() throws Exception {
		assertObjectEquals(
			"[{path:'foo',location:'bar',responseHeaders:{baz:'qux',qux:'quux'}}]",
			parse("  foo  :  bar  :  {  baz  :  'qux'  ,  qux:  'quux'  }  ")
		);
	}

	@Test
	public void b07_nestedHeaders() throws Exception {
		assertObjectEquals(
			"[{path:'foo',location:'bar',responseHeaders:{a:{b:'c'}}}]",
			parse("foo:bar:{a:{b:'c'}}")
		);
	}

	@Test
	public void b08_nestedHeaders_complex() throws Exception {
		assertObjectEquals(
			"[{path:'foo',location:'bar',responseHeaders:{a:{b:'c',d:'e'},f:{g:'h',i:'j'}}}]",
			parse("foo:bar:{a:{b:'c',d:'e'},f:{g:'h',i:'j'}}")
		);
	}

	@Test
	public void b09_nestedHeaders_complex_withSpaces() throws Exception {
		assertObjectEquals(
			"[{path:'foo',location:'bar',responseHeaders:{a:{b:'c',d:'e'},f:{g:'h',i:'j'}}}]",
			parse("  foo  :  bar  :  {  a  :  {  b  :  'c'  ,  d  :  'e'  }  ,  f  :  {  g  :  'h'  ,  i  :  'j'  }  }  ")
		);
	}

	@Test
	public void b10_emptyHeaders() throws Exception {
		assertObjectEquals(
			"[{path:'foo',location:'bar'}]",
			parse("foo:bar:{}")
		);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Multiple mappings
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void c01_multipleMappings_basic2Part() throws Exception {
		assertObjectEquals(
			"[{path:'foo',location:'bar'},{path:'baz',location:'qux'}]",
			parse("foo:bar,baz:qux")
		);
	}

	@Test
	public void c02_multipleMappings_basic2Part_withSpaces() throws Exception {
		assertObjectEquals(
			"[{path:'foo',location:'bar'},{path:'baz',location:'qux'}]",
			parse("  foo  :  bar  ,  baz  :  qux  ")
		);
	}

	@Test
	public void c03_multipleMappings_basic3Part() throws Exception {
		assertObjectEquals(
			"[{path:'foo',location:'bar',responseHeaders:{a:'b'}},{path:'baz',location:'qux',responseHeaders:{c:'d'}}]",
			parse("foo:bar:{a:'b'},baz:qux:{c:'d'}")
		);
	}

	@Test
	public void c04_multipleMappings_basic3Part_withSpaces() throws Exception {
		assertObjectEquals(
			"[{path:'foo',location:'bar',responseHeaders:{a:'b'}},{path:'baz',location:'qux',responseHeaders:{c:'d'}}]",
			parse("  foo  :  bar  :  {  a  :  'b'  }  ,  baz  :  qux  :  {  c  :  'd'  }  ")
		);
	}

	@Test
	public void c05_multipleMappings_multipleHeaders() throws Exception {
		assertObjectEquals(
			"[{path:'foo',location:'bar',responseHeaders:{a:'b',c:'d'}},{path:'baz',location:'qux',responseHeaders:{e:'f',g:'h'}}]",
			parse("foo:bar:{a:'b',c:'d'},baz:qux:{e:'f',g:'h'}")
		);
	}

	@Test
	public void c06_multipleMappings_multipleHeaders_withSpaces() throws Exception {
		assertObjectEquals(
			"[{path:'foo',location:'bar',responseHeaders:{a:'b',c:'d'}},{path:'baz',location:'qux',responseHeaders:{e:'f',g:'h'}}]",
			parse("  foo  :  bar  :  {  a  :  'b'  ,  c  :  'd'  }  ,  baz  :  qux  :  {  e  :  'f'  ,  g  :  'h'  }  ")
		);
	}

	@Test
	public void c07_multipleMappings_nestedHeaders() throws Exception {
		assertObjectEquals(
			"[{path:'foo',location:'bar',responseHeaders:{a:{b:'c'}}},{path:'baz',location:'qux',responseHeaders:{d:{e:'f'}}}]",
			parse("foo:bar:{a:{b:'c'}},baz:qux:{d:{e:'f'}}")
		);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Error conditions
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void d01_error_malformedJson() {
		try {
			parse("foo:bar:xxx");
			fail("Exception expected.");
		} catch (ParseException e) {
			assertContains(e, "Expected { at beginning of headers.");
		}
	}

	@Test
	public void d02_error_textFollowingJson() {
		try {
			parse("foo:bar:{a:'b'}x");
			fail("Exception expected.");
		} catch (ParseException e) {
			assertContains(e, "Invalid text following headers.");
		}
	}

	@Test
	public void d03_error_missingLocation() {
		try {
			parse("foo");
			fail("Exception expected.");
		} catch (ParseException e) {
			assertContains(e, "Couldn't find ':' following path.");
		}
	}

	@Test
	public void d04_error_danglingColonAfterLocation() {
		try {
			parse("foo:bar:");
			fail("Exception expected.");
		} catch (ParseException e) {
			assertContains(e, "Found extra ':' following location.");
		}
	}

	@Test
	public void d05_error_malformedHeaders_openEnded() {
		try {
			parse("foo:bar:{");
			fail("Exception expected.");
		} catch (ParseException e) {
			assertContains(e, "Malformed headers.");
		}
	}

	@Test
	public void d06_error_malformedHeaders_mismatchedBrackets() {
		try {
			parse("foo:bar:{{}");
			fail("Exception expected.");
		} catch (ParseException e) {
			assertContains(e, "Malformed headers.");
		}
	}
}
