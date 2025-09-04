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
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.junit.jupiter.api.*;

class OpSwaggerAnnotation_Test extends SimpleTestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	OpSwagger a1 = OpSwaggerAnnotation.create()
		.consumes("consumes")
		.deprecated("deprecated")
		.description("description")
		.externalDocs(ExternalDocsAnnotation.DEFAULT)
		.operationId("operationId")
		.parameters("parameters")
		.produces("produces")
		.responses("responses")
		.schemes("schemes")
		.summary("summary")
		.tags("tags")
		.value("value")
		.build();

	OpSwagger a2 = OpSwaggerAnnotation.create()
		.consumes("consumes")
		.deprecated("deprecated")
		.description("description")
		.externalDocs(ExternalDocsAnnotation.DEFAULT)
		.operationId("operationId")
		.parameters("parameters")
		.produces("produces")
		.responses("responses")
		.schemes("schemes")
		.summary("summary")
		.tags("tags")
		.value("value")
		.build();

	@Test void a01_basic() {
		assertJson(""
			+ "{"
				+ "consumes:['consumes'],"
				+ "deprecated:'deprecated',"
				+ "description:['description'],"
				+ "externalDocs:{description:[],url:''},"
				+ "operationId:'operationId',"
				+ "parameters:['parameters'],"
				+ "produces:['produces'],"
				+ "responses:['responses'],"
				+ "schemes:['schemes'],"
				+ "summary:['summary'],"
				+ "tags:['tags'],"
				+ "value:['value']"
			+ "}", a1
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

	@OpSwagger(
		consumes="consumes",
		deprecated="deprecated",
		description="description",
		externalDocs=@ExternalDocs,
		operationId="operationId",
		parameters="parameters",
		produces="produces",
		responses="responses",
		schemes="schemes",
		summary="summary",
		tags="tags",
		value="value"
	)
	public static class D1 {}
	OpSwagger d1 = D1.class.getAnnotationsByType(OpSwagger.class)[0];

	@OpSwagger(
		consumes="consumes",
		deprecated="deprecated",
		description="description",
		externalDocs=@ExternalDocs,
		operationId="operationId",
		parameters="parameters",
		produces="produces",
		responses="responses",
		schemes="schemes",
		summary="summary",
		tags="tags",
		value="value"
	)
	public static class D2 {}
	OpSwagger d2 = D2.class.getAnnotationsByType(OpSwagger.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}
}