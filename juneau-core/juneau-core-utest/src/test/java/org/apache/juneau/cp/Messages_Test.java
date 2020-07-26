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

import java.util.*;

import static java.util.Locale.*;

import org.apache.juneau.cp.test1.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Messages_Test {

	@Test
	public void a01_nonExistent() throws Exception {
		assertThrown(()->Messages.of(Test1.class)).contains("Could not find bundle path for class");
		assertThrown(()->Messages.of(Test1.class,"bad.properties")).contains("Bundle path should not end with '.properties'");
	}

	@Test
	public void a02_sameDirectory() throws Exception {
		Messages x = Messages.of(MessageBundleTest1.class);
		assertString(x.getString("file")).is("MessageBundleTest1.properties");
		assertString(x.getBundle(JAPANESE).getString("file")).is("MessageBundleTest1_ja.properties");
		assertString(x.getBundle(JAPAN).getString("file")).is("MessageBundleTest1_ja_JP.properties");
		assertString(x.getBundle(CHINA).getString("file")).is("MessageBundleTest1.properties");
		assertString(x.getBundle((Locale)null).getString("file")).is("MessageBundleTest1.properties");
	}
}
