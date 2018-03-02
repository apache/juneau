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
package org.apache.juneau.rest.test;

import static org.apache.juneau.http.HttpMethodName.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.utils.*;

/**
 * JUnit automated testcase resource.
 * Tests the <code>@RestMethod.path()</code> annotation.
 */
@RestResource(
	path="/testPathVariables"
)
public class PathVariablesResource extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	@RestMethod(name=GET, path="/test1/{x}/foo/{y}/bar/{z}/*")
	public StringMessage test1(@Path String x, @Path int y, @Path boolean z) {
		return new StringMessage("x={0},y={1},z={2}", x, y, z);
	}

	@RestMethod(name=GET, path="/test2/{z}/foo/{y}/bar/{x}/*")
	public StringMessage test2(@Path("x") String x, @Path("y") int y, @Path("z") boolean z) {
		return new StringMessage("x={0},y={1},z={2}", x, y, z);
	}

	@RestMethod(name=GET, path="/test3/{0}/foo/{1}/bar/{2}/*")
	public StringMessage test3(@Path String x, @Path int y, @Path boolean z) {
		return new StringMessage("x={0},y={1},z={2}", x, y, z);
	}

	@RestMethod(name=GET, path="/test4/{2}/foo/{1}/bar/{0}/*")
	public StringMessage test4(@Path String x, @Path int y, @Path boolean z) {
		return new StringMessage("x={0},y={1},z={2}", x, y, z);
	}
}
