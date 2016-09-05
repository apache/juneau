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
import static org.apache.juneau.html.HtmlDocSerializerContext.*;
import static org.apache.juneau.server.annotation.VarCategory.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.ini.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.server.*;
import org.apache.juneau.server.annotation.*;

/**
 * Shows contents of the microservice configuration file.
 */
@RestResource(
	path="/config",
	label="Configuration",
	description="Contents of configuration file.",
	properties={
		@Property(name=HTMLDOC_links, value="{up:'$R{requestParentURI}',options:'$R{servletURI}?method=OPTIONS',edit:'$R{servletURI}/edit'}"),
	}
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
		return getConfig();
	}

	/**
	 * [GET /edit] - Show config file edit page.
	 *
	 * @param req The HTTP request.
	 * @return The config file as a reader resource.
	 * @throws Exception
	 */
	@RestMethod(name="GET", path="/edit", description="Show config file edit page.")
	public ReaderResource getConfigEditPage(RestRequest req) throws Exception {
		// Note that we don't want variables in the config file to be resolved,
		// so we need to escape any $ characters we see.
		req.setAttribute("contents", getConfig().toString().replaceAll("\\$", "\\\\\\$"));
		return req.getReaderResource("ConfigEdit.html", true);
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
		input={
			@Var(category=ATTR, name="section", description="Section name.")
		}
	)
	public ObjectMap getConfigSection(@Attr("section") String section) throws Exception {
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
		input={
			@Var(category=ATTR, name="section", description="Section name."),
			@Var(category=ATTR, name="key", description="Entry name.")
		}
	)
	public String getConfigEntry(@Attr("section") String section, @Attr("key") String key) throws Exception {
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
		input={
			@Var(category=PARAM, name="contents", description="New contents in INI file format.")
		}
	)
	public ConfigFile setConfigContentsFormPost(@Param("contents") String contents) throws Exception {
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
		input={
			@Var(category=CONTENT, description="New contents in INI file format.")
		}
	)
	public ConfigFile setConfigContents(@Content Reader contents) throws Exception {
		ConfigFile cf2 = ConfigMgr.DEFAULT.create().load(contents);
		return getConfig().merge(cf2).save();
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
		input={
			@Var(category=ATTR, name="section", description="Section name."),
			@Var(category=CONTENT, description="New contents for section as a simple map with string keys and values.")
		}
	)
	public ObjectMap setConfigSection(@Attr("section") String section, @Content Map<String,String> contents) throws Exception {
		getConfig().setSection(section, contents);
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
		input={
			@Var(category=ATTR, name="section", description="Section name."),
			@Var(category=ATTR, name="key", description="Entry name."),
			@Var(category=CONTENT, description="New value as a string.")
		}
	)
	public String setConfigSection(@Attr("section") String section, @Attr("key") String key, @Content String value) throws Exception {
		getConfig().put(section, key, value, false);
		return getSection(section).getString(key);
	}

	private ObjectMap getSection(String name) {
		ObjectMap m = getConfig().getSectionMap(name);
		if (m == null)
			throw new RestException(SC_NOT_FOUND, "Section not found.");
		return m;
	}
}
