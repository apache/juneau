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
import org.apache.juneau.internal.*;
import org.apache.juneau.utils.*;

/**
 * Builder class for {@link StaticFiles} objects.
 */
public class StaticFilesBuilder extends FileFinderBuilder {

	List<Header> headers = AList.of();
	MimetypesFileTypeMap mimeTypes = new ExtendedMimetypesFileTypeMap();

	/**
	 * Create a new {@link StaticFiles} using this builder.
	 *
	 * @return A new {@link StaticFiles}
	 */
	@Override
	public StaticFiles build() {
		return new StaticFiles(this);
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
	public StaticFilesBuilder headers(Header...headers) {
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
	public StaticFilesBuilder addMimeTypes(String mimeTypes) {
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
	public StaticFilesBuilder mimeTypes(MimetypesFileTypeMap mimeTypes) {
		this.mimeTypes = mimeTypes;
		return this;
	}

	// <FluentSetters>

	@Override
	public StaticFilesBuilder cp(Class<?> c, String path, boolean recursive) {
		super.cp(c, path, recursive);
		return this;
	}

	@Override
	public StaticFilesBuilder dir(String path) {
		super.dir(path);
		return this;
	}

	@Override
	public StaticFilesBuilder path(Path path) {
		super.path(path);
		return this;
	}

	@Override
	public StaticFilesBuilder caching(long cachingLimit) {
		super.caching(cachingLimit);
		return this;
	}

	@Override
	public StaticFilesBuilder include(String...patterns) {
		super.include(patterns);
		return this;
	}

	@Override
	public StaticFilesBuilder exclude(String...patterns) {
		super.exclude(patterns);
		return this;
	}

	// </FluentSetters>
}
