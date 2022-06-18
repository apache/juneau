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

import static org.apache.juneau.http.HttpMethod.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.testutils.pojos.*;

/**
 * JUnit automated testcase resource.
 */
@Rest(
	path="/testLargePojos"
)
public class LargePojosResource extends BasicRestServlet implements BasicUniversalJenaConfig {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Test how long it takes to serialize/parse various content types.
	//====================================================================================================
	@RestOp(method=GET, path="/")
	public LargePojo testGet() {
		return LargePojo.get();
	}

	@RestOp(method=PUT, path="/")
	public String testPut(@Content LargePojo in) {
		return "ok";
	}
}
