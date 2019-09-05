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

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.apache.juneau.http.HttpMethodName.*;
import static org.apache.juneau.rest.testutils.TestUtils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.annotation.ExternalDocs;
import org.apache.juneau.jsonschema.annotation.Schema;
import org.apache.juneau.xml.*;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.Contact;
import org.apache.juneau.http.annotation.License;
import org.apache.juneau.http.annotation.Query;
import org.apache.juneau.http.annotation.Response;
import org.apache.juneau.http.annotation.Tag;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.utils.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests the {@link BasicRestInfoProvider} class.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BasicRestInfoProviderTest {

	//=================================================================================================================
	// Setup
	//=================================================================================================================

	private Swagger getSwaggerWithFile(Object resource) throws Exception {
		RestContext rc = RestContext.create(resource).classpathResourceFinder(TestClasspathResourceFinder.class).build();
		RestRequest req = rc.getCallHandler().createRequest(new RestCall(new MockServletRequest(), null));
		RestInfoProvider ip = rc.getInfoProvider();
		return ip.getSwagger(req);
	}

	private static Swagger getSwagger(Object resource) throws Exception {
		RestContext rc = RestContext.create(resource).build();
		RestRequest req = rc.getCallHandler().createRequest(new RestCall(new MockServletRequest(), null));
		RestInfoProvider ip = rc.getInfoProvider();
		return ip.getSwagger(req);
	}

	public static class TestClasspathResourceFinder extends ClasspathResourceFinderBasic {
		@Override
		public InputStream findResource(Class<?> baseClass, String name, Locale locale) throws IOException {
			if (name.endsWith(".json"))
				return BasicRestInfoProvider.class.getResourceAsStream("BasicRestInfoProviderTest_swagger.json");
			return super.findResource(baseClass, name, locale);
		}
	}

	//=================================================================================================================
	// /<root>
	//=================================================================================================================

	@RestResource
	public static class A01 {}

	@Test
	public void a01_swagger_default() throws Exception {
		Swagger x = getSwagger(new A01());
		assertEquals("2.0", x.getSwagger());
		assertEquals(null, x.getHost());
		assertEquals(null, x.getBasePath());
		assertEquals(null, x.getSchemes());
	}
	@Test
	public void a01_swagger_default_withFile() throws Exception {
		Swagger x = getSwaggerWithFile(new A01());
		assertEquals("0.0", x.getSwagger());
		assertEquals("s-host", x.getHost());
		assertEquals("s-basePath", x.getBasePath());
		assertObjectEquals("['s-scheme']", x.getSchemes());
	}


	@RestResource(swagger=@ResourceSwagger("{swagger:'3.0',host:'a-host',basePath:'a-basePath',schemes:['a-scheme']}"))
	public static class A02 {}

	@Test
	public void a02_swagger_ResourceSwagger_value() throws Exception {
		Swagger x = getSwagger(new A02());
		assertEquals("3.0", x.getSwagger());
		assertEquals("a-host", x.getHost());
		assertEquals("a-basePath", x.getBasePath());
		assertObjectEquals("['a-scheme']", x.getSchemes());
	}
	@Test
	public void a02_swagger_ResourceSwagger_value_withFile() throws Exception {
		Swagger x = getSwaggerWithFile(new A02());
		assertEquals("3.0", x.getSwagger());
		assertEquals("a-host", x.getHost());
		assertEquals("a-basePath", x.getBasePath());
		assertObjectEquals("['a-scheme']", x.getSchemes());
	}

	//=================================================================================================================
	// /info
	//=================================================================================================================

	@RestResource(
		title="a-title",
		description="a-description"
	)
	public static class B01 {}

	@Test
	public void b01a_info_RestResource() throws Exception {
		Info x = getSwagger(new B01()).getInfo();
		assertEquals("a-title", x.getTitle());
		assertEquals("a-description", x.getDescription());
		assertEquals(null, x.getVersion());
		assertEquals(null, x.getTermsOfService());
		assertEquals(null, x.getContact());
		assertEquals(null, x.getLicense());
	}
	@Test
	public void b01b_info_RestResource_withFile() throws Exception {
		Info x = getSwaggerWithFile(new B01()).getInfo();
		assertEquals("s-title", x.getTitle());
		assertEquals("s-description", x.getDescription());
		assertEquals("0.0.0", x.getVersion());
		assertEquals("s-termsOfService", x.getTermsOfService());
		assertObjectEquals("{name:'s-name',url:'s-url',email:'s-email'}", x.getContact());
		assertObjectEquals("{name:'s-name',url:'s-url'}", x.getLicense());
	}

	@RestResource(
		messages="BasicRestInfoProviderTest",
		title="$L{foo}",
		description="$L{foo}"
	)
	public static class B02 {}

	@Test
	public void b02a_info_RestResource_localized() throws Exception {
		Info x = getSwagger(new B02()).getInfo();
		assertEquals("l-foo", x.getTitle());
		assertEquals("l-foo", x.getDescription());
	}
	@Test
	public void b02b_info_RestResource_localized_withFile() throws Exception {
		Info x = getSwaggerWithFile(new B02()).getInfo();
		assertEquals("s-title", x.getTitle());
		assertEquals("s-description", x.getDescription());
	}

	@RestResource(
		title="a-title",
		description="a-description",
		swagger=@ResourceSwagger(
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
	public static class B03 {}

	@Test
	public void b03a_info_ResourceSwagger_value() throws Exception {
		Info x = getSwagger(new B03()).getInfo();
		assertEquals("b-title", x.getTitle());
		assertEquals("b-description", x.getDescription());
		assertEquals("2.0.0", x.getVersion());
		assertEquals("a-termsOfService", x.getTermsOfService());
		assertObjectEquals("{name:'a-name',url:'a-url',email:'a-email'}", x.getContact());
		assertObjectEquals("{name:'a-name',url:'a-url'}", x.getLicense());
	}
	@Test
	public void b03b_info_ResourceSwagger_value_withFile() throws Exception {
		Info x = getSwaggerWithFile(new B03()).getInfo();
		assertEquals("b-title", x.getTitle());
		assertEquals("b-description", x.getDescription());
		assertEquals("2.0.0", x.getVersion());
		assertEquals("a-termsOfService", x.getTermsOfService());
		assertObjectEquals("{name:'a-name',url:'a-url',email:'a-email'}", x.getContact());
		assertObjectEquals("{name:'a-name',url:'a-url'}", x.getLicense());
	}

	@RestResource(
		messages="BasicRestInfoProviderTest",
		title="a-title",
		description="a-description",
		swagger=@ResourceSwagger("{info:{title:'$L{bar}',description:'$L{bar}'}}")
	)
	public static class B04 {}

	@Test
	public void b04_info_ResourceSwagger_value_localised() throws Exception {
		assertEquals("l-bar", getSwagger(new B04()).getInfo().getTitle());
		assertEquals("l-bar", getSwaggerWithFile(new B04()).getInfo().getTitle());
		assertEquals("l-bar", getSwagger(new B04()).getInfo().getDescription());
		assertEquals("l-bar", getSwaggerWithFile(new B04()).getInfo().getDescription());
	}

	@RestResource(
		title="a-title",
		description="a-description",
		swagger=@ResourceSwagger(
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
	public static class B05 {}

	@Test
	public void b05a_info_ResourceSwagger_title() throws Exception {
		Info x = getSwagger(new B05()).getInfo();
		assertEquals("c-title", x.getTitle());
		assertEquals("c-description", x.getDescription());
		assertEquals("3.0.0", x.getVersion());
		assertEquals("b-termsOfService", x.getTermsOfService());
		assertObjectEquals("{name:'b-name',url:'b-url',email:'b-email'}", x.getContact());
		assertObjectEquals("{name:'b-name',url:'b-url'}", x.getLicense());
	}
	@Test
	public void b05b_info_ResourceSwagger_title_withFile() throws Exception {
		Info x = getSwaggerWithFile(new B05()).getInfo();
		assertEquals("c-title", x.getTitle());
		assertEquals("c-description", x.getDescription());
		assertEquals("3.0.0", x.getVersion());
		assertEquals("b-termsOfService", x.getTermsOfService());
		assertObjectEquals("{name:'b-name',url:'b-url',email:'b-email'}", x.getContact());
		assertObjectEquals("{name:'b-name',url:'b-url'}", x.getLicense());
	}

	@RestResource(
		title="a-title",
		description="a-description",
		swagger=@ResourceSwagger(
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
			contact=@Contact("{name:'$L{foo}',url:'$L{bar}',email:'$L{baz}'}"),
			license=@License("{name:'$L{foo}',url:'$L{bar}'}")
		),
		messages="BasicRestInfoProviderTest"
	)
	public static class B06 {}

	@Test
	public void b06a_info_ResourceSwagger_title_localized() throws Exception {
		Info x = getSwagger(new B06()).getInfo();
		assertEquals("l-baz", x.getTitle());
		assertEquals("l-baz", x.getDescription());
		assertEquals("l-foo", x.getVersion());
		assertEquals("l-foo", x.getTermsOfService());
		assertObjectEquals("{name:'l-foo',url:'l-bar',email:'l-baz'}", x.getContact());
		assertObjectEquals("{name:'l-foo',url:'l-bar'}", x.getLicense());
	}
	@Test
	public void b06b_info_ResourceSwagger_title_localized_withFile() throws Exception {
		Info x = getSwaggerWithFile(new B06()).getInfo();
		assertEquals("l-baz", x.getTitle());
		assertEquals("l-baz", x.getDescription());
		assertEquals("l-foo", x.getVersion());
		assertEquals("l-foo", x.getTermsOfService());
		assertObjectEquals("{name:'l-foo',url:'l-bar',email:'l-baz'}", x.getContact());
		assertObjectEquals("{name:'l-foo',url:'l-bar'}", x.getLicense());
	}

	@RestResource(
		swagger=@ResourceSwagger(
			title="c-title",
			description="c-description"
		)
	)
	public static class B07 {}

	@Test
	public void b07a_title_ResourceSwagger_title_only() throws Exception {
		Info x = getSwagger(new B07()).getInfo();
		assertEquals("c-title", x.getTitle());
		assertEquals("c-description", x.getDescription());
	}
	@Test
	public void b07b_title_ResourceSwagger_title_only_withFile() throws Exception {
		Info x = getSwaggerWithFile(new B07()).getInfo();
		assertEquals("c-title", x.getTitle());
		assertEquals("c-description", x.getDescription());
	}

	//=================================================================================================================
	// /tags
	//=================================================================================================================

	@RestResource
	public static class C01 {}

	@Test
	public void c01a_tags_default() throws Exception {
		Swagger x = getSwagger(new C01());
		assertEquals(null, x.getTags());
	}
	@Test
	public void c01b_tags_default_withFile() throws Exception {
		Swagger x = getSwaggerWithFile(new C01());
		assertObjectEquals("[{name:'s-name',description:'s-description',externalDocs:{description:'s-description',url:'s-url'}}]", x.getTags());
	}

	// Tags in @ResourceSwagger(value) should override file.
	@RestResource(
		swagger=@ResourceSwagger(
			"{tags:[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}}]}"
		)
	)
	public static class C02 {}

	@Test
	public void c02a_tags_ResourceSwagger_value() throws Exception {
		Swagger x = getSwagger(new C02());
		assertObjectEquals("[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}}]", x.getTags());
	}
	@Test
	public void c02b_tags_ResourceSwagger_value_withFile() throws Exception {
		Swagger x = getSwaggerWithFile(new C02());
		assertObjectEquals("[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}}]", x.getTags());
	}

	// Tags in both @ResourceSwagger(value) and @ResourceSwagger(tags) should accumulate.
	@RestResource(
		swagger=@ResourceSwagger(
			value="{tags:[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}}]}",
			tags=@Tag(name="b-name",description="b-description",externalDocs=@ExternalDocs(description="b-description",url="b-url"))
		)
	)
	public static class C03 {}

	@Test
	public void c03a_tags_ResourceSwagger_tags() throws Exception {
		Swagger x = getSwagger(new C03());
		assertObjectEquals("[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}},{name:'b-name',description:'b-description',externalDocs:{description:'b-description',url:'b-url'}}]", x.getTags());
	}
	@Test
	public void c03b_tags_ResourceSwagger_tags_withFile() throws Exception {
		Swagger x = getSwaggerWithFile(new C03());
		assertObjectEquals("[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}},{name:'b-name',description:'b-description',externalDocs:{description:'b-description',url:'b-url'}}]", x.getTags());
	}

	// Same as above but without [] outer characters.
	@RestResource(
		swagger=@ResourceSwagger(
			value="{tags:[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}}]}",
			tags=@Tag(name="b-name",value=" { description:'b-description', externalDocs: { description:'b-description', url:'b-url' } } ")
		)
	)
	public static class C04 {}

	@Test
	public void c04a_tags_ResourceSwagger_tags() throws Exception {
		Swagger x = getSwagger(new C04());
		assertObjectEquals("[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}},{name:'b-name',description:'b-description',externalDocs:{description:'b-description',url:'b-url'}}]", x.getTags());
	}
	@Test
	public void c04b_tags_ResourceSwagger_tags_withFile() throws Exception {
		Swagger x = getSwaggerWithFile(new C04());
		assertObjectEquals("[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}},{name:'b-name',description:'b-description',externalDocs:{description:'b-description',url:'b-url'}}]", x.getTags());
	}

	// Tags in both Swagger.json and @ResourceSwagger(tags) should accumulate.
	@RestResource(
		swagger=@ResourceSwagger(
			tags=@Tag(name="b-name",description="b-description",externalDocs=@ExternalDocs(description="b-description",url="b-url"))
		)
	)
	public static class C05 {}

	@Test
	public void c05a_tags_ResourceSwagger_tags_only() throws Exception {
		Swagger x = getSwagger(new C05());
		assertObjectEquals("[{name:'b-name',description:'b-description',externalDocs:{description:'b-description',url:'b-url'}}]", x.getTags());
	}
	@Test
	public void c05b_tags_ResourceSwagger_tags_only_witFile() throws Exception {
		Swagger x = getSwaggerWithFile(new C05());
		assertObjectEquals("[{name:'s-name',description:'s-description',externalDocs:{description:'s-description',url:'s-url'}},{name:'b-name',description:'b-description',externalDocs:{description:'b-description',url:'b-url'}}]", x.getTags());
	}

	// Dup tag names should be overwritten
	@RestResource(
		swagger=@ResourceSwagger(
			tags={
				@Tag(name="s-name",description="b-description",externalDocs=@ExternalDocs(description="b-description",url="b-url")),
				@Tag(name="s-name",value="{description:'c-description',externalDocs:{description:'c-description',url:'c-url'}}")
			}
		)
	)
	public static class C06 {}

	@Test
	public void c06a_tags_ResourceSwagger_tags_dups() throws Exception {
		Swagger x = getSwagger(new C06());
		assertObjectEquals("[{name:'s-name',description:'c-description',externalDocs:{description:'c-description',url:'c-url'}}]", x.getTags());
	}
	@Test
	public void c06b_tags_ResourceSwagger_tags_dups_withFile() throws Exception {
		Swagger x = getSwaggerWithFile(new C06());
		assertObjectEquals("[{name:'s-name',description:'c-description',externalDocs:{description:'c-description',url:'c-url'}}]", x.getTags());
	}

	@RestResource(
		swagger=@ResourceSwagger(
			value="{tags:[{name:'$L{foo}',description:'$L{foo}',externalDocs:{description:'$L{foo}',url:'$L{foo}'}}]}",
			tags=@Tag(name="$L{foo}",description="$L{foo}",externalDocs=@ExternalDocs(description="$L{foo}",url="$L{foo}"))
		),
		messages="BasicRestInfoProviderTest"
	)
	public static class C07 {}

	@Test
	public void c07a_tags_ResourceSwagger_tags_localised() throws Exception {
		Swagger x = getSwagger(new C07());
		assertObjectEquals("[{name:'l-foo',description:'l-foo',externalDocs:{description:'l-foo',url:'l-foo'}}]", x.getTags());
	}
	@Test
	public void c07b_tags_ResourceSwagger_tags_localised_withFile() throws Exception {
		Swagger x = getSwaggerWithFile(new C07());
		assertObjectEquals("[{name:'l-foo',description:'l-foo',externalDocs:{description:'l-foo',url:'l-foo'}}]", x.getTags());
	}

	// Auto-detect tags defined on methods.
	@RestResource
	public static class C08 {
		@RestMethod(swagger=@MethodSwagger(tags="foo"))
		public void doFoo() {}
	}

	@Test
	public void c08a_tags_ResourceSwagger_tags_loose() throws Exception {
		Swagger x = getSwagger(new C08());
		assertObjectEquals("[{name:'foo'}]", x.getTags());
	}
	@Test
	public void c08b_tags_ResourceSwagger_tags_loose_withFile() throws Exception {
		Swagger x = getSwaggerWithFile(new C08());
		assertObjectEquals("[{name:'s-name',description:'s-description',externalDocs:{description:'s-description',url:'s-url'}},{name:'foo'}]", x.getTags());
	}

	// Comma-delimited list
	@RestResource
	public static class C09 {
		@RestMethod(swagger=@MethodSwagger(tags=" foo, bar "))
		public void doFoo() {}
	}

	@Test
	public void c09a_tags_ResourceSwagger_tags_loose_cdl() throws Exception {
		Swagger x = getSwagger(new C09());
		assertObjectEquals("[{name:'foo'},{name:'bar'}]", x.getTags());
	}
	@Test
	public void c09b_tags_ResourceSwagger_tags_loose_cdl_withFile() throws Exception {
		Swagger x = getSwaggerWithFile(new C09());
		assertObjectEquals("[{name:'s-name',description:'s-description',externalDocs:{description:'s-description',url:'s-url'}},{name:'foo'},{name:'bar'}]", x.getTags());
	}

	// ObjectList
	@RestResource
	public static class C10 {
		@RestMethod(swagger=@MethodSwagger(tags="['foo', 'bar']"))
		public void doFoo() {}
	}

	@Test
	public void c10a_tags_ResourceSwagger_tags_loose_objectlist() throws Exception {
		Swagger x = getSwagger(new C10());
		assertObjectEquals("[{name:'foo'},{name:'bar'}]", x.getTags());
	}
	@Test
	public void c10b_tags_ResourceSwagger_tags_loose_objectlist_withFile() throws Exception {
		Swagger x = getSwaggerWithFile(new C10());
		assertObjectEquals("[{name:'s-name',description:'s-description',externalDocs:{description:'s-description',url:'s-url'}},{name:'foo'},{name:'bar'}]", x.getTags());
	}

	// ObjectList localized
	@RestResource(messages="BasicRestInfoProviderTest")
	public static class C11 {
		@RestMethod(swagger=@MethodSwagger(tags="['$L{foo}', '$L{bar}']"))
		public void doFoo() {}
	}

	@Test
	public void c11a_tags_ResourceSwagger_tags_loose_objectlist_localized() throws Exception {
		Swagger x = getSwagger(new C11());
		assertObjectEquals("[{name:'l-foo'},{name:'l-bar'}]", x.getTags());
	}
	@Test
	public void c11b_tags_ResourceSwagger_tags_loose_objectlist_localized_withFile() throws Exception {
		Swagger x = getSwaggerWithFile(new C11());
		assertObjectEquals("[{name:'s-name',description:'s-description',externalDocs:{description:'s-description',url:'s-url'}},{name:'l-foo'},{name:'l-bar'}]", x.getTags());
	}

	// Comma-delimited list localized
	@RestResource(messages="BasicRestInfoProviderTest")
	public static class C12 {
		@RestMethod(swagger=@MethodSwagger(tags=" $L{foo}, $L{bar} "))
		public void doFoo() {}
	}

	@Test
	public void c12a_tags_ResourceSwagger_tags_loose_cdl_localized() throws Exception {
		Swagger x = getSwagger(new C12());
		assertObjectEquals("[{name:'l-foo'},{name:'l-bar'}]", x.getTags());
	}
	@Test
	public void c12b_tags_ResourceSwagger_tags_loose_cdl_localized_withFile() throws Exception {
		Swagger x = getSwaggerWithFile(new C12());
		assertObjectEquals("[{name:'s-name',description:'s-description',externalDocs:{description:'s-description',url:'s-url'}},{name:'l-foo'},{name:'l-bar'}]", x.getTags());
	}

	//=================================================================================================================
	// /externalDocs
	//=================================================================================================================

	@RestResource
	public static class D01 {}

	@Test
	public void d01a_externalDocs_default() throws Exception {
		ExternalDocumentation x = getSwagger(new D01()).getExternalDocs();
		assertEquals(null, x);
	}
	@Test
	public void d01b_externalDocs_default_withFile() throws Exception {
		ExternalDocumentation x = getSwaggerWithFile(new D01()).getExternalDocs();
		assertObjectEquals("{description:'s-description',url:'s-url'}", x);
	}


	@RestResource(
		swagger=@ResourceSwagger("{externalDocs:{description:'a-description',url:'a-url'}}")
	)
	public static class D02 {}

	@Test
	public void d02a_externalDocs_ResourceSwagger_value() throws Exception {
		ExternalDocumentation x = getSwagger(new D02()).getExternalDocs();
		assertObjectEquals("{description:'a-description',url:'a-url'}", x);
	}
	@Test
	public void d02b_externalDocs_ResourceSwagger_value_withFile() throws Exception {
		ExternalDocumentation x = getSwaggerWithFile(new D02()).getExternalDocs();
		assertObjectEquals("{description:'a-description',url:'a-url'}", x);
	}


	@RestResource(
		swagger=@ResourceSwagger(
			value="{externalDocs:{description:'a-description',url:'a-url'}}",
			externalDocs=@ExternalDocs(description="b-description",url="b-url")
		)
	)
	public static class D03 {}

	@Test
	public void d03a_externalDocs_ResourceSwagger_externalDocs() throws Exception {
		ExternalDocumentation x = getSwagger(new D03()).getExternalDocs();
		assertObjectEquals("{description:'b-description',url:'b-url'}", x);
	}
	@Test
	public void d03b_externalDocs_ResourceSwagger_externalDocs_withFile() throws Exception {
		ExternalDocumentation x = getSwaggerWithFile(new D03()).getExternalDocs();
		assertObjectEquals("{description:'b-description',url:'b-url'}", x);
	}

	@RestResource(
		swagger=@ResourceSwagger(
			value="{info:{externalDocs:{description:'a-description',url:'a-url'}}}",
			externalDocs=@ExternalDocs(" description:'b-description', url:'b-url' ")
			)
	)
	public static class D04 {}

	@Test
	public void d04a_externalDocs_ResourceSwagger_externalDocs() throws Exception {
		ExternalDocumentation x = getSwagger(new D04()).getExternalDocs();
		assertObjectEquals("{description:'b-description',url:'b-url'}", x);
	}
	@Test
	public void d04b_externalDocs_ResourceSwagger_externalDocs_withFile() throws Exception {
		ExternalDocumentation x = getSwaggerWithFile(new D04()).getExternalDocs();
		assertObjectEquals("{description:'b-description',url:'b-url'}", x);
	}

	@RestResource(
		swagger=@ResourceSwagger(
			value="{externalDocs:{description:'a-description',url:'a-url'}}",
			externalDocs=@ExternalDocs("{description:'$L{foo}',url:'$L{bar}'}")
		),
		messages="BasicRestInfoProviderTest"
	)
	public static class D05 {}

	@Test
	public void d05a_externalDocs_ResourceSwagger_externalDocs_localised() throws Exception {
		ExternalDocumentation x = getSwagger(new D05()).getExternalDocs();
		assertObjectEquals("{description:'l-foo',url:'l-bar'}", x);
	}
	@Test
	public void d05b_externalDocs_ResourceSwagger_externalDocs_localised_withFile() throws Exception {
		ExternalDocumentation x = getSwaggerWithFile(new D05()).getExternalDocs();
		assertObjectEquals("{description:'l-foo',url:'l-bar'}", x);
	}

	//=================================================================================================================
	// /paths/<path>/<method>
	//=================================================================================================================

	@RestResource
	public static class E01 {
		@RestMethod(name=GET,path="/path/{foo}")
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void e01a_operation_summary_default() throws Exception {
		Operation x = getSwagger(new E01()).getPaths().get("/path/{foo}").get("get");
		assertEquals("doFoo", x.getOperationId());
		assertEquals(null, x.getSummary());
		assertEquals(null, x.getDescription());
		assertEquals(null, x.getDeprecated());
		assertEquals(null, x.getSchemes());
	}
	@Test
	public void e01b_operation_summary_default_withFile() throws Exception {
		Operation x = getSwaggerWithFile(new E01()).getPaths().get("/path/{foo}").get("get");
		assertEquals("s-operationId", x.getOperationId());
		assertEquals("s-summary", x.getSummary());
		assertEquals("s-description", x.getDescription());
		assertObjectEquals("true", x.getDeprecated());
		assertObjectEquals("['s-scheme']", x.getSchemes());
	}

	@RestResource(
		swagger=@ResourceSwagger(
			"paths:{'/path/{foo}':{get:{operationId:'a-operationId',summary:'a-summary',description:'a-description',deprecated:false,schemes:['a-scheme']}}}"
		)
	)
	public static class E02 {
		@RestMethod(name=GET,path="/path/{foo}")
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void e02a_operation_summary_swaggerOnClass() throws Exception {
		Operation x = getSwagger(new E02()).getPaths().get("/path/{foo}").get("get");
		assertEquals("a-operationId", x.getOperationId());
		assertEquals("a-summary", x.getSummary());
		assertEquals("a-description", x.getDescription());
		assertObjectEquals("false", x.getDeprecated());
		assertObjectEquals("['a-scheme']", x.getSchemes());
	}
	@Test
	public void e02b_operation_summary_swaggerOnClass_withFile() throws Exception {
		Operation x = getSwaggerWithFile(new E02()).getPaths().get("/path/{foo}").get("get");
		assertEquals("a-operationId", x.getOperationId());
		assertEquals("a-summary", x.getSummary());
		assertEquals("a-description", x.getDescription());
		assertObjectEquals("false", x.getDeprecated());
		assertObjectEquals("['a-scheme']", x.getSchemes());
	}

	@RestResource(
		swagger=@ResourceSwagger(
			"paths:{'/path/{foo}':{get:{operationId:'a-operationId',summary:'a-summary',description:'a-description',deprecated:false,schemes:['a-scheme']}}}"
		)
	)
	public static class E03 {
		@RestMethod(name=GET,path="/path/{foo}",
			swagger=@MethodSwagger("operationId:'b-operationId',summary:'b-summary',description:'b-description',deprecated:false,schemes:['b-scheme']")
		)
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void e03a_operation_summary_swaggerOnMethod() throws Exception {
		Operation x = getSwagger(new E03()).getPaths().get("/path/{foo}").get("get");
		assertEquals("b-operationId", x.getOperationId());
		assertEquals("b-summary", x.getSummary());
		assertEquals("b-description", x.getDescription());
		assertObjectEquals("false", x.getDeprecated());
		assertObjectEquals("['b-scheme']", x.getSchemes());
	}
	@Test
	public void e03b_operation_summary_swaggerOnMethod_withFile() throws Exception {
		Operation x = getSwaggerWithFile(new E03()).getPaths().get("/path/{foo}").get("get");
		assertEquals("b-operationId", x.getOperationId());
		assertEquals("b-summary", x.getSummary());
		assertEquals("b-description", x.getDescription());
		assertObjectEquals("false", x.getDeprecated());
		assertObjectEquals("['b-scheme']", x.getSchemes());
	}

	@RestResource(
		swagger=@ResourceSwagger(
			"paths:{'/path/{foo}':{get:{operationId:'a-operationId',summary:'a-summary',description:'a-description',deprecated:false,schemes:['a-scheme']}}}"
		)
	)
	public static class E04 {
		@RestMethod(name=GET,path="/path/{foo}",
			swagger=@MethodSwagger(
				operationId="c-operationId",
				summary="c-summary",
				description="c-description",
				deprecated="false",
				schemes="d-scheme-1, d-scheme-2"
			)
		)
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void e04a_operation_summary_swaggerOnAnnotation() throws Exception {
		Operation x = getSwagger(new E04()).getPaths().get("/path/{foo}").get("get");
		assertEquals("c-operationId", x.getOperationId());
		assertEquals("c-summary", x.getSummary());
		assertEquals("c-description", x.getDescription());
		assertObjectEquals("['d-scheme-1','d-scheme-2']", x.getSchemes());
	}
	@Test
	public void e04b_operation_summary_swaggerOnAnnotation_withFile() throws Exception {
		Operation x = getSwaggerWithFile(new E04()).getPaths().get("/path/{foo}").get("get");
		assertEquals("c-operationId", x.getOperationId());
		assertEquals("c-summary", x.getSummary());
		assertEquals("c-description", x.getDescription());
		assertObjectEquals("['d-scheme-1','d-scheme-2']", x.getSchemes());
	}

	@RestResource(
		messages="BasicRestInfoProviderTest",
		swagger=@ResourceSwagger(
			"paths:{'/path/{foo}':{get:{operationId:'a-operationId',summary:'a-summary',description:'a-description',deprecated:false,schemes:['a-scheme']}}}"
		)
	)
	public static class E05 {
		@RestMethod(name=GET,path="/path/{foo}",
			swagger=@MethodSwagger(
				summary="$L{foo}",
				operationId="$L{foo}",
				description="$L{foo}",
				deprecated="$L{false}",
				schemes="$L{foo}"
			)
		)
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void e05a_operation_summary_swaggerOnAnnotation_localized() throws Exception {
		Operation x = getSwagger(new E05()).getPaths().get("/path/{foo}").get("get");
		assertEquals("l-foo", x.getOperationId());
		assertEquals("l-foo", x.getSummary());
		assertEquals("l-foo", x.getDescription());
		assertObjectEquals("false", x.getDeprecated());
		assertObjectEquals("['l-foo']", x.getSchemes());
	}
	@Test
	public void e05b_operation_summary_swaggerOnAnnotation_localized_withFile() throws Exception {
		Operation x = getSwaggerWithFile(new E05()).getPaths().get("/path/{foo}").get("get");
		assertEquals("l-foo", x.getOperationId());
		assertEquals("l-foo", x.getSummary());
		assertEquals("l-foo", x.getDescription());
		assertObjectEquals("false", x.getDeprecated());
		assertObjectEquals("['l-foo']", x.getSchemes());
	}

	@RestResource(
		swagger=@ResourceSwagger(
			"paths:{'/path/{foo}':{get:{summary:'a-summary',description:'a-description'}}}"
		)
	)
	public static class E06 {
		@RestMethod(name=GET,path="/path/{foo}",
			summary="d-summary",
			description="d-description"
		)
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void e06a_operation_summary_RestMethod() throws Exception {
		Operation x = getSwagger(new E06()).getPaths().get("/path/{foo}").get("get");
		assertEquals("a-summary", x.getSummary());
		assertEquals("a-description", x.getDescription());
	}
	@Test
	public void e06b_operation_summary_RestMethod_withFile() throws Exception {
		Operation x = getSwaggerWithFile(new E06()).getPaths().get("/path/{foo}").get("get");
		assertEquals("a-summary", x.getSummary());
		assertEquals("a-description", x.getDescription());
	}

	@RestResource(
		swagger=@ResourceSwagger(
			"paths:{'/path/{foo}':{get:{}}}"
		)
	)
	public static class E07 {
		@RestMethod(name=GET,path="/path/{foo}",
			summary="d-summary",
			description="d-description"
		)
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void e07a_operation_summary_RestMethod() throws Exception {
		Operation x = getSwagger(new E07()).getPaths().get("/path/{foo}").get("get");
		assertEquals("d-summary", x.getSummary());
		assertEquals("d-description", x.getDescription());
	}
	@Test
	public void e07b_operation_summary_RestMethod_withFile() throws Exception {
		Operation x = getSwaggerWithFile(new E07()).getPaths().get("/path/{foo}").get("get");
		assertEquals("d-summary", x.getSummary());
		assertEquals("d-description", x.getDescription());
	}

	//=================================================================================================================
	// /paths/<path>/<method>/tags
	//=================================================================================================================

	@RestResource
	public static class MD01 {

		@RestMethod(name=GET,path="/path/{foo}")
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void md01_operation_tags_default() throws Exception {
		assertObjectEquals("null", getSwagger(new MD01()).getPaths().get("/path/{foo}").get("get").getTags());
		assertObjectEquals("['s-tag']", getSwaggerWithFile(new MD01()).getPaths().get("/path/{foo}").get("get").getTags());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{tags:['a-tag']}}}"))
	public static class MD02 {
		@RestMethod(name=GET,path="/path/{foo}")
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void md02_operation_tags_swaggerOnClass() throws Exception {
		assertObjectEquals("['a-tag']", getSwagger(new MD02()).getPaths().get("/path/{foo}").get("get").getTags());
		assertObjectEquals("['a-tag']", getSwaggerWithFile(new MD02()).getPaths().get("/path/{foo}").get("get").getTags());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{tags:['a-tag']}}}"))
	public static class MD03 {
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger("tags:['b-tag']"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void md03_operation_tags_swaggerOnMethod() throws Exception {
		assertObjectEquals("['b-tag']", getSwagger(new MD03()).getPaths().get("/path/{foo}").get("get").getTags());
		assertObjectEquals("['b-tag']", getSwaggerWithFile(new MD03()).getPaths().get("/path/{foo}").get("get").getTags());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{tags:['a-tag']}}}"))
	public static class MD04a {
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(tags="['c-tag-1','c-tag-2']"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void md04a_operation_tags_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("['c-tag-1','c-tag-2']", getSwagger(new MD04a()).getPaths().get("/path/{foo}").get("get").getTags());
		assertObjectEquals("['c-tag-1','c-tag-2']", getSwaggerWithFile(new MD04a()).getPaths().get("/path/{foo}").get("get").getTags());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{tags:['a-tag']}}}"))
	public static class MD04b {
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(tags="c-tag-1, c-tag-2"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void md04b_operation_tags_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("['c-tag-1','c-tag-2']", getSwagger(new MD04b()).getPaths().get("/path/{foo}").get("get").getTags());
		assertObjectEquals("['c-tag-1','c-tag-2']", getSwaggerWithFile(new MD04b()).getPaths().get("/path/{foo}").get("get").getTags());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{tags:'a-tags'}}}"))
	public static class MD05 {
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(tags="$L{foo}"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void md05_operation_tags_swaggerOnAnnotation_localized() throws Exception {
		assertObjectEquals("['l-foo']", getSwagger(new MD05()).getPaths().get("/path/{foo}").get("get").getTags());
		assertObjectEquals("['l-foo']", getSwaggerWithFile(new MD05()).getPaths().get("/path/{foo}").get("get").getTags());
	}

	//=================================================================================================================
	// /paths/<path>/<method>/externalDocs
	//=================================================================================================================

	@RestResource
	public static class ME01 {

		@RestMethod(name=GET,path="/path/{foo}")
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void me01_operation_externalDocs_default() throws Exception {
		assertObjectEquals("null", getSwagger(new ME01()).getPaths().get("/path/{foo}").get("get").getExternalDocs());
		assertObjectEquals("{description:'s-description',url:'s-url'}", getSwaggerWithFile(new ME01()).getPaths().get("/path/{foo}").get("get").getExternalDocs());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{externalDocs:{description:'a-description',url:'a-url'}}}}"))
	public static class ME02 {
		@RestMethod(name=GET,path="/path/{foo}")
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void me02_operation_externalDocs_swaggerOnClass() throws Exception {
		assertObjectEquals("{description:'a-description',url:'a-url'}", getSwagger(new ME02()).getPaths().get("/path/{foo}").get("get").getExternalDocs());
		assertObjectEquals("{description:'a-description',url:'a-url'}", getSwaggerWithFile(new ME02()).getPaths().get("/path/{foo}").get("get").getExternalDocs());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{externalDocs:{description:'a-description',url:'a-url'}}}}"))
	public static class ME03 {
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger("externalDocs:{description:'b-description',url:'b-url'}"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void me03_operation_externalDocs_swaggerOnMethod() throws Exception {
		assertObjectEquals("{description:'b-description',url:'b-url'}", getSwagger(new ME03()).getPaths().get("/path/{foo}").get("get").getExternalDocs());
		assertObjectEquals("{description:'b-description',url:'b-url'}", getSwaggerWithFile(new ME03()).getPaths().get("/path/{foo}").get("get").getExternalDocs());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{externalDocs:{description:'a-description',url:'a-url'}}}}"))
	public static class ME04a {
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(externalDocs=@ExternalDocs(description="c-description",url="c-url")))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void me04a_operation_externalDocs_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("{description:'c-description',url:'c-url'}", getSwagger(new ME04a()).getPaths().get("/path/{foo}").get("get").getExternalDocs());
		assertObjectEquals("{description:'c-description',url:'c-url'}", getSwaggerWithFile(new ME04a()).getPaths().get("/path/{foo}").get("get").getExternalDocs());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{externalDocs:{description:'a-description',url:'a-url'}}}}"))
	public static class ME04b {
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(externalDocs=@ExternalDocs("{description:'d-description',url:'d-url'}")))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void me04b_operation_externalDocs_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("{description:'d-description',url:'d-url'}", getSwagger(new ME04b()).getPaths().get("/path/{foo}").get("get").getExternalDocs());
		assertObjectEquals("{description:'d-description',url:'d-url'}", getSwaggerWithFile(new ME04b()).getPaths().get("/path/{foo}").get("get").getExternalDocs());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{externalDocs:{description:'a-description',url:'a-url'}}}}"))
	public static class ME05 {
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(externalDocs=@ExternalDocs("{description:'$L{foo}',url:'$L{foo}'}")))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void me05_operation_externalDocs_swaggerOnAnnotation_localized() throws Exception {
		assertObjectEquals("{description:'l-foo',url:'l-foo'}", getSwagger(new ME05()).getPaths().get("/path/{foo}").get("get").getExternalDocs());
		assertObjectEquals("{description:'l-foo',url:'l-foo'}", getSwaggerWithFile(new ME05()).getPaths().get("/path/{foo}").get("get").getExternalDocs());
	}

	//=================================================================================================================
	// /paths/<path>/<method>/consumes
	//=================================================================================================================

	@RestResource
	public static class MF01 {

		@RestMethod(name=GET,path="/path/{foo}")
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mf01_operation_consumes_default() throws Exception {
		assertObjectEquals("null", getSwagger(new MF01()).getPaths().get("/path/{foo}").get("get").getConsumes());
		assertObjectEquals("['s-consumes']", getSwaggerWithFile(new MF01()).getPaths().get("/path/{foo}").get("get").getConsumes());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{consumes:['a-consumes']}}}"))
	public static class MF02 {
		@RestMethod(name=GET,path="/path/{foo}")
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mf02_operation_consumes_swaggerOnClass() throws Exception {
		assertObjectEquals("['a-consumes']", getSwagger(new MF02()).getPaths().get("/path/{foo}").get("get").getConsumes());
		assertObjectEquals("['a-consumes']", getSwaggerWithFile(new MF02()).getPaths().get("/path/{foo}").get("get").getConsumes());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{consumes:['a-consumes']}}}"))
	public static class MF03 {
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger("consumes:['b-consumes']"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mf03_operation_consumes_swaggerOnMethod() throws Exception {
		assertObjectEquals("['b-consumes']", getSwagger(new MF03()).getPaths().get("/path/{foo}").get("get").getConsumes());
		assertObjectEquals("['b-consumes']", getSwaggerWithFile(new MF03()).getPaths().get("/path/{foo}").get("get").getConsumes());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{consumes:['a-consumes']}}}"))
	public static class MF04a {
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(consumes="['c-consumes-1','c-consumes-2']"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mf04a_operation_consumes_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("['c-consumes-1','c-consumes-2']", getSwagger(new MF04a()).getPaths().get("/path/{foo}").get("get").getConsumes());
		assertObjectEquals("['c-consumes-1','c-consumes-2']", getSwaggerWithFile(new MF04a()).getPaths().get("/path/{foo}").get("get").getConsumes());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{consumes:['a-consumes']}}}"))
	public static class MF04b {
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(consumes="c-consumes-1, c-consumes-2"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mf04b_operation_consumes_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("['c-consumes-1','c-consumes-2']", getSwagger(new MF04b()).getPaths().get("/path/{foo}").get("get").getConsumes());
		assertObjectEquals("['c-consumes-1','c-consumes-2']", getSwaggerWithFile(new MF04b()).getPaths().get("/path/{foo}").get("get").getConsumes());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{consumes:['a-consumes']}}}"))
	public static class MF05 {
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(consumes="['$L{foo}']"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void me05_operation_consumes_swaggerOnAnnotation_localized() throws Exception {
		assertObjectEquals("['l-foo']", getSwagger(new MF05()).getPaths().get("/path/{foo}").get("get").getConsumes());
		assertObjectEquals("['l-foo']", getSwaggerWithFile(new MF05()).getPaths().get("/path/{foo}").get("get").getConsumes());
	}

	@RestResource(parsers={JsonParser.class})
	public static class MF06a {
		@RestMethod(name=PUT,path="/path2/{foo}")
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mf06a_operation_consumes_parsersOnClass() throws Exception {
		assertObjectEquals("null", getSwagger(new MF06a()).getPaths().get("/path2/{foo}").get("put").getConsumes());
		assertObjectEquals("null", getSwaggerWithFile(new MF06a()).getPaths().get("/path2/{foo}").get("put").getConsumes());
	}

	@RestResource(parsers={JsonParser.class})
	public static class MF06b {
		@RestMethod(name=PUT,path="/path2/{foo}",parsers={XmlParser.class})
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mf06b_operation_consumes_parsersOnClassAndMethod() throws Exception {
		assertObjectEquals("['text/xml','application/xml']", getSwagger(new MF06b()).getPaths().get("/path2/{foo}").get("put").getConsumes());
		assertObjectEquals("['text/xml','application/xml']", getSwaggerWithFile(new MF06b()).getPaths().get("/path2/{foo}").get("put").getConsumes());
	}

	@RestResource(parsers={JsonParser.class},swagger=@ResourceSwagger("paths:{'/path2/{foo}':{put:{consumes:['a-consumes']}}}"))
	public static class MF06c {
		@RestMethod(name=PUT,path="/path2/{foo}",parsers={XmlParser.class})
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mf06c_operation_consumes_parsersOnClassAndMethodWithSwagger() throws Exception {
		assertObjectEquals("['a-consumes']", getSwagger(new MF06c()).getPaths().get("/path2/{foo}").get("put").getConsumes());
		assertObjectEquals("['a-consumes']", getSwaggerWithFile(new MF06c()).getPaths().get("/path2/{foo}").get("put").getConsumes());
	}

	//=================================================================================================================
	// /paths/<path>/<method>/produces
	//=================================================================================================================

	@RestResource
	public static class MG01 {

		@RestMethod(name=GET,path="/path/{foo}")
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mg01_operation_produces_default() throws Exception {
		assertObjectEquals("null", getSwagger(new MG01()).getPaths().get("/path/{foo}").get("get").getProduces());
		assertObjectEquals("['s-produces']", getSwaggerWithFile(new MG01()).getPaths().get("/path/{foo}").get("get").getProduces());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{produces:['a-produces']}}}"))
	public static class MG02 {
		@RestMethod(name=GET,path="/path/{foo}")
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mg02_operation_produces_swaggerOnClass() throws Exception {
		assertObjectEquals("['a-produces']", getSwagger(new MG02()).getPaths().get("/path/{foo}").get("get").getProduces());
		assertObjectEquals("['a-produces']", getSwaggerWithFile(new MG02()).getPaths().get("/path/{foo}").get("get").getProduces());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{produces:['a-produces']}}}"))
	public static class MG03 {
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger("produces:['b-produces']"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mg03_operation_produces_swaggerOnMethod() throws Exception {
		assertObjectEquals("['b-produces']", getSwagger(new MG03()).getPaths().get("/path/{foo}").get("get").getProduces());
		assertObjectEquals("['b-produces']", getSwaggerWithFile(new MG03()).getPaths().get("/path/{foo}").get("get").getProduces());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{produces:['a-produces']}}}"))
	public static class MG04a {
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(produces="['c-produces-1','c-produces-2']"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mg04a_operation_produces_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("['c-produces-1','c-produces-2']", getSwagger(new MG04a()).getPaths().get("/path/{foo}").get("get").getProduces());
		assertObjectEquals("['c-produces-1','c-produces-2']", getSwaggerWithFile(new MG04a()).getPaths().get("/path/{foo}").get("get").getProduces());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{produces:['a-produces']}}}"))
	public static class MG04b {
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(produces="c-produces-1, c-produces-2"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mg04b_operation_produces_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("['c-produces-1','c-produces-2']", getSwagger(new MG04b()).getPaths().get("/path/{foo}").get("get").getProduces());
		assertObjectEquals("['c-produces-1','c-produces-2']", getSwaggerWithFile(new MG04b()).getPaths().get("/path/{foo}").get("get").getProduces());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{produces:['a-produces']}}}"))
	public static class MG05 {
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(produces="['$L{foo}']"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mg05_operation_produces_swaggerOnAnnotation_localized() throws Exception {
		assertObjectEquals("['l-foo']", getSwagger(new MG05()).getPaths().get("/path/{foo}").get("get").getProduces());
		assertObjectEquals("['l-foo']", getSwaggerWithFile(new MG05()).getPaths().get("/path/{foo}").get("get").getProduces());
	}

	@RestResource(serializers={JsonSerializer.class})
	public static class MG06a {
		@RestMethod(name=PUT,path="/path2/{foo}")
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mg06a_operation_produces_serializersOnClass() throws Exception {
		assertObjectEquals("null", getSwagger(new MG06a()).getPaths().get("/path2/{foo}").get("put").getProduces());
		assertObjectEquals("null", getSwaggerWithFile(new MG06a()).getPaths().get("/path2/{foo}").get("put").getProduces());
	}

	@RestResource(serializers={JsonSerializer.class})
	public static class MG06b {
		@RestMethod(name=PUT,path="/path2/{foo}",serializers={XmlSerializer.class})
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mg06b_operation_produces_serializersOnClassAndMethod() throws Exception {
		assertObjectEquals("['text/xml']", getSwagger(new MG06b()).getPaths().get("/path2/{foo}").get("put").getProduces());
		assertObjectEquals("['text/xml']", getSwaggerWithFile(new MG06b()).getPaths().get("/path2/{foo}").get("put").getProduces());
	}

	@RestResource(serializers={JsonSerializer.class},swagger=@ResourceSwagger("paths:{'/path2/{foo}':{put:{produces:['a-produces']}}}"))
	public static class MG06c {
		@RestMethod(name=PUT,path="/path2/{foo}",serializers={XmlSerializer.class})
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mg06c_operation_produces_serializersOnClassAndMethodWithSwagger() throws Exception {
		assertObjectEquals("['a-produces']", getSwagger(new MG06c()).getPaths().get("/path2/{foo}").get("put").getProduces());
		assertObjectEquals("['a-produces']", getSwaggerWithFile(new MG06c()).getPaths().get("/path2/{foo}").get("put").getProduces());
	}

	//=================================================================================================================
	// /paths/<path>/<method>/deprecated
	//=================================================================================================================

	@RestResource
	public static class MH06 {
		@RestMethod(name=GET,path="/path2/{foo}")
		@Deprecated
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mh06_operation_deprecated_Deprecated() throws Exception {
		assertObjectEquals("true", getSwagger(new MH06()).getPaths().get("/path2/{foo}").get("get").getDeprecated());
		assertObjectEquals("true", getSwaggerWithFile(new MH06()).getPaths().get("/path2/{foo}").get("get").getDeprecated());
	}

	@RestResource
	@Deprecated
	public static class MH07 {
		@RestMethod(name=GET,path="/path2/{foo}")
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mh07_operation_deprecated_Deprecated() throws Exception {
		assertObjectEquals("true", getSwagger(new MH07()).getPaths().get("/path2/{foo}").get("get").getDeprecated());
		assertObjectEquals("true", getSwaggerWithFile(new MH07()).getPaths().get("/path2/{foo}").get("get").getDeprecated());
	}

	//=================================================================================================================
	// /paths/<path>/<method>/parameters/query
	//=================================================================================================================

	@RestResource
	public static class NA01 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}

	@Test
	public void na01a_query_type_default() throws Exception {
		ParameterInfo x = getSwagger(new NA01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo");
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
	@Test
	public void na01b_query_type_default_withFile() throws Exception {
		ParameterInfo x = getSwaggerWithFile(new NA01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo");
		assertEquals("string", x.getType());
		assertEquals("s-description", x.getDescription());
		assertObjectEquals("true", x.getRequired());
		assertObjectEquals("true", x.getAllowEmptyValue());
		assertObjectEquals("true", x.getExclusiveMaximum());
		assertObjectEquals("true", x.getExclusiveMinimum());
		assertObjectEquals("true", x.getUniqueItems());
		assertEquals("s-format", x.getFormat());
		assertEquals("s-collectionFormat", x.getCollectionFormat());
		assertEquals("s-pattern", x.getPattern());
		assertObjectEquals("1.0", x.getMaximum());
		assertObjectEquals("1.0", x.getMinimum());
		assertObjectEquals("1.0", x.getMultipleOf());
		assertObjectEquals("1", x.getMaxLength());
		assertObjectEquals("1", x.getMinLength());
		assertObjectEquals("1", x.getMaxItems());
		assertObjectEquals("1", x.getMinItems());
	}

	@RestResource(
		swagger=@ResourceSwagger({
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
	public static class NA02 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}

	@Test
	public void na02a_query_type_swaggerOnClass() throws Exception {
		ParameterInfo x = getSwagger(new NA02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo");
		assertEquals("int32", x.getType());
		assertEquals("a-description", x.getDescription());
		assertObjectEquals("false", x.getRequired());
		assertObjectEquals("false", x.getAllowEmptyValue());
		assertObjectEquals("false", x.getExclusiveMaximum());
		assertObjectEquals("false", x.getExclusiveMinimum());
		assertObjectEquals("false", x.getUniqueItems());
		assertEquals("a-format", x.getFormat());
		assertEquals("a-collectionFormat", x.getCollectionFormat());
		assertEquals("a-pattern", x.getPattern());
		assertObjectEquals("2.0", x.getMaximum());
		assertObjectEquals("2.0", x.getMinimum());
		assertObjectEquals("2.0", x.getMultipleOf());
		assertObjectEquals("2", x.getMaxLength());
		assertObjectEquals("2", x.getMinLength());
		assertObjectEquals("2", x.getMaxItems());
		assertObjectEquals("2", x.getMinItems());
	}
	@Test
	public void na02b_query_type_swaggerOnClass_withFile() throws Exception {
		ParameterInfo x = getSwaggerWithFile(new NA02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo");
		assertEquals("int32", x.getType());
		assertEquals("a-description", x.getDescription());
		assertObjectEquals("false", x.getRequired());
		assertObjectEquals("false", x.getAllowEmptyValue());
		assertObjectEquals("false", x.getExclusiveMaximum());
		assertObjectEquals("false", x.getExclusiveMinimum());
		assertObjectEquals("false", x.getUniqueItems());
		assertEquals("a-format", x.getFormat());
		assertEquals("a-collectionFormat", x.getCollectionFormat());
		assertEquals("a-pattern", x.getPattern());
		assertObjectEquals("2.0", x.getMaximum());
		assertObjectEquals("2.0", x.getMinimum());
		assertObjectEquals("2.0", x.getMultipleOf());
		assertObjectEquals("2", x.getMaxLength());
		assertObjectEquals("2", x.getMinLength());
		assertObjectEquals("2", x.getMaxItems());
		assertObjectEquals("2", x.getMinItems());
	}

	@RestResource(
		swagger=@ResourceSwagger({
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
	public static class NA03 {
		@RestMethod(name=GET,path="/path/{foo}/query",
			swagger=@MethodSwagger({
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
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void na03a_query_type_swaggerOnMethod() throws Exception {
		ParameterInfo x = getSwagger(new NA03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo");
		assertEquals("int64", x.getType());
		assertEquals("b-description", x.getDescription());
		assertObjectEquals("true", x.getRequired());
		assertObjectEquals("true", x.getAllowEmptyValue());
		assertObjectEquals("true", x.getExclusiveMaximum());
		assertObjectEquals("true", x.getExclusiveMinimum());
		assertObjectEquals("true", x.getUniqueItems());
		assertEquals("b-format", x.getFormat());
		assertEquals("b-collectionFormat", x.getCollectionFormat());
		assertEquals("b-pattern", x.getPattern());
		assertObjectEquals("3.0", x.getMaximum());
		assertObjectEquals("3.0", x.getMinimum());
		assertObjectEquals("3.0", x.getMultipleOf());
		assertObjectEquals("3", x.getMaxLength());
		assertObjectEquals("3", x.getMinLength());
		assertObjectEquals("3", x.getMaxItems());
		assertObjectEquals("3", x.getMinItems());
	}
	@Test
	public void na03b_query_type_swaggerOnMethod_withFile() throws Exception {
		ParameterInfo x = getSwaggerWithFile(new NA03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo");
		assertEquals("int64", x.getType());
		assertEquals("b-description", x.getDescription());
		assertObjectEquals("true", x.getRequired());
		assertObjectEquals("true", x.getAllowEmptyValue());
		assertObjectEquals("true", x.getExclusiveMaximum());
		assertObjectEquals("true", x.getExclusiveMinimum());
		assertObjectEquals("true", x.getUniqueItems());
		assertEquals("b-format", x.getFormat());
		assertEquals("b-collectionFormat", x.getCollectionFormat());
		assertEquals("b-pattern", x.getPattern());
		assertObjectEquals("3.0", x.getMaximum());
		assertObjectEquals("3.0", x.getMinimum());
		assertObjectEquals("3.0", x.getMultipleOf());
		assertObjectEquals("3", x.getMaxLength());
		assertObjectEquals("3", x.getMinLength());
		assertObjectEquals("3", x.getMaxItems());
		assertObjectEquals("3", x.getMinItems());
	}

	//=================================================================================================================
	// /paths/<path>/<method>/parameters/query/example
	//=================================================================================================================

	@RestResource
	public static class NR01 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}

	@Test
	public void nr01_query_example_default() throws Exception {
		assertEquals(null, getSwagger(new NR01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExample());
		assertEquals("{id:1}", getSwaggerWithFile(new NR01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExample());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',x-example:'{id:2}'}]}}}"))
	public static class NR02 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}

	@Test
	public void nr02_query_example_swaggerOnClass() throws Exception {
		assertEquals("{id:2}", getSwagger(new NR02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExample());
		assertEquals("{id:2}", getSwaggerWithFile(new NR02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExample());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',x-example:'{id:2}'}]}}}"))
	public static class NR03 {
		@RestMethod(name=GET,path="/path/{foo}/query",swagger=@MethodSwagger("parameters:[{'in':'query',name:'foo',x-example:'{id:3}'}]"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void nr03_query_example_swaggerOnMethod() throws Exception {
		assertEquals("{id:3}", getSwagger(new NR03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExample());
		assertEquals("{id:3}", getSwaggerWithFile(new NR03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExample());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',x-example:'{id:2}'}]}}}"))
	public static class NR04 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",example="{id:4}") Foo foo) {
			return null;
		}
	}

	@Test
	public void nr04_query_example_swaggerOnAnnotation() throws Exception {
		assertEquals("{id:4}", getSwagger(new NR04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExample());
		assertEquals("{id:4}", getSwaggerWithFile(new NR04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExample());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',x-example:'{id:2}'}]}}}"))
	public static class NR05 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",example="{id:$L{5}}") Foo foo) {
			return null;
		}
	}

	@Test
	public void nr05_query_example_swaggerOnAnnotation_localized() throws Exception {
		assertEquals("{id:5}", getSwagger(new NR05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExample());
		assertEquals("{id:5}", getSwaggerWithFile(new NR05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExample());
	}

	//=================================================================================================================
	// /paths/<path>/<method>/parameters/body/examples
	//=================================================================================================================

	@RestResource
	public static class NS01 {
		@RestMethod(name=GET,path="/path/{foo}/body")
		public Foo doFoo(@Body Foo foo) {
			return null;
		}
	}

	@Test
	public void ns01_body_examples_default() throws Exception {
		assertEquals(null, getSwagger(new NS01()).getPaths().get("/path/{foo}/body").get("get").getParameter("body",null).getExamples());
		assertObjectEquals("{foo:'a'}", getSwaggerWithFile(new NS01()).getPaths().get("/path/{foo}/body").get("get").getParameter("body",null).getExamples());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/body':{get:{parameters:[{'in':'body',x-examples:{foo:'b'}}]}}}"))
	public static class NS02 {
		@RestMethod(name=GET,path="/path/{foo}/body")
		public Foo doFoo(@Body Foo foo) {
			return null;
		}
	}

	@Test
	public void ns02_body_examples_swaggerOnClass() throws Exception {
		assertObjectEquals("{foo:'b'}", getSwagger(new NS02()).getPaths().get("/path/{foo}/body").get("get").getParameter("body",null).getExamples());
		assertObjectEquals("{foo:'b'}", getSwaggerWithFile(new NS02()).getPaths().get("/path/{foo}/body").get("get").getParameter("body",null).getExamples());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/body':{get:{parameters:[{'in':'body',x-examples:{foo:'b'}}]}}}"))
	public static class NS03 {
		@RestMethod(name=GET,path="/path/{foo}/body",swagger=@MethodSwagger("parameters:[{'in':'body',x-examples:{foo:'c'}}]"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void ns03_body_examples_swaggerOnMethods() throws Exception {
		assertObjectEquals("{foo:'c'}", getSwagger(new NS03()).getPaths().get("/path/{foo}/body").get("get").getParameter("body",null).getExamples());
		assertObjectEquals("{foo:'c'}", getSwaggerWithFile(new NS03()).getPaths().get("/path/{foo}/body").get("get").getParameter("body",null).getExamples());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/body':{get:{parameters:[{'in':'body',x-examples:{foo:'b'}}]}}}"))
	public static class NS04 {
		@RestMethod(name=GET,path="/path/{foo}/body")
		public Foo doFoo(@Body(examples="{foo:'d'}") Foo foo) {
			return null;
		}
	}

	@Test
	public void ns04_body_examples_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("{foo:'d'}", getSwagger(new NS04()).getPaths().get("/path/{foo}/body").get("get").getParameter("body",null).getExamples());
		assertObjectEquals("{foo:'d'}", getSwaggerWithFile(new NS04()).getPaths().get("/path/{foo}/body").get("get").getParameter("body",null).getExamples());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/body':{get:{parameters:[{'in':'body',examples:{foo:'b'}}]}}}"))
	public static class NS05 {
		@RestMethod(name=GET,path="/path/{foo}/body")
		public Foo doFoo(@Body(examples="{foo:'$L{foo}'}") Foo foo) {
			return null;
		}
	}

	@Test
	public void ns05_body_examples_swaggerOnAnnotation_localized() throws Exception {
		assertObjectEquals("{foo:'l-foo'}", getSwagger(new NS05()).getPaths().get("/path/{foo}/body").get("get").getParameter("body",null).getExamples());
		assertObjectEquals("{foo:'l-foo'}", getSwaggerWithFile(new NS05()).getPaths().get("/path/{foo}/body").get("get").getParameter("body",null).getExamples());
	}

	//=================================================================================================================
	// /paths/<path>/<method>/parameters/query/schema
	//=================================================================================================================

	@RestResource
	public static class NT01 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}

	@Test
	public void nt01_query_schema_default() throws Exception {
		assertObjectEquals("{properties:{id:{format:'int32',type:'integer'}}}", getSwagger(new NT01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query","foo").getSchema());
		assertObjectEquals("{'$ref':'#/definitions/Foo'}", getSwaggerWithFile(new NT01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query","foo").getSchema());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{in:'query',name:'foo',schema:{$ref:'b'}}]}}}"))
	public static class NT02 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}

	@Test
	public void nt02_query_schema_swaggerOnClass() throws Exception {
		assertObjectEquals("{'$ref':'b'}", getSwagger(new NT02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query","foo").getSchema());
		assertObjectEquals("{'$ref':'b'}", getSwaggerWithFile(new NT02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query","foo").getSchema());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{in:'query',name:'foo',schema:{$ref:'b'}}]}}}"))
	public static class NT03 {

		@RestMethod(name=GET,path="/path/{foo}/query",swagger=@MethodSwagger("parameters:[{'in':'query',name:'foo',schema:{$ref:'c'}}]"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void nt03_query_schema_swaggerOnMethnt() throws Exception {
		assertObjectEquals("{'$ref':'c'}", getSwagger(new NT03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query","foo").getSchema());
		assertObjectEquals("{'$ref':'c'}", getSwaggerWithFile(new NT03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query","foo").getSchema());
	}

	//=================================================================================================================
	// /paths/<path>/<method>/responses/<response>/description
	//=================================================================================================================

	@RestResource
	public static class OA01a {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public void doFoo(Value<OA01x> foo) {}
	}
	@RestResource
	public static class OA01b {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public OA01x doFoo() { return null;}
	}
	@Response(code=100)
	public static class OA01x {
		public String foo;
	}

	@Test
	public void oa01a_responses_100_description_default() throws Exception {
		assertEquals("Continue", getSwagger(new OA01a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
		assertEquals("s-100-description", getSwaggerWithFile(new OA01a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
	}
	@Test
	public void oa01b_responses_100_description_default() throws Exception {
		assertEquals("Continue", getSwagger(new OA01b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
		assertEquals("s-100-description", getSwaggerWithFile(new OA01b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{description:'a-100-description'}}}}}"))
	public static class OA02 {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public void doFoo(@ResponseStatus Value<Integer> foo) {}
	}

	@Test
	public void oa02_response_100_description_swaggerOnClass() throws Exception {
		assertEquals("a-100-description", getSwagger(new OA02()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
		assertEquals("a-100-description", getSwaggerWithFile(new OA02()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{description:'a-100-description'}}}}}"))
	public static class OA03 {
		@RestMethod(name=GET,path="/path/{foo}/responses/100",swagger=@MethodSwagger("responses:{100:{description:'b-100-description'}}"))
		public void doFoo(@ResponseStatus Value<Integer> foo) {}
	}

	@Test
	public void oa03_response_100_description_swaggerOnMethod() throws Exception {
		assertEquals("b-100-description", getSwagger(new OA03()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
		assertEquals("b-100-description", getSwaggerWithFile(new OA03()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{description:'a-100-description'}}}}}"))
	public static class OA04a {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public void doFoo(Value<OA04x> foo) {}
	}
	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{description:'a-100-description'}}}}}"))
	public static class OA04b {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public OA04x doFoo() {return null;}
	}
	@Response(code=100,description="c-100-description")
	public static class OA04x {}

	@Test
	public void oa04a_response_100_description_swaggerOnAnnotation() throws Exception {
		assertEquals("c-100-description", getSwagger(new OA04a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
		assertEquals("c-100-description", getSwaggerWithFile(new OA04a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
	}
	@Test
	public void oa04b_response_100_description_swaggerOnAnnotation() throws Exception {
		assertEquals("c-100-description", getSwagger(new OA04b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
		assertEquals("c-100-description", getSwaggerWithFile(new OA04b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{description:'a-100-description'}}}}}"))
	public static class OA05a {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public void doFoo(Value<OA05x> foo) {}
	}
	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{description:'a-100-description'}}}}}"))
	public static class OA05b {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public OA05x doFoo() {return null;}
	}
	@Response(code=100,description="$L{foo}")
	public static class OA05x {}

	@Test
	public void oa05a_response_100_description_swaggerOnAnnotation_localized() throws Exception {
		assertEquals("l-foo", getSwagger(new OA05a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
		assertEquals("l-foo", getSwaggerWithFile(new OA05a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
	}
	@Test
	public void oa05b_response_100_description_swaggerOnAnnotation_localized() throws Exception {
		assertEquals("l-foo", getSwagger(new OA05b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
		assertEquals("l-foo", getSwaggerWithFile(new OA05b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
	}

	//=================================================================================================================
	// /paths/<path>/<method>/responses/<response>/headers
	//=================================================================================================================

	@RestResource
	public static class OB01a {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public void doFoo(Value<OB01x> foo) {}
	}
	@RestResource
	public static class OB01b {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public OB01x doFoo() {return null;}
	}
	@Response(code=100)
	public static class OB01x {
		public String foo;
	}

	@Test
	public void ob01a_responses_100_headers_default() throws Exception {
		assertEquals(null, getSwagger(new OB01a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
		assertObjectEquals("{'X-Foo':{description:'s-description',type:'integer',format:'int32'}}", getSwaggerWithFile(new OB01a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
	}
	@Test
	public void ob01b_responses_100_headers_default() throws Exception {
		assertEquals(null, getSwagger(new OB01b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
		assertObjectEquals("{'X-Foo':{description:'s-description',type:'integer',format:'int32'}}", getSwaggerWithFile(new OB01b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{headers:{'X-Foo':{description:'b-description',type:'integer',format:'int32'}}}}}}}"))
	public static class OB02 {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public Foo doFoo(@ResponseStatus Value<Integer> foo) {
			return null;
		}
	}

	@Test
	public void ob02_response_100_headers_swaggerOnClass() throws Exception {
		assertObjectEquals("{'X-Foo':{description:'b-description',type:'integer',format:'int32'}}", getSwagger(new OB02()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
		assertObjectEquals("{'X-Foo':{description:'b-description',type:'integer',format:'int32'}}", getSwaggerWithFile(new OB02()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{headers:{'X-Foo':{description:'b-description',type:'integer',format:'int32'}}}}}}}"))
	public static class OB03 {
		@RestMethod(name=GET,path="/path/{foo}/responses/100",swagger=@MethodSwagger("responses:{100:{headers:{'X-Foo':{description:'c-description',type:'integer',format:'int32'}}}}"))
		public Foo doFoo(@ResponseStatus Value<Integer> foo) {
			return null;
		}
	}

	@Test
	public void ob03_response_100_headers_swaggerOnMethod() throws Exception {
		assertObjectEquals("{'X-Foo':{description:'c-description',type:'integer',format:'int32'}}", getSwagger(new OB03()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
		assertObjectEquals("{'X-Foo':{description:'c-description',type:'integer',format:'int32'}}", getSwaggerWithFile(new OB03()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{headers:{'X-Foo':{description:'b-description',type:'integer',format:'int32'}}}}}}}"))
	public static class OB04a {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public void doFoo(Value<OB04x> foo) {}
	}
	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{headers:{'X-Foo':{description:'b-description',type:'integer',format:'int32'}}}}}}}"))
	public static class OB04b {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public OB04x doFoo() {return null;}
	}
	@Response(code=100,headers=@ResponseHeader(name="X-Foo",description="d-description",type="integer",format="int32"))
	public static class OB04x {}

	@Test
	public void ob04a_response_100_headers_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("{'X-Foo':{description:'d-description',type:'integer',format:'int32'}}", getSwagger(new OB04a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
		assertObjectEquals("{'X-Foo':{description:'d-description',type:'integer',format:'int32'}}", getSwaggerWithFile(new OB04a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
	}
	@Test
	public void ob04b_response_100_headers_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("{'X-Foo':{description:'d-description',type:'integer',format:'int32'}}", getSwagger(new OB04b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
		assertObjectEquals("{'X-Foo':{description:'d-description',type:'integer',format:'int32'}}", getSwaggerWithFile(new OB04b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{headers:{'X-Foo':{description:'b-description',type:'integer',format:'int32'}}}}}}}"))
	public static class OB05a {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public void doFoo(Value<OB05x> foo) {}
	}
	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{headers:{'X-Foo':{description:'b-description',type:'integer',format:'int32'}}}}}}}"))
	public static class OB05b {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public OB05x doFoo() {return null;}
	}
	@Response(code=100,headers=@ResponseHeader(name="X-Foo",description="$L{foo}",type="integer",format="int32"))
	public static class OB05x {}

	@Test
	public void ob05a_response_100_headers_swaggerOnAnnotation_localized() throws Exception {
		assertObjectEquals("{'X-Foo':{description:'l-foo',type:'integer',format:'int32'}}", getSwagger(new OB05a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
		assertObjectEquals("{'X-Foo':{description:'l-foo',type:'integer',format:'int32'}}", getSwaggerWithFile(new OB05a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
	}
	@Test
	public void ob05b_response_100_headers_swaggerOnAnnotation_localized() throws Exception {
		assertObjectEquals("{'X-Foo':{description:'l-foo',type:'integer',format:'int32'}}", getSwagger(new OB05b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
		assertObjectEquals("{'X-Foo':{description:'l-foo',type:'integer',format:'int32'}}", getSwaggerWithFile(new OB05b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
	}

	//=================================================================================================================
	// /paths/<path>/<method>/responses/<response>/example
	//=================================================================================================================

	@RestResource
	public static class OC01a {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public void doFoo(Value<OC01x> foo) {}
	}
	@RestResource
	public static class OC01b {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public OC01x doFoo() {return null;}
	}
	@Response(code=100)
	public static class OC01x {
		public String foo;
	}

	@Test
	public void oc01a_responses_100_example_default() throws Exception {
		assertEquals(null, getSwagger(new OC01a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
		assertEquals("{foo:'a'}", getSwaggerWithFile(new OC01a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
	}
	@Test
	public void oc01b_responses_100_example_default() throws Exception {
		assertEquals(null, getSwagger(new OC01b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
		assertEquals("{foo:'a'}", getSwaggerWithFile(new OC01b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{example:{foo:'b'}}}}}}"))
	public static class OC02 {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public void doFoo(@ResponseStatus Value<Integer> foo) {}
	}

	@Test
	public void oc02_response_100_example_swaggerOnClass() throws Exception {
		assertObjectEquals("{foo:'b'}", getSwagger(new OC02()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
		assertObjectEquals("{foo:'b'}", getSwaggerWithFile(new OC02()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{example:{foo:'b'}}}}}}"))
	public static class OC03 {
		@RestMethod(name=GET,path="/path/{foo}/responses/100",swagger=@MethodSwagger("responses:{100:{example:{foo:'c'}}}"))
		public void doFoo(@ResponseStatus Value<Integer> foo) {}
	}

	@Test
	public void oc03_response_100_example_swaggerOnMethod() throws Exception {
		assertObjectEquals("{foo:'c'}", getSwagger(new OC03()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
		assertObjectEquals("{foo:'c'}", getSwaggerWithFile(new OC03()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{example:{foo:'b'}}}}}}"))
	public static class OC04a {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public void doFoo(Value<OC04x> foo) {}
	}
	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{example:{foo:'b'}}}}}}"))
	public static class OC04b {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public OC04x doFoo() {return null;}
	}
	@Response(code=100,example="{foo:'d'}")
	public static class OC04x {
		public String foo;
	}

	@Test
	public void oc04a_response_100_example_swaggerOnAnnotation() throws Exception {
		assertEquals("{foo:'d'}", getSwagger(new OC04a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
		assertEquals("{foo:'d'}", getSwaggerWithFile(new OC04a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
	}
	@Test
	public void oc04b_response_100_example_swaggerOnAnnotation() throws Exception {
		assertEquals("{foo:'d'}", getSwagger(new OC04b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
		assertEquals("{foo:'d'}", getSwaggerWithFile(new OC04b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{example:{foo:'b'}}}}}}"))
	public static class OC05a {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public void doFoo(Value<OC05x> foo) {}
	}
	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{example:{foo:'b'}}}}}}"))
	public static class OC05b {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public OC05x doFoo() {return null;}
	}
	@Response(code=100,example="{foo:'$L{foo}'}")
	public static class OC05x {
		public String foo;
	}

	@Test
	public void oc05a_response_100_example_swaggerOnAnnotation_localized() throws Exception {
		assertEquals("{foo:'l-foo'}", getSwagger(new OC05a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
		assertEquals("{foo:'l-foo'}", getSwaggerWithFile(new OC05a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
	}
	@Test
	public void oc05b_response_100_example_swaggerOnAnnotation_localized() throws Exception {
		assertEquals("{foo:'l-foo'}", getSwagger(new OC05b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
		assertEquals("{foo:'l-foo'}", getSwaggerWithFile(new OC05b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
	}

	//=================================================================================================================
	// /paths/<path>/<method>/responses/<response>/examples
	//=================================================================================================================

	@RestResource
	public static class OD01a {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public void doFoo(Value<OD01x> foo) {}
	}
	@RestResource
	public static class OD01b {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public OD01x doFoo() {return null;}
	}
	@Response(code=100)
	public static class OD01x {
		public String foo;
	}

	@Test
	public void od01a_responses_100_examples_default() throws Exception {
		assertEquals(null, getSwagger(new OD01a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
		assertObjectEquals("{foo:'a'}", getSwaggerWithFile(new OD01a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
	}
	@Test
	public void od01b_responses_100_examples_default() throws Exception {
		assertEquals(null, getSwagger(new OD01b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
		assertObjectEquals("{foo:'a'}", getSwaggerWithFile(new OD01b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{examples:{foo:{bar:'b'}}}}}}}"))
	public static class OD02 {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public void doFoo(@ResponseStatus Value<Integer> foo) {}
	}

	@Test
	public void od02_response_100_examples_swaggerOnClass() throws Exception {
		assertObjectEquals("{foo:{bar:'b'}}", getSwagger(new OD02()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
		assertObjectEquals("{foo:{bar:'b'}}", getSwaggerWithFile(new OD02()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{examples:{foo:{bar:'b'}}}}}}}"))
	public static class OD03 {
		@RestMethod(name=GET,path="/path/{foo}/responses/100",swagger=@MethodSwagger("responses:{100:{examples:{foo:{bar:'c'}}}}"))
		public void doFoo(@ResponseStatus Value<Integer> foo) {}
	}

	@Test
	public void od03_response_100_examples_swaggerOnMethod() throws Exception {
		assertObjectEquals("{foo:{bar:'c'}}", getSwagger(new OD03()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
		assertObjectEquals("{foo:{bar:'c'}}", getSwaggerWithFile(new OD03()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{examples:{foo:{bar:'b'}}}}}}}"))
	public static class OD04a {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public void doFoo(Value<OD04x> foo) {}
	}
	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{examples:{foo:{bar:'b'}}}}}}}"))
	public static class OD04b {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public OD04x doFoo() {return null;}
	}
	@Response(code=100,examples="{foo:{bar:'d'}}")
	public static class OD04x {}

	@Test
	public void od04a_response_100_examples_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("{foo:{bar:'d'}}", getSwagger(new OD04a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
		assertObjectEquals("{foo:{bar:'d'}}", getSwaggerWithFile(new OD04a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
	}
	@Test
	public void od04b_response_100_examples_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("{foo:{bar:'d'}}", getSwagger(new OD04b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
		assertObjectEquals("{foo:{bar:'d'}}", getSwaggerWithFile(new OD04b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{examples:{foo:{bar:'b'}}}}}}}"))
	public static class OD05a {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public void doFoo(Value<OD05x> foo) {}
	}
	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{examples:{foo:{bar:'b'}}}}}}}"))
	public static class OD05b {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public OD05x doFoo() {return null;}
	}
	@Response(code=100,examples="{foo:{bar:'$L{foo}'}}")
	public static class OD05x {}

	@Test
	public void od05a_response_100_examples_swaggerOnAnnotation_lodalized() throws Exception {
		assertObjectEquals("{foo:{bar:'l-foo'}}", getSwagger(new OD05a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
		assertObjectEquals("{foo:{bar:'l-foo'}}", getSwaggerWithFile(new OD05a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
	}
	@Test
	public void od05b_response_100_examples_swaggerOnAnnotation_lodalized() throws Exception {
		assertObjectEquals("{foo:{bar:'l-foo'}}", getSwagger(new OD05b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
		assertObjectEquals("{foo:{bar:'l-foo'}}", getSwaggerWithFile(new OD05b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
	}

	//=================================================================================================================
	// /paths/<path>/<method>/responses/<response>/schema
	//=================================================================================================================

	@RestResource
	public static class OE01a {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public void doFoo(Value<OE01x> foo) {}
	}
	@RestResource
	public static class OE01b {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public OE01x doFoo() {return null;}
	}
	@Response(code=100)
	public static class OE01x extends Foo {}

	@Test
	public void oe01a_responses_100_schema_default() throws Exception {
		assertObjectEquals("{type:'object',properties:{id:{format:'int32',type:'integer'}}}", getSwagger(new OE01a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema());
		assertObjectEquals("{type:'array',items:{'$ref':'#/definitions/Foo'}}", getSwaggerWithFile(new OE01a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema());
	}
	@Test
	public void oe01b_responses_100_schema_default() throws Exception {
		assertObjectEquals("{type:'object',properties:{id:{format:'int32',type:'integer'}}}", getSwagger(new OE01b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema());
		assertObjectEquals("{type:'array',items:{'$ref':'#/definitions/Foo'}}", getSwaggerWithFile(new OE01b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{schema:{$ref:'b'}}}}}}"))
	public static class OE02 {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public void doFoo(@ResponseStatus Value<Integer> foo) {}
	}

	@Test
	public void oe02_response_100_schema_swaggerOnClass() throws Exception {
		assertObjectEquals("{'$ref':'b'}", getSwagger(new OE02()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema());
		assertObjectEquals("{'$ref':'b'}", getSwaggerWithFile(new OE02()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{schema:{$ref:'b'}}}}}}"))
	public static class OE03 {
		@RestMethod(name=GET,path="/path/{foo}/responses/100",swagger=@MethodSwagger("responses:{100:{schema:{$ref:'c'}}}}"))
		public void doFoo(@ResponseStatus Value<Integer> foo) {}
	}

	@Test
	public void oe03_response_100_schema_swaggerOnMethoe() throws Exception {
		assertObjectEquals("{'$ref':'c'}", getSwagger(new OE03()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema());
		assertObjectEquals("{'$ref':'c'}", getSwaggerWithFile(new OE03()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{schema:{$ref:'b'}}}}}}"))
	public static class OE04a {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public void doFoo(Value<OE04x> foo) {}
	}
	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{schema:{$ref:'b'}}}}}}"))
	public static class OE04b {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public OE04x doFoo() {return null;}
	}
	@Response(code=100,schema=@Schema($ref="d"))
	public static class OE04x extends Foo {}

	@Test
	public void oe04a_response_100_schema_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("{'$ref':'d'}", getSwagger(new OE04a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema());
		assertObjectEquals("{'$ref':'d'}", getSwaggerWithFile(new OE04a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema());
	}
	@Test
	public void oe04b_response_100_schema_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("{'$ref':'d'}", getSwagger(new OE04b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema());
		assertObjectEquals("{'$ref':'d'}", getSwaggerWithFile(new OE04b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{schema:{$ref:'b'}}}}}}"))
	public static class OE05a {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public void doFoo(Value<OE05x> foo) {}
	}
	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{schema:{$ref:'b'}}}}}}"))
	public static class OE05b {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public OE05x doFoo() {return null;}
	}
	@Response(code=100,schema=@Schema("{$ref:'$L{foo}'}"))
	public static class OE05x extends Foo {}

	@Test
	public void oe05a_response_100_schema_swaggerOnAnnotation_loealized() throws Exception {
		assertObjectEquals("{'$ref':'l-foo'}", getSwagger(new OE05a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema());
		assertObjectEquals("{'$ref':'l-foo'}", getSwaggerWithFile(new OE05a()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema());
	}
	@Test
	public void oe05b_response_100_schema_swaggerOnAnnotation_loealized() throws Exception {
		assertObjectEquals("{'$ref':'l-foo'}", getSwagger(new OE05b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema());
		assertObjectEquals("{'$ref':'l-foo'}", getSwaggerWithFile(new OE05b()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getSchema());
	}

	@Bean(typeName="Foo")
	public static class Foo {
		public int id;
	}

	//=================================================================================================================
	// Example bean with getter-only property.
	//=================================================================================================================

	@RestResource
	public static class P extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestMethod(name=GET,path="/")
		public P01 doFoo(@Body P01 body) {
			return null;
		}
	}

	@Bean(sort=true)
	public static class P01 {
		private int f1;

		public P01 setF1(int f1) {
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
		public static P01 example() {
			return new P01().setF1(1);
		}
	}

	static MockRest p = MockRest.build(P.class);

	@Test
	public void p01_bodyWithReadOnlyProperty() throws Exception {
		Swagger s = JsonParser.DEFAULT.parse(p.options("/").accept("application/json").execute().getBodyAsString(), Swagger.class);
		Operation o = s.getOperation("/", "get");
		ParameterInfo pi = o.getParameter("body", null);
		assertEquals("{\n\tf1: 1,\n\tf2: 2\n}", pi.getExamples().get("application/json+simple"));
		ResponseInfo ri = o.getResponse("200");
		assertEquals("{\n\tf1: 1,\n\tf2: 2\n}", ri.getExamples().get("application/json+simple"));
	}
}
