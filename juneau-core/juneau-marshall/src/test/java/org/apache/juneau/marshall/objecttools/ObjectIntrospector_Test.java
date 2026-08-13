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
package org.apache.juneau.marshall.objecttools;

import static org.apache.juneau.commons.utils.ObjectUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ObjectIntrospector_Test extends TestBase {

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test void a01_Basic() throws Exception {
		var in = nullObject(String.class);

		// Null target object short-circuits before the allow-list check, regardless of configuration.
		var r = new ObjectIntrospector(in, null).invokeMethod("substring(int,int)", "[3,6]");
		assertNull(r);

		in = "foobar";
		r = new ObjectIntrospector(in).allow(String.class, "substring(int,int)").invokeMethod("substring(int,int)", "[3,6]");
		assertEquals("bar", r);

		r = new ObjectIntrospector(in).allow(String.class, "toString").invokeMethod("toString", null);
		assertEquals("foobar", r);

		r = new ObjectIntrospector(in).allow(String.class, "toString").invokeMethod("toString", "");
		assertEquals("foobar", r);

		r = new ObjectIntrospector(in).allow(String.class, "toString").invokeMethod("toString", "[]");
		assertEquals("foobar", r);

		// Unknown method names fail resolution before the allow-list is ever consulted.
		assertThrows(NoSuchMethodException.class, ()->new ObjectIntrospector("foobar").allowAll().invokeMethod("noSuchMethod", "[3,6]"));

		r = new ObjectIntrospector(null).invokeMethod(String.class.getMethod("toString"), null);
		assertNull(r);

		r = new ObjectIntrospector("foobar").allow(String.class, "toString").invokeMethod(String.class.getMethod("toString"), null);
		assertEquals("foobar", r);
	}

	//====================================================================================================
	// Secure-by-default: no allow-list configured -> denied.
	//====================================================================================================
	@Test void a02_defaultDeny_stringSignature() {
		var oi = new ObjectIntrospector("foobar");
		assertThrows(MethodNotAllowlistedException.class, ()->oi.invokeMethod("toString", null));
	}

	@Test void a03_defaultDeny_rawMethod() throws Exception {
		var oi = new ObjectIntrospector("foobar");
		var m = String.class.getMethod("toString");
		assertThrows(MethodNotAllowlistedException.class, ()->oi.invokeMethod(m, null));
	}

	//====================================================================================================
	// allow(Class, String...) - explicit allow-list.
	//====================================================================================================
	@Test void a04_allow_explicitSignatureDispatches() throws Exception {
		var r = new ObjectIntrospector("foobar").allow(String.class, "substring(int,int)").invokeMethod("substring(int,int)", "[3,6]");
		assertEquals("bar", r);
	}

	@Test void a05_allow_doesNotImplicitlyAllowOtherMethods() {
		var oi = new ObjectIntrospector("foobar").allow(String.class, "substring(int,int)");
		assertThrows(MethodNotAllowlistedException.class, ()->oi.invokeMethod("toString", null));
	}

	@Test void a06_allow_multipleCallsAreOred() throws Exception {
		var oi = new ObjectIntrospector("foobar").allow(String.class, "toString").allow(String.class, "substring(int,int)");
		assertEquals("foobar", oi.invokeMethod("toString", null));
		assertEquals("bar", oi.invokeMethod("substring(int,int)", "[3,6]"));
	}

	@Test void a07_allow_predicateForm() throws Exception {
		var oi = new ObjectIntrospector("foobar").allow(m -> m.getName().equals("toString"));
		assertEquals("foobar", oi.invokeMethod("toString", null));
		assertThrows(MethodNotAllowlistedException.class, ()->oi.invokeMethod("substring(int,int)", "[3,6]"));
	}

	//====================================================================================================
	// allowAll() - opt-in escape hatch restores pre-10.0 allow-any behavior.
	//====================================================================================================
	@Test void a08_allowAll_restoresDispatch() throws Exception {
		var oi = new ObjectIntrospector("foobar").allowAll();
		assertEquals("bar", oi.invokeMethod("substring(int,int)", "[3,6]"));
		assertEquals("foobar", oi.invokeMethod("toString", null));
	}

	//====================================================================================================
	// MethodNotAllowlistedException - actionable message.
	//====================================================================================================
	@Test void a09_deniedExceptionMessageIsActionable() {
		var oi = new ObjectIntrospector("foobar");
		var e = assertThrows(MethodNotAllowlistedException.class, ()->oi.invokeMethod("toString", null));
		assertTrue(e.getMessage().contains("toString"));
		assertTrue(e.getMessage().contains("allow"));
	}
}
