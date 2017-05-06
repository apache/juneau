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
package org.apache.juneau.https;

import static org.apache.juneau.TestUtils.*;
import static org.junit.Assert.*;

import org.apache.juneau.http.*;
import org.junit.*;

/**
 * Verifies that the Accept class handles parameters and extensions correctly.
 */
public class AcceptExtensionsTest {

	//--------------------------------------------------------------------------------
	// Verifies that media type parameters are distinguished from media range extensions.
	//--------------------------------------------------------------------------------
	@Test
	public void testExtensions() throws Exception {
		Accept accept;
		MediaTypeRange mr;
		
		accept = Accept.forString("text/json");
		mr = accept.asRanges().get(0);
		assertTextEquals("text/json", mr);
		assertTextEquals("text/json", mr.getMediaType());
		assertObjectEquals("{}", mr.getMediaType().getParameters());
		assertTextEquals("1.0", mr.getQValue());
		assertObjectEquals("{}", mr.getExtensions());

		accept = Accept.forString("foo,bar");
		mr = accept.asRanges().get(0);
		assertTextEquals("foo", mr);
		assertTextEquals("foo", mr.getMediaType());
		assertObjectEquals("{}", mr.getMediaType().getParameters());
		assertTextEquals("1.0", mr.getQValue());
		assertObjectEquals("{}", mr.getExtensions());

		accept = Accept.forString(" foo , bar ");
		mr = accept.asRanges().get(0);
		assertTextEquals("foo", mr);
		assertTextEquals("foo", mr.getMediaType());
		assertObjectEquals("{}", mr.getMediaType().getParameters());
		assertTextEquals("1.0", mr.getQValue());
		assertObjectEquals("{}", mr.getExtensions());

		accept = Accept.forString("text/json;a=1;q=0.9;b=2");
		mr = accept.asRanges().get(0);
		assertTextEquals("text/json;a=1;q=0.9;b=2", mr);
		assertTextEquals("text/json;a=1", mr.getMediaType());
		assertObjectEquals("{a:['1']}", mr.getMediaType().getParameters());
		assertTextEquals("0.9", mr.getQValue());
		assertObjectEquals("{b:['2']}", mr.getExtensions());
		
		accept = Accept.forString("text/json;a=1;a=2;q=0.9;b=3;b=4");
		mr = accept.asRanges().get(0);
		assertTextEquals("text/json;a=1;a=2;q=0.9;b=3;b=4", mr);
		assertTextEquals("text/json;a=1;a=2", mr.getMediaType());
		assertObjectEquals("{a:['1','2']}", mr.getMediaType().getParameters());
		assertTextEquals("0.9", mr.getQValue());
		assertObjectEquals("{b:['3','4']}", mr.getExtensions());

		accept = Accept.forString("text/json;a=1");
		mr = accept.asRanges().get(0);
		assertTextEquals("text/json;a=1", mr);
		assertTextEquals("text/json;a=1", mr.getMediaType());
		assertObjectEquals("{a:['1']}", mr.getMediaType().getParameters());
		assertTextEquals("1.0", mr.getQValue());
		assertObjectEquals("{}", mr.getExtensions());

		accept = Accept.forString("text/json;a=1;");
		mr = accept.asRanges().get(0);
		assertTextEquals("text/json;a=1", mr);
		assertTextEquals("text/json;a=1", mr.getMediaType());
		assertObjectEquals("{a:['1']}", mr.getMediaType().getParameters());
		assertTextEquals("1.0", mr.getQValue());
		assertObjectEquals("{}", mr.getExtensions());
		
		accept = Accept.forString("text/json;q=0.9");
		mr = accept.asRanges().get(0);
		assertTextEquals("text/json;q=0.9", mr);
		assertTextEquals("text/json", mr.getMediaType());
		assertObjectEquals("{}", mr.getMediaType().getParameters());
		assertTextEquals("0.9", mr.getQValue());
		assertObjectEquals("{}", mr.getExtensions());

		accept = Accept.forString("text/json;q=0.9;");
		mr = accept.asRanges().get(0);
		assertTextEquals("text/json;q=0.9", mr);
		assertTextEquals("text/json", mr.getMediaType());
		assertObjectEquals("{}", mr.getMediaType().getParameters());
		assertTextEquals("0.9", mr.getQValue());
		assertObjectEquals("{}", mr.getExtensions());
	}
	
	//--------------------------------------------------------------------------------
	// Tests the Accept.hasSubtypePart() method.
	//--------------------------------------------------------------------------------
	@Test
	public void testHasSubtypePart() {
		Accept accept = Accept.forString("text/json+x,text/foo+y;q=0.0");
		assertTrue(accept.hasSubtypePart("json"));
		assertTrue(accept.hasSubtypePart("x"));
		assertFalse(accept.hasSubtypePart("foo"));
		assertFalse(accept.hasSubtypePart("y"));
	}
}
