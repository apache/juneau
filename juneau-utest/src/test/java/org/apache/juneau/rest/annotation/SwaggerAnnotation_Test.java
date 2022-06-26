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
package org.apache.juneau.rest.annotation;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class SwaggerAnnotation_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Swagger a1 = SwaggerAnnotation.create()
		.contact(ContactAnnotation.DEFAULT)
		.description("description")
		.externalDocs(ExternalDocsAnnotation.DEFAULT)
		.license(LicenseAnnotation.DEFAULT)
		.tags(TagAnnotation.DEFAULT)
		.termsOfService("termsOfService")
		.title("title")
		.value("value")
		.version("version")
		.build();

	Swagger a2 = SwaggerAnnotation.create()
		.contact(ContactAnnotation.DEFAULT)
		.description("description")
		.externalDocs(ExternalDocsAnnotation.DEFAULT)
		.license(LicenseAnnotation.DEFAULT)
		.tags(TagAnnotation.DEFAULT)
		.termsOfService("termsOfService")
		.title("title")
		.value("value")
		.version("version")
		.build();

	@Test
	public void a01_basic() {
		assertObject(a1).asJson().is(""
			+ "{"
				+ "contact:{email:'',name:'',url:''},"
				+ "description:['description'],"
				+ "externalDocs:{description:[],url:''},"
				+ "license:{name:'',url:''},"
				+ "tags:[{description:[],externalDocs:{description:[],url:''},name:''}],"
				+ "termsOfService:['termsOfService'],"
				+ "title:['title'],"
				+ "value:['value'],"
				+ "version:'version'"
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
		assertTrue(bc1 == bc2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other methods.
	//------------------------------------------------------------------------------------------------------------------

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Swagger(
		contact=@Contact,
		description="description",
		externalDocs=@ExternalDocs,
		license=@License,
		tags=@Tag,
		termsOfService="termsOfService",
		title="title",
		value="value",
		version="version"
	)
	public static class D1 {}
	Swagger d1 = D1.class.getAnnotationsByType(Swagger.class)[0];

	@Swagger(
		contact=@Contact,
		description="description",
		externalDocs=@ExternalDocs,
		license=@License,
		tags=@Tag,
		termsOfService="termsOfService",
		title="title",
		value="value",
		version="version"
	)
	public static class D2 {}
	Swagger d2 = D2.class.getAnnotationsByType(Swagger.class)[0];

	@Test
	public void d01_comparisonWithDeclarativeAnnotations() {
		assertObject(d1).is(d2).is(a1);
		assertInteger(d1.hashCode()).is(d2.hashCode()).is(a1.hashCode()).isNotAny(0,-1);
	}
}
