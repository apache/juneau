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

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.junit.jupiter.api.*;

class ArrayBuilder_Test extends SimpleTestBase {

	@Test void a01_basic() {
		String[] empty = {};
		ArrayBuilder<String> x = ArrayBuilder.of(String.class).filter(y -> y != null).size(2);
		assertArray(x.orElse(empty));
		x.add(null);
		assertArray(x.orElse(empty));
		x.add("a");
		assertArray(x.orElse(empty), "a");
		x.add("b");
		x.add(null);
		assertArray(x.orElse(empty), "a,b");
		x.add("c");
		assertArray(x.orElse(empty), "a,b,c");
	}
}