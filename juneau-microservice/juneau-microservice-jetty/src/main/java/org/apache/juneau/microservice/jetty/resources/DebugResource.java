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
package org.apache.juneau.microservice.jetty.resources;

import java.io.*;

import org.apache.juneau.html.annotation.HtmlDocConfig;
import org.apache.juneau.http.response.*;
import org.apache.juneau.microservice.jetty.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.beans.*;
import org.apache.juneau.rest.servlet.BasicRestServlet;

/**
 * Microservice debug utilities.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../../index.html#juneau-microservice-jetty">juneau-microservice-jetty</a>
 * </ul>
 *
 * @serial exclude
 */
@Rest(
	path="/debug",
	title="Debug",
	description="Debug Utilities.",
	allowedMethodParams="OPTIONS,POST"
)
@HtmlDocConfig(
	navlinks={
		"up: request:/..",
		"jetty-thread-dump: servlet:/jetty/dump?method=POST",
		"api: servlet:/api",
		"stats: servlet:/stats"
	}
)
public class DebugResource extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * [GET /] - Shows child utilities.
	 *
	 * @return Child utility links.
	 */
	@RestGet(path="/", description="Show contents of config file.")
	public ResourceDescriptions getChildren() {
		return ResourceDescriptions
			.create()
			.append("jetty/dump", "Jetty thread dump")
		;
	}

	/**
	 * [GET /jetty/dump] - Generates and retrieves the jetty thread dump.
	 *
	 * @param req The request.
	 * @param res The response.
	 * @return The thread dump contents.
	 */
	@RestGet(path="/jetty/dump", description="Generates and retrieves the jetty thread dump.")
	public Reader getJettyDump(RestRequest req, RestResponse res) {
		res.setContentType("text/plain");
		return new StringReader(JettyMicroservice.getInstance().getServer().dump());
	}

	/**
	 * [POST /jetty/dump] - Generates and saves the jetty thread dump file to jetty-thread-dump.log.
	 *
	 * @param req The request.
	 * @param res The response.
	 * @throws Exception Gets converted to 500 response.
	 * @return The thread dump contents.
	 */
	@RestPost(path="/jetty/dump", description="Generates and saves the jetty thread dump file to jetty-thread-dump.log.")
	public Ok createJettyDump(RestRequest req, RestResponse res) throws Exception {
		String dump = JettyMicroservice.getInstance().getServer().dump();
		try (FileWriter fw = new FileWriter(req.getConfig().get("Logging/logDir").orElse("") + "/jetty-thread-dump.log")) {
			fw.write(dump);
		}
		return Ok.OK;
	}
}
