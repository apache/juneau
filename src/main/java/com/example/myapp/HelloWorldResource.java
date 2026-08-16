/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.myapp;

import org.apache.juneau.http.*;
import org.apache.juneau.marshall.html.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * Minimal Juneau starter resource. Returns a {@link Greeting} POJO (JSON/XML/HTML negotiation), carries an
 * {@link HtmlDocConfig} view, exposes an auto Swagger/OPTIONS page, demonstrates {@code @Path}/{@code @Query}
 * binding, and reads one value from {@code my-app.yaml}.
 */
@Rest(
	path="/helloWorld",
	title="Hello World",
	description="Minimal Juneau starter resource — returns a POJO so the same endpoint serves JSON, XML, and HTML.",
	config="my-app.yaml",
	defaultAccept="application/json"
)
@HtmlDocConfig(
	navlinks={
		"api: servlet:/api",
		"stats: servlet:/stats"
	},
	aside={
		"<div class='text'>",
		"\t<p>This page is a serialized POJO. Request it with <code>Accept: application/json</code>, <code>text/xml</code>, or <code>text/html</code> to see content negotiation.</p>",
		"\t<p>The <span class='link'>api</span> link shows the auto-generated Swagger / OPTIONS page.</p>",
		"</div>"
	},
	asideFloat="RIGHT"
)
public class HelloWorldResource extends BasicRestResource {

	/**
	 * Returns a greeting POJO. The message text is read from <c>my-app.yaml</c>.
	 *
	 * @param req The incoming request (used to read the resolved config).
	 * @return The greeting.
	 */
	@RestGet(path="/*", summary="Returns a greeting POJO (message text comes from my-app.yaml).")
	public Greeting sayHello(RestRequest req) {
		var message = req.getConfig().getString("HelloWorld/message");
		if (message == null)
			message = "Hello world!";
		return new Greeting("world", message);
	}

	/**
	 * Greets a named user. Demonstrates {@code @Path} + {@code @Query} binding.
	 *
	 * @param name The path variable.
	 * @param loud If <jk>true</jk>, the message is upper-cased.
	 * @return The greeting.
	 */
	@RestGet(path="/greet/{name}", summary="Greets a named user; ?loud=true upper-cases the message.")
	public Greeting greet(@Path("name") String name, @Query("loud") boolean loud) {
		var message = "Hello " + name + "!";
		return new Greeting(name, loud ? message.toUpperCase() : message);
	}
}
