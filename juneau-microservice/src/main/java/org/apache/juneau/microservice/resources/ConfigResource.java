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
package org.apache.juneau.microservice.resources;

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.dto.html5.HtmlBuilder.*;

import java.io.*;
import java.util.Map;

import org.apache.juneau.*;
import org.apache.juneau.dto.html5.*;
import org.apache.juneau.ini.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.annotation.Body;

/**
 * Shows contents of the microservice configuration file.
 */
@RestResource(
	path="/config",
	title="Configuration",
	description="Contents of configuration file.",
	htmldoc=@HtmlDoc(
		links={
			"up: request:/..",
			"options: servlet:/?method=OPTIONS",
			"edit: servlet:/edit"
		}
	)
)
public class ConfigResource extends Resource {
	private static final long serialVersionUID = 1L;

	/**
	 * [GET /] - Show contents of config file.
	 *
	 * @return The config file.
	 * @throws Exception
	 */
	@RestMethod(name="GET", path="/", description="Show contents of config file.")
	public ConfigFile getConfigContents() throws Exception {
		return getServletConfig().getConfigFile();
	}

	/**
	 * [GET /edit] - Show config file edit page.
	 *
	 * @param req The HTTP request.
	 * @return The config file as a reader resource.
	 * @throws Exception
	 */
	@RestMethod(name="GET", path="/edit", description="Edit config file.")
	public Form getConfigEditForm(RestRequest req) throws Exception {
		return form().id("form").action("servlet:/").method("POST").enctype("application/x-www-form-urlencoded").children(
			div()._class("data").children(
				table(
					tr(td().style("text-align:right").children(button("submit","Submit"),button("reset","Reset"))),
					tr(th().child("Contents")),
					tr(th().child(
						textarea().name("contents").rows(40).cols(120).style("white-space:pre;word-wrap:normal;overflow-x:scroll;font-family:monospace;")
							.text(getConfigContents().toString()))
					)
				)
			)
		);
	}

	/**
	 * [GET /{section}] - Show config file section.
	 *
	 * @param section The section name.
	 * @return The config file section.
	 * @throws Exception
	 */
	@RestMethod(name="GET", path="/{section}",
		description="Show config file section.",
		swagger=@MethodSwagger(
			parameters={
				@Parameter(in="path", name="section", description="Section name.")
			}
		)
	)
	public ObjectMap getConfigSection(@Path("section") String section) throws Exception {
		return getSection(section);
	}

	/**
	 * [GET /{section}/{key}] - Show config file entry.
	 *
	 * @param section The section name.
	 * @param key The section key.
	 * @return The value of the config file entry.
	 * @throws Exception
	 */
	@RestMethod(name="GET", path="/{section}/{key}",
		description="Show config file entry.",
		swagger=@MethodSwagger(
			parameters={
				@Parameter(in="path", name="section", description="Section name."),
				@Parameter(in="path", name="key", description="Entry name.")
			}
		)
	)
	public String getConfigEntry(@Path("section") String section, @Path("key") String key) throws Exception {
		return getSection(section).getString(key);
	}

	/**
	 * [POST /] - Sets contents of config file from a FORM post.
	 *
	 * @param contents The new contents of the config file.
	 * @return The new config file contents.
	 * @throws Exception
	 */
	@RestMethod(name="POST", path="/",
		description="Sets contents of config file from a FORM post.",
		swagger=@MethodSwagger(
			parameters={
				@Parameter(in="formData", name="contents", description="New contents in INI file format.")
			}
		)
	)
	public ConfigFile setConfigContentsFormPost(@FormData("contents") String contents) throws Exception {
		return setConfigContents(new StringReader(contents));
	}

	/**
	 * [PUT /] - Sets contents of config file.
	 *
	 * @param contents The new contents of the config file.
	 * @return The new config file contents.
	 * @throws Exception
	 */
	@RestMethod(name="PUT", path="/",
		description="Sets contents of config file.",
		swagger=@MethodSwagger(
			parameters={
				@Parameter(in="body", description="New contents in INI file format.")
			}
		)
	)
	public ConfigFile setConfigContents(@Body Reader contents) throws Exception {
		ConfigFile cf2 = new ConfigFileBuilder().build(contents);
		return getConfigContents().merge(cf2).save();
	}

	/**
	 * [PUT /{section}] - Add or overwrite a config file section.
	 *
	 * @param section The section name.
	 * @param contents The new contents of the config file section.
	 * @return The new section.
	 * @throws Exception
	 */
	@RestMethod(name="PUT", path="/{section}",
		description="Add or overwrite a config file section.",
		swagger=@MethodSwagger(
			parameters={
				@Parameter(in="path", name="section", description="Section name."),
				@Parameter(in="body", description="New contents for section as a simple map with string keys and values.")
			}
		)
	)
	public ObjectMap setConfigSection(@Path("section") String section, @Body Map<String,String> contents) throws Exception {
		getConfigContents().setSection(section, contents);
		return getSection(section);
	}

	/**
	 * [PUT /{section}/{key}] - Add or overwrite a config file entry.
	 *
	 * @param section The section name.
	 * @param key The section key.
	 * @param value The new value.
	 * @return The new value.
	 * @throws Exception
	 */
	@RestMethod(name="PUT", path="/{section}/{key}",
		description="Add or overwrite a config file entry.",
		swagger=@MethodSwagger(
			parameters={
				@Parameter(in="path", name="section", description="Section name."),
				@Parameter(in="path", name="key", description="Entry name."),
				@Parameter(in="body", description="New value as a string.")
			}
		)
	)
	public String setConfigSection(@Path("section") String section, @Path("key") String key, @Body String value) throws Exception {
		getConfigContents().put(section, key, value, false);
		return getSection(section).getString(key);
	}

	private ObjectMap getSection(String name) throws Exception {
		ObjectMap m = getConfigContents().getSectionMap(name);
		if (m == null)
			throw new RestException(SC_NOT_FOUND, "Section not found.");
		return m;
	}
}
