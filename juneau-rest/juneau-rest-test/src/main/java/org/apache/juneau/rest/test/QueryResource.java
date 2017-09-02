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

import org.apache.juneau.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testQuery"
)
public class QueryResource extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Default values.
	//====================================================================================================

	@RestMethod(name="GET", path="/defaultQuery", defaultQuery={"f1:1","f2=2"," f3 : 3 "})
	public ObjectMap defaultQuery(RequestQuery query) {
		return new ObjectMap()
			.append("f1", query.getString("f1"))
			.append("f2", query.getString("f2"))
			.append("f3", query.getString("f3"));
	}

	@RestMethod(name="GET", path="/annotatedQuery")
	public ObjectMap annotatedQuery(@Query("f1") String f1, @Query("f2") String f2, @Query("f3") String f3) {
		return new ObjectMap()
			.append("f1", f1)
			.append("f2", f2)
			.append("f3", f3);
	}

	@RestMethod(name="GET", path="/annotatedQueryDefault")
	public ObjectMap annotatedQueryDefault(@Query(value="f1",def="1") String f1, @Query(value="f2",def="2") String f2, @Query(value="f3",def="3") String f3) {
		return new ObjectMap()
			.append("f1", f1)
			.append("f2", f2)
			.append("f3", f3);
	}

	@RestMethod(name="GET", path="/annotatedAndDefaultQuery", defaultQuery={"f1:1","f2=2"," f3 : 3 "})
	public ObjectMap annotatedAndDefaultQuery(@Query(value="f1",def="4") String f1, @Query(value="f2",def="5") String f2, @Query(value="f3",def="6") String f3) {
		return new ObjectMap()
			.append("f1", f1)
			.append("f2", f2)
			.append("f3", f3);
	}
}
