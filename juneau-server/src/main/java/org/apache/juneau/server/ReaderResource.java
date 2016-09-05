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
package org.apache.juneau.server;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.server.response.*;
import org.apache.juneau.svl.*;

/**
 * Represents the contents of a text file with convenience methods for resolving
 * 	{@link Var} variables and adding HTTP response headers.
 * <p>
 * This class is handled special by the {@link WritableHandler} class.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class ReaderResource implements Writable {

	private String contents;
	private String mediaType;
	private VarResolverSession varSession;
	private Map<String,String> headers = new LinkedHashMap<String,String>();

	/**
	 * Constructor.
	 *
	 * @param contents The contents of this resource.
	 * @param mediaType The HTTP media type.
	 */
	protected ReaderResource(String contents, String mediaType) {
		this.contents = contents;
		this.mediaType = mediaType;
	}

	/**
	 * Add an HTTP response header.
	 *
	 * @param name The header name.
	 * @param value The header value converted to a string using {@link Object#toString()}.
	 * @return This object (for method chaining).
	 */
	public ReaderResource setHeader(String name, Object value) {
		headers.put(name, value == null ? "" : value.toString());
		return this;
	}

	/**
	 * Use the specified {@link VarResolver} to resolve any {@link Var StringVars} in the
	 * contents of this file when the {@link #writeTo(Writer)} or {@link #toString()} methods are called.
	 *
	 * @param varSession The string variable resolver to use to resolve string variables.
	 * @return This object (for method chaining).
	 */
	public ReaderResource setVarSession(VarResolverSession varSession) {
		this.varSession = varSession;
		return this;
	}

	/**
	 * Get the HTTP response headers.
	 *
	 * @return The HTTP response headers.
	 */
	public Map<String,String> getHeaders() {
		return headers;
	}

	@Override /* Writeable */
	public void writeTo(Writer w) throws IOException {
		if (varSession != null)
			varSession.resolveTo(contents, w);
		else
			w.write(contents);
	}

	@Override /* Streamable */
	public String getMediaType() {
		return mediaType;
	}

	@Override /* Object */
	public String toString() {
		if (varSession != null)
			return varSession.resolve(contents);
		return contents;
	}
}
