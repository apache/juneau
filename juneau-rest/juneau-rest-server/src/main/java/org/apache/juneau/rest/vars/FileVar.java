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
package org.apache.juneau.rest.vars;

import org.apache.juneau.rest.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.utils.*;

/**
 * File resource variable resolver
 *
 * <p>
 * The format for this var is <js>"$F{path[,defaultValue]}"</js>.
 *
 * <p>
 * File variables resolve to the contents of resource files located on the classpath or local JVM directory.
 * They use the {@link RestRequest#getClasspathReaderResource(String)} method to retrieve the contents of the file.
 * That in turn uses the {@link ClasspathResourceFinder} associated with the servlet class to find the file.
 *
 * <p>
 * The {@link ClasspathResourceFinder} is similar to {@link Class#getResourceAsStream(String)} except if it doesn't find the
 * resource on this class, it searches up the parent hierarchy chain.
 *
 * <p>
 * If the resource cannot be found in the classpath, then an attempt is made to look in the JVM working directory.
 * <br>Path traversals outside the working directory are not allowed for security reasons.

 * <p>
 * Localized resources (based on the locale of the HTTP request) are supported.
 * For example, if looking for the resource <js>"MyResource.txt"</js> for the Japanese locale, we will look for
 * files in the following order:
 * <ol>
 * 	<li><js>"MyResource_ja_JP.txt"</js>
 * 	<li><js>"MyResource_ja.txt"</js>
 * 	<li><js>"MyResource.txt"</js>
 * </ol>
 *
 * <p>
 * Example:
 * <p class='bcode'>
 * 	<ja>@RestResource</ja>(
 * 		htmldoc=<ja>@HtmlDoc</ja>(
 * 			aside=<js>"$F{resources/MyAsideMessage.html, Oops not found!}"</js>
 * 		)
 * 	)
 * </p>
 *
 * <p>
 * Files of type HTML, XHTML, XML, JSON, Javascript, and CSS will be stripped of comments.
 * This allows you to place license headers in files without them being serialized to the output.
 *
 * @see org.apache.juneau.svl
 */
public class FileVar extends DefaultingVar {

	private static final String SESSION_req = "req";

	/**
	 * The name of this variable.
	 */
	public static final String NAME = "F";

	/**
	 * Constructor.
	 */
	public FileVar() {
		super(NAME);
	}

	@Override /* Parameter */
	public String resolve(VarResolverSession session, String key) throws Exception {
		RestRequest req = session.getSessionObject(RestRequest.class, SESSION_req);
		ReaderResource rr = req.getClasspathReaderResource(key);
		return (rr == null ? null : rr.toCommentStrippedString());
	}
}