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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.*;

/**
 * Wraps an {@link HttpServletResponse} and caches the output stream in a separate buffer for debugging purposes.
 */
public class CachingHttpServletResponse extends HttpServletResponseWrapper {

	private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private final ServletOutputStream os;

	/**
	 * Wraps the specified response inside a {@link CachingHttpServletResponse} if it isn't already.
	 *
	 * @param res The response to wrap.
	 * @return The wrapped request.
	 * @throws IOException Thrown by underlying body stream.
	 */
	public static CachingHttpServletResponse wrap(HttpServletResponse res) throws IOException {
		if (res instanceof CachingHttpServletResponse)
			return (CachingHttpServletResponse)res;
		return new CachingHttpServletResponse(res);
	}

	/**
	 * Constructor.
	 *
	 * @param res The wrapped servlet response.
	 * @throws IOException Thrown by underlying stream.
	 */
	protected CachingHttpServletResponse(HttpServletResponse res) throws IOException {
		super(res);
		os = res.getOutputStream();
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return new ServletOutputStream() {

			@Override
			public boolean isReady() {
				return os.isReady();
			}

			@Override
			public void setWriteListener(WriteListener writeListener) {
				os.setWriteListener(writeListener);
			}

			@Override
			public void write(int b) throws IOException {
				baos.write(b);
				os.write(b);
			}

			@Override
			public void flush() throws IOException {
				os.flush();
			}

			@Override
			public void close() throws IOException {
				os.close();
			}
		};
	}

	/**
	 * Returns the body of the servlet response without consuming the stream.
	 *
	 * @return The body of the response.
	 */
	public byte[] getBody() {
		return baos.toByteArray();
	}
}
