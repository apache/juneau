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
package org.apache.juneau.rest.mock2;

import java.io.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.entity.*;
import org.apache.http.message.*;
import org.apache.juneau.internal.*;

/**
 * An implementation of {@link HttpClientConnection} specifically for use in mocked connections using the {@link MockHttpConnection} class.
 *
 * This implementation is NOT thread safe.
 */
public class MockHttpClientConnection implements HttpClientConnection {

	private final MockHttpConnection c;
	private volatile MockHttpRequest req;
	private volatile MockHttpResponse res;

	/**
	 * Constructor.
	 *
	 * @param c The API for performing the connections.
	 */
	public MockHttpClientConnection(MockHttpConnection c) {
		this.c = c;
	}

	@Override /* HttpClientConnection */
	public void close() throws IOException {}

	@Override /* HttpClientConnection */
	public boolean isOpen() {
		return true;
	}

	@Override /* HttpClientConnection */
	public boolean isStale() {
		return false;
	}

	@Override /* HttpClientConnection */
	public void setSocketTimeout(int timeout) {}

	@Override /* HttpClientConnection */
	public int getSocketTimeout() {
		return Integer.MAX_VALUE;
	}

	@Override /* HttpClientConnection */
	public void shutdown() throws IOException {}

	@Override /* HttpClientConnection */
	public HttpConnectionMetrics getMetrics() {
		return null;
	}

	@Override /* HttpClientConnection */
	public boolean isResponseAvailable(int timeout) throws IOException {
		return true;
	}

	@Override /* HttpClientConnection */
	public void sendRequestHeader(HttpRequest request) throws HttpException, IOException {
		try {
			RequestLine rl = request.getRequestLine();
			req = c.request(rl.getMethod(), rl.getUri(), null, null);
			for (Header h : request.getAllHeaders())
				req.header(h.getName(), h.getValue());
		} catch (Exception e) {
			throw new HttpException(e.getMessage(), e);
		}
	}

	@Override /* HttpClientConnection */
	public void sendRequestEntity(HttpEntityEnclosingRequest request) throws HttpException, IOException {
		req.body(request.getEntity() == null ? "" : IOUtils.readBytes(request.getEntity().getContent(), 1024));
	}

	@Override /* HttpClientConnection */
	public HttpResponse receiveResponseHeader() throws HttpException, IOException {
		try {
			res = req.run();
			HttpResponse response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, res.getStatus(), res.getMessage()));
			for (Map.Entry<String,String[]> e : res.getHeaders().entrySet())
				for (String hv : e.getValue())
					response.addHeader(e.getKey(), hv);
			return response;
		} catch (Exception e) {
			throw new HttpException(e.getMessage(), e);
		}
	}

	@Override /* HttpClientConnection */
	public void receiveResponseEntity(HttpResponse response) throws HttpException, IOException {
		BasicHttpEntity e = new BasicHttpEntity();
		e.setContent(res.getBody().asInputStream());
		response.setEntity(e);
	}

	@Override /* HttpClientConnection */
	public void flush() throws IOException {}
}
