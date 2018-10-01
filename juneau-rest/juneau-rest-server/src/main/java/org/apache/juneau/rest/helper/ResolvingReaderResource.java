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
package org.apache.juneau.rest.helper;

import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.svl.*;

/**
 * An extension of {@link ReaderResource} that allows automatic resolution of SVL variables.
 */
public class ResolvingReaderResource extends ReaderResource {

	private final VarResolverSession varSession;

	/**
	 * Constructor.
	 *
	 * @param b Builder containing values to initialize this object with.
	 * @throws IOException
	 */
	protected ResolvingReaderResource(Builder b) throws IOException {
		this(b.mediaType, b.headers, b.cached, b.varResolver, b.contents.toArray());
	}

	/**
	 * Constructor.
	 *
	 * @param mediaType The resource media type.
	 * @param headers The HTTP response headers for this streamed resource.
	 * @param varSession Optional variable resolver for resolving variables in the string.
	 * @param cached
	 * 	Identifies if this resource is cached in memory.
	 * 	<br>If <jk>true</jk>, the contents will be loaded into a String for fast retrieval.
	 * @param contents
	 * 	The resource contents.
	 * 	<br>If multiple contents are specified, the results will be concatenated.
	 * 	<br>Contents can be any of the following:
	 * 	<ul>
	 * 		<li><code>InputStream</code>
	 * 		<li><code>Reader</code> - Converted to UTF-8 bytes.
	 * 		<li><code>File</code>
	 * 		<li><code>CharSequence</code> - Converted to UTF-8 bytes.
	 * 	</ul>
	 * @throws IOException
	 */
	public ResolvingReaderResource(MediaType mediaType, Map<String,Object> headers, boolean cached, VarResolverSession varSession, Object...contents) throws IOException {
		super(mediaType, headers, cached, contents);
		this.varSession = varSession;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new instance of a {@link Builder} for this class.
	 *
	 * @return A new instance of a {@link Builder}.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Builder class for constructing {@link ResolvingReaderResource} objects.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='link'>{@doc juneau-rest-server.RestMethod.ReaderResource}
	 * </ul>
	 */
	public static class Builder extends ReaderResource.Builder {
		VarResolverSession varResolver;

		/**
		 * Specifies the variable resolver to use for this resource.
		 *
		 * @param varResolver The variable resolver.
		 * @return This object (for method chaining).
		 */
		public Builder varResolver(VarResolverSession varResolver) {
			this.varResolver = varResolver;
			return this;
		}

		@Override
		public Builder mediaType(String mediaType) {
			super.mediaType(mediaType);
			return this;
		}

		@Override
		public Builder mediaType(MediaType mediaType) {
			super.mediaType(mediaType);
			return this;
		}

		@Override
		public Builder contents(Object...contents) {
			super.contents(contents);
			return this;
		}

		@Override
		public Builder header(String name, Object value) {
			super.header(name, value);
			return this;
		}

		@Override
		public Builder headers(Map<String,Object> headers) {
			super.headers(headers);
			return this;
		}

		@Override
		public Builder cached() {
			super.cached();
			return this;
		}

		/**
		 * Create a new {@link ResolvingReaderResource} using values in this builder.
		 *
		 * @return A new immutable {@link ResolvingReaderResource} object.
		 * @throws IOException
		 */
		@Override
		public ResolvingReaderResource build() throws IOException {
			return new ResolvingReaderResource(this);
		}
	}

	@ResponseBody
	@Override /* Writeable */
	public Writer writeTo(Writer w) throws IOException {
		for (Object o : contents) {
			if (o != null) {
				if (varSession == null)
					pipe(o, w);
				else
					varSession.resolveTo(read(o), w);
			}
		}
		return w;
	}
}
