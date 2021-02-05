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
package org.apache.juneau.assertions;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import static java.util.Optional.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BooleanAssertion_Test {

	@Test
	public void a01_basic() throws Exception {

		assertThrown(()->assertBoolean((Boolean)null).exists()).is("Value was null.");
		assertBoolean(true).exists();
		assertThrown(()->assertBoolean(empty()).exists()).is("Value was null.");
		assertBoolean(true).exists();

		assertBoolean(empty()).doesNotExist();
		assertThrown(()->assertBoolean(true).doesNotExist()).is("Value was not null.");

		assertBoolean(empty()).isEqual(null);
		assertBoolean(true).isEqual(true);
		assertBoolean(of(true)).isEqual(true);

		assertBoolean(true).isTrue();
		assertThrown(()->assertBoolean(true).isFalse()).is("Value was true.");
		assertBoolean(false).isFalse();
		assertThrown(()->assertBoolean(false).isTrue()).is("Value was false.");

		assertThrown(()->assertBoolean(true).isEqual(false)).contains("Unexpected value.");
		assertBoolean(empty()).isEqual(null);

		assertBoolean(true).isNot("true");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->BooleanAssertion.create(null).msg("Foo {0}", 1).exists()).is("Foo 1");
		BooleanAssertion.create(null).stdout().stderr();
	}
}
