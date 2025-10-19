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
package org.apache.juneau.rest.swagger;

import java.util.*;

import org.apache.juneau.bean.swagger.Swagger;
import org.apache.juneau.cp.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.svl.*;

/**
 * Basic implementation of a {@link SwaggerProvider}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauBeanSwagger2">juneau-bean-swagger-v2</a>
 * </ul>
 */
public class BasicSwaggerProvider implements SwaggerProvider {

	private final VarResolver vr;
	private final JsonSchemaGenerator js;
	private final Messages messages;
	private final FileFinder fileFinder;
	private final BeanStore beanStore;

	/**
	 * Constructor.
	 *
	 * @param beanStore The bean store containing injectable beans for this logger.
	 */
	public BasicSwaggerProvider(BeanStore beanStore) {
		// @formatter:off
		this(
			SwaggerProvider
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
	 * @param builder The builder containing the settings for this Swagger provider.
	 */
	public BasicSwaggerProvider(SwaggerProvider.Builder builder) {
		this.beanStore = builder.beanStore;
		this.vr = builder.varResolver().orElse(beanStore.getBean(VarResolver.class).orElse(VarResolver.DEFAULT));
		this.js = builder.jsonSchemaGenerator().orElse(beanStore.getBean(JsonSchemaGenerator.class).orElse(JsonSchemaGenerator.DEFAULT));
		this.messages = builder.messages().orElse(null);
		this.fileFinder = builder.fileFinder().orElse(null);
	}

	/**
	 * Returns the Swagger associated with the specified context of a {@link Rest}-annotated class.
	 *
	 * <p>
	 * Subclasses can override this to provide their own method for generating Swagger.
	 *
	 * @param context The context of the {@link Rest}-annotated class.
	 * @param locale The request locale.
	 * @return A new {@link Swagger} object.
	 * @throws Exception If an error occurred producing the Swagger.
	 */
	@Override /* Overridden from SwaggerProvider */
	public Swagger getSwagger(RestContext context, Locale locale) throws Exception {

		Class<?> c = context.getResourceClass();
		FileFinder ff = fileFinder != null ? fileFinder : FileFinder.create(beanStore).cp(c, null, false).build();
		Messages mb = messages != null ? messages.forLocale(locale) : Messages.create(c).build().forLocale(locale);
		VarResolverSession vrs = vr.createSession().bean(Messages.class, mb);
		BasicSwaggerProviderSession session = new BasicSwaggerProviderSession(context, locale, ff, messages, vrs, js.getSession());

		return session.getSwagger();
	}
}