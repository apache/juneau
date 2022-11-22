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
package org.apache.juneau.rest.staticfile;

import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.http.HttpResources.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.FileUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;

import java.io.*;
import java.util.*;

import javax.activation.*;

import org.apache.http.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.*;

/**
 * API for retrieving localized static files from either the classpath or file system.
 *
 * <p>
 * Provides the same functionality as {@link BasicFileFinder} but adds support for returning files as {@link HttpResource}
 * objects with arbitrary headers.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.StaticFiles">Static files</a>
 * </ul>
 */
public class BasicStaticFiles implements StaticFiles {

	private final Header[] headers;
	private final MimetypesFileTypeMap mimeTypes;
	private final int hashCode;
	private final FileFinder fileFinder;

	/**
	 * Creates a new builder for this object.
	 *
	 * @param beanStore The bean store to use for creating beans.
	 * @return A new builder for this object.
	 */
	public static StaticFiles.Builder create(BeanStore beanStore) {
		return new StaticFiles.Builder(beanStore);
	}

	/**
	 * Constructor.
	 *
	 * @param beanStore The bean store containing injectable beans for this logger.
	 */
	public BasicStaticFiles(BeanStore beanStore) {
		this(StaticFiles
			.create(beanStore)
			.type(BasicStaticFiles.class)
			.dir("static")
			.dir("htdocs")
			.cp(beanStore.getBean(ResourceSupplier.class).get().getResourceClass(), "htdocs", true)
			.cp(beanStore.getBean(ResourceSupplier.class).get().getResourceClass(), "/htdocs", true)
			.caching(1_000_000)
			.exclude("(?i).*\\.(class|properties)")
			.headers(cacheControl("max-age=86400, public"))
		);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder object.
	 */
	public BasicStaticFiles(StaticFiles.Builder builder) {
		this.headers = builder.headers.toArray(new Header[builder.headers.size()]);
		this.mimeTypes = builder.mimeTypes;
		this.hashCode = HashCode.of(hashCode(), headers);
		this.fileFinder = builder.fileFinder.build();
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Can be used when subclassing and overriding the {@link #resolve(String, Locale)} method.
	 */
	protected BasicStaticFiles() {
		super();
		this.headers = new Header[0];
		this.mimeTypes = null;
		this.hashCode = HashCode.of(hashCode(), headers);
		this.fileFinder = null;
	}

	/**
	 * Resolve the specified path.
	 *
	 * <p>
	 * Subclasses can override this method to provide specialized handling.
	 *
	 * @param path The path to resolve to a static file.
	 * @param locale Optional locale.
	 * @return The resource, or <jk>null</jk> if not found.
	 */
	@Override /* StaticFiles */
	public Optional<HttpResource> resolve(String path, Locale locale) {
		try {
			Optional<InputStream> is = getStream(path, locale);
			if (! is.isPresent())
				return empty();
			return optional(
				streamResource(is.get())
					.setHeaders(contentType(mimeTypes == null ? null : mimeTypes.getContentType(getFileName(path))))
					.addHeaders(headers)
			);
		} catch (IOException e) {
			throw new InternalServerError(e);
		}
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return super.equals(o) && o instanceof BasicStaticFiles && eq(this, (BasicStaticFiles)o, (x,y)->eq(x.headers, y.headers));
	}

	@Override /* FileFinder */
	public Optional<InputStream> getStream(String name, Locale locale) throws IOException {
		return fileFinder.getStream(name, locale);
	}

	@Override /* FileFinder */
	public Optional<String> getString(String name, Locale locale) throws IOException {
		return fileFinder.getString(name, locale);
	}

	@Override /* Object */
	public String toString() {
		return filteredMap()
			.append("headers", headers)
			.asReadableString();
	}
}
