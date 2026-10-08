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

import static java.nio.charset.StandardCharsets.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.util.*;

/**
 * Mixin that serves WebJar files from the classpath at {@code /webjars/*}.
 *
 * <p>
 * Compose it into the resource that renders console pages, next to {@code DataTablesMixin}:
 * {@code @Rest(mixins={FreemarkerMixin.class, ViewsMixin.class, DataTablesMixin.class, WebJarsMixin.class})}.
 * The console toolkit packs ({@code jquery}, {@code datatables}, {@code datatables-buttons}) resolve their URLs
 * through {@link WebJarResolver} to this mount; a host that doesn't mount it renders the page, but those URLs 404.
 *
 * <p>
 * Only files under {@code META-INF/resources/webjars/} are served, and only with a known extension ({@code js},
 * {@code css}, {@code map}, {@code woff2}, {@code woff}, {@code ttf}, {@code svg}, {@code png}).  The remainder path
 * is checked as delivered and again after one round of percent-decoding, and is never normalized: a {@code .} or
 * {@code ..} segment, an empty segment, a leading {@code /}, a backslash or an encoded separator returns 404, as does
 * a directory or a missing file.  Responses carry {@code Cache-Control: public, max-age=31536000, immutable}; the
 * URLs are cache-busted by {@link WebJarResolver}.
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 * <p>
 * The mount path is pinned at the op level by {@code @RestGet(path="/webjars/*")}; a class-level
 * {@code @Rest(paths=...)} would be ignored under the mixin pattern.
 *
 * @since 10.0.0
 */
@Rest
public class WebJarsMixin {

	/** The mount path, relative to the host. */
	public static final String PATH = "/webjars";

	/** {@code Cache-Control} for every served file. */
	static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

	private static final Map<String,String> CONTENT_TYPES = Map.of(
		"js", "text/javascript;charset=utf-8",
		"css", "text/css;charset=utf-8",
		"map", "application/json",
		"woff2", "font/woff2",
		"woff", "font/woff",
		"ttf", "font/ttf",
		"svg", "image/svg+xml",
		"png", "image/png"
	);

	private static final ClasspathAssetCache ASSETS = new ClasspathAssetCache(WebJarsMixin.class);

	/**
	 * [GET /webjars/*] &mdash; serve one WebJar file.
	 *
	 * @param req The current request.
	 * @return The file.
	 * @throws NotFound If the path fails a check, has an unknown extension, or names no file.
	 */
	@RestGet(path=PATH + "/*", summary="WebJar files", swagger=@OpSwagger(ignore=true))
	public HttpResource getWebJarFile(RestRequest req) {
		var path = checkedPath(req.getPathParams().getRemainderUndecoded().asString().orElse(null));
		var type = path == null ? null : contentType(path);
		if (type == null || WebJarsMixin.class.getResource("/" + WebJarResolver.WEBJARS_ROOT + path) == null)
			throw new NotFound();
		return ASSETS.serve("/" + WebJarResolver.WEBJARS_ROOT + path, type, CACHE_CONTROL);
	}

	/**
	 * [HEAD /webjars/*] &mdash; the {@code GET} headers without the body (the response processor drops the body).
	 *
	 * @param req The current request.
	 * @return The file.
	 * @throws NotFound If the path fails a check, has an unknown extension, or names no file.
	 */
	@RestOp(method="HEAD", path=PATH + "/*", summary="WebJar files (HEAD)", swagger=@OpSwagger(ignore=true))
	public HttpResource headWebJarFile(RestRequest req) {
		return getWebJarFile(req);
	}

	/**
	 * Checks a raw (undecoded) remainder path, then its percent-decoded form.
	 *
	 * @param raw The raw remainder path, or <jk>null</jk>.
	 * @return The decoded path, or <jk>null</jk> if either form fails a check.
	 */
	static String checkedPath(String raw) {
		if (raw == null || ! safe(raw))
			return null;
		String decoded;
		try {
			decoded = URLDecoder.decode(raw.replace("+", "%2B"), UTF_8);
		} catch (IllegalArgumentException e) {
			return null;
		}
		return safe(decoded) ? decoded : null;
	}

	/**
	 * Returns the content type for a path's extension, compared case-insensitively.
	 *
	 * @param path The path.
	 * @return The content type, or <jk>null</jk> if the extension isn't served.
	 */
	static String contentType(String path) {
		var name = path.substring(path.lastIndexOf('/') + 1);
		var dot = name.lastIndexOf('.');
		return dot < 0 ? null : CONTENT_TYPES.get(name.substring(dot + 1).toLowerCase(Locale.ROOT));
	}

	private static boolean safe(String p) {
		if (p.isEmpty() || p.startsWith("/") || p.indexOf('\\') >= 0)
			return false;
		var lower = p.toLowerCase(Locale.ROOT);
		if (lower.contains("%2f") || lower.contains("%5c"))
			return false;
		for (var seg : p.split("/", -1))
			if (seg.isEmpty() || ".".equals(seg) || "..".equals(seg))
				return false;
		return true;
	}
}
