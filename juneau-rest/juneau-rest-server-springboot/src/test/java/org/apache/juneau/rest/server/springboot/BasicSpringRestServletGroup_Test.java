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
package org.apache.juneau.rest.server.springboot;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Validates that {@link BasicSpringRestServletGroup} is instantiable through a concrete subclass
 * (the class itself contributes no logic beyond the {@code @Rest(mixins=NavigationMixin.class)}
 * annotation and inherited {@link BasicSpringRestServlet} behavior).
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.SpringbootTest
class BasicSpringRestServletGroup_Test extends TestBase {

	public static class GroupHost extends BasicSpringRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_instantiable() {
		assertNotNull(new GroupHost());
	}
}
