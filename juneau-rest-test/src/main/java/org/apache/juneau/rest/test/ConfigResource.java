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

import org.apache.juneau.ini.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testConfig"
)
@SuppressWarnings("serial")
public class ConfigResource extends Resource {

	@RestMethod(name="GET", path="/")
	public ConfigFile test1(RestRequest req) {
		return req.getConfig();
	}

	@RestMethod(name="GET", path="/{key}/{class}")
	public Object test2(RestRequest req, @Path("key") String key, @Path("class") Class<?> c) throws Exception {
		return req.getConfig().getObject(c, key);
	}
}
