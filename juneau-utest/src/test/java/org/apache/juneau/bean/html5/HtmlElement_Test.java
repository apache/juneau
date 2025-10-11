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
package org.apache.juneau.bean.html5;

import static org.apache.juneau.bean.html5.HtmlBuilder.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class HtmlElement_Test extends TestBase {

	@Test void a01_getAttr_withNull() {
		A x = a();
		assertNull(x.getAttr(String.class, "nonexistent"));
	}

	@Test void a02_getAttr_withValue() {
		A x = a().href("test");
		assertString("test", x.getAttr(String.class, "href"));
	}

	@Test void a03_deminimize() {
		Button x1 = button().disabled(true);
		assertString("<button disabled='disabled'></button>", x1);
		
		Button x2 = button().disabled(false);
		assertString("<button></button>", x2);
		
		Button x3 = button().disabled("custom");
		assertString("<button disabled='custom'></button>", x3);
	}

	@Test void a04_attr_withNull() {
		A x = a().href("test");
		assertString("<a href='test'></a>", x);
		x.attr("href", null);
		assertString("<a></a>", x);
	}

	@Test void a05_attr_urlConversion() {
		A x = a();
		x.attr("url", "http://example.com");
		assertString("http://example.com", x.getAttr(String.class, "url"));
		
		A x2 = a();
		x2.attr("href", "http://example.com");
		assertString("http://example.com", x2.getAttr(String.class, "href"));
		
		Form x3 = form();
		x3.attr("action", "http://example.com");
		assertString("http://example.com", x3.getAttr(String.class, "action"));
	}
}