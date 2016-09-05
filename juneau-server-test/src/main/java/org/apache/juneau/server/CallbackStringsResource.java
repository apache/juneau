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
package org.apache.juneau.server;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.server.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testCallback"
)
public class CallbackStringsResource extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Test GET
	//====================================================================================================
	@RestMethod(name="GET", path="/")
	public ObjectMap test1(RestRequest req) throws Exception {
		return new ObjectMap().append("method","GET").append("headers", getFooHeaders(req)).append("content", req.getInputAsString());
	}

	//====================================================================================================
	// Test PUT
	//====================================================================================================
	@RestMethod(name="PUT", path="/")
	public ObjectMap testCharsetOnResponse(RestRequest req) throws Exception {
		return new ObjectMap().append("method","PUT").append("headers", getFooHeaders(req)).append("content", req.getInputAsString());
	}

	private Map<String,Object> getFooHeaders(RestRequest req) {
		Map<String,Object> m = new TreeMap<String,Object>();
		for (Map.Entry<String,Object> e : req.getHeaders().entrySet())
			if (e.getKey().startsWith("Foo-"))
				m.put(e.getKey(), e.getValue());
		return m;
	}
}
