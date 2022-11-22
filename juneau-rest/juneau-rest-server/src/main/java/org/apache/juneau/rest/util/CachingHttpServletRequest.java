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
package org.apache.juneau.rest.util;

import static org.apache.juneau.common.internal.IOUtils.*;

import java.io.IOException;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Wraps an {@link HttpServletRequest} and preloads the content into memory for debugging purposes.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class CachingHttpServletRequest extends HttpServletRequestWrapper {

	private final byte[] content;

	/**
	 * Wraps the specified request inside a {@link CachingHttpServletRequest} if it isn't already.
	 *
	 * @param req The request to wrap.
	 * @return The wrapped request.
	 * @throws IOException Thrown by underlying content stream.
	 */
	public static CachingHttpServletRequest wrap(HttpServletRequest req) throws IOException {
		if (req instanceof CachingHttpServletRequest)
			return (CachingHttpServletRequest)req;
		return new CachingHttpServletRequest(req);
	}

	/**
	 * Constructor.
	 *
	 * @param req The request being wrapped.
	 * @throws IOException If content could not be loaded into memory.
	 */
	protected CachingHttpServletRequest(HttpServletRequest req) throws IOException {
		super(req);
		this.content = readBytes(req.getInputStream());
	}

	@Override
	public ServletInputStream getInputStream() {
		return new BoundedServletInputStream(content);
	}

	/**
	 * Returns the content of the servlet request without consuming the stream.
	 *
	 * @return The content of the request.
	 */
	public byte[] getContent() {
		return content;
	}
}
