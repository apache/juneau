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

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.internal.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class KeywordStoreTest {

	//====================================================================================================
	// test - Basic tests
	//====================================================================================================
	@Test
	public void test() {
		KeywordSet ks = new KeywordSet("aaa", "zzz");
		assertTrue(ks.contains("aaa"));
		assertTrue(ks.contains("zzz"));
		assertFalse(ks.contains("xxx"));
		assertFalse(ks.contains("aaaa"));
		assertFalse(ks.contains("zzzz"));
		assertFalse(ks.contains("\u0000\u1000"));
		assertFalse(ks.contains("z"));
		assertFalse(ks.contains(null));
		assertFalse(ks.contains("a|"));
		assertFalse(ks.contains("|a"));
		assertFalse(ks.contains("Aa"));
		assertFalse(ks.contains("aA"));
	}
}