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
package org.apache.juneau.microservice.examples;

import java.io.*;
import java.util.Map;

import org.apache.juneau.commons.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.classic.response.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.html.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * Shows contents of the microservice configuration file, and updates it.
 *
 * <p>
 * Reads are {@code GET}s.  Writes are {@code PUT}s only, sent by a REST client or from the command line; there is no
 * HTML edit form, because a browser form cannot carry the header-borne token a CSRF boundary requires.  A {@code PUT}
 * cannot be sent cross-site without a CORS preflight, so a boundary in front of this resource (see
 * {@link AdminBoundary}) admits it without a token:
 * <p class='bcode'>
 * 	curl -X PUT -H 'Content-Type: text/plain' --data-binary @my.cfg http://localhost:10000/config
 * 	curl -X PUT -H 'Content-Type: text/plain' -d 'newValue' http://localhost:10000/config/MySection/myKey
 * </p>
 *
 * <p>
 * A {@code GET} returns the whole configuration, including any secrets in it, and this resource has no
 * authentication of its own.  Do not expose it on an untrusted network.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link AdminBoundary}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauMicroservice">juneau-microservice Basics</a>
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
		"stats: servlet:/stats"
	},
	resolveBodyVars="true"
)
@SuppressWarnings({
	"java:S112" // throws Exception intentional - callback/lifecycle method
})
public class ConfigResource extends BasicRestServlet {
	@Response
	@Schema(description = "Section not found.")
	@SuppressWarnings({
		"java:S110" // Inheritance depth acceptable for microservice hierarchy
	})
	private class SectionNotFound extends NotFound {
		private static final long serialVersionUID = 1L;

		/** Constructor. */
		SectionNotFound() {
			super("Section not found.");
		}
	}

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
	/**
	 * Returns the config file contents as a nested map.
	 *
	 * @return The config as a map of maps.
	 */
	public JsonMap getConfig() {
		return getContext().getConfig().toMap();
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
	/**
	 * Returns the value of a config entry for the given section and key.
	 *
	 * @param section Section name in config file.
	 * @param key Key name in section.
	 * @return The entry value.
	 * @throws SectionNotFound If the section is not found.
	 */
	public String getConfigEntry(
			@Path("section") @Schema(d="Section name in config file.") String section,
			@Path("key") @Schema(d="Key name in section.") String key
		) throws SectionNotFound {

		return getSection(section).getString(key);
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
	/**
	 * Returns the contents of a config section as a map.
	 *
	 * @param section Section name in config file.
	 * @return The section as a map.
	 * @throws SectionNotFound If the section is not found.
	 */
	public JsonMap getConfigSection(
			@Path("section") @Schema(d="Section name in config file.") String section
		) throws SectionNotFound {

		return getSection(section);
	}

	@Mutating("Changes the configuration")
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
	/**
	 * Updates the config file from raw text in INI format.
	 *
	 * @param contents New contents in INI file format.
	 * @return The config as a map.
	 * @throws Exception If parsing or loading fails.
	 */
	public JsonMap setConfigContents(
			@Content @Schema(d="New contents in INI file format.") Reader contents
		) throws Exception {

		return getContext().getConfig().load(contents, true).toMap();
	}

	@Mutating("Changes the configuration")
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
	/**
	 * Adds or overwrites a config section.
	 *
	 * @param section Section name in config file.
	 * @param contents New contents of config section as a simple map of key/value pairs.
	 * @return The updated section as a map.
	 * @throws Exception If the update fails.
	 */
	public JsonMap setConfigSection(
			@Path("section") @Schema(d="Section name in config file.") String section,
			@Content @Schema(d="New contents of config section as a simple map of key/value pairs.")
			Map<String,Object> contents
		) throws Exception {

		getContext().getConfig().setSection(section, null, contents);
		return getSection(section);
	}

	@Mutating("Changes the configuration")
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
	/**
	 * Adds or overwrites a config entry value.
	 *
	 * @param section Section name in config file.
	 * @param key Key name in section.
	 * @param value New value for entry.
	 * @return The updated value.
	 * @throws SectionNotFound If the section is not found.
	 */
	public String setConfigValue(
			@Path("section") @Schema(d="Section name in config file.") String section,
			@Path("key") @Schema(d="Key name in section.") String key,
			@Content @Schema(d="New value for entry.") String value
		) throws SectionNotFound {

		getContext().getConfig().set(section + '/' + key, value);
		return getSection(section).getString(key);
	}

	private JsonMap getSection(String name) throws SectionNotFound {
		return getContext().getConfig().getSection(name).asMap().orElseThrow(SectionNotFound::new);
	}
}