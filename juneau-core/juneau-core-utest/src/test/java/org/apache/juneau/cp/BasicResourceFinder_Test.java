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

import org.apache.juneau.cp.test3.*;
import org.apache.juneau.cp.test4.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class BasicResourceFinder_Test {

	@Test
	public void a01_basic() throws Exception {
		ResourceFinder x = BasicResourceFinder.INSTANCE;

		assertStream(x.findResource(null,"files/Test1.properties",null)).doesNotExist();
		assertStream(x.findResource(null,"files/Test1.properties",JAPANESE)).doesNotExist();
		assertStream(x.findResource(null,"files/Test1.properties",JAPAN)).doesNotExist();
		assertStream(x.findResource(null,"files/Test1.properties",CHINA)).doesNotExist();
		assertStream(x.findResource(Test1.class,"files/Test1.properties",null)).string().contains("Test1.properties");
		assertStream(x.findResource(Test1.class,"files/Test1.properties",JAPANESE)).string().contains("Test1_ja.properties");
		assertStream(x.findResource(Test1.class,"files/Test1.properties",JAPAN)).string().contains("Test1_ja_JP.properties");
		assertStream(x.findResource(Test1.class,"files/Test1.properties",CHINA)).string().contains("Test1.properties");
		assertStream(x.findResource(Test2.class,"files/Test1.properties",null)).doesNotExist();
		assertStream(x.findResource(Test2.class,"files/Test1.properties",JAPANESE)).doesNotExist();
		assertStream(x.findResource(Test2.class,"files/Test1.properties",JAPAN)).doesNotExist();
		assertStream(x.findResource(Test2.class,"files/Test1.properties",CHINA)).doesNotExist();

		assertStream(x.findResource(null,"Test2.properties",null)).doesNotExist();
		assertStream(x.findResource(null,"Test2.properties",JAPANESE)).doesNotExist();
		assertStream(x.findResource(null,"Test2.properties",JAPAN)).doesNotExist();
		assertStream(x.findResource(null,"Test2.properties",CHINA)).doesNotExist();
		assertStream(x.findResource(Test1.class,"Test2.properties",null)).doesNotExist();
		assertStream(x.findResource(Test1.class,"Test2.properties",JAPANESE)).doesNotExist();
		assertStream(x.findResource(Test1.class,"Test2.properties",JAPAN)).doesNotExist();
		assertStream(x.findResource(Test1.class,"Test2.properties",CHINA)).doesNotExist();
		assertStream(x.findResource(Test2.class,"Test2.properties",null)).string().contains("Test2.properties");
		assertStream(x.findResource(Test2.class,"Test2.properties",JAPANESE)).string().contains("Test2_ja.properties");
		assertStream(x.findResource(Test2.class,"Test2.properties",JAPAN)).string().contains("Test2_ja_JP.properties");
		assertStream(x.findResource(Test2.class,"Test2.properties",CHINA)).string().contains("Test2.properties");

		assertStream(x.findResource(null,"files/Test3.properties",null)).string().contains("Test3.properties");
		assertStream(x.findResource(null,"files/Test3.properties",JAPANESE)).string().contains("Test3_ja.properties");
		assertStream(x.findResource(null,"files/Test3.properties",JAPAN)).string().contains("Test3_ja_JP.properties");
		assertStream(x.findResource(null,"files/Test3.properties",CHINA)).string().contains("Test3.properties");
		assertStream(x.findResource(Test1.class,"files/Test3.properties",null)).string().contains("Test3.properties");
		assertStream(x.findResource(Test1.class,"files/Test3.properties",JAPANESE)).string().contains("Test3_ja.properties");
		assertStream(x.findResource(Test1.class,"files/Test3.properties",JAPAN)).string().contains("Test3_ja_JP.properties");
		assertStream(x.findResource(Test1.class,"files/Test3.properties",CHINA)).string().contains("Test3.properties");
		assertStream(x.findResource(Test2.class,"files/Test3.properties",null)).string().contains("Test3.properties");
		assertStream(x.findResource(Test2.class,"files/Test3.properties",JAPANESE)).string().contains("Test3_ja.properties");
		assertStream(x.findResource(Test2.class,"files/Test3.properties",JAPAN)).string().contains("Test3_ja_JP.properties");
		assertStream(x.findResource(Test2.class,"files/Test3.properties",CHINA)).string().contains("Test3.properties");

		assertStream(x.findResource(null,"Test4",null)).doesNotExist();
		assertStream(x.findResource(null,"Test4",JAPANESE)).doesNotExist();
		assertStream(x.findResource(null,"Test4",JAPAN)).doesNotExist();
		assertStream(x.findResource(null,"Test4",CHINA)).doesNotExist();
		assertStream(x.findResource(Test1.class,"Test4",null)).doesNotExist();
		assertStream(x.findResource(Test1.class,"Test4",JAPANESE)).doesNotExist();
		assertStream(x.findResource(Test1.class,"Test4",JAPAN)).doesNotExist();
		assertStream(x.findResource(Test1.class,"Test4",CHINA)).doesNotExist();
		assertStream(x.findResource(Test2.class,"Test4",null)).string().contains("Test4");
		assertStream(x.findResource(Test2.class,"Test4",JAPANESE)).string().contains("Test4_ja");
		assertStream(x.findResource(Test2.class,"Test4",JAPAN)).string().contains("Test4_ja_JP");
		assertStream(x.findResource(Test2.class,"Test4",CHINA)).string().contains("Test4");
	}

	@Test
	public void a02_noFileSystem() throws Exception {
		ResourceFinder x = new BasicResourceFinder(false, false);

		assertStream(x.findResource(null,"files/Test1.properties",null)).doesNotExist();
		assertStream(x.findResource(null,"files/Test1.properties",JAPANESE)).doesNotExist();
		assertStream(x.findResource(null,"files/Test1.properties",JAPAN)).doesNotExist();
		assertStream(x.findResource(null,"files/Test1.properties",CHINA)).doesNotExist();
		assertStream(x.findResource(Test1.class,"files/Test1.properties",null)).string().contains("Test1.properties");
		assertStream(x.findResource(Test1.class,"files/Test1.properties",JAPANESE)).string().contains("Test1_ja.properties");
		assertStream(x.findResource(Test1.class,"files/Test1.properties",JAPAN)).string().contains("Test1_ja_JP.properties");
		assertStream(x.findResource(Test1.class,"files/Test1.properties",CHINA)).string().contains("Test1.properties");
		assertStream(x.findResource(Test2.class,"files/Test1.properties",null)).doesNotExist();
		assertStream(x.findResource(Test2.class,"files/Test1.properties",JAPANESE)).doesNotExist();
		assertStream(x.findResource(Test2.class,"files/Test1.properties",JAPAN)).doesNotExist();
		assertStream(x.findResource(Test2.class,"files/Test1.properties",CHINA)).doesNotExist();

		assertStream(x.findResource(null,"files/Test3.properties",null)).doesNotExist();
		assertStream(x.findResource(null,"files/Test3.properties",JAPANESE)).doesNotExist();
		assertStream(x.findResource(null,"files/Test3.properties",JAPAN)).doesNotExist();
		assertStream(x.findResource(null,"files/Test3.properties",CHINA)).doesNotExist();
		assertStream(x.findResource(Test1.class,"files/Test3.properties",null)).doesNotExist();
		assertStream(x.findResource(Test1.class,"files/Test3.properties",JAPANESE)).doesNotExist();
		assertStream(x.findResource(Test1.class,"files/Test3.properties",JAPAN)).doesNotExist();
		assertStream(x.findResource(Test1.class,"files/Test3.properties",CHINA)).doesNotExist();
		assertStream(x.findResource(Test2.class,"files/Test3.properties",null)).doesNotExist();
		assertStream(x.findResource(Test2.class,"files/Test3.properties",JAPANESE)).doesNotExist();
		assertStream(x.findResource(Test2.class,"files/Test3.properties",JAPAN)).doesNotExist();
		assertStream(x.findResource(Test2.class,"files/Test3.properties",CHINA)).doesNotExist();
	}

	@Test
	public void a03_recursive() throws Exception {
		ResourceFinder x = new BasicResourceFinder(true, true);

		assertStream(x.findResource(null,"files/Test1.properties",null)).doesNotExist();
		assertStream(x.findResource(null,"files/Test1.properties",JAPANESE)).doesNotExist();
		assertStream(x.findResource(null,"files/Test1.properties",JAPAN)).doesNotExist();
		assertStream(x.findResource(null,"files/Test1.properties",CHINA)).doesNotExist();
		assertStream(x.findResource(Test1.class,"files/Test1.properties",null)).string().contains("Test1.properties");
		assertStream(x.findResource(Test1.class,"files/Test1.properties",JAPANESE)).string().contains("Test1_ja.properties");
		assertStream(x.findResource(Test1.class,"files/Test1.properties",JAPAN)).string().contains("Test1_ja_JP.properties");
		assertStream(x.findResource(Test1.class,"files/Test1.properties",CHINA)).string().contains("Test1.properties");
		assertStream(x.findResource(Test2.class,"files/Test1.properties",null)).string().contains("Test1.properties");
		assertStream(x.findResource(Test2.class,"files/Test1.properties",JAPANESE)).string().contains("Test1_ja.properties");
		assertStream(x.findResource(Test2.class,"files/Test1.properties",JAPAN)).string().contains("Test1_ja_JP.properties");
		assertStream(x.findResource(Test2.class,"files/Test1.properties",CHINA)).string().contains("Test1.properties");

		assertStream(x.findResource(null,"Test2.properties",null)).doesNotExist();
		assertStream(x.findResource(null,"Test2.properties",JAPANESE)).doesNotExist();
		assertStream(x.findResource(null,"Test2.properties",JAPAN)).doesNotExist();
		assertStream(x.findResource(null,"Test2.properties",CHINA)).doesNotExist();
		assertStream(x.findResource(Test1.class,"Test2.properties",null)).doesNotExist();
		assertStream(x.findResource(Test1.class,"Test2.properties",JAPANESE)).doesNotExist();
		assertStream(x.findResource(Test1.class,"Test2.properties",JAPAN)).doesNotExist();
		assertStream(x.findResource(Test1.class,"Test2.properties",CHINA)).doesNotExist();
		assertStream(x.findResource(Test2.class,"Test2.properties",null)).string().contains("Test2.properties");
		assertStream(x.findResource(Test2.class,"Test2.properties",JAPANESE)).string().contains("Test2_ja.properties");
		assertStream(x.findResource(Test2.class,"Test2.properties",JAPAN)).string().contains("Test2_ja_JP.properties");
		assertStream(x.findResource(Test2.class,"Test2.properties",CHINA)).string().contains("Test2.properties");

		assertStream(x.findResource(null,"files/Test3.properties",null)).string().contains("Test3.properties");
		assertStream(x.findResource(null,"files/Test3.properties",JAPANESE)).string().contains("Test3_ja.properties");
		assertStream(x.findResource(null,"files/Test3.properties",JAPAN)).string().contains("Test3_ja_JP.properties");
		assertStream(x.findResource(null,"files/Test3.properties",CHINA)).string().contains("Test3.properties");
		assertStream(x.findResource(Test1.class,"files/Test3.properties",null)).string().contains("Test3.properties");
		assertStream(x.findResource(Test1.class,"files/Test3.properties",JAPANESE)).string().contains("Test3_ja.properties");
		assertStream(x.findResource(Test1.class,"files/Test3.properties",JAPAN)).string().contains("Test3_ja_JP.properties");
		assertStream(x.findResource(Test1.class,"files/Test3.properties",CHINA)).string().contains("Test3.properties");
		assertStream(x.findResource(Test2.class,"files/Test3.properties",null)).string().contains("Test3.properties");
		assertStream(x.findResource(Test2.class,"files/Test3.properties",JAPANESE)).string().contains("Test3_ja.properties");
		assertStream(x.findResource(Test2.class,"files/Test3.properties",JAPAN)).string().contains("Test3_ja_JP.properties");
		assertStream(x.findResource(Test2.class,"files/Test3.properties",CHINA)).string().contains("Test3.properties");
	}

	@Test
	public void a04_invalidNames() throws Exception {
		ResourceFinder x = BasicResourceFinder.INSTANCE;

		for (String s : new String[]{"bad.properties","",null,"files/../files/Test1.properties"}) {
			assertStream(x.findResource(null,s,null)).doesNotExist();
			assertStream(x.findResource(null,s,JAPANESE)).doesNotExist();
			assertStream(x.findResource(null,s,JAPAN)).doesNotExist();
			assertStream(x.findResource(null,s,CHINA)).doesNotExist();
			assertStream(x.findResource(Test1.class,s,null)).doesNotExist();
			assertStream(x.findResource(Test1.class,s,JAPANESE)).doesNotExist();
			assertStream(x.findResource(Test1.class,s,JAPAN)).doesNotExist();
			assertStream(x.findResource(Test1.class,s,CHINA)).doesNotExist();
			assertStream(x.findResource(Test2.class,s,null)).doesNotExist();
			assertStream(x.findResource(Test2.class,s,JAPANESE)).doesNotExist();
			assertStream(x.findResource(Test2.class,s,JAPAN)).doesNotExist();
			assertStream(x.findResource(Test2.class,s,CHINA)).doesNotExist();
		}

		String s = ".";
		assertStream(x.findResource(null,s,null)).doesNotExist();
		assertStream(x.findResource(null,s,JAPANESE)).doesNotExist();
		assertStream(x.findResource(null,s,JAPAN)).doesNotExist();
		assertStream(x.findResource(null,s,CHINA)).doesNotExist();
		assertStream(x.findResource(Test1.class,s,null)).string().contains("Test1.class");
		assertStream(x.findResource(Test1.class,s,JAPANESE)).string().contains("Test1.class");
		assertStream(x.findResource(Test1.class,s,JAPAN)).string().contains("Test1.class");
		assertStream(x.findResource(Test1.class,s,CHINA)).string().contains("Test1.class");
		assertStream(x.findResource(Test2.class,s,null)).string().contains("Test2.class");
		assertStream(x.findResource(Test2.class,s,JAPANESE)).string().contains("Test2.class");
		assertStream(x.findResource(Test2.class,s,JAPAN)).string().contains("Test2.class");
		assertStream(x.findResource(Test2.class,s,CHINA)).string().contains("Test2.class");
	}
}
