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

import org.apache.juneau.http.response.*;
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
 * Contents of files are retrieved from the request using {@link RestRequest#getStaticFiles()}.

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
 * <p class='bjava'>
 *  <ja>@HtmlDocConfig</ja>(
 * 		aside=<js>"$F{resources/MyAsideMessage.html, Oops not found!}"</js>
 * 	)
 * </p>
 *
 * <p>
 * Files of type HTML, XHTML, XML, JSON, Javascript, and CSS will be stripped of comments.
 * This allows you to place license headers in files without them being serialized to the output.
 *
 * <p>
 * This variable resolver requires that a {@link RestRequest} bean be available in the session bean store.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.SvlVariables">SVL Variables</a>
 * </ul>
 */
public class FileVar extends DefaultingVar {

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

		RestRequest req = session.getBean(RestRequest.class).orElseThrow(InternalServerError::new);

		String s = req.getStaticFiles().getString(key, null).orElse(null);
		if (s == null)
			return null;
		String subType = FileUtils.getExtension(key);
		if ("html".equals(subType) || "xhtml".equals(subType) || "xml".equals(subType))
			s = s.replaceAll("(?s)<!--(.*?)-->\\s*", "");
		else if ("json".equals(subType) || "javascript".equals(subType) || "css".equals(subType))
			s = s.replaceAll("(?s)\\/\\*(.*?)\\*\\/\\s*", "");
		return s;
	}

	@Override /* Var */
	public boolean canResolve(VarResolverSession session) {
		return session.getBean(RestRequest.class).isPresent();
	}
}