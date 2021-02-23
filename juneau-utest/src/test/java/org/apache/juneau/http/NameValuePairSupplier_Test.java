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
import org.apache.juneau.collections.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class NameValuePairSupplier_Test {

	@Test
	public void a01_basic() {
		NameValuePairSupplier x = NameValuePairSupplier.of();

		assertObject(x.iterator()).asJson().is("[]");
		x.add(pair("Foo","bar"));
		assertObject(x.iterator()).asJson().is("['Foo=bar']");
		x.add(pair("Foo","baz"));
		assertObject(x.iterator()).asJson().is("['Foo=bar','Foo=baz']");
		x.add(NameValuePairSupplier.of());
		assertObject(x.iterator()).asJson().is("['Foo=bar','Foo=baz']");
		x.add(NameValuePairSupplier.of(pair("Foo","qux")));
		assertObject(x.iterator()).asJson().is("['Foo=bar','Foo=baz','Foo=qux']");
		x.add(NameValuePairSupplier.of(pair("Foo","q2x"), pair("Foo","q3x")));
		assertObject(x.iterator()).asJson().is("['Foo=bar','Foo=baz','Foo=qux','Foo=q2x','Foo=q3x']");
		x.add(NameValuePairSupplier.of(NameValuePairSupplier.of(pair("Foo","q4x"),pair("Foo","q5x"))));
		assertObject(x.iterator()).asJson().is("['Foo=bar','Foo=baz','Foo=qux','Foo=q2x','Foo=q3x','Foo=q4x','Foo=q5x']");
		x.add((Header)null);
		assertObject(x.iterator()).asJson().is("['Foo=bar','Foo=baz','Foo=qux','Foo=q2x','Foo=q3x','Foo=q4x','Foo=q5x']");
		x.add((NameValuePairSupplier)null);
		assertObject(x.iterator()).asJson().is("['Foo=bar','Foo=baz','Foo=qux','Foo=q2x','Foo=q3x','Foo=q4x','Foo=q5x']");

		assertObject(new NameValuePairSupplier.Null().iterator()).asJson().is("[]");
	}

	@Test
	public void a02_creators() {
		NameValuePairSupplier x;

		x = NameValuePairSupplier.of(pair("Foo","bar"), pair("Foo","baz"), null);
		assertObject(x.iterator()).asJson().is("['Foo=bar','Foo=baz']");

		x = NameValuePairSupplier.of(AList.of(pair("Foo","bar"), pair("Foo","baz"), null));
		assertObject(x.iterator()).asJson().is("['Foo=bar','Foo=baz']");

		x = NameValuePairSupplier.ofPairs("Foo","bar","Foo","baz");
		assertObject(x.iterator()).asJson().is("['Foo=bar','Foo=baz']");

		assertThrown(()->NameValuePairSupplier.ofPairs("Foo")).is("Odd number of parameters passed into NameValuePairSupplier.ofPairs()");

		assertThrown(()->NameValuePairSupplier.of("Foo")).is("Invalid type passed to NameValuePairSupplier.of(): java.lang.String");
	}

	@Test
	public void a03_addMethods() {
		String pname = "NameValuePairSupplierTest.x";

		NameValuePairSupplier x = NameValuePairSupplier.create().resolving();
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
		NameValuePairSupplier x = NameValuePairSupplier
			.create()
			.add("X1","1")
			.add(NameValuePairSupplier.ofPairs("X2","2"));
		assertObject(x.toArray()).asJson().is("['X1=1','X2=2']");
		assertObject(x.toArray(new NameValuePair[0])).asJson().is("['X1=1','X2=2']");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	private static NameValuePair pair(String name, Object val) {
		return BasicNameValuePair.of(name, val);
	}

	private static HttpPartSerializerSession openApiSession() {
		return OpenApiSerializer.DEFAULT.createPartSession(null);
	}
}
