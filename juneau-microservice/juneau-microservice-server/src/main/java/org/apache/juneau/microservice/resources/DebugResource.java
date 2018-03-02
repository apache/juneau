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
package org.apache.juneau.microservice.resources;

import static org.apache.juneau.http.HttpMethodName.*;

import java.io.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.labels.*;

/**
 * Microservice debug utilities.
 */
@RestResource(
	path="/debug",
	title="Debug",
	description="Debug Utilities.",
	htmldoc=@HtmlDoc(
		navlinks={
			"up: request:/..",
			"jetty-thread-dump: servlet:/jetty/dump?method=POST",
			"options: servlet:/?method=OPTIONS"
		}
	),
	allowedMethodParams="OPTIONS,POST"
)
@SuppressWarnings("javadoc")
public class DebugResource extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * [GET /] - Shows child utilities.
	 * 
	 * @return Child utility links.
	 * @throws Exception 
	 */
	@RestMethod(name=GET, path="/", description="Show contents of config file.")
	public ResourceDescription[] getChildren() throws Exception {
		return new ResourceDescription[] {
			new ResourceDescription("jetty/dump", "Jetty thread dump")
		};
	}

	/**
	 * [GET /jetty/dump] - Generates and retrieves the jetty thread dump.
	 */
	@RestMethod(name=GET, path="/jetty/dump", description="Generates and retrieves the jetty thread dump.")
	public Reader getJettyDump(RestRequest req, RestResponse res) {
		res.setContentType("text/plain");
		return new StringReader(RestMicroservice.getInstance().getServer().dump());
	}

	/**
	 * [POST /jetty/dump] - Generates and saves the jetty thread dump file to jetty-thread-dump.log.
	 */
	@RestMethod(name=POST, path="/jetty/dump", description="Generates and saves the jetty thread dump file to jetty-thread-dump.log.")
	public String createJettyDump(RestRequest req, RestResponse res) throws Exception {
		String dump = RestMicroservice.getInstance().getServer().dump();
		try (FileWriter fw = new FileWriter(req.getConfig().getString("Logging/logDir") + "/jetty-thread-dump.log")) {
			IOUtils.pipe(dump, fw);
		}
		return "OK";
	}
}
