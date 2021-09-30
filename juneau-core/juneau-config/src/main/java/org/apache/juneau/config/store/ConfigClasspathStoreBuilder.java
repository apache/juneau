// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.config.store;

import java.lang.reflect.*;
import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * Builder for {@link ConfigClasspathStore} objects.
 * {@review}
 */
@FluentSetters
public class ConfigClasspathStoreBuilder extends ConfigStoreBuilder {

	/**
	 * Constructor, default settings.
	 */
	protected ConfigClasspathStoreBuilder() {
		super();
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The bean to copy from.
	 */
	protected ConfigClasspathStoreBuilder(ConfigClasspathStore copyFrom) {
		super(copyFrom);
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The builder to copy from.
	 */
	protected ConfigClasspathStoreBuilder(ConfigClasspathStoreBuilder copyFrom) {
		super(copyFrom);
	}

	@Override /* ContextBuilder */
	public ConfigClasspathStoreBuilder copy() {
		return new ConfigClasspathStoreBuilder(this);
	}

	@Override /* ContextBuilder */
	public ConfigClasspathStore build() {
		return new ConfigClasspathStore(this);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - ContextBuilder */
	public ConfigClasspathStoreBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigClasspathStoreBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigClasspathStoreBuilder apply(AnnotationWorkList work) {
		super.apply(work);
		return this;
	}

	@Override /* GENERATED - ContextBuilder */
	public ConfigClasspathStoreBuilder debug() {
		super.debug();
		return this;
	}

	// </FluentSetters>
}
