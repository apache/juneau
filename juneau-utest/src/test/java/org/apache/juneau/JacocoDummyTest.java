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
package org.apache.juneau;

import static org.apache.juneau.utest.utils.Utils2.*;
import java.lang.reflect.*;

import org.apache.juneau.common.internal.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.xml.annotation.*;
import org.junit.jupiter.api.*;

class JacocoDummyTest extends SimpleTestBase {

	//====================================================================================================
	// Dummy code to add test coverage in Jacoco.
	//====================================================================================================
	@Test void accessPrivateConstructorsOnStaticUtilityClasses() throws Exception {

		Class<?>[] classes = new Class[] {
			StringUtils.class, ArrayUtils.class, ClassUtils.class, CollectionUtils.class
		};

		for (Class<?> c : classes) {
			Constructor<?> c1 = c.getDeclaredConstructor();
			c1.setAccessible(true);
			c1.newInstance();
		}

		XmlFormat.valueOf(XmlFormat.DEFAULT.toString());
		assertNotThrown(()->Visibility.valueOf(Visibility.DEFAULT.toString()));
	}
}