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
package org.apache.juneau.objecttools;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ObjectIntrospector_Test {

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test
	public void a01_Basic() throws Exception {
		String in = null;
		Object r;

		r = new ObjectIntrospector(in, null).invokeMethod("substring(int,int)", "[3,6]");
		assertNull(r);

		in = "foobar";
		r = new ObjectIntrospector(in).invokeMethod("substring(int,int)", "[3,6]");
		assertEquals("bar", r);

		r = new ObjectIntrospector(in).invokeMethod("toString", null);
		assertEquals("foobar", r);

		r = new ObjectIntrospector(in).invokeMethod("toString", "");
		assertEquals("foobar", r);

		r = new ObjectIntrospector(in).invokeMethod("toString", "[]");
		assertEquals("foobar", r);

		assertThrown(()->new ObjectIntrospector("foobar").invokeMethod("noSuchMethod", "[3,6]")).isType(NoSuchMethodException.class);

		r = new ObjectIntrospector(null).invokeMethod(String.class.getMethod("toString"), null);
		assertNull(r);

		r = new ObjectIntrospector("foobar").invokeMethod(String.class.getMethod("toString"), null);
		assertEquals("foobar", r);
	}
}
