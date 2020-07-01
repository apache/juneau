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
import org.apache.juneau.http.header.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class HeaderSupplierTest {

	@Test
	public void a01_basic() {
		HeaderSupplier h = HeaderSupplier.create();
		assertObject(h.iterator()).json().is("[]");
		h.add(header("Foo","bar"));
		assertObject(h.iterator()).json().is("['Foo: bar']");
		h.add(header("Foo","baz"));
		assertObject(h.iterator()).json().is("['Foo: bar','Foo: baz']");
		h.add(HeaderSupplier.create());
		assertObject(h.iterator()).json().is("['Foo: bar','Foo: baz']");
		h.add(HeaderSupplier.create().add(header("Foo","qux")));
		assertObject(h.iterator()).json().is("['Foo: bar','Foo: baz','Foo: qux']");
		h.add(HeaderSupplier.create().add(header("Foo","quux")).add(header("Foo","quuux")));
		assertObject(h.iterator()).json().is("['Foo: bar','Foo: baz','Foo: qux','Foo: quux','Foo: quuux']");
		h.add(HeaderSupplier.create().add(HeaderSupplier.create().add(header("Foo","ruux")).add(header("Foo","ruuux"))));
		assertObject(h.iterator()).json().is("['Foo: bar','Foo: baz','Foo: qux','Foo: quux','Foo: quuux','Foo: ruux','Foo: ruuux']");
		h.add((Header)null);
		assertObject(h.iterator()).json().is("['Foo: bar','Foo: baz','Foo: qux','Foo: quux','Foo: quuux','Foo: ruux','Foo: ruuux']");
		h.add((HeaderSupplier)null);
		assertObject(h.iterator()).json().is("['Foo: bar','Foo: baz','Foo: qux','Foo: quux','Foo: quuux','Foo: ruux','Foo: ruuux']");
	}

	private static Header header(String name, Object val) {
		return BasicHeader.of(name, val);
	}

}
