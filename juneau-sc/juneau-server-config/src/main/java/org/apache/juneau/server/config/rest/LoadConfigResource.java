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
package org.apache.juneau.server.config.rest;

import static org.apache.juneau.http.HttpMethodName.GET;

import org.apache.juneau.http.annotation.Path;
import org.apache.juneau.rest.RestServlet;
import org.apache.juneau.rest.annotation.RestMethod;
import org.apache.juneau.rest.annotation.RestResource;
import org.apache.juneau.server.config.repository.GetConfiguration;

@RestResource(path = "/configs/*")
public class LoadConfigResource extends RestServlet {

	private static final long serialVersionUID = 8247663789227304097L;

	@RestMethod(name = GET, path = "/{project}/*", consumes = "application/json", produces = "application/json")
	public String get(@Path("project") String project) {

		return "{'msg':'OK'}";
	}

	@RestMethod(name = GET, path = "/{project}/{branch}/*", consumes = "application/json", produces = "application/json")
	public String gets(@Path("project") String project, @Path("branch") String branch) {

		try {
			new GetConfiguration(project, branch).execute();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		

		return "{'msg':'OK'}";
	}

}
