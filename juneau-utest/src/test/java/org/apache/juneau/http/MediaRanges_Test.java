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
package org.apache.juneau.http;

import static org.apache.juneau.MediaRanges.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;
import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class MediaRanges_Test extends SimpleTestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Verifies that media type parameters are distinguished from media range extensions.
	//-----------------------------------------------------------------------------------------------------------------
	@Test void a01_extensions() {
		MediaRanges x1;
		MediaRange x2;

		x1 = of("text/json");
		x2 = x1.getRange(0);
		assertString("text/json", x2);
		assertObject(x2.getParameters()).asJson().is("[]");
		assertString("1.0", x2.getQValue());
		assertObject(x2.getExtensions()).asJson().is("[]");

		assertNull(x1.getRange(-1));
		assertNull(x1.getRange(1));

		x1 = of("foo,bar");
		x2 = x1.getRange(0);
		assertString("foo", x2);
		assertObject(x2.getParameters()).asJson().is("[]");
		assertString("1.0", x2.getQValue());
		assertObject(x2.getExtensions()).asJson().is("[]");

		x1 = of(" foo , bar ");
		x2 = x1.getRange(0);
		assertString("foo", x2);
		assertObject(x2.getParameters()).asJson().is("[]");
		assertString("1.0", x2.getQValue());
		assertObject(x2.getExtensions()).asJson().is("[]");

		x1 = of("text/json;a=1;q=0.9;b=2");
		x2 = x1.getRange(0);
		assertString("text/json;a=1;q=0.9;b=2", x2);
		assertObject(x2.getParameters()).asJson().is("[{name:'a',value:'1'}]");
		assertString("0.9", x2.getQValue());
		assertObject(x2.getExtensions()).asJson().is("[{name:'b',value:'2'}]");

		x1 = of("text/json;a=1;a=2;q=0.9;b=3;b=4");
		x2 = x1.getRange(0);
		assertString("text/json;a=1;a=2;q=0.9;b=3;b=4", x2);
		assertObject(x2.getParameters()).asJson().is("[{name:'a',value:'1'},{name:'a',value:'2'}]");
		assertString("0.9", x2.getQValue());
		assertObject(x2.getExtensions()).asJson().is("[{name:'b',value:'3'},{name:'b',value:'4'}]");

		x1 = of("text/json;a=1;a=2;q=1.0;b=3;b=4");
		x2 = x1.getRange(0);
		assertString("text/json;a=1;a=2;q=1.0;b=3;b=4", x2);
		assertObject(x2.getParameters()).asJson().is("[{name:'a',value:'1'},{name:'a',value:'2'}]");
		assertString("1.0", x2.getQValue());
		assertObject(x2.getExtensions()).asJson().is("[{name:'b',value:'3'},{name:'b',value:'4'}]");

		x1 = of("text/json;a=1");
		x2 = x1.getRange(0);
		assertString("text/json;a=1", x2);
		assertObject(x2.getParameters()).asJson().is("[{name:'a',value:'1'}]");
		assertString("1.0", x2.getQValue());
		assertObject(x2.getExtensions()).asJson().is("[]");

		x1 = of("text/json;a=1;");
		x2 = x1.getRange(0);
		assertString("text/json;a=1", x2);
		assertObject(x2.getParameters()).asJson().is("[{name:'a',value:'1'}]");
		assertString("1.0", x2.getQValue());
		assertObject(x2.getExtensions()).asJson().is("[]");

		x1 = of("text/json;q=0.9");
		x2 = x1.getRange(0);
		assertString("text/json;q=0.9", x2);
		assertObject(x2.getParameters()).asJson().is("[]");
		assertString("0.9", x2.getQValue());
		assertObject(x2.getExtensions()).asJson().is("[]");

		x1 = of("text/json;q=0.9;");
		x2 = x1.getRange(0);
		assertString("text/json;q=0.9", x2);
		assertObject(x2.getParameters()).asJson().is("[]");
		assertString("0.9", x2.getQValue());
		assertObject(x2.getExtensions()).asJson().is("[]");
	}

	@Test void a02_hasSubtypePart() {
		MediaRanges mr = of("text/json+x,text/foo+y;q=0.0");
		assertTrue(mr.hasSubtypePart("json"));
		assertTrue(mr.hasSubtypePart("x"));
		assertFalse(mr.hasSubtypePart("foo"));
		assertFalse(mr.hasSubtypePart("y"));
	}

	@Test void a03_ordering() {
		assertString("text/json", of("text/json"));
		assertString("text/json,text/*", of("text/json,text/*"));
		assertString("text/json,text/*", of("text/*,text/json"));
		assertString("text/*,text/*", of("text/*,text/*"));
		assertString("text/*,*/text", of("*/text,text/*"));
		assertString("text/*,*/text", of("text/*,*/text"));
		assertString("a;q=0.9,b;q=0.1", of("a;q=0.9,b;q=0.1"));
		assertString("b;q=0.9,a;q=0.1", of("b;q=0.9,a;q=0.1"));
		assertString("a,b;q=0.9,c;q=0.1,d;q=0.0", of("a,b;q=0.9,c;q=0.1,d;q=0"));
		assertString("a,b;q=0.9,c;q=0.1,d;q=0.0", of("d;q=0,c;q=0.1,b;q=0.9,a"));
		assertString("a,b;q=0.9,c;q=0.1,d;q=0.0", of("a;q=1,b;q=0.9,c;q=0.1,d;q=0"));
		assertString("a,b;q=0.9,c;q=0.1,d;q=0.0", of("d;q=0,c;q=0.1,b;q=0.9,a;q=1"));
		assertString("d,c;q=0.9,b;q=0.1,a;q=0.0", of("a;q=0,b;q=0.1,c;q=0.9,d;q=1"));
		assertString("*", of("*"));
		assertString("", of(""));
		assertString("", of(null));
		assertString("foo/bar/baz", of("foo/bar/baz"));
	}

	@Test void a04_match() {
		MediaRanges x1 = of("text/json");
		assertEquals(0, x1.match(alist(MediaType.of("text/json"))));
		assertEquals(-1, x1.match(alist(MediaType.of("text/foo"))));
		assertEquals(-1, x1.match(alist((MediaType)null)));
		assertEquals(-1, x1.match(null));

		MediaRanges x2 = of("");
		assertEquals(-1, x2.match(alist(MediaType.of("text/json"))));
	}

	@Test void a05_getRanges() {
		MediaRanges x1 = of("text/json");
		assertObject(x1.toList()).asJson().is("['text/json']");
	}
}