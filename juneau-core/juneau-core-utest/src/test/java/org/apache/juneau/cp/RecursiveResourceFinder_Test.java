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
public class RecursiveResourceFinder_Test {

	@Test
	public void a01_basic() throws Exception {
		ResourceFinder x = RecursiveResourceFinder.INSTANCE;

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
}
