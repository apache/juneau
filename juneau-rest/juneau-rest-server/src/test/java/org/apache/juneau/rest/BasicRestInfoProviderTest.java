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

import org.apache.juneau.annotation.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.utils.*;
import org.junit.*;

@SuppressWarnings("javadoc")
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
	// /paths/<path>/<method>
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource()
	public static class M01 {
		
		@RestMethod(name=GET,path="/path/{foo}")
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void m01_operation_default() throws Exception {
		assertObjectEquals("{operationId:'doFoo',responses:{'200':{description:'OK',schema:{type:'object',properties:{id:{format:'int32',type:'integer'}}}}}}", getSwagger(new M01()).getPaths().get("/path/{foo}").get("get"));
		assertObjectEquals("{operationId:'s-operationId',summary:'s-summary',description:'s-description',tags:['s-tag'],externalDocs:{description:'s-description',url:'s-url'},consumes:['s-consumes'],produces:['s-produces'],responses:{'200':{description:'OK',schema:{type:'object',properties:{id:{format:'int32',type:'integer'}}}}},schemes:['s-scheme'],deprecated:true}", getSwaggerWithFile(new M01()).getPaths().get("/path/{foo}").get("get"));
	}

	@RestResource(swagger=@ResourceSwagger("{paths:{'/path/{foo}':{get:{summary:'a-summary',description:'a-description',operationId:'a-operationId',deprecated:true,consumes:['a-consumes'],produces:['a-produces'],tags:['a-tag'],schemes:['a-scheme'],externalDocs:{description: 'a-description',url: 'a-url'}}}}}"))
	public static class M02 {		
		@RestMethod(name=GET,path="/path/{foo}")
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void m02_operation_swaggerOnClass() throws Exception {
		assertObjectEquals("{operationId:'a-operationId',summary:'a-summary',description:'a-description',tags:['a-tag'],externalDocs:{description:'a-description',url:'a-url'},consumes:['a-consumes'],produces:['a-produces'],responses:{'200':{description:'OK',schema:{type:'object',properties:{id:{format:'int32',type:'integer'}}}}},schemes:['a-scheme'],deprecated:true}", getSwagger(new M02()).getPaths().get("/path/{foo}").get("get"));
		assertObjectEquals("{operationId:'a-operationId',summary:'a-summary',description:'a-description',tags:['a-tag'],externalDocs:{description:'a-description',url:'a-url'},consumes:['a-consumes'],produces:['a-produces'],responses:{'200':{description:'OK',schema:{type:'object',properties:{id:{format:'int32',type:'integer'}}}}},schemes:['a-scheme'],deprecated:true}", getSwaggerWithFile(new M02()).getPaths().get("/path/{foo}").get("get"));
	}

	@RestResource(swagger=@ResourceSwagger("{paths:{'/path/{foo}':{get:{summary:'a-summary',description:'a-description',operationId:'a-operationId',deprecated:true,consumes:['a-consumes'],produces:['a-produces'],tags:['a-tag'],schemes:['a-scheme'],externalDocs:{description:'a-description',url:'a-url'}}}}}"))
	public static class M03 {		
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger("summary:'b-summary',description:'b-description',operationId:'b-operationId',deprecated:false,consumes:['b-consumes'],produces:['b-produces'],tags:['b-tag'],schemes:['b-scheme'],externalDocs:{description:'b-description',url:'b-url'}}"))
		public Foo doFoo() {
			return null;
		}
	}
	
	@Test
	public void m03_operation_swaggerOnMethod() throws Exception {
		assertObjectEquals("{operationId:'b-operationId',summary:'b-summary',description:'b-description',tags:['b-tag'],externalDocs:{description:'b-description',url:'b-url'},consumes:['b-consumes'],produces:['b-produces'],responses:{'200':{description:'OK',schema:{type:'object',properties:{id:{format:'int32',type:'integer'}}}}},schemes:['b-scheme'],deprecated:false}", getSwagger(new M03()).getPaths().get("/path/{foo}").get("get"));
		assertObjectEquals("{operationId:'b-operationId',summary:'b-summary',description:'b-description',tags:['b-tag'],externalDocs:{description:'b-description',url:'b-url'},consumes:['b-consumes'],produces:['b-produces'],responses:{'200':{description:'OK',schema:{type:'object',properties:{id:{format:'int32',type:'integer'}}}}},schemes:['b-scheme'],deprecated:false}", getSwaggerWithFile(new M03()).getPaths().get("/path/{foo}").get("get"));
	}

	@RestResource(swagger=@ResourceSwagger("{paths:{'/path/{foo}':{get:{summary:'a-summary',description:'a-description',operationId:'a-operationId',deprecated:true,consumes:['a-consumes'],produces:['a-produces'],tags:['a-tag'],schemes:['a-scheme'],externalDocs:{description:'a-description',url:'a-url'}}}}}"))
	public static class M04 {		
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(summary="b-summary",description="b-description",operationId="b-operationId",deprecated="false",consumes="b-consumes",produces="b-produces",tags="b-tag",schemes="b-scheme",externalDocs="description:'b-description',url:'b-url'"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void m04_operation_swaggerOnAnnotation() throws Exception {
		assertObjectEquals("{operationId:'b-operationId',summary:'b-summary',description:'b-description',tags:['b-tag'],externalDocs:{description:'b-description',url:'b-url'},consumes:['b-consumes'],produces:['b-produces'],responses:{'200':{description:'OK',schema:{type:'object',properties:{id:{format:'int32',type:'integer'}}}}},schemes:['b-scheme'],deprecated:false}", getSwagger(new M04()).getPaths().get("/path/{foo}").get("get"));
		assertObjectEquals("{operationId:'b-operationId',summary:'b-summary',description:'b-description',tags:['b-tag'],externalDocs:{description:'b-description',url:'b-url'},consumes:['b-consumes'],produces:['b-produces'],responses:{'200':{description:'OK',schema:{type:'object',properties:{id:{format:'int32',type:'integer'}}}}},schemes:['b-scheme'],deprecated:false}", getSwaggerWithFile(new M04()).getPaths().get("/path/{foo}").get("get"));
	}

	@RestResource(messages="BasicRestInfoProviderTest", swagger=@ResourceSwagger("{paths:{'/path/{foo}':{get:{summary:'a-summary',description:'a-description',operationId:'a-operationId',deprecated:true,consumes:['a-consumes'],produces:['a-produces'],tags:['a-tag'],schemes:['a-scheme'],externalDocs:{description:'a-description',url:'a-url'}}}}}"))
	public static class M05 {		
		@RestMethod(name=GET,path="/path/{foo}",swagger=@MethodSwagger(summary="$L{foo}",description="$L{foo}",operationId="$L{foo}",deprecated="$L{foo}",consumes="$L{foo}",produces="$L{foo}",tags="$L{foo}",schemes="$L{foo}",externalDocs="description:'$L{foo}',url:'$L{foo}'"))
		public Foo doFoo() {
			return null;
		}
	}

	@Test
	public void m05_operation_swaggerOnAnnotation_localized() throws Exception {
		assertObjectEquals("{operationId:'l-foo',summary:'l-foo',description:'l-foo',tags:['l-foo'],externalDocs:{description:'l-foo',url:'l-foo'},consumes:['l-foo'],produces:['l-foo'],responses:{'200':{description:'OK',schema:{type:'object',properties:{id:{format:'int32',type:'integer'}}}}},schemes:['l-foo'],deprecated:false}", getSwagger(new M05()).getPaths().get("/path/{foo}").get("get"));
		assertObjectEquals("{operationId:'l-foo',summary:'l-foo',description:'l-foo',tags:['l-foo'],externalDocs:{description:'l-foo',url:'l-foo'},consumes:['l-foo'],produces:['l-foo'],responses:{'200':{description:'OK',schema:{type:'object',properties:{id:{format:'int32',type:'integer'}}}}},schemes:['l-foo'],deprecated:false}", getSwaggerWithFile(new M05()).getPaths().get("/path/{foo}").get("get"));
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// /paths/<path>/<method>/description
	//-----------------------------------------------------------------------------------------------------------------
	
	//	paths: {
//		'/path/{foo}': {
//			get: {
//				summary: 's-summary',
//				description: 's-description',
//				operationId: 's-operationId',
//				deprecated: true,
//				consumes: ['s-consumes'],
//				produces: ['s-produces'],
//				tags: ['s-tag'],
//				schemes: ['s-scheme'],
//				externalDocs: {
//					description: 's-description',
//					url: 's-url'
//				},
//				parameters: [
//					{
//						name: 's-query-name',
//						in: 'query',
//						description: 's-description',
//						type: 'string',
//						format: 's-format',
//						pattern: 's-pattern',
//						collectionFormat: 's-collectionFormat',
//						minimum: 1.0,
//						maximum: 2.0,
//						multipleOf: 3.0,
//						minLength: 1,
//						maxLength: 2,
//						minItems: 3,
//						maxItems: 4,
//						required: true,
//						allowEmptyValue: true,
//						exclusiveMinimum: true,
//						exclusiveMaximum: true,
//						uniqueItems: true,						
//						schemaInfo: {
//						}						
//					},
//					{
//						name: 's-header-name',
//						in: 'header',
//						description: 's-description',
//						type: 'string',
//						format: 's-format',
//						pattern: 's-pattern',
//						collectionFormat: 's-collectionFormat',
//						minimum: 1.0,
//						maximum: 2.0,
//						multipleOf: 3.0,
//						minLength: 1,
//						maxLength: 2,
//						minItems: 3,
//						maxItems: 4,
//						required: true,
//						allowEmptyValue: true,
//						exclusiveMinimum: true,
//						exclusiveMaximum: true,
//						uniqueItems: true,						
//						schemaInfo: {
//							$ref: '#/definitions/Foo'
//						}						
//					},
//					{
//						name: 's-path-name',
//						in: 'path',
//						description: 's-description',
//						type: 'string',
//						format: 's-format',
//						pattern: 's-pattern',
//						collectionFormat: 's-collectionFormat',
//						minimum: 1.0,
//						maximum: 2.0,
//						multipleOf: 3.0,
//						minLength: 1,
//						maxLength: 2,
//						minItems: 3,
//						maxItems: 4,
//						required: true,
//						allowEmptyValue: true,
//						exclusiveMinimum: true,
//						exclusiveMaximum: true,
//						uniqueItems: true,						
//						schemaInfo: {
//							$ref: '#/definitions/Foo'
//						}						
//					},
//					{
//						name: 's-formData-name',
//						in: 'formData',
//						description: 's-description',
//						type: 'string',
//						format: 's-format',
//						pattern: 's-pattern',
//						collectionFormat: 's-collectionFormat',
//						minimum: 1.0,
//						maximum: 2.0,
//						multipleOf: 3.0,
//						minLength: 1,
//						maxLength: 2,
//						minItems: 3,
//						maxItems: 4,
//						required: true,
//						allowEmptyValue: true,
//						exclusiveMinimum: true,
//						exclusiveMaximum: true,
//						uniqueItems: true,						
//						schemaInfo: {
//							$ref: '#/definitions/Foo'
//						}						
//					},
//					{
//						name: 's-body-name',
//						in: 'body',
//						description: 's-description',
//						type: 'string',
//						format: 's-format',
//						pattern: 's-pattern',
//						collectionFormat: 's-collectionFormat',
//						minimum: 1.0,
//						maximum: 2.0,
//						multipleOf: 3.0,
//						minLength: 1,
//						maxLength: 2,
//						minItems: 3,
//						maxItems: 4,
//						required: true,
//						allowEmptyValue: true,
//						exclusiveMinimum: true,
//						exclusiveMaximum: true,
//						uniqueItems: true,						
//						schemaInfo: {
//							$ref: '#/definitions/Foo'
//						}						
//					}
//				],
//				responses: {
//					100: {
//						description:'s-100-description',
//						schema: {
//							type: array,
//							items: {
//								$ref: '#/definitions/Foo'
//							}
//						},
//						headers: {
//							X-Foo: {
//								type: 'integer',
//								format: 'int32',
//								description: 's-description'
//							}
//						},
//						examples: {
//							foo: {bar:123},
//							bar: 'baz'
//						}
//					}
//				},
//				security: [
//					{foo_auth:['read:foo','write-foo']}
//				]
//			} 
//		}
//	},
//	definitions: {
//		Foo: {
//			type: 'object',
//			properties: {
//				id: {
//					type: 'integer',
//					format: 'int64'
//				}
//			},
//			xml: {
//				name: 'Foo'
//			}
//		}
//	}
	
	@Bean(typeName="Foo")
	public static class Foo {
		public int id;
	}

}
