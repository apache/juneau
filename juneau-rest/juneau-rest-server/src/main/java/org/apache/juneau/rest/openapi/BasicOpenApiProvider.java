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
package org.apache.juneau.rest.openapi;

import static org.apache.juneau.commons.utils.Utils.*;

import java.util.*;

import org.apache.juneau.bean.openapi3.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.rest.*;

/**
 * Basic implementation of an {@link OpenApiProvider}.
 *
 * <p>
 * Default provider used by {@link RestContext} when no override is configured. Creates a fresh
 * {@link BasicOpenApiProviderSession} per call, which walks the {@code @Rest} / {@code @RestOp}
 * annotation graph and emits an {@link OpenApi} 3.1 document.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerOpenApi">OpenAPI 3.1 Server Emission</a>
 * </ul>
 */
public class BasicOpenApiProvider implements OpenApiProvider {

	private final VarResolver vr;
	private final JsonSchemaGenerator js;
	private final Messages messages;
	private final FileFinder fileFinder;
	private final BeanStore beanStore;

	/**
	 * Constructor.
	 *
	 * @param beanStore The bean store containing injectable beans for this provider.
	 */
	public BasicOpenApiProvider(BeanStore beanStore) {
		// @formatter:off
		this(
			OpenApiProvider
				.create(beanStore)
				.varResolver(()->beanStore.getBean(VarResolver.class).get())
				.fileFinder(()->beanStore.getBean(FileFinder.class).get())
				.messages(()->beanStore.getBean(Messages.class).get())
				.jsonSchemaGenerator(()->beanStore.getBean(JsonSchemaGenerator.class).get())
		);
		// @formatter:on
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this OpenAPI provider.
	 */
	public BasicOpenApiProvider(OpenApiProvider.Builder builder) {
		this.beanStore = builder.beanStore;
		this.vr = builder.varResolver().orElse(beanStore.getBean(VarResolver.class).orElse(VarResolver.DEFAULT));
		this.js = builder.jsonSchemaGenerator().orElse(beanStore.getBean(JsonSchemaGenerator.class).orElse(JsonSchemaGenerator.DEFAULT));
		this.messages = builder.messages().orElse(null);
		this.fileFinder = builder.fileFinder().orElse(null);
	}

	/**
	 * Returns the OpenAPI 3.1 document associated with the specified context of a {@link Rest}-annotated class.
	 *
	 * <p>
	 * Subclasses can override this to provide their own method for generating the document.
	 *
	 * @param context The context of the {@link Rest}-annotated class.
	 * @param locale The request locale.
	 * @return A new {@link OpenApi} object.
	 * @throws Exception If an error occurred producing the document.
	 */
	@Override /* Overridden from OpenApiProvider */
	public OpenApi getOpenApi(RestContext context, Locale locale) throws Exception {

		var c = context.getResourceClass();
		var ff = nn(fileFinder) ? fileFinder : FileFinder.create(beanStore).cp(c, null, false).build();
		var mb = nn(messages) ? messages.forLocale(locale) : Messages.create(c).build().forLocale(locale);
		var vrs = vr.createSession().bean(Messages.class, mb);
		var session = new BasicOpenApiProviderSession(context, locale, ff, mb, vrs, js.getSession());

		return session.getOpenApi();
	}
}
