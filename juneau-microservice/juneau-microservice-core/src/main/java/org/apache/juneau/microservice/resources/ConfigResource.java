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

import java.io.*;
import java.util.Map;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.dto.html5.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Content;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;

/**
 * Shows contents of the microservice configuration file.
 *
 * <ul class='seealso'>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-microservice-core">juneau-microservice-core</a>
 * </ul>
 *
 * @serial exclude
 */
@Rest(
	path="/config",
	title="Configuration",
	description="Contents of configuration file."
)
@HtmlDocConfig(
	navlinks={
		"up: request:/..",
		"api: servlet:/api",
		"stats: servlet:/stats",
		"edit: servlet:/edit"
	}
)
@SuppressWarnings("javadoc")
public class ConfigResource extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	@RestGet(
		path="/",
		summary="Get config file contents",
		description="Show contents of config file as a JsonMap.",
		swagger=@OpSwagger(
			responses={
				"200:{ description:'Config file as a map of map of objects.', example:{'':{defaultKey:'defaultValue'},'Section1':{key1:'val1',key2:123}}}"
			}
		)
	)
	public JsonMap getConfig() {
		return getContext().getConfig().toMap();
	}

	@RestGet(
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
							.text(getContext().getConfig().toString()))
					)
				)
			)
		);
	}

	@RestGet(
		path="/{section}",
		summary="Get config file section contents",
		description="Show contents of config file section as a JsonMap.",
		swagger=@OpSwagger(
			responses={
				"200:{ description:'Config file section as a map of objects.', example:{key1:'val1',key2:123}}"
			}
		)
	)
	public JsonMap getConfigSection(
			@Path("section") @Schema(d="Section name in config file.") String section
		) throws SectionNotFound {

		return getSection(section);
	}

	@RestGet(
		path="/{section}/{key}",
		summary="Get config file entry value",
		description="Show value of config file entry as a simple string.",
		swagger=@OpSwagger(
			responses={
				"200:{ description:'Entry value.', example:'servlet:/htdocs/themes/dark.css'}"
			}
		)
	)
	public String getConfigEntry(
			@Path("section") @Schema(d="Section name in config file.") String section,
			@Path("key") @Schema(d="Key name in section.") String key
		) throws SectionNotFound {

		return getSection(section).getString(key);
	}

	@RestPost(
		path="/",
		summary="Update config file contents",
		description="Update the contents of the config file from a FORM post.",
		swagger=@OpSwagger(
			responses={
				"200:{ description:'Config file section as a map of objects.', example:{key1:'val1',key2:123}}"
			}
		)
	)
	public JsonMap setConfigContentsFormPost(
			@FormData("contents") @Schema(d="New contents in INI file format.") String contents
		) throws Exception {

		return setConfigContents(new StringReader(contents));
	}

	@RestPut(
		path="/",
		summary="Update config file contents",
		description="Update the contents of the config file from raw text.",
		swagger=@OpSwagger(
			responses={
				"200:{ description:'Config file section as a map of objects.', example:{key1:'val1',key2:123}}"
			}
		)
	)
	public JsonMap setConfigContents(
			@Content @Schema(d="New contents in INI file format.") Reader contents
		) throws Exception {

		return getContext().getConfig().load(contents, true).toMap();
	}

	@RestPut(
		path="/{section}",
		summary="Update config section contents",
		description="Add or overwrite a config file section.",
		swagger=@OpSwagger(
			responses={
				"200:{ description:'Config file section as a map of objects.', example:{key1:'val1',key2:123}}"
			}
		)
	)
	public JsonMap setConfigSection(
			@Path("section") @Schema(d="Section name in config file.") String section,
			@Content @Schema(d="New contents of config section as a simple map of key/value pairs.")
			Map<String,Object> contents
		) throws Exception {

		getContext().getConfig().setSection(section, null, contents);
		return getSection(section);
	}

	@RestPut(
		path="/{section}/{key}",
		summary="Update config entry value",
		description="Add or overwrite a config file entry.",
		swagger=@OpSwagger(
			responses={
				"200:{ description:'The updated value.', example:'servlet:/htdocs/themes/dark.css'}"
			}
		)
	)
	public String setConfigValue(
			@Path("section") @Schema(d="Section name in config file.") String section,
			@Path("key") @Schema(d="Key name in section.") String key,
			@Content @Schema(d="New value for entry.") String value
		) throws SectionNotFound {

		getContext().getConfig().set(section + '/' + key, value);
		return getSection(section).getString(key);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper beans
	//-----------------------------------------------------------------------------------------------------------------

	@Response @Schema(description="Section not found.")
	private class SectionNotFound extends NotFound {
		private static final long serialVersionUID = 1L;

		SectionNotFound() {
			super("Section not found.");
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods
	//-----------------------------------------------------------------------------------------------------------------

	private JsonMap getSection(String name) throws SectionNotFound {
		return getContext().getConfig().getSection(name).asMap().orElseThrow(SectionNotFound::new);
	}
}
