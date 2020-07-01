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
package org.apache.juneau.http;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.http.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class NameValuePairSupplierTest {

	@Test
	public void a01_basic() {
		NameValuePairSupplier h = NameValuePairSupplier.of();
		assertObject(h.iterator()).json().is("[]");
		h.add(header("Foo","bar"));
		assertObject(h.iterator()).json().is("[{name:'Foo',value:'bar'}]");
		h.add(header("Foo","baz"));
		assertObject(h.iterator()).json().is("[{name:'Foo',value:'bar'},{name:'Foo',value:'baz'}]");
		h.add(NameValuePairSupplier.of());
		assertObject(h.iterator()).json().is("[{name:'Foo',value:'bar'},{name:'Foo',value:'baz'}]");
		h.add(NameValuePairSupplier.of(header("Foo","qux")));
		assertObject(h.iterator()).json().is("[{name:'Foo',value:'bar'},{name:'Foo',value:'baz'},{name:'Foo',value:'qux'}]");
		h.add(NameValuePairSupplier.of(header("Foo","q2x"), header("Foo","q3x")));
		assertObject(h.iterator()).json().is("[{name:'Foo',value:'bar'},{name:'Foo',value:'baz'},{name:'Foo',value:'qux'},{name:'Foo',value:'q2x'},{name:'Foo',value:'q3x'}]");
		h.add(NameValuePairSupplier.of(NameValuePairSupplier.of(header("Foo","q4x"),header("Foo","q5x"))));
		assertObject(h.iterator()).json().is("[{name:'Foo',value:'bar'},{name:'Foo',value:'baz'},{name:'Foo',value:'qux'},{name:'Foo',value:'q2x'},{name:'Foo',value:'q3x'},{name:'Foo',value:'q4x'},{name:'Foo',value:'q5x'}]");
		h.add((NameValuePair)null);
		assertObject(h.iterator()).json().is("[{name:'Foo',value:'bar'},{name:'Foo',value:'baz'},{name:'Foo',value:'qux'},{name:'Foo',value:'q2x'},{name:'Foo',value:'q3x'},{name:'Foo',value:'q4x'},{name:'Foo',value:'q5x'}]");
		h.add((NameValuePairSupplier)null);
		assertObject(h.iterator()).json().is("[{name:'Foo',value:'bar'},{name:'Foo',value:'baz'},{name:'Foo',value:'qux'},{name:'Foo',value:'q2x'},{name:'Foo',value:'q3x'},{name:'Foo',value:'q4x'},{name:'Foo',value:'q5x'}]");
	}

	private static NameValuePair header(String name, Object val) {
		return BasicNameValuePair.of(name, val);
	}
}
