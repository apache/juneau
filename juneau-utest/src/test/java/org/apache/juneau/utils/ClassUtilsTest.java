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
package org.apache.juneau.utils;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.internal.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ClassUtilsTest {

	//====================================================================================================
	// Fuzzy constructor args
	//====================================================================================================
	@Test
	public void castOrCreateWithFuzzyArgs() throws Exception {
		FA t = null;

		t = ClassUtils.castOrCreate(FA.class, FA.class, true);
		assertEquals(1, t.c);

		t = ClassUtils.castOrCreate(FA.class, FA.class, true, "foo");
		assertEquals(2, t.c);

		t = ClassUtils.castOrCreate(FA.class, FA.class, true, 123, "foo");
		assertEquals(3, t.c);

		t = ClassUtils.castOrCreate(FA.class, FA.class, true, "foo", 123);
		assertEquals(3, t.c);

		FB t2 = null;

		assertThrown(()->ClassUtils.castOrCreate(FB.class, FB.class, true)).is("Could not instantiate class org.apache.juneau.utils.ClassUtilsTest$FB");

		t2 = ClassUtils.castOrCreate(FB.class, FB.class, true, "foo");
		assertEquals(1, t2.c);

		t2 = ClassUtils.castOrCreate(FB.class, FB.class, true, 123, "foo");
		assertEquals(1, t2.c);

		t2 = ClassUtils.castOrCreate(FB.class, FB.class, true, "foo", 123);
		assertEquals(1, t2.c);
	}

	public static class FA {
		int c;
		public FA() {
			c = 1;
		}
		public FA(String foo) {
			c = 2;
		}
		public FA(int foo, String bar) {
			c = 3;
		}
	}

	public static class FB {
		int c;
		public FB(String foo) {
			c = 1;
		}
	}
}
