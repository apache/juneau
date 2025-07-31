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
package org.apache.juneau.jsonschema.annotation;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ExternalDocsAnnotation_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	ExternalDocs a1 = ExternalDocsAnnotation.create()
		.description("description")
		.url("url")
		.build();

	ExternalDocs a2 = ExternalDocsAnnotation.create()
		.description("description")
		.url("url")
		.build();

	@Test
	public void a01_basic() {
		assertObject(a1).asString().is(""
			+ "{"
				+ "description:['description'],"
				+ "url:'url'"
			+ "}"
		);
	}

	@Test
	public void a02_testEquivalency() {
		assertObject(a1).is(a2);
		assertInteger(a1.hashCode()).is(a2.hashCode()).isNotAny(0,-1);
	}

	//------------------------------------------------------------------------------------------------------------------
	// PropertyStore equivalency.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_testEquivalencyInPropertyStores() {
		BeanContext bc1 = BeanContext.create().annotations(a1).build();
		BeanContext bc2 = BeanContext.create().annotations(a2).build();
		assertSame(bc1, bc2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other methods.
	//------------------------------------------------------------------------------------------------------------------

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@ExternalDocs(
		description="description",
		url="url"
	)
	public static class D1 {}
	ExternalDocs d1 = D1.class.getAnnotationsByType(ExternalDocs.class)[0];

	@ExternalDocs(
		description="description",
		url="url"
	)
	public static class D2 {}
	ExternalDocs d2 = D2.class.getAnnotationsByType(ExternalDocs.class)[0];

	@Test
	public void d01_comparisonWithDeclarativeAnnotations() {
		assertObject(d1).is(d2).is(a1);
		assertInteger(d1.hashCode()).is(d2.hashCode()).is(a1.hashCode()).isNotAny(0,-1);
	}
}