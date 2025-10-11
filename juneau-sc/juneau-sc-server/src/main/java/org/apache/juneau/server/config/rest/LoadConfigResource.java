/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.server.config.rest;

import org.apache.juneau.http.annotation.Path;
import org.apache.juneau.json.JsonSerializer;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.server.config.repository.GetConfiguration;

@Rest(path="/configs/*")
@SuppressWarnings("javadoc")
public class LoadConfigResource extends RestServlet {

	private static final long serialVersionUID = 8247663789227304097L;

	@RestGet(path="/{project}/{branch}/*", produces="application/json")
	public String gets(@Path("project") String project, @Path("branch") String branch) throws Exception {
		JsonSerializer jsonSerializer = JsonSerializer.DEFAULT_READABLE;

		GetConfiguration config = new GetConfiguration(project, branch);
		config.execute();

		return jsonSerializer.serialize(config.get());
	}

}
