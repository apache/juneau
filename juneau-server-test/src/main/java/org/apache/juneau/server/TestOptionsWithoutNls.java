/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.server;

import org.apache.juneau.server.annotation.*;
import org.apache.juneau.server.labels.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testOptionsWithoutNls"
)
public class TestOptionsWithoutNls extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Should get to the options page without errors
	//====================================================================================================
	@RestMethod(name="OPTIONS", path="/testOptions/*")
	public ResourceOptions testOptions(RestRequest req) {
		return new ResourceOptions(this, req);
	}

	//====================================================================================================
	// Missing resource bundle should cause {!!x} string.
	//====================================================================================================
	@RestMethod(name="GET", path="/testMissingResourceBundle")
	public String test(RestRequest req) {
		return req.getMessage("bad", 1, 2, 3);
	}

}
