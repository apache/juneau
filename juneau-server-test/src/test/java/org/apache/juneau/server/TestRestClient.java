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

import java.security.*;

import javax.net.ssl.*;

import org.apache.http.conn.ssl.*;
import org.apache.http.impl.client.*;
import org.apache.juneau.client.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;

/**
 * REST client with lenient SSL support and lax redirection strategy.
 */
class TestRestClient extends RestClient {

	public TestRestClient(Class<? extends Serializer> s, Class<? extends Parser> p) throws InstantiationException {
		super(s,p);
		setRootUrl(Constants.getServerTestUrl());
	}

	public TestRestClient(Serializer s, Parser p) {
		super(s,p);
		setRootUrl(Constants.getServerTestUrl());
	}

	public TestRestClient() {
		setRootUrl(Constants.getServerTestUrl());
	}

	public TestRestClient(CloseableHttpClient c) {
		super(c);
		setRootUrl(Constants.getServerTestUrl());
	}

	public static SSLConnectionSocketFactory getSSLSocketFactory() throws Exception {
		SSLContext sslContext = SSLContext.getInstance("SSL");
		TrustManager tm = new SimpleX509TrustManager(true);
		sslContext.init(null, new TrustManager[]{tm}, new SecureRandom());
		return new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
	}

	@Override /* RestClient */
	protected CloseableHttpClient createHttpClient() throws Exception {
		try {
			return HttpClients.custom().setSSLSocketFactory(getSSLSocketFactory()).setRedirectStrategy(new LaxRedirectStrategy()).build();
		} catch (KeyStoreException e) {
			throw new RuntimeException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}
}
