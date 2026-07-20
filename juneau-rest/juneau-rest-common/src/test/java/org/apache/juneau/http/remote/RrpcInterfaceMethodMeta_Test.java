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
package org.apache.juneau.http.remote;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link RrpcInterfaceMethodMeta}.
 */
class RrpcInterfaceMethodMeta_Test extends TestBase {

	// Regression: getHeaderDefault/getQueryDefault/getFormDataDefault/getPathDefault(null) NPE'd because they
	// delegated straight into a null-hostile Map.of()-backed map, despite each documenting an "or null" @return.
	@Test void getXxxDefault_nullName() throws Exception {
		var m = Object.class.getMethod("toString");
		var meta = new RrpcInterfaceMethodMeta(m, "POST", "/x", RemoteReturn.BODY);

		assertNull(meta.getHeaderDefault(null));
		assertNull(meta.getQueryDefault(null));
		assertNull(meta.getFormDataDefault(null));
		assertNull(meta.getPathDefault(null));
	}
}
