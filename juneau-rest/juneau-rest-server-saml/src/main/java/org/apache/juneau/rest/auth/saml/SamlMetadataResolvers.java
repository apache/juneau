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
package org.apache.juneau.rest.auth.saml;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;

import javax.xml.parsers.*;

import org.opensaml.saml.metadata.resolver.*;
import org.opensaml.saml.metadata.resolver.impl.*;
import org.w3c.dom.*;

import net.shibboleth.shared.component.*;
import net.shibboleth.shared.resolver.*;
import net.shibboleth.shared.xml.impl.*;

/**
 * Convenience factories for building {@link MetadataResolver} instances backed by an OpenSAML implementation.
 *
 * <p>
 * Juneau ships <i>convenience factories only</i> &mdash; no bundled default
 * {@link MetadataResolver} singleton.  Applications either:
 * <ul>
 * 	<li>Call {@link #file(File)} for a local SAML metadata file (typical dev/staging setup).
 * 	<li>Call {@link #url(String)} for a remote {@code /metadata} endpoint (typical production setup).
 * 	<li>Build their own {@link MetadataResolver} directly using OpenSAML APIs (advanced).
 * </ul>
 *
 * <p>
 * Both factories return resolvers that are <b>already initialized</b> &mdash; ready to hand to
 * {@link SamlAssertionValidator.Builder#metadataResolver(MetadataResolver)} immediately.
 *
 * <h5 class='section'>Notes:</h5>
 * <p>
 * The URL factory uses the JDK {@link HttpClient} (Java 17) to fetch the metadata blob once at construction
 * time and wraps the parsed DOM in a {@link DOMMetadataResolver}.  No background refresh is performed; applications
 * that need periodic refresh should construct their own {@code HTTPMetadataResolver} (from
 * {@code opensaml-saml-impl}) with an Apache HttpClient + refresh policy.
 *
 * @since 10.0.0
 */
public final class SamlMetadataResolvers {

	private SamlMetadataResolvers() {}

	/**
	 * Creates a {@link MetadataResolver} that loads SAML 2.0 metadata from the given file.
	 *
	 * <p>
	 * The file is parsed at construction time and re-parsed on disk modification by the underlying
	 * {@link FilesystemMetadataResolver}.
	 *
	 * @param file The metadata XML file on the local filesystem.  Must exist and be readable.
	 * @return An initialized {@link MetadataResolver}.
	 * @throws IOException If the file cannot be parsed or initialized.
	 */
	public static MetadataResolver file(File file) throws IOException {
		if (file == null)
			throw new IllegalArgumentException("file must not be null");
		if (!file.isFile())
			throw new FileNotFoundException("SAML metadata file not found: " + file);

		OpenSamlBootstrap.ensureInitialized();
		try {
			var parserPool = new BasicParserPool();
			parserPool.initialize();

			var resolver = new FilesystemMetadataResolver(file);
			resolver.setId(file.getAbsolutePath());
			resolver.setParserPool(parserPool);
			resolver.setRequireValidMetadata(true);
			resolver.initialize();
			return resolver;
		} catch (ComponentInitializationException | ResolverException e) {
			throw new IOException("Failed to initialize FilesystemMetadataResolver for " + file, e);
		}
	}

	/**
	 * Creates a {@link MetadataResolver} that fetches SAML 2.0 metadata from the given URL once and serves it
	 * from an in-memory DOM.
	 *
	 * <p>
	 * Fetch happens on the calling thread with a 30-second connect/request timeout.  Applications that need
	 * periodic refresh should construct an OpenSAML {@code HTTPMetadataResolver} directly with an Apache
	 * HttpClient.
	 *
	 * @param url The metadata URL.  Must be an absolute HTTPS or HTTP URL.
	 * @return An initialized {@link MetadataResolver}.
	 * @throws IOException If the URL cannot be fetched or the metadata is malformed.
	 */
	public static MetadataResolver url(String url) throws IOException {
		if (url == null)
			throw new IllegalArgumentException("url must not be null");

		OpenSamlBootstrap.ensureInitialized();
		try {
			var client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(30))
				.build();
			var req = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.timeout(Duration.ofSeconds(30))
				.GET()
				.build();
			var resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
			if (resp.statusCode() < 200 || resp.statusCode() >= 300)
				throw new IOException("Failed to fetch SAML metadata from " + url + " (HTTP " + resp.statusCode() + ")");

			var dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
			dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			dbf.setXIncludeAware(false);
			dbf.setExpandEntityReferences(false);
			var doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(resp.body()));
			Element root = doc.getDocumentElement();

			var resolver = new DOMMetadataResolver(root);
			resolver.setId(url);
			resolver.setRequireValidMetadata(true);
			resolver.initialize();
			return resolver;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while fetching SAML metadata from " + url, e);
		} catch (ComponentInitializationException | javax.xml.parsers.ParserConfigurationException
				| org.xml.sax.SAXException e) {
			throw new IOException("Failed to initialize DOMMetadataResolver for " + url, e);
		}
	}

	/**
	 * Creates a {@link MetadataResolver} that loads SAML 2.0 metadata from the given {@link Path}.
	 *
	 * <p>
	 * Convenience overload of {@link #file(File)}; useful for callers that have a {@link Path} on hand.
	 *
	 * @param path The metadata XML file as a {@link Path}.
	 * @return An initialized {@link MetadataResolver}.
	 * @throws IOException If the file cannot be parsed or initialized.
	 */
	public static MetadataResolver file(Path path) throws IOException {
		if (path == null)
			throw new IllegalArgumentException("path must not be null");
		return file(path.toFile());
	}
}
