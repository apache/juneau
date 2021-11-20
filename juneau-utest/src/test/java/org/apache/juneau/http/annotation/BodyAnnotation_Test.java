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
package org.apache.juneau.http.annotation;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BodyAnnotation_Test {

	private static final String CNAME = BodyAnnotation_Test.class.getName();

	public static class X1 {}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Body a1 = BodyAnnotation.create()
		.api("api")
		.d("d")
		.description("description")
		.ex("ex")
		.example("example")
		.examples("examples")
		.exs("exs")
		.on("on")
		.onClass(X1.class)
		.r(true)
		.required(true)
		.schema(SchemaAnnotation.create().build())
		.value("value")
		.build();

	Body a2 = BodyAnnotation.create()
		.api("api")
		.d("d")
		.description("description")
		.ex("ex")
		.example("example")
		.examples("examples")
		.exs("exs")
		.on("on")
		.onClass(X1.class)
		.r(true)
		.required(true)
		.schema(SchemaAnnotation.create().build())
		.value("value")
		.build();

	@Test
	public void a01_basic() {
		assertObject(a1).asJson().is(""
			+ "{"
				+ "api:['api'],"
				+ "d:['d'],"
				+ "description:['description'],"
				+ "ex:['ex'],"
				+ "example:['example'],"
				+ "examples:['examples'],"
				+ "exs:['exs'],"
				+ "on:['on'],"
				+ "onClass:['"+CNAME+"$X1'],"
				+ "r:true,"
				+ "required:true,"
				+ "schema:{'$ref':'',_default:[],_enum:[],additionalProperties:[],allOf:[],cf:'',collectionFormat:'',d:[],description:[],df:[],discriminator:'',e:[],emax:false,emin:false,ex:[],example:[],examples:[],exclusiveMaximum:false,exclusiveMinimum:false,exs:[],externalDocs:{description:[],url:'',value:[]},f:'',format:'',ignore:false,items:{'$ref':'',_default:[],_enum:[],cf:'',collectionFormat:'',df:[],e:[],emax:false,emin:false,exclusiveMaximum:false,exclusiveMinimum:false,f:'',format:'',items:{'$ref':'',_default:[],_enum:[],cf:'',collectionFormat:'',df:[],e:[],emax:false,emin:false,exclusiveMaximum:false,exclusiveMinimum:false,f:'',format:'',items:[],max:'',maxItems:-1,maxLength:-1,maxi:-1,maximum:'',maxl:-1,min:'',minItems:-1,minLength:-1,mini:-1,minimum:'',minl:-1,mo:'',multipleOf:'',p:'',pattern:'',t:'',type:'',ui:false,uniqueItems:false,value:[]},max:'',maxItems:-1,maxLength:-1,maxi:-1,maximum:'',maxl:-1,min:'',minItems:-1,minLength:-1,mini:-1,minimum:'',minl:-1,mo:'',multipleOf:'',p:'',pattern:'',t:'',type:'',ui:false,uniqueItems:false,value:[]},max:'',maxItems:-1,maxLength:-1,maxProperties:-1,maxi:-1,maximum:'',maxl:-1,maxp:-1,min:'',minItems:-1,minLength:-1,minProperties:-1,mini:-1,minimum:'',minl:-1,minp:-1,mo:'',multipleOf:'',on:[],onClass:[],p:'',pattern:'',properties:[],r:false,readOnly:false,required:false,ro:false,t:'',title:'',type:'',ui:false,uniqueItems:false,value:[],xml:[]},"
				+ "value:['value']"
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

	public static class C1 {
		public int f1;
		public void m1() {}
	}
	public static class C2 {
		public int f2;
		public void m2() {}
	}

	@Test
	public void c01_otherMethods() throws Exception {
		Body c1 = BodyAnnotation.create(C1.class).on(C2.class).build();
		Body c2 = BodyAnnotation.create("a").on("b").build();
		Body c4 = BodyAnnotation.create().on(C1.class.getMethod("m1")).on(C2.class.getMethod("m2")).build();

		assertObject(c1).asJson().contains("on:['"+CNAME+"$C1','"+CNAME+"$C2']");
		assertObject(c2).asJson().contains("on:['a','b']");
		assertObject(c4).asJson().contains("on:['"+CNAME+"$C1.m1()','"+CNAME+"$C2.m2()']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Body(
		api="api",
		d="d",
		description="description",
		ex="ex",
		example="example",
		examples="examples",
		exs="exs",
		on="on",
		onClass=X1.class,
		r=true,
		required=true,
		schema=@Schema,
		value="value"
	)
	public static class D1 {}
	Body d1 = D1.class.getAnnotationsByType(Body.class)[0];

	@Body(
		api="api",
		d="d",
		description="description",
		ex="ex",
		example="example",
		examples="examples",
		exs="exs",
		on="on",
		onClass=X1.class,
		r=true,
		required=true,
		schema=@Schema,
		value="value"
	)
	public static class D2 {}
	Body d2 = D2.class.getAnnotationsByType(Body.class)[0];

	@Test
	public void d01_comparisonWithDeclarativeAnnotations() {
		assertObject(d1).is(d2).is(a1);
		assertInteger(d1.hashCode()).is(d2.hashCode()).is(a1.hashCode()).isNotAny(0,-1);
	}
}
