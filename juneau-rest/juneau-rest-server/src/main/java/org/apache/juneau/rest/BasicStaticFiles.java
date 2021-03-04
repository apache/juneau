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
package org.apache.juneau.rest;

import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.internal.FileUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;

import java.io.*;
import java.util.*;

import javax.activation.*;

import org.apache.http.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.internal.*;

/**
 * API for retrieving localized static files from either the classpath or file system.
 *
 * <p>
 * Provides the same functionality as {@link BasicFileFinder} but adds support for returning files as {@link BasicHttpResource}
 * objects with arbitrary headers.
 */
public class BasicStaticFiles extends BasicFileFinder implements StaticFiles {

	private final Header[] headers;
	private final MimetypesFileTypeMap mimeTypes;
	private final int hashCode;

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder for this object.
	 */
	public static StaticFilesBuilder create() {
		return new StaticFilesBuilder();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder object.
	 */
	public BasicStaticFiles(StaticFilesBuilder builder) {
		super(builder);
		this.headers = builder.headers.toArray(new Header[builder.headers.size()]);
		this.mimeTypes = builder.mimeTypes;
		this.hashCode = HashCode.of(hashCode(), headers);
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
	public Optional<BasicHttpResource> resolve(String path, Locale locale) {
		try {
			Optional<InputStream> is = getStream(path);
			if (! is.isPresent())
				return Optional.empty();
			return Optional.of(
				BasicHttpResource
					.of(is.get())
					.header(contentType(mimeTypes == null ? null : mimeTypes.getContentType(getFileName(path))))
					.headers(headers)
			);
		} catch (IOException e) {
			throw new InternalServerError(e);
		}
	}

	@Override /* FileFinder */
	public OMap toMap() {
		return super.toMap()
			.a("headers", headers)
		;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return super.equals(o) && o instanceof BasicStaticFiles && eq(this, (BasicStaticFiles)o, (x,y)->eq(x.headers, y.headers));
	}
}
