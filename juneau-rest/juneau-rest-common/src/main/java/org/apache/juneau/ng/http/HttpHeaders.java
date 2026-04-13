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
package org.apache.juneau.ng.http;

import org.apache.juneau.commons.http.MediaRanges;
import org.apache.juneau.commons.http.MediaType;
import org.apache.juneau.commons.http.StringRanges;
import java.net.*;
import java.time.*;
import java.util.function.*;

import org.apache.juneau.http.header.EntityTag;
import org.apache.juneau.http.header.EntityTags;
import org.apache.juneau.ng.http.header.*;

/**
 * Static factory methods for creating common HTTP headers.
 *
 * <p>
 * This is a convenience class that provides shorthand access to all named header types in
 * {@code org.apache.juneau.ng.http.header}.
 *
 * <p class='bjava'>
 * 	<jc>// Import statically for clean DSL-style usage</jc>
 * 	import static org.apache.juneau.ng.http.HttpHeaders.*;
 *
 * 	NgRestRequest <jv>req</jv> = client.get(<js>"/api/resource"</js>)
 * 		.header(accept(<js>"application/json"</js>))
 * 		.header(authorization(<js>"Bearer "</js> + token));
 * </p>
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public final class HttpHeaders {

	private HttpHeaders() {}

	/** @param value The header value. @return A new header. */
	public static Accept accept(String value) { return Accept.of(value); }
	/** @param value Supplier of the wire header value. @return A new header. */
	public static Accept acceptLazyWire(Supplier<String> value) { return Accept.ofLazyWire(value); }
	/** @param value Parsed media ranges. @return A new header. */
	public static Accept accept(MediaRanges value) { return Accept.of(value); }
	/** @param value Supplier of parsed media ranges. @return A new header. */
	public static Accept acceptLazyParsed(Supplier<MediaRanges> value) { return Accept.ofLazyParsed(value); }

	/** @param value The header value. @return A new header. */
	public static AcceptCharset acceptCharset(String value) { return AcceptCharset.of(value); }
	/** @param value Supplier of the wire header value. @return A new header. */
	public static AcceptCharset acceptCharsetLazyWire(Supplier<String> value) { return AcceptCharset.ofLazyWire(value); }
	/** @param value Parsed string ranges. @return A new header. */
	public static AcceptCharset acceptCharset(StringRanges value) { return AcceptCharset.of(value); }
	/** @param value Supplier of string ranges. @return A new header. */
	public static AcceptCharset acceptCharsetLazyParsed(Supplier<StringRanges> value) { return AcceptCharset.ofLazyParsed(value); }

	/** @param value The header value. @return A new header. */
	public static AcceptEncoding acceptEncoding(String value) { return AcceptEncoding.of(value); }
	/** @param value Supplier of the wire header value. @return A new header. */
	public static AcceptEncoding acceptEncodingLazyWire(Supplier<String> value) { return AcceptEncoding.ofLazyWire(value); }
	/** @param value Parsed string ranges. @return A new header. */
	public static AcceptEncoding acceptEncoding(StringRanges value) { return AcceptEncoding.of(value); }
	/** @param value Supplier of string ranges. @return A new header. */
	public static AcceptEncoding acceptEncodingLazyParsed(Supplier<StringRanges> value) { return AcceptEncoding.ofLazyParsed(value); }

	/** @param value The header value. @return A new header. */
	public static AcceptLanguage acceptLanguage(String value) { return AcceptLanguage.of(value); }
	/** @param value Supplier of the wire header value. @return A new header. */
	public static AcceptLanguage acceptLanguageLazyWire(Supplier<String> value) { return AcceptLanguage.ofLazyWire(value); }
	/** @param value Parsed string ranges. @return A new header. */
	public static AcceptLanguage acceptLanguage(StringRanges value) { return AcceptLanguage.of(value); }
	/** @param value Supplier of string ranges. @return A new header. */
	public static AcceptLanguage acceptLanguageLazyParsed(Supplier<StringRanges> value) { return AcceptLanguage.ofLazyParsed(value); }

	/** @param value The header value. @return A new header. */
	public static AcceptRanges acceptRanges(String value) { return AcceptRanges.of(value); }
	/** @param value The header value supplier. @return A new header. */
	public static AcceptRanges acceptRanges(Supplier<String> value) { return AcceptRanges.of(value); }

	/** @param value The header value. @return A new header. */
	public static Age age(String value) { return Age.of(value); }
	/** @param seconds Age in seconds. @return A new header. */
	public static Age age(int seconds) { return Age.of(seconds); }

	/** @param value The header value. @return A new header. */
	public static Allow allow(String value) { return Allow.of(value); }
	/** @param methods Allowed methods. @return A new header. */
	public static Allow allow(String... methods) { return Allow.of(methods); }
	/** @param tokenSupplier Lazy token list. @return A new header. */
	public static Allow allowLazyTokens(Supplier<String[]> tokenSupplier) { return Allow.ofLazyTokens(tokenSupplier); }

	/** @param value The header value. @return A new header. */
	public static Authorization authorization(String value) { return Authorization.of(value); }
	/** @param value The header value supplier. @return A new header. */
	public static Authorization authorization(Supplier<String> value) { return Authorization.of(value); }

	/** @param value The header value. @return A new header. */
	public static CacheControl cacheControl(String value) { return CacheControl.of(value); }
	/** @param value The header value supplier. @return A new header. */
	public static CacheControl cacheControl(Supplier<String> value) { return CacheControl.of(value); }

	/** @param value The header value. @return A new header. */
	public static Connection connection(String value) { return Connection.of(value); }

	/** @param value The header value. @return A new header. */
	public static ContentDisposition contentDisposition(String value) { return ContentDisposition.of(value); }
	/** @param value Parsed string ranges. @return A new header. */
	public static ContentDisposition contentDisposition(StringRanges value) { return ContentDisposition.of(value); }
	/** @param value Supplier of string ranges. @return A new header. */
	public static ContentDisposition contentDisposition(Supplier<StringRanges> value) { return ContentDisposition.ofLazyParsed(value); }

	/** @param value The header value. @return A new header. */
	public static ContentEncoding contentEncoding(String value) { return ContentEncoding.of(value); }

	/** @param value The header value. @return A new header. */
	public static ContentLanguage contentLanguage(String value) { return ContentLanguage.of(value); }

	/** @param value The header value. @return A new header. */
	public static ContentLength contentLength(String value) { return ContentLength.of(value); }
	/** @param bytes The content length in bytes. @return A new header. */
	public static ContentLength contentLength(long bytes) { return ContentLength.of(bytes); }

	/** @param value The header value. @return A new header. */
	public static ContentLocation contentLocation(String value) { return ContentLocation.of(value); }
	/** @param uri Content location URI. @return A new header. */
	public static ContentLocation contentLocation(URI uri) { return ContentLocation.of(uri); }

	/** @param value The header value. @return A new header. */
	public static ContentRange contentRange(String value) { return ContentRange.of(value); }

	/** @param value The header value. @return A new header. */
	public static ContentType contentType(String value) { return ContentType.of(value); }
	/** @param value Supplier of the wire header value. @return A new header. */
	public static ContentType contentTypeLazyWire(Supplier<String> value) { return ContentType.ofLazyWire(value); }
	/** @param value Parsed media type. @return A new header. */
	public static ContentType contentType(MediaType value) { return ContentType.of(value); }
	/** @param value Supplier of media type. @return A new header. */
	public static ContentType contentTypeLazyParsed(Supplier<MediaType> value) { return ContentType.ofLazyParsed(value); }

	/** @param value The header value. @return A new header. */
	public static Date date(String value) { return Date.of(value); }
	/** @param value HTTP-date. @return A new header. */
	public static Date date(ZonedDateTime value) { return Date.of(value); }

	/** @param value The header value. @return A new header. */
	public static ETag eTag(String value) { return ETag.of(value); }
	/** @param value Parsed entity-tag. @return A new header. */
	public static ETag eTag(EntityTag value) { return ETag.of(value); }

	/** @param value The header value. @return A new header. */
	public static Expect expect(String value) { return Expect.of(value); }

	/** @param value The header value. @return A new header. */
	public static Expires expires(String value) { return Expires.of(value); }
	/** @param value HTTP-date. @return A new header. */
	public static Expires expires(ZonedDateTime value) { return Expires.of(value); }

	/** @param value The header value. @return A new header. */
	public static Forwarded forwarded(String value) { return Forwarded.of(value); }

	/** @param value The header value. @return A new header. */
	public static From from(String value) { return From.of(value); }

	/** @param value The header value. @return A new header. */
	public static Host host(String value) { return Host.of(value); }

	/** @param value The header value. @return A new header. */
	public static IfMatch ifMatch(String value) { return IfMatch.of(value); }
	/** @param value Parsed entity-tags. @return A new header. */
	public static IfMatch ifMatch(EntityTags value) { return IfMatch.of(value); }

	/** @param value The header value. @return A new header. */
	public static IfModifiedSince ifModifiedSince(String value) { return IfModifiedSince.of(value); }
	/** @param value HTTP-date. @return A new header. */
	public static IfModifiedSince ifModifiedSince(ZonedDateTime value) { return IfModifiedSince.of(value); }

	/** @param value The header value. @return A new header. */
	public static IfNoneMatch ifNoneMatch(String value) { return IfNoneMatch.of(value); }
	/** @param value Parsed entity-tags. @return A new header. */
	public static IfNoneMatch ifNoneMatch(EntityTags value) { return IfNoneMatch.of(value); }

	/** @param value The header value. @return A new header. */
	public static IfRange ifRange(String value) { return IfRange.of(value); }
	/** @param value Entity-tag. @return A new header. */
	public static IfRange ifRange(EntityTag value) { return IfRange.of(value); }
	/** @param value HTTP-date. @return A new header. */
	public static IfRange ifRange(ZonedDateTime value) { return IfRange.of(value); }
	/** @param value Supplier of tag or date. @return A new header. */
	public static IfRange ifRange(Supplier<?> value) { return IfRange.of(value); }

	/** @param value The header value. @return A new header. */
	public static IfUnmodifiedSince ifUnmodifiedSince(String value) { return IfUnmodifiedSince.of(value); }
	/** @param value HTTP-date. @return A new header. */
	public static IfUnmodifiedSince ifUnmodifiedSince(ZonedDateTime value) { return IfUnmodifiedSince.of(value); }

	/** @param value The header value. @return A new header. */
	public static LastModified lastModified(String value) { return LastModified.of(value); }
	/** @param value HTTP-date. @return A new header. */
	public static LastModified lastModified(ZonedDateTime value) { return LastModified.of(value); }

	/** @param value The header value. @return A new header. */
	public static Location location(String value) { return Location.of(value); }
	/** @param uri Redirect URI. @return A new header. */
	public static Location location(URI uri) { return Location.of(uri); }

	/** @param value The header value. @return A new header. */
	public static MaxForwards maxForwards(String value) { return MaxForwards.of(value); }
	/** @param hops Max forwards count. @return A new header. */
	public static MaxForwards maxForwards(int hops) { return MaxForwards.of(hops); }

	/** @param value The header value. @return A new header. */
	public static Origin origin(String value) { return Origin.of(value); }

	/** @param value The header value. @return A new header. */
	public static Pragma pragma(String value) { return Pragma.of(value); }

	/** @param value The header value. @return A new header. */
	public static ProxyAuthenticate proxyAuthenticate(String value) { return ProxyAuthenticate.of(value); }

	/** @param value The header value. @return A new header. */
	public static ProxyAuthorization proxyAuthorization(String value) { return ProxyAuthorization.of(value); }

	/** @param value The header value. @return A new header. */
	public static Range range(String value) { return Range.of(value); }

	/** @param value The header value. @return A new header. */
	public static Referer referer(String value) { return Referer.of(value); }
	/** @param uri Referer URI. @return A new header. */
	public static Referer referer(URI uri) { return Referer.of(uri); }

	/** @param value The header value. @return A new header. */
	public static RetryAfter retryAfter(String value) { return RetryAfter.of(value); }
	/** @param seconds Delta seconds. @return A new header. */
	public static RetryAfter retryAfter(int seconds) { return RetryAfter.of(Integer.valueOf(seconds)); }
	/** @param value HTTP-date. @return A new header. */
	public static RetryAfter retryAfter(ZonedDateTime value) { return RetryAfter.of(value); }
	/** @param value Supplier of seconds or date. @return A new header. */
	public static RetryAfter retryAfter(Supplier<?> value) { return RetryAfter.of(value); }

	/** @param value The header value. @return A new header. */
	public static Server server(String value) { return Server.of(value); }

	/** @param value The header value. @return A new header. */
	public static TE te(String value) { return TE.of(value); }
	/** @param value Parsed string ranges. @return A new header. */
	public static TE te(StringRanges value) { return TE.of(value); }
	/** @param value Supplier of the wire header value. @return A new header. */
	public static TE teLazyWire(Supplier<String> value) { return TE.ofLazyWire(value); }
	/** @param value Supplier of string ranges. @return A new header. */
	public static TE teLazyParsed(Supplier<StringRanges> value) { return TE.ofLazyParsed(value); }

	/** @param value The header value. @return A new header. */
	public static Trailer trailer(String value) { return Trailer.of(value); }

	/** @param value The header value. @return A new header. */
	public static TransferEncoding transferEncoding(String value) { return TransferEncoding.of(value); }

	/** @param value The header value. @return A new header. */
	public static Upgrade upgrade(String value) { return Upgrade.of(value); }

	/** @param value The header value. @return A new header. */
	public static UserAgent userAgent(String value) { return UserAgent.of(value); }
	/** @param value The header value supplier. @return A new header. */
	public static UserAgent userAgent(Supplier<String> value) { return UserAgent.of(value); }

	/** @param value The header value. @return A new header. */
	public static Vary vary(String value) { return Vary.of(value); }

	/** @param value The header value. @return A new header. */
	public static Via via(String value) { return Via.of(value); }

	/** @param value The header value. @return A new header. */
	public static Warning warning(String value) { return Warning.of(value); }

	/** @param value The header value. @return A new header. */
	public static WwwAuthenticate wwwAuthenticate(String value) { return WwwAuthenticate.of(value); }

	// Juneau-specific headers
	/** @param value The header value. @return A new header. */
	public static NoTrace noTrace(String value) { return NoTrace.of(value); }
	/** @return A no-trace header with value {@code "true"}. */
	public static NoTrace noTrace() { return NoTrace.of("true"); }

	/** @param value The header value. @return A new header. */
	public static Debug debug(String value) { return Debug.of(value); }
	/** @return A debug header with value {@code "true"}. */
	public static Debug debug() { return Debug.of("true"); }

	/** @param value The header value. @return A new header. */
	public static ClientVersion clientVersion(String value) { return ClientVersion.of(value); }

	/** @param value The header value. @return A new header. */
	public static Thrown thrown(String value) { return Thrown.of(value); }

	/**
	 * Creates a generic {@link HttpHeader} with the given name and value.
	 *
	 * @param name The header name. Must not be <jk>null</jk>.
	 * @param value The header value. May be <jk>null</jk>.
	 * @return A new header. Never <jk>null</jk>.
	 */
	public static HttpHeader header(String name, String value) {
		return HttpHeaderBean.of(name, value);
	}

	/**
	 * Creates a generic {@link HttpHeader} with a lazy value supplier.
	 *
	 * @param name The header name. Must not be <jk>null</jk>.
	 * @param valueSupplier Supplier for the header value. Must not be <jk>null</jk>.
	 * @return A new header. Never <jk>null</jk>.
	 */
	public static HttpHeader header(String name, Supplier<String> valueSupplier) {
		return HttpHeaderBean.of(name, valueSupplier);
	}
}
