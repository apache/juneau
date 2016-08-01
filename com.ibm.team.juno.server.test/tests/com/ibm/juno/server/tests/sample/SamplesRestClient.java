/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.tests.sample;

import java.security.*;

import javax.net.ssl.*;

import org.apache.http.conn.ssl.*;
import org.apache.http.impl.client.*;

import com.ibm.juno.client.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.server.tests.*;

/**
 * REST client with lenient SSL support and lax redirection strategy.
 */
public class SamplesRestClient extends RestClient {

	public SamplesRestClient(Class<? extends Serializer<?>> s, Class<? extends Parser<?>> p) throws InstantiationException {
		super(s,p);
		setRootUrl(Constants.getJunoSamplesUrl());
	}

	public SamplesRestClient(Serializer<?> s, Parser<?> p) {
		super(s,p);
		setRootUrl(Constants.getJunoSamplesUrl());
	}

	public SamplesRestClient() {
		setRootUrl(Constants.getJunoSamplesUrl());
	}

	public SamplesRestClient(CloseableHttpClient c) {
		super(c);
		setRootUrl(Constants.getJunoSamplesUrl());
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
