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
package org.apache.juneau.cp;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;
import static java.util.Locale.*;

import org.apache.juneau.cp.test2.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.plaintext.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Resources_Test {

	private static Parser PARSER = PlainTextParser.DEFAULT;

	@Test
	public void a01_basic_BasicResourceFinder_nocache() throws Exception {
		ResourceManager rm = of(Test2.class);
		String f, fja, fjp;

		f = "Test2.properties"; fja = "Test2_ja.properties"; fjp = "Test2_ja_JP.properties";
		assertStream(rm.getStream(f)).string().contains(f);
		assertStream(rm.getStream(f,JAPANESE)).string().contains(fja);
		assertStream(rm.getStream(f,JAPAN)).string().contains(fjp);
		assertStream(rm.getStream(f,CHINA)).string().contains(f);
		assertString(rm.getString(f)).contains(f);
		assertString(rm.getString(f,JAPANESE)).contains(fja);
		assertString(rm.getString(f,JAPAN)).contains(fjp);
		assertString(rm.getString(f,CHINA)).contains(f);
		assertObject(rm.getResource(String.class,PARSER,f)).string().contains(f);
		assertObject(rm.getResource(String.class,PARSER,f,JAPANESE)).string().contains(fja);
		assertObject(rm.getResource(String.class,PARSER,f,JAPAN)).string().contains(fjp);
		assertObject(rm.getResource(String.class,PARSER,f,CHINA)).string().contains(f);

		f = "bad.properties"; fja = "bad.properties"; fjp = "bad.properties";
		assertStream(rm.getStream(f)).doesNotExist();
		assertStream(rm.getStream(f,JAPANESE)).doesNotExist();
		assertStream(rm.getStream(f,JAPAN)).doesNotExist();
		assertStream(rm.getStream(f,CHINA)).doesNotExist();
		assertString(rm.getString(f)).doesNotExist();
		assertString(rm.getString(f,JAPANESE)).doesNotExist();
		assertString(rm.getString(f,JAPAN)).doesNotExist();
		assertString(rm.getString(f,CHINA)).doesNotExist();
		assertObject(rm.getResource(String.class,PARSER,f)).doesNotExist();
		assertObject(rm.getResource(String.class,PARSER,f,JAPANESE)).doesNotExist();
		assertObject(rm.getResource(String.class,PARSER,f,JAPAN)).doesNotExist();
		assertObject(rm.getResource(String.class,PARSER,f,CHINA)).doesNotExist();
	}

	@Test
	public void a02_basic_BasicResourceFinder_cache() throws Exception {
		ResourceManager rm = of(Test2.class, BasicResourceFinder.INSTANCE, true);
		String f, fja, fjp;

		for (int i = 0; i <= 1; i++) {
			f = "Test2.properties"; fja = "Test2_ja.properties"; fjp = "Test2_ja_JP.properties";
			assertStream(rm.getStream(f)).string().contains(f);
			assertStream(rm.getStream(f,JAPANESE)).string().contains(fja);
			assertStream(rm.getStream(f,JAPAN)).string().contains(fjp);
			assertStream(rm.getStream(f,CHINA)).string().contains(f);
			assertString(rm.getString(f)).contains(f);
			assertString(rm.getString(f,JAPANESE)).contains(fja);
			assertString(rm.getString(f,JAPAN)).contains(fjp);
			assertString(rm.getString(f,CHINA)).contains(f);
			assertObject(rm.getResource(String.class,PARSER,f)).string().contains(f);
			assertObject(rm.getResource(String.class,PARSER,f,JAPANESE)).string().contains(fja);
			assertObject(rm.getResource(String.class,PARSER,f,JAPAN)).string().contains(fjp);
			assertObject(rm.getResource(String.class,PARSER,f,CHINA)).string().contains(f);

			f = "bad.properties"; fja = "bad.properties"; fjp = "bad.properties";
			assertStream(rm.getStream(f)).doesNotExist();
			assertStream(rm.getStream(f,JAPANESE)).doesNotExist();
			assertStream(rm.getStream(f,JAPAN)).doesNotExist();
			assertStream(rm.getStream(f,CHINA)).doesNotExist();
			assertString(rm.getString(f)).doesNotExist();
			assertString(rm.getString(f,JAPANESE)).doesNotExist();
			assertString(rm.getString(f,JAPAN)).doesNotExist();
			assertString(rm.getString(f,CHINA)).doesNotExist();
			assertObject(rm.getResource(String.class,PARSER,f)).doesNotExist();
			assertObject(rm.getResource(String.class,PARSER,f,JAPANESE)).doesNotExist();
			assertObject(rm.getResource(String.class,PARSER,f,JAPAN)).doesNotExist();
			assertObject(rm.getResource(String.class,PARSER,f,CHINA)).doesNotExist();
		}
	}

	@Test
	public void a03_basic_other() throws Exception {
		ResourceManager rm = of(Test2.class);

		assertStream(rm.getStream(null)).doesNotExist();
		assertString(rm.getString(null)).doesNotExist();
		assertObject(rm.getResource(String.class,PARSER,null)).doesNotExist();
		assertStream(rm.getStream("")).doesNotExist();
		assertString(rm.getString("")).doesNotExist();
		assertObject(rm.getResource(String.class,PARSER,"")).doesNotExist();
	}


	//------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//------------------------------------------------------------------------------------------------------------------

	private ResourceManager of(Class<?> baseClass, ResourceFinder resourceFinder, boolean useCache) {
		return new ResourceManager(baseClass, resourceFinder, useCache);
	}

	private ResourceManager of(Class<?> baseClass) {
		return new ResourceManager(baseClass);
	}
}
