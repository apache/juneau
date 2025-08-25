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

import static org.apache.juneau.TestUtils.*;
import static org.junit.Assert.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Tag;
import org.junit.jupiter.api.*;

class SwaggerAnnotation_Test extends SimpleTestBase {

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

	@Test void a01_basic() {
		assertJson(a1, ""
			+ "{"
				+ "contact:{description:[],email:'',name:'',url:''},"
				+ "description:['description'],"
				+ "externalDocs:{description:[],url:''},"
				+ "license:{description:[],name:'',url:''},"
				+ "tags:[{description:[],externalDocs:{description:[],url:''},name:''}],"
				+ "termsOfService:['termsOfService'],"
				+ "title:['title'],"
				+ "value:['value'],"
				+ "version:'version'"
			+ "}"
		);
	}

	@Test void a02_testEquivalency() {
		assertEquals(a2, a1);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEquals(a1.hashCode(), a2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// PropertyStore equivalency.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_testEquivalencyInPropertyStores() {
		var bc1 = BeanContext.create().annotations(a1).build();
		var bc2 = BeanContext.create().annotations(a2).build();
		assertSame(bc1, bc2);
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

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}
}