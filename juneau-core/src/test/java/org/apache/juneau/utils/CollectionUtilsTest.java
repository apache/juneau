/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.utils;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;

import org.apache.juneau.*;
import org.junit.*;

@SuppressWarnings("javadoc")
public class CollectionUtilsTest {

	//====================================================================================================
	// reverse(LinkedHashMap)
	//====================================================================================================
	@Test
	public void testReverse() throws Exception {
		assertNull(reverse(null));

		assertObjectEquals("{b:2,a:1}", reverse(new ObjectMap("{a:1,b:2}")));
		assertObjectEquals("{}", reverse(new ObjectMap("{}")));
	}
}
