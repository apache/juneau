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

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * JUnit automated testcase resource.
 * Tests the RestServlet.getPath() method.
 */
@RestResource(
	path="/testPath",
	children={
		PathResource.TestPath2.class
	}
)
public class PathResource extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Basic tests
	//====================================================================================================
	@RestMethod(name="GET", path="/")
	public String doGet() {
		return getPath();
	}

	@RestResource(
		path="/testPath2",
		children={
			PathResource.TestPath3.class
		}
	)
	public static class TestPath2 extends RestServletDefault {
		private static final long serialVersionUID = 1L;
		// Basic tests
		@RestMethod(name="GET", path="/")
		public String doGet() {
			return getPath();
		}
	}

	@RestResource(
		path="/testPath3"
	)
	public static class TestPath3a extends RestServletDefault {
		private static final long serialVersionUID = 1L;
		// Basic tests
		@RestMethod(name="GET", path="/")
		public String doGet() {
			return getPath();
		}
	}

	public static class TestPath3 extends TestPath3a {
		private static final long serialVersionUID = 1L;
	}
}
