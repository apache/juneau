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
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.internal.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ArrayBuilder_Test {

	@Test
	public void a01_basic() {
		ArrayBuilder<String> x = ArrayBuilder.create(String.class, 2);
		assertObject(x.toArray()).asJson().is("[]");
		x.add(null);
		assertObject(x.toArray()).asJson().is("[]");
		x.add("a");
		assertObject(x.toArray()).asJson().is("['a']");
		x.add("b");
		x.add(null);
		assertObject(x.toArray()).asJson().is("['a','b']");
		assertThrown(()->x.add("c")).isType(ArrayIndexOutOfBoundsException.class);
	}
}
