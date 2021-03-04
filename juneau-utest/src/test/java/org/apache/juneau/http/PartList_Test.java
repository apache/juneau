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
import static org.apache.juneau.http.HttpParts.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class PartList_Test {

	@Test
	public void a01_basic() {
		PartListBuilder x = PartList.create();

		assertObject(x.build().iterator()).asJson().is("[]");
		x.add(part("Foo","bar"));
		assertObject(x.build().iterator()).asJson().is("['Foo=bar']");
		x.add(part("Foo","baz"));
		assertObject(x.build().iterator()).asJson().is("['Foo=bar','Foo=baz']");
		x.add(PartList.of());
		assertObject(x.build().iterator()).asJson().is("['Foo=bar','Foo=baz']");
		x.add(PartList.of(part("Foo","qux")));
		assertObject(x.build().iterator()).asJson().is("['Foo=bar','Foo=baz','Foo=qux']");
		x.add(PartList.of(part("Foo","q2x"), part("Foo","q3x")));
		assertObject(x.build().iterator()).asJson().is("['Foo=bar','Foo=baz','Foo=qux','Foo=q2x','Foo=q3x']");
		x.add(PartList.of(part("Foo","q4x"),part("Foo","q5x")));
		assertObject(x.build().iterator()).asJson().is("['Foo=bar','Foo=baz','Foo=qux','Foo=q2x','Foo=q3x','Foo=q4x','Foo=q5x']");
		x.add((PartList)null);
		assertObject(x.build().iterator()).asJson().is("['Foo=bar','Foo=baz','Foo=qux','Foo=q2x','Foo=q3x','Foo=q4x','Foo=q5x']");

		assertObject(new PartList.Null().iterator()).asJson().is("[]");
	}

	@Test
	public void a02_creators() {
		PartList x;

		x = PartList.of(part("Foo","bar"), part("Foo","baz"), null);
		assertObject(x.iterator()).asJson().is("['Foo=bar','Foo=baz']");

		x = PartList.of(AList.of(part("Foo","bar"), part("Foo","baz"), null));
		assertObject(x.iterator()).asJson().is("['Foo=bar','Foo=baz']");

		x = PartList.ofPairs("Foo","bar","Foo","baz");
		assertObject(x.iterator()).asJson().is("['Foo=bar','Foo=baz']");

		assertThrown(()->PartList.ofPairs("Foo")).is("Odd number of parameters passed into PartList.ofPairs()");
	}

	@Test
	public void a03_addMethods() {
		String pname = "NameValuePairSupplierTest.x";

		PartListBuilder x = PartList.create().resolving();
		System.setProperty(pname, "y");

		x.add("X1","bar");
		x.add("X2","$S{"+pname+"}");
		x.add("X3","bar");
		x.add("X4",()->"$S{"+pname+"}");
		x.add("X5","bar",HttpPartType.QUERY,openApiSession(),null,false);
		x.add("X6","$S{"+pname+"}",HttpPartType.QUERY,openApiSession(),null,false);

		assertString(x.build().toString()).is("X1=bar&X2=y&X3=bar&X4=y&X5=bar&X6=y");

		System.setProperty(pname, "z");

		assertString(x.build().toString()).is("X1=bar&X2=z&X3=bar&X4=z&X5=bar&X6=z");

		System.clearProperty(pname);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	private static Part part(String name, Object val) {
		return basicPart(name, val);
	}

	private static HttpPartSerializerSession openApiSession() {
		return OpenApiSerializer.DEFAULT.createPartSession(null);
	}
}
