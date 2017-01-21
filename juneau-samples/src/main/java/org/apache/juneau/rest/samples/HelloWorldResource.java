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
package org.apache.juneau.rest.samples;

import static org.apache.juneau.html.HtmlDocSerializerContext.*;

import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Sample REST resource that prints out a simple "Hello world!" message.
 */
@RestResource(
	messages="nls/HelloWorldResource",
	path="/helloWorld",
	properties={
		@Property(name=HTMLDOC_links, value="{up:'$R{requestParentURI}',options:'?method=OPTIONS'}")
	}
)
public class HelloWorldResource extends Resource {
	private static final long serialVersionUID = 1L;

	/** GET request handler */
	@RestMethod(name="GET", path="/*")
	public String sayHello() {
		return "Hello world!";
	}
}
