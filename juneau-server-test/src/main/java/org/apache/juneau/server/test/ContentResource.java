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
package org.apache.juneau.server.test;

import static org.apache.juneau.server.RestServletContext.*;

import java.util.*;

import org.apache.juneau.server.*;
import org.apache.juneau.server.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testContent",
	properties={
		@Property(name=REST_allowMethodParam, value="*")
	}
)
public class ContentResource extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Basic tests
	//====================================================================================================
	@RestMethod(name="POST", path="/boolean")
	public boolean testBool(@Body boolean b) {
		return b;
	}

	@RestMethod(name="POST", path="/Boolean")
	public Boolean testBoolean(@Body Boolean b) {
		return b;
	}

	@RestMethod(name="POST", path="/int")
	public int testInt(@Body int i) {
		return i;
	}

	@RestMethod(name="POST", path="/Integer")
	public Integer testInteger(@Body Integer i) {
		return i;
	}

	@RestMethod(name="POST", path="/float")
	public float testFloat(@Body float f) {
		return f;
	}

	@RestMethod(name="POST", path="/Float")
	public Float testFloat2(@Body Float f) {
		return f;
	}

	@RestMethod(name="POST", path="/Map")
	public TreeMap<String,String> testMap(@Body TreeMap<String,String> m) {
		return m;
	}

	@RestMethod(name="POST", path="/B")
	public DTO2s.B testPojo1(@Body DTO2s.B b) {
		return b;
	}

	@RestMethod(name="POST", path="/C")
	public DTO2s.C testPojo2(@Body DTO2s.C c) {
		return c;
	}
}
