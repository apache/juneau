/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.commons.beanquery;

import static org.apache.juneau.commons.TestAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.TestBase;
import org.junit.jupiter.api.*;

class BeanQuerySyntaxException_Test extends TestBase {

	//====================================================================================================
	// a - Coded constructor
	//====================================================================================================

	@Test
	void a01_codedConstructor_carriesCode() {
		var e = new BeanQuerySyntaxException(BeanQuerySyntaxException.Code.UNKNOWN_OPERATOR, "Unknown search operator: '%s'", "$foo");
		assertBean(e, "code,message", "UNKNOWN_OPERATOR,Unknown search operator: '$foo'");
	}

	@Test
	void a02_code_neverNullOnAnyConstant() {
		for (var code : BeanQuerySyntaxException.Code.values()) {
			var e = new BeanQuerySyntaxException(code, "x");
			assertNotNull(e.code());
			assertEquals(code, e.code());
		}
	}
}
