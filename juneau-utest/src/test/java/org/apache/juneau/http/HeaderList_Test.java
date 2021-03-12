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
import static org.apache.juneau.http.HttpHeaders.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.http.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class HeaderList_Test {

	@Test
	public void a01_basic() {
		HeaderListBuilder x = HeaderList.create();

		assertObject(x.build().iterator()).asJson().is("[]");
		x.append(header("Foo","bar"));
		assertObject(x.build().iterator()).asJson().is("['Foo: bar']");
		x.append(header("Foo","baz"));
		assertObject(x.build().iterator()).asJson().is("['Foo: bar','Foo: baz']");
		x.append(HeaderList.of().getAll());
		assertObject(x.build().iterator()).asJson().is("['Foo: bar','Foo: baz']");
		x.append(HeaderList.of(header("Foo","qux")).getAll());
		assertObject(x.build().iterator()).asJson().is("['Foo: bar','Foo: baz','Foo: qux']");
		x.append(HeaderList.of(header("Foo","q2x"), header("Foo","q3x")).getAll());
		assertObject(x.build().iterator()).asJson().is("['Foo: bar','Foo: baz','Foo: qux','Foo: q2x','Foo: q3x']");
		x.append(HeaderList.of(header("Foo","q4x"), header("Foo","q5x")).getAll());
		assertObject(x.build().iterator()).asJson().is("['Foo: bar','Foo: baz','Foo: qux','Foo: q2x','Foo: q3x','Foo: q4x','Foo: q5x']");
		x.append((Header)null);
		assertObject(x.build().iterator()).asJson().is("['Foo: bar','Foo: baz','Foo: qux','Foo: q2x','Foo: q3x','Foo: q4x','Foo: q5x']");
		x.append((List<Header>)null);
		assertObject(x.build().iterator()).asJson().is("['Foo: bar','Foo: baz','Foo: qux','Foo: q2x','Foo: q3x','Foo: q4x','Foo: q5x']");

		assertObject(new HeaderList.Null().iterator()).asJson().is("[]");
	}

	@Test
	public void a02_creators() {
		HeaderList x;

		x = headerList(header("Foo","bar"), header("Foo","baz"), null);
		assertObject(x.iterator()).asJson().is("['Foo: bar','Foo: baz']");

		x = headerList(AList.of(header("Foo","bar"), header("Foo","baz"), null));
		assertObject(x.iterator()).asJson().is("['Foo: bar','Foo: baz']");

		x = headerList("Foo","bar","Foo","baz");
		assertObject(x.iterator()).asJson().is("['Foo: bar','Foo: baz']");

		assertThrown(()->headerList("Foo")).is("Odd number of parameters passed into HeaderList.ofPairs()");
	}

	@Test
	public void a03_addMethods() {
		String pname = "HeaderSupplierTest.x";

		HeaderListBuilder x = HeaderList.create().resolving();
		System.setProperty(pname, "y");

		x.append("X1","bar");
		x.append("X2","$S{"+pname+"}");
		x.append("X3","bar");
		x.append("X4",()->"$S{"+pname+"}");
		x.append(SerializedHeader.of("X5","bar",openApiSession(),null,false));

		assertString(x.build().toString()).is("[X1: bar, X2: y, X3: bar, X4: y, X5: bar]");

		System.setProperty(pname, "z");

		assertString(x.build().toString()).is("[X1: bar, X2: z, X3: bar, X4: z, X5: bar]");

		System.clearProperty(pname);
	}

	@Test
	public void a04_toArrayMethods() {
		HeaderListBuilder x = HeaderList
			.create()
			.append("X1","1")
			.append(headerList("X2","2").getAll());
		assertObject(x.build().getAll()).asJson().is("['X1: 1','X2: 2']");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	private static Header header(String name, Object val) {
		return basicHeader(name, val);
	}

	private static HttpPartSerializerSession openApiSession() {
		return OpenApiSerializer.DEFAULT.createPartSession(null);
	}
}
