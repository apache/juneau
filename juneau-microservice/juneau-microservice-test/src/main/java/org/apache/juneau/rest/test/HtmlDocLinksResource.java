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
 * Validates inheritance on the @HtmlDoc.navlinks() annotation.
 */
@RestResource(
	path="/testHtmlDocLinks",
	htmldoc=@HtmlDoc(
		navlinks={"links1a","links1b"}
	),
	children={
		HtmlDocLinksResource.HtmlDocLinksResource2.class
	}
)
public class HtmlDocLinksResource extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	@RestMethod(path="/test1")
	public Object test1() {
		return "OK";
	}

	@RestMethod(
		path="/test2",
		htmldoc=@HtmlDoc(
			navlinks={"links2a","links2b"}
		)
	)
	public Object test2() {
		return "OK";
	}

	@RestMethod(
		path="/test3",
		htmldoc=@HtmlDoc(
			navlinks={"INHERIT","links3a","links3b"}
		)
	)
	public Object test3() {
		return "OK";
	}

	@RestMethod(
		path="/test4",
		htmldoc=@HtmlDoc(
			navlinks={"links4a","INHERIT","links4b"}
		)
	)
	public Object test4() {
		return "OK";
	}

	@RestMethod(
		path="/test5",
		htmldoc=@HtmlDoc(
			navlinks={"links5a","links5b","INHERIT"}
		)
	)
	public Object test5() {
		return "OK";
	}

	@RestMethod(
		path="/test6a",
		htmldoc=@HtmlDoc(
			navlinks={"INHERIT","[0]:links6a","[3]:links6b"}
		)
	)
	public Object test6a() {
		return "OK";
	}

	@RestMethod(
		path="/test6b",
		htmldoc=@HtmlDoc(
			navlinks={"[1]:links6a","[2]:links6b","INHERIT"}
		)
	)
	public Object test6b() {
		return "OK";
	}

	@RestMethod(
		path="/test6c",
		htmldoc=@HtmlDoc(
			navlinks={"[1]:links6a","[0]:links6b"}
		)
	)
	public Object test6c() {
		return "OK";
	}

	@RestMethod(
		path="/test6d",
		htmldoc=@HtmlDoc(
			navlinks={"INHERIT","foo[0]:links6a","bar[3]:links6b"}
		)
	)
	public Object test6d() {
		return "OK";
	}

	@RestMethod(
		path="/test6e",
		htmldoc=@HtmlDoc(
			navlinks={"foo[1]:links6a","bar[2]:links6b","INHERIT"}
		)
	)
	public Object test6e() {
		return "OK";
	}

	@RestMethod(
		path="/test6f",
		htmldoc=@HtmlDoc(
			navlinks={"foo[1]:links6a","bar[0]:links6b"}
		)
	)
	public Object test6f() {
		return "OK";
	}

	@RestResource(
		path="/testHtmlDocLinks2",
		htmldoc=@HtmlDoc(
			navlinks={"INHERIT","links11a","links11b"}
		)
	)
	public static class HtmlDocLinksResource2 extends HtmlDocLinksResource {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@RestMethod(path="/test11")
		public Object test11() {
			return "OK";
		}

		@RestMethod(
			path="/test12",
			htmldoc=@HtmlDoc(
				navlinks={"links12a","links12b"}
			)
		)
		public Object test12() {
			return "OK";
		}

		@RestMethod(
			path="/test13",
			htmldoc=@HtmlDoc(
				navlinks={"INHERIT","links13a","links13b"}
			)
		)
		public Object test13() {
			return "OK";
		}

		@RestMethod(
			path="/test14",
			htmldoc=@HtmlDoc(
				navlinks={"links14a","INHERIT","links14b"}
			)
		)
		public Object test14() {
			return "OK";
		}

		@RestMethod(
			path="/test15",
			htmldoc=@HtmlDoc(
				navlinks={"links15a","links15b","INHERIT"}
			)
		)
		public Object test15() {
			return "OK";
		}

		@RestMethod(
			path="/test16a",
			htmldoc=@HtmlDoc(
				navlinks={"INHERIT","[0]:links16a","[3]:links16b"}
			)
		)
		public Object test16a() {
			return "OK";
		}

		@RestMethod(
			path="/test16b",
			htmldoc=@HtmlDoc(
				navlinks={"[1]:links16a","[2]:links16b","INHERIT"}
			)
		)
		public Object test16b() {
			return "OK";
		}

		@RestMethod(
			path="/test16c",
			htmldoc=@HtmlDoc(
				navlinks={"[1]:links16a","[0]:links16b"}
			)
		)
		public Object test16c() {
			return "OK";
		}

		@RestMethod(
			path="/test16d",
			htmldoc=@HtmlDoc(
				navlinks={"INHERIT","foo[0]:links16a","bar[3]:links16b"}
			)
		)
		public Object test16d() {
			return "OK";
		}

		@RestMethod(
			path="/test16e",
			htmldoc=@HtmlDoc(
				navlinks={"foo[1]:links16a","bar[2]:links16b","INHERIT"}
			)
		)
		public Object test16e() {
			return "OK";
		}

		@RestMethod(
			path="/test16f",
			htmldoc=@HtmlDoc(
				navlinks={"foo[1]:links16a","bar[0]:links16b"}
			)
		)
		public Object test16f() {
			return "OK";
		}
	}
}
