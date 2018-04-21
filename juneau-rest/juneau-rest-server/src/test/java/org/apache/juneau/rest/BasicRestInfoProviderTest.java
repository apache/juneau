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

import static org.apache.juneau.rest.TestUtils.*;

import java.io.*;
import java.util.*;

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
		Swagger s = ip.getSwagger(req);
		s.setSwagger(null);
		return s;
	}

	private Swagger getSwagger(Object resource) throws Exception {
		RestContext rc = RestContext.create(resource).build();
		RestRequest req = rc.getCallHandler().createRequest(new MockHttpServletRequest());
		RestInfoProvider ip = rc.getInfoProvider();
		Swagger s = ip.getSwagger(req);
		s.setSwagger(null);
		return s;
	}
	
	public static class TestClasspathResourceFinder extends ClasspathResourceFinderBasic {

		@Override
		public InputStream findResource(Class<?> baseClass, String name, Locale locale) throws IOException {
			if (name.endsWith(".json"))
				return BasicRestInfoProvider.class.getResourceAsStream("BasicRestinfoProviderTest_swagger.json");
			return super.findResource(baseClass, name, locale);
		}
		
	}
	
	
	//-----------------------------------------------------------------------------------------------------------------
	// /info/title
	//-----------------------------------------------------------------------------------------------------------------

	@RestResource(title="a-title")
	public static class A1 {}

	@Test
	public void title_RestResource_title() throws Exception {
		assertObjectEquals("{info:{title:'a-title'}}", getSwagger(new A1()));
		assertObjectEquals("{info:{title:'s-title'}}", getSwaggerWithFile(new A1()));
	}

	
	@RestResource(title="$L{foo}",messages="BasicRestInfoProviderTest")
	public static class A1L {}

	@Test
	public void title_RestResource_title_localized() throws Exception {
		assertObjectEquals("{info:{title:'l-foo'}}", getSwagger(new A1L()));
		assertObjectEquals("{info:{title:'s-title'}}", getSwaggerWithFile(new A1L()));
	}

	
	@RestResource(title="a-title", swagger=@ResourceSwagger("{info:{title:'b-title'}}"))
	public static class A2 {}

	@Test
	public void title_ResourceSwagger_value() throws Exception {
		assertObjectEquals("{info:{title:'b-title'}}", getSwagger(new A2()));
		assertObjectEquals("{info:{title:'b-title'}}", getSwaggerWithFile(new A2()));
	}
	

	@RestResource(title="a-title", swagger=@ResourceSwagger("{info:{title:'$L{bar}'}}"), messages="BasicRestInfoProviderTest")
	public static class A2L {}
	
	@Test
	public void title_ResourceSwagger_value_localised() throws Exception {
		assertObjectEquals("{info:{title:'l-bar'}}", getSwagger(new A2L()));
		assertObjectEquals("{info:{title:'l-bar'}}", getSwaggerWithFile(new A2L()));
	}

	
	@RestResource(title="a-title", swagger=@ResourceSwagger(value="{info:{title:'b-title'}}", title="c-title"))
	public static class A3 {}

	@Test
	public void title_ResourceSwagger_title() throws Exception {
		assertObjectEquals("{info:{title:'c-title'}}", getSwagger(new A3()));
		assertObjectEquals("{info:{title:'c-title'}}", getSwaggerWithFile(new A3()));
	}
	
	
	@RestResource(title="a-title", swagger=@ResourceSwagger(value="{info:{title:'b-title'}}", title="$L{baz}"), messages="BasicRestInfoProviderTest")
	public static class A3L {}
	
	@Test
	public void title_RsourceSwagger_title_localized() throws Exception {
		assertObjectEquals("{info:{title:'l-baz'}}", getSwagger(new A3L()));
		assertObjectEquals("{info:{title:'l-baz'}}", getSwaggerWithFile(new A3L()));
	}

	
	@RestResource(swagger=@ResourceSwagger(title="c-title"))
	public static class A4 {}

	@Test
	public void title_RsourceSwagger_title_only() throws Exception {
		assertObjectEquals("{info:{title:'c-title'}}", getSwagger(new A4()));
		assertObjectEquals("{info:{title:'c-title'}}", getSwaggerWithFile(new A4()));
	}
}
