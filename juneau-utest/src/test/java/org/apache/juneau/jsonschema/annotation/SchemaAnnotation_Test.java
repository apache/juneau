/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.jsonschema.annotation;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.junit.jupiter.api.*;

class SchemaAnnotation_Test extends TestBase {

	private static final String CNAME = SchemaAnnotation_Test.class.getName();

	private static class X1 {}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Schema a1 = SchemaAnnotation.create()
		._default("a")
		._enum("b")
		.$ref("c")
		.additionalProperties("d")
		.allOf("e")
		.cf("f")
		.collectionFormat("g")
		.d("h")
		.description("i")
		.df("j")
		.discriminator("k")
		.e("l")
		.emax(true)
		.emin(true)
		.exclusiveMaximum(true)
		.exclusiveMinimum(true)
		.externalDocs(ExternalDocsAnnotation.DEFAULT)
		.f("m")
		.format("n")
		.ignore(true)
		.items(ItemsAnnotation.DEFAULT)
		.max("o")
		.maxi(1)
		.maximum("p")
		.maxItems(2)
		.maxl(3)
		.maxLength(4)
		.maxp(5)
		.maxProperties(6)
		.min("q")
		.mini(7)
		.minimum("r")
		.minItems(8)
		.minl(9)
		.minLength(10)
		.minp(11)
		.minProperties(12)
		.mo("s")
		.multipleOf("t")
		.on("u")
		.onClass(X1.class)
		.p("v")
		.pattern("w")
		.properties("x")
		.r(true)
		.readOnly(true)
		.required(true)
		.ro(true)
		.t("z")
		.title("aa")
		.type("bb")
		.ui(true)
		.uniqueItems(true)
		.xml("cc")
		.build();

	Schema a2 = SchemaAnnotation.create()
		._default("a")
		._enum("b")
		.$ref("c")
		.additionalProperties("d")
		.allOf("e")
		.cf("f")
		.collectionFormat("g")
		.d("h")
		.description("i")
		.df("j")
		.discriminator("k")
		.e("l")
		.emax(true)
		.emin(true)
		.exclusiveMaximum(true)
		.exclusiveMinimum(true)
		.externalDocs(ExternalDocsAnnotation.DEFAULT)
		.f("m")
		.format("n")
		.ignore(true)
		.items(ItemsAnnotation.DEFAULT)
		.max("o")
		.maxi(1)
		.maximum("p")
		.maxItems(2)
		.maxl(3)
		.maxLength(4)
		.maxp(5)
		.maxProperties(6)
		.min("q")
		.mini(7)
		.minimum("r")
		.minItems(8)
		.minl(9)
		.minLength(10)
		.minp(11)
		.minProperties(12)
		.mo("s")
		.multipleOf("t")
		.on("u")
		.onClass(X1.class)
		.p("v")
		.pattern("w")
		.properties("x")
		.r(true)
		.readOnly(true)
		.required(true)
		.ro(true)
		.t("z")
		.title("aa")
		.type("bb")
		.ui(true)
		.uniqueItems(true)
		.xml("cc")
		.build();

	@Test void a01_basic() {
		assertBean(a1,
			"$ref,_default,_enum,additionalProperties,aev,allOf,allowEmptyValue,cf,collectionFormat,d,description,df,discriminator,e,emax,emin,exclusiveMaximum,exclusiveMinimum,externalDocs{description,url},f,format,ignore,items{$ref,_default,_enum,cf,collectionFormat,description,df,e,emax,emin,exclusiveMaximum,exclusiveMinimum,f,format,items{$ref,_default,_enum,cf,collectionFormat,description,df,e,emax,emin,exclusiveMaximum,exclusiveMinimum,f,format,items,max,maxItems,maxLength,maxi,maximum,maxl,min,minItems,minLength,mini,minimum,minl,mo,multipleOf,p,pattern,t,type,ui,uniqueItems},max,maxItems,maxLength,maxi,maximum,maxl,min,minItems,minLength,mini,minimum,minl,mo,multipleOf,p,pattern,t,type,ui,uniqueItems},max,maxItems,maxLength,maxProperties,maxi,maximum,maxl,maxp,min,minItems,minLength,minProperties,mini,minimum,minl,minp,mo,multipleOf,on,onClass,p,pattern,properties,r,readOnly,required,ro,sie,skipIfEmpty,t,title,type,ui,uniqueItems,xml",
			"c,[a],[b],[d],false,[e],false,f,g,[h],[i],[j],k,[l],true,true,true,true,{[],},m,n,true,{,[],[],,,[],[],[],false,false,false,false,,,{,[],[],,,[],[],[],false,false,false,false,,,[],,-1,-1,-1,,-1,,-1,-1,-1,,-1,,,,,,,false,false},,-1,-1,-1,,-1,,-1,-1,-1,,-1,,,,,,,false,false},o,2,4,6,1,p,3,5,q,8,10,12,7,r,9,11,s,t,[u],[X1],v,w,[x],true,true,true,true,false,false,z,aa,bb,true,true,[cc]");
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

	public static class C1 {
		public int f1;
		public void m1() {}  // NOSONAR
	}
	public static class C2 {
		public int f2;
		public void m2() {}  // NOSONAR
	}

	@Test void c01_otherMethods() throws Exception {
		var c1 = SchemaAnnotation.create(C1.class).on(C2.class).build();
		var c2 = SchemaAnnotation.create("a").on("b").build();
		var c3 = SchemaAnnotation.create().on(C1.class.getField("f1")).on(C2.class.getField("f2")).build();
		var c4 = SchemaAnnotation.create().on(C1.class.getMethod("m1")).on(C2.class.getMethod("m2")).build();

		assertBean(c1, "on", "["+CNAME+"$C1,"+CNAME+"$C2]");
		assertBean(c2, "on", "[a,b]");
		assertBean(c3, "on", "["+CNAME+"$C1.f1,"+CNAME+"$C2.f2]");
		assertBean(c4, "on", "["+CNAME+"$C1.m1(),"+CNAME+"$C2.m2()]");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Schema(
		_default="a",
		_enum="b",
		$ref="c",
		additionalProperties="d",
		allOf="e",
		cf="f",
		collectionFormat="g",
		d="h",
		description="i",
		df="j",
		discriminator="k",
		e="l",
		emax=true,
		emin=true,
		exclusiveMaximum=true,
		exclusiveMinimum=true,
		externalDocs=@ExternalDocs,
		f="m",
		format="n",
		ignore=true,
		items=@Items,
		max="o",
		maxi=1,
		maximum="p",
		maxItems=2,
		maxl=3,
		maxLength=4,
		maxp=5,
		maxProperties=6,
		min="q",
		mini=7,
		minimum="r",
		minItems=8,
		minl=9,
		minLength=10,
		minp=11,
		minProperties=12,
		mo="s",
		multipleOf="t",
		on="u",
		onClass=X1.class,
		p="v",
		pattern="w",
		properties="x",
		r=true,
		readOnly=true,
		required=true,
		ro=true,
		t="z",
		title="aa",
		type="bb",
		ui=true,
		uniqueItems=true,
		xml="cc"
	)
	public static class D1 {}
	Schema d1 = D1.class.getAnnotationsByType(Schema.class)[0];

	@Schema(
		_default="a",
		_enum="b",
		$ref="c",
		additionalProperties="d",
		allOf="e",
		cf="f",
		collectionFormat="g",
		d="h",
		description="i",
		df="j",
		discriminator="k",
		e="l",
		emax=true,
		emin=true,
		exclusiveMaximum=true,
		exclusiveMinimum=true,
		externalDocs=@ExternalDocs,
		f="m",
		format="n",
		ignore=true,
		items=@Items,
		max="o",
		maxi=1,
		maximum="p",
		maxItems=2,
		maxl=3,
		maxLength=4,
		maxp=5,
		maxProperties=6,
		min="q",
		mini=7,
		minimum="r",
		minItems=8,
		minl=9,
		minLength=10,
		minp=11,
		minProperties=12,
		mo="s",
		multipleOf="t",
		on="u",
		onClass=X1.class,
		p="v",
		pattern="w",
		properties="x",
		r=true,
		readOnly=true,
		required=true,
		ro=true,
		t="z",
		title="aa",
		type="bb",
		ui=true,
		uniqueItems=true,
		xml="cc"
	)
	public static class D2 {}
	Schema d2 = D2.class.getAnnotationsByType(Schema.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// JSON Schema Draft 2020-12 properties
	//------------------------------------------------------------------------------------------------------------------

	Schema draft2020_1 = SchemaAnnotation.create()
		._const("constantValue")
		.examples("example1", "example2")
		.$comment("This is a schema comment")
		.deprecatedProperty(true)
		.exclusiveMaximumValue("100")
		.exclusiveMinimumValue("0")
		.contentMediaType("application/json")
		.contentEncoding("base64")
		.prefixItems("string", "number")
		.unevaluatedItems("false")
		.unevaluatedProperties("false")
		.dependentSchemas("prop1:{type:'string'}")
		.dependentRequired("prop1:prop2,prop3")
		._if("properties:{foo:{const:'bar'}}")
		._then("required:['baz']")
		._else("required:['qux']")
		.$defs("MyDef:{type:'string'}")
		.$id("https://example.com/schemas/my-schema")
		.build();

	Schema draft2020_2 = SchemaAnnotation.create()
		._const("constantValue")
		.examples("example1", "example2")
		.$comment("This is a schema comment")
		.deprecatedProperty(true)
		.exclusiveMaximumValue("100")
		.exclusiveMinimumValue("0")
		.contentMediaType("application/json")
		.contentEncoding("base64")
		.prefixItems("string", "number")
		.unevaluatedItems("false")
		.unevaluatedProperties("false")
		.dependentSchemas("prop1:{type:'string'}")
		.dependentRequired("prop1:prop2,prop3")
		._if("properties:{foo:{const:'bar'}}")
		._then("required:['baz']")
		._else("required:['qux']")
		.$defs("MyDef:{type:'string'}")
		.$id("https://example.com/schemas/my-schema")
		.build();

	@Test void e01_draft2020_basic() {
		assertBean(draft2020_1,
			"$comment,$defs,$id,_const,_else,_if,_then,contentEncoding,contentMediaType,dependentRequired,dependentSchemas,deprecatedProperty,examples,exclusiveMaximumValue,exclusiveMinimumValue,prefixItems,unevaluatedItems,unevaluatedProperties",
			"[This is a schema comment],[MyDef:{type:'string'}],https://example.com/schemas/my-schema,[constantValue],[required:['qux']],[properties:{foo:{const:'bar'}}],[required:['baz']],base64,application/json,[prop1:prop2,prop3],[prop1:{type:'string'}],true,[example1,example2],100,0,[string,number],[false],[false]");
	}

	@Test void e02_draft2020_testEquivalency() {
		assertEquals(draft2020_2, draft2020_1);
		assertNotEqualsAny(draft2020_1.hashCode(), 0, -1);
		assertEquals(draft2020_1.hashCode(), draft2020_2.hashCode());
	}

	@Test void e03_draft2020_testEquivalencyInPropertyStores() {
		var bc1 = BeanContext.create().annotations(draft2020_1).build();
		var bc2 = BeanContext.create().annotations(draft2020_2).build();
		assertSame(bc1, bc2);
	}

	@Schema(
		_const="constantValue",
		examples={"example1", "example2"},
		$comment="This is a schema comment",
		deprecatedProperty=true,
		exclusiveMaximumValue="100",
		exclusiveMinimumValue="0",
		contentMediaType="application/json",
		contentEncoding="base64",
		prefixItems={"string", "number"},
		unevaluatedItems="false",
		unevaluatedProperties="false",
		dependentSchemas="prop1:{type:'string'}",
		dependentRequired="prop1:prop2,prop3",
		_if="properties:{foo:{const:'bar'}}",
		_then="required:['baz']",
		_else="required:['qux']",
		$defs="MyDef:{type:'string'}",
		$id="https://example.com/schemas/my-schema"
	)
	public static class E1 {}
	Schema e1 = E1.class.getAnnotationsByType(Schema.class)[0];

	@Schema(
		_const="constantValue",
		examples={"example1", "example2"},
		$comment="This is a schema comment",
		deprecatedProperty=true,
		exclusiveMaximumValue="100",
		exclusiveMinimumValue="0",
		contentMediaType="application/json",
		contentEncoding="base64",
		prefixItems={"string", "number"},
		unevaluatedItems="false",
		unevaluatedProperties="false",
		dependentSchemas="prop1:{type:'string'}",
		dependentRequired="prop1:prop2,prop3",
		_if="properties:{foo:{const:'bar'}}",
		_then="required:['baz']",
		_else="required:['qux']",
		$defs="MyDef:{type:'string'}",
		$id="https://example.com/schemas/my-schema"
	)
	public static class E2 {}
	Schema e2 = E2.class.getAnnotationsByType(Schema.class)[0];

	@Test void e04_draft2020_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(draft2020_1, e1, e2);
		assertNotEqualsAny(draft2020_1.hashCode(), 0, -1);
		assertEqualsAll(draft2020_1.hashCode(), e1.hashCode(), e2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Backward compatibility: exclusiveMaximum/exclusiveMinimum fallback
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_backwardCompatibility_exclusiveMaxMin() {
		// Test that old boolean exclusiveMaximum/exclusiveMinimum still work
		Schema oldStyle = SchemaAnnotation.create()
			.exclusiveMaximum(true)
			.exclusiveMinimum(true)
			.maximum("100")
			.minimum("0")
			.build();
		
		assertBean(oldStyle, "exclusiveMaximum,exclusiveMinimum,maximum,minimum", "true,true,100,0");
		
		// Test that new numeric style takes precedence
		Schema newStyle = SchemaAnnotation.create()
			.exclusiveMaximumValue("100")
			.exclusiveMinimumValue("0")
			.build();
		
		assertBean(newStyle, "exclusiveMaximumValue,exclusiveMinimumValue", "100,0");
		
		// Test that new style takes precedence when both are set
		Schema mixed = SchemaAnnotation.create()
			.exclusiveMaximum(false)
			.exclusiveMinimum(false)
			.exclusiveMaximumValue("100")
			.exclusiveMinimumValue("0")
			.build();
		
		assertBean(mixed, "exclusiveMaximum,exclusiveMinimum,exclusiveMaximumValue,exclusiveMinimumValue", "false,false,100,0");
	}
}