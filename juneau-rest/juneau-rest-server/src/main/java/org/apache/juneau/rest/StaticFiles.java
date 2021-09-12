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

import java.nio.file.*;
import java.util.*;

import javax.activation.*;

import org.apache.http.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.utils.*;

/**
 * API for retrieving localized static files from either the classpath or file system.
 */
public interface StaticFiles extends FileFinder {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Represents no static files */
	public abstract class Null implements StaticFiles {}

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder create() {
		return new Builder();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends FileFinder.Builder {

		List<Header> headers;
		MimetypesFileTypeMap mimeTypes;

		/**
		 * Constructor.
		 */
		protected Builder() {
			headers = AList.create();
			mimeTypes = new ExtendedMimetypesFileTypeMap();
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder being copied.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			headers = AList.of(copyFrom.headers);
			mimeTypes = copyFrom.mimeTypes;
		}

		@Override
		public StaticFiles build() {
			return (StaticFiles)super.build();
		}

		@Override
		protected Class<? extends FileFinder> getDefaultType() {
			return BasicStaticFiles.class;
		}

		/**
		 * Appends headers to add to HTTP responses.
		 *
		 * <p>
		 * Can be called multiple times to add multiple headers.
		 *
		 * @param headers The headers to add.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder headers(Header...headers) {
			this.headers.addAll(Arrays.asList(headers));
			return this;
		}

		/**
		 * Prepend the MIME type values to the MIME types registry.
		 *
		 * @param mimeTypes A .mime.types formatted string of entries.  See {@link MimetypesFileTypeMap#addMimeTypes(String)}.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder addMimeTypes(String mimeTypes) {
			this.mimeTypes.addMimeTypes(mimeTypes);
			return this;
		}

		/**
		 * Replaces the MIME types registry used for determining content types.
		 *
		 * @param mimeTypes The new MIME types registry.
		 * @return This object (for method chaining).
		 */
		@FluentSetter
		public Builder mimeTypes(MimetypesFileTypeMap mimeTypes) {
			this.mimeTypes = mimeTypes;
			return this;
		}

		@Override
		public Builder copy() {
			return new Builder(this);
		}

		// <FluentSetters>

		@Override /* GENERATED - FileFinderBuilder */
		public Builder beanStore(BeanStore value) {
			super.beanStore(value);
			return this;
		}

		@Override /* GENERATED - FileFinderBuilder */
		public Builder caching(long cachingLimit) {
			super.caching(cachingLimit);
			return this;
		}

		@Override /* GENERATED - FileFinderBuilder */
		public Builder cp(Class<?> c, String path, boolean recursive) {
			super.cp(c, path, recursive);
			return this;
		}

		@Override /* GENERATED - FileFinderBuilder */
		public Builder dir(String path) {
			super.dir(path);
			return this;
		}

		@Override /* GENERATED - FileFinderBuilder */
		public Builder exclude(String...patterns) {
			super.exclude(patterns);
			return this;
		}

		@Override /* GENERATED - FileFinderBuilder */
		public Builder type(Class<? extends org.apache.juneau.cp.FileFinder> value) {
			super.type(value);
			return this;
		}

		@Override /* GENERATED - FileFinderBuilder */
		public Builder include(String...patterns) {
			super.include(patterns);
			return this;
		}

		@Override /* GENERATED - FileFinderBuilder */
		public Builder path(Path path) {
			super.path(path);
			return this;
		}

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Resolve the specified path.
	 *
	 * @param path The path to resolve to a static file.
	 * @param locale Optional locale.
	 * @return The resource, or <jk>null</jk> if not found.
	 */
	public Optional<HttpResource> resolve(String path, Locale locale);
}
