/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.client;

import org.apache.juneau.internal.*;

/**
 * SSL configuration options that get passed to {@link RestClient#enableSSL(SSLOpts)}.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class SSLOpts {

	private String protocols = getDefaultProtocols();
	private CertValidate certValidate = CertValidate.DEFAULT;
	private HostVerify hostVerify = HostVerify.DEFAULT;

	/**
	 * Reusable SSL options for lenient SSL (no cert validation or hostname verification).
	 */
	public static final SSLOpts LAX = new SSLOpts(null, CertValidate.LAX, HostVerify.LAX);

	/**
	 * Reusable SSL options for normal SSL (default cert validation and hostname verification).
	 */
	public static final SSLOpts DEFAULT = new SSLOpts(null, CertValidate.DEFAULT, HostVerify.DEFAULT);

	/**
	 * Constructor.
	 */
	public SSLOpts() {}

	/**
	 * Constructor.
	 *
	 * @param protocols A comma-delimited list of supported SSL protocols.
	 * 	If <jk>null</jk>, uses the value returned by {@link #getDefaultProtocols()}.
	 * @param certValidate Certificate validation setting.
	 * @param hostVerify Host verification setting.
	 */
	public SSLOpts(String protocols, CertValidate certValidate, HostVerify hostVerify) {
		if (protocols != null)
			this.protocols = protocols;
		this.certValidate = certValidate;
		this.hostVerify = hostVerify;
	}

	/**
	 * Returns the default list of SSL protocols to support when the <code>protocols</code>
	 * 	parameter on the constructor is <jk>null</jk>.
	 * <p>
	 * The default value is <jk>"SSL_TLS,TLS,SSL"</js> unless overridden by one of the following
	 * 	system properties:
	 * <ul>
	 * 	<li><js>"com.ibm.team.repository.transport.client.protocol"</js>
	 * 	<li><js>"transport.client.protocol"</js>
	 * </ul>
	 * <p>
	 * Subclasses can override this method to provide their own logic for determining default supported protocols.
	 *
	 * @return The comma-delimited list of supported protocols.
	 */
	protected String getDefaultProtocols() {
		String sp = System.getProperty("com.ibm.team.repository.transport.client.protocol");
		if (StringUtils.isEmpty(sp))
			sp = System.getProperty("transport.client.protocol");
		if (StringUtils.isEmpty(sp))
			sp = "SSL_TLS,TLS,SSL";
		return sp;
	}


	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

	/**
	 * Bean property getter:  <property>protocols</property>.
	 *
	 * @return The value of the <property>protocols</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public String getProtocols() {
		return protocols;
	}

	/**
	 * Bean property setter:  <property>protocols</property>.
	 *
	 * @param protocols The new value for the <property>properties</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SSLOpts setProtocols(String protocols) {
		this.protocols = protocols;
		return this;
	}

	/**
	 * Bean property getter:  <property>certValidate</property>.
	 *
	 * @return The value of the <property>certValidate</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public CertValidate getCertValidate() {
		return certValidate;
	}

	/**
	 * Bean property setter:  <property>certValidate</property>.
	 *
	 * @param certValidate The new value for the <property>properties</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SSLOpts setCertValidate(CertValidate certValidate) {
		this.certValidate = certValidate;
		return this;
	}

	/**
	 * Bean property getter:  <property>hostVerify</property>.
	 *
	 * @return The value of the <property>hostVerify</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public HostVerify getHostVerify() {
		return hostVerify;
	}

	/**
	 * Bean property setter:  <property>hostVerify</property>.
	 *
	 * @param hostVerify The new value for the <property>properties</property> property on this bean.
	 * @return This object (for method chaining).
	 */
	public SSLOpts setHostVerify(HostVerify hostVerify) {
		this.hostVerify = hostVerify;
		return this;
	}


	//--------------------------------------------------------------------------------
	// Enums
	//--------------------------------------------------------------------------------

	/**
	 * Certificate validation options.
	 * <p>
	 * Used as enum for {@link SSLOpts#getCertValidate()} property.
	 */
	@SuppressWarnings("hiding")
	public static enum CertValidate {

		/**
		 * Verify that the certificate is valid, but allow for self-signed certificates.
		 */
		LAX,

		/**
		 * Do normal certificate chain validation.
		 */
		DEFAULT
	}

	/**
	 * Certificate host verification options.
	 * <p>
	 * Used as enum for {@link SSLOpts#getHostVerify()} property.
	 */
	@SuppressWarnings("hiding")
	public enum HostVerify {

		/**
		 * Don't verify the hostname in the certificate.
		 */
		LAX,

		/**
		 * Do normal hostname verification.
		 */
		DEFAULT
	}
}
