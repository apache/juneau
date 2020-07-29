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
package org.apache.juneau.rest.testutils;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock2.*;

@SuppressWarnings({})
public class TestUtils extends org.apache.juneau.testutils.TestUtils {

	/**
	 * Gets the swagger for the specified @Resource-annotated object.
	 * @param c
	 * @return
	 */
	public static Swagger getSwagger(Class<?> c) {
		try {
			RestContext rc = RestContext.create(c.newInstance()).build();
			RestRequest req = rc.createRequest(new RestCall(rc, new MockServletRequest(), null));
			RestInfoProvider ip = rc.getInfoProvider();
			return ip.getSwagger(req);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
