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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.FileUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.io.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.cp.*;
import org.apache.juneau.rest.server.*;

/**
 * API for retrieving localized static files from either the classpath or file system.
 *
 * <p>
 * Provides the same functionality as {@link BasicFileFinder} but adds support for returning files as {@link HttpResource}
 * objects with arbitrary headers.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/StaticFiles">Static files</a>
 * </ul>
 */
@SuppressWarnings({
	"resource", // Resource management handled externally
	"java:S115", // Constants use UPPER_snakeCase convention (e.g., PROP_headers)
})
public class BasicStaticFiles implements StaticFiles {

	// Property name constants
	private static final String PROP_headers = "headers";

	/**
	 * Creates a new builder for this object.
	 *
	 * @param beanStore The bean store to use for creating beans.
	 * @return A new builder for this object.
	 */
	public static StaticFiles.Builder create(BeanStore beanStore) {
		return new StaticFiles.Builder(beanStore);
	}

	private final HttpHeader[] headers;
	private final MimeTypeDetector mimeTypes;
	private final int hashCode;

	private final FileFinder fileFinder;

	/**
	 * Constructor.
	 *
	 * @param beanStore The bean store containing injectable beans for this static files object. Must not be <jk>null</jk>.
	 */
	public BasicStaticFiles(BeanStore beanStore) {
		// @formatter:off
		this(StaticFiles
			.create(beanStore)
			.dir("static")
			.dir("htdocs")
			.cp(beanStore.getBean(ResourceSupplier.class).orElseThrow(() -> new IllegalStateException("ResourceSupplier not found")).getResourceClass(), "htdocs", true)
			.cp(beanStore.getBean(ResourceSupplier.class).orElseThrow(() -> new IllegalStateException("ResourceSupplier not found")).getResourceClass(), "/htdocs", true)
			.caching(1_000_000)
			.exclude("(?i).*\\.(class|properties)")
			.headers(CacheControl.of("max-age=86400, public"))
		);
		// @formatter:on
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder object. Must not be <jk>null</jk>.
	 */
	public BasicStaticFiles(StaticFiles.Builder builder) {
		this.headers = builder.headers.toArray(new HttpHeader[builder.headers.size()]);
		this.mimeTypes = builder.mimeTypes;
		this.hashCode = h(hashCode(), headers);
		this.fileFinder = builder.fileFinder.build();
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Can be used when subclassing and overriding the {@link #resolve(String, Locale)} method.
	 */
	protected BasicStaticFiles() {
		this.headers = new HttpHeader[0];
		this.mimeTypes = null;
		this.hashCode = h(hashCode(), headers);
		this.fileFinder = null;
	}

	@Override /* Overridden from Object */
	public boolean equals(Object o) {
		return this == o || (o instanceof BasicStaticFiles o2 && eq(this, o2, (x, y) -> Arrays.equals(x.headers, y.headers)));
	}

	@Override /* Overridden from FileFinder */
	public Optional<InputStream> getStream(String name, Locale locale) throws IOException {
		return fileFinder.getStream(name, locale);
	}

	@Override /* Overridden from FileFinder */
	public Optional<String> getString(String name, Locale locale) throws IOException {
		return fileFinder.getString(name, locale);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	/**
	 * Resolve the specified path.
	 *
	 * <p>
	 * Subclasses can override this method to provide specialized handling.
	 *
	 * @param path The path to resolve to a static file.
	 * @param locale Optional locale.
	 * @return The resource, or an empty {@link Optional} if not found.
	 */
	@Override /* Overridden from StaticFiles */
	public Optional<HttpResource> resolve(String path, Locale locale) {
		try {
			Optional<InputStream> is = getStream(path, locale);
			if (! is.isPresent())
				return oe();
			var ct = mimeTypes == null ? null : mimeTypes.getContentType(getFileName(path));
			var hdrs = new ArrayList<HttpHeader>();
			if (ct != null)
				hdrs.add(ContentType.of(ct));
			for (var h : headers)
				if (h != null)
					hdrs.add(h);
			return o(HttpResourceBean.of(StreamBody.of(is.get()), hdrs));
		} catch (IOException e) {
			throw new InternalServerError(e);
		}
	}

	protected FluentMap<String,Object> properties() {
		// @formatter:off
		return filteredBeanPropertyMap()
			.a(PROP_headers, headers);
		// @formatter:on
	}

	@Override /* Overridden from Object */
	public String toString() {
		return r(properties());
	}
}