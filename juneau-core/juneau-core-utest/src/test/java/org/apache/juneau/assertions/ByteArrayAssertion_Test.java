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

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ByteArrayAssertion_Test {

	@Test
	public void a01_basic() throws Exception {
		byte[] x1={}, x2={'a','b'};

		assertThrown(()->assertBytes(null).exists()).is("Value was null.");
		assertBytes(x1).exists();

		assertBytes(null).doesNotExist();
		assertThrown(()->assertBytes(x1).doesNotExist()).is("Value was not null.");

		assertThrown(()->assertBytes(null).isSize(0)).is("Value was null.");
		assertBytes(x1).isSize(0);
		assertThrown(()->assertBytes(x1).isSize(1)).is("Array did not have the expected size.  Expect=1, Actual=0.");
		assertBytes(x2).isSize(2);
		assertThrown(()->assertBytes(x2).isSize(0)).is("Array did not have the expected size.  Expect=0, Actual=2.");

		assertThrown(()->assertBytes(null).isEmpty()).is("Value was null.");
		assertBytes(x1).isEmpty();
		assertThrown(()->assertBytes(x2).isEmpty()).is("Array was not empty.");

		assertThrown(()->assertBytes(null).isNotEmpty()).is("Value was null.");
		assertThrown(()->assertBytes(x1).isNotEmpty()).is("Array was empty.");
		assertBytes(x2).isNotEmpty();

		assertBytes(null).item(0).doesNotExist();
		assertBytes(x1).item(0).doesNotExist();
		assertBytes(x2).item(0).exists();

		assertBytes(null).string().isNull();
		assertBytes(x1).string().is("");
		assertBytes(x2).string().is("ab");
		assertThrown(()->assertBytes(x2).string().is("xx")).is("Unexpected value.\n\tExpect=[xx]\n\tActual=[ab]");

		assertBytes(null).base64().isNull();
		assertBytes(x1).base64().is("");
		assertBytes(x2).base64().is("YWI=");
		assertThrown(()->assertBytes(x2).base64().is("xx")).is("Unexpected value.\n\tExpect=[xx]\n\tActual=[YWI=]");

		assertBytes(null).hex().isNull();
		assertBytes(x1).hex().is("");
		assertBytes(x2).hex().is("6162");
		assertThrown(()->assertBytes(x2).hex().is("xx")).is("Unexpected value.\n\tExpect=[xx]\n\tActual=[6162]");

		assertBytes(null).spacedHex().isNull();
		assertBytes(x1).spacedHex().is("");
		assertBytes(x2).spacedHex().is("61 62");
		assertThrown(()->assertBytes(x2).spacedHex().is("xx")).is("Unexpected value.\n\tExpect=[xx]\n\tActual=[61 62]");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->ByteArrayAssertion.create(null).msg("Foo {0}", 1).exists()).is("Foo 1");
		ByteArrayAssertion.create(null).stdout().stderr();
	}
}
