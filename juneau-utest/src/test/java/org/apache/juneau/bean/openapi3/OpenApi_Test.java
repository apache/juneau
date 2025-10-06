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
package org.apache.juneau.bean.openapi3;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.bean.openapi3.OpenApiBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link OpenApi}.
 */
class OpenApi_Test extends TestBase {

	@Nested class A_basicTests extends TestBase {

		private static final BeanTester<OpenApi> TESTER =
			testBean(
				bean()
					.setComponents(components().setSchemas(map("a1", schemaInfo().setType("a2"))))
					.setExternalDocs(externalDocumentation().setUrl(URI.create("b")))
					.setInfo(info().setTitle("c1").setVersion("c2"))
					.setOpenapi("3.0.0")
					.setPaths(map("d1", pathItem().setGet(operation().setSummary("d2"))))
					.setSecurity(list(securityRequirement().setRequirements(map("e1",list("e2")))))
					.setServers(list(server().setUrl(URI.create("f"))))
					.setTags(list(tag().setName("g")))
			)
			.props("components{schemas{a1{type}}},externalDocs{url},info{title,version},openapi,paths{d1{get{summary}}},security{0{requirements{e1}}},servers{0{url}},tags{0{name}}")
			.vals("{{{a2}}},{b},{c1,c2},3.0.0,{{{d2}}},{{{[e2]}}},{{f}},{{g}}")
			.json("{components:{schemas:{a1:{type:'a2'}}},externalDocs:{url:'b'},info:{title:'c1',version:'c2'},openapi:'3.0.0',paths:{d1:{get:{summary:'d2'}}},security:[{requirements:{e1:['e2']}}],servers:[{url:'f'}],tags:[{name:'g'}]}")
			.string("{'components':{'schemas':{'a1':{'type':'a2'}}},'externalDocs':{'url':'b'},'info':{'title':'c1','version':'c2'},'openapi':'3.0.0','paths':{'d1':{'get':{'summary':'d2'}}},'security':[{'requirements':{'e1':['e2']}}],'servers':[{'url':'f'}],'tags':[{'name':'g'}]}".replace('\'','"'))
		;

		@Test void a01_gettersAndSetters() {
			TESTER.assertGettersAndSetters();
		}

		@Test void a02_copy() {
			TESTER.assertCopy();
		}

		@Test void a03_toJson() {
			TESTER.assertToJson();
		}

		@Test void a04_fromJson() {
			TESTER.assertFromJson();
		}

		@Test void a05_roundTrip() {
			TESTER.assertRoundTrip();
		}

		@Test void a06_toString() {
			TESTER.assertToString();
		}

		@Test void a07_keySet() {
			assertList(TESTER.bean().keySet(), "components", "externalDocs", "info", "openapi", "paths", "security", "servers", "tags");
		}
	}

	@Nested class B_emptyTests extends TestBase {

		private static final BeanTester<OpenApi> TESTER =
			testBean(bean())
			.props("openapi,info,externalDocs,servers,tags,paths,components,security")
			.vals("3.0.0,<null>,<null>,<null>,<null>,<null>,<null>,<null>")
			.json("{openapi:'3.0.0'}")
			.string("{\"openapi\":\"3.0.0\"}")
		;

		@Test void b01_gettersAndSetters() {
			TESTER.assertGettersAndSetters();
		}

		@Test void b02_copy() {
			TESTER.assertCopy();
		}

		@Test void b03_toJson() {
			TESTER.assertToJson();
		}

		@Test void b04_fromJson() {
			TESTER.assertFromJson();
		}

		@Test void b05_roundTrip() {
			TESTER.assertRoundTrip();
		}

		@Test void b06_toString() {
			TESTER.assertToString();
		}

		@Test void b07_keySet() {
			assertList(TESTER.bean().keySet(), "openapi");
		}
	}

	@Nested class C_extraProperties extends TestBase {
		private static final BeanTester<OpenApi> TESTER =
			testBean(
				bean()
					.set("components", components().setSchemas(map("a1", schemaInfo("a2"))))
					.set("externalDocs", externalDocumentation().setUrl(URI.create("b")))
					.set("info", info().setTitle("c").setVersion("d"))
					.set("openapi", "e")
					.set("paths", map("f1", pathItem().setGet(operation().setSummary("f2"))))
					.set("security", list(securityRequirement().setRequirements(map("g1",list("g2")))))
					.set("servers", list(server().setUrl(URI.create("h"))))
					.set("tags", list(tag("i")))
					.set("x1", "x1a")
					.set("x2", null)
			)
			.props("components{schemas{a1{type}}},externalDocs{url},info{title,version},openapi,paths{f1{get{summary}}},security{#{requirements{g1{#{toString}}}}},servers{#{url}},tags{#{name}},x1,x2")
			.vals("{{{a2}}},{b},{c,d},e,{{{f2}}},{[{{{[{g2}]}}}]},{[{h}]},{[{i}]},x1a,<null>")
			.json("{components:{schemas:{a1:{type:'a2'}}},externalDocs:{url:'b'},info:{title:'c',version:'d'},openapi:'e',paths:{f1:{get:{summary:'f2'}}},security:[{requirements:{g1:['g2']}}],servers:[{url:'h'}],tags:[{name:'i'}],x1:'x1a'}")
			.string("{'components':{'schemas':{'a1':{'type':'a2'}}},'externalDocs':{'url':'b'},'info':{'title':'c','version':'d'},'openapi':'e','paths':{'f1':{'get':{'summary':'f2'}}},'security':[{'requirements':{'g1':['g2']}}],'servers':[{'url':'h'}],'tags':[{'name':'i'}],'x1':'x1a'}".replace('\'', '"'))
		;

		@Test void c01_gettersAndSetters() {
			TESTER.assertGettersAndSetters();
		}

		@Test void c02_copy() {
			TESTER.assertCopy();
		}

		@Test void c03_toJson() {
			TESTER.assertToJson();
		}

		@Test void c04_fromJson() {
			TESTER.assertFromJson();
		}

		@Test void c05_roundTrip() {
			TESTER.assertRoundTrip();
		}

		@Test void c06_toString() {
			TESTER.assertToString();
		}

		@Test void c07_keySet() {
			assertList(TESTER.bean().keySet(), "components", "externalDocs", "info", "openapi", "paths", "security", "servers", "tags", "x1", "x2");
		}

		@Test void c08_get() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> obj.get(prop, Object.class),
				"components{schemas{a1{type}}},externalDocs{url},info{title,version},openapi,paths{f1{get{summary}}},security{#{requirements{g1{#{toString}}}}},servers{#{url}},tags{#{name}},x1,x2",
				"{{{a2}}},{b},{c,d},e,{{{f2}}},{[{{{[{g2}]}}}]},{[{h}]},{[{i}]},x1a,<null>"
			);
		}

		@Test void c09_getTypes() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> simpleClassNameOf(obj.get(prop, Object.class)),
				"components,externalDocs,info,openapi,paths,security,servers,tags,x1,x2",
				"Components,ExternalDocumentation,Info,String,TreeMap,ArrayList,ArrayList,ArrayList,String,<null>"
			);
		}

		@Test void c10_nullPropertyValue() {
			assertThrows(IllegalArgumentException.class, ()->bean().get(null));
			assertThrows(IllegalArgumentException.class, ()->bean().get(null, String.class));
			assertThrows(IllegalArgumentException.class, ()->bean().set(null, "a"));
		}
	}

	@Nested class D_additionalMethods extends TestBase {

		@Test void d01_addMethods() {
			assertBean(
				bean()
					.addPath("/test", pathItem().setGet(operation().setSummary("a"))),
				"paths{/test{get{summary}}}",
				"{{{a}}}"
			);
		}

		@Test void d02_asMap() {
			assertBean(
				bean()
					.setInfo(info().setTitle("a1").setVersion("a2"))
					.set("x1", "x1a")
					.asMap(),
				"info{title,version},openapi,x1",
				"{a1,a2},3.0.0,x1a"
			);
		}

		@Test void d03_extraKeys() {
			var x = bean().set("x1", "x1a").set("x2", "x2a");
			assertList(x.extraKeys(), "x1", "x2");
			assertEmpty(bean().extraKeys());
		}
	}

	@Nested class E_strictMode extends TestBase {

		@Test void e01_strictModeSetThrowsException() {
			var x = bean().strict();
			assertThrows(RuntimeException.class, () -> x.set("foo", "bar"));
		}

		@Test void e02_nonStrictModeAllowsSet() {
			var x = bean(); // not strict
			assertDoesNotThrow(() -> x.set("foo", "bar"));
		}

		@Test void e03_strictModeToggle() {
			var x = bean();
			assertFalse(x.isStrict());
			x.strict();
			assertTrue(x.isStrict());
			x.strict(false);
			assertFalse(x.isStrict());
		}
	}

	@Nested class G_utilityMethods extends TestBase {

		@Test void g01_findRef() {
			var x = openApi()
				.setComponents(components().setSchemas(map("a1", schemaInfo().setType("a2"))));

			assertBean(
				x.findRef("#/components/schemas/a1", SchemaInfo.class), 
				"type", 
				"a2"
			);

			assertNull(x.findRef("#/components/schemas/notfound", SchemaInfo.class));

			assertThrows(IllegalArgumentException.class, () -> x.findRef(null, SchemaInfo.class));
			assertThrows(IllegalArgumentException.class, () -> x.findRef("a", null));
			assertThrows(IllegalArgumentException.class, () -> x.findRef("", SchemaInfo.class));
			assertThrowsWithMessage(BasicRuntimeException.class, "Unsupported reference:  'invalid'", () -> x.findRef("invalid", SchemaInfo.class));
		}
	}

	//---------------------------------------------------------------------------------------------
	// Helper methods
	//---------------------------------------------------------------------------------------------

	private static OpenApi bean() {
		return openApi();
	}
}