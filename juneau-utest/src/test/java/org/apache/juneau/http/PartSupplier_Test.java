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

import org.apache.juneau.collections.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class PartSupplier_Test {

	@Test
	public void a01_basic() {
		PartSupplier x = PartSupplier.of();

		assertObject(x.iterator()).asJson().is("[]");
		x.add(pair("Foo","bar"));
		assertObject(x.iterator()).asJson().is("['Foo=bar']");
		x.add(pair("Foo","baz"));
		assertObject(x.iterator()).asJson().is("['Foo=bar','Foo=baz']");
		x.add(PartSupplier.of());
		assertObject(x.iterator()).asJson().is("['Foo=bar','Foo=baz']");
		x.add(PartSupplier.of(pair("Foo","qux")));
		assertObject(x.iterator()).asJson().is("['Foo=bar','Foo=baz','Foo=qux']");
		x.add(PartSupplier.of(pair("Foo","q2x"), pair("Foo","q3x")));
		assertObject(x.iterator()).asJson().is("['Foo=bar','Foo=baz','Foo=qux','Foo=q2x','Foo=q3x']");
		x.add(PartSupplier.of(PartSupplier.of(pair("Foo","q4x"),pair("Foo","q5x"))));
		assertObject(x.iterator()).asJson().is("['Foo=bar','Foo=baz','Foo=qux','Foo=q2x','Foo=q3x','Foo=q4x','Foo=q5x']");
		x.add((PartSupplier)null);
		assertObject(x.iterator()).asJson().is("['Foo=bar','Foo=baz','Foo=qux','Foo=q2x','Foo=q3x','Foo=q4x','Foo=q5x']");

		assertObject(new PartSupplier.Null().iterator()).asJson().is("[]");
	}

	@Test
	public void a02_creators() {
		PartSupplier x;

		x = PartSupplier.of(pair("Foo","bar"), pair("Foo","baz"), null);
		assertObject(x.iterator()).asJson().is("['Foo=bar','Foo=baz']");

		x = PartSupplier.of(AList.of(pair("Foo","bar"), pair("Foo","baz"), null));
		assertObject(x.iterator()).asJson().is("['Foo=bar','Foo=baz']");

		x = PartSupplier.ofPairs("Foo","bar","Foo","baz");
		assertObject(x.iterator()).asJson().is("['Foo=bar','Foo=baz']");

		assertThrown(()->PartSupplier.ofPairs("Foo")).is("Odd number of parameters passed into NameValuePairSupplier.ofPairs()");

		assertThrown(()->PartSupplier.of("Foo")).is("Invalid type passed to NameValuePairSupplier.of(): java.lang.String");
	}

	@Test
	public void a03_addMethods() {
		String pname = "NameValuePairSupplierTest.x";

		PartSupplier x = PartSupplier.create().resolving();
		System.setProperty(pname, "y");

		x.add("X1","bar");
		x.add("X2","$S{"+pname+"}");
		x.add("X3","bar");
		x.add("X4",()->"$S{"+pname+"}");
		x.add("X5","bar",HttpPartType.QUERY,openApiSession(),null,false);
		x.add("X6","$S{"+pname+"}",HttpPartType.QUERY,openApiSession(),null,false);

		assertString(x.toString()).is("X1=bar&X2=y&X3=bar&X4=y&X5=bar&X6=y");

		System.setProperty(pname, "z");

		assertString(x.toString()).is("X1=bar&X2=z&X3=bar&X4=z&X5=bar&X6=z");

		System.clearProperty(pname);
	}

	@Test
	public void a04_toArrayMethods() {
		PartSupplier x = PartSupplier
			.create()
			.add("X1","1")
			.add(PartSupplier.ofPairs("X2","2"));
		assertObject(x.toArray()).asJson().is("['X1=1','X2=2']");
		assertObject(x.toArray(new Part[0])).asJson().is("['X1=1','X2=2']");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	private static Part pair(String name, Object val) {
		return BasicPart.of(name, val);
	}

	private static HttpPartSerializerSession openApiSession() {
		return OpenApiSerializer.DEFAULT.createPartSession(null);
	}
}
