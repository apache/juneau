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
package org.apache.juneau.http;

import java.io.*;
import java.util.*;

import org.apache.http.*;
import org.apache.juneau.svl.*;

/**
 * Builder class for constructing {@link ResolvingReaderResource} objects.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-server.RestMethod.ReaderResource}
 * </ul>
 */
public class ResolvingResourceReaderBuilder extends ReaderResourceBuilder {
	VarResolverSession varResolver;

	/**
	 * Specifies the variable resolver to use for this resource.
	 *
	 * @param varResolver The variable resolver.
	 * @return This object (for method chaining).
	 */
	public ResolvingResourceReaderBuilder varResolver(VarResolverSession varResolver) {
		this.varResolver = varResolver;
		return this;
	}

	/**
	 * Create a new {@link ResolvingReaderResource} using values in this builder.
	 *
	 * @return A new immutable {@link ResolvingReaderResource} object.
	 * @throws IOException Thrown by underlying stream.
	 */
	@Override
	public ResolvingReaderResource build() throws IOException {
		return new ResolvingReaderResource(this);
	}

	// <FluentSetters>

	@Override /* GENERATED - ReaderResourceBuilder */
	public ResolvingResourceReaderBuilder cached() {
		super.cached();
		return this;
	}

	@Override /* GENERATED - ReaderResourceBuilder */
	public ResolvingResourceReaderBuilder contents(Object contents) {
		super.contents(contents);
		return this;
	}

	@Override /* GENERATED - ReaderResourceBuilder */
	public ResolvingResourceReaderBuilder header(String name, Object value) {
		super.header(name, value);
		return this;
	}

	@Override /* GENERATED - ReaderResourceBuilder */
	public ResolvingResourceReaderBuilder headers(Header...headers) {
		super.headers(headers);
		return this;
	}

	@Override /* GENERATED - ReaderResourceBuilder */
	public ResolvingResourceReaderBuilder headers(Map<String,Object> headers) {
		super.headers(headers);
		return this;
	}

	@Override /* GENERATED - ReaderResourceBuilder */
	public ResolvingResourceReaderBuilder mediaType(String mediaType) {
		super.mediaType(mediaType);
		return this;
	}

	@Override /* GENERATED - ReaderResourceBuilder */
	public ResolvingResourceReaderBuilder mediaType(MediaType mediaType) {
		super.mediaType(mediaType);
		return this;
	}

	// </FluentSetters>
}