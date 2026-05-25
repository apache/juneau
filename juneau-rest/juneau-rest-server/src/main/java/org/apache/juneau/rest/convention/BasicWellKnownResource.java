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
package org.apache.juneau.rest.convention;

import java.io.*;

import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Mixin that serves <a href="https://www.rfc-editor.org/rfc/rfc8615">RFC 8615</a>
 * {@code /.well-known/*} discovery endpoints.
 *
 * <p>
 * Sibling of {@link BasicFaviconResource}, {@link BasicSeoResource}, and
 * {@link BasicVersionResource}. All four classes live in the
 * {@code org.apache.juneau.rest.convention} convention-endpoints mixin pack.
 *
 * <p>
 * In v1 the mixin mounts a single literal path &mdash; {@code /.well-known/security.txt} per
 * <a href="https://www.rfc-editor.org/rfc/rfc9116">RFC 9116</a>. The class is structured so that
 * future entries (for example {@code /.well-known/openid-configuration} from a separate AuthN
 * mixin pack) can be added without touching this source: each future entry ships its own mixin
 * class with its own {@link Rest#paths() @Rest(paths)} declaration.
 *
 * <p>
 * Compose into a host resource via
 * {@link Rest#mixins() @Rest(mixins=BasicWellKnownResource.class)}; the
 * {@code /.well-known/security.txt} URL becomes available alongside the host's own endpoints with
 * no further wiring.
 *
 * <h5 class='section'>Hardcoded mount path:</h5>
 *
 * <p>
 * Unlike the sibling api-docs and ops mixins (see {@link BasicSwaggerResource},
 * {@link BasicVersionResource}, etc.), the mount path here is <b>not</b> SVL-configurable
 * &mdash; {@code /.well-known/security.txt} is fixed by
 * <a href="https://www.rfc-editor.org/rfc/rfc8615">RFC 8615</a> (Well-Known URIs) and
 * <a href="https://www.rfc-editor.org/rfc/rfc9116">RFC 9116</a> (security.txt). Both RFCs are
 * explicit that the {@code /.well-known/} prefix is the discovery convention and that the
 * filename suffix is the protocol-defined registry key &mdash; a runtime override here would
 * make the endpoint undiscoverable by spec-compliant clients.
 *
 * <h5 class='section'>Mixin-only deployment:</h5>
 *
 * <p>
 * This resource is designed for composition via {@code @Rest(mixins=...)}. The mount path is
 * pinned at the op level by {@link RestGet @RestGet(path="/.well-known/security.txt")} on
 * {@link #getSecurityTxt}; a class-level {@code @Rest(paths=...)} declaration would be silently
 * ignored under the mixin pattern (see {@link Rest#paths() @Rest(paths)} Javadoc).
 *
 * <h5 class='figure'>Composition example:</h5>
 *
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(path=<js>"/api"</js>, mixins=BasicWellKnownResource.<jk>class</jk>)
 * 	<jk>public class</jk> ApiResource <jk>extends</jk> RestServlet { }
 *
 * 	<jc>// Configure the security.txt body via a @Bean factory:</jc>
 * 	<ja>@Bean</ja> BasicWellKnownResource wellKnown() {
 * 		<jk>return</jk> BasicWellKnownResource.<jsm>create</jsm>()
 * 			.securityTxt(<js>"Contact: security@example.com\nExpires: 2027-01-01T00:00:00Z\n"</js>)
 * 			.build();
 * 	}
 * </p>
 *
 * <h5 class='section'>Defaults &amp; behavior:</h5>
 *
 * <ul class='spaced-list'>
 * 	<li><b>No default body.</b> Per RFC 9116, {@code security.txt}'s mere presence is meaningful;
 * 		shipping a placeholder body would be misleading. If the builder is not given a body via
 * 		{@link Builder#securityTxt(String) securityTxt(String)}, the endpoint returns
 * 		{@code 404 Not Found} (i.e. "we don't have one"). Loud documentation in topic page.
 * 	<li><b>Content-Type.</b> {@code text/plain; charset=UTF-8} when a body is configured.
 * 	<li><b>Excluded from generated Swagger / OpenAPI specs</b> via
 * 		{@link OpSwagger#ignore() @OpSwagger(ignore=true)}.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link BasicFaviconResource}
 * 	<li class='jc'>{@link BasicSeoResource}
 * 	<li class='jc'>{@link BasicVersionResource}
 * 	<li class='link'><a class="doclink" href="https://www.rfc-editor.org/rfc/rfc8615">RFC 8615 &mdash; Well-Known URIs</a>
 * 	<li class='link'><a class="doclink" href="https://www.rfc-editor.org/rfc/rfc9116">RFC 9116 &mdash; security.txt</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerComposition">REST Server &mdash; Composition (mixins, paths)</a>
 * </ul>
 *
 * @since 9.5.0
 */
// @formatter:off
@Rest
public class BasicWellKnownResource {

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	private final String securityTxt;

	/** No-arg constructor &mdash; results in a 404 on {@code /.well-known/security.txt}. */
	public BasicWellKnownResource() {
		this(create());
	}

	/**
	 * Builder constructor.
	 *
	 * @param builder The builder.
	 */
	protected BasicWellKnownResource(Builder builder) {
		securityTxt = builder.securityTxt;
	}

	/**
	 * [GET /.well-known/security.txt] &mdash; emit the configured RFC 9116 body, or 404 when
	 * unset.
	 *
	 * @param res The current REST response.
	 * @throws IOException If an I/O error occurs while writing the response.
	 * @throws NotFound If no {@code security.txt} body is configured.
	 */
	@RestGet(
		path="/.well-known/security.txt",
		summary="security.txt",
		description="RFC 9116 security.txt — disclosure / contact info for the operator.",
		swagger=@OpSwagger(ignore=true)
	)
	public void getSecurityTxt(RestResponse res) throws IOException {
		if (securityTxt == null)
			throw new NotFound();
		try (var w = res.getDirectWriter("text/plain; charset=UTF-8")) {
			w.write(securityTxt);
		}
	}

	/**
	 * Returns the configured {@code security.txt} body (test/inspection helper).
	 *
	 * @return The body, or <jk>null</jk> if unset.
	 */
	public String getSecurityTxtBody() {
		return securityTxt;
	}

	/**
	 * Builder for {@link BasicWellKnownResource} instances.
	 */
	public static class Builder {

		private String securityTxt;

		/** Constructor &mdash; package access for {@link BasicWellKnownResource#create()}. */
		protected Builder() {}

		/**
		 * Sets the {@code security.txt} body that will be served at
		 * {@code /.well-known/security.txt}.
		 *
		 * <p>
		 * Per RFC 9116, the body must include at least one {@code Contact:} field and an
		 * {@code Expires:} field; the mixin does not validate the content &mdash; that's the
		 * caller's responsibility.
		 *
		 * @param value The body content. Must not be <jk>null</jk> or blank.
		 * @return This object.
		 */
		public Builder securityTxt(String value) {
			securityTxt = value;
			return this;
		}

		/**
		 * Builds a {@link BasicWellKnownResource} instance.
		 *
		 * @return A configured instance.
		 */
		public BasicWellKnownResource build() {
			return new BasicWellKnownResource(this);
		}
	}
}
