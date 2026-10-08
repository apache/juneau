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
package org.apache.juneau.rest.server.staticfile;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.rest.server.*;

/**
 * Resolves a WebJar file to a cache-busted URL under the {@link WebJarsMixin} mount.
 *
 * <p>
 * An asset is named {@code groupId:artifactId:path} (see {@link #asset(String, String, String)}).  The path is
 * relative to {@code META-INF/resources/webjars/} and may contain {@link #VERSION_TOKEN}, which is replaced with the
 * version read from the WebJar's own {@code META-INF/maven/<groupId>/<artifactId>/pom.properties}.  The URL gets a
 * {@code ?v=<version>} cache-buster, so a version bump in the app's pom changes every URL.
 *
 * <p>
 * A missing WebJar, or a missing file in a present WebJar, throws {@link IllegalStateException}, so a page never
 * renders a tag that would 404.  Versions and file checks are cached for the life of the class loader.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	String <jv>asset</jv> = WebJarResolver.<jsm>asset</jsm>(<js>"org.webjars"</js>, <js>"jquery"</js>, <js>"jquery/{version}/jquery.min.js"</js>);
 * 	String <jv>url</jv> = WebJarResolver.<jsm>resolve</jsm>(<jv>req</jv>, <jv>asset</jv>);  <jc>// .../webjars/jquery/3.7.1/jquery.min.js?v=3.7.1</jc>
 * </p>
 *
 * @since 10.0.0
 */
public final class WebJarResolver {

	/** Classpath root of every WebJar's files. */
	public static final String WEBJARS_ROOT = "META-INF/resources/webjars/";

	/** Placeholder in an asset path that is replaced with the WebJar's version. */
	public static final String VERSION_TOKEN = "{version}";

	private static final ClassLoader LOADER = WebJarResolver.class.getClassLoader();
	private static final Map<String,String> VERSIONS = new ConcurrentHashMap<>();
	private static final Map<String,Boolean> PRESENT = new ConcurrentHashMap<>();

	private WebJarResolver() {}

	/**
	 * Builds an asset id.
	 *
	 * @param groupId The WebJar group id, e.g. {@code org.webjars}.
	 * @param artifactId The WebJar artifact id, e.g. {@code jquery}.
	 * @param path The file path under {@code META-INF/resources/webjars/}, optionally containing {@link #VERSION_TOKEN}.
	 * @return The asset id {@code groupId:artifactId:path}.
	 */
	public static String asset(String groupId, String artifactId, String path) {
		return groupId + ":" + artifactId + ":" + path;
	}

	/**
	 * Returns the version of a WebJar on the classpath.
	 *
	 * @param groupId The WebJar group id.
	 * @param artifactId The WebJar artifact id.
	 * @return The version from the WebJar's {@code pom.properties}.
	 * @throws IllegalStateException If the WebJar isn't on the classpath or names no version.
	 */
	public static String version(String groupId, String artifactId) {
		var key = groupId + ":" + artifactId;
		var v = VERSIONS.get(key);
		if (v == null) {
			v = readVersion(groupId, artifactId);
			VERSIONS.put(key, v);
		}
		return v;
	}

	/**
	 * Resolves an asset id to its file path under {@code META-INF/resources/webjars/}, checking that the file exists.
	 *
	 * @param asset The asset id (see {@link #asset(String, String, String)}).
	 * @return The path with {@link #VERSION_TOKEN} filled in.
	 * @throws IllegalArgumentException If the asset id is malformed.
	 * @throws IllegalStateException If the WebJar or the file isn't on the classpath.
	 */
	public static String resolvePath(String asset) {
		var a = parse(asset);
		var version = version(a[0], a[1]);
		var path = a[2].replace(VERSION_TOKEN, version);
		if (! PRESENT.computeIfAbsent(path, k -> LOADER.getResource(WEBJARS_ROOT + k) != null))
			throw new IllegalStateException(String.format("needs WebJar file '%s' (%s:%s:%s), which is not on the classpath.", path, a[0], a[1], version));
		return path;
	}

	/**
	 * Resolves an asset id to an absolute, cache-busted URL under the {@link WebJarsMixin} mount of the request's servlet.
	 *
	 * @param req The in-flight request.
	 * @param asset The asset id (see {@link #asset(String, String, String)}).
	 * @return The URL.
	 */
	public static String resolve(RestRequest req, String asset) {
		var a = parse(asset);
		var path = resolvePath(asset);
		return req.getUriResolver().resolve("servlet:" + WebJarsMixin.PATH + "/" + path) + "?v=" + version(a[0], a[1]);
	}

	private static String readVersion(String groupId, String artifactId) {
		var props = "META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
		try (var in = LOADER.getResourceAsStream(props)) {
			if (in == null)
				throw new IllegalStateException(String.format("needs %s:%s on the classpath (no %s).", groupId, artifactId, props));
			var p = new Properties();
			p.load(in);
			var v = p.getProperty("version", "").trim();
			if (v.isEmpty())
				throw new IllegalStateException(String.format("needs %s:%s on the classpath (%s has no version).", groupId, artifactId, props));
			return v;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static String[] parse(String asset) {
		var a = asset == null ? new String[0] : asset.split(":", 3);
		if (a.length != 3 || a[0].isBlank() || a[1].isBlank() || a[2].isBlank())
			throw new IllegalArgumentException(String.format("WebJar asset must be 'groupId:artifactId:path'; got '%s'.", asset));
		return a;
	}
}
