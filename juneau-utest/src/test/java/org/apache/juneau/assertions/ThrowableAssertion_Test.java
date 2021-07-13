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

import java.io.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ThrowableAssertion_Test {

	private <V extends Throwable> ThrowableAssertion<V> test(V value) {
		return assertThrowable(value).silent();
	}

	@Test
	public void a01_basic() throws Exception {
		RuntimeException x1 = new RuntimeException("foo");

		test(x1).isType(Exception.class).isType(RuntimeException.class);
		assertThrown(()->test(x1).isType(IOException.class)).message().is("Exception was not expected type.\n\tExpect=\"java.io.IOException\".\n\tActual=\"java.lang.RuntimeException\".");
		assertThrown(()->test(null).isType(IOException.class)).message().is("Exception was not thrown.");
		assertThrown(()->test(x1).isType(null)).message().is("Argument \"type\" cannot be null.");

		test(x1).message().is("foo");

		test(null).doesNotExist();
		assertThrown(()->test(x1).doesNotExist()).message().is("Exception was thrown.");

		test(x1).passes(x->x.getMessage().equals("foo"));
		assertThrown(()->test(x1).passes(x->x.getMessage().equals("bar"))).message().is("Unexpected value: \"java.lang.RuntimeException: foo\".");

		test(x1).passes(x->x.getMessage().equals("foo"));
		assertThrown(()->test(x1).passes(x->x.getMessage().equals("bar"))).message().is("Unexpected value: \"java.lang.RuntimeException: foo\".");

		test(x1).passes(x->x.getMessage().equals("foo"));
		assertThrown(()->test(x1).passes(x->x.getMessage().equals("bar"))).message().is("Unexpected value: \"java.lang.RuntimeException: foo\".");

		test(x1).passes(x->x.getMessage().equals("foo"));
		assertThrown(()->test(x1).passes(x->x.getMessage().equals("bar"))).message().is("Unexpected value: \"java.lang.RuntimeException: foo\".");

		test(x1).message().is("foo");
		test(new RuntimeException()).message().doesNotExist();
		test(null).message().doesNotExist();

		test(x1).localizedMessage().is("foo");
		test(new RuntimeException()).localizedMessage().doesNotExist();
		test(null).localizedMessage().doesNotExist();

		test(x1).stackTrace().contains("RuntimeException");
		test(new RuntimeException()).stackTrace().contains("RuntimeException");
		test(null).stackTrace().doesNotExist();

		test(new RuntimeException(x1)).causedBy().message().is("foo");
		test(new RuntimeException()).message().doesNotExist();
		test(null).causedBy().message().doesNotExist();

		test(new RuntimeException(new IOException())).find(RuntimeException.class).exists();
		test(new RuntimeException(new IOException())).find(IOException.class).exists();
		test(new RuntimeException(new IOException())).find(Exception.class).exists();
		test(new RuntimeException(new IOException())).find(FileNotFoundException.class).doesNotExist();
		test(new RuntimeException()).find(RuntimeException.class).exists();
		test(new RuntimeException()).find(IOException.class).doesNotExist();
		test(null).find(RuntimeException.class).doesNotExist();
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->test(null).msg("Foo {0}", 1).exists()).message().is("Foo 1");
		assertThrown(()->test(null).msg("Foo {0}", 1).throwable(RuntimeException.class).exists()).isExactType(RuntimeException.class).message().is("Foo 1");
		test(null).stdout();
	}
}
