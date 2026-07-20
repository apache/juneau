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
package org.apache.juneau.rest.server.swagger;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link SwaggerException} message formatting.
 *
 * <p>
 * Regression: the constructor used to pre-format the location with {@code f(...)} and then pass the concatenated
 * result to a printf constructor, giving a second format pass on which a surviving literal {@code '%'} could be
 * re-interpreted as a format directive.  The message must now be formatted exactly once.
 */
class SwaggerException_Test extends TestBase {

	@Test void a01_singleFormatPass() {
		var ex = new SwaggerException(null, "Unexpected data type '%s'.", "Map");
		assertEquals("Swagger exception:  at Unexpected data type 'Map'.", ex.getMessage());
	}

	@Test void a02_literalPercentInArgSurvives() {
		// A literal '%' inside an argument value must be substituted verbatim and must not throw.
		var ex = new SwaggerException(null, "Unexpected data type '%s'.", "50% off");
		assertEquals("Swagger exception:  at Unexpected data type '50% off'.", ex.getMessage());
	}

	@Test void a03_causePreserved() {
		var cause = new IllegalStateException("nope");
		var ex = new SwaggerException(cause, "In %s.", "location");
		assertSame(cause, ex.getCause());
		assertEquals("Swagger exception:  at In location.", ex.getMessage());
	}
}
