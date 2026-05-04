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
package org.apache.juneau;

import static org.apache.juneau.marshaller.MarshallUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

class ProxyBeanTest extends TestBase {

	//====================================================================================================
	// testBasic
	//====================================================================================================

	public interface A {
		void setFoo(int foo);
		int getFoo();
	}

	@Test void a01_basic() throws Exception {
		var a = json5("{foo:1}", A.class);
		assertEquals(1, a.getFoo());
		a = xml("<object><foo>1</foo></object>", A.class);
		assertEquals(1, a.getFoo());
		a = uon("(foo=1)", A.class);
		assertEquals(1, a.getFoo());
		a = urlEncoding("foo=1", A.class);
		assertEquals(1, a.getFoo());
		a = msgPack("81A3666F6F01", A.class);
		assertEquals(1, a.getFoo());
		a = html("<table><tr><td>foo</td><td>1</td></tr></table>", A.class);
		assertEquals(1, a.getFoo());
		a = openApi("foo=1", A.class);
		assertEquals(1, a.getFoo());
	}
}