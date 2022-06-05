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
import static org.apache.juneau.cp.DefaultClassList.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class DefaultClassList_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_basic() {
		DefaultClassList x = create();
		assertOptional(x.get(String.class)).isNull();

		x = of(Long.class,null);
		assertOptional(x.get(String.class)).isNull();
		assertOptional(x.get(Long.class)).isNotNull();
		assertOptional(x.get(Number.class)).isNotNull();
		assertOptional(x.get(Object.class)).isNotNull();

		assertThrown(()->create().get(null)).asMessage().is("Argument 'type' cannot be null.");
	}
}
