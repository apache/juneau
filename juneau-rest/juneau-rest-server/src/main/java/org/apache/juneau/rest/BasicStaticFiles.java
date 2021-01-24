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
package org.apache.juneau.rest;

import org.apache.juneau.http.header.*;

/**
 * Basic implementation of resource finder used for finding static files served up for REST resources.
 *
 * <p>
 * This implementation has the following attributes:
 * <ul>
 * 	<li>Looks for files in the following locations:
 * 		<li><js>"static"</js> working subdirectory.
 * 		<li><js>"htdocs"</js> working subdirectory.
 * 		<li><js>"htdocs"</js> subpackage from this class and all parent classes.
 * 		<li><js>"htdocs"</js> root package from this class and all parent classes.
 * 	</li>
 * 	<li>Caches any files smaller than 1MB into memory.
 * 	<li>Ignores any <js>".class"</js> or <js>".properties"</js> files found.
 * 	<li>Adds header <js>"Cache-Control: max-age=86400, public"</js> to all resources.
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@link RestContext#REST_staticFiles}
 * </ul>
 */
public class BasicStaticFiles extends StaticFiles {

	/**
	 * Constructor.
	 *
	 * @param context The context of the REST resource this finder belongs to.
	 */
	public BasicStaticFiles(RestContext context) {
		super(StaticFiles
			.create()
			.dir("static")
			.dir("htdocs")
			.cp(context.getResourceClass(), "htdocs", true)
			.cp(context.getResourceClass(), "/htdocs", true)
			.caching(1_000_000)
			.exclude("(?i).*\\.(class|properties)")
			.headers(CacheControl.of("max-age=86400, public"))
		);
	}
}
