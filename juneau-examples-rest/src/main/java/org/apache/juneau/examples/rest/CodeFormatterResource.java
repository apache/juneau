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

import static org.apache.juneau.html.HtmlDocSerializerContext.*;

import java.io.*;

import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

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
		@Property(name=HTMLDOC_links, value="{options:'?method=OPTIONS',source:'$C{Source/gitHub}/org/apache/juneau/examples/rest/CodeFormatterResource.java'}"),
	}
)
@SuppressWarnings({"serial"})
public class CodeFormatterResource extends Resource {

	/** [GET /] - Display query entry page. */
	@RestMethod(name="GET", path="/")
	public ReaderResource getQueryEntryPage(RestRequest req) throws IOException {
		return req.getReaderResource("CodeFormatterResource.html", true);
	}

	/** [POST /] - Add syntax highlighting to input. */
	@RestMethod(name="POST", path="/")
	public String executeQuery(@FormData("code") String code, @FormData("lang") String lang) throws Exception {
		return highlight(code, lang);
	}

	private static String highlight(String code, String lang) throws Exception {
		if (lang.equalsIgnoreCase("xml")) {
			code = code.replaceAll("&", "&amp;");
			code = code.replaceAll("<", "&lt;");
			code = code.replaceAll(">", "&gt;");
			code = code.replaceAll("(&lt;[^\\s&]+&gt;)", "<xt>$1</xt>");
			code = code.replaceAll("(&lt;[^\\s&]+)(\\s)", "<xt>$1</xt>$2");
			code = code.replaceAll("(['\"])(/?&gt;)", "$1<xt>$2</xt>");
			code = code.replaceAll("([\\S]+)=", "<xa>$1</xa>=");
			code = code.replaceAll("=(['\"][^'\"]+['\"])", "=<xs>$1</xs>");
		} else if (lang.equalsIgnoreCase("java")) {
			code = code.replaceAll("&", "&amp;");
			code = code.replaceAll("<", "&lt;");
			code = code.replaceAll(">", "&gt;");
			code = code.replaceAll("(?s)(\\/\\*\\*.*?\\*\\/)", "<jd>$1</jd>"); // javadoc comments
			code = code.replaceAll("(@\\w+)", "<ja>$1</ja>"); // annotations
			code = code.replaceAll("(?s)(?!\\/)(\\/\\*.*?\\*\\/)", "<jc>$1</jc>"); // C style comments
			code = code.replaceAll("(?m)(\\/\\/.*)", "<jc>$1</jc>"); // C++ style comments
			code = code.replaceAll("(?m)('[^'\n]*'|\"[^\"\n]*\")", "<js>$1</js>"); // quotes
			code = code.replaceAll("(?<!@)(import|package|boolean|byte|char|double|float|final|static|transient|synchronized|private|protected|public|int|long|short|abstract|class|interface|extends|implements|null|true|false|void|break|case|catch|continue|default|do|else|finally|for|goto|if|instanceof|native|new|return|super|switch|this|threadsafe|throws|throw|try|while)(?=\\W)", "<jk>$1</jk>"); // quotes
			code = code.replaceAll("<\\/jk>(\\s+)<jk>", "$1"); // quotes
		}
		return code;
	}
}
