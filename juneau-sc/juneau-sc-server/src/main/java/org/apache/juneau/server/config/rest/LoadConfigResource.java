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

import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.server.config.repository.*;

/**
 * REST resource for loading configuration from a Git repository.
 *
 * <p>
 * Fetches configuration files for a given project and branch,
 * combining application defaults with project-specific overrides.
 */
@Rest(path = "/configs/*", guards = ConfigApiKeyGuard.class)
public class LoadConfigResource extends RestServlet {

	private static final long serialVersionUID = 8247663789227304097L;

	/**
	 * Returns configuration for the specified project and branch as JSON.
	 *
	 * @param project Project name.
	 * @param branch Branch name.
	 * @return JSON-serialized configuration map.
	 * @throws Exception If configuration cannot be loaded.
	 */
	@RestGet(path = "/{project}/{branch}/*", produces = "application/json")
	public String gets(@Path("project") String project, @Path("branch") String branch) throws Exception {
		var jsonSerializer = JsonSerializer.DEFAULT_READABLE;

		GetConfiguration config;
		try {
			config = new GetConfiguration(project, branch);
		} catch (@SuppressWarnings("unused") IllegalArgumentException e) {
			// Malformed project/branch value (empty, '..', backslash, NUL, or '/' in project).
			throw new BadRequest("Invalid project or branch.");
		}
		try {
			config.execute();
		} catch (@SuppressWarnings("unused") IllegalArgumentException e) {
			// A crafted project/branch that escapes the checkout root is rejected by
			// FileUtils.resolveSafely(). Map that boundary violation to 403 with a generic
			// message so the offending path is never echoed back to the client.
			throw new Forbidden("Access denied.");
		}

		return jsonSerializer.write(config.get());
	}
}