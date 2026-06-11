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
package org.apache.juneau.marshall.jsonschema;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.*;
import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S1186", // Empty test method intentional for framework testing
	"removal" // Tests deprecated API for backward compatibility
})
class SchemaAnnotation_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Schema a1 = SchemaAnnotation.create()
		.default_("a")
		.enum_("b")
		.$ref("c")
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
		.p("v")
		.pattern("w")
		.r(true)
		.readOnly(true)
		.required(true)
		.ro(true)
		.su("ee")
		.summary("dd")
		.t("z")
		.title("aa")
		.type("bb")
		.ui(true)
		.uniqueItems(true)
		.xml("cc")
		.build();

	Schema a2 = SchemaAnnotation.create()
		.default_("a")
		.enum_("b")
		.$ref("c")
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
		.p("v")
		.pattern("w")
		.r(true)
		.readOnly(true)
		.required(true)
		.ro(true)
		.su("ee")
		.summary("dd")
		.t("z")
		.title("aa")
		.type("bb")
		.ui(true)
		.uniqueItems(true)
		.xml("cc")
		.build();

	@Test void a01_basic() {
		assertBean(a1,
			"$ref,default_,enum_,aev,allOf,allowEmptyValue,cf,collectionFormat,d,description,df,discriminator,e,emax,emin,exclusiveMaximum,exclusiveMinimum,externalDocs{description,url},f,format,ignore,items{$ref,default_,enum_,cf,collectionFormat,description,df,e,emax,emin,exclusiveMaximum,exclusiveMinimum,f,format,items{$ref,default_,enum_,cf,collectionFormat,description,df,e,emax,emin,exclusiveMaximum,exclusiveMinimum,f,format,max,maxItems,maxLength,maxi,maximum,maxl,min,minItems,minLength,mini,minimum,minl,mo,multipleOf,p,pattern,t,type,ui,uniqueItems},max,maxItems,maxLength,maxi,maximum,maxl,min,minItems,minLength,mini,minimum,minl,mo,multipleOf,p,pattern,t,type,ui,uniqueItems},max,maxItems,maxLength,maxProperties,maxi,maximum,maxl,maxp,min,minItems,minLength,minProperties,mini,minimum,minl,minp,mo,multipleOf,p,pattern,r,readOnly,required,ro,sie,skipIfEmpty,su,summary,t,title,type,ui,uniqueItems,xml",
			"c,[a],[b],false,[e],false,f,g,[h],[i],[j],k,[l],true,true,true,true,{[],},m,n,true,{,[],[],,,[],[],[],false,false,false,false,,,{,[],[],,,[],[],[],false,false,false,false,,,,-1,-1,-1,,-1,,-1,-1,-1,,-1,,,,,,,false,false},,-1,-1,-1,,-1,,-1,-1,-1,,-1,,,,,,,false,false},o,2,4,6,1,p,3,5,q,8,10,12,7,r,9,11,s,t,v,w,true,true,true,true,false,false,ee,dd,z,aa,bb,true,true,[cc]");
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
		var bc1 = MarshallingContext.create().annotations(a1).build();
		var bc2 = MarshallingContext.create().annotations(a2).build();
		assertSame(bc1, bc2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other methods.
	//------------------------------------------------------------------------------------------------------------------

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Schema(
		default_="a",
		enum_="b",
		$ref="c",
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
		p="v",
		pattern="w",
		r=true,
		readOnly=true,
		required=true,
		ro=true,
		su="ee",
		summary="dd",
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
		default_="a",
		enum_="b",
		$ref="c",
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
		p="v",
		pattern="w",
		r=true,
		readOnly=true,
		required=true,
		ro=true,
		su="ee",
		summary="dd",
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

	Schema draft20201 = SchemaAnnotation.create()
		.const_("constantValue")
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
		.if_("properties:{foo:{const:'bar'}}")
		.then_("required:['baz']")
		.else_("required:['qux']")
		.$defs("MyDef:{type:'string'}")
		.$id("https://example.com/schemas/my-schema")
		.build();

	Schema draft20202 = SchemaAnnotation.create()
		.const_("constantValue")
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
		.if_("properties:{foo:{const:'bar'}}")
		.then_("required:['baz']")
		.else_("required:['qux']")
		.$defs("MyDef:{type:'string'}")
		.$id("https://example.com/schemas/my-schema")
		.build();

	@Test void e01_draft2020_basic() {
		assertBean(draft20201,
			"$comment,$defs,$id,const_,else_,if_,then_,contentEncoding,contentMediaType,dependentRequired,dependentSchemas,deprecatedProperty,examples,exclusiveMaximumValue,exclusiveMinimumValue,prefixItems,unevaluatedItems,unevaluatedProperties",
			"[This is a schema comment],[MyDef:{type:'string'}],https://example.com/schemas/my-schema,[constantValue],[required:['qux']],[properties:{foo:{const:'bar'}}],[required:['baz']],base64,application/json,[prop1:prop2,prop3],[prop1:{type:'string'}],true,[example1,example2],100,0,[string,number],[false],[false]");
	}

	@Test void e02_draft2020_testEquivalency() {
		assertEquals(draft20202, draft20201);
		assertNotEqualsAny(draft20201.hashCode(), 0, -1);
		assertEquals(draft20201.hashCode(), draft20202.hashCode());
	}

	@Test void e03_draft2020_testEquivalencyInPropertyStores() {
		var bc1 = MarshallingContext.create().annotations(draft20201).build();
		var bc2 = MarshallingContext.create().annotations(draft20202).build();
		assertSame(bc1, bc2);
	}

	@Schema(
		const_="constantValue",
		examples={"example1", "example2"},
		$comment="This is a schema comment",
		deprecated_=true,
		exclusiveMaximumValue="100",
		exclusiveMinimumValue="0",
		contentMediaType="application/json",
		contentEncoding="base64",
		prefixItems={"string", "number"},
		unevaluatedItems="false",
		unevaluatedProperties="false",
		dependentSchemas="prop1:{type:'string'}",
		dependentRequired="prop1:prop2,prop3",
		if_="properties:{foo:{const:'bar'}}",
		then_="required:['baz']",
		else_="required:['qux']",
		$defs="MyDef:{type:'string'}",
		$id="https://example.com/schemas/my-schema"
	)
	public static class E1 {}
	Schema e1 = E1.class.getAnnotationsByType(Schema.class)[0];

	@Schema(
		const_="constantValue",
		examples={"example1", "example2"},
		$comment="This is a schema comment",
		deprecated_=true,
		exclusiveMaximumValue="100",
		exclusiveMinimumValue="0",
		contentMediaType="application/json",
		contentEncoding="base64",
		prefixItems={"string", "number"},
		unevaluatedItems="false",
		unevaluatedProperties="false",
		dependentSchemas="prop1:{type:'string'}",
		dependentRequired="prop1:prop2,prop3",
		if_="properties:{foo:{const:'bar'}}",
		then_="required:['baz']",
		else_="required:['qux']",
		$defs="MyDef:{type:'string'}",
		$id="https://example.com/schemas/my-schema"
	)
	public static class E2 {}
	Schema e2 = E2.class.getAnnotationsByType(Schema.class)[0];

	@Test void e04_draft2020_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(draft20201, e1, e2);
		assertNotEqualsAny(draft20201.hashCode(), 0, -1);
		assertEqualsAll(draft20201.hashCode(), e1.hashCode(), e2.hashCode());
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

	@Test void g01_summary_asMap_emptyAnnotationOmitsKey() throws Exception {
		assertFalse(SchemaAnnotation.asMap(SchemaAnnotation.DEFAULT).containsKey("summary"));
	}

	@Test void g02_summary_asMap_summaryWins() throws Exception {
		var a = SchemaAnnotation.create().summary("the-real-summary").build();
		assertEquals("the-real-summary", SchemaAnnotation.asMap(a).get("summary"));
	}

	@Test void g03_summary_asMap_suAliasUsedWhenSummaryEmpty() throws Exception {
		var a = SchemaAnnotation.create().su("from-alias").build();
		assertEquals("from-alias", SchemaAnnotation.asMap(a).get("summary"));
	}

	@Test void g04_summary_asMap_summaryTakesPrecedenceOverSu() throws Exception {
		var a = SchemaAnnotation.create().summary("primary").su("alias").build();
		assertEquals("primary", SchemaAnnotation.asMap(a).get("summary"));
	}

	@Test void g05_summary_emptyValueOmitsKey() throws Exception {
		var a = SchemaAnnotation.create().summary("").su("").build();
		assertFalse(SchemaAnnotation.asMap(a).containsKey("summary"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Builder coverage for boolean setters not exercised by a1/a2 (aev, allowEmptyValue, sie, skipIfEmpty).
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_builder_aev() {
		var a = SchemaAnnotation.create().aev(true).build();
		assertTrue(a.aev());
	}

	@Test void h02_builder_allowEmptyValue() {
		var a = SchemaAnnotation.create().allowEmptyValue(true).build();
		assertTrue(a.allowEmptyValue());
	}

	@Test void h03_builder_sie() {
		var a = SchemaAnnotation.create().sie(true).build();
		assertTrue(a.sie());
	}

	@Test void h04_builder_skipIfEmpty() {
		var a = SchemaAnnotation.create().skipIfEmpty(true).build();
		assertTrue(a.skipIfEmpty());
	}

	//------------------------------------------------------------------------------------------------------------------
	// asMap() branches not exercised elsewhere.
	//------------------------------------------------------------------------------------------------------------------

	@Test void i01_asMap_nullAnnotation_returnsEmptyMap() throws Exception {
		var m = SchemaAnnotation.asMap(null);
		assertNotNull(m);
		assertTrue(m.isEmpty());
	}

	@Test void i02_asMap_ignoreTrue_includesIgnoreKey() throws Exception {
		var a = SchemaAnnotation.create().ignore(true).build();
		assertEquals("true", SchemaAnnotation.asMap(a).get("ignore"));
	}

	@Test void i03_asMap_exclusiveMaximumBooleanFallback() throws Exception {
		// Forces the `else if (a.exclusiveMaximum() || a.emax())` short-circuit branch. // NOSONAR
		var a = SchemaAnnotation.create().exclusiveMaximum(true).build();
		assertEquals("true", SchemaAnnotation.asMap(a).get("exclusiveMaximum"));
	}

	@Test void i04_asMap_exclusiveMaximumEmaxAlias() throws Exception {
		// Triggers the second operand of `exclusiveMaximum() || emax()`.
		var a = SchemaAnnotation.create().emax(true).build();
		assertEquals("true", SchemaAnnotation.asMap(a).get("exclusiveMaximum"));
	}

	@Test void i05_asMap_exclusiveMinimumBooleanFallback() throws Exception {
		var a = SchemaAnnotation.create().exclusiveMinimum(true).build();
		assertEquals("true", SchemaAnnotation.asMap(a).get("exclusiveMinimum"));
	}

	@Test void i06_asMap_exclusiveMinimumEminAlias() throws Exception {
		var a = SchemaAnnotation.create().emin(true).build();
		assertEquals("true", SchemaAnnotation.asMap(a).get("exclusiveMinimum"));
	}

	@Test void i07_asMap_readOnlyTrue() throws Exception {
		var a = SchemaAnnotation.create().readOnly(true).build();
		assertEquals(true, SchemaAnnotation.asMap(a).get("readOnly"));
	}

	@Test void i08_asMap_roAliasTrue() throws Exception {
		// Triggers `readOnly() || ro()` second operand.
		var a = SchemaAnnotation.create().ro(true).build();
		assertEquals(true, SchemaAnnotation.asMap(a).get("readOnly"));
	}

	@Test void i09_asMap_requiredTrue() throws Exception {
		var a = SchemaAnnotation.create().required(true).build();
		assertEquals(true, SchemaAnnotation.asMap(a).get("required"));
	}

	@Test void i10_asMap_rAliasTrue() throws Exception {
		// Triggers `required() || r()` second operand.
		var a = SchemaAnnotation.create().r(true).build();
		assertEquals(true, SchemaAnnotation.asMap(a).get("required"));
	}

	@Test void i11_asMap_uniqueItemsTrue() throws Exception {
		var a = SchemaAnnotation.create().uniqueItems(true).build();
		assertEquals(true, SchemaAnnotation.asMap(a).get("uniqueItems"));
	}

	@Test void i12_asMap_uiAliasTrue() throws Exception {
		// Triggers `uniqueItems() || ui()` second operand.
		var a = SchemaAnnotation.create().ui(true).build();
		assertEquals(true, SchemaAnnotation.asMap(a).get("uniqueItems"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// asMap() with non-default Items.
	//
	// SchemaAnnotation.asMap(...) calls merge(m.getMap(PROP_items), a.items()).  When the result
	// map has no prior "items" entry, getMap(...) returns null and the private merge(JsonMap,Items)
	// lazy-creates the inner map. Same lazy-create pattern lives in ExternalDocsAnnotation.merge
	// and SubItemsAnnotation.merge.
	//------------------------------------------------------------------------------------------------------------------

	@Test void j01_asMap_withFullyPopulatedItems() {
		var items = ItemsAnnotation.create()
			.$ref("#/items-ref")
			.cf("csv")
			.collectionFormat("multi")
			.df("zero")
			.default_("one")
			.e("X")
			.enum_("Y")
			.emax(true)
			.emin(true)
			.exclusiveMaximum(true)
			.exclusiveMinimum(true)
			.f("int32")
			.format("int64")
			.items(SubItemsAnnotation.create().type("string").build())
			.max("100")
			.maxi(5)
			.maximum("200")
			.maxItems(6)
			.maxl(7)
			.maxLength(8)
			.min("1")
			.mini(2)
			.minimum("3")
			.minItems(4)
			.minl(9)
			.minLength(10)
			.mo("2")
			.multipleOf("3")
			.p("[a-z]+")
			.pattern("[A-Z]+")
			.t("integer")
			.type("number")
			.ui(true)
			.uniqueItems(true)
			.build();
		var a = SchemaAnnotation.create().items(items).build();
		var m = SchemaAnnotation.asMap(a);
		assertNotNull(m);
		var itemsMap = m.getMap("items");
		assertNotNull(itemsMap, "items map should be lazy-created when annotation is non-default");
		assertEquals("#/items-ref", itemsMap.getString("$ref"));
		assertEquals("number", itemsMap.getString("type"));
	}

	@Test void j02_asMap_withItemsShortFormOnly() {
		// Same lazy-create path as j01, exercised via short-form alias setters only.
		var items = ItemsAnnotation.create()
			.$ref("#/r")
			.cf("ssv")
			.df("d")
			.e("e")
			.emax(true)
			.emin(true)
			.f("float")
			.max("99")
			.maxi(11)
			.maxl(12)
			.min("0")
			.mini(13)
			.minl(14)
			.mo("5")
			.p("p")
			.t("string")
			.ui(true)
			.build();
		var a = SchemaAnnotation.create().items(items).build();
		var m = SchemaAnnotation.asMap(a);
		var itemsMap = m.getMap("items");
		assertNotNull(itemsMap);
		assertEquals("#/r", itemsMap.getString("$ref"));
		assertEquals("string", itemsMap.getString("type"));
	}

	@Test void j03_asMap_withDefaultItems_omitsItemsKey() throws Exception {
		// merge(JsonMap, Items) returns early on default Items; no key should appear.
		var a = SchemaAnnotation.create().items(ItemsAnnotation.DEFAULT).type("string").build();
		assertFalse(SchemaAnnotation.asMap(a).containsKey("items"));
	}
}