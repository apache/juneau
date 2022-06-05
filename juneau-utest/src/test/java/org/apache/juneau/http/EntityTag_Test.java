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
package org.apache.juneau.http;

import static org.junit.runners.MethodSorters.*;

import java.util.function.*;

import static org.apache.juneau.assertions.Assertions.*;

import org.apache.juneau.http.header.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class EntityTag_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Method tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_basic() throws Exception {

		EntityTag x1 = new EntityTag("\"foo\"");
		assertString(x1).is("\"foo\"");
		assertString(x1.getEntityValue()).is("foo");
		assertBoolean(x1.isWeak()).isFalse();
		assertBoolean(x1.isAny()).isFalse();

		EntityTag x2 = new EntityTag("W/\"foo\"");
		assertString(x2).is("W/\"foo\"");
		assertString(x2.getEntityValue()).is("foo");
		assertBoolean(x2.isWeak()).isTrue();
		assertBoolean(x2.isAny()).isFalse();

		EntityTag x3 = new EntityTag("*");
		assertString(x3).is("*");
		assertString(x3.getEntityValue()).is("*");
		assertBoolean(x3.isWeak()).isFalse();
		assertBoolean(x3.isAny()).isTrue();

		EntityTag x5 = new EntityTag("\"\"");
		assertString(x5).is("\"\"");
		assertString(x5.getEntityValue()).is("");
		assertBoolean(x5.isWeak()).isFalse();
		assertBoolean(x5.isAny()).isFalse();

		EntityTag x6 = EntityTag.of("\"foo\"");
		assertString(x6).is("\"foo\"");
		assertString(x6.getEntityValue()).is("foo");
		assertBoolean(x6.isWeak()).isFalse();
		assertBoolean(x6.isAny()).isFalse();

		EntityTag x7 = EntityTag.of((Supplier<?>)()->"\"foo\"");
		assertString(x7).is("\"foo\"");
		assertString(x7.getEntityValue()).is("foo");
		assertBoolean(x7.isWeak()).isFalse();
		assertBoolean(x7.isAny()).isFalse();

		assertObject(EntityTag.of(null)).isNull();
		assertObject(EntityTag.of((Supplier<?>)()->null)).isNull();

		assertThrown(()->new EntityTag("foo")).asMessage().is("Invalid value for entity-tag: [foo]");
		assertThrown(()->new EntityTag("\"")).asMessage().is("Invalid value for entity-tag: [\"]");
		assertThrown(()->new EntityTag("")).asMessage().is("Invalid value for entity-tag: []");
		assertThrown(()->new EntityTag(null)).asMessage().is("Argument 'value' cannot be null.");
		assertThrown(()->new EntityTag("\"a")).asMessage().is("Invalid value for entity-tag: [\"a]");
		assertThrown(()->new EntityTag("a\"")).asMessage().is("Invalid value for entity-tag: [a\"]");
		assertThrown(()->new EntityTag("W/\"")).asMessage().is("Invalid value for entity-tag: [W/\"]");
	}
}
