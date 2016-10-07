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

import java.io.*;

import org.apache.juneau.microservice.*;
import org.apache.juneau.server.*;
import org.apache.juneau.server.annotation.*;

/**
 * Service at <code>/codeFormatter</code>.
 * Used for executing SQL queries against the repository database.
 */
@RestResource(
	path="/codeFormatter",
	messages="nls/CodeFormatterResource",
	properties={
		@Property(name=HTMLDOC_title, value="Code Formatter"),
		@Property(name=HTMLDOC_description, value="Add syntax highlighting tags to source code"),
		@Property(name=HTMLDOC_links, value="{options:'?method=OPTIONS',source:'$R{servletParentURI}/source?classes=(org.apache.juneau.server.samples.CodeFormatterResource)'}"),
	}
)
@SuppressWarnings("serial")
public class CodeFormatterResource extends Resource {

	/** [GET /] - Display query entry page. */
	@RestMethod(name="GET", path="/")
	public ReaderResource getQueryEntryPage(RestRequest req) throws IOException {
		return req.getReaderResource("CodeFormatterResource.html", true);
	}

	/** [POST /] - Execute SQL query. */
	@RestMethod(name="POST", path="/")
	public String executeQuery(@FormData("code") String code, @FormData("lang") String lang) throws Exception {
		return SourceResource.highlight(code, lang);
	}
}
