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

import static org.apache.juneau.dto.html5.HtmlBuilder.*;
import static org.apache.juneau.http.HttpMethodName.*;

import org.apache.juneau.dto.html5.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Service at <code>/codeFormatter</code>.
 * Used for executing SQL queries against the repository database.
 */
@RestResource(
	path="/codeFormatter",
	messages="nls/CodeFormatterResource",
	title="Code Formatter",
	description="Utility for generating HTML code-formatted source code",
	htmldoc=@HtmlDoc(
		navlinks={
			"up: servlet:/..",
			"options: servlet:/?method=OPTIONS",
			"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/$R{servletClassSimple}.java"
		},
		aside={
			"<div style='min-width:200px' class='text'>",
			"	<p>Utility for adding code syntax tags to Java and XML/HTML code.</p>",
			"	<p>It's by no means perfect, but provides a good starting point.</p>",
			"</div>"
		},
		style="aside {display:table-caption;}"
	),
	swagger=@ResourceSwagger(
		contact=@Contact(name="Juneau Developer",email="dev@juneau.apache.org"),
		license=@License(name="Apache 2.0",url="http://www.apache.org/licenses/LICENSE-2.0.html"),
		version="2.0",
		termsOfService="You are on your own.",
		externalDocs=@ExternalDocs(description="Apache Juneau",url="http://juneau.apache.org")
	)
)
@SuppressWarnings({"serial"})
public class CodeFormatterResource extends BasicRestServlet {

	/** [GET /] - Display query entry page. */
	@RestMethod
	public Div get(RestRequest req) {
		return div(
			script("text/javascript",
				new String[]{
				"\n	// Quick and dirty function to allow tabs in textarea."
				+"\n	function checkTab(e) {"
				+"\n		if (e.keyCode == 9) {"
				+"\n			var t = e.target;"
				+"\n			var ss = t.selectionStart, se = t.selectionEnd;"
				+"\n			t.value = t.value.slice(0,ss).concat('\\t').concat(t.value.slice(ss,t.value.length));"
				+"\n			e.preventDefault();"
				+"\n		}"
				+"\n	}"
				+"\n	// Load results from IFrame into this document."
				+"\n	function loadResults(b) {"
				+"\n		var doc = b.contentDocument || b.contentWindow.document;"
				+"\n		var data = doc.getElementById('data') || doc.getElementsByTagName('body')[0];"
				+"\n		document.getElementById('results').innerHTML = data.innerHTML;"
				+"\n	}"}
			),
			form("form").action("codeFormatter").method(POST).target("buff").children(
				table(
					tr(
						th("Language: "),
						td(
							select().name("lang").children(
								option("java","Java"),
								option("xml", "XML")
							)
						),
						td(button("submit", "Submit"), button("reset", "Reset"))
					),
					tr(
						td().colspan(3).children(
							textarea().name("code").style("min-width:800px;min-height:400px;font-family:Courier;font-size:9pt;").onkeydown("checkTab(event)")
						)
					)
				)
			),
			br(),
			div().id("results")._class("monospace"),
			iframe().name("buff").style("display:none").onload("parent.loadResults(this)")
		);
	}

	/** [POST /] - Add syntax highlighting to input. */
	@RestMethod
	public String post(@FormData("code") String code, @FormData("lang") String lang) throws Exception {
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
