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
package org.apache.juneau.internal;

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ObjectUtilsTest {

	@Test
	public void a01_eq() {
		assertTrue(ObjectUtils.eq("foo","foo"));
		assertTrue(ObjectUtils.eq(null,null));
		assertFalse(ObjectUtils.eq(null,"foo"));
		assertFalse(ObjectUtils.eq("foo",null));
	}

	@Test
	public void a02_ne() {
		assertFalse(ObjectUtils.ne("foo","foo"));
		assertFalse(ObjectUtils.ne(null,null));
		assertTrue(ObjectUtils.ne(null,"foo"));
		assertTrue(ObjectUtils.ne("foo",null));
	}
}
