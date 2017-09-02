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

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Validates inheritance on the @HtmlDoc annotation.
 */
@RestResource(
	path="/testHtmlDoc",
	htmldoc=@HtmlDoc(
		aside={"aside1a","aside1b","INHERIT"},
		footer={"footer1a","footer1b"},
		header={"header1a","header1b"},
		nav={"nav1a","nav1b"},
		script={"script1a","script1b"},
		style={"style1a","style1b"},
		stylesheet="stylesheet1"
	),
	children={
		HtmlDocResource.HtmlDocResource2.class
	}
)
public class HtmlDocResource extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	@RestMethod(path="/test1")
	public Object test1() {
		return "OK";
	}

	@RestMethod(
		path="/test2",
		htmldoc=@HtmlDoc(
			aside={"aside2a","aside2b"},
			footer={"footer2a","footer2b"},
			header={"header2a","header2b"},
			nav={"nav2a","nav2b"},
			script={"script2a","script2b"},
			style={"style2a","style2b"},
			stylesheet="stylesheet2"
		)
	)
	public Object test2() {
		return "OK";
	}

	@RestMethod(
		path="/test3",
		htmldoc=@HtmlDoc(
			aside={"INHERIT","aside3a","aside3b"},
			footer={"INHERIT","footer3a","footer3b"},
			header={"INHERIT","header3a","header3b"},
			nav={"INHERIT","nav3a","nav3b"},
			script={"INHERIT","script3a","script3b"},
			style={"INHERIT","style3a","style3b"}
		)
	)
	public Object test3() {
		return "OK";
	}

	@RestMethod(
		path="/test4",
		htmldoc=@HtmlDoc(
			aside={"aside4a","INHERIT","aside4b"},
			footer={"footer4a","INHERIT","footer4b"},
			header={"header4a","INHERIT","header4b"},
			nav={"nav4a","INHERIT","nav4b"},
			script={"script4a","INHERIT","script4b"},
			style={"style4a","INHERIT","style4b"}
		)
	)
	public Object test4() {
		return "OK";
	}

	@RestMethod(
		path="/test5",
		htmldoc=@HtmlDoc(
			aside={"aside5a","aside5b","INHERIT"},
			footer={"footer5a","footer5b","INHERIT"},
			header={"header5a","header5b","INHERIT"},
			nav={"nav5a","nav5b","INHERIT"},
			script={"script5a","script5b","INHERIT"},
			style={"style5a","style5b","INHERIT"}
		)
	)
	public Object test5() {
		return "OK";
	}

	@RestResource(
		path="/testHtmlDoc2",
		htmldoc=@HtmlDoc(
			aside={"INHERIT","aside11a","aside11b"},
			footer={"footer11a","INHERIT","footer11b"},
			header={"header11a","header11b","INHERIT"},
			nav={"INHERIT","nav11a","nav11b"},
			script={"script11a","script11b"},
			style={"style11a","style11b"},
			stylesheet="stylesheet11"
		)
	)
	public static class HtmlDocResource2 extends HtmlDocResource {
		private static final long serialVersionUID = 1L;

		@RestMethod(path="/test11")
		public Object test11() {
			return "OK";
		}

		@RestMethod(
			path="/test12",
			htmldoc=@HtmlDoc(
				aside={"aside12a","aside12b"},
				footer={"footer12a","footer12b"},
				header={"header12a","header12b"},
				nav={"nav12a","nav12b"},
				script={"script12a","script12b"},
				style={"style12a","style12b"},
				stylesheet="stylesheet12"
			)
		)
		public Object test12() {
			return "OK";
		}

		@RestMethod(
			path="/test13",
			htmldoc=@HtmlDoc(
				aside={"INHERIT","aside13a","aside13b"},
				footer={"INHERIT","footer13a","footer13b"},
				header={"INHERIT","header13a","header13b"},
				nav={"INHERIT","nav13a","nav13b"},
				script={"INHERIT","script13a","script13b"},
				style={"INHERIT","style13a","style13b"}
			)
		)
		public Object test13() {
			return "OK";
		}

		@RestMethod(
			path="/test14",
			htmldoc=@HtmlDoc(
				aside={"aside14a","INHERIT","aside14b"},
				footer={"footer14a","INHERIT","footer14b"},
				header={"header14a","INHERIT","header14b"},
				nav={"nav14a","INHERIT","nav14b"},
				script={"script14a","INHERIT","script14b"},
				style={"style14a","INHERIT","style14b"}
			)
		)
		public Object test14() {
			return "OK";
		}

		@RestMethod(
			path="/test15",
			htmldoc=@HtmlDoc(
				aside={"aside15a","aside15b","INHERIT"},
				footer={"footer15a","footer15b","INHERIT"},
				header={"header15a","header15b","INHERIT"},
				nav={"nav15a","nav15b","INHERIT"},
				script={"script15a","script15b","INHERIT"},
				style={"style15a","style15b","INHERIT"}
			)
		)
		public Object test15() {
			return "OK";
		}
	}
}
