/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.samples;

import static com.ibm.juno.core.html.HtmlDocSerializerProperties.*;

import java.util.*;

import javax.servlet.ServletException;

import com.ibm.juno.client.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.microservice.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;
import com.ibm.juno.server.labels.*;

/**
 * Sample resource that shows how to mirror query results from a Docker registry.
 */
@RestResource(
	path="/docker",
	label="Sample Docker resource",
	properties={
		@Property(name=HTMLDOC_links, value="{up:'$R{requestParentURI}',options:'?method=OPTIONS',source:'$R{servletParentURI}/source?classes=(com.ibm.juno.server.samples.AtomFeedResource)'}")
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
