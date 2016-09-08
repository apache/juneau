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
package org.apache.juneau.server.samples;

import static org.apache.juneau.html.HtmlDocSerializerContext.*;

import java.util.*;

import javax.servlet.*;

import org.apache.juneau.client.*;
import org.apache.juneau.json.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.server.*;
import org.apache.juneau.server.annotation.*;
import org.apache.juneau.server.labels.*;

/**
 * Sample resource that shows how to mirror query results from a Docker registry.
 */
@RestResource(
	path="/docker",
	label="Sample Docker resource",
	properties={
		@Property(name=HTMLDOC_links, value="{up:'$R{requestParentURI}',options:'?method=OPTIONS',source:'$R{servletParentURI}/source?classes=(org.apache.juneau.server.samples.AtomFeedResource)'}")
	}
)
public class DockerRegistryResource extends Resource {
	private static final long serialVersionUID = 1L;

	// Get registry URL from samples.cfg file.
	private String registryUrl = getConfig().getString("DockerRegistry/url");

	RestClient rc;

	@Override /* Servlet */
	public void init() throws ServletException {
		super.init();
		rc = new RestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
	}

	@Override /* Servlet */
	public void destroy() {
		rc.closeQuietly();
		super.destroy();
	}

	/** [GET /] - Show child resources. */
	@SuppressWarnings("nls")
	@RestMethod(name="GET", path="/")
	public ResourceDescription[] getChildren(RestRequest req) {
		return new ResourceDescription[] {
			new ResourceDescription(req, "search", "Search Registry")
		};
	}

	/**
	 * PUT request handler.
	 * Replaces the feed with the specified content, and then mirrors it as the response.
	 */
	@RestMethod(name="GET", path="/search")
	public QueryResults query(@Param("q") String q) throws Exception {
		String url = registryUrl + "/search" + (q == null ? "" : "?q=" + q);
		synchronized(rc) {
			return rc.doGet(url).getResponse(QueryResults.class);
		}
	}

	public static class QueryResults {
		public int num_results;
		public String query;
		public List<DockerImage> results;
	}

	public static class DockerImage {
		public String name, description;
	}
}
