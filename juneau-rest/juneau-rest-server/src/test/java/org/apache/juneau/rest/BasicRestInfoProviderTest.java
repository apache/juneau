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

import static org.junit.Assert.assertEquals;
import static org.apache.juneau.rest.TestUtils.*;
import static org.apache.juneau.http.HttpMethodName.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.json.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.utils.*;
import org.junit.*;
import org.junit.runners.*;

@SuppressWarnings("javadoc")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BasicRestInfoProviderTest {
	
	private Swagger getSwaggerWithFile(Object resource) throws Exception {
		RestContext rc = RestContext.create(resource).classpathResourceFinder(TestClasspathResourceFinder.class).build();
		RestRequest req = rc.getCallHandler().createRequest(new MockHttpServletRequest());
		RestInfoProvider ip = rc.getInfoProvider();
		return ip.getSwagger(req);
	}

	private Swagger getSwagger(Object resource) throws Exception {
		RestContext rc = RestContext.create(resource).build();
		RestRequest req = rc.getCallHandler().createRequest(new MockHttpServletRequest());
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
	
	
	//-----------------------------------------------------------------------------------------------------------------
	// /swagger
	// "swagger": "2.0",
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class A01 {}
	
	@Test
	public void a01_swagger_default() throws Exception {
		assertEquals("2.0", getSwagger(new A01()).getSwagger());
		assertEquals("0.0", getSwaggerWithFile(new A01()).getSwagger());
	}

	
	@RestResource(swagger=@ResourceSwagger("{swagger:'3.0'}"))
	public static class A02 {}
	
	@Test
	public void a02_swagger_ResourceSwagger_value() throws Exception {
		assertEquals("3.0", getSwagger(new A02()).getSwagger());
		assertEquals("3.0", getSwaggerWithFile(new A02()).getSwagger());
	}
	
	
	//-----------------------------------------------------------------------------------------------------------------
	// /info/title
	// "title": "Swagger Petstore",
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource(title="a-title")
	public static class B01 {}

	@Test
	public void b01_title_RestResource_title() throws Exception {
		assertEquals("a-title", getSwagger(new B01()).getInfo().getTitle());
		assertEquals("s-title", getSwaggerWithFile(new B01()).getInfo().getTitle());
	}

	
	@RestResource(title="$L{foo}",messages="BasicRestInfoProviderTest")
	public static class B02 {}

	@Test
	public void b02_title_RestResource_title_localized() throws Exception {
		assertEquals("l-foo", getSwagger(new B02()).getInfo().getTitle());
		assertEquals("s-title", getSwaggerWithFile(new B02()).getInfo().getTitle());
	}

	
	@RestResource(title="a-title", swagger=@ResourceSwagger("{info:{title:'b-title'}}"))
	public static class B03 {}

	@Test
	public void b03_title_ResourceSwagger_value() throws Exception {
		assertEquals("b-title", getSwagger(new B03()).getInfo().getTitle());
		assertEquals("b-title", getSwaggerWithFile(new B03()).getInfo().getTitle());
	}
	

	@RestResource(title="a-title", swagger=@ResourceSwagger("{info:{title:'$L{bar}'}}"), messages="BasicRestInfoProviderTest")
	public static class B04 {}
	
	@Test
	public void b04_title_ResourceSwagger_value_localised() throws Exception {
		assertEquals("l-bar", getSwagger(new B04()).getInfo().getTitle());
		assertEquals("l-bar", getSwaggerWithFile(new B04()).getInfo().getTitle());
	}

	
	@RestResource(title="a-title", swagger=@ResourceSwagger(value="{info:{title:'b-title'}}", title="c-title"))
	public static class B05 {}

	@Test
	public void b05_title_ResourceSwagger_title() throws Exception {
		assertEquals("c-title", getSwagger(new B05()).getInfo().getTitle());
		assertEquals("c-title", getSwaggerWithFile(new B05()).getInfo().getTitle());
	}
	
	
	@RestResource(title="a-title", swagger=@ResourceSwagger(value="{info:{title:'b-title'}}", title="$L{baz}"), messages="BasicRestInfoProviderTest")
	public static class B06 {}
	
	@Test
	public void b06_title_ResourceSwagger_title_localized() throws Exception {
		assertEquals("l-baz", getSwagger(new B06()).getInfo().getTitle());
		assertEquals("l-baz", getSwaggerWithFile(new B06()).getInfo().getTitle());
	}

	
	@RestResource(swagger=@ResourceSwagger(title="c-title"))
	public static class B07 {}

	@Test
	public void b07_title_ResourceSwagger_title_only() throws Exception {
		assertEquals("c-title", getSwagger(new B07()).getInfo().getTitle());
		assertEquals("c-title", getSwaggerWithFile(new B07()).getInfo().getTitle());
	}
	
	
	//-----------------------------------------------------------------------------------------------------------------
	// /info/description
	// "description": "This is a sample server Petstore server.  You can find out more about Swagger at [http://swagger.io](http://swagger.io) or on [irc.freenode.net, #swagger](http://swagger.io/irc/).  For this sample, you can use the api key `special-key` to test the authorization filters.",
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource(description="a-description")
	public static class C01 {}

	@Test
	public void c01_description_RestResource_description() throws Exception {
		assertEquals("a-description", getSwagger(new C01()).getInfo().getDescription());
		assertEquals("s-description", getSwaggerWithFile(new C01()).getInfo().getDescription());
	}

	
	@RestResource(description="$L{foo}",messages="BasicRestInfoProviderTest")
	public static class C02 {}

	@Test
	public void c02_description_RestResource_description_localized() throws Exception {
		assertEquals("l-foo", getSwagger(new C02()).getInfo().getDescription());
		assertEquals("s-description", getSwaggerWithFile(new C02()).getInfo().getDescription());
	}

	
	@RestResource(description="a-description", swagger=@ResourceSwagger("{info:{description:'b-description'}}"))
	public static class C03 {}

	@Test
	public void c03_description_ResourceSwagger_value() throws Exception {
		assertEquals("b-description", getSwagger(new C03()).getInfo().getDescription());
		assertEquals("b-description", getSwaggerWithFile(new C03()).getInfo().getDescription());
	}
	

	@RestResource(description="a-description", swagger=@ResourceSwagger("{info:{description:'$L{bar}'}}"), messages="BasicRestInfoProviderTest")
	public static class C04 {}
	
	@Test
	public void c04_description_ResourceSwagger_value_localised() throws Exception {
		assertEquals("l-bar", getSwagger(new C04()).getInfo().getDescription());
		assertEquals("l-bar", getSwaggerWithFile(new C04()).getInfo().getDescription());
	}

	
	@RestResource(description="a-description", swagger=@ResourceSwagger(value="{info:{description:'b-description'}}", description="c-description"))
	public static class C05 {}

	@Test
	public void c05_description_ResourceSwagger_description() throws Exception {
		assertEquals("c-description", getSwagger(new C05()).getInfo().getDescription());
		assertEquals("c-description", getSwaggerWithFile(new C05()).getInfo().getDescription());
	}
	
	
	@RestResource(description="a-description", swagger=@ResourceSwagger(value="{info:{description:'b-description'}}", description="$L{baz}"), messages="BasicRestInfoProviderTest")
	public static class C06 {}
	
	@Test
	public void c06_description_ResourceSwagger_description_localized() throws Exception {
		assertEquals("l-baz", getSwagger(new C06()).getInfo().getDescription());
		assertEquals("l-baz", getSwaggerWithFile(new C06()).getInfo().getDescription());
	}

	
	@RestResource(swagger=@ResourceSwagger(description="c-description"))
	public static class C07 {}

	@Test
	public void c07_description_ResourceSwagger_description_only() throws Exception {
		assertEquals("c-description", getSwagger(new C07()).getInfo().getDescription());
		assertEquals("c-description", getSwaggerWithFile(new C07()).getInfo().getDescription());
	}

	
	//-----------------------------------------------------------------------------------------------------------------
	// /info/version
	// "version": "1.0.0",
	//-----------------------------------------------------------------------------------------------------------------
	
	@RestResource()
	public static class D01 {}
	
	@Test
	public void d01_version_default() throws Exception {
		assertEquals(null, getSwagger(new D01()).getInfo().getVersion());
		assertEquals("0.0.0", getSwaggerWithFile(new D01()).getInfo().getVersion());
	}


	@RestResource(swagger=@ResourceSwagger("{info:{version:'2.0.0'}}"))
	public static class D02 {}
	
	@Test
	public void d02_version_ResourceSwagger_value() throws Exception {
		assertEquals("2.0.0", getSwagger(new D02()).getInfo().getVersion());
		assertEquals("2.0.0", getSwaggerWithFile(new D02()).getInfo().getVersion());
	}

	
	@RestResource(swagger=@ResourceSwagger(value="{info:{version:'2.0.0'}}", version="3.0.0"))
	public static class D03 {}
	
	@Test
	public void d03_version_ResourceSwagger_version() throws Exception {
		assertEquals("3.0.0", getSwagger(new D03()).getInfo().getVersion());
		assertEquals("3.0.0", getSwaggerWithFile(new D03()).getInfo().getVersion());
	}

	@RestResource(swagger=@ResourceSwagger(value="{info:{version:'2.0.0'}}", version="$L{foo}"), messages="BasicRestInfoProviderTest")
	public static class D04 {}
	
	@Test
	public void d04_version_ResourceSwagger_version_localised() throws Exception {
		assertEquals("l-foo", getSwagger(new D04()).getInfo().getVersion());
		assertEquals("l-foo", getSwaggerWithFile(new D04()).getInfo().getVersion());
	}
	

	//-----------------------------------------------------------------------------------------------------------------
	// /info/termsOfService
	// "termsOfService": "http://swagger.io/terms/",
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class E01 {}
	
	@Test
	public void e01_termsOfService_default() throws Exception {
		assertEquals(null, getSwagger(new E01()).getInfo().getTermsOfService());
		assertEquals("s-termsOfService", getSwaggerWithFile(new E01()).getInfo().getTermsOfService());
	}


	@RestResource(swagger=@ResourceSwagger("{info:{termsOfService:'a-termsOfService'}}"))
	public static class E02 {}
	
	@Test
	public void e02_termsOfService_ResourceSwagger_value() throws Exception {
		assertEquals("a-termsOfService", getSwagger(new E02()).getInfo().getTermsOfService());
		assertEquals("a-termsOfService", getSwaggerWithFile(new E02()).getInfo().getTermsOfService());
	}

	
	@RestResource(swagger=@ResourceSwagger(value="{info:{termsOfService:'a-termsOfService'}}", termsOfService="b-termsOfService"))
	public static class E03 {}
	
	@Test
	public void e03_termsOfService_ResourceSwagger_termsOfService() throws Exception {
		assertEquals("b-termsOfService", getSwagger(new E03()).getInfo().getTermsOfService());
		assertEquals("b-termsOfService", getSwaggerWithFile(new E03()).getInfo().getTermsOfService());
	}

	@RestResource(swagger=@ResourceSwagger(value="{info:{termsOfService:'a-termsOfService'}}", termsOfService="$L{foo}"), messages="BasicRestInfoProviderTest")
	public static class E04 {}
	
	@Test
	public void e04_termsOfService_ResourceSwagger_termsOfService_localised() throws Exception {
		assertEquals("l-foo", getSwagger(new E04()).getInfo().getTermsOfService());
		assertEquals("l-foo", getSwaggerWithFile(new E04()).getInfo().getTermsOfService());
	}

	
	//-----------------------------------------------------------------------------------------------------------------
	// /info/contact
	// "contact": {
	// 	"email": "apiteam@swagger.io"
	// },
	//-----------------------------------------------------------------------------------------------------------------
	
	@RestResource()
	public static class F01 {}
	
	@Test
	public void f01_contact_default() throws Exception {
		assertEquals(null, getSwagger(new F01()).getInfo().getContact());
		assertObjectEquals("{name:'s-name',url:'s-url',email:'s-email'}", getSwaggerWithFile(new F01()).getInfo().getContact());
	}


	@RestResource(swagger=@ResourceSwagger("{info:{contact:{name:'a-name',url:'a-url',email:'a-email'}}}"))
	public static class F02 {}
	
	@Test
	public void f02_contact_ResourceSwagger_value() throws Exception {
		assertObjectEquals("{name:'a-name',url:'a-url',email:'a-email'}", getSwagger(new F02()).getInfo().getContact());
		assertObjectEquals("{name:'a-name',url:'a-url',email:'a-email'}", getSwaggerWithFile(new F02()).getInfo().getContact());
	}

	
	@RestResource(swagger=@ResourceSwagger(value="{info:{contact:{name:'a-name',url:'a-url',email:'a-email'}}}", contact="{name:'b-name',url:'b-url',email:'b-email'}"))
	public static class F03 {}
	
	@Test
	public void f03_contact_ResourceSwagger_contact() throws Exception {
		assertObjectEquals("{name:'b-name',url:'b-url',email:'b-email'}", getSwagger(new F03()).getInfo().getContact());
		assertObjectEquals("{name:'b-name',url:'b-url',email:'b-email'}", getSwaggerWithFile(new F03()).getInfo().getContact());
	}

	@RestResource(swagger=@ResourceSwagger(value="{info:{contact:{name:'a-name',url:'a-url',email:'a-email'}}}", contact=" name:'b-name', url:'b-url', email:'b-email' "))
	public static class F04 {}
	
	@Test
	public void f04_contact_ResourceSwagger_contact() throws Exception {
		assertObjectEquals("{name:'b-name',url:'b-url',email:'b-email'}", getSwagger(new F04()).getInfo().getContact());
		assertObjectEquals("{name:'b-name',url:'b-url',email:'b-email'}", getSwaggerWithFile(new F04()).getInfo().getContact());
	}

	@RestResource(swagger=@ResourceSwagger(value="{info:{contact:{name:'a-name',url:'a-url',email:'a-email'}}}", contact="{name:'$L{foo}',url:'$L{bar}',email:'$L{baz}'}"), messages="BasicRestInfoProviderTest")
	public static class F05 {}
	
	@Test
	public void f05_contact_ResourceSwagger_contact_localised() throws Exception {
		assertObjectEquals("{name:'l-foo',url:'l-bar',email:'l-baz'}", getSwagger(new F05()).getInfo().getContact());
		assertObjectEquals("{name:'l-foo',url:'l-bar',email:'l-baz'}", getSwaggerWithFile(new F05()).getInfo().getContact());
	}
	

	//-----------------------------------------------------------------------------------------------------------------
	// /info/license
	// "license": {
	// 	"name": "Apache 2.0",
	// 	"url": "http://www.apache.org/licenses/LICENSE-2.0.html"
	// }
	//-----------------------------------------------------------------------------------------------------------------
	
	@RestResource()
	public static class G01 {}
	
	@Test
	public void g01_license_default() throws Exception {
		assertEquals(null, getSwagger(new G01()).getInfo().getLicense());
		assertObjectEquals("{name:'s-name',url:'s-url'}", getSwaggerWithFile(new G01()).getInfo().getLicense());
	}


	@RestResource(swagger=@ResourceSwagger("{info:{license:{name:'a-name',url:'a-url'}}}"))
	public static class G02 {}
	
	@Test
	public void g02_license_ResourceSwagger_value() throws Exception {
		assertObjectEquals("{name:'a-name',url:'a-url'}", getSwagger(new G02()).getInfo().getLicense());
		assertObjectEquals("{name:'a-name',url:'a-url'}", getSwaggerWithFile(new G02()).getInfo().getLicense());
	}

	
	@RestResource(swagger=@ResourceSwagger(value="{info:{license:{name:'a-name',url:'a-url'}}}", license="{name:'b-name',url:'b-url'}"))
	public static class G03 {}
	
	@Test
	public void g03_license_ResourceSwagger_license() throws Exception {
		assertObjectEquals("{name:'b-name',url:'b-url'}", getSwagger(new G03()).getInfo().getLicense());
		assertObjectEquals("{name:'b-name',url:'b-url'}", getSwaggerWithFile(new G03()).getInfo().getLicense());
	}

	@RestResource(swagger=@ResourceSwagger(value="{info:{license:{name:'a-name',url:'a-url'}}}", license=" name:'b-name', url:'b-url' "))
	public static class G04 {}
	
	@Test
	public void g04_license_ResourceSwagger_license() throws Exception {
		assertObjectEquals("{name:'b-name',url:'b-url'}", getSwagger(new G04()).getInfo().getLicense());
		assertObjectEquals("{name:'b-name',url:'b-url'}", getSwaggerWithFile(new G04()).getInfo().getLicense());
	}

	@RestResource(swagger=@ResourceSwagger(value="{info:{license:{name:'a-name',url:'a-url'}}}", license="{name:'$L{foo}',url:'$L{bar}'}"), messages="BasicRestInfoProviderTest")
	public static class G05 {}
	
	@Test
	public void g05_license_ResourceSwagger_license_localised() throws Exception {
		assertObjectEquals("{name:'l-foo',url:'l-bar'}", getSwagger(new G05()).getInfo().getLicense());
		assertObjectEquals("{name:'l-foo',url:'l-bar'}", getSwaggerWithFile(new G05()).getInfo().getLicense());
	}

	
	//-----------------------------------------------------------------------------------------------------------------
	// /host
	// "host": "petstore.swagger.io",
	//-----------------------------------------------------------------------------------------------------------------
	
	@RestResource()
	public static class H01 {}
	
	@Test
	public void h01_host_default() throws Exception {
		assertEquals(null, getSwagger(new H01()).getHost());
		assertEquals("s-host", getSwaggerWithFile(new H01()).getHost());
	}

	
	@RestResource(swagger=@ResourceSwagger("{host:'a-host'}"))
	public static class H02 {}
	
	@Test
	public void h02_host_ResourceSwagger_value() throws Exception {
		assertEquals("a-host", getSwagger(new H02()).getHost());
		assertEquals("a-host", getSwaggerWithFile(new H02()).getHost());
	}
	
	
	//-----------------------------------------------------------------------------------------------------------------
	// /basePath
	// "basePath": "/v2",
	//-----------------------------------------------------------------------------------------------------------------
	
	@RestResource()
	public static class I01 {}
	
	@Test
	public void i01_basePath_default() throws Exception {
		assertEquals(null, getSwagger(new I01()).getBasePath());
		assertEquals("s-basePath", getSwaggerWithFile(new I01()).getBasePath());
	}

	
	@RestResource(swagger=@ResourceSwagger("{basePath:'a-basePath'}"))
	public static class I02 {}
	
	@Test
	public void i02_basePath_ResourceSwagger_value() throws Exception {
		assertEquals("a-basePath", getSwagger(new I02()).getBasePath());
		assertEquals("a-basePath", getSwaggerWithFile(new I02()).getBasePath());
	}

	
	//-----------------------------------------------------------------------------------------------------------------
	// /tags
	// "tags": [
	//	{
	//		"name": "pet",
	//		"description": "Everything about your Pets",
	//		"externalDocs": {
	//			"description": "Find out more",
	//			"url": "http://swagger.io"
	//		}
	//	},
	// ],
	//-----------------------------------------------------------------------------------------------------------------
	
	@RestResource()
	public static class J01 {}
	
	@Test
	public void j01_tags_default() throws Exception {
		assertEquals(null, getSwagger(new J01()).getTags());
		assertObjectEquals("[{name:'s-name',description:'s-description',externalDocs:{description:'s-description',url:'s-url'}}]", getSwaggerWithFile(new J01()).getTags());
	}

	// Tags in @ResourceSwagger(value) should override file.
	@RestResource(swagger=@ResourceSwagger("{tags:[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}}]}"))
	public static class J02 {}
	
	@Test
	public void j02_tags_ResourceSwagger_value() throws Exception {
		assertObjectEquals("[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}}]", getSwagger(new J02()).getTags());
		assertObjectEquals("[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}}]", getSwaggerWithFile(new J02()).getTags());
	}

	// Tags in both @ResourceSwagger(value) and @ResourceSwagger(tags) should accumulate.
	@RestResource(swagger=@ResourceSwagger(value="{tags:[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}}]}", tags="[{name:'b-name',description:'b-description',externalDocs:{description:'b-description',url:'b-url'}}]"))
	public static class J03 {}
	
	@Test
	public void j03_tags_ResourceSwagger_tags() throws Exception {
		assertObjectEquals("[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}},{name:'b-name',description:'b-description',externalDocs:{description:'b-description',url:'b-url'}}]", getSwagger(new J03()).getTags());
		assertObjectEquals("[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}},{name:'b-name',description:'b-description',externalDocs:{description:'b-description',url:'b-url'}}]", getSwaggerWithFile(new J03()).getTags());
	}

	// Same as above but without [] outer characters.
	@RestResource(swagger=@ResourceSwagger(value="{tags:[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}}]}", tags=" { name:'b-name', description:'b-description', externalDocs: { description:'b-description', url:'b-url' } } "))
	public static class J04 {}
	
	@Test
	public void j04_tags_ResourceSwagger_tags() throws Exception {
		assertObjectEquals("[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}},{name:'b-name',description:'b-description',externalDocs:{description:'b-description',url:'b-url'}}]", getSwagger(new J04()).getTags());
		assertObjectEquals("[{name:'a-name',description:'a-description',externalDocs:{description:'a-description',url:'a-url'}},{name:'b-name',description:'b-description',externalDocs:{description:'b-description',url:'b-url'}}]", getSwaggerWithFile(new J04()).getTags());
	}
	
	// Tags in both Swagger.json and @ResourceSwagger(tags) should accumulate.
	@RestResource(swagger=@ResourceSwagger(tags="[{name:'b-name',description:'b-description',externalDocs:{description:'b-description',url:'b-url'}}]"))
	public static class J05 {}
	
	@Test
	public void j05_tags_ResourceSwagger_tags_only() throws Exception {
		assertObjectEquals("[{name:'b-name',description:'b-description',externalDocs:{description:'b-description',url:'b-url'}}]", getSwagger(new J05()).getTags());
		assertObjectEquals("[{name:'s-name',description:'s-description',externalDocs:{description:'s-description',url:'s-url'}},{name:'b-name',description:'b-description',externalDocs:{description:'b-description',url:'b-url'}}]", getSwaggerWithFile(new J05()).getTags());
	}
	
	// Dup tag names should be overwritten
	@RestResource(swagger=@ResourceSwagger(tags="[{name:'s-name',description:'b-description',externalDocs:{description:'b-description',url:'b-url'}},{name:'s-name',description:'c-description',externalDocs:{description:'c-description',url:'c-url'}}]"))
	public static class J06 {}
	
	@Test
	public void j06_tags_ResourceSwagger_tags_dups() throws Exception {
		assertObjectEquals("[{name:'s-name',description:'c-description',externalDocs:{description:'c-description',url:'c-url'}}]", getSwagger(new J06()).getTags());
		assertObjectEquals("[{name:'s-name',description:'c-description',externalDocs:{description:'c-description',url:'c-url'}}]", getSwaggerWithFile(new J06()).getTags());
	}


	@RestResource(swagger=@ResourceSwagger(value="{tags:[{name:'$L{foo}',description:'$L{foo}',externalDocs:{description:'$L{foo}',url:'$L{foo}'}}]}", tags="[{name:'$L{foo}',description:'$L{foo}',externalDocs:{description:'$L{foo}',url:'$L{foo}'}}]"), messages="BasicRestInfoProviderTest")
	public static class J07 {}
	
	@Test
	public void j07_tags_ResourceSwagger_tags_localised() throws Exception {
		assertObjectEquals("[{name:'l-foo',description:'l-foo',externalDocs:{description:'l-foo',url:'l-foo'}}]", getSwagger(new J07()).getTags());
		assertObjectEquals("[{name:'l-foo',description:'l-foo',externalDocs:{description:'l-foo',url:'l-foo'}}]", getSwaggerWithFile(new J07()).getTags());
	}

	// Auto-detect tags defined on methods.
	@RestResource()
	public static class J08 {
		
		@RestMethod(swagger=@MethodSwagger(tags="foo"))
		public void doFoo() {}
	}
	
	@Test
	public void j08_tags_ResourceSwagger_tags_loose() throws Exception {
		assertObjectEquals("[{name:'foo'}]", getSwagger(new J08()).getTags());
		assertObjectEquals("[{name:'s-name',description:'s-description',externalDocs:{description:'s-description',url:'s-url'}},{name:'foo'}]", getSwaggerWithFile(new J08()).getTags());
	}
	
	// Comma-delimited list
	@RestResource()
	public static class J09 {
		
		@RestMethod(swagger=@MethodSwagger(tags=" foo, bar "))
		public void doFoo() {}
	}
	
	@Test
	public void j09_tags_ResourceSwagger_tags_loose_cdl() throws Exception {
		assertObjectEquals("[{name:'foo'},{name:'bar'}]", getSwagger(new J09()).getTags());
		assertObjectEquals("[{name:'s-name',description:'s-description',externalDocs:{description:'s-description',url:'s-url'}},{name:'foo'},{name:'bar'}]", getSwaggerWithFile(new J09()).getTags());
	}

	// ObjectList
	@RestResource()
	public static class J10 {
		
		@RestMethod(swagger=@MethodSwagger(tags="['foo', 'bar']"))
		public void doFoo() {}
	}
	
	@Test
	public void j10_tags_ResourceSwagger_tags_loose_objectlist() throws Exception {
		assertObjectEquals("[{name:'foo'},{name:'bar'}]", getSwagger(new J10()).getTags());
		assertObjectEquals("[{name:'s-name',description:'s-description',externalDocs:{description:'s-description',url:'s-url'}},{name:'foo'},{name:'bar'}]", getSwaggerWithFile(new J10()).getTags());
	}

	// ObjectList localized
	@RestResource(messages="BasicRestInfoProviderTest")
	public static class J11 {
		
		@RestMethod(swagger=@MethodSwagger(tags="['$L{foo}', '$L{bar}']"))
		public void doFoo() {}
	}
	
	@Test
	public void j11_tags_ResourceSwagger_tags_loose_objectlist_localized() throws Exception {
		assertObjectEquals("[{name:'l-foo'},{name:'l-bar'}]", getSwagger(new J11()).getTags());
		assertObjectEquals("[{name:'s-name',description:'s-description',externalDocs:{description:'s-description',url:'s-url'}},{name:'l-foo'},{name:'l-bar'}]", getSwaggerWithFile(new J11()).getTags());
	}

	// Comma-delimited list localized
	@RestResource(messages="BasicRestInfoProviderTest")
	public static class J12 {
		
		@RestMethod(swagger=@MethodSwagger(tags=" $L{foo}, $L{bar} "))
		public void doFoo() {}
	}
	
	@Test
	public void j12_tags_ResourceSwagger_tags_loose_cdl_localized() throws Exception {
		assertObjectEquals("[{name:'l-foo'},{name:'l-bar'}]", getSwagger(new J12()).getTags());
		assertObjectEquals("[{name:'s-name',description:'s-description',externalDocs:{description:'s-description',url:'s-url'}},{name:'l-foo'},{name:'l-bar'}]", getSwaggerWithFile(new J12()).getTags());
	}

	
	//-----------------------------------------------------------------------------------------------------------------
	// /schemes
	// "schemes": [
	//	"http"
	// ],
	//-----------------------------------------------------------------------------------------------------------------
	
	@RestResource()
	public static class K01 {}
	
	@Test
	public void k01_schemes_default() throws Exception {
		assertEquals(null, getSwagger(new K01()).getSchemes());
		assertObjectEquals("['s-scheme']", getSwaggerWithFile(new K01()).getSchemes());
	}

	
	@RestResource(swagger=@ResourceSwagger("{schemes:['a-scheme']}"))
	public static class K02 {}
	
	@Test
	public void k02_schemes_ResourceSwagger_value() throws Exception {
		assertObjectEquals("['a-scheme']", getSwagger(new K02()).getSchemes());
		assertObjectEquals("['a-scheme']", getSwaggerWithFile(new K02()).getSchemes());
	}
	
	
	//-----------------------------------------------------------------------------------------------------------------
	// /externalDocs
	// "externalDocs": {
	//	"description": "Find out more about Swagger",
	//	"url": "http://swagger.io"
	// }
	//-----------------------------------------------------------------------------------------------------------------
	
	@RestResource()
	public static class L01 {}
	
	@Test
	public void l01_externalDocs_default() throws Exception {
		assertEquals(null, getSwagger(new L01()).getExternalDocs());
		assertObjectEquals("{description:'s-description',url:'s-url'}", getSwaggerWithFile(new L01()).getExternalDocs());
	}


	@RestResource(swagger=@ResourceSwagger("{externalDocs:{description:'a-description',url:'a-url'}}"))
	public static class L02 {}
	
	@Test
	public void l02_externalDocs_ResourceSwagger_value() throws Exception {
		assertObjectEquals("{description:'a-description',url:'a-url'}", getSwagger(new L02()).getExternalDocs());
		assertObjectEquals("{description:'a-description',url:'a-url'}", getSwaggerWithFile(new L02()).getExternalDocs());
	}

	
	@RestResource(swagger=@ResourceSwagger(value="{externalDocs:{description:'a-description',url:'a-url'}}", externalDocs="{description:'b-description',url:'b-url'}"))
	public static class L03 {}
	
	@Test
	public void l03_externalDocs_ResourceSwagger_externalDocs() throws Exception {
		assertObjectEquals("{description:'b-description',url:'b-url'}", getSwagger(new L03()).getExternalDocs());
		assertObjectEquals("{description:'b-description',url:'b-url'}", getSwaggerWithFile(new L03()).getExternalDocs());
	}

	@RestResource(swagger=@ResourceSwagger(value="{info:{externalDocs:{description:'a-description',url:'a-url'}}}", externalDocs=" description:'b-description', url:'b-url' "))
	public static class L04 {}
	
	@Test
	public void l04_externalDocs_ResourceSwagger_externalDocs() throws Exception {
		assertObjectEquals("{description:'b-description',url:'b-url'}", getSwagger(new L04()).getExternalDocs());
		assertObjectEquals("{description:'b-description',url:'b-url'}", getSwaggerWithFile(new L04()).getExternalDocs());
	}

	@RestResource(swagger=@ResourceSwagger(value="{externalDocs:{description:'a-description',url:'a-url'}}", externalDocs="{description:'$L{foo}',url:'$L{bar}'}"), messages="BasicRestInfoProviderTest")
	public static class L05 {}
	
	@Test
	public void l05_externalDocs_ResourceSwagger_externalDocs_localised() throws Exception {
		assertObjectEquals("{description:'l-foo',url:'l-bar'}", getSwagger(new L05()).getExternalDocs());
		assertObjectEquals("{description:'l-foo',url:'l-bar'}", getSwaggerWithFile(new L05()).getExternalDocs());
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/operationId
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class MA01 {
		
		@RestMethod(name=GET,path="/path/{foo}")
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void ma01_operation_operationId_default() throws Exception {
		assertEquals("doFoo", getSwagger(new MA01()).getPaths().get("/path/{foo}").get("get").getOperationId());
		assertEquals("s-operationId", getSwaggerWithFile(new MA01()).getPaths().get("/path/{foo}").get("get").getOperationId());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{operationId:'a-operationId'}}}"))
	public static class MA02 {		
		@RestMethod(name=GET,path="/path/{foo}")
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void ma02_operation_operationId_swaggerOnClass() throws Exception {
		assertEquals("a-operationId", getSwagger(new MA02()).getPaths().get("/path/{foo}").get("get").getOperationId());
		assertEquals("a-operationId", getSwaggerWithFile(new MA02()).getPaths().get("/path/{foo}").get("get").getOperationId());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{operationId:'a-operationId'}}}"))
	public static class MA03 {		
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger("operationId:'b-operationId'"))
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void ma03_operation_operationId_swaggerOnMethod() throws Exception {
		assertEquals("b-operationId", getSwagger(new MA03()).getPaths().get("/path/{foo}").get("get").getOperationId());
		assertEquals("b-operationId", getSwaggerWithFile(new MA03()).getPaths().get("/path/{foo}").get("get").getOperationId());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{operationId:'a-operationId'}}}"))
	public static class MA04 {		
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(operationId="c-operationId"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void ma04_operation_operationId_swaggerOnAnnotation() throws Exception {
		assertEquals("c-operationId", getSwagger(new MA04()).getPaths().get("/path/{foo}").get("get").getOperationId());
		assertEquals("c-operationId", getSwaggerWithFile(new MA04()).getPaths().get("/path/{foo}").get("get").getOperationId());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{operationId:'a-operationId'}}}"))
	public static class MA05 {		
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(operationId="$L{foo}"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void ma05_operation_operationId_swaggerOnAnnotation_localized() throws Exception {
		assertEquals("l-foo", getSwagger(new MA05()).getPaths().get("/path/{foo}").get("get").getOperationId());
		assertEquals("l-foo", getSwaggerWithFile(new MA05()).getPaths().get("/path/{foo}").get("get").getOperationId());
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/summary
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class MB01 {
		
		@RestMethod(name=GET,path="/path/{foo}")
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void mb01_operation_summary_default() throws Exception {
		assertEquals(null, getSwagger(new MB01()).getPaths().get("/path/{foo}").get("get").getSummary());
		assertEquals("s-summary", getSwaggerWithFile(new MB01()).getPaths().get("/path/{foo}").get("get").getSummary());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{summary:'a-summary'}}}"))
	public static class MB02 {		
		@RestMethod(name=GET,path="/path/{foo}")
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void mb02_operation_summary_swaggerOnClass() throws Exception {
		assertEquals("a-summary", getSwagger(new MB02()).getPaths().get("/path/{foo}").get("get").getSummary());
		assertEquals("a-summary", getSwaggerWithFile(new MB02()).getPaths().get("/path/{foo}").get("get").getSummary());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{summary:'a-summary'}}}"))
	public static class MB03 {		
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger("summary:'b-summary'"))
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void mb03_operation_summary_swaggerOnMethod() throws Exception {
		assertEquals("b-summary", getSwagger(new MB03()).getPaths().get("/path/{foo}").get("get").getSummary());
		assertEquals("b-summary", getSwaggerWithFile(new MB03()).getPaths().get("/path/{foo}").get("get").getSummary());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{summary:'a-summary'}}}"))
	public static class MB04 {		
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(summary="c-summary"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mb04_operation_summary_swaggerOnAnnotation() throws Exception {
		assertEquals("c-summary", getSwagger(new MB04()).getPaths().get("/path/{foo}").get("get").getSummary());
		assertEquals("c-summary", getSwaggerWithFile(new MB04()).getPaths().get("/path/{foo}").get("get").getSummary());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{summary:'a-summary'}}}"))
	public static class MB05 {		
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(summary="$L{foo}"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mb05_operation_summary_swaggerOnAnnotation_localized() throws Exception {
		assertEquals("l-foo", getSwagger(new MB05()).getPaths().get("/path/{foo}").get("get").getSummary());
		assertEquals("l-foo", getSwaggerWithFile(new MB05()).getPaths().get("/path/{foo}").get("get").getSummary());
	}
	
	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{summary:'a-summary'}}}"))
	public static class MB06 {		
		@RestMethod(name=GET,path="/path/{foo}",summary="d-summary")
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mb06_operation_summary_RestMethod() throws Exception {
		assertEquals("a-summary", getSwagger(new MB06()).getPaths().get("/path/{foo}").get("get").getSummary());
		assertEquals("a-summary", getSwaggerWithFile(new MB06()).getPaths().get("/path/{foo}").get("get").getSummary());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{}}}"))
	public static class MB07 {		
		@RestMethod(name=GET,path="/path/{foo}",summary="d-summary")
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mb07_operation_summary_RestMethod() throws Exception {
		assertEquals("d-summary", getSwagger(new MB07()).getPaths().get("/path/{foo}").get("get").getSummary());
		assertEquals("d-summary", getSwaggerWithFile(new MB07()).getPaths().get("/path/{foo}").get("get").getSummary());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/description
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class MC01 {
		
		@RestMethod(name=GET,path="/path/{foo}")
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void mc01_operation_description_default() throws Exception {
		assertEquals(null, getSwagger(new MC01()).getPaths().get("/path/{foo}").get("get").getDescription());
		assertEquals("s-description", getSwaggerWithFile(new MC01()).getPaths().get("/path/{foo}").get("get").getDescription());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{description:'a-description'}}}"))
	public static class MC02 {		
		@RestMethod(name=GET,path="/path/{foo}")
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void mc02_operation_description_swaggerOnClass() throws Exception {
		assertEquals("a-description", getSwagger(new MC02()).getPaths().get("/path/{foo}").get("get").getDescription());
		assertEquals("a-description", getSwaggerWithFile(new MC02()).getPaths().get("/path/{foo}").get("get").getDescription());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{description:'a-description'}}}"))
	public static class MC03 {		
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger("description:'b-description'"))
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void mc03_operation_description_swaggerOnMethod() throws Exception {
		assertEquals("b-description", getSwagger(new MC03()).getPaths().get("/path/{foo}").get("get").getDescription());
		assertEquals("b-description", getSwaggerWithFile(new MC03()).getPaths().get("/path/{foo}").get("get").getDescription());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{description:'a-description'}}}"))
	public static class MC04 {		
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(description="c-description"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mc04_operation_description_swaggerOnAnnotation() throws Exception {
		assertEquals("c-description", getSwagger(new MC04()).getPaths().get("/path/{foo}").get("get").getDescription());
		assertEquals("c-description", getSwaggerWithFile(new MC04()).getPaths().get("/path/{foo}").get("get").getDescription());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{description:'a-description'}}}"))
	public static class MC05 {		
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(description="$L{foo}"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mc05_operation_description_swaggerOnAnnotation_localized() throws Exception {
		assertEquals("l-foo", getSwagger(new MC05()).getPaths().get("/path/{foo}").get("get").getDescription());
		assertEquals("l-foo", getSwaggerWithFile(new MC05()).getPaths().get("/path/{foo}").get("get").getDescription());
	}
	
	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{description:'a-description'}}}"))
	public static class MC06 {		
		@RestMethod(name=GET,path="/path/{foo}",description="d-description")
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mc06_operation_description_RestMethod() throws Exception {
		assertEquals("a-description", getSwagger(new MC06()).getPaths().get("/path/{foo}").get("get").getDescription());
		assertEquals("a-description", getSwaggerWithFile(new MC06()).getPaths().get("/path/{foo}").get("get").getDescription());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{}}}"))
	public static class MC07 {		
		@RestMethod(name=GET,path="/path/{foo}",description="d-description")
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mc07_operation_description_RestMethod() throws Exception {
		assertEquals("d-description", getSwagger(new MC07()).getPaths().get("/path/{foo}").get("get").getDescription());
		assertEquals("d-description", getSwaggerWithFile(new MC07()).getPaths().get("/path/{foo}").get("get").getDescription());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/tags
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
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
	
	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/externalDocs
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
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
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(externalDocs="description:'c-description',url:'c-url'"))
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
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(externalDocs="{description:'d-description',url:'d-url'}"))
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
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(externalDocs="{description:'$L{foo}',url:'$L{foo}'}"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void me05_operation_externalDocs_swaggerOnAnnotation_localized() throws Exception {
		assertObjectEquals("{description:'l-foo',url:'l-foo'}", getSwagger(new ME05()).getPaths().get("/path/{foo}").get("get").getExternalDocs());
		assertObjectEquals("{description:'l-foo',url:'l-foo'}", getSwaggerWithFile(new ME05()).getPaths().get("/path/{foo}").get("get").getExternalDocs());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/consumes
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
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

	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/produces
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
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

	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/deprecated
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class MH01 {
		@RestMethod(name=GET,path="/path/{foo}")
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void mh01_operation_deprecated_default() throws Exception {
		assertEquals(null, getSwagger(new MH01()).getPaths().get("/path/{foo}").get("get").getDeprecated());
		assertObjectEquals("true", getSwaggerWithFile(new MH01()).getPaths().get("/path/{foo}").get("get").getDeprecated());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{deprecated:false}}}"))
	public static class MH02 {		
		@RestMethod(name=GET,path="/path/{foo}")
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void mh02_operation_deprecated_swaggerOnClass() throws Exception {
		assertObjectEquals("false", getSwagger(new MH02()).getPaths().get("/path/{foo}").get("get").getDeprecated());
		assertObjectEquals("false", getSwaggerWithFile(new MH02()).getPaths().get("/path/{foo}").get("get").getDeprecated());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{deprecated:false}}}"))
	public static class MH03 {		
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger("deprecated:false"))
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void mh03_operation_deprecated_swaggerOnMethod() throws Exception {
		assertObjectEquals("false", getSwagger(new MH03()).getPaths().get("/path/{foo}").get("get").getDeprecated());
		assertObjectEquals("false", getSwaggerWithFile(new MH03()).getPaths().get("/path/{foo}").get("get").getDeprecated());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{deprecated:false}}}"))
	public static class MH04 {		
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(deprecated="false"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mh04_operation_deprecated_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("false", getSwagger(new MH04()).getPaths().get("/path/{foo}").get("get").getDeprecated());
		assertObjectEquals("false", getSwaggerWithFile(new MH04()).getPaths().get("/path/{foo}").get("get").getDeprecated());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{deprecated:false}}}"))
	public static class MH05 {		
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(deprecated="$L{false}"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mh05_operation_deprecated_swaggerOnAnnotation_localized() throws Exception {
		assertObjectEquals("false", getSwagger(new MH05()).getPaths().get("/path/{foo}").get("get").getDeprecated());
		assertObjectEquals("false", getSwaggerWithFile(new MH05()).getPaths().get("/path/{foo}").get("get").getDeprecated());
	}

	@RestResource()
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
	
	@RestResource()
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

	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/schemes
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class MI01 {
		@RestMethod(name=GET,path="/path/{foo}")
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void mi01_operation_schemes_default() throws Exception {
		assertEquals(null, getSwagger(new MI01()).getPaths().get("/path/{foo}").get("get").getSchemes());
		assertObjectEquals("['s-scheme']", getSwaggerWithFile(new MI01()).getPaths().get("/path/{foo}").get("get").getSchemes());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{schemes:['a-scheme']}}}"))
	public static class MI02 {		
		@RestMethod(name=GET,path="/path/{foo}")
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void mi02_operation_schemes_swaggerOnClass() throws Exception {
		assertObjectEquals("['a-scheme']", getSwagger(new MI02()).getPaths().get("/path/{foo}").get("get").getSchemes());
		assertObjectEquals("['a-scheme']", getSwaggerWithFile(new MI02()).getPaths().get("/path/{foo}").get("get").getSchemes());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{schemes:['a-scheme']}}}"))
	public static class MI03 {		
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger("schemes:['b-scheme']"))
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void mi03_operation_schemes_swaggerOnMethod() throws Exception {
		assertObjectEquals("['b-scheme']", getSwagger(new MI03()).getPaths().get("/path/{foo}").get("get").getSchemes());
		assertObjectEquals("['b-scheme']", getSwaggerWithFile(new MI03()).getPaths().get("/path/{foo}").get("get").getSchemes());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{schemes:['a-scheme']}}}"))
	public static class MI04a {		
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(schemes="['c-scheme-1','c-scheme-2']"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mi04a_operation_schemes_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("['c-scheme-1','c-scheme-2']", getSwagger(new MI04a()).getPaths().get("/path/{foo}").get("get").getSchemes());
		assertObjectEquals("['c-scheme-1','c-scheme-2']", getSwaggerWithFile(new MI04a()).getPaths().get("/path/{foo}").get("get").getSchemes());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{schemes:['a-scheme']}}}"))
	public static class MI04b {		
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(schemes="d-scheme-1, d-scheme-2"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mi04b_operation_schemes_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("['d-scheme-1','d-scheme-2']", getSwagger(new MI04b()).getPaths().get("/path/{foo}").get("get").getSchemes());
		assertObjectEquals("['d-scheme-1','d-scheme-2']", getSwaggerWithFile(new MI04b()).getPaths().get("/path/{foo}").get("get").getSchemes());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}':{get:{schemes:['a-scheme']}}}"))
	public static class MI05 {		
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(schemes="$L{foo}"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void mi05_operation_schemes_swaggerOnAnnotation_localized() throws Exception {
		assertObjectEquals("['l-foo']", getSwagger(new MI05()).getPaths().get("/path/{foo}").get("get").getSchemes());
		assertObjectEquals("['l-foo']", getSwaggerWithFile(new MI05()).getPaths().get("/path/{foo}").get("get").getSchemes());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/parameters/query/type
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class NA01 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void na01_query_type_default() throws Exception {
		assertEquals(null, getSwagger(new NA01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getType());
		assertEquals("string", getSwaggerWithFile(new NA01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getType());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',type:'int32'}]}}}"))
	public static class NA02 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void na02_query_type_swaggerOnClass() throws Exception {
		assertEquals("int32", getSwagger(new NA02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getType());
		assertEquals("int32", getSwaggerWithFile(new NA02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getType());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',type:'int32'}]}}}"))
	public static class NA03 {		
		@RestMethod(name=GET,path="/path/{foo}/query",swagger=@MethodSwagger("parameters:[{'in':'query',name:'foo',type:'int64'}]"))
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void na03_query_type_swaggerOnMethod() throws Exception {
		assertEquals("int64", getSwagger(new NA03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getType());
		assertEquals("int64", getSwaggerWithFile(new NA03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getType());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',type:'int32'}]}}}"))
	public static class NA04 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",type="boolean") Foo foo) {
			return null;
		}
	}

	@Test
	public void na04_query_type_swaggerOnAnnotation() throws Exception {
		assertEquals("boolean", getSwagger(new NA04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getType());
		assertEquals("boolean", getSwaggerWithFile(new NA04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getType());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',type:'int32'}]}}}"))
	public static class NA05 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",type="$L{foo}") Foo foo) {
			return null;
		}
	}

	@Test
	public void na05_query_type_swaggerOnAnnotation_localized() throws Exception {
		assertEquals("l-foo", getSwagger(new NA05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getType());
		assertEquals("l-foo", getSwaggerWithFile(new NA05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getType());
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/parameters/query/type
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class NB01 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void nb01_query_description_default() throws Exception {
		assertEquals(null, getSwagger(new NB01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getDescription());
		assertEquals("s-description", getSwaggerWithFile(new NB01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getDescription());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',description:'a-description'}]}}}"))
	public static class NB02 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void nb02_query_description_swaggerOnClass() throws Exception {
		assertEquals("a-description", getSwagger(new NB02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getDescription());
		assertEquals("a-description", getSwaggerWithFile(new NB02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getDescription());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',description:'a-description'}]}}}"))
	public static class NB03 {		
		@RestMethod(name=GET,path="/path/{foo}/query",swagger=@MethodSwagger("parameters:[{'in':'query',name:'foo',description:'b-description'}]"))
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void nb03_query_description_swaggerOnMethod() throws Exception {
		assertEquals("b-description", getSwagger(new NB03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getDescription());
		assertEquals("b-description", getSwaggerWithFile(new NB03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getDescription());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',description:'a-description'}]}}}"))
	public static class NB04 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",description="c-description") Foo foo) {
			return null;
		}
	}

	@Test
	public void nb04_query_description_swaggerOnAnnotation() throws Exception {
		assertEquals("c-description", getSwagger(new NB04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getDescription());
		assertEquals("c-description", getSwaggerWithFile(new NB04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getDescription());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',description:'a-description'}]}}}"))
	public static class NB05 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",description="$L{foo}") Foo foo) {
			return null;
		}
	}

	@Test
	public void nb05_query_description_swaggerOnAnnotation_localized() throws Exception {
		assertEquals("l-foo", getSwagger(new NB05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getDescription());
		assertEquals("l-foo", getSwaggerWithFile(new NB05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getDescription());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/parameters/query/required
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class NC01 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void nc01_query_required_default() throws Exception {
		assertEquals(null, getSwagger(new NC01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getRequired());
		assertObjectEquals("true", getSwaggerWithFile(new NC01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getRequired());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',required:false}]}}}"))
	public static class NC02 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void nc02_query_required_swaggerOnClass() throws Exception {
		assertObjectEquals("false", getSwagger(new NC02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getRequired());
		assertObjectEquals("false", getSwaggerWithFile(new NC02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getRequired());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',required:false}]}}}"))
	public static class NC03 {		
		@RestMethod(name=GET,path="/path/{foo}/query",swagger=@MethodSwagger("parameters:[{'in':'query',name:'foo',required:'true'}]"))
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void nc03_query_required_swaggerOnMethod() throws Exception {
		assertObjectEquals("true", getSwagger(new NC03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getRequired());
		assertObjectEquals("true", getSwaggerWithFile(new NC03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getRequired());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',required:false}]}}}"))
	public static class NC04 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",required="true") Foo foo) {
			return null;
		}
	}

	@Test
	public void nc04_query_required_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("true", getSwagger(new NC04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getRequired());
		assertObjectEquals("true", getSwaggerWithFile(new NC04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getRequired());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',required:false}]}}}"))
	public static class NC05 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",required="$L{false}") Foo foo) {
			return null;
		}
	}

	@Test
	public void nc05_query_required_swaggerOnAnnotation_localized() throws Exception {
		assertObjectEquals("false", getSwagger(new NC05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getRequired());
		assertObjectEquals("false", getSwaggerWithFile(new NC05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getRequired());
	}

	@RestResource()
	public static class NC06 {		
		@RestMethod(name=GET,path="/path/{foo}/path")
		public Foo doFoo(@Path(name="foo") Foo foo) {
			return null;
		}
	}

	@Test
	public void nc06_path_required_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("true", getSwagger(new NC06()).getPaths().get("/path/{foo}/path").get("get").getParameter("path", "foo").getRequired());
		assertObjectEquals("true", getSwaggerWithFile(new NC06()).getPaths().get("/path/{foo}/path").get("get").getParameter("path", "foo").getRequired());
	}
	
	@RestResource()
	public static class NC07 {		
		@RestMethod(name=GET,path="/path/{foo}/body")
		public Foo doFoo(@Body() Foo foo) {
			return null;
		}
	}

	@Test
	public void nc07_body_required_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("true", getSwagger(new NC07()).getPaths().get("/path/{foo}/body").get("get").getParameter("body", null).getRequired());
		assertObjectEquals("true", getSwaggerWithFile(new NC07()).getPaths().get("/path/{foo}/body").get("get").getParameter("body", null).getRequired());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/parameters/query/allowEmptyValue
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class ND01 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void nd01_query_allowEmptyValue_default() throws Exception {
		assertEquals(null, getSwagger(new ND01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getAllowEmptyValue());
		assertObjectEquals("true", getSwaggerWithFile(new ND01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getAllowEmptyValue());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',allowEmptyValue:false}]}}}"))
	public static class ND02 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void nd02_query_allowEmptyValue_swaggerOnClass() throws Exception {
		assertObjectEquals("false", getSwagger(new ND02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getAllowEmptyValue());
		assertObjectEquals("false", getSwaggerWithFile(new ND02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getAllowEmptyValue());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',allowEmptyValue:false}]}}}"))
	public static class ND03 {		
		@RestMethod(name=GET,path="/path/{foo}/query",swagger=@MethodSwagger("parameters:[{'in':'query',name:'foo',allowEmptyValue:'true'}]"))
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void nd03_query_allowEmptyValue_swaggerOnMethod() throws Exception {
		assertObjectEquals("true", getSwagger(new ND03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getAllowEmptyValue());
		assertObjectEquals("true", getSwaggerWithFile(new ND03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getAllowEmptyValue());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',allowEmptyValue:false}]}}}"))
	public static class ND04 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",allowEmptyValue="true") Foo foo) {
			return null;
		}
	}

	@Test
	public void nd04_query_allowEmptyValue_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("true", getSwagger(new ND04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getAllowEmptyValue());
		assertObjectEquals("true", getSwaggerWithFile(new ND04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getAllowEmptyValue());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',allowEmptyValue:false}]}}}"))
	public static class ND05 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",allowEmptyValue="$L{false}") Foo foo) {
			return null;
		}
	}

	@Test
	public void nd05_query_allowEmptyValue_swaggerOnAnnotation_localized() throws Exception {
		assertObjectEquals("false", getSwagger(new ND05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getAllowEmptyValue());
		assertObjectEquals("false", getSwaggerWithFile(new ND05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getAllowEmptyValue());
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/parameters/query/exclusiveMaximum
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class NE01 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void ne01_query_exclusiveMaximum_default() throws Exception {
		assertEquals(null, getSwagger(new NE01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExclusiveMaximum());
		assertObjectEquals("true", getSwaggerWithFile(new NE01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExclusiveMaximum());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',exclusiveMaximum:false}]}}}"))
	public static class NE02 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void ne02_query_exclusiveMaximum_swaggerOnClass() throws Exception {
		assertObjectEquals("false", getSwagger(new NE02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExclusiveMaximum());
		assertObjectEquals("false", getSwaggerWithFile(new NE02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExclusiveMaximum());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',exclusiveMaximum:false}]}}}"))
	public static class NE03 {		
		@RestMethod(name=GET,path="/path/{foo}/query",swagger=@MethodSwagger("parameters:[{'in':'query',name:'foo',exclusiveMaximum:'true'}]"))
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void ne03_query_exclusiveMaximum_swaggerOnMethod() throws Exception {
		assertObjectEquals("true", getSwagger(new NE03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExclusiveMaximum());
		assertObjectEquals("true", getSwaggerWithFile(new NE03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExclusiveMaximum());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',exclusiveMaximum:false}]}}}"))
	public static class NE04 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",exclusiveMaximum="true") Foo foo) {
			return null;
		}
	}

	@Test
	public void ne04_query_exclusiveMaximum_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("true", getSwagger(new NE04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExclusiveMaximum());
		assertObjectEquals("true", getSwaggerWithFile(new NE04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExclusiveMaximum());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',exclusiveMaximum:false}]}}}"))
	public static class NE05 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",exclusiveMaximum="$L{false}") Foo foo) {
			return null;
		}
	}

	@Test
	public void ne05_query_exclusiveMaximum_swaggerOnAnnotation_localized() throws Exception {
		assertObjectEquals("false", getSwagger(new NE05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExclusiveMaximum());
		assertObjectEquals("false", getSwaggerWithFile(new NE05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExclusiveMaximum());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/parameters/query/exclusiveMinimum
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class NF01 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void nf01_query_exclusiveMinimum_default() throws Exception {
		assertEquals(null, getSwagger(new NF01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExclusiveMinimum());
		assertObjectEquals("true", getSwaggerWithFile(new NF01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExclusiveMinimum());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',exclusiveMinimum:false}]}}}"))
	public static class NF02 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void nf02_query_exclusiveMinimum_swaggerOnClass() throws Exception {
		assertObjectEquals("false", getSwagger(new NF02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExclusiveMinimum());
		assertObjectEquals("false", getSwaggerWithFile(new NF02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExclusiveMinimum());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',exclusiveMinimum:false}]}}}"))
	public static class NF03 {		
		@RestMethod(name=GET,path="/path/{foo}/query",swagger=@MethodSwagger("parameters:[{'in':'query',name:'foo',exclusiveMinimum:'true'}]"))
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void nf03_query_exclusiveMinimum_swaggerOnMethod() throws Exception {
		assertObjectEquals("true", getSwagger(new NF03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExclusiveMinimum());
		assertObjectEquals("true", getSwaggerWithFile(new NF03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExclusiveMinimum());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',exclusiveMinimum:false}]}}}"))
	public static class NF04 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",exclusiveMinimum="true") Foo foo) {
			return null;
		}
	}

	@Test
	public void nf04_query_exclusiveMinimum_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("true", getSwagger(new NF04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExclusiveMinimum());
		assertObjectEquals("true", getSwaggerWithFile(new NF04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExclusiveMinimum());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',exclusiveMinimum:false}]}}}"))
	public static class NF05 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",exclusiveMinimum="$L{false}") Foo foo) {
			return null;
		}
	}

	@Test
	public void nf05_query_exclusiveMinimum_swaggerOnAnnotation_localized() throws Exception {
		assertObjectEquals("false", getSwagger(new NF05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExclusiveMinimum());
		assertObjectEquals("false", getSwaggerWithFile(new NF05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExclusiveMinimum());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/parameters/query/uniqueItems
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class NG01 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void ng01_query_uniqueItems_default() throws Exception {
		assertEquals(null, getSwagger(new NG01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getUniqueItems());
		assertObjectEquals("true", getSwaggerWithFile(new NG01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getUniqueItems());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',uniqueItems:false}]}}}"))
	public static class NG02 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void ng02_query_uniqueItems_swaggerOnClass() throws Exception {
		assertObjectEquals("false", getSwagger(new NG02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getUniqueItems());
		assertObjectEquals("false", getSwaggerWithFile(new NG02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getUniqueItems());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',uniqueItems:false}]}}}"))
	public static class NG03 {		
		@RestMethod(name=GET,path="/path/{foo}/query",swagger=@MethodSwagger("parameters:[{'in':'query',name:'foo',uniqueItems:'true'}]"))
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void ng03_query_uniqueItems_swaggerOnMethod() throws Exception {
		assertObjectEquals("true", getSwagger(new NG03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getUniqueItems());
		assertObjectEquals("true", getSwaggerWithFile(new NG03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getUniqueItems());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',uniqueItems:false}]}}}"))
	public static class NG04 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",uniqueItems="true") Foo foo) {
			return null;
		}
	}

	@Test
	public void ng04_query_uniqueItems_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("true", getSwagger(new NG04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getUniqueItems());
		assertObjectEquals("true", getSwaggerWithFile(new NG04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getUniqueItems());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',uniqueItems:false}]}}}"))
	public static class NG05 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",uniqueItems="$L{false}") Foo foo) {
			return null;
		}
	}

	@Test
	public void ng05_query_uniqueItems_swaggerOnAnnotation_localized() throws Exception {
		assertObjectEquals("false", getSwagger(new NG05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getUniqueItems());
		assertObjectEquals("false", getSwaggerWithFile(new NG05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getUniqueItems());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/parameters/query/format
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class NH01 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void nh01_query_format_default() throws Exception {
		assertEquals(null, getSwagger(new NH01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getFormat());
		assertEquals("s-format", getSwaggerWithFile(new NH01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getFormat());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',format:'a-format'}]}}}"))
	public static class NH02 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void nh02_query_format_swaggerOnClass() throws Exception {
		assertEquals("a-format", getSwagger(new NH02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getFormat());
		assertEquals("a-format", getSwaggerWithFile(new NH02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getFormat());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',format:'a-format'}]}}}"))
	public static class NH03 {		
		@RestMethod(name=GET,path="/path/{foo}/query",swagger=@MethodSwagger("parameters:[{'in':'query',name:'foo',format:'b-format'}]"))
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void nh03_query_format_swaggerOnMethod() throws Exception {
		assertEquals("b-format", getSwagger(new NH03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getFormat());
		assertEquals("b-format", getSwaggerWithFile(new NH03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getFormat());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',format:'a-format'}]}}}"))
	public static class NH04 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",format="c-format") Foo foo) {
			return null;
		}
	}

	@Test
	public void nh04_query_format_swaggerOnAnnotation() throws Exception {
		assertEquals("c-format", getSwagger(new NH04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getFormat());
		assertEquals("c-format", getSwaggerWithFile(new NH04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getFormat());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',format:'a-format'}]}}}"))
	public static class NH05 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",format="$L{foo}") Foo foo) {
			return null;
		}
	}

	@Test
	public void nh05_query_format_swaggerOnAnnotation_localized() throws Exception {
		assertEquals("l-foo", getSwagger(new NH05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getFormat());
		assertEquals("l-foo", getSwaggerWithFile(new NH05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getFormat());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/parameters/query/collectionFormat
	//-----------------------------------------------------------------------------------------------------------------
	
	@RestResource()
	public static class NI01 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void ni01_query_collectionFormat_default() throws Exception {
		assertEquals(null, getSwagger(new NI01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getCollectionFormat());
		assertEquals("s-collectionFormat", getSwaggerWithFile(new NI01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getCollectionFormat());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',collectionFormat:'a-collectionFormat'}]}}}"))
	public static class NI02 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void ni02_query_collectionFormat_swaggerOnClass() throws Exception {
		assertEquals("a-collectionFormat", getSwagger(new NI02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getCollectionFormat());
		assertEquals("a-collectionFormat", getSwaggerWithFile(new NI02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getCollectionFormat());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',collectionFormat:'a-collectionFormat'}]}}}"))
	public static class NI03 {		
		@RestMethod(name=GET,path="/path/{foo}/query",swagger=@MethodSwagger("parameters:[{'in':'query',name:'foo',collectionFormat:'b-collectionFormat'}]"))
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void ni03_query_collectionFormat_swaggerOnMethod() throws Exception {
		assertEquals("b-collectionFormat", getSwagger(new NI03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getCollectionFormat());
		assertEquals("b-collectionFormat", getSwaggerWithFile(new NI03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getCollectionFormat());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',collectionFormat:'a-collectionFormat'}]}}}"))
	public static class NI04 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",collectionFormat="c-collectionFormat") Foo foo) {
			return null;
		}
	}

	@Test
	public void ni04_query_collectionFormat_swaggerOnAnnotation() throws Exception {
		assertEquals("c-collectionFormat", getSwagger(new NI04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getCollectionFormat());
		assertEquals("c-collectionFormat", getSwaggerWithFile(new NI04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getCollectionFormat());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',collectionFormat:'a-collectionFormat'}]}}}"))
	public static class NI05 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",collectionFormat="$L{foo}") Foo foo) {
			return null;
		}
	}

	@Test
	public void ni05_query_collectionFormat_swaggerOnAnnotation_localized() throws Exception {
		assertEquals("l-foo", getSwagger(new NI05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getCollectionFormat());
		assertEquals("l-foo", getSwaggerWithFile(new NI05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getCollectionFormat());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/parameters/query/pattern
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class NJ01 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void nj01_query_pattern_default() throws Exception {
		assertEquals(null, getSwagger(new NJ01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getPattern());
		assertEquals("s-pattern", getSwaggerWithFile(new NJ01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getPattern());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',pattern:'a-pattern'}]}}}"))
	public static class NJ02 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void nj02_query_pattern_swaggerOnClass() throws Exception {
		assertEquals("a-pattern", getSwagger(new NJ02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getPattern());
		assertEquals("a-pattern", getSwaggerWithFile(new NJ02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getPattern());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',pattern:'a-pattern'}]}}}"))
	public static class NJ03 {		
		@RestMethod(name=GET,path="/path/{foo}/query",swagger=@MethodSwagger("parameters:[{'in':'query',name:'foo',pattern:'b-pattern'}]"))
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void nj03_query_pattern_swaggerOnMethod() throws Exception {
		assertEquals("b-pattern", getSwagger(new NJ03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getPattern());
		assertEquals("b-pattern", getSwaggerWithFile(new NJ03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getPattern());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',pattern:'a-pattern'}]}}}"))
	public static class NJ04 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",pattern="c-pattern") Foo foo) {
			return null;
		}
	}

	@Test
	public void nj04_query_pattern_swaggerOnAnnotation() throws Exception {
		assertEquals("c-pattern", getSwagger(new NJ04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getPattern());
		assertEquals("c-pattern", getSwaggerWithFile(new NJ04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getPattern());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',pattern:'a-pattern'}]}}}"))
	public static class NJ05 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",pattern="$L{foo}") Foo foo) {
			return null;
		}
	}

	@Test
	public void nj05_query_pattern_swaggerOnAnnotation_localized() throws Exception {
		assertEquals("l-foo", getSwagger(new NJ05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getPattern());
		assertEquals("l-foo", getSwaggerWithFile(new NJ05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getPattern());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/parameters/query/maximum
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class NK01 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void nk01_query_maximum_default() throws Exception {
		assertEquals(null, getSwagger(new NK01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaximum());
		assertObjectEquals("1.0", getSwaggerWithFile(new NK01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaximum());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',maximum:2.0}]}}}"))
	public static class NK02 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void nk02_query_maximum_swaggerOnClass() throws Exception {
		assertObjectEquals("2.0", getSwagger(new NK02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaximum());
		assertObjectEquals("2.0", getSwaggerWithFile(new NK02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaximum());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',maximum:2.0}]}}}"))
	public static class NK03 {		
		@RestMethod(name=GET,path="/path/{foo}/query",swagger=@MethodSwagger("parameters:[{'in':'query',name:'foo',maximum:3.0}]"))
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void nk03_query_maximum_swaggerOnMethod() throws Exception {
		assertObjectEquals("3.0", getSwagger(new NK03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaximum());
		assertObjectEquals("3.0", getSwaggerWithFile(new NK03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaximum());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',maximum:2.0}]}}}"))
	public static class NK04 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",maximum="4.0") Foo foo) {
			return null;
		}
	}

	@Test
	public void nk04_query_maximum_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("4.0", getSwagger(new NK04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaximum());
		assertObjectEquals("4.0", getSwaggerWithFile(new NK04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaximum());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',maximum:2.0}]}}}"))
	public static class NK05 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",maximum="$L{50}") Foo foo) {
			return null;
		}
	}

	@Test
	public void nk05_query_maximum_swaggerOnAnnotation_localized() throws Exception {
		assertObjectEquals("5.0", getSwagger(new NK05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaximum());
		assertObjectEquals("5.0", getSwaggerWithFile(new NK05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaximum());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/parameters/query/minimum
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class NL01 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void nl01_query_minimum_default() throws Exception {
		assertEquals(null, getSwagger(new NL01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinimum());
		assertObjectEquals("1.0", getSwaggerWithFile(new NL01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinimum());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',minimum:2.0}]}}}"))
	public static class NL02 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void nl02_query_minimum_swaggerOnClass() throws Exception {
		assertObjectEquals("2.0", getSwagger(new NL02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinimum());
		assertObjectEquals("2.0", getSwaggerWithFile(new NL02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinimum());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',minimum:2.0}]}}}"))
	public static class NL03 {		
		@RestMethod(name=GET,path="/path/{foo}/query",swagger=@MethodSwagger("parameters:[{'in':'query',name:'foo',minimum:3.0}]"))
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void nl03_query_minimum_swaggerOnMethod() throws Exception {
		assertObjectEquals("3.0", getSwagger(new NL03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinimum());
		assertObjectEquals("3.0", getSwaggerWithFile(new NL03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinimum());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',minimum:2.0}]}}}"))
	public static class NL04 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",minimum="4.0") Foo foo) {
			return null;
		}
	}

	@Test
	public void nl04_query_minimum_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("4.0", getSwagger(new NL04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinimum());
		assertObjectEquals("4.0", getSwaggerWithFile(new NL04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinimum());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',minimum:2.0}]}}}"))
	public static class NL05 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",minimum="$L{50}") Foo foo) {
			return null;
		}
	}

	@Test
	public void nl05_query_minimum_swaggerOnAnnotation_localized() throws Exception {
		assertObjectEquals("5.0", getSwagger(new NL05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinimum());
		assertObjectEquals("5.0", getSwaggerWithFile(new NL05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinimum());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/parameters/query/multipleOf
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class NM01 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void nm01_query_multipleOf_default() throws Exception {
		assertEquals(null, getSwagger(new NM01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMultipleOf());
		assertObjectEquals("1.0", getSwaggerWithFile(new NM01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMultipleOf());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',multipleOf:2.0}]}}}"))
	public static class NM02 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void nm02_query_multipleOf_swaggerOnClass() throws Exception {
		assertObjectEquals("2.0", getSwagger(new NM02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMultipleOf());
		assertObjectEquals("2.0", getSwaggerWithFile(new NM02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMultipleOf());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',multipleOf:2.0}]}}}"))
	public static class NM03 {		
		@RestMethod(name=GET,path="/path/{foo}/query",swagger=@MethodSwagger("parameters:[{'in':'query',name:'foo',multipleOf:3.0}]"))
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void nm03_query_multipleOf_swaggerOnMethod() throws Exception {
		assertObjectEquals("3.0", getSwagger(new NM03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMultipleOf());
		assertObjectEquals("3.0", getSwaggerWithFile(new NM03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMultipleOf());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',multipleOf:2.0}]}}}"))
	public static class NM04 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",multipleOf="4.0") Foo foo) {
			return null;
		}
	}

	@Test
	public void nm04_query_multipleOf_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("4.0", getSwagger(new NM04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMultipleOf());
		assertObjectEquals("4.0", getSwaggerWithFile(new NM04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMultipleOf());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',multipleOf:2.0}]}}}"))
	public static class NM05 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",multipleOf="$L{50}") Foo foo) {
			return null;
		}
	}

	@Test
	public void nm05_query_multipleOf_swaggerOnAnnotation_localized() throws Exception {
		assertObjectEquals("5.0", getSwagger(new NM05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMultipleOf());
		assertObjectEquals("5.0", getSwaggerWithFile(new NM05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMultipleOf());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/parameters/query/maxLength
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class NN01 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void nn01_query_maxLength_default() throws Exception {
		assertEquals(null, getSwagger(new NN01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaxLength());
		assertObjectEquals("1", getSwaggerWithFile(new NN01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaxLength());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',maxLength:2}]}}}"))
	public static class NN02 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void nn02_query_maxLength_swaggerOnClass() throws Exception {
		assertObjectEquals("2", getSwagger(new NN02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaxLength());
		assertObjectEquals("2", getSwaggerWithFile(new NN02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaxLength());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',maxLength:2}]}}}"))
	public static class NN03 {		
		@RestMethod(name=GET,path="/path/{foo}/query",swagger=@MethodSwagger("parameters:[{'in':'query',name:'foo',maxLength:3}]"))
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void nn03_query_maxLength_swaggerOnMethod() throws Exception {
		assertObjectEquals("3", getSwagger(new NN03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaxLength());
		assertObjectEquals("3", getSwaggerWithFile(new NN03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaxLength());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',maxLength:2}]}}}"))
	public static class NN04 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",maxLength="4") Foo foo) {
			return null;
		}
	}

	@Test
	public void nn04_query_maxLength_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("4", getSwagger(new NN04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaxLength());
		assertObjectEquals("4", getSwaggerWithFile(new NN04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaxLength());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',maxLength:2}]}}}"))
	public static class NN05 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",maxLength="$L{5}") Foo foo) {
			return null;
		}
	}

	@Test
	public void nn05_query_maxLength_swaggerOnAnnotation_localized() throws Exception {
		assertObjectEquals("5", getSwagger(new NN05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaxLength());
		assertObjectEquals("5", getSwaggerWithFile(new NN05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaxLength());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/parameters/query/minLength
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class NO01 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void no01_query_minLength_default() throws Exception {
		assertEquals(null, getSwagger(new NO01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinLength());
		assertObjectEquals("1", getSwaggerWithFile(new NO01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinLength());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',minLength:2}]}}}"))
	public static class NO02 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void no02_query_minLength_swaggerOnClass() throws Exception {
		assertObjectEquals("2", getSwagger(new NO02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinLength());
		assertObjectEquals("2", getSwaggerWithFile(new NO02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinLength());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',minLength:2}]}}}"))
	public static class NO03 {		
		@RestMethod(name=GET,path="/path/{foo}/query",swagger=@MethodSwagger("parameters:[{'in':'query',name:'foo',minLength:3}]"))
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void no03_query_minLength_swaggerOnMethod() throws Exception {
		assertObjectEquals("3", getSwagger(new NO03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinLength());
		assertObjectEquals("3", getSwaggerWithFile(new NO03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinLength());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',minLength:2}]}}}"))
	public static class NO04 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",minLength="4") Foo foo) {
			return null;
		}
	}

	@Test
	public void no04_query_minLength_swaggerOnAnootation() throws Exception {
		assertObjectEquals("4", getSwagger(new NO04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinLength());
		assertObjectEquals("4", getSwaggerWithFile(new NO04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinLength());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',minLength:2}]}}}"))
	public static class NO05 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",minLength="$L{5}") Foo foo) {
			return null;
		}
	}

	@Test
	public void no05_query_minLength_swaggerOnAnootation_localized() throws Exception {
		assertObjectEquals("5", getSwagger(new NO05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinLength());
		assertObjectEquals("5", getSwaggerWithFile(new NO05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinLength());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/parameters/query/maxItems
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class NP01 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void np01_query_maxItems_default() throws Exception {
		assertEquals(null, getSwagger(new NP01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaxItems());
		assertObjectEquals("1", getSwaggerWithFile(new NP01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaxItems());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',maxItems:2}]}}}"))
	public static class NP02 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void np02_query_maxItems_swaggerOnClass() throws Exception {
		assertObjectEquals("2", getSwagger(new NP02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaxItems());
		assertObjectEquals("2", getSwaggerWithFile(new NP02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaxItems());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',maxItems:2}]}}}"))
	public static class NP03 {		
		@RestMethod(name=GET,path="/path/{foo}/query",swagger=@MethodSwagger("parameters:[{'in':'query',name:'foo',maxItems:3}]"))
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void np03_query_maxItems_swaggerOnMethod() throws Exception {
		assertObjectEquals("3", getSwagger(new NP03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaxItems());
		assertObjectEquals("3", getSwaggerWithFile(new NP03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaxItems());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',maxItems:2}]}}}"))
	public static class NP04 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",maxItems="4") Foo foo) {
			return null;
		}
	}

	@Test
	public void np04_query_maxItems_swaggerOnAnpotation() throws Exception {
		assertObjectEquals("4", getSwagger(new NP04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaxItems());
		assertObjectEquals("4", getSwaggerWithFile(new NP04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaxItems());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',maxItems:2}]}}}"))
	public static class NP05 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",maxItems="$L{5}") Foo foo) {
			return null;
		}
	}

	@Test
	public void np05_query_maxItems_swaggerOnAnpotation_localized() throws Exception {
		assertObjectEquals("5", getSwagger(new NP05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaxItems());
		assertObjectEquals("5", getSwaggerWithFile(new NP05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMaxItems());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/parameters/query/minItems
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class NQ01 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void nq01_query_minItems_default() throws Exception {
		assertEquals(null, getSwagger(new NQ01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinItems());
		assertObjectEquals("1", getSwaggerWithFile(new NQ01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinItems());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',minItems:2}]}}}"))
	public static class NQ02 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void nq02_query_minItems_swaggerOnClass() throws Exception {
		assertObjectEquals("2", getSwagger(new NQ02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinItems());
		assertObjectEquals("2", getSwaggerWithFile(new NQ02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinItems());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',minItems:2}]}}}"))
	public static class NQ03 {		
		@RestMethod(name=GET,path="/path/{foo}/query",swagger=@MethodSwagger("parameters:[{'in':'query',name:'foo',minItems:3}]"))
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void nq03_query_minItems_swaggerOnMethod() throws Exception {
		assertObjectEquals("3", getSwagger(new NQ03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinItems());
		assertObjectEquals("3", getSwaggerWithFile(new NQ03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinItems());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',minItems:2}]}}}"))
	public static class NQ04 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",minItems="4") Foo foo) {
			return null;
		}
	}

	@Test
	public void nq04_query_minItems_swaggerOnAnqotation() throws Exception {
		assertObjectEquals("4", getSwagger(new NQ04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinItems());
		assertObjectEquals("4", getSwaggerWithFile(new NQ04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinItems());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',minItems:2}]}}}"))
	public static class NQ05 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",minItems="$L{5}") Foo foo) {
			return null;
		}
	}

	@Test
	public void nq05_query_minItems_swaggerOnAnqotation_localized() throws Exception {
		assertObjectEquals("5", getSwagger(new NQ05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinItems());
		assertObjectEquals("5", getSwaggerWithFile(new NQ05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getMinItems());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/parameters/query/example
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class NR01 {
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void nr01_query_example_default() throws Exception {
		assertEquals(null, getSwagger(new NR01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExample());
		assertObjectEquals("{id:1}", getSwaggerWithFile(new NR01()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExample());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',x-example:{id:2}}]}}}"))
	public static class NR02 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query("foo") Foo foo) {
			return null;
		}
	}
	
	@Test
	public void nr02_query_example_swaggerOnClass() throws Exception {
		assertObjectEquals("{id:2}", getSwagger(new NR02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExample());
		assertObjectEquals("{id:2}", getSwaggerWithFile(new NR02()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExample());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',x-example:{id:2}}]}}}"))
	public static class NR03 {		
		@RestMethod(name=GET,path="/path/{foo}/query",swagger=@MethodSwagger("parameters:[{'in':'query',name:'foo',x-example:{id:3}}]"))
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void nr03_query_example_swaggerOnMethod() throws Exception {
		assertObjectEquals("{id:3}", getSwagger(new NR03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExample());
		assertObjectEquals("{id:3}", getSwaggerWithFile(new NR03()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExample());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',x-example:{id:2}}]}}}"))
	public static class NR04 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",example="{id:4}") Foo foo) {
			return null;
		}
	}

	@Test
	public void nr04_query_example_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("{id:4}", getSwagger(new NR04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExample());
		assertObjectEquals("{id:4}", getSwaggerWithFile(new NR04()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExample());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/query':{get:{parameters:[{'in':'query',name:'foo',x-example:{id:2}}]}}}"))
	public static class NR05 {		
		@RestMethod(name=GET,path="/path/{foo}/query")
		public Foo doFoo(@Query(name="foo",example="{id:$L{5}}") Foo foo) {
			return null;
		}
	}

	@Test
	public void nr05_query_example_swaggerOnAnnotation_localized() throws Exception {
		assertObjectEquals("{id:5}", getSwagger(new NR05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExample());
		assertObjectEquals("{id:5}", getSwaggerWithFile(new NR05()).getPaths().get("/path/{foo}/query").get("get").getParameter("query", "foo").getExample());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/parameters/body/examples
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
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
	public void ns03_body_examples_swaggerOnMethns() throws Exception {
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

	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/parameters/query/schema
	//-----------------------------------------------------------------------------------------------------------------

	// TODO
	
	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/responses/<response>/description
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class OA01 {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public Foo doFoo(@Response(code=100) Value<Foo> foo) {
			return null;
		}
	}
	
	@Test
	public void oa01_responses_100_description_default() throws Exception {
		assertEquals("Continue", getSwagger(new OA01()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
		assertEquals("s-100-description", getSwaggerWithFile(new OA01()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{description:'a-100-description'}}}}}"))
	public static class OA02 {		
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public Foo doFoo(@ResponseStatus Value<Integer> foo) {
			return null;
		}
	}
	
	@Test
	public void oa02_response_100_description_swaggerOnClass() throws Exception {
		assertEquals("a-100-description", getSwagger(new OA02()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
		assertEquals("a-100-description", getSwaggerWithFile(new OA02()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{description:'a-100-description'}}}}}"))
	public static class OA03 {		
		@RestMethod(name=GET,path="/path/{foo}/responses/100",swagger=@MethodSwagger("responses:{100:{description:'b-100-description'}}"))
		public Foo doFoo(@ResponseStatus Value<Integer> foo) {
			return null;
		}
	}
	
	@Test
	public void oa03_response_100_description_swaggerOnMethod() throws Exception {
		assertEquals("b-100-description", getSwagger(new OA03()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
		assertEquals("b-100-description", getSwaggerWithFile(new OA03()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{description:'a-100-description'}}}}}"))
	public static class OA04 {		
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public Foo doFoo(@Response(code=100,description="c-100-description") Value<Foo> foo) {
			return null;
		}
	}

	@Test
	public void oa04_response_100_description_swaggerOnAnnotation() throws Exception {
		assertEquals("c-100-description", getSwagger(new OA04()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
		assertEquals("c-100-description", getSwaggerWithFile(new OA04()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{description:'a-100-description'}}}}}"))
	public static class OA05 {		
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public Foo doFoo(@Response(code=100,description="$L{foo}") Value<Foo> foo) {
			return null;
		}
	}

	@Test
	public void oa05_response_100_description_swaggerOnAnnotation_localized() throws Exception {
		assertEquals("l-foo", getSwagger(new OA05()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
		assertEquals("l-foo", getSwaggerWithFile(new OA05()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getDescription());
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/responses/<response>/headers
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class OB01 {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public Foo doFoo(@Response(code=100) Value<Foo> foo) {
			return null;
		}
	}
	
	@Test
	public void ob01_responses_100_headers_default() throws Exception {
		assertEquals(null, getSwagger(new OB01()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
		assertObjectEquals("{'X-Foo':{description:'s-description',type:'integer',format:'int32'}}", getSwaggerWithFile(new OB01()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
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
	public static class OB04 {		
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public Foo doFoo(@Response(code=100,headers="'X-Foo':{description:'d-description',type:'integer',format:'int32'}") Value<Foo> foo) {
			return null;
		}
	}

	@Test
	public void ob04_response_100_headers_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("{'X-Foo':{description:'d-description',type:'integer',format:'int32'}}", getSwagger(new OB04()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
		assertObjectEquals("{'X-Foo':{description:'d-description',type:'integer',format:'int32'}}", getSwaggerWithFile(new OB04()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{headers:{'X-Foo':{description:'b-description',type:'integer',format:'int32'}}}}}}}"))
	public static class OB05 {		
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public Foo doFoo(@Response(code=100,headers="'X-Foo':{description:'$L{foo}',type:'integer',format:'int32'}") Value<Foo> foo) {
			return null;
		}
	}

	@Test
	public void ob05_response_100_headers_swaggerOnAnnotation_localized() throws Exception {
		assertObjectEquals("{'X-Foo':{description:'l-foo',type:'integer',format:'int32'}}", getSwagger(new OB05()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
		assertObjectEquals("{'X-Foo':{description:'l-foo',type:'integer',format:'int32'}}", getSwaggerWithFile(new OB05()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getHeaders());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/responses/<response>/example
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class OC01 {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public Foo doFoo(@Response(code=100) Value<Foo> foo) {
			return null;
		}
	}
	
	@Test
	public void oc01_responses_100_example_default() throws Exception {
		assertEquals(null, getSwagger(new OC01()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
		assertObjectEquals("{foo:'a'}", getSwaggerWithFile(new OC01()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{example:{foo:'b'}}}}}}"))
	public static class OC02 {		
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public Foo doFoo(@ResponseStatus Value<Integer> foo) {
			return null;
		}
	}
	
	@Test
	public void oc02_response_100_example_swaggerOnClass() throws Exception {
		assertObjectEquals("{foo:'b'}", getSwagger(new OC02()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
		assertObjectEquals("{foo:'b'}", getSwaggerWithFile(new OC02()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{example:{foo:'b'}}}}}}"))
	public static class OC03 {		
		@RestMethod(name=GET,path="/path/{foo}/responses/100",swagger=@MethodSwagger("responses:{100:{example:{foo:'c'}}}"))
		public Foo doFoo(@ResponseStatus Value<Integer> foo) {
			return null;
		}
	}
	
	@Test
	public void oc03_response_100_example_swaggerOnMethod() throws Exception {
		assertObjectEquals("{foo:'c'}", getSwagger(new OC03()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
		assertObjectEquals("{foo:'c'}", getSwaggerWithFile(new OC03()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{example:{foo:'b'}}}}}}"))
	public static class OC04 {		
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public Foo doFoo(@Response(code=100,example="{foo:'d'}") Value<Foo> foo) {
			return null;
		}
	}

	@Test
	public void oc04_response_100_example_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("{foo:'d'}", getSwagger(new OC04()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
		assertObjectEquals("{foo:'d'}", getSwaggerWithFile(new OC04()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{example:{foo:'b'}}}}}}"))
	public static class OC05 {		
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public Foo doFoo(@Response(code=100,example="{foo:'$L{foo}'}") Value<Foo> foo) {
			return null;
		}
	}

	@Test
	public void oc05_response_100_example_swaggerOnAnnotation_localized() throws Exception {
		assertObjectEquals("{foo:'l-foo'}", getSwagger(new OC05()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
		assertObjectEquals("{foo:'l-foo'}", getSwaggerWithFile(new OC05()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExample());
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/responses/<response>/examples
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class OD01 {
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public Foo doFoo(@Response(code=100) Value<Foo> foo) {
			return null;
		}
	}
	
	@Test
	public void od01_responses_100_examples_default() throws Exception {
		assertEquals(null, getSwagger(new OD01()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
		assertObjectEquals("{foo:'a'}", getSwaggerWithFile(new OD01()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{examples:{foo:{bar:'b'}}}}}}}"))
	public static class OD02 {		
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public Foo doFoo(@ResponseStatus Value<Integer> foo) {
			return null;
		}
	}
	
	@Test
	public void od02_response_100_examples_swaggerOnClass() throws Exception {
		assertObjectEquals("{foo:{bar:'b'}}", getSwagger(new OD02()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
		assertObjectEquals("{foo:{bar:'b'}}", getSwaggerWithFile(new OD02()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{examples:{foo:{bar:'b'}}}}}}}"))
	public static class OD03 {		
		@RestMethod(name=GET,path="/path/{foo}/responses/100",swagger=@MethodSwagger("responses:{100:{examples:{foo:{bar:'c'}}}}"))
		public Foo doFoo(@ResponseStatus Value<Integer> foo) {
			return null;
		}
	}
	
	@Test
	public void od03_response_100_examples_swaggerOnMethod() throws Exception {
		assertObjectEquals("{foo:{bar:'c'}}", getSwagger(new OD03()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
		assertObjectEquals("{foo:{bar:'c'}}", getSwaggerWithFile(new OD03()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
	}

	@RestResource(swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{examples:{foo:{bar:'b'}}}}}}}"))
	public static class OD04 {		
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public Foo doFoo(@Response(code=100,examples="{foo:{bar:'d'}}") Value<Foo> foo) {
			return null;
		}
	}

	@Test
	public void od04_response_100_examples_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("{foo:{bar:'d'}}", getSwagger(new OD04()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
		assertObjectEquals("{foo:{bar:'d'}}", getSwaggerWithFile(new OD04()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("paths:{'/path/{foo}/responses/100':{get:{responses:{100:{examples:{foo:{bar:'b'}}}}}}}"))
	public static class OD05 {		
		@RestMethod(name=GET,path="/path/{foo}/responses/100")
		public Foo doFoo(@Response(code=100,examples="{foo:{bar:'$L{foo}'}}") Value<Foo> foo) {
			return null;
		}
	}

	@Test
	public void od05_response_100_examples_swaggerOnAnnotation_lodalized() throws Exception {
		assertObjectEquals("{foo:{bar:'l-foo'}}", getSwagger(new OD05()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
		assertObjectEquals("{foo:{bar:'l-foo'}}", getSwaggerWithFile(new OD05()).getPaths().get("/path/{foo}/responses/100").get("get").getResponse(100).getExamples());
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/responses/<response>/schema
	//-----------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------------
	// @Header on POJO
	//-----------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------------
	// @Header on parameter
	//-----------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------------
	// @Query on POJO
	//-----------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------------
	// @Query on parameter
	//-----------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------------
	// @FormData on POJO
	//-----------------------------------------------------------------------------------------------------------------
	
	//-----------------------------------------------------------------------------------------------------------------
	// @FormData on parameter
	//-----------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------------
	// @Path on POJO
	//-----------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------------
	// @Path on parameter
	//-----------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------------
	// @Body on POJO
	//-----------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------------
	// @Body on parameter
	//-----------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------------
	// @Response on POJO
	//-----------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------------
	// @Response on parameter
	//-----------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------------
	// @Response on throwable
	//-----------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------------
	// @Responses on POJO
	//-----------------------------------------------------------------------------------------------------------------
	
	//-----------------------------------------------------------------------------------------------------------------
	// @ResponseStatus on POJO
	//-----------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------------
	// @ResponseStatuses on POJO
	//-----------------------------------------------------------------------------------------------------------------
	
	//-----------------------------------------------------------------------------------------------------------------
	// @ResponseHeader on POJO
	//-----------------------------------------------------------------------------------------------------------------

	//-----------------------------------------------------------------------------------------------------------------
	// @ResponseHeaders on POJO
	//-----------------------------------------------------------------------------------------------------------------
	
	@Bean(typeName="Foo")
	public static class Foo {
		public int id;
	}

}
