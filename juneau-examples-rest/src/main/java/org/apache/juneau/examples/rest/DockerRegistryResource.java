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
package org.apache.juneau.examples.rest;

import java.util.*;

import org.apache.juneau.ini.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.labels.*;

/**
 * Sample resource that shows how to mirror query results from a Docker registry.
 */
@RestResource(
	path="/docker",
	title="Sample Docker resource",
	htmldoc=@HtmlDoc(
		links={
			"up: request:/..",
			"options: servlet:/?method=OPTIONS",
			"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/DockerRegistryResource.java"
		},
		aside={
			"<div style='min-width:200px' class='text'>",
			"	<p>REST API for searching Docker registries.</p>",
			"	<p>To use, you must first specify the Docker registry URL in the <code>[Docker]</code> section of the config file.</p>",
			"</div>"
		}
	)
)
public class DockerRegistryResource extends Resource {
	private static final long serialVersionUID = 1L;

	// Get registry URL from examples.cfg file.
	private String registryUrl;

	RestClient rc;

	@Override /* Servlet */
	public synchronized void init(RestConfig servletConfig) throws Exception {
		super.init(servletConfig);
		ConfigFile cf = servletConfig.getConfigFile();
		registryUrl = cf.getString("DockerRegistry/url");
		rc = new RestClientBuilder().build();
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
			new ResourceDescription("search", "Search Registry")
		};
	}

	/**
	 * PUT request handler.
	 * Replaces the feed with the specified content, and then mirrors it as the response.
	 */
	@RestMethod(name="GET", path="/search")
	public QueryResults query(@Query("q") String q) throws Exception {
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
