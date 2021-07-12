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

import java.util.*;

import static java.util.Optional.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ByteArrayAssertion_Test {

	private ByteArrayAssertion test(byte[] value) {
		return assertBytes(value).silent();
	}

	private ByteArrayAssertion test(Optional<byte[]> value) {
		return assertBytes(value).silent();
	}

	@Test
	public void a01_basic() throws Exception {
		byte[] x1={}, x2={'a','b'};

		assertThrown(()->test((byte[])null).exists()).is("Value was null.");
		test(x1).exists();
		assertThrown(()->test(empty()).exists()).is("Value was null.");
		test(x1).exists();

		test((byte[])null).doesNotExist();
		assertThrown(()->test(x1).doesNotExist()).is("Value was not null.");
		test(empty()).doesNotExist();
		assertThrown(()->test(x1).doesNotExist()).is("Value was not null.");

		assertThrown(()->test(empty()).isSize(0)).is("Value was null.");
		test(x1).isSize(0);
		assertThrown(()->test(x1).isSize(1)).is("Array did not have the expected size.\n\tExpect=1.\n\tActual=0.");
		test(x2).isSize(2);
		assertThrown(()->test(of(x1)).isSize(1)).is("Array did not have the expected size.\n\tExpect=1.\n\tActual=0.");
		test(x2).isSize(2);
		assertThrown(()->test(x2).isSize(0)).is("Array did not have the expected size.\n\tExpect=0.\n\tActual=2.");

		assertThrown(()->test(empty()).isEmpty()).is("Value was null.");
		test(x1).isEmpty();
		assertThrown(()->test(x2).isEmpty()).is("Array was not empty.");

		assertThrown(()->test(empty()).isNotEmpty()).is("Value was null.");
		assertThrown(()->test(x1).isNotEmpty()).is("Array was empty.");
		test(x2).isNotEmpty();

		test(empty()).item(0).doesNotExist();
		test(x1).item(0).doesNotExist();
		test(x2).item(0).exists();

		test(empty()).asString().isNull();
		test(x1).asString().is("");
		test(x2).asString().is("ab");
		assertThrown(()->test(x2).asString().is("xx")).is("Unexpected value.\n\tExpect=\"xx\".\n\tActual=\"ab\".");

		test(empty()).asBase64().isNull();
		test(x1).asBase64().is("");
		test(x2).asBase64().is("YWI=");
		assertThrown(()->test(x2).asBase64().is("xx")).is("Unexpected value.\n\tExpect=\"xx\".\n\tActual=\"YWI=\".");

		test(empty()).asHex().isNull();
		test(x1).asHex().is("");
		test(x2).asHex().is("6162");
		assertThrown(()->test(x2).asHex().is("xx")).is("Unexpected value.\n\tExpect=\"xx\".\n\tActual=\"6162\".");

		test(empty()).asSpacedHex().isNull();
		test(x1).asSpacedHex().is("");
		test(x2).asSpacedHex().is("61 62");
		assertThrown(()->test(x2).asSpacedHex().is("xx")).is("Unexpected value.\n\tExpect=\"xx\".\n\tActual=\"61 62\".");
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->test((byte[])null).msg("Foo {0}", 1).exists()).is("Foo 1");
		assertThrown(()->test((byte[])null).msg("Foo {0}", 1).throwable(RuntimeException.class).exists()).isExactType(RuntimeException.class).is("Foo 1");
		test((byte[])null).stdout();
	}
}
