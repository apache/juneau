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
package org.apache.juneau.rest.server.auth.saml;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.*;
import java.nio.file.*;
import java.security.cert.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import javax.xml.parsers.*;

import org.apache.juneau.commons.utils.*;
import org.opensaml.saml.metadata.resolver.*;
import org.opensaml.saml.metadata.resolver.filter.*;
import org.opensaml.saml.metadata.resolver.filter.impl.*;
import org.opensaml.saml.metadata.resolver.impl.*;
import org.opensaml.security.credential.impl.*;
import org.opensaml.security.criteria.*;
import org.opensaml.security.x509.*;
import org.opensaml.xmlsec.config.impl.*;
import org.opensaml.xmlsec.signature.support.impl.*;
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
		if (!file.isFile()) // HTT: false branch (file present) requires valid SAML metadata XML; covered by integration tests
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
			throw ioex(e, "Failed to initialize FilesystemMetadataResolver for %s", file);
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
	 * <h5 class='section'>Transport:</h5>
	 * <p>
	 * The metadata blob supplies the trust-anchor signing certificate, so the transport must not be
	 * downgradeable by a network intermediary.  The URL must therefore use <js>"https"</js> or target a
	 * loopback host (<js>"localhost"</js>/<js>"127.0.0.1"</js>/<js>"::1"</js>, for local development); a
	 * plaintext <js>"http"</js> URL aimed at any other host is rejected.  When the IdP offers signed metadata,
	 * prefer {@link #url(String, X509Certificate)} so the metadata's own XML signature is verified against a
	 * pinned certificate independently of the transport.
	 *
	 * @param url The metadata URL.  Must use HTTPS or target a loopback host.
	 * @return An initialized {@link MetadataResolver}.
	 * @throws IOException If the URL cannot be fetched or the metadata is malformed.
	 */
	public static MetadataResolver url(String url) throws IOException {
		return url(url, null);
	}

	/**
	 * Creates a {@link MetadataResolver} that fetches SAML 2.0 metadata from the given URL and verifies the
	 * metadata's XML signature against the supplied pinned certificate before trusting it.
	 *
	 * <p>
	 * This is the strongest option: the metadata document's own enveloped signature is validated against
	 * <jv>metadataSigningCert</jv>, so a substituted or tampered metadata blob is rejected regardless of the
	 * transport used to fetch it.  The same transport rule as {@link #url(String)} still applies.
	 *
	 * <p>
	 * The response body is bounded to {@link SamlAuthFilter#DEFAULT_MAX_INFLATED_BYTES} (1 MiB); use
	 * {@link #url(String, X509Certificate, long)} to override that cap.
	 *
	 * @param url The metadata URL.  Must use HTTPS or target a loopback host.
	 * @param metadataSigningCert The certificate whose public key signed the metadata document.  When
	 * 	<jk>null</jk>, no signature validation is performed (equivalent to {@link #url(String)}).
	 * @return An initialized {@link MetadataResolver}.
	 * @throws IOException If the URL cannot be fetched, the metadata is malformed, or its signature does not
	 * 	verify against the pinned certificate.
	 */
	public static MetadataResolver url(String url, X509Certificate metadataSigningCert) throws IOException {
		return url(url, metadataSigningCert, SamlAuthFilter.DEFAULT_MAX_INFLATED_BYTES);
	}

	/**
	 * Creates a {@link MetadataResolver} that fetches SAML 2.0 metadata from the given URL, verifies the
	 * metadata's XML signature against the supplied pinned certificate (when provided), and bounds the fetched
	 * response body to <jv>maxMetadataBytes</jv>.
	 *
	 * <p>
	 * A malicious or compromised metadata endpoint must not be able to exhaust heap by returning an
	 * unbounded response.  The cap is enforced both against a declared {@code Content-Length} header (rejected
	 * up front) and against the actual streamed byte count (aborted mid-fetch), so a missing or lying
	 * {@code Content-Length} cannot bypass it.
	 *
	 * @param url The metadata URL.  Must use HTTPS or target a loopback host.
	 * @param metadataSigningCert The certificate whose public key signed the metadata document.  When
	 * 	<jk>null</jk>, no signature validation is performed.
	 * @param maxMetadataBytes The maximum number of response bytes to accept before aborting the fetch.
	 * @return An initialized {@link MetadataResolver}.
	 * @throws IOException If the URL cannot be fetched, the response exceeds <jv>maxMetadataBytes</jv>, the
	 * 	metadata is malformed, or its signature does not verify against the pinned certificate.
	 */
	public static MetadataResolver url(String url, X509Certificate metadataSigningCert, long maxMetadataBytes) throws IOException {
		if (url == null)
			throw new IllegalArgumentException("url must not be null");
		UriUtils.assertSecureOrLoopback(URI.create(url));

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
			var resp = client.send(req, boundedByteArrayBodyHandler(maxMetadataBytes));
			if (resp.statusCode() < 200 || resp.statusCode() >= 300) // HTT: false branch (2xx success) requires live SAML metadata endpoint; covered by integration tests
				throw ioex("Failed to fetch SAML metadata from %s (HTTP %s)", url, resp.statusCode());

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
			if (metadataSigningCert != null)
				resolver.setMetadataFilter(signatureValidationFilter(metadataSigningCert));
			resolver.initialize();
			return resolver;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw ioex(e, "Interrupted while fetching SAML metadata from %s", url);
		} catch (ComponentInitializationException | javax.xml.parsers.ParserConfigurationException
				| org.xml.sax.SAXException e) {
			throw ioex(e, "Failed to initialize DOMMetadataResolver for %s", url);
		}
	}

	private static MetadataFilter signatureValidationFilter(X509Certificate cert) throws ComponentInitializationException {
		var credential = new BasicX509Credential(cert);
		var trustEngine = new ExplicitKeySignatureTrustEngine(
			new StaticCredentialResolver(credential),
			DefaultSecurityConfigurationBootstrap.buildBasicInlineKeyInfoCredentialResolver());
		var filter = new SignatureValidationFilter(trustEngine);
		filter.setDefaultCriteria(new CriteriaSet(new UsageCriterion(org.opensaml.security.credential.UsageType.SIGNING)));
		filter.initialize();
		return filter;
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
		if (path == null) // HTT: false branch (valid path) delegates to file(File); covered wherever file(File) is tested
			throw new IllegalArgumentException("path must not be null");
		return file(path.toFile());
	}

	/**
	 * Builds a {@link HttpResponse.BodyHandler} that rejects a declared {@code Content-Length} over
	 * <jv>maxBytes</jv> up front, and otherwise aborts streaming as soon as the actual byte count exceeds
	 * <jv>maxBytes</jv> &mdash; so a missing or understated {@code Content-Length} cannot bypass the cap.
	 */
	private static HttpResponse.BodyHandler<byte[]> boundedByteArrayBodyHandler(long maxBytes) {
		return responseInfo -> {
			var declaredLength = responseInfo.headers().firstValueAsLong("Content-Length");
			if (declaredLength.isPresent() && declaredLength.getAsLong() > maxBytes)
				return BoundedByteArraySubscriber.rejected(maxBytes, declaredLength.getAsLong());
			return new BoundedByteArraySubscriber(maxBytes);
		};
	}

	/**
	 * A {@link HttpResponse.BodySubscriber} that accumulates the response body into a byte array, aborting
	 * (cancelling the upstream subscription and failing {@link #getBody()}) as soon as the accumulated byte
	 * count exceeds a fixed cap.
	 */
	private static final class BoundedByteArraySubscriber implements HttpResponse.BodySubscriber<byte[]> {
		private final long maxBytes;
		private final ByteArrayOutputStream out = new ByteArrayOutputStream();
		private final CompletableFuture<byte[]> future = new CompletableFuture<>();
		private Flow.Subscription subscription;
		private long total;

		BoundedByteArraySubscriber(long maxBytes) {
			this.maxBytes = maxBytes;
		}

		static BoundedByteArraySubscriber rejected(long maxBytes, long declaredLength) {
			var subscriber = new BoundedByteArraySubscriber(maxBytes);
			subscriber.future.completeExceptionally(
				ioex("SAML metadata response declares Content-Length %s bytes, exceeding the %s-byte cap", declaredLength, maxBytes));
			return subscriber;
		}

		@Override /* BodySubscriber */
		public CompletionStage<byte[]> getBody() {
			return future;
		}

		@Override /* Flow.Subscriber */
		public void onSubscribe(Flow.Subscription subscription) {
			if (future.isDone()) { // Already rejected via a declared Content-Length over the cap.
				subscription.cancel();
				return;
			}
			subscription.request(Long.MAX_VALUE);
			this.subscription = subscription;
		}

		@Override /* Flow.Subscriber */
		public void onNext(List<ByteBuffer> item) {
			if (future.isDone())
				return;
			for (var buf : item) {
				total += buf.remaining();
				if (total > maxBytes) {
					subscription.cancel();
					future.completeExceptionally(ioex("SAML metadata response exceeds the %s-byte cap", maxBytes));
					return;
				}
				var bytes = new byte[buf.remaining()];
				buf.get(bytes);
				out.writeBytes(bytes);
			}
		}

		@Override /* Flow.Subscriber */
		public void onError(Throwable throwable) {
			future.completeExceptionally(throwable);
		}

		@Override /* Flow.Subscriber */
		public void onComplete() {
			if (!future.isDone())
				future.complete(out.toByteArray());
		}
	}
}
