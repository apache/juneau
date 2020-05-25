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

import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;

import org.apache.juneau.assertions.*;

/**
 * Represents the body of an HTTP response.
 *
 * <p>
 * Provides convenience methods for retrieval and assertions of HTTP response bodies.
 */
public class MockServletResponseBody {

	private final MockServletResponse response;
	private final byte[] body;

	/**
	 * Constructor.
	 *
	 * @param response The HTTP response.
	 * @param body The body of the request.
	 */
	public MockServletResponseBody(MockServletResponse response, byte[] body) {
		this.body = body;
		this.response = response;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Raw streams
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the HTTP response message body as an input stream.
	 *
	 * @return
	 * 	The HTTP response message body input stream, never <jk>null</jk>.
	 * 	<br>For responses without a body(e.g. HTTP 204), returns an empty stream.
	 * @throws IOException If a stream or illegal state exception was thrown.
	 */
	public InputStream asInputStream() throws IOException {
		return new ByteArrayInputStream(body);
	}

	/**
	 * Returns the HTTP response message body as a byte array.
	 *
	 * @return
	 * 	The HTTP response message body input stream, never <jk>null</jk>.
	 * 	<br>For responses without a body(e.g. HTTP 204), returns an empty stream.
	 * @throws IOException If a stream or illegal state exception was thrown.
	 */
	public byte[] asBytes() throws IOException {
		return body;
	}

	/**
	 * Returns the HTTP response message body as a reader based on the charset on the <code>Content-Type</code> response header.
	 *
	 * @return
	 * 	The HTTP response message body reader, never <jk>null</jk>.
	 * 	<br>For responses without a body(e.g. HTTP 204), returns an empty reader.
	 * @throws IOException If an exception occurred.
	 */
	public Reader asReader() throws IOException {

		// Figure out what the charset of the response is.
		String cs = null;
		String ct = response.getContentType();

		// First look for "charset=" in Content-Type header of response.
		if (ct != null && ct.contains("charset="))
			cs = ct.substring(ct.indexOf("charset=")+8).trim();

		return asReader(cs);
	}

	/**
	 * Returns the HTTP response message body as a reader using the specified charset.
	 *
	 * @param charset
	 * 	The charset to use for the reader.
	 * 	<br>If <jk>null</jk>, <js>"UTF-8"</js> is used.
	 * @return
	 * 	The HTTP response message body reader, never <jk>null</jk>.
	 * 	<br>For responses without a body(e.g. HTTP 204), returns an empty reader.
	 * @throws IOException If an exception occurred.
	 */
	public Reader asReader(String charset) throws IOException {
		try {
			return new InputStreamReader(asInputStream(), charset == null ? "UTF-8" : charset);
		} catch (UnsupportedEncodingException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Returns the contents of this body as a string.
	 *
	 * @return The response as a string.
	 */
	public String asString() {
		try (Reader r = asReader()) {
			return read(r).toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Assertions
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Provides the ability to perform fluent-style assertions on this response body.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response body equals the text "OK".</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.assertBody().is(<js>"OK"</js>);
	 *
	 * 	<jc>// Validates the response body contains the text "OK".</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.assertBody().contains(<js>"OK"</js>);
	 *
	 * 	<jc>// Validates the response body passes a predicate test.</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.assertBody().passes(x -&gt; x.contains(<js>"OK"</js>));
	 *
	 * 	<jc>// Validates the response body matches a regular expression.</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.assertBody().matches(<js>".*OK.*"</js>);
	 *
	 * 	<jc>// Validates the response body matches a regular expression using regex flags.</jc>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.assertBody().matches(<js>".*OK.*"</js>,  <jsf>MULTILINE</jsf> &amp; <jsf>CASE_INSENSITIVE</jsf>);
	 *
	 * 	<jc>// Validates the response body matches a regular expression in the form of an existing Pattern.</jc>
	 * 	Pattern p = Pattern.<jsm>compile</jsm>(<js>".*OK.*"</js>);
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.assertBody().matches(p);
	 * </p>
	 *
	 * <p>
	 * The assertion test returns the original response object allowing you to chain multiple requests like so:
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response body matches a regular expression.</jc>
	 * 	MyBean bean = client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.run()
	 * 		.assertBody().matches(<js>".*OK.*"</js>);
	 * 		.assertBody().doesNotMatch(<js>".*ERROR.*"</js>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 * </ul>
	 *
	 * @return A new fluent assertion object.
	 * @throws IOException If REST call failed.
	 */
	public FluentStringAssertion<MockServletResponse> assertThat() throws IOException {
		return new FluentStringAssertion<>(asString(), response);
	}
}
