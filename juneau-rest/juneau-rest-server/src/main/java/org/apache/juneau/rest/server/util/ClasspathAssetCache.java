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
package org.apache.juneau.rest.server.util;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.util.concurrent.*;

import org.apache.juneau.commons.utils.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.resource.*;

/**
 * A themeable/configurable asset-serving mixin's cache of classpath-shipped assets: reads each configured
 * resource's bytes once, computes and caches its {@link ChecksumUtils#hash8(byte[]) content hash}, and wraps
 * bytes as a cacheable {@link HttpResource} - the read+cache+hash+cache-buster-URL+serve bundle that
 * {@code ConsoleChromeMixin} (juneau-rest-server-console-ui) and {@code ViewsMixin} (juneau-rest-server-views)
 * previously each hand-rolled as their own private statics and static cache maps.
 *
 * <h5 class='section'>Version-anchor:</h5>
 * <p>
 * {@link #buildVersion()} resolves {@link Package#getImplementationVersion()} from the constructor-supplied
 * <b>anchor</b> class, not from this class's own class/package. A mixin composes one {@link ClasspathAssetCache}
 * per mixin class (mirroring the static caches this replaces) and anchors it on <b>its own</b> class, so the
 * resolved implementation version - and therefore every {@code ?v=<buildVersion>-<hash8>} cache-buster URL this
 * cache builds - matches what that mixin's own jar shipped with, exactly as it did before extraction. Anchoring
 * on this class instead would resolve {@code juneau-rest-server}'s own implementation version for every
 * consuming mixin, silently changing every consumer's cache-buster URLs.
 *
 * <h5 class='section'>Thread-safety:</h5>
 * <p>
 * Backed by {@link ConcurrentHashMap}s; safe to share across concurrent requests. Classpath-shipped resources
 * are assumed immutable for the lifetime of the JVM, so bytes and hashes are computed at most once per resource.
 *
 * @since 10.0.0
 */
public class ClasspathAssetCache {

	private final Class<?> anchor;
	private final ConcurrentHashMap<String,byte[]> byteCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String,String> hashCache = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param anchor
	 * 	The class that anchors both classpath resource resolution ({@link Class#getResourceAsStream(String)}) and
	 * 	{@link #buildVersion()}'s {@link Package#getImplementationVersion()} resolution. Typically the owning
	 * 	mixin's own class (see the class javadoc's version-anchor section). Must not be <jk>null</jk>.
	 */
	public ClasspathAssetCache(Class<?> anchor) {
		this.anchor = rnn(anchor);
	}

	/** Test-only accessor for the constructor-supplied anchor class. */
	Class<?> anchor() {
		return anchor;
	}

	/**
	 * Reads (and caches) the given classpath resource's bytes, resolved relative to this cache's anchor class.
	 *
	 * @param classpathResource A classpath-root-absolute resource path (e.g. {@code "/org/apache/juneau/foo/bar.js"}).
	 * 	Must not be <jk>null</jk> and must resolve to an existing resource on the anchor's classpath.
	 * @return The resource's bytes (served from cache on every call after the first for that resource).
	 */
	public byte[] bytes(String classpathResource) {
		return byteCache.computeIfAbsent(classpathResource, this::read);
	}

	private byte[] read(String classpathResource) {
		try (var in = anchor.getResourceAsStream(classpathResource)) {
			if (in == null)
				throw new IOException("Classpath resource not found: " + classpathResource);
			return IoUtils.readBytes(in);
		} catch (IOException e) {  // HTT: unreachable - callers only pass already-validated, jar-shipped classpath resources.
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Computes (and caches) the given classpath resource's 8-hex-char content hash.
	 *
	 * @param classpathResource A classpath-root-absolute resource path.  Same constraints as {@link #bytes(String)}.
	 * @return The resource's {@link ChecksumUtils#hash8(byte[])} content hash.
	 */
	public String hash(String classpathResource) {
		return hashCache.computeIfAbsent(classpathResource, r -> ChecksumUtils.hash8(bytes(r)));
	}

	/**
	 * Resolves the framework build version for asset cache-busting from this cache's anchor class's package,
	 * falling back to {@code "dev"} when unset (e.g. running from an IDE/test classpath rather than a packaged
	 * jar) - see the class javadoc's version-anchor section.
	 *
	 * @return The anchor's {@link Package#getImplementationVersion()}, or {@code "dev"} if unset.
	 */
	public String buildVersion() {
		var v = anchor.getPackage().getImplementationVersion();
		return v == null ? "dev" : v;  // HTT: the non-null branch only fires when running from a packaged jar with a manifest Implementation-Version - unreachable when tests run against unpackaged target/classes.
	}

	/**
	 * Builds the {@code ?v=<buildVersion>-<hash8>} content-sensitive cache-buster query suffix for the given
	 * classpath resource, suitable for appending to that resource's served URL.
	 *
	 * @param classpathResource A classpath-root-absolute resource path.  Same constraints as {@link #bytes(String)}.
	 * @return The {@code "?v=" + buildVersion() + "-" + hash(classpathResource)} suffix.
	 */
	public String cacheBuster(String classpathResource) {
		return "?v=" + buildVersion() + "-" + hash(classpathResource);
	}

	/**
	 * Wraps pre-computed bytes as a cacheable {@link HttpResource} carrying the given content type and
	 * {@code Cache-Control} header.
	 *
	 * @param bytes The response body bytes.  Must not be <jk>null</jk>.
	 * @param contentType The {@code Content-Type} header value (e.g. {@code "text/css;charset=utf-8"}).
	 * @param cacheControl The {@code Cache-Control} header value (e.g. {@code "max-age=86400, public"}).
	 * @return The wrapped, cacheable resource.
	 */
	public HttpResource wrap(byte[] bytes, String contentType, String cacheControl) {
		return HttpResourceBean.of(
			ByteArrayBody.of(bytes, contentType),
			list(ContentType.of(contentType), CacheControl.of(cacheControl)));
	}

	/**
	 * Reads (and caches) the given classpath resource and wraps it as a cacheable {@link HttpResource} - the
	 * {@link #bytes(String)} + {@link #wrap(byte[],String,String)} bundle every classpath-asset-serving endpoint
	 * needs.
	 *
	 * @param classpathResource A classpath-root-absolute resource path.  Same constraints as {@link #bytes(String)}.
	 * @param contentType The {@code Content-Type} header value.
	 * @param cacheControl The {@code Cache-Control} header value.
	 * @return The wrapped, cacheable resource.
	 */
	public HttpResource serve(String classpathResource, String contentType, String cacheControl) {
		return wrap(bytes(classpathResource), contentType, cacheControl);
	}
}
