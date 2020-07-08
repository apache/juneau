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

import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;
import java.util.*;

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
	 * @throws IOException Thrown by underlying stream.
	 */
	protected ResolvingReaderResource(ResolvingResourceReaderBuilder b) throws IOException {
		super(b);
		this.varSession = b.varResolver;
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
	 * 		<li><c>InputStream</c>
	 * 		<li><c>Reader</c> - Converted to UTF-8 bytes.
	 * 		<li><c>File</c>
	 * 		<li><c>CharSequence</c> - Converted to UTF-8 bytes.
	 * 	</ul>
	 * @throws IOException Thrown by underlying stream.
	 */
	public ResolvingReaderResource(MediaType mediaType, Map<String,Object> headers, boolean cached, VarResolverSession varSession, Object...contents) throws IOException {
		super(mediaType, headers, cached, contents);
		this.varSession = varSession;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a new instance of a {@link ResolvingResourceReaderBuilder} for this class.
	 *
	 * @return A new instance of a {@link ResolvingResourceReaderBuilder}.
	 */
	public static ResolvingResourceReaderBuilder create() {
		return new ResolvingResourceReaderBuilder();
	}

	@ResponseBody
	@Override /* Writeable */
	public Writer writeTo(Writer w) throws IOException {
		if (contents != null) {
			if (varSession == null)
				pipe(contents, w);
			else
				varSession.resolveTo(read(contents), w);
		}
		return w;
	}
}
