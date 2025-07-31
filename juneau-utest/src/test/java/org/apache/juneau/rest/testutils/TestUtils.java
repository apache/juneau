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

import org.apache.juneau.bean.swagger.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.swagger.*;

@SuppressWarnings({})
public class TestUtils extends org.apache.juneau.utest.utils.Utils2 {

	/**
	 * Gets the swagger for the specified @Resource-annotated object.
	 * @param c
	 * @return
	 */
	public static Swagger getSwagger(Class<?> c) {
		try {
			Object r = c.getDeclaredConstructor().newInstance();
			RestContext rc = RestContext.create(r.getClass(),null,null).init(()->r).build();
			RestOpContext ctx = RestOpContext.create(TestUtils.class.getMethod("getSwagger", Class.class), rc).build();
			RestSession session = RestSession.create(rc).resource(r).req(new MockServletRequest()).res(new MockServletResponse()).build();
			RestRequest req = ctx.createRequest(session);
			SwaggerProvider ip = rc.getSwaggerProvider();
			return ip.getSwagger(rc, req.getLocale());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}