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

	@Test
	public void a01_basic() throws Exception {
		RuntimeException x1 = new RuntimeException("foo");

		assertThrowable(x1).isType(Exception.class).isType(RuntimeException.class);
		assertThrown(()->assertThrowable(x1).isType(IOException.class)).is("Exception was not expected type.\n\tExpect=[java.io.IOException]\n\tActual=[java.lang.RuntimeException]");
		assertThrown(()->assertThrowable(null).isType(IOException.class)).is("Exception was not expected type.\n\tExpect=[java.io.IOException]\n\tActual=[null]");
		assertThrown(()->assertThrowable(x1).isType(null)).is("Parameter 'type' cannot be null.");

		assertThrowable(x1).contains("foo");
		assertThrown(()->assertThrowable(x1).contains("bar")).is("Exception message did not contain expected substring.\n\tSubstring=[bar]\n\tText=[foo]");
		assertThrown(()->assertThrowable(null).contains("foo")).is("Exception was not thrown.");
		assertThrown(()->assertThrowable(x1).contains((String[])null)).is("Parameter 'substrings' cannot be null.");
		assertThrowable(x1).contains((String)null);

		assertThrowable(null).doesNotExist();
		assertThrown(()->assertThrowable(x1).doesNotExist()).is("Exception was thrown.");

		assertThrowable(x1).passes(x->x.getMessage().equals("foo"));
		assertThrown(()->assertThrowable(x1).passes(x->x.getMessage().equals("bar"))).is("Value did not pass predicate test.\n\tValue=[java.lang.RuntimeException: foo]");

		assertThrowable(x1).passes(x->x.getMessage().equals("foo"));
		assertThrown(()->assertThrowable(x1).passes(x->x.getMessage().equals("bar"))).is("Value did not pass predicate test.\n\tValue=[java.lang.RuntimeException: foo]");

		assertThrowable(x1).passes(x->x.getMessage().equals("foo"));
		assertThrown(()->assertThrowable(x1).passes(x->x.getMessage().equals("bar"))).is("Value did not pass predicate test.\n\tValue=[java.lang.RuntimeException: foo]");

		assertThrowable(x1).passes(x->x.getMessage().equals("foo"));
		assertThrown(()->assertThrowable(x1).passes(x->x.getMessage().equals("bar"))).is("Value did not pass predicate test.\n\tValue=[java.lang.RuntimeException: foo]");

		assertThrowable(x1).message().is("foo");
		assertThrowable(new RuntimeException()).message().doesNotExist();
		assertThrowable(null).message().doesNotExist();

		assertThrowable(x1).localizedMessage().is("foo");
		assertThrowable(new RuntimeException()).localizedMessage().doesNotExist();
		assertThrowable(null).localizedMessage().doesNotExist();

		assertThrowable(x1).stackTrace().contains("RuntimeException");
		assertThrowable(new RuntimeException()).stackTrace().contains("RuntimeException");
		assertThrowable(null).stackTrace().doesNotExist();

		assertThrowable(new RuntimeException(x1)).causedBy().message().is("foo");
		assertThrowable(new RuntimeException()).message().doesNotExist();
		assertThrowable(null).causedBy().message().doesNotExist();

		assertThrowable(new RuntimeException(new IOException())).find(RuntimeException.class).exists();
		assertThrowable(new RuntimeException(new IOException())).find(IOException.class).exists();
		assertThrowable(new RuntimeException(new IOException())).find(Exception.class).exists();
		assertThrowable(new RuntimeException(new IOException())).find(FileNotFoundException.class).doesNotExist();
		assertThrowable(new RuntimeException()).find(RuntimeException.class).exists();
		assertThrowable(new RuntimeException()).find(IOException.class).doesNotExist();
		assertThrowable(null).find(RuntimeException.class).doesNotExist();
	}

	@Test
	public void a02_other() throws Exception {
		assertThrown(()->ThrowableAssertion.create(null).msg("Foo {0}", 1).exists()).is("Foo 1");
		ThrowableAssertion.create(null).stdout().stderr();
	}
}
