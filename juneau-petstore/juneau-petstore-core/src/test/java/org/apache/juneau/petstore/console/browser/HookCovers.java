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
package org.apache.juneau.petstore.console.browser;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Marks a browser test as exercising one or more of the toolkit's public hook namespaces
 * ({@code JuneauViews.HOOK_NAMESPACES}) through a real petstore page: a direct call from petstore script, or
 * declarative petstore config that drives the namespace.  {@code PetstoreCoverage_Test} checks the claims against
 * the registry the shipped script exposes.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@HookCovers</ja>(<js>"rowActions"</js>)
 * 	<jk>class</jk> PetstoreTypedConfirm_BrowserTest <jk>extends</jk> TestBase { ... }
 * </p>
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface HookCovers {

	/** @return The hook namespace names, e.g. {@code "dialogs"}. */
	String[] value();
}
