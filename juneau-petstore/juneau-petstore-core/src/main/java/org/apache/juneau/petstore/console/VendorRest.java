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
package org.apache.juneau.petstore.console;

import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.staticfile.*;
import org.apache.juneau.commons.inject.*;

/**
 * Serves the third-party browser libraries the console pages need (jQuery and DataTables) from the WebJars on the
 * petstore classpath, at {@code /console/vendor/*}.
 *
 * <p>
 * The views toolkit binds datatables cards to jQuery and DataTables but does not bundle them, so the pages that have
 * such a card load them from here.  The URL path is the WebJar's own layout, for example
 * {@code /console/vendor/jquery/3.7.1/jquery.min.js} and {@code /console/vendor/datatables.net/js/dataTables.min.js}.
 */
@Rest(path="/vendor", staticFiles=VendorRest.WebJars.class)
public class VendorRest extends BasicRestServlet {

	private static final long serialVersionUID = 1L;

	/** The classpath root every WebJar publishes its files under. */
	private static final String WEBJARS = "/META-INF/resources/webjars";

	/** The static files backing {@link VendorRest}: only the WebJar root, so nothing else on the classpath is exposed. */
	public static class WebJars extends BasicStaticFiles {

		/**
		 * Constructor.
		 *
		 * @param beanStore The bean store.
		 */
		public WebJars(BeanStore beanStore) {
			super(StaticFiles.create(beanStore).cp(VendorRest.class, WEBJARS, false).caching(1_000_000).exclude("(?i).*\\.(class|properties)"));
		}
	}

	/**
	 * [GET /vendor/*] &mdash; a file from a WebJar.
	 *
	 * @param req The current request.
	 * @param path The file path within the WebJar root.
	 * @param locale The request locale.
	 * @return The file.
	 */
	@RestGet(path="/*", summary="WebJar file", swagger=@OpSwagger(ignore=true))
	public HttpResource get(RestRequest req, @Path("/*") String path, Locale locale) {
		return StaticFilesMixin.resolveStaticFile(req, path, locale);
	}
}
