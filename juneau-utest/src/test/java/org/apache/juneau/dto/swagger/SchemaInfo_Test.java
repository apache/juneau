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
import static org.apache.juneau.dto.swagger.SwaggerBuilder.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

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
		assertString(t.setFormat("foo").getFormat()).is("foo");
		assertString(t.setFormat(null).getFormat()).isNull();
		assertString(t.setTitle("foo").getTitle()).is("foo");
		assertString(t.setTitle(null).getTitle()).isNull();
		assertString(t.setDescription("foo").getDescription()).is("foo");
		assertString(t.setDescription(null).getDescription()).isNull();
		assertString(t.setDefault("foo").getDefault()).is("foo");
		assertObject(t.setDefault(new StringBuilder("foo")).getDefault()).isType(StringBuilder.class).asString().is("foo");
		assertObject(t.setDefault(null).getDefault()).isNull();
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
		assertString(t.setPattern("foo").getPattern()).is("foo");
		assertString(t.setPattern(null).getPattern()).isNull();
		assertObject(t.setMaxItems(123).getMaxItems()).isType(Integer.class).is(123);
		assertObject(t.setMinItems(123).getMinItems()).isType(Integer.class).is(123);
		assertObject(t.setUniqueItems(true).getUniqueItems()).isType(Boolean.class).is(true);
		assertObject(t.setMaxProperties(123).getMaxProperties()).isType(Integer.class).is(123);
		assertObject(t.setMinProperties(123).getMinProperties()).isType(Integer.class).is(123);
		assertObject(t.setRequiredProperties("x").getRequiredProperties()).isType(Set.class).asJson().is("['x']");
		assertObject(t.setRequiredProperties((Collection<String>)null).getRequiredProperties()).isNull();
		assertObject(t.setEnum(set("foo","bar")).getEnum()).isType(Set.class).asJson().is("['foo','bar']");
		assertObject(t.setEnum(set()).getEnum()).isType(Set.class).asJson().is("[]");
		assertObject(t.setEnum((Collection<Object>)null).getEnum()).isNull();
		assertObject(t.addEnum("foo","bar").getEnum()).isType(Set.class).asJson().is("['foo','bar']");
		assertObject(t.addEnum("baz").getEnum()).isType(Set.class).asJson().is("['foo','bar','baz']");
		assertString(t.setType("foo").getType()).is("foo");
		assertString(t.setType(null).getType()).isNull();
		assertObject(t.setItems(items("foo")).getItems()).asJson().is("{type:'foo'}");
		assertObject(t.setAllOf(set("foo","bar")).getAllOf()).isType(Set.class).asJson().is("['foo','bar']");
		assertObject(t.setAllOf(set()).getAllOf()).isType(Set.class).asJson().is("[]");
		assertObject(t.setAllOf((Collection<Object>)null).getAllOf()).isNull();
		assertObject(t.addAllOf("foo","bar").getAllOf()).isType(Set.class).asJson().is("['foo','bar']");
		assertObject(t.setProperties(map("foo",new SchemaInfo().setType("foo"))).getProperties()).isType(Map.class).asJson().is("{foo:{type:'foo'}}");
		assertObject(t.setProperties(map()).getProperties()).isType(Map.class).asJson().is("{}");
		assertObject(t.setProperties((Map<String,SchemaInfo>)null).getProperties()).isNull();
		assertObject(t.setAdditionalProperties(new SchemaInfo().setType("foo")).getAdditionalProperties()).isType(SchemaInfo.class).asJson().is("{type:'foo'}");
		assertObject(t.setAdditionalProperties(new SchemaInfo()).getAdditionalProperties()).isType(SchemaInfo.class).asJson().is("{}");
		assertObject(t.setAdditionalProperties((SchemaInfo)null).getAdditionalProperties()).isNull();
		assertString(t.setDiscriminator("foo").getDiscriminator()).is("foo");
		assertString(t.setDiscriminator(null).getDiscriminator()).isNull();
		assertObject(t.setReadOnly(true).getReadOnly()).isType(Boolean.class).is(true);
		assertObject(t.setXml(xml().setName("foo")).getXml()).asJson().is("{name:'foo'}");
		assertObject(t.setExternalDocs(externalDocumentation("foo")).getExternalDocs()).asJson().is("{url:'foo'}");
		assertObject(t.setExample("foo").getExample()).is("foo");
		assertObject(t.setExample(123).getExample()).is(123);
		assertObject(t.setExample(null).getExample()).isNull();
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

		assertString(t.get("default", String.class)).is("a");
		assertString(t.get("enum", String.class)).is("['b']");
		assertString(t.get("additionalProperties", String.class)).is("{c:['c1']}");
		assertString(t.get("allOf", String.class)).is("['d']");
		assertString(t.get("description", String.class)).is("e");
		assertString(t.get("discriminator", String.class)).is("f");
		assertString(t.get("example", String.class)).is("g");
		assertString(t.get("exclusiveMaximum", String.class)).is("true");
		assertString(t.get("exclusiveMinimum", String.class)).is("true");
		assertString(t.get("externalDocs", String.class)).is("{url:'h'}");
		assertString(t.get("format", String.class)).is("i");
		assertString(t.get("items", String.class)).is("{type:'j'}");
		assertString(t.get("maximum", String.class)).is("123.0");
		assertString(t.get("maxItems", String.class)).is("123");
		assertString(t.get("maxLength", String.class)).is("123");
		assertString(t.get("maxProperties", String.class)).is("123");
		assertString(t.get("minimum", String.class)).is("123.0");
		assertString(t.get("minItems", String.class)).is("123");
		assertString(t.get("minLength", String.class)).is("123");
		assertString(t.get("minProperties", String.class)).is("123");
		assertString(t.get("multipleOf", String.class)).is("123.0");
		assertString(t.get("pattern", String.class)).is("k");
		assertString(t.get("properties", String.class)).is("{l:{l1:1}}");
		assertString(t.get("readOnly", String.class)).is("true");
		assertString(t.get("requiredProperties", String.class)).is("['x']");
		assertString(t.get("title", String.class)).is("m");
		assertString(t.get("type", String.class)).is("n");
		assertString(t.get("uniqueItems", String.class)).is("true");
		assertString(t.get("xml", String.class)).is("{name:'o'}");
		assertString(t.get("$ref", String.class)).is("ref");

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
	public void b02_copy() throws Exception {
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
	public void b03_keySet() throws Exception {
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
