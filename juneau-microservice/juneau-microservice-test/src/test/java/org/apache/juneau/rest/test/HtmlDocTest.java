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
package org.apache.juneau.rest.test;

import static org.junit.Assert.*;

import org.apache.juneau.rest.client.*;
import org.junit.*;

/**
 * Validates inheritance on the @HtmlDoc annotation.
 */
public class HtmlDocTest extends RestTestcase {
	private RestClient client = TestMicroservice.DEFAULT_CLIENT;

	private String get(String uri) throws Exception {
		return client.doGet(uri).accept("text/html").getResponseAsString().replace('\n', ' ').replace('"', '\'');
	}
	private String header(String r) {
		return r.substring(r.indexOf("<header>")+8, r.indexOf("</header>"));
	}
	private String script(String r) {
		return r.substring(r.indexOf("<script>")+8, r.indexOf("</script>"));
	}
	private String style(String r) {
		return r.substring(r.indexOf("<style>")+7, r.indexOf("</style>"));
	}
	private String nav(String r) {
		return r.substring(r.indexOf("<nav>")+5, r.indexOf("</nav>"));
	}
	private String aside(String r) {
		return r.substring(r.indexOf("<aside>")+7, r.indexOf("</aside>"));
	}
	private String footer(String r) {
		return r.substring(r.indexOf("<footer>")+8, r.indexOf("</footer>"));
	}

	/**
	 * @RestResource(
	 * 	path="/testHtmlDoc",
	 * 	htmldoc=@HtmlDoc(
	 * 		aside={"aside1a","aside1b","INHERIT"},
	 * 		footer={"footer1a","footer1b"},
	 * 		header={"header1a","header1b"},
	 * 		nav={"nav1a","nav1b"},
	 * 		script={"script1a","script1b"},
	 * 		style={"style1a","style1b"},
	 * 		stylesheet="stylesheet1"
	 * 	)
	 * )
	 */
	@Test
	public void test1() throws Exception {
		String r = get("/testHtmlDoc/test1");
		assertEquals("header1a header1b", header(r));
		assertEquals("script1a script1b", script(r));
		assertEquals("@import '/testHtmlDoc/stylesheet1'; style1a style1b", style(r));
		assertEquals("nav1a nav1b", nav(r));
		assertEquals("aside1a aside1b", aside(r));
		assertEquals("footer1a footer1b", footer(r));
	}

	/**
	 * @RestMethod(
	 * 	path="/test2",
	 * 	htmldoc=@HtmlDoc(
	 * 		aside={"aside2a","aside2b"},
	 * 		footer={"footer2a","footer2b"},
	 * 		header={"header2a","header2b"},
	 * 		nav={"nav2a","nav2b"},
	 * 		script={"script2a","script2b"},
	 * 		style={"style2a","style2b"},
	 * 		stylesheet="stylesheet2"
	 * 	)
	 * )
	 */
	@Test
	public void test2() throws Exception {
		String r = get("/testHtmlDoc/test2");
		assertEquals("header2a header2b", header(r));
		assertEquals("script2a script2b", script(r));
		assertEquals("@import '/testHtmlDoc/stylesheet2'; style2a style2b", style(r));
		assertEquals("nav2a nav2b", nav(r));
		assertEquals("aside2a aside2b", aside(r));
		assertEquals("footer2a footer2b", footer(r));
	}

	/**
	 * @RestMethod(
	 * 	path="/test3",
	 * 	htmldoc=@HtmlDoc(
	 * 		aside={"INHERIT","aside3a","aside3b"},
	 * 		footer={"INHERIT","footer3a","footer3b"},
	 * 		header={"INHERIT","header3a","header3b"},
	 * 		nav={"INHERIT","nav3a","nav3b"},
	 * 		script={"INHERIT","script3a","script3b"},
	 * 		style={"INHERIT","style3a","style3b"}
	 * 	)
	 * )
	 */
	@Test
	public void test3() throws Exception {
		String r = get("/testHtmlDoc/test3");
		assertEquals("header1a header1b header3a header3b", header(r));
		assertEquals("script1a script1b script3a script3b", script(r));
		assertEquals("@import '/testHtmlDoc/stylesheet1'; style1a style1b style3a style3b", style(r));
		assertEquals("nav1a nav1b nav3a nav3b", nav(r));
		assertEquals("aside1a aside1b aside3a aside3b", aside(r));
		assertEquals("footer1a footer1b footer3a footer3b", footer(r));
	}

	/**
	 * @RestMethod(
	 * 	path="/test4",
	 * 	htmldoc=@HtmlDoc(
	 * 		aside={"aside4a","INHERIT","aside4b"},
	 * 		footer={"footer4a","INHERIT","footer4b"},
	 * 		header={"header4a","INHERIT","header4b"},
	 * 		nav={"nav4a","INHERIT","nav4b"},
	 * 		script={"script4a","INHERIT","script4b"},
	 * 		style={"style4a","INHERIT","style4b"}
	 * 	)
	 * )
	 */
	@Test
	public void test4() throws Exception {
		String r = get("/testHtmlDoc/test4");
		assertEquals("header4a header1a header1b header4b", header(r));
		assertEquals("script4a script1a script1b script4b", script(r));
		assertEquals("@import '/testHtmlDoc/stylesheet1'; style4a style1a style1b style4b", style(r));
		assertEquals("nav4a nav1a nav1b nav4b", nav(r));
		assertEquals("aside4a aside1a aside1b aside4b", aside(r));
		assertEquals("footer4a footer1a footer1b footer4b", footer(r));
	}

	/**
	 * @RestMethod(
	 * 	path="/test5",
	 * 	htmldoc=@HtmlDoc(
	 * 		aside={"aside5a","aside5b","INHERIT"},
	 * 		footer={"footer5a","footer5b","INHERIT"},
	 * 		header={"header5a","header5b","INHERIT"},
	 * 		nav={"nav5a","nav5b","INHERIT"},
	 * 		script={"script5a","script5b","INHERIT"},
	 * 		style={"style5a","style5b","INHERIT"}
	 * 	)
	 * )
	 */
	@Test
	public void test5() throws Exception {
		String r = get("/testHtmlDoc/test5");
		assertEquals("header5a header5b header1a header1b", header(r));
		assertEquals("script5a script5b script1a script1b", script(r));
		assertEquals("@import '/testHtmlDoc/stylesheet1'; style5a style5b style1a style1b", style(r));
		assertEquals("nav5a nav5b nav1a nav1b", nav(r));
		assertEquals("aside5a aside5b aside1a aside1b", aside(r));
		assertEquals("footer5a footer5b footer1a footer1b", footer(r));
	}

	/**
	 * @RestResource(
	 * 	path="/testHtmlDoc2",
	 * 	htmldoc=@HtmlDoc(
	 * 		aside={"INHERIT","aside11a","aside11b"},
	 * 		footer={"footer11a","INHERIT","footer11b"},
	 * 		header={"header11a","header11b","INHERIT"},
	 * 		nav={"INHERIT","nav11a","nav11b"},
	 * 		script={"script11a","script11b"},
	 * 		style={"style11a","style11b"},
	 * 		stylesheet="stylesheet11"
	 * 	)
	 * )
	 */
	@Test
	public void test11() throws Exception {
		String r = get("/testHtmlDoc/testHtmlDoc2/test11");
		assertEquals("header11a header11b header1a header1b", header(r));
		assertEquals("script11a script11b", script(r));
		assertEquals("@import '/testHtmlDoc/testHtmlDoc2/stylesheet11'; style11a style11b", style(r));
		assertEquals("nav1a nav1b nav11a nav11b", nav(r));
		assertEquals("aside1a aside1b aside11a aside11b", aside(r));
		assertEquals("footer11a footer1a footer1b footer11b", footer(r));
	}

	/**
	 * @RestMethod(
	 * 	path="/test12",
	 * 	htmldoc=@HtmlDoc(
	 * 		aside={"aside12a","aside12b"},
	 * 		footer={"footer12a","footer12b"},
	 * 		header={"header12a","header12b"},
	 * 		nav={"nav12a","nav12b"},
	 * 		script={"script12a","script12b"},
	 * 		style={"style12a","style12b"},
	 * 		stylesheet="stylesheet12"
	 * 	)
	 * )
	 */
	@Test
	public void test12() throws Exception {
		String r = get("/testHtmlDoc/testHtmlDoc2/test12");
		assertEquals("header12a header12b", header(r));
		assertEquals("script12a script12b", script(r));
		assertEquals("@import '/testHtmlDoc/testHtmlDoc2/stylesheet12'; style12a style12b", style(r));
		assertEquals("nav12a nav12b", nav(r));
		assertEquals("aside12a aside12b", aside(r));
		assertEquals("footer12a footer12b", footer(r));
	}

	/**
	 * @RestMethod(
	 * 	path="/test13",
	 * 	htmldoc=@HtmlDoc(
	 * 		aside={"INHERIT","aside13a","aside13b"},
	 * 		footer={"INHERIT","footer13a","footer13b"},
	 * 		header={"INHERIT","header13a","header13b"},
	 * 		nav={"INHERIT","nav13a","nav13b"},
	 * 		script={"INHERIT","script13a","script13b"},
	 * 		style={"INHERIT","style13a","style13b"}
	 * 	)
	 * )
	 */
	@Test
	public void test13() throws Exception {
		String r = get("/testHtmlDoc/testHtmlDoc2/test13");
		assertEquals("header11a header11b header1a header1b header13a header13b", header(r));
		assertEquals("script11a script11b script13a script13b", script(r));
		assertEquals("@import '/testHtmlDoc/testHtmlDoc2/stylesheet11'; style11a style11b style13a style13b", style(r));
		assertEquals("nav1a nav1b nav11a nav11b nav13a nav13b", nav(r));
		assertEquals("aside1a aside1b aside11a aside11b aside13a aside13b", aside(r));
		assertEquals("footer11a footer1a footer1b footer11b footer13a footer13b", footer(r));
	}

	/**
	 * @RestMethod(
	 * 	path="/test14",
	 * 	htmldoc=@HtmlDoc(
	 * 		aside={"aside14a","INHERIT","aside14b"},
	 * 		footer={"footer14a","INHERIT","footer14b"},
	 * 		header={"header14a","INHERIT","header14b"},
	 * 		nav={"nav14a","INHERIT","nav14b"},
	 * 		script={"script14a","INHERIT","script14b"},
	 * 		style={"style14a","INHERIT","style14b"}
	 * 	)
	 * )
	 */
	@Test
	public void test14() throws Exception {
		String r = get("/testHtmlDoc/testHtmlDoc2/test14");
		assertEquals("header14a header11a header11b header1a header1b header14b", header(r));
		assertEquals("script14a script11a script11b script14b", script(r));
		assertEquals("@import '/testHtmlDoc/testHtmlDoc2/stylesheet11'; style14a style11a style11b style14b", style(r));
		assertEquals("nav14a nav1a nav1b nav11a nav11b nav14b", nav(r));
		assertEquals("aside14a aside1a aside1b aside11a aside11b aside14b", aside(r));
		assertEquals("footer14a footer11a footer1a footer1b footer11b footer14b", footer(r));
	}

	/**
	 * @RestMethod(
	 * 	path="/test15",
	 * 	htmldoc=@HtmlDoc(
	 * 		aside={"aside15a","aside15b","INHERIT"},
	 * 		footer={"footer15a","footer15b","INHERIT"},
	 * 		header={"header15a","header15b","INHERIT"},
	 * 		nav={"nav15a","nav15b","INHERIT"},
	 * 		script={"script15a","script15b","INHERIT"},
	 * 		style={"style15a","style15b","INHERIT"}
	 * 	)
	 * )
	 */
	@Test
	public void test15() throws Exception {
		String r = get("/testHtmlDoc/testHtmlDoc2/test15");
		assertEquals("header15a header15b header11a header11b header1a header1b", header(r));
		assertEquals("script15a script15b script11a script11b", script(r));
		assertEquals("@import '/testHtmlDoc/testHtmlDoc2/stylesheet11'; style15a style15b style11a style11b", style(r));
		assertEquals("nav15a nav15b nav1a nav1b nav11a nav11b", nav(r));
		assertEquals("aside15a aside15b aside1a aside1b aside11a aside11b", aside(r));
		assertEquals("footer15a footer15b footer11a footer1a footer1b footer11b", footer(r));
	}
}