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
package org.apache.juneau.rest.server.mcp.v20260728;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.net.*;
import java.util.*;

/**
 * Pure, side-effect-free construction of the RFC 9728 / SEP-2351 well-known Protected Resource Metadata (PRM) URIs for a
 * given canonical resource identifier.
 *
 * <p>
 * Per <a class="doclink" href="https://datatracker.ietf.org/doc/html/rfc9728#section-3.1">RFC 9728 &sect;3.1</a> and
 * SEP-2351, the well-known segment {@code /.well-known/oauth-protected-resource} is inserted between the host and the
 * resource's path component:
 *
 * <ul>
 * 	<li>Resource {@code https://host/mcp} &rarr; PRM at {@code https://host/.well-known/oauth-protected-resource/mcp}
 * 		(path-insertion).
 * 	<li>Resource {@code https://host} (no path) &rarr; PRM at {@code https://host/.well-known/oauth-protected-resource}
 * 		(root form).
 * </ul>
 *
 * <p>
 * {@link #candidates(URI)} returns the ordered client lookup sequence &mdash; the path-inserted URI first, then the root
 * fallback &mdash; matching the ordered fallback SEP-2351 specifies.  The server hosts the path-inserted form (and, for
 * a rooted resource, that <i>is</i> the root form).
 *
 * @since 10.0.0
 */
public final class McpWellKnownRouting {

	/** The RFC 9728 well-known path segment. */
	public static final String WELL_KNOWN_PATH = "/.well-known/oauth-protected-resource";

	private McpWellKnownRouting() {}

	/**
	 * Returns the path-inserted PRM URI for the supplied resource identifier (RFC 9728 &sect;3.1).
	 *
	 * @param resource The canonical resource identifier.  Must not be <jk>null</jk> and must be absolute (scheme +
	 * 	authority).
	 * @return The PRM document URI.
	 * @throws IllegalArgumentException If {@code resource} is <jk>null</jk> or is missing a scheme/authority.
	 */
	public static URI metadataUri(URI resource) {
		var base = baseOf(resource);
		return URI.create(base + WELL_KNOWN_PATH + normalizePath(resource.getRawPath()));
	}

	/**
	 * Returns the root-form PRM URI for the supplied resource identifier &mdash; the ordered fallback location
	 * (SEP-2351).
	 *
	 * @param resource The canonical resource identifier.  Must not be <jk>null</jk> and must be absolute (scheme +
	 * 	authority).
	 * @return The root PRM document URI.
	 * @throws IllegalArgumentException If {@code resource} is <jk>null</jk> or is missing a scheme/authority.
	 */
	public static URI rootMetadataUri(URI resource) {
		return URI.create(baseOf(resource) + WELL_KNOWN_PATH);
	}

	/**
	 * Returns the ordered PRM lookup candidates &mdash; the path-inserted URI first, then the root fallback (SEP-2351).
	 *
	 * <p>
	 * When the resource has no path component the two coincide and a single-element list is returned.
	 *
	 * @param resource The canonical resource identifier.  Must not be <jk>null</jk> and must be absolute (scheme +
	 * 	authority).
	 * @return The ordered candidate list (1 or 2 entries).  Never <jk>null</jk>.
	 * @throws IllegalArgumentException If {@code resource} is <jk>null</jk> or is missing a scheme/authority.
	 */
	public static List<URI> candidates(URI resource) {
		var pathInserted = metadataUri(resource);
		var root = rootMetadataUri(resource);
		return pathInserted.equals(root) ? List.of(pathInserted) : List.of(pathInserted, root);
	}

	/**
	 * Returns the servlet-relative request path at which this server hosts its PRM document for the supplied resource
	 * identifier &mdash; i.e. {@link #metadataUri(URI)}'s path component.
	 *
	 * @param resource The canonical resource identifier.  Must not be <jk>null</jk>.
	 * @return The request path (e.g. {@code /.well-known/oauth-protected-resource/mcp}).
	 * @throws IllegalArgumentException If {@code resource} is <jk>null</jk>.
	 */
	public static String wellKnownRequestPath(URI resource) {
		assertArgNotNull("resource", resource);
		return WELL_KNOWN_PATH + normalizePath(resource.getRawPath());
	}

	private static String baseOf(URI resource) {
		assertArgNotNull("resource", resource);
		var scheme = resource.getScheme();
		var authority = resource.getRawAuthority();
		if (scheme == null || authority == null)
			throw iaex("resource %s must be an absolute URI with a scheme and authority", resource);
		return scheme + "://" + authority;
	}

	private static String normalizePath(String rawPath) {
		if (rawPath == null || rawPath.isEmpty())
			return "";
		var p = rawPath;
		while (p.endsWith("/"))
			p = p.substring(0, p.length() - 1);
		if (p.isEmpty())
			return "";
		return p.startsWith("/") ? p : "/" + p;
	}
}
