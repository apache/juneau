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
package org.apache.juneau;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.apache.juneau.json.JsonSerializer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Verifies that the MediaRange and MediaType classes parse and sort Accept headers correctly.
 */
@RunWith(Parameterized.class)
public class MediaRangeTest {
	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{ "0", "text/json", "['text/json']" },
			{ "1", "text/json,text/*", "['text/json','text/*']" },
			{ "2", "text/*,text/json", "['text/json','text/*']" },
			{ "3", "text/*,text/*", "['text/*']" },
			{ "4", "*/text,text/*", "['text/*','*/text']" },
			{ "5", "text/*,*/text", "['text/*','*/text']" },
			{ "6", "A;q=0.9,B;q=0.1", "['a;q=0.9','b;q=0.1']" },
			{ "7", "B;q=0.9,A;q=0.1", "['b;q=0.9','a;q=0.1']" },
			{ "8", "A,B;q=0.9,C;q=0.1,D;q=0", "['a','b;q=0.9','c;q=0.1','d;q=0.0']" },
			{ "9", "D;q=0,C;q=0.1,B;q=0.9,A", "['a','b;q=0.9','c;q=0.1','d;q=0.0']" },
			{ "10", "A;q=1,B;q=0.9,C;q=0.1,D;q=0", "['a','b;q=0.9','c;q=0.1','d;q=0.0']" },
			{ "11", "D;q=0,C;q=0.1,B;q=0.9,A;q=1", "['a','b;q=0.9','c;q=0.1','d;q=0.0']" },
			{ "12", "A;q=0,B;q=0.1,C;q=0.9,D;q=1", "['d','c;q=0.9','b;q=0.1','a;q=0.0']" },
			{ "13", "*", "['*']" },
			{ "14", "", "['*/*']" },
			{ "15", null, "['*/*']" },
			{ "16", "foo/bar/baz", "['foo/bar/baz']" },
		});
	}
	
	private String label, mediaRange, expected;
	
	public MediaRangeTest(String label, String mediaRange, String expected) {
		this.label = label;
		this.mediaRange = mediaRange;
		this.expected = expected;
	}
	
	@Test
	public void test() {
		MediaRange[] r = MediaRange.parse(mediaRange);
		assertEquals(label + " failed", expected, JsonSerializer.DEFAULT_LAX.toString(r));
	}
}
