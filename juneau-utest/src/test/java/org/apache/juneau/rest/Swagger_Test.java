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
package org.apache.juneau.rest;

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;
import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.bean.swagger.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Contact;
import org.apache.juneau.http.annotation.License;
import org.apache.juneau.http.annotation.Tag;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.annotation.Swagger;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.rest.staticfile.*;
import org.apache.juneau.rest.swagger.*;
import org.apache.juneau.xml.*;
import org.junit.jupiter.api.*;

class Swagger_Test extends SimpleTestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Setup
	//------------------------------------------------------------------------------------------------------------------

	public void testMethod() { /* no-op */ }

	private org.apache.juneau.bean.swagger.Swagger getSwaggerWithFile(Object resource) throws Exception {
		RestContext rc = RestContext.create(resource.getClass(),null,null).init(()->resource).defaultClasses(TestClasspathFileFinder.class).build();
		RestOpContext roc = RestOpContext.create(Swagger_Test.class.getMethod("testMethod"), rc).build();
		RestSession call = RestSession.create(rc).resource(resource).req(new MockServletRequest()).res(new MockServletResponse()).build();
		RestRequest req = roc.createRequest(call);
		SwaggerProvider ip = rc.getSwaggerProvider();
		return ip.getSwagger(rc, req.getLocale());
	}

	private static org.apache.juneau.bean.swagger.Swagger getSwagger(Object resource) throws Exception {
		RestContext rc = RestContext.create(resource.getClass(),null,null).init(()->resource).build();
		RestOpContext roc = RestOpContext.create(Swagger_Test.class.getMethod("testMethod"), rc).build();
		RestSession call = RestSession.create(rc).resource(resource).req(new MockServletRequest()).res(new MockServletResponse()).build();
		RestRequest req = roc.createRequest(call);
		SwaggerProvider ip = rc.getSwaggerProvider();
		return ip.getSwagger(rc, req.getLocale());
	}

	public static class TestClasspathFileFinder extends BasicStaticFiles {

		public TestClasspathFileFinder() {
			super(StaticFiles.create(BeanStore.INSTANCE).cp(Swagger_Test.class, null, false));
		}

		@Override /* FileFinder */
		public Optional<InputStream> getStream(String name, Locale locale) throws IOException {
			if (name.endsWith(".json"))
				return optional(SwaggerProvider.class.getResourceAsStream("BasicRestInfoProviderTest_swagger.json"));
			return super.getStream(name, locale);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// /<root>
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A1 {}

	@Test void a01_swagger_default() throws Exception {
		org.apache.juneau.bean.swagger.Swagger x = getSwagger(new A1());
		assertEquals("2.0", x.getSwagger());
		assertEquals(null, x.getHost());
		assertEquals(null, x.getBasePath());
		assertEquals(null, x.getSchemes());
	}
	@Test void a01_swagger_default_withFile() throws Exception {
		org.apache.juneau.bean.swagger.Swagger x = getSwaggerWithFile(new A1());
		assertEquals("0.0", x.getSwagger());
		assertEquals("s-host", x.getHost());
		assertEquals("s-basePath", x.getBasePath());
		assertJson(x.getSchemes(), "['s-scheme']");
	}


	@Rest(swagger=@Swagger("{swagger:'3.0',host:'a-host',basePath:'a-basePath',schemes:['a-scheme']}"))
	public static class A2 {}

	@Test void a02_swagger_Swagger_value() throws Exception {
		org.apache.juneau.bean.swagger.Swagger x = getSwagger(new A2());
		assertEquals("3.0", x.getSwagger());
		assertEquals("a-host", x.getHost());
		assertEquals("a-basePath", x.getBasePath());
		assertJson(x.getSchemes(), "['a-scheme']");
	}
	@Test void a02_swagger_Swagger_value_withFile() throws Exception {
		org.apache.juneau.bean.swagger.Swagger x = getSwaggerWithFile(new A2());
		assertEquals("3.0", x.getSwagger());
		assertEquals("a-host", x.getHost());
		assertEquals("a-basePath", x.getBasePath());
		assertJson(x.getSchemes(), "['a-scheme']");
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
		Info x = getSwagger(new B1()).getInfo();
		assertEquals("a-title", x.getTitle());
		assertEquals("a-description", x.getDescription());
		assertEquals(null, x.getVersion());
		assertEquals(null, x.getTermsOfService());
		assertEquals(null, x.getContact());
		assertEquals(null, x.getLicense());
	}
	@Test void b01b_info_Rest_withFile() throws Exception {
		Info x = getSwaggerWithFile(new B1()).getInfo();
		assertEquals("s-title", x.getTitle());
		assertEquals("s-description", x.getDescription());
		assertEquals("0.0.0", x.getVersion());
		assertEquals("s-termsOfService", x.getTermsOfService());
		assertJson(x.getContact(), "{name:'s-name',url:'s-url',email:'s-email'}");
		assertJson(x.getLicense(), "{name:'s-name',url:'s-url'}");
	}

	@Rest(
		messages="BasicRestInfoProviderTest",
		title="$L{foo}",
		description="$L{foo}"
	)
	public static class B2 {}

	@Test void b02a_info_Rest_localized() throws Exception {
		Info x = getSwagger(new B2()).getInfo();
		assertEquals("l-foo", x.getTitle());
		assertEquals("l-foo", x.getDescription());
	}
	@Test void b02b_info_Rest_localized_withFile() throws Exception {
		Info x = getSwaggerWithFile(new B2()).getInfo();
		assertEquals("s-title", x.getTitle());
		assertEquals("s-description", x.getDescription());
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
		Info x = getSwagger(new B3()).getInfo();
		assertEquals("b-title", x.getTitle());
		assertEquals("b-description", x.getDescription());
		assertEquals("2.0.0", x.getVersion());
		assertEquals("a-termsOfService", x.getTermsOfService());
		assertJson(x.getContact(), "{name:'a-name',url:'a-url',email:'a-email'}");
		assertJson(x.getLicense(), "{name:'a-name',url:'a-url'}");
	}
	@Test void b03b_info_Swagger_value_withFile() throws Exception {
		Info x = getSwaggerWithFile(new B3()).getInfo();
		assertEquals("b-title", x.getTitle());
		assertEquals("b-description", x.getDescription());
		assertEquals("2.0.0", x.getVersion());
		assertEquals("a-termsOfService", x.getTermsOfService());
		assertJson(x.getContact(), "{name:'a-name',url:'a-url',email:'a-email'}");
		assertJson(x.getLicense(), "{name:'a-name',url:'a-url'}");
	}

	@Rest(
		messages="BasicRestInfoProviderTest",
		title="a-title",
		description="a-description",
		swagger=@Swagger("{info:{title:'$L{bar}',description:'$L{bar}'}}")
	)
	public static class B4 {}

	@Test void b04_info_Swagger_value_localised() throws Exception {
		assertEquals("l-bar", getSwagger(new B4()).getInfo().getTitle());
		assertEquals("l-bar", getSwaggerWithFile(new B4()).getInfo().getTitle());
		assertEquals("l-bar", getSwagger(new B4()).getInfo().getDescription());
		assertEquals("l-bar", getSwaggerWithFile(new B4()).getInfo().getDescription());
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
		Info x = getSwagger(new B5()).getInfo();
		assertEquals("c-title", x.getTitle());
		assertEquals("c-description", x.getDescription());
		assertEquals("3.0.0", x.getVersion());
		assertEquals("b-termsOfService", x.getTermsOfService());
		assertJson(x.getContact(), "{name:'b-name',url:'b-url',email:'b-email'}");
		assertJson(x.getLicense(), "{name:'b-name',url:'b-url'}");
	}
	@Test void b05b_info_Swagger_title_withFile() throws Exception {
		Info x = getSwaggerWithFile(new B5()).getInfo();
		assertEquals("c-title", x.getTitle());
		assertEquals("c-description", x.getDescription());
		assertEquals("3.0.0", x.getVersion());
		assertEquals("b-termsOfService", x.getTermsOfService());
		assertJson(x.getContact(), "{name:'b-name',url:'b-url',email:'b-email'}");
		assertJson(x.getLicense(), "{name:'b-name',url:'b-url'}");
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
		Info x = getSwagger(new B6()).getInfo();
		assertEquals("l-baz", x.getTitle());
		assertEquals("l-baz", x.getDescription());
		assertEquals("l-foo", x.getVersion());
		assertEquals("l-foo", x.getTermsOfService());
		assertJson(x.getContact(), "{name:'l-foo',url:'l-bar',email:'l-baz'}");
		assertJson(x.getLicense(), "{name:'l-foo',url:'l-bar'}");
	}
	@Test void b06b_info_Swagger_title_localized_withFile() throws Exception {
		Info x = getSwaggerWithFile(new B6()).getInfo();
		assertEquals("l-baz", x.getTitle());
		assertEquals("l-baz", x.getDescription());
		assertEquals("l-foo", x.getVersion());
		assertEquals("l-foo", x.getTermsOfService());
		assertJson(x.getContact(), "{name:'l-foo',url:'l-bar',email:'l-baz'}");
		assertJson(x.getLicense(), "{name:'l-foo',url:'l-bar'}");
	}

	@Rest(
		swagger=@Swagger(
			title="c-title",
			description="c-description"
		)
	)
	public static class B07 {}

	@Test void b07a_title_Swagger_title_only() throws Exception {
		Info x = getSwagger(new B07()).getInfo();
		assertEquals("c-title", x.getTitle());
		assertEquals("c-description", x.getDescription());
	}
	@Test void b07b_title_Swagger_title_only_withFile() throws Exception {
		Info x = getSwaggerWithFile(new B07()).getInfo();
		assertEquals("c-title", x.getTitle());
		assertEquals("c-description", x.getDescription());
	}

	//------------------------------------------------------------------------------------------------------------------
	// /tags
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C1 {}

	@Test void c01a_tags_default() throws Exception {
		org.apache.juneau.bean.swagger.Swagger x = getSwagger(new C1());
		assertEquals(null, x.getTags());
	}
	@Test void c01b_tags_default_withFile() throws Exception {
		org.apache.juneau.bean.swagger.Swagger x = getSwaggerWithFile(new C1());
		assertJson(x.getTags(), "[{name:'s-name',description:'s-description',externalDocs:{description:'s-description',url:'s-url'}}]");
	}

	// Tags in @ResourceSwagger(value) should override file.
	@Rest(
		swagger=@Swagger(
			"{tags:[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}}]}"
		)
	)
	public static class C2 {}

	@Test void c02a_tags_Swagger_value() throws Exception {
		org.apache.juneau.bean.swagger.Swagger x = getSwagger(new C2());
		assertJson(x.getTags(), "[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}}]");
	}
	@Test void c02b_tags_Swagger_value_withFile() throws Exception {
		org.apache.juneau.bean.swagger.Swagger x = getSwaggerWithFile(new C2());
		assertJson(x.getTags(), "[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}}]");
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
		org.apache.juneau.bean.swagger.Swagger x = getSwagger(new C3());
		assertJson(x.getTags(), "[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}},{name:'b-name',description:'b-description',externalDocs:{description:'b-description',url:'b-url'}}]");
	}
	@Test void c03b_tags_Swagger_tags_withFile() throws Exception {
		org.apache.juneau.bean.swagger.Swagger x = getSwaggerWithFile(new C3());
		assertJson(x.getTags(), "[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}},{name:'b-name',description:'b-description',externalDocs:{description:'b-description',url:'b-url'}}]");
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
		org.apache.juneau.bean.swagger.Swagger x = getSwagger(new C4());
		assertJson(x.getTags(), "[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}},{name:'b-name',description:'b-description',externalDocs:{description:'b-description',url:'b-url'}}]");
	}
	@Test void c04b_tags_Swagger_tags_withFile() throws Exception {
		org.apache.juneau.bean.swagger.Swagger x = getSwaggerWithFile(new C4());
		assertJson(x.getTags(), "[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}},{name:'b-name',description:'b-description',externalDocs:{description:'b-description',url:'b-url'}}]");
	}

	// Tags in both Swagger.json and @ResourceSwagger(tags) should accumulate.
	@Rest(
		swagger=@Swagger(
			tags=@Tag(name="b-name",description="b-description",externalDocs=@ExternalDocs(description="b-description",url="b-url"))
		)
	)
	public static class C5 {}

	@Test void c05a_tags_Swagger_tags_only() throws Exception {
		org.apache.juneau.bean.swagger.Swagger x = getSwagger(new C5());
		assertJson(x.getTags(), "[{name:'b-name',description:'b-description',externalDocs:{description:'b-description',url:'b-url'}}]");
	}
	@Test void c05b_tags_Swagger_tags_only_witFile() throws Exception {
		org.apache.juneau.bean.swagger.Swagger x = getSwaggerWithFile(new C5());
		assertJson(x.getTags(), "[{name:'s-name',description:'s-description',externalDocs:{description:'s-description',url:'s-url'}},{name:'b-name',description:'b-description',externalDocs:{description:'b-description',url:'b-url'}}]");
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
		org.apache.juneau.bean.swagger.Swagger x = getSwagger(new C6());
		assertJson(x.getTags(), "[{name:'s-name',description:'c-description',externalDocs:{description:'c-description',url:'c-url'}}]");
	}
	@Test void c06b_tags_Swagger_tags_dups_withFile() throws Exception {
		org.apache.juneau.bean.swagger.Swagger x = getSwaggerWithFile(new C6());
		assertJson(x.getTags(), "[{name:'s-name',description:'c-description',externalDocs:{description:'c-description',url:'c-url'}}]");
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
		org.apache.juneau.bean.swagger.Swagger x = getSwagger(new C7());
		assertJson(x.getTags(), "[{name:'l-foo',description:'l-foo',externalDocs:{description:'l-foo',url:'l-foo'}}]");
	}
	@Test void c07b_tags_Swagger_tags_localised_withFile() throws Exception {
		org.apache.juneau.bean.swagger.Swagger x = getSwaggerWithFile(new C7());
		assertJson(x.getTags(), "[{name:'l-foo',description:'l-foo',externalDocs:{description:'l-foo',url:'l-foo'}}]");
	}

	// Auto-detect tags defined on methods.
	@Rest
	public static class C8 {
		@RestOp(swagger=@OpSwagger(tags="foo"))
		public void a() { /* no-op */ }
	}

	@Test void c08a_tags_Swagger_tags_loose() throws Exception {
		org.apache.juneau.bean.swagger.Swagger x = getSwagger(new C8());
		assertJson(x.getTags(), "[{name:'foo'}]");
	}
	@Test void c08b_tags_Swagger_tags_loose_withFile() throws Exception {
		org.apache.juneau.bean.swagger.Swagger x = getSwaggerWithFile(new C8());
		assertJson(x.getTags(), "[{name:'s-name',description:'s-description',externalDocs:{description:'s-description',url:'s-url'}},{name:'foo'}]");
	}

	// Comma-delimited list
	@Rest
	public static class C9 {
		@RestOp(swagger=@OpSwagger(tags=" foo, bar "))
		public void a() {/* no-op */}
	}

	@Test void c09a_tags_Swagger_tags_loose_cdl() throws Exception {
		org.apache.juneau.bean.swagger.Swagger x = getSwagger(new C9());
		assertJson(x.getTags(), "[{name:'foo'},{name:'bar'}]");
	}
	@Test void c09b_tags_Swagger_tags_loose_cdl_withFile() throws Exception {
		org.apache.juneau.bean.swagger.Swagger x = getSwaggerWithFile(new C9());
		assertJson(x.getTags(), "[{name:'s-name',description:'s-description',externalDocs:{description:'s-description',url:'s-url'}},{name:'foo'},{name:'bar'}]");
	}

	// JsonList
	@Rest
	public static class C10 {
		@RestGet(swagger=@OpSwagger(tags="['foo', 'bar']"))
		public void a() {/* no-op */}
	}

	@Test void c10a_tags_Swagger_tags_loose_olist() throws Exception {
		org.apache.juneau.bean.swagger.Swagger x = getSwagger(new C10());
		assertJson(x.getTags(), "[{name:'foo'},{name:'bar'}]");
	}
	@Test void c10b_tags_Swagger_tags_loose_olist_withFile() throws Exception {
		org.apache.juneau.bean.swagger.Swagger x = getSwaggerWithFile(new C10());
		assertJson(x.getTags(), "[{name:'s-name',description:'s-description',externalDocs:{description:'s-description',url:'s-url'}},{name:'foo'},{name:'bar'}]");
	}

	// JsonList localized
	@Rest(messages="BasicRestInfoProviderTest")
	public static class C11 {
		@RestGet(swagger=@OpSwagger(tags="['$L{foo}', '$L{bar}']"))
		public void a() {/* no-op */}
	}

	@Test void c11a_tags_Swagger_tags_loose_olist_localized() throws Exception {
		org.apache.juneau.bean.swagger.Swagger x = getSwagger(new C11());
		assertJson(x.getTags(), "[{name:'l-foo'},{name:'l-bar'}]");
	}
	@Test void c11b_tags_Swagger_tags_loose_olist_localized_withFile() throws Exception {
		org.apache.juneau.bean.swagger.Swagger x = getSwaggerWithFile(new C11());
		assertJson(x.getTags(), "[{name:'s-name',description:'s-description',externalDocs:{description:'s-description',url:'s-url'}},{name:'l-foo'},{name:'l-bar'}]");
	}

	// Comma-delimited list localized
	@Rest(messages="BasicRestInfoProviderTest")
	public static class C12 {
		@RestGet(swagger=@OpSwagger(tags=" $L{foo}, $L{bar} "))
		public void a() {/* no-op */}
	}

	@Test void c12a_tags_Swagger_tags_loose_cdl_localized() throws Exception {
		org.apache.juneau.bean.swagger.Swagger x = getSwagger(new C12());
		assertJson(x.getTags(), "[{name:'l-foo'},{name:'l-bar'}]");
	}
	@Test void c12b_tags_Swagger_tags_loose_cdl_localized_withFile() throws Exception {
		org.apache.juneau.bean.swagger.Swagger x = getSwaggerWithFile(new C12());
		assertJson(x.getTags(), "[{name:'s-name',description:'s-description',externalDocs:{description:'s-description',url:'s-url'}},{name:'l-foo'},{name:'l-bar'}]");
	}

	//------------------------------------------------------------------------------------------------------------------
	// /externalDocs
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D1 {}

	@Test void d01a_externalDocs_default() throws Exception {
		ExternalDocumentation x = getSwagger(new D1()).getExternalDocs();
		assertEquals(null, x);
	}
	@Test void d01b_externalDocs_default_withFile() throws Exception {
		ExternalDocumentation x = getSwaggerWithFile(new D1()).getExternalDocs();
		assertJson(x, "{description:'s-description',url:'s-url'}");
	}


	@Rest(
		swagger=@Swagger("{externalDocs:{description:'a-description',url:'a-url'}}")
	)
	public static class D2 {}

	@Test void d02a_externalDocs_Swagger_value() throws Exception {
		ExternalDocumentation x = getSwagger(new D2()).getExternalDocs();
		assertJson(x, "{description:'a-description',url:'a-url'}");
	}
	@Test void d02b_externalDocs_Swagger_value_withFile() throws Exception {
		ExternalDocumentation x = getSwaggerWithFile(new D2()).getExternalDocs();
		assertJson(x, "{description:'a-description',url:'a-url'}");
	}


	@Rest(
		swagger=@Swagger(
			value="{externalDocs:{description:'a-description',url:'a-url'}}",
			externalDocs=@ExternalDocs(description="b-description",url="b-url")
		)
	)
	public static class D3 {}

	@Test void d03a_externalDocs_Swagger_externalDocs() throws Exception {
		ExternalDocumentation x = getSwagger(new D3()).getExternalDocs();
		assertJson(x, "{description:'b-description',url:'b-url'}");
	}
	@Test void d03b_externalDocs_Swagger_externalDocs_withFile() throws Exception {
		ExternalDocumentation x = getSwaggerWithFile(new D3()).getExternalDocs();
		assertJson(x, "{description:'b-description',url:'b-url'}");
	}

	@Rest(
		swagger=@Swagger(
			value="{info:{externalDocs:{description:'a-description',url:'a-url'}}}",
			externalDocs=@ExternalDocs(description="b-description", url="b-url")
		)
	)
	public static class D4 {}

	@Test void d04a_externalDocs_Swagger_externalDocs() throws Exception {
		ExternalDocumentation x = getSwagger(new D4()).getExternalDocs();
		assertJson(x, "{description:'b-description',url:'b-url'}");
	}
	@Test void d04b_externalDocs_Swagger_externalDocs_withFile() throws Exception {
		ExternalDocumentation x = getSwaggerWithFile(new D4()).getExternalDocs();
		assertJson(x, "{description:'b-description',url:'b-url'}");
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
		ExternalDocumentation x = getSwagger(new D5()).getExternalDocs();
		assertJson(x, "{description:'l-foo',url:'l-bar'}");
	}
	@Test void d05b_externalDocs_Swagger_externalDocs_localised_withFile() throws Exception {
		ExternalDocumentation x = getSwaggerWithFile(new D5()).getExternalDocs();
		assertJson(x, "{description:'l-foo',url:'l-bar'}");
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
		Operation x = getSwagger(new E1()).getPaths().get("/path/{foo}").get("get");
		assertEquals("a", x.getOperationId());
		assertEquals(null, x.getSummary());
		assertEquals(null, x.getDescription());
		assertEquals(null, x.getDeprecated());
		assertEquals(null, x.getSchemes());
	}
	@Test void e01b_operation_summary_default_withFile() throws Exception {
		Operation x = getSwaggerWithFile(new E1()).getPaths().get("/path/{foo}").get("get");
		assertEquals("s-operationId", x.getOperationId());
		assertEquals("s-summary", x.getSummary());
		assertEquals("s-description", x.getDescription());
		assertJson(x.getDeprecated(), "true");
		assertJson(x.getSchemes(), "['s-scheme']");
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
		Operation x = getSwagger(new E2()).getPaths().get("/path/{foo}").get("get");
		assertEquals("a-operationId", x.getOperationId());
		assertEquals("a-summary", x.getSummary());
		assertEquals("a-description", x.getDescription());
		assertJson(x.getDeprecated(), "false");
		assertJson(x.getSchemes(), "['a-scheme']");
	}
	@Test void e02b_operation_summary_swaggerOnClass_withFile() throws Exception {
		Operation x = getSwaggerWithFile(new E2()).getPaths().get("/path/{foo}").get("get");
		assertEquals("a-operationId", x.getOperationId());
		assertEquals("a-summary", x.getSummary());
		assertEquals("a-description", x.getDescription());
		assertJson(x.getDeprecated(), "false");
		assertJson(x.getSchemes(), "['a-scheme']");
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
		Operation x = getSwagger(new E3()).getPaths().get("/path/{foo}").get("get");
		assertEquals("b-operationId", x.getOperationId());
		assertEquals("b-summary", x.getSummary());
		assertEquals("b-description", x.getDescription());
		assertJson(x.getDeprecated(), "false");
		assertJson(x.getSchemes(), "['b-scheme']");
	}
	@Test void e03b_operation_summary_swaggerOnMethod_withFile() throws Exception {
		Operation x = getSwaggerWithFile(new E3()).getPaths().get("/path/{foo}").get("get");
		assertEquals("b-operationId", x.getOperationId());
		assertEquals("b-summary", x.getSummary());
		assertEquals("b-description", x.getDescription());
		assertJson(x.getDeprecated(), "false");
		assertJson(x.getSchemes(), "['b-scheme']");
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
		Operation x = getSwagger(new E4()).getPaths().get("/path/{foo}").get("get");
		assertEquals("c-operationId", x.getOperationId());
		assertEquals("c-summary", x.getSummary());
		assertEquals("c-description", x.getDescription());
		assertJson(x.getSchemes(), "['d-scheme-1','d-scheme-2']");
	}
	@Test void e04b_operation_summary_swaggerOnAnnotation_withFile() throws Exception {
		Operation x = getSwaggerWithFile(new E4()).getPaths().get("/path/{foo}").get("get");
		assertEquals("c-operationId", x.getOperationId());
		assertEquals("c-summary", x.getSummary());
		assertEquals("c-description", x.getDescription());
		assertJson(x.getSchemes(), "['d-scheme-1','d-scheme-2']");
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
		Operation x = getSwagger(new E5()).getPaths().get("/path/{foo}").get("get");
		assertEquals("l-foo", x.getOperationId());
		assertEquals("l-foo", x.getSummary());
		assertEquals("l-foo", x.getDescription());
		assertJson(x.getDeprecated(), "false");
		assertJson(x.getSchemes(), "['l-foo']");
	}
	@Test void e05b_operation_summary_swaggerOnAnnotation_localized_withFile() throws Exception {
		Operation x = getSwaggerWithFile(new E5()).getPaths().get("/path/{foo}").get("get");
		assertEquals("l-foo", x.getOperationId());
		assertEquals("l-foo", x.getSummary());
		assertEquals("l-foo", x.getDescription());
		assertJson(x.getDeprecated(), "false");
		assertJson(x.getSchemes(), "['l-foo']");
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
		Operation x = getSwagger(new E6()).getPaths().get("/path/{foo}").get("get");
		assertEquals("a-summary", x.getSummary());
		assertEquals("a-description", x.getDescription());
	}
	@Test void e06b_operation_summary_RestOp_withFile() throws Exception {
		Operation x = getSwaggerWithFile(new E6()).getPaths().get("/path/{foo}").get("get");
		assertEquals("a-summary", x.getSummary());
		assertEquals("a-description", x.getDescription());
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
		Operation x = getSwagger(new E7()).getPaths().get("/path/{foo}").get("get");
		assertEquals("d-summary", x.getSummary());
		assertEquals("d-description", x.getDescription());
	}
	@Test void e07b_operation_summary_RestOp_withFile() throws Exception {
		Operation x = getSwaggerWithFile(new E7()).getPaths().get("/path/{foo}").get("get");
		assertEquals("d-summary", x.getSummary());
		assertEquals("d-description", x.getDescription());
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
		assertJson(getSwagger(new F1()).getPaths().get("/path/{foo}").get("get").getTags(), "null");
		assertJson(getSwaggerWithFile(new F1()).getPaths().get("/path/{foo}").get("get").getTags(), "['s-tag']");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{tags:['a-tag']}}}"))
	public static class F2 {
		@RestGet(path="/path/{foo}")
		public X a() {
			return null;
		}
	}

	@Test void f02_operation_tags_swaggerOnClass() throws Exception {
		assertJson(getSwagger(new F2()).getPaths().get("/path/{foo}").get("get").getTags(), "['a-tag']");
		assertJson(getSwaggerWithFile(new F2()).getPaths().get("/path/{foo}").get("get").getTags(), "['a-tag']");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{tags:['a-tag']}}}"))
	public static class F3 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger("tags:['b-tag']"))
		public X a() {
			return null;
		}
	}

	@Test void f03_operation_tags_swaggerOnMethod() throws Exception {
		assertJson(getSwagger(new F3()).getPaths().get("/path/{foo}").get("get").getTags(), "['b-tag']");
		assertJson(getSwaggerWithFile(new F3()).getPaths().get("/path/{foo}").get("get").getTags(), "['b-tag']");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{tags:['a-tag']}}}"))
	public static class F4 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger(tags="['c-tag-1','c-tag-2']"))
		public X a() {
			return null;
		}
	}

	@Test void f04_operation_tags_swaggerOnAnnotation() throws Exception {
		assertJson(getSwagger(new F4()).getPaths().get("/path/{foo}").get("get").getTags(), "['c-tag-1','c-tag-2']");
		assertJson(getSwaggerWithFile(new F4()).getPaths().get("/path/{foo}").get("get").getTags(), "['c-tag-1','c-tag-2']");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{tags:['a-tag']}}}"))
	public static class F5 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger(tags="c-tag-1, c-tag-2"))
		public X a() {
			return null;
		}
	}

	@Test void f05_operation_tags_swaggerOnAnnotation() throws Exception {
		assertJson(getSwagger(new F5()).getPaths().get("/path/{foo}").get("get").getTags(), "['c-tag-1','c-tag-2']");
		assertJson(getSwaggerWithFile(new F5()).getPaths().get("/path/{foo}").get("get").getTags(), "['c-tag-1','c-tag-2']");
	}

	@Rest(messages="BasicRestInfoProviderTest", swagger=@Swagger("paths:{'/path/{foo}':{get:{tags:'a-tags'}}}"))
	public static class F6 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger(tags="$L{foo}"))
		public X a() {
			return null;
		}
	}

	@Test void f06_operation_tags_swaggerOnAnnotation_localized() throws Exception {
		assertJson(getSwagger(new F6()).getPaths().get("/path/{foo}").get("get").getTags(), "['l-foo']");
		assertJson(getSwaggerWithFile(new F6()).getPaths().get("/path/{foo}").get("get").getTags(), "['l-foo']");
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
		assertJson(getSwagger(new G1()).getPaths().get("/path/{foo}").get("get").getExternalDocs(), "null");
		assertJson(getSwaggerWithFile(new G1()).getPaths().get("/path/{foo}").get("get").getExternalDocs(), "{description:'s-description',url:'s-url'}");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{externalDocs:{description:'a-description',url:'a-url'}}}}"))
	public static class G2 {
		@RestGet(path="/path/{foo}")
		public X a() {
			return null;
		}
	}

	@Test void g02_operation_externalDocs_swaggerOnClass() throws Exception {
		assertJson(getSwagger(new G2()).getPaths().get("/path/{foo}").get("get").getExternalDocs(), "{description:'a-description',url:'a-url'}");
		assertJson(getSwaggerWithFile(new G2()).getPaths().get("/path/{foo}").get("get").getExternalDocs(), "{description:'a-description',url:'a-url'}");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{externalDocs:{description:'a-description',url:'a-url'}}}}"))
	public static class G3 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger("externalDocs:{description:'b-description',url:'b-url'}"))
		public X a() {
			return null;
		}
	}

	@Test void g03_operation_externalDocs_swaggerOnMethod() throws Exception {
		assertJson(getSwagger(new G3()).getPaths().get("/path/{foo}").get("get").getExternalDocs(), "{description:'b-description',url:'b-url'}");
		assertJson(getSwaggerWithFile(new G3()).getPaths().get("/path/{foo}").get("get").getExternalDocs(), "{description:'b-description',url:'b-url'}");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{externalDocs:{description:'a-description',url:'a-url'}}}}"))
	public static class G4 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger(externalDocs=@ExternalDocs(description="c-description",url="c-url")))
		public X a() {
			return null;
		}
	}

	@Test void g04_operation_externalDocs_swaggerOnAnnotation() throws Exception {
		assertJson(getSwagger(new G4()).getPaths().get("/path/{foo}").get("get").getExternalDocs(), "{description:'c-description',url:'c-url'}");
		assertJson(getSwaggerWithFile(new G4()).getPaths().get("/path/{foo}").get("get").getExternalDocs(), "{description:'c-description',url:'c-url'}");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{externalDocs:{description:'a-description',url:'a-url'}}}}"))
	public static class G5 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger(externalDocs=@ExternalDocs(description="d-description",url="d-url")))
		public X a() {
			return null;
		}
	}

	@Test void g05_operation_externalDocs_swaggerOnAnnotation() throws Exception {
		assertJson(getSwagger(new G5()).getPaths().get("/path/{foo}").get("get").getExternalDocs(), "{description:'d-description',url:'d-url'}");
		assertJson(getSwaggerWithFile(new G5()).getPaths().get("/path/{foo}").get("get").getExternalDocs(), "{description:'d-description',url:'d-url'}");
	}

	@Rest(messages="BasicRestInfoProviderTest", swagger=@Swagger("paths:{'/path/{foo}':{get:{externalDocs:{description:'a-description',url:'a-url'}}}}"))
	public static class G6 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger(externalDocs=@ExternalDocs(description="$L{foo}",url="$L{foo}")))
		public X a() {
			return null;
		}
	}

	@Test void g06_operation_externalDocs_swaggerOnAnnotation_localized() throws Exception {
		assertJson(getSwagger(new G6()).getPaths().get("/path/{foo}").get("get").getExternalDocs(), "{description:'l-foo',url:'l-foo'}");
		assertJson(getSwaggerWithFile(new G6()).getPaths().get("/path/{foo}").get("get").getExternalDocs(), "{description:'l-foo',url:'l-foo'}");
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
		assertJson(getSwagger(new H1()).getPaths().get("/path/{foo}").get("get").getConsumes(), "null");
		assertJson(getSwaggerWithFile(new H1()).getPaths().get("/path/{foo}").get("get").getConsumes(), "['s-consumes']");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{consumes:['a-consumes']}}}"))
	public static class H2 {
		@RestGet(path="/path/{foo}")
		public X a() {
			return null;
		}
	}

	@Test void h02_operation_consumes_swaggerOnClass() throws Exception {
		assertJson(getSwagger(new H2()).getPaths().get("/path/{foo}").get("get").getConsumes(), "['a-consumes']");
		assertJson(getSwaggerWithFile(new H2()).getPaths().get("/path/{foo}").get("get").getConsumes(), "['a-consumes']");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{consumes:['a-consumes']}}}"))
	public static class H3 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger("consumes:['b-consumes']"))
		public X a() {
			return null;
		}
	}

	@Test void h03_operation_consumes_swaggerOnMethod() throws Exception {
		assertJson(getSwagger(new H3()).getPaths().get("/path/{foo}").get("get").getConsumes(), "['b-consumes']");
		assertJson(getSwaggerWithFile(new H3()).getPaths().get("/path/{foo}").get("get").getConsumes(), "['b-consumes']");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{consumes:['a-consumes']}}}"))
	public static class H4 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger(consumes="['c-consumes-1','c-consumes-2']"))
		public X a() {
			return null;
		}
	}

	@Test void h04_operation_consumes_swaggerOnAnnotation() throws Exception {
		assertJson(getSwagger(new H4()).getPaths().get("/path/{foo}").get("get").getConsumes(), "['c-consumes-1','c-consumes-2']");
		assertJson(getSwaggerWithFile(new H4()).getPaths().get("/path/{foo}").get("get").getConsumes(), "['c-consumes-1','c-consumes-2']");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{consumes:['a-consumes']}}}"))
	public static class H5 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger(consumes="c-consumes-1, c-consumes-2"))
		public X a() {
			return null;
		}
	}

	@Test void h05_operation_consumes_swaggerOnAnnotation() throws Exception {
		assertJson(getSwagger(new H5()).getPaths().get("/path/{foo}").get("get").getConsumes(), "['c-consumes-1','c-consumes-2']");
		assertJson(getSwaggerWithFile(new H5()).getPaths().get("/path/{foo}").get("get").getConsumes(), "['c-consumes-1','c-consumes-2']");
	}

	@Rest(messages="BasicRestInfoProviderTest", swagger=@Swagger("paths:{'/path/{foo}':{get:{consumes:['a-consumes']}}}"))
	public static class H6 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger(consumes="['$L{foo}']"))
		public X a() {
			return null;
		}
	}

	@Test void h06_operation_consumes_swaggerOnAnnotation_localized() throws Exception {
		assertJson(getSwagger(new H6()).getPaths().get("/path/{foo}").get("get").getConsumes(), "['l-foo']");
		assertJson(getSwaggerWithFile(new H6()).getPaths().get("/path/{foo}").get("get").getConsumes(), "['l-foo']");
	}

	@Rest(parsers={JsonParser.class})
	public static class H7 {
		@RestPut(path="/path2/{foo}")
		public X a() {
			return null;
		}
	}

	@Test void h07_operation_consumes_parsersOnClass() throws Exception {
		assertJson(getSwagger(new H7()).getPaths().get("/path2/{foo}").get("put").getConsumes(), "null");
		assertJson(getSwaggerWithFile(new H7()).getPaths().get("/path2/{foo}").get("put").getConsumes(), "null");
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
		assertJson(getSwagger(new H8()).getPaths().get("/path2/{foo}").get("put").getConsumes(), "['text/xml','application/xml']");
		assertJson(getSwaggerWithFile(new H8()).getPaths().get("/path2/{foo}").get("put").getConsumes(), "['text/xml','application/xml']");
	}

	@Rest(parsers={JsonParser.class},swagger=@Swagger("paths:{'/path2/{foo}':{put:{consumes:['a-consumes']}}}"))
	public static class H9 {
		@RestPut(path="/path2/{foo}",parsers={XmlParser.class})
		public X a() {
			return null;
		}
	}

	@Test void h09_operation_consumes_parsersOnClassAndMethodWithSwagger() throws Exception {
		assertJson(getSwagger(new H9()).getPaths().get("/path2/{foo}").get("put").getConsumes(), "['a-consumes']");
		assertJson(getSwaggerWithFile(new H9()).getPaths().get("/path2/{foo}").get("put").getConsumes(), "['a-consumes']");
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
		assertJson(getSwagger(new I1()).getPaths().get("/path/{foo}").get("get").getProduces(), "null");
		assertJson(getSwaggerWithFile(new I1()).getPaths().get("/path/{foo}").get("get").getProduces(), "['s-produces']");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{produces:['a-produces']}}}"))
	public static class I2 {
		@RestGet(path="/path/{foo}")
		public X a() {
			return null;
		}
	}

	@Test void i02_operation_produces_swaggerOnClass() throws Exception {
		assertJson(getSwagger(new I2()).getPaths().get("/path/{foo}").get("get").getProduces(), "['a-produces']");
		assertJson(getSwaggerWithFile(new I2()).getPaths().get("/path/{foo}").get("get").getProduces(), "['a-produces']");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{produces:['a-produces']}}}"))
	public static class I3 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger("produces:['b-produces']"))
		public X a() {
			return null;
		}
	}

	@Test void i03_operation_produces_swaggerOnMethod() throws Exception {
		assertJson(getSwagger(new I3()).getPaths().get("/path/{foo}").get("get").getProduces(), "['b-produces']");
		assertJson(getSwaggerWithFile(new I3()).getPaths().get("/path/{foo}").get("get").getProduces(), "['b-produces']");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{produces:['a-produces']}}}"))
	public static class I4 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger(produces="['c-produces-1','c-produces-2']"))
		public X a() {
			return null;
		}
	}

	@Test void i04_operation_produces_swaggerOnAnnotation() throws Exception {
		assertJson(getSwagger(new I4()).getPaths().get("/path/{foo}").get("get").getProduces(), "['c-produces-1','c-produces-2']");
		assertJson(getSwaggerWithFile(new I4()).getPaths().get("/path/{foo}").get("get").getProduces(), "['c-produces-1','c-produces-2']");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}':{get:{produces:['a-produces']}}}"))
	public static class I5 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger(produces="c-produces-1, c-produces-2"))
		public X a() {
			return null;
		}
	}

	@Test void i05_operation_produces_swaggerOnAnnotation() throws Exception {
		assertJson(getSwagger(new I5()).getPaths().get("/path/{foo}").get("get").getProduces(), "['c-produces-1','c-produces-2']");
		assertJson(getSwaggerWithFile(new I5()).getPaths().get("/path/{foo}").get("get").getProduces(), "['c-produces-1','c-produces-2']");
	}

	@Rest(messages="BasicRestInfoProviderTest", swagger=@Swagger("paths:{'/path/{foo}':{get:{produces:['a-produces']}}}"))
	public static class I6 {
		@RestGet(path="/path/{foo}",swagger=@OpSwagger(produces="['$L{foo}']"))
		public X a() {
			return null;
		}
	}

	@Test void i06_operation_produces_swaggerOnAnnotation_localized() throws Exception {
		assertJson(getSwagger(new I6()).getPaths().get("/path/{foo}").get("get").getProduces(), "['l-foo']");
		assertJson(getSwaggerWithFile(new I6()).getPaths().get("/path/{foo}").get("get").getProduces(), "['l-foo']");
	}

	@Rest(serializers={JsonSerializer.class})
	public static class I7 {
		@RestPut(path="/path2/{foo}")
		public X a() {
			return null;
		}
	}

	@Test void i07_operation_produces_serializersOnClass() throws Exception {
		assertJson(getSwagger(new I7()).getPaths().get("/path2/{foo}").get("put").getProduces(), "null");
		assertJson(getSwaggerWithFile(new I7()).getPaths().get("/path2/{foo}").get("put").getProduces(), "null");
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
		assertJson(getSwagger(new I8()).getPaths().get("/path2/{foo}").get("put").getProduces(), "['text/xml']");
		assertJson(getSwaggerWithFile(new I8()).getPaths().get("/path2/{foo}").get("put").getProduces(), "['text/xml']");
	}

	@Rest(serializers={JsonSerializer.class},swagger=@Swagger("paths:{'/path2/{foo}':{put:{produces:['a-produces']}}}"))
	public static class I9 {
		@RestPut(path="/path2/{foo}",serializers={XmlSerializer.class})
		public X a() {
			return null;
		}
	}

	@Test void i09_operation_produces_serializersOnClassAndMethodWithSwagger() throws Exception {
		assertJson(getSwagger(new I9()).getPaths().get("/path2/{foo}").get("put").getProduces(), "['a-produces']");
		assertJson(getSwaggerWithFile(new I9()).getPaths().get("/path2/{foo}").get("put").getProduces(), "['a-produces']");
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
		assertJson(getSwagger(new J1()).getPaths().get("/path2/{foo}").get("get").getDeprecated(), "true");
		assertJson(getSwaggerWithFile(new J1()).getPaths().get("/path2/{foo}").get("get").getDeprecated(), "true");
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
		assertJson(getSwagger(new J2()).getPaths().get("/path2/{foo}").get("get").getDeprecated(), "true");  // NOSONAR
		assertJson(getSwaggerWithFile(new J2()).getPaths().get("/path2/{foo}").get("get").getDeprecated(), "true");  // NOSONAR
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
		ParameterInfo x = getSwagger(new K1()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo");
		assertEquals("object", x.getType());
		assertEquals(null, x.getDescription());
		assertEquals(null, x.getRequired());
		assertEquals(null, x.getAllowEmptyValue());
		assertEquals(null, x.getExclusiveMaximum());
		assertEquals(null, x.getExclusiveMinimum());
		assertEquals(null, x.getUniqueItems());
		assertEquals(null, x.getFormat());
		assertEquals(null, x.getCollectionFormat());
		assertEquals(null, x.getPattern());
		assertEquals(null, x.getMaximum());
		assertEquals(null, x.getMinimum());
		assertEquals(null, x.getMultipleOf());
		assertEquals(null, x.getMaxLength());
		assertEquals(null, x.getMinLength());
		assertEquals(null, x.getMaxItems());
		assertEquals(null, x.getMinItems());
	}
	@Test void k01b_query_type_default_withFile() throws Exception {
		ParameterInfo x = getSwaggerWithFile(new K1()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo");
		assertEquals("string", x.getType());
		assertEquals("s-description", x.getDescription());
		assertJson(x.getRequired(), "true");
		assertJson(x.getAllowEmptyValue(), "true");
		assertJson(x.getExclusiveMaximum(), "true");
		assertJson(x.getExclusiveMinimum(), "true");
		assertJson(x.getUniqueItems(), "true");
		assertEquals("s-format", x.getFormat());
		assertEquals("s-collectionFormat", x.getCollectionFormat());
		assertEquals("s-pattern", x.getPattern());
		assertJson(x.getMaximum(), "1.0");
		assertJson(x.getMinimum(), "1.0");
		assertJson(x.getMultipleOf(), "1.0");
		assertJson(x.getMaxLength(), "1");
		assertJson(x.getMinLength(), "1");
		assertJson(x.getMaxItems(), "1");
		assertJson(x.getMinItems(), "1");
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
		ParameterInfo x = getSwagger(new K2()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo");
		assertEquals("int32", x.getType());
		assertEquals("a-description", x.getDescription());
		assertJson(x.getRequired(), "false");
		assertJson(x.getAllowEmptyValue(), "false");
		assertJson(x.getExclusiveMaximum(), "false");
		assertJson(x.getExclusiveMinimum(), "false");
		assertJson(x.getUniqueItems(), "false");
		assertEquals("a-format", x.getFormat());
		assertEquals("a-collectionFormat", x.getCollectionFormat());
		assertEquals("a-pattern", x.getPattern());
		assertJson(x.getMaximum(), "2.0");
		assertJson(x.getMinimum(), "2.0");
		assertJson(x.getMultipleOf(), "2.0");
		assertJson(x.getMaxLength(), "2");
		assertJson(x.getMinLength(), "2");
		assertJson(x.getMaxItems(), "2");
		assertJson(x.getMinItems(), "2");
	}
	@Test void k02b_query_type_swaggerOnClass_withFile() throws Exception {
		ParameterInfo x = getSwaggerWithFile(new K2()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo");
		assertEquals("int32", x.getType());
		assertEquals("a-description", x.getDescription());
		assertJson(x.getRequired(), "false");
		assertJson(x.getAllowEmptyValue(), "false");
		assertJson(x.getExclusiveMaximum(), "false");
		assertJson(x.getExclusiveMinimum(), "false");
		assertJson(x.getUniqueItems(), "false");
		assertEquals("a-format", x.getFormat());
		assertEquals("a-collectionFormat", x.getCollectionFormat());
		assertEquals("a-pattern", x.getPattern());
		assertJson(x.getMaximum(), "2.0");
		assertJson(x.getMinimum(), "2.0");
		assertJson(x.getMultipleOf(), "2.0");
		assertJson(x.getMaxLength(), "2");
		assertJson(x.getMinLength(), "2");
		assertJson(x.getMaxItems(), "2");
		assertJson(x.getMinItems(), "2");
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
		ParameterInfo x = getSwagger(new K3()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo");
		assertEquals("int64", x.getType());
		assertEquals("b-description", x.getDescription());
		assertJson(x.getRequired(), "true");
		assertJson(x.getAllowEmptyValue(), "true");
		assertJson(x.getExclusiveMaximum(), "true");
		assertJson(x.getExclusiveMinimum(), "true");
		assertJson(x.getUniqueItems(), "true");
		assertEquals("b-format", x.getFormat());
		assertEquals("b-collectionFormat", x.getCollectionFormat());
		assertEquals("b-pattern", x.getPattern());
		assertJson(x.getMaximum(), "3.0");
		assertJson(x.getMinimum(), "3.0");
		assertJson(x.getMultipleOf(), "3.0");
		assertJson(x.getMaxLength(), "3");
		assertJson(x.getMinLength(), "3");
		assertJson(x.getMaxItems(), "3");
		assertJson(x.getMinItems(), "3");
	}
	@Test void k03b_query_type_swaggerOnMethod_withFile() throws Exception {
		ParameterInfo x = getSwaggerWithFile(new K3()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo");
		assertEquals("int64", x.getType());
		assertEquals("b-description", x.getDescription());
		assertJson(x.getRequired(), "true");
		assertJson(x.getAllowEmptyValue(), "true");
		assertJson(x.getExclusiveMaximum(), "true");
		assertJson(x.getExclusiveMinimum(), "true");
		assertJson(x.getUniqueItems(), "true");
		assertEquals("b-format", x.getFormat());
		assertEquals("b-collectionFormat", x.getCollectionFormat());
		assertEquals("b-pattern", x.getPattern());
		assertJson(x.getMaximum(), "3.0");
		assertJson(x.getMinimum(), "3.0");
		assertJson(x.getMultipleOf(), "3.0");
		assertJson(x.getMaxLength(), "3");
		assertJson(x.getMinLength(), "3");
		assertJson(x.getMaxItems(), "3");
		assertJson(x.getMinItems(), "3");
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
		assertJson(getSwagger(new N1()).getPaths().get("/path/{foo}/query").get("get").getParameter("query","foo").getSchema(), "{properties:{a:{format:'int32',type:'integer'}}}");
		assertJson(getSwaggerWithFile(new N1()).getPaths().get("/path/{foo}/query").get("get").getParameter("query","foo").getSchema(), "{'$ref':'#/definitions/Foo'}");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}/query':{get:{parameters:[{in:'query',name:'foo',schema:{$ref:'b'}}]}}}"))
	public static class N2 {
		@RestGet(path="/path/{foo}/query")
		public X a(@Query("foo") X foo) {
			return null;
		}
	}

	@Test void n02_query_schema_swaggerOnClass() throws Exception {
		assertJson(getSwagger(new N2()).getPaths().get("/path/{foo}/query").get("get").getParameter("query","foo").getSchema(), "{'$ref':'b'}");
		assertJson(getSwaggerWithFile(new N2()).getPaths().get("/path/{foo}/query").get("get").getParameter("query","foo").getSchema(), "{'$ref':'b'}");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}/query':{get:{parameters:[{in:'query',name:'foo',schema:{$ref:'b'}}]}}}"))
	public static class N3 {

		@RestGet(path="/path/{foo}/query",swagger=@OpSwagger("parameters:[{'in':'query',name:'foo',schema:{$ref:'c'}}]"))
		public X a() {
			return null;
		}
	}

	@Test void n03_query_schema_swaggerOnMethnt() throws Exception {
		assertJson(getSwagger(new N3()).getPaths().get("/path/{foo}/query").get("get").getParameter("query","foo").getSchema(), "{'$ref':'c'}");
		assertJson(getSwaggerWithFile(new N3()).getPaths().get("/path/{foo}/query").get("get").getParameter("query","foo").getSchema(), "{'$ref':'c'}");
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
		assertEquals("Continue", getSwagger(new O1a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
		assertEquals("s-100-description", getSwaggerWithFile(new O1a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
	}
	@Test void o01b_responses_100_description_default() throws Exception {
		assertEquals("Continue", getSwagger(new O1b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
		assertEquals("s-100-description", getSwaggerWithFile(new O1b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{description:'a-100-description'}}}}}"))
	public static class O2 {
		@RestGet(path="/path/{foo}/responses/100")
		public void a(@StatusCode Value<Integer> foo) {/* no-op */}
	}

	@Test void o02_response_100_description_swaggerOnClass() throws Exception {
		assertEquals("a-100-description", getSwagger(new O2()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
		assertEquals("a-100-description", getSwaggerWithFile(new O2()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{description:'a-100-description'}}}}}"))
	public static class O3 {
		@RestGet(path="/path/{foo}/responses/100",swagger=@OpSwagger("responses:{100:{description:'b-100-description'}}"))
		public void a(@StatusCode Value<Integer> foo) {/* no-op */}
	}

	@Test void o03_response_100_description_swaggerOnMethod() throws Exception {
		assertEquals("b-100-description", getSwagger(new O3()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
		assertEquals("b-100-description", getSwaggerWithFile(new O3()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
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
		assertEquals("c-100-description", getSwagger(new O4a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
		assertEquals("c-100-description", getSwaggerWithFile(new O4a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
	}
	@Test void o04b_response_100_description_swaggerOnAnnotation() throws Exception {
		assertEquals("c-100-description", getSwagger(new O4b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
		assertEquals("c-100-description", getSwaggerWithFile(new O4b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
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
		assertEquals("l-foo", getSwagger(new O5a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
		assertEquals("l-foo", getSwaggerWithFile(new O5a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
	}
	@Test void o05b_response_100_description_swaggerOnAnnotation_localized() throws Exception {
		assertEquals("l-foo", getSwagger(new O5b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
		assertEquals("l-foo", getSwaggerWithFile(new O5b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
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
		assertEquals(null, getSwagger(new P1a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
		assertJson(getSwaggerWithFile(new P1a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "{'X-Foo':{description:'s-description',type:'integer',format:'int32'}}");
	}
	@Test void p01b_responses_100_headers_default() throws Exception {
		assertEquals(null, getSwagger(new P1b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
		assertJson(getSwaggerWithFile(new P1b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "{'X-Foo':{description:'s-description',type:'integer',format:'int32'}}");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{headers:{'X-Foo':{description:'b-description',type:'integer',format:'int32'}}}}}}}"))
	public static class P2 {
		@RestGet(path="/path/{foo}/responses/100")
		public X a(@StatusCode Value<Integer> foo) {
			return null;
		}
	}

	@Test void p02_response_100_headers_swaggerOnClass() throws Exception {
		assertJson(getSwagger(new P2()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "{'X-Foo':{description:'b-description',type:'integer',format:'int32'}}");
		assertJson(getSwaggerWithFile(new P2()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "{'X-Foo':{description:'b-description',type:'integer',format:'int32'}}");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{headers:{'X-Foo':{description:'b-description',type:'integer',format:'int32'}}}}}}}"))
	public static class P3 {
		@RestGet(path="/path/{foo}/responses/100",swagger=@OpSwagger("responses:{100:{headers:{'X-Foo':{description:'c-description',type:'integer',format:'int32'}}}}"))
		public X a(@StatusCode Value<Integer> foo) {
			return null;
		}
	}

	@Test void p03_response_100_headers_swaggerOnMethod() throws Exception {
		assertJson(getSwagger(new P3()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "{'X-Foo':{description:'c-description',type:'integer',format:'int32'}}");
		assertJson(getSwaggerWithFile(new P3()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "{'X-Foo':{description:'c-description',type:'integer',format:'int32'}}");
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
		assertJson(getSwagger(new P4a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "{'X-Foo':{description:'d-description',type:'integer',format:'int32'}}");
		assertJson(getSwaggerWithFile(new P4a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "{'X-Foo':{description:'d-description',type:'integer',format:'int32'}}");
	}
	@Test void p04b_response_100_headers_swaggerOnAnnotation() throws Exception {
		assertJson(getSwagger(new P4b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "{'X-Foo':{description:'d-description',type:'integer',format:'int32'}}");
		assertJson(getSwaggerWithFile(new P4b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "{'X-Foo':{description:'d-description',type:'integer',format:'int32'}}");
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
		assertJson(getSwagger(new P5a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "{'X-Foo':{description:'l-foo',type:'integer',format:'int32'}}");
		assertJson(getSwaggerWithFile(new P5a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "{'X-Foo':{description:'l-foo',type:'integer',format:'int32'}}");
	}
	@Test void p05b_response_100_headers_swaggerOnAnnotation_localized() throws Exception {
		assertJson(getSwagger(new P5b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "{'X-Foo':{description:'l-foo',type:'integer',format:'int32'}}");
		assertJson(getSwaggerWithFile(new P5b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders(), "{'X-Foo':{description:'l-foo',type:'integer',format:'int32'}}");
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
		assertEquals(null, getSwagger(new R1a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
		assertJson(getSwaggerWithFile(new R1a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "{foo:'a'}");
	}
	@Test void r01b_responses_100_examples_default() throws Exception {
		assertEquals(null, getSwagger(new R1b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
		assertJson(getSwaggerWithFile(new R1b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "{foo:'a'}");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{examples:{foo:{bar:'b'}}}}}}}"))
	public static class R2 {
		@RestGet(path="/path/{foo}/responses/100")
		public void a(@StatusCode Value<Integer> foo) {/* no-op */}
	}

	@Test void r02_response_100_examples_swaggerOnClass() throws Exception {
		assertJson(getSwagger(new R2()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "{foo:{bar:'b'}}");
		assertJson(getSwaggerWithFile(new R2()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "{foo:{bar:'b'}}");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{examples:{foo:{bar:'b'}}}}}}}"))
	public static class R3 {
		@RestGet(path="/path/{foo}/responses/100",swagger=@OpSwagger("responses:{100:{examples:{foo:{bar:'c'}}}}"))
		public void a(@StatusCode Value<Integer> foo) {/* no-op */}
	}

	@Test void r03_response_100_examples_swaggerOnMethod() throws Exception {
		assertJson(getSwagger(new R3()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "{foo:{bar:'c'}}");
		assertJson(getSwaggerWithFile(new R3()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "{foo:{bar:'c'}}");
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
		assertJson(getSwagger(new R4a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "{foo:{bar:'d'}}");
		assertJson(getSwaggerWithFile(new R4a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "{foo:{bar:'d'}}");
	}
	@Test void r04b_response_100_examples_swaggerOnAnnotation() throws Exception {
		assertJson(getSwagger(new R4b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "{foo:{bar:'d'}}");
		assertJson(getSwaggerWithFile(new R4b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "{foo:{bar:'d'}}");
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
		assertJson(getSwagger(new R5a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "{foo:{bar:'l-foo'}}");
		assertJson(getSwaggerWithFile(new R5a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "{foo:{bar:'l-foo'}}");
	}
	@Test void r05b_response_100_examples_swaggerOnAnnotation_lodalized() throws Exception {
		assertJson(getSwagger(new R5b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "{foo:{bar:'l-foo'}}");
		assertJson(getSwaggerWithFile(new R5b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples(), "{foo:{bar:'l-foo'}}");
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
		assertJson(getSwagger(new S1a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "{type:'object',properties:{a:{format:'int32',type:'integer'}}}");
		assertJson(getSwaggerWithFile(new S1a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "{type:'array',items:{'$ref':'#/definitions/Foo'}}");
	}
	@Test void s01b_responses_100_schema_default() throws Exception {
		assertJson(getSwagger(new S1b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "{type:'object',properties:{a:{format:'int32',type:'integer'}}}");
		assertJson(getSwaggerWithFile(new S1b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "{type:'array',items:{'$ref':'#/definitions/Foo'}}");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{schema:{$ref:'b'}}}}}}"))
	public static class S2 {
		@RestGet(path="/path/{foo}/responses/100")
		public void a(@StatusCode Value<Integer> foo) {/* no-op */}
	}

	@Test void s02_response_100_schema_swaggerOnClass() throws Exception {
		assertJson(getSwagger(new S2()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "{'$ref':'b'}");
		assertJson(getSwaggerWithFile(new S2()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "{'$ref':'b'}");
	}

	@Rest(swagger=@Swagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{schema:{$ref:'b'}}}}}}"))
	public static class S3 {
		@RestGet(path="/path/{foo}/responses/100",swagger=@OpSwagger("responses:{100:{schema:{$ref:'c'}}}}"))
		public void a(@StatusCode Value<Integer> foo) {/* no-op */}
	}

	@Test void s03_response_100_schema_swaggerOnMethoe() throws Exception {
		assertJson(getSwagger(new S3()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "{'$ref':'c'}");
		assertJson(getSwaggerWithFile(new S3()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "{'$ref':'c'}");
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
		assertJson(getSwagger(new S4a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "{'$ref':'d'}");
		assertJson(getSwaggerWithFile(new S4a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "{'$ref':'d'}");
	}
	@Test void s04b_response_100_schema_swaggerOnAnnotation() throws Exception {
		assertJson(getSwagger(new S4b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "{'$ref':'d'}");
		assertJson(getSwaggerWithFile(new S4b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "{'$ref':'d'}");
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
		assertJson(getSwagger(new S5a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "{'$ref':'l-foo'}");
		assertJson(getSwaggerWithFile(new S5a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "{'$ref':'l-foo'}");
	}
	@Test void s05b_response_100_schema_swaggerOnAnnotation_loealized() throws Exception {
		assertJson(getSwagger(new S5b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "{'$ref':'l-foo'}");
		assertJson(getSwaggerWithFile(new S5b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema(), "{'$ref':'l-foo'}");
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

		public T2 setF1(int f1) {
			this.f1 = f1;
			return this;
		}

		public int getF1() {
			return f1;
		}

		public int getF2() {
			return 2;
		}

		@Example
		public static T2 example() {
			return new T2().setF1(1);
		}
	}

	@Test void t01_bodyWithReadOnlyProperty() throws Exception {
		MockRestClient p = MockRestClient.build(T1.class);
		org.apache.juneau.bean.swagger.Swagger s = JsonParser.DEFAULT.parse(p.get("/api").accept("application/json").run().getContent().asString(), org.apache.juneau.bean.swagger.Swagger.class);
		Operation o = s.getOperation("/", "get");

		ResponseInfo ri = o.getResponse("200");
		assertEquals("{\n\tf1: 1,\n\tf2: 2\n}", ri.getExamples().get("application/json5"));
	}
}