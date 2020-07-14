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
package org.apache.juneau.http.header;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.http.*;
import org.junit.*;

/**
 * Verifies that the Accept class handles parameters and extensions correctly.
 */
@FixMethodOrder(NAME_ASCENDING)
public class AcceptExtensionsTest {

	//-----------------------------------------------------------------------------------------------------------------
	// Verifies that media type parameters are distinguished from media range extensions.
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testExtensions() throws Exception {
		Accept accept;
		MediaRange mr;

		accept = Accept.of("text/json");
		mr = accept.getRange(0);
		assertString(mr).is("text/json");
		assertObject(mr.getParameters()).json().is("[]");
		assertString(mr.getQValue()).is("1.0");
		assertObject(mr.getExtensions()).json().is("[]");

		accept = Accept.of("foo,bar");
		mr = accept.getRange(0);
		assertString(mr).is("foo");
		assertObject(mr.getParameters()).json().is("[]");
		assertString(mr.getQValue()).is("1.0");
		assertObject(mr.getExtensions()).json().is("[]");

		accept = Accept.of(" foo , bar ");
		mr = accept.getRange(0);
		assertString(mr).is("foo");
		assertObject(mr.getParameters()).json().is("[]");
		assertString(mr.getQValue()).is("1.0");
		assertObject(mr.getExtensions()).json().is("[]");

		accept = Accept.of("text/json;a=1;q=0.9;b=2");
		mr = accept.getRange(0);
		assertString(mr).is("text/json;a=1;q=0.9;b=2");
		assertObject(mr.getParameters()).json().is("['a=1']");
		assertString(mr.getQValue()).is("0.9");
		assertObject(mr.getExtensions()).json().is("['b=2']");

		accept = Accept.of("text/json;a=1;a=2;q=0.9;b=3;b=4");
		mr = accept.getRange(0);
		assertString(mr).is("text/json;a=1;a=2;q=0.9;b=3;b=4");
		assertObject(mr.getParameters()).json().is("['a=1','a=2']");
		assertString(mr.getQValue()).is("0.9");
		assertObject(mr.getExtensions()).json().is("['b=3','b=4']");

		accept = Accept.of("text/json;a=1");
		mr = accept.getRange(0);
		assertString(mr).is("text/json;a=1");
		assertObject(mr.getParameters()).json().is("['a=1']");
		assertString(mr.getQValue()).is("1.0");
		assertObject(mr.getExtensions()).json().is("[]");

		accept = Accept.of("text/json;a=1;");
		mr = accept.getRange(0);
		assertString(mr).is("text/json;a=1");
		assertObject(mr.getParameters()).json().is("['a=1']");
		assertString(mr.getQValue()).is("1.0");
		assertObject(mr.getExtensions()).json().is("[]");

		accept = Accept.of("text/json;q=0.9");
		mr = accept.getRange(0);
		assertString(mr).is("text/json;q=0.9");
		assertObject(mr.getParameters()).json().is("[]");
		assertString(mr.getQValue()).is("0.9");
		assertObject(mr.getExtensions()).json().is("[]");

		accept = Accept.of("text/json;q=0.9;");
		mr = accept.getRange(0);
		assertString(mr).is("text/json;q=0.9");
		assertObject(mr.getParameters()).json().is("[]");
		assertString(mr.getQValue()).is("0.9");
		assertObject(mr.getExtensions()).json().is("[]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Tests the Accept.hasSubtypePart() method.
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void testHasSubtypePart() {
		Accept accept = Accept.of("text/json+x,text/foo+y;q=0.0");
		MediaRanges mr = accept.asRanges();
		assertTrue(mr.hasSubtypePart("json"));
		assertTrue(mr.hasSubtypePart("x"));
		assertFalse(mr.hasSubtypePart("foo"));
		assertFalse(mr.hasSubtypePart("y"));
	}
}
