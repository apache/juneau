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

import static org.apache.juneau.dto.html5.HtmlBuilder.*;
import static org.apache.juneau.http.HttpMethodName.*;

import java.io.*;
import java.util.Map;

import org.apache.juneau.*;
import org.apache.juneau.dto.html5.*;
import org.apache.juneau.html.annotation.HtmlDocConfig;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.FormData;
import org.apache.juneau.http.annotation.Path;
import org.apache.juneau.http.annotation.Response;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.http.exception.*;

/**
 * Shows contents of the microservice configuration file.
 */
@Rest(
	path="/config",
	title="Configuration",
	description="Contents of configuration file."
)
@HtmlDocConfig(
	navlinks={
		"up: request:/..",
		"options: servlet:/?method=OPTIONS",
		"stats: servlet:/stats",
		"edit: servlet:/edit"
	}
)
@SuppressWarnings("javadoc")
public class ConfigResource extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	@RestMethod(
		name=GET,
		path="/",
		summary="Get config file contents",
		description="Show contents of config file as an ObjectMap.",
		swagger=@MethodSwagger(
			responses={
				"200:{ description:'Config file as a map of map of objects.', 'x-example':{'':{defaultKey:'defaultValue'},'Section1':{key1:'val1',key2:123}}}"
			}
		)
	)
	public ObjectMap getConfig() {
		return getServletConfig().getConfig().toMap();
	}

	@RestMethod(
		name=GET,
		path="/edit",
		summary="Render form entry page for editing config file",
		description="Renders a form entry page for editing the raw text of a config file."
	)
	public Form getConfigEditForm() {
		return form().id("form").action("servlet:/").method("POST").enctype("application/x-www-form-urlencoded").children(
			div()._class("data").children(
				table(
					tr(td().style("text-align:right").children(button("submit","Submit"),button("reset","Reset"))),
					tr(th().child("Contents")),
					tr(th().child(
						textarea().name("contents").rows(40).cols(120).style("white-space:pre;word-wrap:normal;overflow-x:scroll;font-family:monospace;")
							.text(getServletConfig().getConfig().toString()))
					)
				)
			)
		);
	}

	@RestMethod(
		name=GET,
		path="/{section}",
		summary="Get config file section contents",
		description="Show contents of config file section as an ObjectMap.",
		swagger=@MethodSwagger(
			responses={
				"200:{ description:'Config file section as a map of objects.', 'x-example':{key1:'val1',key2:123}}"
			}
		)
	)
	public ObjectMap getConfigSection(
			@Path(name="section", description="Section name in config file.", example="REST") String section
		) throws SectionNotFound, BadConfig {

		return getSection(section);
	}

	@RestMethod(
		name=GET,
		path="/{section}/{key}",
		summary="Get config file entry value",
		description="Show value of config file entry as a simple string.",
		swagger=@MethodSwagger(
			responses={
				"200:{ description:'Entry value.', 'x-example':'servlet:/htdocs/themes/dark.css'}"
			}
		)
	)
	public String getConfigEntry(
			@Path(name="section", description="Section name in config file.", example="REST") String section,
			@Path(name="key", description="Key name in section.", example="theme") String key
		) throws SectionNotFound, BadConfig {

		return getSection(section).getString(key);
	}

	@RestMethod(
		name=POST,
		path="/",
		summary="Update config file contents",
		description="Update the contents of the config file from a FORM post.",
		swagger=@MethodSwagger(
			responses={
				"200:{ description:'Config file section as a map of objects.', 'x-example':{key1:'val1',key2:123}}"
			}
		)
	)
	public ObjectMap setConfigContentsFormPost(
			@FormData(name="contents", description="New contents in INI file format.") String contents
		) throws Exception {

		return setConfigContents(new StringReader(contents));
	}

	@RestMethod(
		name=PUT,
		path="/",
		summary="Update config file contents",
		description="Update the contents of the config file from raw text.",
		swagger=@MethodSwagger(
			responses={
				"200:{ description:'Config file section as a map of objects.', 'x-example':{key1:'val1',key2:123}}"
			}
		)
	)
	public ObjectMap setConfigContents(
			@Body(description="New contents in INI file format.") Reader contents
		) throws Exception {

		return getServletConfig().getConfig().load(contents, true).toMap();
	}

	@RestMethod(
		name=PUT,
		path="/{section}",
		summary="Update config section contents",
		description="Add or overwrite a config file section.",
		swagger=@MethodSwagger(
			responses={
				"200:{ description:'Config file section as a map of objects.', 'x-example':{key1:'val1',key2:123}}"
			}
		)
	)
	public ObjectMap setConfigSection(
			@Path(name="section", description="Section name in config file.", example="REST") String section,
			@Body(
				description="New contents of config section as a simple map of key/value pairs.",
				example="{theme:'servlet:/htdocs/themes/dark.css'}"
			) Map<String,Object> contents
		) throws Exception {

		getServletConfig().getConfig().setSection(section, null, contents);
		return getSection(section);
	}

	@RestMethod(
		name=PUT,
		path="/{section}/{key}",
		summary="Update config entry value",
		description="Add or overwrite a config file entry.",
		swagger=@MethodSwagger(
			responses={
				"200:{ description:'The updated value.', 'x-example':'servlet:/htdocs/themes/dark.css'}"
			}
		)
	)
	public String setConfigValue(
			@Path(name="section", description="Section name in config file.", example="REST") String section,
			@Path(name="key", description="Key name in section.", example="theme") String key,
			@Body(description="New value for entry.", example="servlet:/htdocs/themes/dark.css") String value
		) throws SectionNotFound, BadConfig {

		getServletConfig().getConfig().set(section + '/' + key, value);
		return getSection(section).getString(key);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper beans
	//-----------------------------------------------------------------------------------------------------------------

	@Response(description="Section not found.")
	private class SectionNotFound extends NotFound {
		private static final long serialVersionUID = 1L;

		SectionNotFound() {
			super("Section not found.");
		}
	}

	@Response(description="The configuration file contained syntax errors and could not be parsed.")
	private class BadConfig extends InternalServerError {
		private static final long serialVersionUID = 1L;

		BadConfig(Exception e) {
			super(e, "The configuration file contained syntax errors and could not be parsed.");
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods
	//-----------------------------------------------------------------------------------------------------------------

	private ObjectMap getSection(String name) throws SectionNotFound, BadConfig {
		ObjectMap m;
		try {
			m = getServletConfig().getConfig().getSectionAsMap(name);
		} catch (ParseException e) {
			throw new BadConfig(e);
		}
		if (m == null)
			throw new SectionNotFound();
		return m;
	}
}
