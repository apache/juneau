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
package org.apache.juneau.junit.bct.annotations;

import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.junit.bct.BctConfiguration.*;
import static org.apache.juneau.commons.lang.TriState.*;

import java.util.*;

import org.apache.juneau.junit.bct.*;
import org.junit.jupiter.api.extension.*;

/**
 * JUnit 5 extension that processes {@link BctConfig} annotations.
 *
 * <p>
 * This extension automatically sets BCT settings before test execution and clears them after.
 * It supports both class-level and method-level annotations, with method-level taking precedence.
 */
public class BctConfigExtension implements BeforeEachCallback, AfterEachCallback {

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		var al = getAnnotations(context);

		if (al.isEmpty())
			return;

		clear();

		al.stream().map(x -> x.sortMaps()).filter(x -> neq(x, UNSET)).findFirst().ifPresent(x -> set(BCT_SORT_MAPS, x == TRUE));
		al.stream().map(x -> x.sortCollections()).filter(x -> neq(x, UNSET)).findFirst().ifPresent(x -> set(BCT_SORT_COLLECTIONS, x == TRUE));
		al.stream().map(x -> x.beanConverter()).filter(x -> neq(x, BeanConverter.class)).findFirst().map(x -> eq(x, BasicBeanConverter.class) ? null : x).ifPresent(x -> setConverter(x));
	}

	@SuppressWarnings("java:S3011")
	private static void setConverter(Class<? extends BeanConverter> x) {
		safe(()->{
			var c = x.getDeclaredConstructor();
			c.setAccessible(true);
			set(c.newInstance());
		}, e -> rex(e, "Failed to instantiate BeanConverter: {0}. It must have a no-arg constructor.", x.getName()));
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		var al = getAnnotations(context);

		if (al.isEmpty())
			return;

		clear();
	}

	private static List<BctConfig> getAnnotations(ExtensionContext context) {
		var l = new ArrayList<BctConfig>();
		context.getTestMethod().map(x -> x.getAnnotation(BctConfig.class)).ifPresent(l::add);
		context.getTestClass().map(x -> x.getAnnotation(BctConfig.class)).ifPresent(l::add);
		return l;
	}
}
