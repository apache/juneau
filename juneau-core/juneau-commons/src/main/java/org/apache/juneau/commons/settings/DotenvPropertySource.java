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
package org.apache.juneau.commons.settings;

import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Property source backed by a dotenv file.
 */
public class DotenvPropertySource implements PropertySource {

	private static final String DEFAULT_PATH = ".env";
	private static final String DOTENV_PATH_PROP = "juneau.dotenv.path";
	private static final String DOTENV_PATH_ENV = "JUNEAU_DOTENV_PATH";

	private final AtomicReference<Map<String,String>> map = new AtomicReference<>();
	private final Path path;

	/**
	 * Constructor with default path discovery.
	 */
	public DotenvPropertySource() {
		this(resolvePath());
	}

	/**
	 * Constructor.
	 *
	 * @param path Dotenv path.
	 */
	public DotenvPropertySource(Path path) {
		this.path = path;
	}

	private static Path resolvePath() {
		var configured = System.getProperty(DOTENV_PATH_PROP);
		if (configured == null || configured.isEmpty())
			configured = System.getenv(DOTENV_PATH_ENV);
		if (configured == null || configured.isEmpty())
			configured = DEFAULT_PATH;
		return Paths.get(configured);
	}

	@Override
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for dotenv property lookup and parsing logic
	})
	public PropertyLookupResult get(String name) {
		var values = map.updateAndGet(existing -> existing != null ? existing : load(path));
		var value = values.get(name);
		return value == null ? PropertyLookupResult.missing() : PropertyLookupResult.present(opt(value));
	}

	@SuppressWarnings({
		"java:S3776" // line-oriented dotenv parsing intentionally keeps all validation branches in one place.
	})
	private static Map<String,String> load(Path path) {
		if (path == null || ! Files.exists(path))
			return Collections.emptyMap();
		var m = new LinkedHashMap<String,String>();
		try (var r = Files.newBufferedReader(path)) {
			String line;
			while ((line = r.readLine()) != null) {
				line = line.trim();
				if (! (line.isEmpty() || line.startsWith("#"))) {
					var i = line.indexOf('=');
					if (i > 0) {
						var key = line.substring(0, i).trim();
						var value = line.substring(i + 1).trim();
						if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")))
							value = value.substring(1, value.length() - 1);
						m.put(key, value);
					}
				}
			}
		} catch (@SuppressWarnings("unused") IOException unused) {
			return Collections.emptyMap();
		}
		return m;
	}
}
