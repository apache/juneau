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

/**
 * Wrapper bean for the resource class associated with an OpenAPI provider.
 *
 * <p>
 * Registered in the bean store so that {@link OpenApiProvider} implementations and their builders may
 * declare it as a constructor or method-injected dependency rather than discovering it from the request
 * context. Mirrors {@link org.apache.juneau.rest.swagger.SwaggerResource} for the OpenAPI 3.1 path.
 *
 * @param value The resource class. May be {@code null} if not associated with a specific resource.
 */
public record OpenApiResource(Class<?> value) {

	/**
	 * Convenience factory for an {@code OpenApiResource} wrapping the given class.
	 *
	 * @param resourceClass The resource class. Can be {@code null}.
	 * @return A new {@code OpenApiResource}.
	 */
	public static OpenApiResource of(Class<?> resourceClass) {
		return new OpenApiResource(resourceClass);
	}
}
