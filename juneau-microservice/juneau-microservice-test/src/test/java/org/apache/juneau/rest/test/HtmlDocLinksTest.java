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
 * Validates inheritance on the @HtmlDoc.navlinks() annotation.
 */
public class HtmlDocLinksTest extends RestTestcase {
	private RestClient client = TestMicroservice.DEFAULT_CLIENT;

	private String get(String uri) throws Exception {
		return client.doGet(uri).accept("text/html").getResponseAsString().replace('\n', ' ').replace('"', '\'').replaceAll(".*<nav>", "<nav>").replaceAll("</nav>.*", "</nav>");
	}

	/**
	 * @RestResource(
	 * 	path="/testHtmlDocLinks",
	 * 	htmldoc=@HtmlDoc(
	 * 		navlinks={"links1a","links1b"}
	 * 	)
	 * )
	 */
	@Test
	public void test1() throws Exception {
		String r = get("/testHtmlDocLinks/test1");
		assertEquals("<nav><ol><li>links1a</li><li>links1b</li></ol></nav>", r);
	}

	/**
	 * @RestMethod(
	 * 	path="/test2",
	 * 	htmldoc=@HtmlDoc(
	 * 		navlinks={"links2a","links2b"}
	 * 	)
	 * )
	 */
	@Test
	public void test2() throws Exception {
		String r = get("/testHtmlDocLinks/test2");
		assertEquals("<nav><ol><li>links2a</li><li>links2b</li></ol></nav>", r);
	}

	/**
	 * @RestMethod(
	 * 	path="/test3",
	 * 	htmldoc=@HtmlDoc(
	 * 		navlinks={"INHERIT","links3a","links3b"}
	 * 	)
	 * )
	 */
	@Test
	public void test3() throws Exception {
		String r = get("/testHtmlDocLinks/test3");
		assertEquals("<nav><ol><li>links1a</li><li>links1b</li><li>links3a</li><li>links3b</li></ol></nav>", r);
	}

	/**
	 * @RestMethod(
	 * 	path="/test4",
	 * 	htmldoc=@HtmlDoc(
	 * 		navlinks={"links4a","INHERIT","links4b"}
	 * 	)
	 * )
	 */
	@Test
	public void test4() throws Exception {
		String r = get("/testHtmlDocLinks/test4");
		assertEquals("<nav><ol><li>links4a</li><li>links1a</li><li>links1b</li><li>links4b</li></ol></nav>", r);
	}

	/**
	 * @RestMethod(
	 * 	path="/test5",
	 * 	htmldoc=@HtmlDoc(
	 * 		navlinks={"links5a","links5b","INHERIT"}
	 * 	)
	 * )
	 */
	@Test
	public void test5() throws Exception {
		String r = get("/testHtmlDocLinks/test5");
		assertEquals("<nav><ol><li>links5a</li><li>links5b</li><li>links1a</li><li>links1b</li></ol></nav>", r);
	}

	/**
	 * @RestMethod(
	 * 	path="/test6a",
	 * 	htmldoc=@HtmlDoc(
	 * 		navlinks={"INHERIT","[0]:links6a","[3]:links6b"}
	 * 	)
	 * )
	 */
	@Test
	public void test6a() throws Exception {
		String r = get("/testHtmlDocLinks/test6a");
		assertEquals("<nav><ol><li>links6a</li><li>links1a</li><li>links1b</li><li>links6b</li></ol></nav>", r);
	}

	/**
	 * @RestMethod(
	 * 	path="/test6b",
	 * 	htmldoc=@HtmlDoc(
	 * 		navlinks={"[1]:links6a","[2]:links6b","INHERIT"}
	 * 	)
	 * )
	 */
	@Test
	public void test6b() throws Exception {
		String r = get("/testHtmlDocLinks/test6b");
		assertEquals("<nav><ol><li>links6a</li><li>links6b</li><li>links1a</li><li>links1b</li></ol></nav>", r);
	}

	/**
	 * @RestMethod(
	 * 	path="/test6c",
	 * 	htmldoc=@HtmlDoc(
	 * 		navlinks={"[1]:links6a","[0]:links6b"}
	 * 	)
	 * )
	 */
	@Test
	public void test6c() throws Exception {
		String r = get("/testHtmlDocLinks/test6c");
		assertEquals("<nav><ol><li>links6b</li><li>links6a</li></ol></nav>", r);
	}

	/**
	 * @RestMethod(
	 * 	path="/test6d",
	 * 	htmldoc=@HtmlDoc(
	 * 		navlinks={"INHERIT","foo[0]:links6a","bar[3]:links6b"}
	 * 	)
	 * )
	 */
	@Test
	public void test6d() throws Exception {
		String r = get("/testHtmlDocLinks/test6d");
		assertEquals("<nav><ol><li><a href='/testHtmlDocLinks/links6a'>foo</a></li><li>links1a</li><li>links1b</li><li><a href='/testHtmlDocLinks/links6b'>bar</a></li></ol></nav>", r);
	}

	/**
	 * @RestMethod(
	 * 	path="/test6e",
	 * 	htmldoc=@HtmlDoc(
	 * 		navlinks={"foo[1]:links6a","bar[2]:links6b","INHERIT"}
	 * 	)
	 * )
	 */
	@Test
	public void test6e() throws Exception {
		String r = get("/testHtmlDocLinks/test6e");
		assertEquals("<nav><ol><li><a href='/testHtmlDocLinks/links6a'>foo</a></li><li><a href='/testHtmlDocLinks/links6b'>bar</a></li><li>links1a</li><li>links1b</li></ol></nav>", r);
	}

	/**
	 * @RestMethod(
	 * 	path="/test6f",
	 * 	htmldoc=@HtmlDoc(
	 * 		navlinks={"foo[1]:links6a","bar[0]:links6b"}
	 * 	)
	 * )
	 */
	@Test
	public void test6f() throws Exception {
		String r = get("/testHtmlDocLinks/test6f");
		assertEquals("<nav><ol><li><a href='/testHtmlDocLinks/links6b'>bar</a></li><li><a href='/testHtmlDocLinks/links6a'>foo</a></li></ol></nav>", r);
	}

	/**
	 * @RestResource(
	 * 	path="/testHtmlDocLinks2",
	 * 	htmldoc=@HtmlDoc(
	 * 		navlinks={"INHERIT","links11a","links11b"}
	 * 	)
	 * )
	 */
	@Test
	public void test11() throws Exception {
		String r = get("/testHtmlDocLinks/testHtmlDocLinks2/test11");
		assertEquals("<nav><ol><li>links1a</li><li>links1b</li><li>links11a</li><li>links11b</li></ol></nav>", r);
	}

	/**
	 * @RestMethod(
	 * 	path="/test12",
	 * 	htmldoc=@HtmlDoc(
	 * 		navlinks={"links12a","links12b"}
	 * 	)
	 * )
	 */
	@Test
	public void test12() throws Exception {
		String r = get("/testHtmlDocLinks/testHtmlDocLinks2/test12");
		assertEquals("<nav><ol><li>links12a</li><li>links12b</li></ol></nav>", r);
	}

	/**
	 * @RestMethod(
	 * 	path="/test13",
	 * 	htmldoc=@HtmlDoc(
	 * 		navlinks={"INHERIT","links13a","links13b"}
	 * 	)
	 * )
	 */
	@Test
	public void test13() throws Exception {
		String r = get("/testHtmlDocLinks/testHtmlDocLinks2/test13");
		assertEquals("<nav><ol><li>links1a</li><li>links1b</li><li>links11a</li><li>links11b</li><li>links13a</li><li>links13b</li></ol></nav>", r);
	}

	/**
	 * @RestMethod(
	 * 	path="/test14",
	 * 	htmldoc=@HtmlDoc(
	 * 		navlinks={"links14a","INHERIT","links14b"}
	 * 	)
	 * )
	 */
	@Test
	public void test14() throws Exception {
		String r = get("/testHtmlDocLinks/testHtmlDocLinks2/test14");
		assertEquals("<nav><ol><li>links14a</li><li>links1a</li><li>links1b</li><li>links11a</li><li>links11b</li><li>links14b</li></ol></nav>", r);
	}

	/**
	 * @RestMethod(
	 * 	path="/test15",
	 * 	htmldoc=@HtmlDoc(
	 * 		navlinks={"links15a","links15b","INHERIT"}
	 * 	)
	 * )
	 */
	@Test
	public void test15() throws Exception {
		String r = get("/testHtmlDocLinks/testHtmlDocLinks2/test15");
		assertEquals("<nav><ol><li>links15a</li><li>links15b</li><li>links1a</li><li>links1b</li><li>links11a</li><li>links11b</li></ol></nav>", r);
	}

	/**
	 * @RestMethod(
	 * 	path="/test16a",
	 * 	htmldoc=@HtmlDoc(
	 * 		navlinks={"INHERIT","[0]:links16a","[3]:links16b"}
	 * 	)
	 * )
	 */
	@Test
	public void test16a() throws Exception {
		String r = get("/testHtmlDocLinks/testHtmlDocLinks2/test16a");
		assertEquals("<nav><ol><li>links16a</li><li>links1a</li><li>links1b</li><li>links16b</li><li>links11a</li><li>links11b</li></ol></nav>", r);
	}

	/**
	 * @RestMethod(
	 * 	path="/test16b",
	 * 	htmldoc=@HtmlDoc(
	 * 		navlinks={"[1]:links16a","[2]:links16b","INHERIT"}
	 * 	)
	 * )
	 */
	@Test
	public void test16b() throws Exception {
		String r = get("/testHtmlDocLinks/testHtmlDocLinks2/test16b");
		assertEquals("<nav><ol><li>links16a</li><li>links16b</li><li>links1a</li><li>links1b</li><li>links11a</li><li>links11b</li></ol></nav>", r);
	}

	/**
	 * @RestMethod(
	 * 	path="/test16c",
	 * 	htmldoc=@HtmlDoc(
	 * 		navlinks={"[1]:links16a","[0]:links16b"}
	 * 	)
	 * )
	 */
	@Test
	public void test16c() throws Exception {
		String r = get("/testHtmlDocLinks/testHtmlDocLinks2/test16c");
		assertEquals("<nav><ol><li>links16b</li><li>links16a</li></ol></nav>", r);
	}

	/**
	 * @RestMethod(
	 * 	path="/test16d",
	 * 	htmldoc=@HtmlDoc(
	 * 		navlinks={"INHERIT","foo[0]:links16a","bar[3]:links16b"}
	 * 	)
	 * )
	 */
	@Test
	public void test16d() throws Exception {
		String r = get("/testHtmlDocLinks/testHtmlDocLinks2/test16d");
		assertEquals("<nav><ol><li><a href='/testHtmlDocLinks/testHtmlDocLinks2/links16a'>foo</a></li><li>links1a</li><li>links1b</li><li><a href='/testHtmlDocLinks/testHtmlDocLinks2/links16b'>bar</a></li><li>links11a</li><li>links11b</li></ol></nav>", r);
	}

	/**
	 * @RestMethod(
	 * 	path="/test16e",
	 * 	htmldoc=@HtmlDoc(
	 * 		navlinks={"foo[1]:links16a","bar[2]:links16b","INHERIT"}
	 * 	)
	 * )
	 */
	@Test
	public void test16e() throws Exception {
		String r = get("/testHtmlDocLinks/testHtmlDocLinks2/test16e");
		assertEquals("<nav><ol><li><a href='/testHtmlDocLinks/testHtmlDocLinks2/links16a'>foo</a></li><li><a href='/testHtmlDocLinks/testHtmlDocLinks2/links16b'>bar</a></li><li>links1a</li><li>links1b</li><li>links11a</li><li>links11b</li></ol></nav>", r);
	}

	/**
	 * @RestMethod(
	 * 	path="/test16f",
	 * 	htmldoc=@HtmlDoc(
	 * 		navlinks={"foo[1]:links16a","bar[0]:links16b"}
	 * 	)
	 * )
	 */
	@Test
	public void test16f() throws Exception {
		String r = get("/testHtmlDocLinks/testHtmlDocLinks2/test16f");
		assertEquals("<nav><ol><li><a href='/testHtmlDocLinks/testHtmlDocLinks2/links16b'>bar</a></li><li><a href='/testHtmlDocLinks/testHtmlDocLinks2/links16a'>foo</a></li></ol></nav>", r);
	}
}
