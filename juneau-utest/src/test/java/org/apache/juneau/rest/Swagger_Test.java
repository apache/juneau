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
package org.apache.juneau.rest;

import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.common.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Tag;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.rest.staticfile.*;
import org.apache.juneau.rest.swagger.*;
import org.apache.juneau.xml.*;
import org.junit.jupiter.api.*;

class Swagger_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Setup
	//------------------------------------------------------------------------------------------------------------------

	public void testMethod() { /* no-op */ }

	private static org.apache.juneau.bean.swagger.Swagger getSwaggerWithFile(Object resource) throws Exception {
		var rc = RestContext.create(resource.getClass(),null,null).init(()->resource).defaultClasses(TestClasspathFileFinder.class).build();
		var roc = RestOpContext.create(Swagger_Test.class.getMethod("testMethod"), rc).build();
		var call = RestSession.create(rc).resource(resource).req(new MockServletRequest()).res(new MockServletResponse()).build();
		var req = roc.createRequest(call);
		var ip = rc.getSwaggerProvider();
		return ip.getSwagger(rc, req.getLocale());
	}

	private static org.apache.juneau.bean.swagger.Swagger getSwagger(Object resource) throws Exception {
		var rc = RestContext.create(resource.getClass(),null,null).init(()->resource).build();
		var roc = RestOpContext.create(Swagger_Test.class.getMethod("testMethod"), rc).build();
		var call = RestSession.create(rc).resource(resource).req(new MockServletRequest()).res(new MockServletResponse()).build();
		var req = roc.createRequest(call);
		var ip = rc.getSwaggerProvider();
		return ip.getSwagger(rc, req.getLocale());
	}

	public static class TestClasspathFileFinder extends BasicStaticFiles {

		public TestClasspathFileFinder() {
			super(StaticFiles.create(BeanStore.INSTANCE).cp(Swagger_Test.class, null, false));
		}

		@Override /* FileFinder */
		public Optional<InputStream> getStream(String name, Locale locale) throws IOException {
			if (name.endsWith(".json"))
				return opt(SwaggerProvider.class.getResourceAsStream("BasicRestInfoProviderTest_swagger.json"));
			return super.getStream(name, locale);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// /<root>
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A1 {}

	@Test void a01_swagger_default() throws Exception {
		assertBean(getSwagger(new A1()), "swagger,host,basePath,schemes", "2.0,<null>,<null>,<null>");
	}
	@Test void a01_swagger_default_withFile() throws Exception {
		assertBean(getSwaggerWithFile(new A1()), "swagger,host,basePath,schemes", "0.0,s-host,s-basePath,[s-scheme]");
	}

	@Rest(swagger=@Swagger("{swagger:'3.0',host:'a-host',basePath:'a-basePath',schemes:['a-scheme']}"))
	public static class A2 {}

	@Test void a02_swagger_Swagger_value() throws Exception {
		assertBean(getSwagger(new A2()), "swagger,host,basePath,schemes", "3.0,a-host,a-basePath,[a-scheme]");
	}
	@Test void a02_swagger_Swagger_value_withFile() throws Exception {
		assertBean(getSwaggerWithFile(new A2()), "swagger,host,basePath,schemes", "3.0,a-host,a-basePath,[a-scheme]");
	}

	//------------------------------------------------------------------------------------------------------------------
	// /info
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		title="a-title",
		description="a-description"
	)
	public static class B1 {}

	@Test void b01a_info_Rest() throws Exception {
		assertBean(
			getSwagger(new B1()).getInfo(),
			"title,description,version,termsOfService,contact,license",
			"a-title,a-description,<null>,<null>,<null>,<null>"
		);
	}

	@Test void b01b_info_Rest_withFile() throws Exception {
		assertBean(
			getSwaggerWithFile(new B1()).getInfo(),
			"title,description,version,termsOfService,contact{name,url,email},license{name,url}",
			"s-title,s-description,0.0.0,s-termsOfService,{s-name,s-url,s-email},{s-name,s-url}"
		);
	}

	@Rest(
		messages="BasicRestInfoProviderTest",
		title="$L{foo}",
		description="$L{foo}"
	)
	public static class B2 {}

	@Test void b02a_info_Rest_localized() throws Exception {
		assertBean(getSwagger(new B2()).getInfo(), "title,description", "l-foo,l-foo");
	}
	@Test void b02b_info_Rest_localized_withFile() throws Exception {
		assertBean(getSwaggerWithFile(new B2()).getInfo(), "title,description", "s-title,s-description");
	}

	@Rest(
		title="a-title",
		description="a-description",
		swagger=@Swagger(
			{
				"info:{",
					"title:'b-title',",
					"description:'b-description',",
					"version:'2.0.0',",
					"termsOfService:'a-termsOfService',",
					"contact:{name:'a-name',url:'a-url',email:'a-email'},",
					"license:{name:'a-name',url:'a-url'}",
				"}"
			}
		)
	)
	public static class B3 {}

	@Test void b03a_info_Swagger_value() throws Exception {
		assertBean(
			getSwagger(new B3()).getInfo(),
			"title,description,version,termsOfService,contact{name,url,email},license{name,url}",
			"b-title,b-description,2.0.0,a-termsOfService,{a-name,a-url,a-email},{a-name,a-url}"
		);
	}
	@Test void b03b_info_Swagger_value_withFile() throws Exception {
		assertBean(
			getSwaggerWithFile(new B3()).getInfo(),
			"title,description,version,termsOfService,contact{name,url,email},license{name,url}",
			"b-title,b-description,2.0.0,a-termsOfService,{a-name,a-url,a-email},{a-name,a-url}"
		);
	}

	@Rest(
		messages="BasicRestInfoProviderTest",
		title="a-title",
		description="a-description",
		swagger=@Swagger("{info:{title:'$L{bar}',description:'$L{bar}'}}")
	)
	public static class B4 {}

	@Test void b04_info_Swagger_value_localised() throws Exception {
		assertBean(getSwagger(new B4()), "info{title,description}", "{l-bar,l-bar}");
		assertBean(getSwaggerWithFile(new B4()), "info{title,description}", "{l-bar,l-bar}");
	}

	@Rest(
		title="a-title",
		description="a-description",
		swagger=@Swagger(
			value= {
				"info:{",
					"title:'b-title',",
					"description:'b-description',",
					"version:'2.0.0',",
					"termsOfService:'a-termsOfService',",
					"contact:{name:'a-name',url:'a-url',email:'a-email'},",
					"license:{name:'a-name',url:'a-url'}",
				"}"
			},
			title="c-title",
			description="c-description",
			version="3.0.0",
			termsOfService="b-termsOfService",
			contact=@Contact(name="b-name",url="b-url",email="b-email"),
			license=@License(name="b-name",url="b-url")
		)
	)
	public static class B5 {}

	@Test void b05a_info_Swagger_title() throws Exception {
		var x = getSwagger(new B5()).getInfo();
		assertBean(
			x,
			"title,description,version,termsOfService,contact{name,url,email},license{name,url}",
			"c-title,c-description,3.0.0,b-termsOfService,{b-name,b-url,b-email},{b-name,b-url}"
		);
	}
	@Test void b05b_info_Swagger_title_withFile() throws Exception {
		var x = getSwaggerWithFile(new B5()).getInfo();
		assertBean(
			x,
			"title,description,version,termsOfService,contact{name,url,email},license{name,url}",
			"c-title,c-description,3.0.0,b-termsOfService,{b-name,b-url,b-email},{b-name,b-url}"
		);
	}

	@Rest(
		title="a-title",
		description="a-description",
		swagger=@Swagger(
			value= {
				"info:{",
					"title:'b-title',",
					"description:'b-description',",
					"version:'2.0.0',",
					"termsOfService:'a-termsOfService',",
					"contact:{name:'a-name',url:'a-url',email:'a-email'},",
					"license:{name:'a-name',url:'a-url'}",
				"}"
			},
			title="$L{baz}",
			description="$L{baz}",
			version="$L{foo}",
			termsOfService="$L{foo}",
			contact=@Contact(name="$L{foo}",url="$L{bar}",email="$L{baz}"),
			license=@License(name="$L{foo}",url="$L{bar}")
		),
		messages="BasicRestInfoProviderTest"
	)
	public static class B6 {}

	@Test void b06a_info_Swagger_title_localized() throws Exception {
		assertBean(
			getSwagger(new B6()).getInfo(),
			"title,description,version,termsOfService,contact{name,url,email},license{name,url}",
			"l-baz,l-baz,l-foo,l-foo,{l-foo,l-bar,l-baz},{l-foo,l-bar}"
		);
	}
	@Test void b06b_info_Swagger_title_localized_withFile() throws Exception {
		assertBean(
			getSwaggerWithFile(new B6()).getInfo(),
			"title,description,version,termsOfService,contact{name,url,email},license{name,url}",
			"l-baz,l-baz,l-foo,l-foo,{l-foo,l-bar,l-baz},{l-foo,l-bar}"
		);
	}

	@Rest(
		swagger=@Swagger(
			title="c-title",
			description="c-description"
		)
	)
	public static class B07 {}

	@Test void b07a_title_Swagger_title_only() throws Exception {
		assertBean(getSwagger(new B07()).getInfo(), "title,description", "c-title,c-description");
	}
	@Test void b07b_title_Swagger_title_only_withFile() throws Exception {
		assertBean(getSwaggerWithFile(new B07()).getInfo(), "title,description", "c-title,c-description");
	}

	//------------------------------------------------------------------------------------------------------------------
	// /tags
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C1 {}

	@Test void c01a_tags_default() throws Exception {
		assertNull(getSwagger(new C1()).getTags());
	}
	@Test void c01b_tags_default_withFile() throws Exception {
		var x = getSwaggerWithFile(new C1());
		assertBeans(
			x.getTags(),
			"name,description,externalDocs{description,url}",
			"s-name,s-description,{s-description,s-url}"
		);
	}

	// Tags in @ResourceSwagger(value) should override file.
	@Rest(
		swagger=@Swagger(
			"{tags:[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}}]}"
		)
	)
	public static class C2 {}

	@Test void c02a_tags_Swagger_value() throws Exception {
		assertBeans(
			getSwagger(new C2()).getTags(),
			"name,description,externalDocs{description,url}",
			"a-name,a-description,{a-description,a-url}"
		);
	}
	@Test void c02b_tags_Swagger_value_withFile() throws Exception {
		assertBeans(
			getSwaggerWithFile(new C2()).getTags(),
			"name,description,externalDocs{description,url}",
			"a-name,a-description,{a-description,a-url}"
		);
	}

	// Tags in both @ResourceSwagger(value) and @ResourceSwagger(tags) should accumulate.
	@Rest(
		swagger=@Swagger(
			value="{tags:[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}}]}",
			tags=@Tag(name="b-name",description="b-description",externalDocs=@ExternalDocs(description="b-description",url="b-url"))
		)
	)
	public static class C3 {}

	@Test void c03a_tags_Swagger_tags() throws Exception {
		assertBeans(
			getSwagger(new C3()).getTags(),
			"name,description,externalDocs{description,url}",
			"a-name,a-description,{a-description,a-url}",
			"b-name,b-description,{b-description,b-url}"
		);
	}
	@Test void c03b_tags_Swagger_tags_withFile() throws Exception {
		assertBeans(
			getSwaggerWithFile(new C3()).getTags(),
			"name,description,externalDocs{description,url}",
			"a-name,a-description,{a-description,a-url}",
			"b-name,b-description,{b-description,b-url}"
		);
	}

	// Same as above but without [] outer characters.
	@Rest(
		swagger=@Swagger(
			value="{tags:[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}}]}",
			tags=@Tag(name="b-name",description="b-description", externalDocs=@ExternalDocs(description="b-description",url="b-url"))
		)
	)
	public static class C4 {}

	@Test void c04a_tags_Swagger_tags() throws Exception {
		assertBeans(
			getSwagger(new C4()).getTags(),
			"name,description,externalDocs{description,url}",
			"a-name,a-description,{a-description,a-url}",
			"b-name,b-description,{b-description,b-url}"
		);
	}
	@Test void c04b_tags_Swagger_tags_withFile() throws Exception {
		assertBeans(
			getSwaggerWithFile(new C4()).getTags(),
			"name,description,externalDocs{description,url}",
			"a-name,a-description,{a-description,a-url}",
			"b-name,b-description,{b-description,b-url}"
		);
	}

	// Tags in both Swagger.json and @ResourceSwagger(tags) should accumulate.
	@Rest(
		swagger=@Swagger(
			tags=@Tag(name="b-name",description="b-description",externalDocs=@ExternalDocs(description="b-description",url="b-url"))
		)
	)
	public static class C5 {}

	@Test void c05a_tags_Swagger_tags_only() throws Exception {
		assertBeans(
			getSwagger(new C5()).getTags(),
			"name,description,externalDocs{description,url}",
			"b-name,b-description,{b-description,b-url}"
		);
	}
	@Test void c05b_tags_Swagger_tags_only_witFile() throws Exception {
		assertBeans(
			getSwaggerWithFile(new C5()).getTags(),
			"name,description,externalDocs{description,url}",
			"s-name,s-description,{s-description,s-url}",
			"b-name,b-description,{b-description,b-url}"
		);
	}

	// Dup tag names should be overwritten
	@Rest(
		swagger=@Swagger(
			tags={
				@Tag(name="s-name",description="b-description",externalDocs=@ExternalDocs(description="b-description",url="b-url")),
				@Tag(name="s-name",description="c-description",externalDocs=@ExternalDocs(description="c-description",url="c-url"))
			}
		)
	)
	public static class C6 {}

	@Test void c06a_tags_Swagger_tags_dups() throws Exception {
		assertBeans(
			getSwagger(new C6()).getTags(),
			"name,description,externalDocs{description,url}",
			"s-name,c-description,{c-description,c-url}"
		);
	}
	@Test void c06b_tags_Swagger_tags_dups_withFile() throws Exception {
		assertBeans(
			getSwaggerWithFile(new C6()).getTags(),
			"name,description,externalDocs{description,url}",
			"s-name,c-description,{c-description,c-url}"
		);
	}

	@Rest(
		swagger=@Swagger(
			value="{tags:[{name:'$L{foo}',description:'$L{foo}',externalDocs:{description:'$L{foo}',url:'$L{foo}'}}]}",
			tags=@Tag(name="$L{foo}",description="$L{foo}",externalDocs=@ExternalDocs(description="$L{foo}",url="$L{foo}"))
		),
		messages="BasicRestInfoProviderTest"
	)
	public static class C7 {}

	@Test void c07a_tags_Swagger_tags_localised() throws Exception {
		assertBeans(
			getSwagger(new C7()).getTags(),
			"name,description,externalDocs{description,url}",
			"l-foo,l-foo,{l-foo,l-foo}"
		);
	}
	@Test void c07b_tags_Swagger_tags_localised_withFile() throws Exception {
		assertBeans(
			getSwaggerWithFile(new C7()).getTags(),
			"name,description,externalDocs{description,url}",
			"l-foo,l-foo,{l-foo,l-foo}"
		);
	}

	// Auto-detect tags defined on methods.
	@Rest
	public static class C8 {
		@RestOp(swagger=@OpSwagger(tags="foo"))
		public void a() { /* no-op */ }
	}

	@Test void c08a_tags_Swagger_tags_loose() throws Exception {
		assertBeans(getSwagger(new C8()).getTags(), "name", "foo");
	}
	@Test void c08b_tags_Swagger_tags_loose_withFile() throws Exception {
		assertBeans(
			getSwaggerWithFile(new C8()).getTags(),
			"name,description,externalDocs{description,url}",
			"s-name,s-description,{s-description,s-url}",
			"foo,<null>,<null>"
		);
	}

	// Comma-delimited list
	@Rest
	public static class C9 {
		@RestOp(swagger=@OpSwagger(tags=" foo, bar "))
		public void a() {/* no-op */}
	}

	@Test void c09a_tags_Swagger_tags_loose_cdl() throws Exception {
		assertBeans(getSwagger(new C9()).getTags(), "name", "foo", "bar");
	}
	@Test void c09b_tags_Swagger_tags_loose_cdl_withFile() throws Exception {
		assertBeans(
			getSwaggerWithFile(new C9()).getTags(),
			"name,description,externalDocs{description,url}",
			"s-name,s-description,{s-description,s-url}",
			"foo,<null>,<null>",
			"bar,<null>,<null>"
		);
	}

	// JsonList
	@Rest
	public static class C10 {
		@RestGet(swagger=@OpSwagger(tags="['foo', 'bar']"))
		public void a() {/* no-op */}
	}

	@Test void c10a_tags_Swagger_tags_loose_olist() throws Exception {
		assertBeans(getSwagger(new C10()).getTags(), "name", "foo", "bar");
	}
	@Test void c10b_tags_Swagger_tags_loose_olist_withFile() throws Exception {
		assertBeans(
			getSwaggerWithFile(new C10()).getTags(),
			"name,description,externalDocs{description,url}",
			"s-name,s-description,{s-description,s-url}",
			"foo,<null>,<null>",
			"bar,<null>,<null>"
		);
	}

	// JsonList localized
	@Rest(messages="BasicRestInfoProviderTest")
	public static class C11 {
		@RestGet(swagger=@OpSwagger(tags="['$L{foo}', '$L{bar}']"))
		public void a() {/* no-op */}
	}

	@Test void c11a_tags_Swagger_tags_loose_olist_localized() throws Exception {
		assertBeans(getSwagger(new C11()).getTags(), "name", "l-foo", "l-bar");
	}
	@Test void c11b_tags_Swagger_tags_loose_olist_localized_withFile() throws Exception {
		assertBeans(
			getSwaggerWithFile(new C11()).getTags(),
			"name,description,externalDocs{description,url}",
			"s-name,s-description,{s-description,s-url}",
			"l-foo,<null>,<null>",
			"l-bar,<null>,<null>"
		);
	}

	// Comma-delimited list localized
	@Rest(messages="BasicRestInfoProviderTest")
	public static class C12 {
		@RestGet(swagger=@OpSwagger(tags=" $L{foo}, $L{bar} "))
		public void a() {/* no-op */}
	}

	@Test void c12a_tags_Swagger_tags_loose_cdl_localized() throws Exception {
		assertBeans(getSwagger(new C12()).getTags(), "name", "l-foo", "l-bar");
	}
	@Test void c12b_tags_Swagger_tags_loose_cdl_localized_withFile() throws Exception {
		assertBeans(
			getSwaggerWithFile(new C12()).getTags(),
			"name,description,externalDocs{description,url}",
			"s-name,s-description,{s-description,s-url}",
			"l-foo,<null>,<null>",
			"l-bar,<null>,<null>"
		);
	}

	//------------------------------------------------------------------------------------------------------------------
	// /externalDocs
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D1 {}

	@Test void d01a_externalDocs_default() throws Exception {
		assertNull(getSwagger(new D1()).getExternalDocs());
	}
	@Test void d01b_externalDocs_default_withFile() throws Exception {
		assertBean(getSwaggerWithFile(new D1()).getExternalDocs(), "description,url", "s-description,s-url");
	}

	@Rest(
		swagger=@Swagger("{externalDocs:{description:'a-description',url:'a-url'}}")
	)
	public static class D2 {}

	@Test void d02a_externalDocs_Swagger_value() throws Exception {
		assertBean(getSwagger(new D2()).getExternalDocs(), "description,url", "a-description,a-url");
	}
	@Test void d02b_externalDocs_Swagger_value_withFile() throws Exception {
		assertBean(getSwaggerWithFile(new D2()).getExternalDocs(), "description,url", "a-description,a-url");
	}

	@Rest(
		swagger=@Swagger(
			value="{externalDocs:{description:'a-description',url:'a-url'}}",
			externalDocs=@ExternalDocs(description="b-description",url="b-url")
		)
	)
	public static class D3 {}

	@Test void d03a_externalDocs_Swagger_externalDocs() throws Exception {
		assertBean(getSwagger(new D3()).getExternalDocs(), "description,url", "b-description,b-url");
	}
	@Test void d03b_externalDocs_Swagger_externalDocs_withFile() throws Exception {
		assertBean(getSwaggerWithFile(new D3()).getExternalDocs(), "description,url", "b-description,b-url");
	}

	@Rest(
		swagger=@Swagger(
			value="{info:{externalDocs:{description:'a-description',url:'a-url'}}}",
			externalDocs=@ExternalDocs(description="b-description", url="b-url")
		)
	)
	public static class D4 {}

	@Test void d04a_externalDocs_Swagger_externalDocs() throws Exception {
		var x = getSwagger(new D4()).getExternalDocs();
		assertBean(x, "description,url", "b-description,b-url");
	}
	@Test void d04b_externalDocs_Swagger_externalDocs_withFile() throws Exception {
		var x = getSwaggerWithFile(new D4()).getExternalDocs();
		assertBean(x, "description,url", "b-description,b-url");
	}

	@Rest(
		swagger=@Swagger(
			value="{externalDocs:{description:'a-description',url:'a-url'}}",
			externalDocs=@ExternalDocs(description="$L{foo}",url="$L{bar}")
		),
		messages="BasicRestInfoProviderTest"
	)
	public static class D5 {}

	@Test void d05a_externalDocs_Swagger_externalDocs_localised() throws Exception {
		var x = getSwagger(new D5()).getExternalDocs();
		assertBean(x, "description,url", "l-foo,l-bar");
	}
	@Test void d05b_externalDocs_Swagger_externalDocs_localised_withFile() throws Exception {
		var x = getSwaggerWithFile(new D5()).getExternalDocs();
		assertBean(x, "description,url", "l-foo,l-bar");
	}

	//------------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class E1 {
		@RestGet(path="/path/{foo}")
		public X a() {
			return null;
		}
	}

	@Test void e01a_operation_summary_default() throws Exception {
		assertBean(getSwagger(new E1()).getPaths().get("/path/{foo}").get("get"),
			"operationId,summary,description,deprecated,schemes",
			"a,<null>,<null>,false,<null>");
	}

	@Test void e01b_operation_summary_default_withFile() throws Exception {
		assertBean(getSwaggerWithFile(new E1()).getPaths().get("/path/{foo}").get("get"), "operationId,summary,description,deprecated,schemes", "s-operationId,s-summary,s-description,true,[s-scheme]");
	}

	@Rest(
		swagger=@Swagger(
			"paths:{'/path/{foo}':{get:{operationId:'a-operationId',summary:'a-summary',description:'a-description',deprecated:false,schemes:['a-scheme']}}}"
		)
	)
	public static class E2 {
		@RestGet(path="/path/{foo}")
		public X a() {
			return null;
		}
	}

	@Test void e02a_operation_summary_swaggerOnClass() throws Exception {
		assertBean(getSwagger(new E2()).getPaths().get("/path/{foo}").get("get"), "operationId,summary,description,deprecated,schemes", "a-operationId,a-summary,a-description,false,[a-scheme]");
	}
	@Test void e02b_operation_summary_swaggerOnClass_withFile() throws Exception {
		assertBean(getSwaggerWithFile(new E2()).getPaths().get("/path/{foo}").get("get"), "operationId,summary,description,deprecated,schemes", "a-operationId,a-summary,a-description,false,[a-scheme]");
	}

	@Rest(
		swagger=@Swagger(
			"paths:{'/path/{foo}':{get:{operationId:'a-operationId',summary:'a-summary',description:'a-description',deprecated:false,schemes:['a-scheme']}}}"
		)
	)
	public static class E3 {
		@RestGet(path="/path/{foo}",
			swagger=@OpSwagger("operationId:'b-operationId',summary:'b-summary',description:'b-description',deprecated:false,schemes:['b-scheme']")
		)
		public X a() {
			return null;
		}
	}

	@Test void e03a_operation_summary_swaggerOnMethod() throws Exception {
		assertBean(getSwagger(new E3()).getPaths().get("/path/{foo}").get("get"), "operationId,summary,description,deprecated,schemes", "b-operationId,b-summary,b-description,false,[b-scheme]");
	}
	@Test void e03b_operation_summary_swaggerOnMethod_withFile() throws Exception {
		assertBean(getSwaggerWithFile(new E3()).getPaths().get("/path/{foo}").get("get"), "operationId,summary,description,deprecated,schemes", "b-operationId,b-summary,b-description,false,[b-scheme]");
	}

	@Rest(
		swagger=@Swagger(
			"paths:{'/path/{foo}':{get:{operationId:'a-operationId',summary:'a-summary',description:'a-description',deprecated:false,schemes:['a-scheme']}}}"
		)
	)
	public static class E4 {
		@RestGet(path="/path/{foo}",
			swagger=@OpSwagger(
				operationId="c-operationId",
				summary="c-summary",
				description="c-description",
				deprecated="false",
				schemes="d-scheme-1, d-scheme-2"
			)
		)
		public X a() {
			return null;
		}
	}

	@Test void e04a_operation_summary_swaggerOnAnnotation() throws Exception {
		assertBean(getSwagger(new E4()).getPaths().get("/path/{foo}").get("get"), "operationId,summary,description,schemes", "c-operationId,c-summary,c-description,[d-scheme-1,d-scheme-2]");
	}
	@Test void e04b_operation_summary_swaggerOnAnnotation_withFile() throws Exception {
		assertBean(getSwaggerWithFile(new E4()).getPaths().get("/path/{foo}").get("get"), "operationId,summary,description,schemes", "c-operationId,c-summary,c-description,[d-scheme-1,d-scheme-2]");
	}

	@Rest(
		messages="BasicRestInfoProviderTest",
		swagger=@Swagger(
			"paths:{'/path/{foo}':{get:{operationId:'a-operationId',summary:'a-summary',description:'a-description',deprecated:false,schemes:['a-scheme']}}}"
		)
	)
	public static class E5 {
		@RestGet(path="/path/{foo}",
			swagger=@OpSwagger(
				summary="$L{foo}",
				operationId="$L{foo}",
				description="$L{foo}",
				deprecated="$L{false}",
				schemes="$L{foo}"
			)
		)
		public X a() {
			return null;
		}
	}

	@Test void e05a_operation_summary_swaggerOnAnnotation_localized() throws Exception {
		assertBean(getSwagger(new E5()).getPaths().get("/path/{foo}").get("get"), "operationId,summary,description,deprecated,schemes", "l-foo,l-foo,l-foo,false,[l-foo]");
	}
	@Test void e05b_operation_summary_swaggerOnAnnotation_localized_withFile() throws Exception {
		assertBean(getSwaggerWithFile(new E5()).getPaths().get("/path/{foo}").get("get"), "operationId,summary,description,deprecated,schemes", "l-foo,l-foo,l-foo,false,[l-foo]");
	}

	@Rest(
		swagger=@Swagger(
			"paths:{'/path/{foo}':{get:{summary:'a-summary',description:'a-description'}}}"
		)
	)
	public static class E6 {
		@RestGet(path="/path/{foo}",
			summary="d-summary",
			description="d-description"
		)
		public X a() {
			return null;
		}
	}

	@Test void e06a_operation_summary_RestOp() throws Exception {
		assertBean(getSwagger(new E6()).getPaths().get("/path/{foo}").get("get"), "summary,description", "a-summary,a-description");
	}
	@Test void e06b_operation_summary_RestOp_withFile() throws Exception {
		assertBean(getSwaggerWithFile(new E6()).getPaths().get("/path/{foo}").get("get"), "summary,description", "a-summary,a-description");
	}

	@Rest(
		swagger=@Swagger(
			"paths:{'/path/{foo}':{get:{}}}"
		)
	)
	public static class E7 {
		@RestGet(path="/path/{foo}",
			summary="d-summary",
			description="d-description"
		)
		public X a() {
			return null;
		}
	}

	@Test void e07a_operation_summary_RestOp() throws Exception {
		assertBean(getSwagger(new E7()).getPaths().get("/path/{foo}").get("get"), "summary,description", "d-summary,d-description");
	}
	@Test void e07b_operation_summary_RestOp_withFile() throws Exception {
		assertBean(getSwaggerWithFile(new E7()).getPaths().get("/path/{foo}").get("get"), "summary,description", "d-summary,d-description");
	}

	//------------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/tags
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class F1 {

		@RestGet(path="/path/{foo}")
		public X a() {
			return null;
		}
	}

	@Test void f01_operation_tags_default() throws Exception {
		assertNull(getSwagger(new F1()).getPaths().get("/path/{foo}").get("get").getTags());
		assertList(getSwaggerWithFile(new F1()).getPaths().get("/path/{foo}").get("get").getTags(), "s-tag");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{tags:['a-tag']}}}"))
	public static class F2 {
		@RestGet(path="/path/{foo}")
		public X a() {
			return null;
		}
	}

	@Test void f02_operation_tags_swaggerOnClass() throws Exception {
		assertList(getSwagger(new F2()).getPaths().get("/path/{foo}").get("get").getTags(), "a-tag");
		assertList(getSwaggerWithFile(new F2()).getPaths().get("/path/{foo}").get("get").getTags(), "a-tag");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{tags:['a-tag']}}}"))
	public static class F3 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger("tags:['b-tag']"))
		public X a() {
			return null;
		}
	}

	@Test void f03_operation_tags_swaggerOnMethod() throws Exception {
		assertList(getSwagger(new F3()).getPaths().get("/path/{foo}").get("get").getTags(), "b-tag");
		assertList(getSwaggerWithFile(new F3()).getPaths().get("/path/{foo}").get("get").getTags(), "b-tag");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{tags:['a-tag']}}}"))
	public static class F4 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger(tags="['c-tag-1','c-tag-2']"))
		public X a() {
			return null;
		}
	}

	@Test void f04_operation_tags_swaggerOnAnnotation() throws Exception {
		assertList(getSwagger(new F4()).getPaths().get("/path/{foo}").get("get").getTags(), "c-tag-1", "c-tag-2");
		assertList(getSwaggerWithFile(new F4()).getPaths().get("/path/{foo}").get("get").getTags(), "c-tag-1", "c-tag-2");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{tags:['a-tag']}}}"))
	public static class F5 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger(tags="c-tag-1, c-tag-2"))
		public X a() {
			return null;
		}
	}

	@Test void f05_operation_tags_swaggerOnAnnotation() throws Exception {
		assertList(getSwagger(new F5()).getPaths().get("/path/{foo}").get("get").getTags(), "c-tag-1", "c-tag-2");
		assertList(getSwaggerWithFile(new F5()).getPaths().get("/path/{foo}").get("get").getTags(), "c-tag-1", "c-tag-2");
	}

	@Rest(messages="BasicRestInfoProviderTest", swagger=@Swagger("paths:{'/path/{foo}':{get:{tags:'a-tags'}}}"))
	public static class F6 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger(tags="$L{foo}"))
		public X a() {
			return null;
		}
	}

	@Test void f06_operation_tags_swaggerOnAnnotation_localized() throws Exception {
		assertList(getSwagger(new F6()).getPaths().get("/path/{foo}").get("get").getTags(), "l-foo");
		assertList(getSwaggerWithFile(new F6()).getPaths().get("/path/{foo}").get("get").getTags(), "l-foo");
	}

	//------------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/externalDocs
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class G1 {

		@RestGet(path="/path/{foo}")
		public X a() {
			return null;
		}
	}

	@Test void g01_operation_externalDocs_default() throws Exception {
		assertNull(getSwagger(new G1()).getPaths().get("/path/{foo}").get("get").getExternalDocs());
		assertBean(getSwaggerWithFile(new G1()).getPaths().get("/path/{foo}").get("get").getExternalDocs(), "description,url", "s-description,s-url");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{externalDocs:{description:'a-description',url:'a-url'}}}}"))
	public static class G2 {
		@RestGet(path="/path/{foo}")
		public X a() {
			return null;
		}
	}

	@Test void g02_operation_externalDocs_swaggerOnClass() throws Exception {
		assertBean(getSwagger(new G2()).getPaths().get("/path/{foo}").get("get").getExternalDocs(), "description,url", "a-description,a-url");
		assertBean(getSwaggerWithFile(new G2()).getPaths().get("/path/{foo}").get("get").getExternalDocs(), "description,url", "a-description,a-url");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{externalDocs:{description:'a-description',url:'a-url'}}}}"))
	public static class G3 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger("externalDocs:{description:'b-description',url:'b-url'}"))
		public X a() {
			return null;
		}
	}

	@Test void g03_operation_externalDocs_swaggerOnMethod() throws Exception {
		assertBean(getSwagger(new G3()).getPaths().get("/path/{foo}").get("get").getExternalDocs(), "description,url", "b-description,b-url");
		assertBean(getSwaggerWithFile(new G3()).getPaths().get("/path/{foo}").get("get").getExternalDocs(), "description,url", "b-description,b-url");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{externalDocs:{description:'a-description',url:'a-url'}}}}"))
	public static class G4 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger(externalDocs=@ExternalDocs(description="c-description",url="c-url")))
		public X a() {
			return null;
		}
	}

	@Test void g04_operation_externalDocs_swaggerOnAnnotation() throws Exception {
		assertBean(getSwagger(new G4()).getPaths().get("/path/{foo}").get("get").getExternalDocs(), "description,url", "c-description,c-url");
		assertBean(getSwaggerWithFile(new G4()).getPaths().get("/path/{foo}").get("get").getExternalDocs(), "description,url", "c-description,c-url");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{externalDocs:{description:'a-description',url:'a-url'}}}}"))
	public static class G5 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger(externalDocs=@ExternalDocs(description="d-description",url="d-url")))
		public X a() {
			return null;
		}
	}

	@Test void g05_operation_externalDocs_swaggerOnAnnotation() throws Exception {
		assertBean(getSwagger(new G5()).getPaths().get("/path/{foo}").get("get").getExternalDocs(), "description,url", "d-description,d-url");
		assertBean(getSwaggerWithFile(new G5()).getPaths().get("/path/{foo}").get("get").getExternalDocs(), "description,url", "d-description,d-url");
	}

	@Rest(messages="BasicRestInfoProviderTest", swagger=@Swagger("paths:{'/path/{foo}':{get:{externalDocs:{description:'a-description',url:'a-url'}}}}"))
	public static class G6 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger(externalDocs=@ExternalDocs(description="$L{foo}",url="$L{foo}")))
		public X a() {
			return null;
		}
	}

	@Test void g06_operation_externalDocs_swaggerOnAnnotation_localized() throws Exception {
		assertBean(getSwagger(new G6()).getPaths().get("/path/{foo}").get("get").getExternalDocs(), "description,url", "l-foo,l-foo");
		assertBean(getSwaggerWithFile(new G6()).getPaths().get("/path/{foo}").get("get").getExternalDocs(), "description,url", "l-foo,l-foo");
	}

	//------------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/consumes
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class H1 {

		@RestGet(path="/path/{foo}")
		public X a() {
			return null;
		}
	}

	@Test void h01_operation_consumes_default() throws Exception {
		assertNull(getSwagger(new H1()).getPaths().get("/path/{foo}").get("get").getConsumes());
		assertList(getSwaggerWithFile(new H1()).getPaths().get("/path/{foo}").get("get").getConsumes(), "s-consumes");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{consumes:['a-consumes']}}}"))
	public static class H2 {
		@RestGet(path="/path/{foo}")
		public X a() {
			return null;
		}
	}

	@Test void h02_operation_consumes_swaggerOnClass() throws Exception {
		assertList(getSwagger(new H2()).getPaths().get("/path/{foo}").get("get").getConsumes(), "a-consumes");
		assertList(getSwaggerWithFile(new H2()).getPaths().get("/path/{foo}").get("get").getConsumes(), "a-consumes");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{consumes:['a-consumes']}}}"))
	public static class H3 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger("consumes:['b-consumes']"))
		public X a() {
			return null;
		}
	}

	@Test void h03_operation_consumes_swaggerOnMethod() throws Exception {
		assertList(getSwagger(new H3()).getPaths().get("/path/{foo}").get("get").getConsumes(), "b-consumes");
		assertList(getSwaggerWithFile(new H3()).getPaths().get("/path/{foo}").get("get").getConsumes(), "b-consumes");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{consumes:['a-consumes']}}}"))
	public static class H4 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger(consumes="['c-consumes-1','c-consumes-2']"))
		public X a() {
			return null;
		}
	}

	@Test void h04_operation_consumes_swaggerOnAnnotation() throws Exception {
		assertList(getSwagger(new H4()).getPaths().get("/path/{foo}").get("get").getConsumes(), "c-consumes-1", "c-consumes-2");
		assertList(getSwaggerWithFile(new H4()).getPaths().get("/path/{foo}").get("get").getConsumes(), "c-consumes-1", "c-consumes-2");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{consumes:['a-consumes']}}}"))
	public static class H5 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger(consumes="c-consumes-1, c-consumes-2"))
		public X a() {
			return null;
		}
	}

	@Test void h05_operation_consumes_swaggerOnAnnotation() throws Exception {
		assertList(getSwagger(new H5()).getPaths().get("/path/{foo}").get("get").getConsumes(), "c-consumes-1", "c-consumes-2");
		assertList(getSwaggerWithFile(new H5()).getPaths().get("/path/{foo}").get("get").getConsumes(), "c-consumes-1", "c-consumes-2");
	}

	@Rest(messages="BasicRestInfoProviderTest", swagger=@Swagger("paths:{'/path/{foo}':{get:{consumes:['a-consumes']}}}"))
	public static class H6 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger(consumes="['$L{foo}']"))
		public X a() {
			return null;
		}
	}

	@Test void h06_operation_consumes_swaggerOnAnnotation_localized() throws Exception {
		assertList(getSwagger(new H6()).getPaths().get("/path/{foo}").get("get").getConsumes(), "l-foo");
		assertList(getSwaggerWithFile(new H6()).getPaths().get("/path/{foo}").get("get").getConsumes(), "l-foo");
	}

	@Rest(parsers={JsonParser.class})
	public static class H7 {
		@RestPut(path="/path2/{foo}")
		public X a() {
			return null;
		}
	}

	@Test void h07_operation_consumes_parsersOnClass() throws Exception {
		assertNull(getSwagger(new H7()).getPaths().get("/path2/{foo}").get("put").getConsumes());
		assertNull(getSwaggerWithFile(new H7()).getPaths().get("/path2/{foo}").get("put").getConsumes());
	}

	@Rest(parsers={JsonParser.class})
	public static class H8 {
		@RestPut(path="/path2/{foo}",parsers={XmlParser.class})
		public X a() {
			return null;
		}
		@RestPut(path="/b")
		public X b() {
			return null;
		}
	}

	@Test void h08_operation_consumes_parsersOnClassAndMethod() throws Exception {
		assertList(getSwagger(new H8()).getPaths().get("/path2/{foo}").get("put").getConsumes(), "text/xml", "application/xml");
		assertList(getSwaggerWithFile(new H8()).getPaths().get("/path2/{foo}").get("put").getConsumes(), "text/xml", "application/xml");
	}

	@Rest(parsers={JsonParser.class},swagger=@Swagger("paths:{'/path2/{foo}':{put:{consumes:['a-consumes']}}}"))
	public static class H9 {
		@RestPut(path="/path2/{foo}",parsers={XmlParser.class})
		public X a() {
			return null;
		}
	}

	@Test void h09_operation_consumes_parsersOnClassAndMethodWithSwagger() throws Exception {
		assertList(getSwagger(new H9()).getPaths().get("/path2/{foo}").get("put").getConsumes(), "a-consumes");
		assertList(getSwaggerWithFile(new H9()).getPaths().get("/path2/{foo}").get("put").getConsumes(), "a-consumes");
	}

	//------------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/produces
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class I1 {

		@RestGet(path="/path/{foo}")
		public X a() {
			return null;
		}
	}

	@Test void i01_operation_produces_default() throws Exception {
		assertNull(getSwagger(new I1()).getPaths().get("/path/{foo}").get("get").getProduces());
		assertList(getSwaggerWithFile(new I1()).getPaths().get("/path/{foo}").get("get").getProduces(), "s-produces");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{produces:['a-produces']}}}"))
	public static class I2 {
		@RestGet(path="/path/{foo}")
		public X a() {
			return null;
		}
	}

	@Test void i02_operation_produces_swaggerOnClass() throws Exception {
		assertList(getSwagger(new I2()).getPaths().get("/path/{foo}").get("get").getProduces(), "a-produces");
		assertList(getSwaggerWithFile(new I2()).getPaths().get("/path/{foo}").get("get").getProduces(), "a-produces");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{produces:['a-produces']}}}"))
	public static class I3 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger("produces:['b-produces']"))
		public X a() {
			return null;
		}
	}

	@Test void i03_operation_produces_swaggerOnMethod() throws Exception {
		assertList(getSwagger(new I3()).getPaths().get("/path/{foo}").get("get").getProduces(), "b-produces");
		assertList(getSwaggerWithFile(new I3()).getPaths().get("/path/{foo}").get("get").getProduces(), "b-produces");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{produces:['a-produces']}}}"))
	public static class I4 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger(produces="['c-produces-1','c-produces-2']"))
		public X a() {
			return null;
		}
	}

	@Test void i04_operation_produces_swaggerOnAnnotation() throws Exception {
		assertList(getSwagger(new I4()).getPaths().get("/path/{foo}").get("get").getProduces(), "c-produces-1", "c-produces-2");
		assertList(getSwaggerWithFile(new I4()).getPaths().get("/path/{foo}").get("get").getProduces(), "c-produces-1", "c-produces-2");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{produces:['a-produces']}}}"))
	public static class I5 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger(produces="c-produces-1, c-produces-2"))
		public X a() {
			return null;
		}
	}

	@Test void i05_operation_produces_swaggerOnAnnotation() throws Exception {
		assertList(getSwagger(new I5()).getPaths().get("/path/{foo}").get("get").getProduces(), "c-produces-1", "c-produces-2");
		assertList(getSwaggerWithFile(new I5()).getPaths().get("/path/{foo}").get("get").getProduces(), "c-produces-1", "c-produces-2");
	}

	@Rest(messages="BasicRestInfoProviderTest", swagger=@Swagger("paths:{'/path/{foo}':{get:{produces:['a-produces']}}}"))
	public static class I6 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger(produces="['$L{foo}']"))
		public X a() {
			return null;
		}
	}

	@Test void i06_operation_produces_swaggerOnAnnotation_localized() throws Exception {
		assertList(getSwagger(new I6()).getPaths().get("/path/{foo}").get("get").getProduces(), "l-foo");
		assertList(getSwaggerWithFile(new I6()).getPaths().get("/path/{foo}").get("get").getProduces(), "l-foo");
	}

	@Rest(serializers={JsonSerializer.class})
	public static class I7 {
		@RestPut(path="/path2/{foo}")
		public X a() {
			return null;
		}
	}

	@Test void i07_operation_produces_serializersOnClass() throws Exception {
		assertNull(getSwagger(new I7()).getPaths().get("/path2/{foo}").get("put").getProduces());
		assertNull(getSwaggerWithFile(new I7()).getPaths().get("/path2/{foo}").get("put").getProduces());
	}

	@Rest(serializers={JsonSerializer.class})
	public static class I8 {
		@RestPut(path="/path2/{foo}",serializers={XmlSerializer.class})
		public X a() {
			return null;
		}
		@RestGet(path="/b")
		public X b() {
			return null;
		}
	}

	@Test void i08_operation_produces_serializersOnClassAndMethod() throws Exception {
		assertList(getSwagger(new I8()).getPaths().get("/path2/{foo}").get("put").getProduces(), "text/xml");
		assertList(getSwaggerWithFile(new I8()).getPaths().get("/path2/{foo}").get("put").getProduces(), "text/xml");
	}

	@Rest(serializers={JsonSerializer.class},swagger=@Swagger("paths:{'/path2/{foo}':{put:{produces:['a-produces']}}}"))
	public static class I9 {
		@RestPut(path="/path2/{foo}",serializers={XmlSerializer.class})
		public X a() {
			return null;
		}
	}

	@Test void i09_operation_produces_serializersOnClassAndMethodWithSwagger() throws Exception {
		assertList(getSwagger(new I9()).getPaths().get("/path2/{foo}").get("put").getProduces(), "a-produces");
		assertList(getSwaggerWithFile(new I9()).getPaths().get("/path2/{foo}").get("put").getProduces(), "a-produces");
	}

	//------------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/deprecated
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class J1 {
		@RestGet(path="/path2/{foo}")
		@Deprecated
		public X a() {
			return null;
		}
	}

	@Test void j01_operation_deprecated_Deprecated() throws Exception {
		assertTrue(getSwagger(new J1()).getPaths().get("/path2/{foo}").get("get").getDeprecated());
		assertTrue(getSwaggerWithFile(new J1()).getPaths().get("/path2/{foo}").get("get").getDeprecated());
	}

	@Rest
	@Deprecated
	public static class J2 {
		@RestGet(path="/path2/{foo}")
		public X a() {
			return null;
		}
	}

	@Test void j02_operation_deprecated_Deprecated() throws Exception {
		assertTrue(getSwagger(new J2()).getPaths().get("/path2/{foo}").get("get").getDeprecated());  // NOSONAR
		assertTrue(getSwaggerWithFile(new J2()).getPaths().get("/path2/{foo}").get("get").getDeprecated());  // NOSONAR
	}

	//------------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/parameters/query
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class K1 {
		@RestGet(path="/path/{foo}/query")
		public X a(@Query("foo") X foo) {
			return null;
		}
	}

	@Test void k01a_query_type_default() throws Exception {
		assertBean(
			getSwagger(new K1()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo"),
			"type,description,required,allowEmptyValue,exclusiveMaximum,exclusiveMinimum,uniqueItems,format,collectionFormat,pattern,maximum,minimum,multipleOf,maxLength,minLength,maxItems,minItems",
			"object,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>"
		);
	}
	@Test void k01b_query_type_default_withFile() throws Exception {
		assertBean(
			getSwaggerWithFile(new K1()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo"),
			"type,description,required,allowEmptyValue,exclusiveMaximum,exclusiveMinimum,uniqueItems,format,collectionFormat,pattern,maximum,minimum,multipleOf,maxLength,minLength,maxItems,minItems",
			"string,s-description,true,true,true,true,true,s-format,s-collectionFormat,s-pattern,1.0,1.0,1.0,1,1,1,1"
		);
	}

	@Rest(
		swagger=@Swagger({
			"paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',",
				"name:'foo',",
				"type:'int32',",
				"description:'a-description',",
				"required:false,",
				"allowEmptyValue:false,",
				"exclusiveMaximum:false,",
				"exclusiveMinimum:false,",
				"uniqueItems:false,",
				"format:'a-format',",
				"collectionFormat:'a-collectionFormat',",
				"pattern:'a-pattern',",
				"maximum:2.0,",
				"minimum:2.0,",
				"multipleOf:2.0,",
				"maxLength:2,",
				"minLength:2,",
				"maxItems:2,",
				"minItems:2",
			"}]}}}"
		})
	)
	public static class K2 {
		@RestGet(path="/path/{foo}/query")
		public X a(@Query("foo") X foo) {
			return null;
		}
	}

	@Test void k02a_query_type_swaggerOnClass() throws Exception {
		assertBean(
			getSwagger(new K2()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo"),
			"type,description,required,allowEmptyValue,exclusiveMaximum,exclusiveMinimum,uniqueItems,format,collectionFormat,pattern,maximum,minimum,multipleOf,maxLength,minLength,maxItems,minItems",
			"int32,a-description,false,false,false,false,false,a-format,a-collectionFormat,a-pattern,2.0,2.0,2.0,2,2,2,2"
		);
	}
	@Test void k02b_query_type_swaggerOnClass_withFile() throws Exception {
		assertBean(
			getSwaggerWithFile(new K2()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo"),
			"type,description,required,allowEmptyValue,exclusiveMaximum,exclusiveMinimum,uniqueItems,format,collectionFormat,pattern,maximum,minimum,multipleOf,maxLength,minLength,maxItems,minItems",
			"int32,a-description,false,false,false,false,false,a-format,a-collectionFormat,a-pattern,2.0,2.0,2.0,2,2,2,2"
		);
	}

	@Rest(
		swagger=@Swagger({
			"paths:{'/path/{foo}/query':{get:{parameters:[{",
				"'in':'query',",
				"name:'foo',",
				"type:'int32',",
				"description:'a-description',",
				"required:false,",
				"allowEmptyValue:false,",
				"exclusiveMaximum:false,",
				"exclusiveMinimum:false,",
				"uniqueItems:false,",
				"format:'a-format',",
				"collectionFormat:'a-collectionFormat',",
				"pattern:'a-pattern',",
				"maximum:2.0,",
				"minimum:2.0,",
				"multipleOf:2.0,",
				"maxLength:2,",
				"minLength:2,",
				"maxItems:2,",
				"minItems:2",
			"}]}}}"
		})
	)
	public static class K3 {
		@RestGet(path="/path/{foo}/query",
			swagger=@OpSwagger({
				"parameters:[{",
					"'in':'query',",
					"name:'foo',",
					"type:'int64',",
					"description:'b-description',",
					"required:'true',",
					"allowEmptyValue:'true',",
					"exclusiveMaximum:'true',",
					"exclusiveMinimum:'true',",
					"uniqueItems:'true',",
					"format:'b-format',",
					"collectionFormat:'b-collectionFormat',",
					"pattern:'b-pattern',",
					"maximum:3.0,",
					"minimum:3.0,",
					"multipleOf:3.0,",
					"maxLength:3,",
					"minLength:3,",
					"maxItems:3,",
					"minItems:3",
				"}]"
			}))
		public X a() {
			return null;
		}
	}

	@Test void k03a_query_type_swaggerOnMethod() throws Exception {
		assertBean(
			getSwagger(new K3()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo"),
			"type,description,required,allowEmptyValue,exclusiveMaximum,exclusiveMinimum,uniqueItems,format,collectionFormat,pattern,maximum,minimum,multipleOf,maxLength,minLength,maxItems,minItems",
			"int64,b-description,true,true,true,true,true,b-format,b-collectionFormat,b-pattern,3.0,3.0,3.0,3,3,3,3"
		);
	}
	@Test void k03b_query_type_swaggerOnMethod_withFile() throws Exception {
		assertBean(
			getSwaggerWithFile(new K3()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo"),
			"type,description,required,allowEmptyValue,exclusiveMaximum,exclusiveMinimum,uniqueItems,format,collectionFormat,pattern,maximum,minimum,multipleOf,maxLength,minLength,maxItems,minItems",
			"int64,b-description,true,true,true,true,true,b-format,b-collectionFormat,b-pattern,3.0,3.0,3.0,3,3,3,3"
		);
	}

	//------------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/parameters/body/examples
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class M1 {
		@RestGet(path="/path/{foo}/body")
		public X a(@Content X foo) {
			return null;
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/parameters/query/schema
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class N1 {
		@RestGet(path="/path/{foo}/query")
		public X a(@Query("foo") X foo) {
			return null;
		}
	}

	@Test void n01_query_schema_default() throws Exception {
		assertBean(
			getSwagger(new N1()).getPaths().get("/path/{foo}/query").get("get").getParameter("query","foo").getSchema(),
			"properties{a{format,type}}",
			"{{int32,integer}}"
		);
		assertBean(
			getSwaggerWithFile(new N1()).getPaths().get("/path/{foo}/query").get("get").getParameter("query","foo").getSchema(),
			"ref",
			"#/definitions/Foo"
		);
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}/query':{get:{parameters:[{in:'query',name:'foo',schema:{$ref:'b'}}]}}}"))
	public static class N2 {
		@RestGet(path="/path/{foo}/query")
		public X a(@Query("foo") X foo) {
			return null;
		}
	}

	@Test void n02_query_schema_swaggerOnClass() throws Exception {
		assertBean(getSwagger(new N2()).getPaths().get("/path/{foo}/query").get("get").getParameter("query","foo").getSchema(), "ref", "b");
		assertBean(getSwaggerWithFile(new N2()).getPaths().get("/path/{foo}/query").get("get").getParameter("query","foo").getSchema(), "ref", "b");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}/query':{get:{parameters:[{in:'query',name:'foo',schema:{$ref:'b'}}]}}}"))
	public static class N3 {

		@RestGet(path="/path/{foo}/query",swagger=@OpSwagger("parameters:[{'in':'query',name:'foo',schema:{$ref:'c'}}]"))
		public X a() {
			return null;
		}
	}

	@Test void n03_query_schema_swaggerOnMethnt() throws Exception {
		assertBean(getSwagger(new N3()).getPaths().get("/path/{foo}/query").get("get").getParameter("query","foo").getSchema(), "ref", "c");
		assertBean(getSwaggerWithFile(new N3()).getPaths().get("/path/{foo}/query").get("get").getParameter("query","foo").getSchema(), "ref", "c");
	}

	//------------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/responses/<response>/description
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class O1a {
		@RestGet(path="/path/{foo}/responses/100")
		public void a(Value<O1c> foo) {/* no-op */}
	}
	@Rest
	public static class O1b {
		@RestGet(path="/path/{foo}/responses/100")
		public O1c a() { return null;}
	}
	@Response @StatusCode(100)
	public static class O1c {
		public String a;
	}

	@Test void o01a_responses_100_description_default() throws Exception {
		assertBean(getSwagger(new O1a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100), "description", "Continue");
		assertBean(getSwaggerWithFile(new O1a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100), "description", "s-100-description");
	}
	@Test void o01b_responses_100_description_default() throws Exception {
		assertBean(getSwagger(new O1b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100), "description", "Continue");
		assertBean(getSwaggerWithFile(new O1b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100), "description", "s-100-description");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{description:'a-100-description'}}}}}"))
	public static class O2 {
		@RestGet(path="/path/{foo}/responses/100")
		public void a(@StatusCode Value<Integer> foo) {/* no-op */}
	}

	@Test void o02_response_100_description_swaggerOnClass() throws Exception {
		assertBean(getSwagger(new O2()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100), "description", "a-100-description");
		assertBean(getSwaggerWithFile(new O2()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100), "description", "a-100-description");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{description:'a-100-description'}}}}}"))
	public static class O3 {
		@RestGet(path="/path/{foo}/responses/100",swagger=@OpSwagger("responses:{100:{description:'b-100-description'}}"))
		public void a(@StatusCode Value<Integer> foo) {/* no-op */}
	}

	@Test void o03_response_100_description_swaggerOnMethod() throws Exception {
		assertBean(getSwagger(new O3()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100), "description", "b-100-description");
		assertBean(getSwaggerWithFile(new O3()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100), "description", "b-100-description");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{description:'a-100-description'}}}}}"))
	public static class O4a {
		@RestGet(path="/path/{foo}/responses/100")
		public void a(Value<O4c> foo) {/* no-op */}
	}
	@Rest(swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{description:'a-100-description'}}}}}"))
	public static class O4b {
		@RestGet(path="/path/{foo}/responses/100")
		public O4c a() {return null;}
	}
	@Response @StatusCode(100) @Schema(description="c-100-description")
	public static class O4c {}

	@Test void o04a_response_100_description_swaggerOnAnnotation() throws Exception {
		assertBean(getSwagger(new O4a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100), "description", "c-100-description");
		assertBean(getSwaggerWithFile(new O4a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100), "description", "c-100-description");
	}
	@Test void o04b_response_100_description_swaggerOnAnnotation() throws Exception {
		assertBean(getSwagger(new O4b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100), "description", "c-100-description");
		assertBean(getSwaggerWithFile(new O4b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100), "description", "c-100-description");
	}

	@Rest(messages="BasicRestInfoProviderTest", swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{description:'a-100-description'}}}}}"))
	public static class O5a {
		@RestGet(path="/path/{foo}/responses/100")
		public void a(Value<O5c> foo) {/* no-op */}
	}
	@Rest(messages="BasicRestInfoProviderTest", swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{description:'a-100-description'}}}}}"))
	public static class O5b {
		@RestGet(path="/path/{foo}/responses/100")
		public O5c a() {return null;}
	}
	@Response @StatusCode(100) @Schema(description="$L{foo}")
	public static class O5c {}

	@Test void o05a_response_100_description_swaggerOnAnnotation_localized() throws Exception {
		assertBean(getSwagger(new O5a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100), "description", "l-foo");
		assertBean(getSwaggerWithFile(new O5a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100), "description", "l-foo");
	}
	@Test void o05b_response_100_description_swaggerOnAnnotation_localized() throws Exception {
		assertBean(getSwagger(new O5b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100), "description", "l-foo");
		assertBean(getSwaggerWithFile(new O5b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100), "description", "l-foo");
	}

	//------------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/responses/<response>/headers
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class P1a {
		@RestGet(path="/path/{foo}/responses/100")
		public void a(Value<P1c> foo) {/* no-op */}
	}
	@Rest
	public static class P1b {
		@RestGet(path="/path/{foo}/responses/100")
		public P1c a() {return null;}
	}
	@Response @StatusCode(100)
	public static class P1c {
		public String a;
	}

	@Test void p01a_responses_100_headers_default() throws Exception {
		assertNull(getSwagger(new P1a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
		assertBean(getSwaggerWithFile(new P1a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "X-Foo{description,type,format}", "{s-description,integer,int32}");
	}
	@Test void p01b_responses_100_headers_default() throws Exception {
		assertNull(getSwagger(new P1b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
		assertBean(getSwaggerWithFile(new P1b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "X-Foo{description,type,format}", "{s-description,integer,int32}");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{headers:{'X-Foo':{description:'b-description',type:'integer',format:'int32'}}}}}}}"))
	public static class P2 {
		@RestGet(path="/path/{foo}/responses/100")
		public X a(@StatusCode Value<Integer> foo) {
			return null;
		}
	}

	@Test void p02_response_100_headers_swaggerOnClass() throws Exception {
		assertBean(getSwagger(new P2()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "X-Foo{description,type,format}", "{b-description,integer,int32}");
		assertBean(getSwaggerWithFile(new P2()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "X-Foo{description,type,format}", "{b-description,integer,int32}");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{headers:{'X-Foo':{description:'b-description',type:'integer',format:'int32'}}}}}}}"))
	public static class P3 {
		@RestGet(path="/path/{foo}/responses/100",swagger=@OpSwagger("responses:{100:{headers:{'X-Foo':{description:'c-description',type:'integer',format:'int32'}}}}"))
		public X a(@StatusCode Value<Integer> foo) {
			return null;
		}
	}

	@Test void p03_response_100_headers_swaggerOnMethod() throws Exception {
		assertBean(getSwagger(new P3()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "X-Foo{description,type,format}", "{c-description,integer,int32}");
		assertBean(getSwaggerWithFile(new P3()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "X-Foo{description,type,format}", "{c-description,integer,int32}");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{headers:{'X-Foo':{description:'b-description',type:'integer',format:'int32'}}}}}}}"))
	public static class P4a {
		@RestGet(path="/path/{foo}/responses/100")
		public void a(Value<P4c> foo) {/* no-op */}
	}
	@Rest(swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{headers:{'X-Foo':{description:'b-description',type:'integer',format:'int32'}}}}}}}"))
	public static class P4b {
		@RestGet(path="/path/{foo}/responses/100")
		public P4c a() {return null;}
	}
	@Response(headers=@Header(name="X-Foo",schema=@Schema(description="d-description",type="integer",format="int32"))) @StatusCode(100)
	public static class P4c {}

	@Test void p04a_response_100_headers_swaggerOnAnnotation() throws Exception {
		assertBean(getSwagger(new P4a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "X-Foo{description,type,format}", "{d-description,integer,int32}");
		assertBean(getSwaggerWithFile(new P4a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "X-Foo{description,type,format}", "{d-description,integer,int32}");
	}
	@Test void p04b_response_100_headers_swaggerOnAnnotation() throws Exception {
		assertBean(getSwagger(new P4b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "X-Foo{description,type,format}", "{d-description,integer,int32}");
		assertBean(getSwaggerWithFile(new P4b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "X-Foo{description,type,format}", "{d-description,integer,int32}");
	}

	@Rest(messages="BasicRestInfoProviderTest", swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{headers:{'X-Foo':{description:'b-description',type:'integer',format:'int32'}}}}}}}"))
	public static class P5a {
		@RestGet(path="/path/{foo}/responses/100")
		public void a(Value<P5c> foo) {/* no-op */}
	}
	@Rest(messages="BasicRestInfoProviderTest", swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{headers:{'X-Foo':{description:'b-description',type:'integer',format:'int32'}}}}}}}"))
	public static class P5b {
		@RestGet(path="/path/{foo}/responses/100")
		public P5c a() {return null;}
	}
	@Response(headers=@Header(name="X-Foo",schema=@Schema(description="$L{foo}",type="integer",format="int32"))) @StatusCode(100)
	public static class P5c {}

	@Test void p05a_response_100_headers_swaggerOnAnnotation_localized() throws Exception {
		assertBean(getSwagger(new P5a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "X-Foo{description,type,format}", "{l-foo,integer,int32}");
		assertBean(getSwaggerWithFile(new P5a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "X-Foo{description,type,format}", "{l-foo,integer,int32}");
	}
	@Test void p05b_response_100_headers_swaggerOnAnnotation_localized() throws Exception {
		assertBean(getSwagger(new P5b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "X-Foo{description,type,format}", "{l-foo,integer,int32}");
		assertBean(getSwaggerWithFile(new P5b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "X-Foo{description,type,format}", "{l-foo,integer,int32}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/responses/<response>/examples
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class R1a {
		@RestGet(path="/path/{foo}/responses/100")
		public void a(Value<R1c> foo) {/* no-op */}
	}
	@Rest
	public static class R1b {
		@RestGet(path="/path/{foo}/responses/100")
		public R1c a() {return null;}
	}
	@Response @StatusCode(100)
	public static class R1c {
		public String a;
	}

	@Test void r01a_responses_100_examples_default() throws Exception {
		assertNull(getSwagger(new R1a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
		assertBean(getSwaggerWithFile(new R1a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "foo", "a");
	}
	@Test void r01b_responses_100_examples_default() throws Exception {
		assertNull(getSwagger(new R1b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
		assertBean(getSwaggerWithFile(new R1b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "foo", "a");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{examples:{foo:{bar:'b'}}}}}}}"))
	public static class R2 {
		@RestGet(path="/path/{foo}/responses/100")
		public void a(@StatusCode Value<Integer> foo) {/* no-op */}
	}

	@Test void r02_response_100_examples_swaggerOnClass() throws Exception {
		assertBean(getSwagger(new R2()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "foo{bar}", "{b}");
		assertBean(getSwaggerWithFile(new R2()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "foo{bar}", "{b}");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{examples:{foo:{bar:'b'}}}}}}}"))
	public static class R3 {
		@RestGet(path="/path/{foo}/responses/100",swagger=@OpSwagger("responses:{100:{examples:{foo:{bar:'c'}}}}"))
		public void a(@StatusCode Value<Integer> foo) {/* no-op */}
	}

	@Test void r03_response_100_examples_swaggerOnMethod() throws Exception {
		assertBean(getSwagger(new R3()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "foo{bar}", "{c}");
		assertBean(getSwaggerWithFile(new R3()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "foo{bar}", "{c}");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{examples:{foo:{bar:'b'}}}}}}}"))
	public static class R4a {
		@RestGet(path="/path/{foo}/responses/100")
		public void a(Value<R4c> foo) {/* no-op */}
	}
	@Rest(swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{examples:{foo:{bar:'b'}}}}}}}"))
	public static class R4b {
		@RestGet(path="/path/{foo}/responses/100")
		public R4c a() {return null;}
	}
	@Response(examples="{foo:{bar:'d'}}") @StatusCode(100)
	public static class R4c {}

	@Test void r04a_response_100_examples_swaggerOnAnnotation() throws Exception {
		assertBean(getSwagger(new R4a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "foo{bar}", "{d}");
		assertBean(getSwaggerWithFile(new R4a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "foo{bar}", "{d}");
	}
	@Test void r04b_response_100_examples_swaggerOnAnnotation() throws Exception {
		assertBean(getSwagger(new R4b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "foo{bar}", "{d}");
		assertBean(getSwaggerWithFile(new R4b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "foo{bar}", "{d}");
	}

	@Rest(messages="BasicRestInfoProviderTest", swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{examples:{foo:{bar:'b'}}}}}}}"))
	public static class R5a {
		@RestGet(path="/path/{foo}/responses/100")
		public void a(Value<R5c> foo) {/* no-op */}
	}
	@Rest(messages="BasicRestInfoProviderTest", swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{examples:{foo:{bar:'b'}}}}}}}"))
	public static class R5b {
		@RestGet(path="/path/{foo}/responses/100")
		public R5c a() {return null;}
	}
	@Response(examples="{foo:{bar:'$L{foo}'}}") @StatusCode(100)
	public static class R5c {}

	@Test void r05a_response_100_examples_swaggerOnAnnotation_lodalized() throws Exception {
		assertBean(getSwagger(new R5a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "foo{bar}", "{l-foo}");
		assertBean(getSwaggerWithFile(new R5a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "foo{bar}", "{l-foo}");
	}
	@Test void r05b_response_100_examples_swaggerOnAnnotation_lodalized() throws Exception {
		assertBean(getSwagger(new R5b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "foo{bar}", "{l-foo}");
		assertBean(getSwaggerWithFile(new R5b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "foo{bar}", "{l-foo}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/responses/<response>/schema
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class S1a {
		@RestGet(path="/path/{foo}/responses/100")
		public void a(Value<S1c> foo) {/* no-op */}
	}
	@Rest
	public static class S1b {
		@RestGet(path="/path/{foo}/responses/100")
		public S1c a() {return null;}
	}
	@Response @StatusCode(100)
	public static class S1c extends X {}

	@Test void s01a_responses_100_schema_default() throws Exception {
		assertBean(getSwagger(new S1a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "type,properties{a{format,type}}", "object,{{int32,integer}}");
		assertBean(getSwaggerWithFile(new S1a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "type,items{ref}", "array,{#/definitions/Foo}");
	}
	@Test void s01b_responses_100_schema_default() throws Exception {
		assertBean(getSwagger(new S1b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "type,properties{a{format,type}}", "object,{{int32,integer}}");
		assertBean(getSwaggerWithFile(new S1b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "type,items{ref}", "array,{#/definitions/Foo}");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{schema:{$ref:'b'}}}}}}"))
	public static class S2 {
		@RestGet(path="/path/{foo}/responses/100")
		public void a(@StatusCode Value<Integer> foo) {/* no-op */}
	}

	@Test void s02_response_100_schema_swaggerOnClass() throws Exception {
		assertBean(getSwagger(new S2()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "ref", "b");
		assertBean(getSwaggerWithFile(new S2()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "ref", "b");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{schema:{$ref:'b'}}}}}}"))
	public static class S3 {
		@RestGet(path="/path/{foo}/responses/100",swagger=@OpSwagger("responses:{100:{schema:{$ref:'c'}}}}"))
		public void a(@StatusCode Value<Integer> foo) {/* no-op */}
	}

	@Test void s03_response_100_schema_swaggerOnMethoe() throws Exception {
		assertBean(getSwagger(new S3()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "ref", "c");
		assertBean(getSwaggerWithFile(new S3()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "ref", "c");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{schema:{$ref:'b'}}}}}}"))
	public static class S4a {
		@RestGet(path="/path/{foo}/responses/100")
		public void a(Value<S4c> foo) {/* no-op */}
	}
	@Rest(swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{schema:{$ref:'b'}}}}}}"))
	public static class S4b {
		@RestGet(path="/path/{foo}/responses/100")
		public S4c a() {return null;}
	}
	@Response(schema=@Schema($ref="d")) @StatusCode(100)
	public static class S4c extends X {}

	@Test void s04a_response_100_schema_swaggerOnAnnotation() throws Exception {
		assertBean(getSwagger(new S4a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "ref", "d");
		assertBean(getSwaggerWithFile(new S4a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "ref", "d");
	}
	@Test void s04b_response_100_schema_swaggerOnAnnotation() throws Exception {
		assertBean(getSwagger(new S4b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "ref", "d");
		assertBean(getSwaggerWithFile(new S4b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "ref", "d");
	}

	@Rest(messages="BasicRestInfoProviderTest", swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{schema:{$ref:'b'}}}}}}"))
	public static class S5a {
		@RestGet(path="/path/{foo}/responses/100")
		public void a(Value<S5c> foo) {/* no-op */}
	}
	@Rest(messages="BasicRestInfoProviderTest", swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{schema:{$ref:'b'}}}}}}"))
	public static class S5b {
		@RestGet(path="/path/{foo}/responses/100")
		public S5c a() {return null;}
	}
	@Response(schema=@Schema($ref="l-foo")) @StatusCode(100)
	public static class S5c extends X {}

	@Test void s05a_response_100_schema_swaggerOnAnnotation_loealized() throws Exception {
		assertBean(getSwagger(new S5a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "ref", "l-foo");
		assertBean(getSwaggerWithFile(new S5a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "ref", "l-foo");
	}
	@Test void s05b_response_100_schema_swaggerOnAnnotation_loealized() throws Exception {
		assertBean(getSwagger(new S5b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "ref", "l-foo");
		assertBean(getSwaggerWithFile(new S5b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "ref", "l-foo");
	}

	@Bean(typeName="Foo")
	public static class X {
		public int a;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Example bean with getter-only property.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class T1 extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet(path="/")
		public T2 a(@Content T2 body) {
			return null;
		}
	}

	@Bean(sort=true)
	public static class T2 {
		private int f1;
		public int getF1() { return f1; }
		public T2 setF1(int v) { f1 = v; return this; }

		public int getF2() { return 2; }

		@Example
		public static T2 example() {
			return new T2().setF1(1);
		}
	}

	@Test void t01_bodyWithReadOnlyProperty() throws Exception {
		var p = MockRestClient.build(T1.class);
		var s = JsonParser.DEFAULT.parse(p.get("/api").accept("application/json").run().getContent().asString(), org.apache.juneau.bean.swagger.Swagger.class);
		var o = s.getOperation("/", "get");

		var ri = o.getResponse("200");
		assertBean(ri.getExamples(), "application/json5", "{\n\tf1: 1,\n\tf2: 2\n}");
	}
}