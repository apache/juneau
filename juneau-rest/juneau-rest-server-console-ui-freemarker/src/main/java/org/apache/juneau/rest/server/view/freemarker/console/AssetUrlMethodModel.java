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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import org.apache.juneau.commons.utils.*;

import freemarker.template.*;

/**
 * The {@code assetUrl(path)} FreeMarker method: appends {@code ?v=<crc32>} of an adopter's own bundled static asset
 * to its URL path, so a browser cannot keep a stale copy after a deploy.
 *
 * <p>
 * Registered by {@link ConsoleFreemarkerMixin} when the builder is given
 * {@link ConsoleFreemarkerMixin.Builder#adopterAssets(ClassLoader, String)}. The token is the same
 * {@link ChecksumUtils#hash8(byte[]) CRC32 hash} Juneau uses for its own assets, computed once per path and cached.
 * A path that already has a query string, is not context-root-absolute (e.g. an external URL), or names no bundled
 * resource is returned unchanged; the last case also logs a warning once per path when dev mode is on.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	&lt;script src="${assetUrl('/js/app.js')}"&gt;&lt;/script&gt;   &lt;#-- /js/app.js?v=1a2b3c4d --&gt;
 * </p>
 *
 * @since 10.0.0
 */
public final class AssetUrlMethodModel implements TemplateMethodModelEx {

	/** The shared-variable name this method registers under. */
	public static final String NAME = "assetUrl";

	private static final Logger LOG = Logger.getLogger(AssetUrlMethodModel.class.getName());

	private final ClassLoader loader;
	private final String root;
	private final boolean devMode;
	private final Map<String,String> tokens = new ConcurrentHashMap<>();

	AssetUrlMethodModel(ClassLoader loader, String resourceRoot, boolean devMode) {
		this.loader = loader;
		var r = resourceRoot.startsWith("/") ? resourceRoot.substring(1) : resourceRoot;
		this.root = r.isEmpty() || r.endsWith("/") ? r : r + "/";
		this.devMode = devMode;
	}

	@Override
	public Object exec(@SuppressWarnings("rawtypes") List args) throws TemplateModelException {
		if (args.size() != 1)
			throw FtlAttrLists.reject(f("%s(path) takes exactly one argument; got '%s'.", NAME, args.size()));
		return versioned(String.valueOf(args.get(0)));
	}

	/**
	 * Returns {@code path} with {@code ?v=<crc32>} appended, or unchanged when it cannot be versioned.
	 *
	 * @param path A context-root-absolute path such as {@code /js/app.js}.
	 * @return The versioned URL.
	 */
	String versioned(String path) {
		if (path.contains("?") || ! path.startsWith("/"))
			return path;
		var token = tokens.computeIfAbsent(path, this::token);
		return token.isEmpty() ? path : path + "?v=" + token;
	}

	private String token(String path) {
		var resource = root + path.substring(1);
		try (var in = loader.getResourceAsStream(resource)) {
			if (nn(in))
				return ChecksumUtils.hash8(in.readAllBytes());
		} catch (IOException e) {
			LOG.log(Level.WARNING, e, () -> "assetUrl: cannot read bundled resource '" + resource + "'.");
			return "";
		}
		if (devMode)
			LOG.warning("assetUrl: no bundled resource '" + resource + "' for '" + path + "'; serving it unversioned.");
		return "";
	}
}
