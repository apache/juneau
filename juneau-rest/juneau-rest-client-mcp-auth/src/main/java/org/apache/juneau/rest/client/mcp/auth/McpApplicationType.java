/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.client.mcp.auth;

import com.nimbusds.openid.connect.sdk.rp.*;

/**
 * The OIDC Dynamic Client Registration {@code application_type} (SEP-837) declared by an MCP client when it registers
 * with an authorization server.
 *
 * <p>
 * This is a Juneau-owned enum so the public DCR API does not leak the Nimbus {@code provided}-scoped
 * {@link ApplicationType} type; it maps 1:1 to the Nimbus value internally via {@link #toNimbus()}.
 *
 * <p>
 * Per the MCP {@code 2026-07-28} client-registration spec, an MCP client <b>MUST</b> specify an appropriate
 * {@code application_type} during Dynamic Client Registration &mdash; omitting it defaults to {@code "web"} under OIDC,
 * which can conflict with native-style (loopback) redirect URIs.  Native apps (desktop, mobile, CLI tools, and
 * locally-hosted web apps accessed via {@code localhost}) <b>SHOULD</b> use {@link #NATIVE}; remote browser-based web
 * apps <b>SHOULD</b> use {@link #WEB}.  Non-OIDC (plain RFC 7591) servers safely ignore the parameter.
 *
 * @since 10.0.0
 */
public enum McpApplicationType {

	/** A native app (desktop, mobile, CLI, or a loopback-hosted local web app) &mdash; maps to {@link ApplicationType#NATIVE}. */
	NATIVE,

	/** A remote browser-based web app &mdash; maps to {@link ApplicationType#WEB}. */
	WEB;

	/**
	 * Maps this Juneau enum value to the equivalent Nimbus {@link ApplicationType}.
	 *
	 * @return The Nimbus {@code application_type}.  Never <jk>null</jk>.
	 */
	ApplicationType toNimbus() {
		return this == NATIVE ? ApplicationType.NATIVE : ApplicationType.WEB;
	}
}
