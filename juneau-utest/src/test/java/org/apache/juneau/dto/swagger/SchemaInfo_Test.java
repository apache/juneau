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
package org.apache.juneau.dto.swagger;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.bean.swagger.SwaggerBuilder.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.bean.swagger.*;
import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Testcase for {@link SchemaInfo}.
 */
@FixMethodOrder(NAME_ASCENDING)
public class SchemaInfo_Test {

	/**
	 * Test method for getters and setters.
	 */
	@Test
	public void a01_gettersAndSetters() {
		SchemaInfo t = new SchemaInfo();
		assertEquals("foo", t.setFormat("foo").getFormat());
		assertString(t.setFormat(null).getFormat()).isNull();
		assertEquals("foo", t.setTitle("foo").getTitle());
		assertString(t.setTitle(null).getTitle()).isNull();
		assertEquals("foo", t.setDescription("foo").getDescription());
		assertString(t.setDescription(null).getDescription()).isNull();
		assertEquals("foo", t.setDefault("foo").getDefault());
		assertObject(t.setDefault(new StringBuilder("foo")).getDefault()).isType(StringBuilder.class).asString().is("foo");
		assertNull(t.setDefault(null).getDefault());
		assertObject(t.setMultipleOf(123).getMultipleOf()).isType(Integer.class).is(123);
		assertObject(t.setMultipleOf(123f).getMultipleOf()).isType(Float.class).is(123f);
		assertObject(t.setMaximum(123).getMaximum()).isType(Integer.class).is(123);
		assertObject(t.setMaximum(123f).getMaximum()).isType(Float.class).is(123f);
		assertObject(t.setExclusiveMaximum(true).getExclusiveMaximum()).isType(Boolean.class).is(true);
		assertObject(t.setMinimum(123).getMinimum()).isType(Integer.class).is(123);
		assertObject(t.setMinimum(123f).getMinimum()).isType(Float.class).is(123f);
		assertObject(t.setExclusiveMinimum(true).getExclusiveMinimum()).isType(Boolean.class).is(true);
		assertObject(t.setMaxLength(123).getMaxLength()).isType(Integer.class).is(123);
		assertObject(t.setMinLength(123).getMinLength()).isType(Integer.class).is(123);
		assertEquals("foo", t.setPattern("foo").getPattern());
		assertString(t.setPattern(null).getPattern()).isNull();
		assertObject(t.setMaxItems(123).getMaxItems()).isType(Integer.class).is(123);
		assertObject(t.setMinItems(123).getMinItems()).isType(Integer.class).is(123);
		assertObject(t.setUniqueItems(true).getUniqueItems()).isType(Boolean.class).is(true);
		assertObject(t.setMaxProperties(123).getMaxProperties()).isType(Integer.class).is(123);
		assertObject(t.setMinProperties(123).getMinProperties()).isType(Integer.class).is(123);
		assertObject(t.setRequiredProperties("x").getRequiredProperties()).isType(Set.class).asJson().is("['x']");
		assertNull(t.setRequiredProperties((Collection<String>)null).getRequiredProperties());
		assertObject(t.setEnum(set("foo","bar")).getEnum()).isType(Set.class).asJson().is("['foo','bar']");
		assertObject(t.setEnum(set()).getEnum()).isType(Set.class).asJson().is("[]");
		assertNull(t.setEnum((Collection<Object>)null).getEnum());
		assertObject(t.addEnum("foo","bar").getEnum()).isType(Set.class).asJson().is("['foo','bar']");
		assertObject(t.addEnum("baz").getEnum()).isType(Set.class).asJson().is("['foo','bar','baz']");
		assertEquals("foo", t.setType("foo").getType());
		assertString(t.setType(null).getType()).isNull();
		assertObject(t.setItems(items("foo")).getItems()).asJson().is("{type:'foo'}");
		assertObject(t.setAllOf(set("foo","bar")).getAllOf()).isType(Set.class).asJson().is("['foo','bar']");
		assertObject(t.setAllOf(set()).getAllOf()).isType(Set.class).asJson().is("[]");
		assertNull(t.setAllOf((Collection<Object>)null).getAllOf());
		assertObject(t.addAllOf("foo","bar").getAllOf()).isType(Set.class).asJson().is("['foo','bar']");
		assertObject(t.setProperties(map("foo",new SchemaInfo().setType("foo"))).getProperties()).isType(Map.class).asJson().is("{foo:{type:'foo'}}");
		assertObject(t.setProperties(map()).getProperties()).isType(Map.class).asJson().is("{}");
		assertNull(t.setProperties((Map<String,SchemaInfo>)null).getProperties());
		assertObject(t.setAdditionalProperties(new SchemaInfo().setType("foo")).getAdditionalProperties()).isType(SchemaInfo.class).asJson().is("{type:'foo'}");
		assertObject(t.setAdditionalProperties(new SchemaInfo()).getAdditionalProperties()).isType(SchemaInfo.class).asJson().is("{}");
		assertNull(t.setAdditionalProperties((SchemaInfo)null).getAdditionalProperties());
		assertEquals("foo", t.setDiscriminator("foo").getDiscriminator());
		assertString(t.setDiscriminator(null).getDiscriminator()).isNull();
		assertObject(t.setReadOnly(true).getReadOnly()).isType(Boolean.class).is(true);
		assertObject(t.setXml(xml().setName("foo")).getXml()).asJson().is("{name:'foo'}");
		assertObject(t.setExternalDocs(externalDocumentation("foo")).getExternalDocs()).asJson().is("{url:'foo'}");
		assertObject(t.setExample("foo").getExample()).is("foo");
		assertObject(t.setExample(123).getExample()).is(123);
		assertNull(t.setExample(null).getExample());
	}

	/**
	 * Test method for {@link SchemaInfo#set(java.lang.String, java.lang.Object)}.
	 */
	@Test
	public void b01_set() throws Exception {
		SchemaInfo t = new SchemaInfo();

		t
			.set("default", "a")
			.set("enum", set("b"))
			.set("additionalProperties", map("c",alist("c1")))
			.set("allOf", set("d"))
			.set("description", "e")
			.set("discriminator", "f")
			.set("example", "g")
			.set("exclusiveMaximum", true)
			.set("exclusiveMinimum", true)
			.set("externalDocs", externalDocumentation("h"))
			.set("format", "i")
			.set("items", items("j"))
			.set("maximum", 123f)
			.set("maxItems", 123)
			.set("maxLength", 123)
			.set("maxProperties", 123)
			.set("minimum", 123f)
			.set("minItems", 123)
			.set("minLength", 123)
			.set("minProperties", 123)
			.set("multipleOf", 123f)
			.set("pattern", "k")
			.set("properties", map("l",map("l1", 1)))
			.set("readOnly", true)
			.set("requiredProperties", set("x"))
			.set("title", "m")
			.set("type", "n")
			.set("uniqueItems", true)
			.set("xml", xml().setName("o"))
			.set("$ref", "ref");

		assertObject(t).asJson().is("{format:'i',title:'m',description:'e','default':'a',multipleOf:123.0,maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'k',maxItems:123,minItems:123,uniqueItems:true,maxProperties:123,minProperties:123,requiredProperties:['x'],'enum':['b'],type:'n',items:{type:'j'},allOf:['d'],properties:{l:{l1:1}},additionalProperties:{c:['c1']},discriminator:'f',readOnly:true,xml:{name:'o'},externalDocs:{url:'h'},example:'g','$ref':'ref'}");

		t
			.set("default", "a")
			.set("enum", "['b']")
			.set("additionalProperties", "{c:['c1']}")
			.set("allOf", "['d']")
			.set("description", "e")
			.set("discriminator", "f")
			.set("example", "g")
			.set("exclusiveMaximum", "true")
			.set("exclusiveMinimum", "true")
			.set("externalDocs", "{url:'h'}")
			.set("format", "i")
			.set("items", "{type:'j'}")
			.set("maximum", "123.0")
			.set("maxItems", "123")
			.set("maxLength", "123")
			.set("maxProperties", "123")
			.set("minimum", "123.0")
			.set("minItems", "123")
			.set("minLength", "123")
			.set("minProperties", "123")
			.set("multipleOf", "123.0")
			.set("pattern", "k")
			.set("properties", "{l:{l1:1}}")
			.set("readOnly", "true")
			.set("requiredProperties", "['x']")
			.set("title", "m")
			.set("type", "n")
			.set("uniqueItems", "true")
			.set("xml", "{name:'o'}")
			.set("$ref", "ref");

		assertObject(t).asJson().is("{format:'i',title:'m',description:'e','default':'a',multipleOf:123.0,maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'k',maxItems:123,minItems:123,uniqueItems:true,maxProperties:123,minProperties:123,requiredProperties:['x'],'enum':['b'],type:'n',items:{type:'j'},allOf:['d'],properties:{l:{l1:1}},additionalProperties:{c:['c1']},discriminator:'f',readOnly:true,xml:{name:'o'},externalDocs:{url:'h'},example:'g','$ref':'ref'}");

		t
			.set("default", new StringBuilder("a"))
			.set("enum", new StringBuilder("['b']"))
			.set("additionalProperties", new StringBuilder("{c:['c1']}"))
			.set("allOf", new StringBuilder("['d']"))
			.set("description", new StringBuilder("e"))
			.set("discriminator", new StringBuilder("f"))
			.set("example", new StringBuilder("g"))
			.set("exclusiveMaximum", new StringBuilder("true"))
			.set("exclusiveMinimum", new StringBuilder("true"))
			.set("externalDocs", new StringBuilder("{url:'h'}"))
			.set("format", new StringBuilder("i"))
			.set("items", new StringBuilder("{type:'j'}"))
			.set("maximum", new StringBuilder("123.0"))
			.set("maxItems", new StringBuilder("123"))
			.set("maxLength", new StringBuilder("123"))
			.set("maxProperties", new StringBuilder("123"))
			.set("minimum", new StringBuilder("123.0"))
			.set("minItems", new StringBuilder("123"))
			.set("minLength", new StringBuilder("123"))
			.set("minProperties", new StringBuilder("123"))
			.set("multipleOf", new StringBuilder("123.0"))
			.set("pattern", new StringBuilder("k"))
			.set("properties", new StringBuilder("{l:{l1:1}}"))
			.set("readOnly", new StringBuilder("true"))
			.set("requiredProperties", new StringBuilder("['x']"))
			.set("title", new StringBuilder("m"))
			.set("type", new StringBuilder("n"))
			.set("uniqueItems", new StringBuilder("true"))
			.set("xml", new StringBuilder("{name:'o'}"))
			.set("$ref", new StringBuilder("ref"));

		assertObject(t).asJson().is("{format:'i',title:'m',description:'e','default':'a',multipleOf:123.0,maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'k',maxItems:123,minItems:123,uniqueItems:true,maxProperties:123,minProperties:123,requiredProperties:['x'],'enum':['b'],type:'n',items:{type:'j'},allOf:['d'],properties:{l:{l1:1}},additionalProperties:{c:['c1']},discriminator:'f',readOnly:true,xml:{name:'o'},externalDocs:{url:'h'},example:'g','$ref':'ref'}");

		assertEquals("a", t.get("default", String.class));
		assertEquals("['b']", t.get("enum", String.class));
		assertEquals("{c:['c1']}", t.get("additionalProperties", String.class));
		assertEquals("['d']", t.get("allOf", String.class));
		assertEquals("e", t.get("description", String.class));
		assertEquals("f", t.get("discriminator", String.class));
		assertEquals("g", t.get("example", String.class));
		assertEquals("true", t.get("exclusiveMaximum", String.class));
		assertEquals("true", t.get("exclusiveMinimum", String.class));
		assertEquals("{url:'h'}", t.get("externalDocs", String.class));
		assertEquals("i", t.get("format", String.class));
		assertEquals("{type:'j'}", t.get("items", String.class));
		assertEquals("123.0", t.get("maximum", String.class));
		assertEquals("123", t.get("maxItems", String.class));
		assertEquals("123", t.get("maxLength", String.class));
		assertEquals("123", t.get("maxProperties", String.class));
		assertEquals("123.0", t.get("minimum", String.class));
		assertEquals("123", t.get("minItems", String.class));
		assertEquals("123", t.get("minLength", String.class));
		assertEquals("123", t.get("minProperties", String.class));
		assertEquals("123.0", t.get("multipleOf", String.class));
		assertEquals("k", t.get("pattern", String.class));
		assertEquals("{l:{l1:1}}", t.get("properties", String.class));
		assertEquals("true", t.get("readOnly", String.class));
		assertEquals("['x']", t.get("requiredProperties", String.class));
		assertEquals("m", t.get("title", String.class));
		assertEquals("n", t.get("type", String.class));
		assertEquals("true", t.get("uniqueItems", String.class));
		assertEquals("{name:'o'}", t.get("xml", String.class));
		assertEquals("ref", t.get("$ref", String.class));

		assertObject(t.get("default", Object.class)).isType(StringBuilder.class);
		assertObject(t.get("enum", Object.class)).isType(Set.class);
		assertObject(t.get("additionalProperties", Object.class)).isType(SchemaInfo.class);
		assertObject(t.get("allOf", Object.class)).isType(Set.class);
		assertObject(t.get("description", Object.class)).isType(String.class);
		assertObject(t.get("discriminator", Object.class)).isType(String.class);
		assertObject(t.get("example", Object.class)).isType(StringBuilder.class);
		assertObject(t.get("exclusiveMaximum", Object.class)).isType(Boolean.class);
		assertObject(t.get("exclusiveMinimum", Object.class)).isType(Boolean.class);
		assertObject(t.get("externalDocs", Object.class)).isType(ExternalDocumentation.class);
		assertObject(t.get("format", Object.class)).isType(String.class);
		assertObject(t.get("items", Object.class)).isType(Items.class);
		assertObject(t.get("maximum", Object.class)).isType(Float.class);
		assertObject(t.get("maxItems", Object.class)).isType(Integer.class);
		assertObject(t.get("maxLength", Object.class)).isType(Integer.class);
		assertObject(t.get("maxProperties", Object.class)).isType(Integer.class);
		assertObject(t.get("minimum", Object.class)).isType(Float.class);
		assertObject(t.get("minItems", Object.class)).isType(Integer.class);
		assertObject(t.get("minLength", Object.class)).isType(Integer.class);
		assertObject(t.get("minProperties", Object.class)).isType(Integer.class);
		assertObject(t.get("multipleOf", Object.class)).isType(Float.class);
		assertObject(t.get("pattern", Object.class)).isType(String.class);
		assertObject(t.get("properties", Object.class)).isType(Map.class);
		assertObject(t.get("readOnly", Object.class)).isType(Boolean.class);
		assertObject(t.get("requiredProperties", Object.class)).isType(Set.class);
		assertObject(t.get("title", Object.class)).isType(String.class);
		assertObject(t.get("type", Object.class)).isType(String.class);
		assertObject(t.get("uniqueItems", Object.class)).isType(Boolean.class);
		assertObject(t.get("xml", Object.class)).isType(Xml.class);
		assertObject(t.get("$ref", Object.class)).isType(String.class);

		t.set("null", null).set(null, "null");
		assertNull(t.get("null", Object.class));
		assertNull(t.get(null, Object.class));
		assertNull(t.get("foo", Object.class));

		String s = "{format:'i',title:'m',description:'e','default':'a',multipleOf:123.0,maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'k',maxItems:123,minItems:123,uniqueItems:true,maxProperties:123,minProperties:123,requiredProperties:['x'],'enum':['b'],type:'n',items:{type:'j'},allOf:['d'],properties:{l:{l1:1}},additionalProperties:{c:['c1']},discriminator:'f',readOnly:true,xml:{name:'o'},externalDocs:{url:'h'},example:'g','$ref':'ref'}";
		assertObject(JsonParser.DEFAULT.parse(s, SchemaInfo.class)).asJson().is(s);
	}

	@Test
	public void b02_copy() {
		SchemaInfo t = new SchemaInfo();

		t = t.copy();

		assertObject(t).asJson().is("{}");

		t
			.set("default", "a")
			.set("enum", set("b"))
			.set("additionalProperties", map("c",alist("c1")))
			.set("allOf", set("d"))
			.set("description", "e")
			.set("discriminator", "f")
			.set("example", "g")
			.set("exclusiveMaximum", true)
			.set("exclusiveMinimum", true)
			.set("externalDocs", externalDocumentation("h"))
			.set("format", "i")
			.set("items", items("j"))
			.set("maximum", 123f)
			.set("maxItems", 123)
			.set("maxLength", 123)
			.set("maxProperties", 123)
			.set("minimum", 123f)
			.set("minItems", 123)
			.set("minLength", 123)
			.set("minProperties", 123)
			.set("multipleOf", 123f)
			.set("pattern", "k")
			.set("properties", map("l",map("l1", 1)))
			.set("readOnly", true)
			.set("requiredProperties", set("x"))
			.set("title", "m")
			.set("type", "n")
			.set("uniqueItems", true)
			.set("xml", xml().setName("o"))
			.set("$ref", "ref")
			.copy();

		assertObject(t).asJson().is("{format:'i',title:'m',description:'e','default':'a',multipleOf:123.0,maximum:123.0,exclusiveMaximum:true,minimum:123.0,exclusiveMinimum:true,maxLength:123,minLength:123,pattern:'k',maxItems:123,minItems:123,uniqueItems:true,maxProperties:123,minProperties:123,requiredProperties:['x'],'enum':['b'],type:'n',items:{type:'j'},allOf:['d'],properties:{l:{l1:1}},additionalProperties:{c:['c1']},discriminator:'f',readOnly:true,xml:{name:'o'},externalDocs:{url:'h'},example:'g','$ref':'ref'}");
	}

	@Test
	public void b03_keySet() {
		SchemaInfo t = new SchemaInfo();

		assertObject(t.keySet()).asJson().is("[]");

		t
			.set("additionalProperties", map("c",alist("c1")))
			.set("allOf", set("d"))
			.set("default", "a")
			.set("description", "e")
			.set("discriminator", "f")
			.set("enum", set("b"))
			.set("example", "g")
			.set("exclusiveMaximum", true)
			.set("exclusiveMinimum", true)
			.set("externalDocs", externalDocumentation("h"))
			.set("format", "i")
			.set("items", items("j"))
			.set("maximum", 123f)
			.set("maxItems", 123)
			.set("maxLength", 123)
			.set("maxProperties", 123)
			.set("minimum", 123f)
			.set("minItems", 123)
			.set("minLength", 123)
			.set("minProperties", 123)
			.set("multipleOf", 123f)
			.set("pattern", "k")
			.set("properties", map("l",map("l1", 1)))
			.set("readOnly", true)
			.set("$ref", "ref")
			.set("requiredProperties", set("x"))
			.set("title", "m")
			.set("type", "n")
			.set("uniqueItems", true)
			.set("xml", xml().setName("o"));

		assertObject(t.keySet()).asJson().is(
			"['additionalProperties','allOf','default','description','discriminator','enum','example','exclusiveMaximum','exclusiveMinimum',"
			+ "'externalDocs','format','items','maximum','maxItems','maxLength','maxProperties','minimum','minItems','minLength','minProperties',"
			+ "'multipleOf','pattern','properties','readOnly','$ref','requiredProperties','title','type','uniqueItems','xml']"
		);
	}
}