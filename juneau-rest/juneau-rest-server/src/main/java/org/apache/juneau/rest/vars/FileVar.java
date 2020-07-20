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

import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.svl.*;

/**
 * File resource variable resolver
 *
 * <p>
 * The format for this var is <js>"$F{path[,defaultValue]}"</js>.
 *
 * <p>
 * File variables resolve to the contents of resource files located on the classpath or local JVM directory.
 * They use the {@link RestRequest#getClasspathResourceAsString(String)} method to retrieve the contents of the file.
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
 * <p class='bcode w800'>
 *  <ja>@HtmlDocConfig</ja>(
 * 		aside=<js>"$F{resources/MyAsideMessage.html, Oops not found!}"</js>
 * 	)
 * </p>
 *
 * <p>
 * Files of type HTML, XHTML, XML, JSON, Javascript, and CSS will be stripped of comments.
 * This allows you to place license headers in files without them being serialized to the output.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-marshall.SimpleVariableLanguage.SvlVariables}
 * </ul>
 */
public class FileVar extends DefaultingVar {

	private static final String SESSION_req = "req";
	private static final String SESSION_crm = "crm";

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

	@Override /* Var */
	public String resolve(VarResolverSession session, String key) throws Exception {

		RestRequest req = session.getSessionObject(RestRequest.class, SESSION_req, false);
		if (req != null) {
			String s = req.getClasspathResourceAsString(key);
			if (s == null)
				return null;
			String subType = FileUtils.getExtension(key);
			if ("html".equals(subType) || "xhtml".equals(subType) || "xml".equals(subType))
				s = s.replaceAll("(?s)<!--(.*?)-->\\s*", "");
			else if ("json".equals(subType) || "javascript".equals(subType) || "css".equals(subType))
				s = s.replaceAll("(?s)\\/\\*(.*?)\\*\\/\\s*", "");
			return s;
		}

		ClasspathResourceManager crm = session.getSessionObject(ClasspathResourceManager.class, SESSION_crm, false);
		if (crm != null)
			return crm.getString(key);

		return null;
	}

	@Override /* Var */
	public boolean canResolve(VarResolverSession session) {
		return session.hasSessionObject(SESSION_req) || session.hasSessionObject(SESSION_crm);
	}
}