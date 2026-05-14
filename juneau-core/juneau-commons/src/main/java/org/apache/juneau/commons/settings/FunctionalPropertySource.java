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
package org.apache.juneau.commons.settings;

import static org.apache.juneau.commons.utils.Utils.*;

import java.util.function.*;

/**
 * A functional interface for creating read-only {@link PropertySource} instances from a function.
 */
@FunctionalInterface
public interface FunctionalPropertySource extends PropertySource {

	@Override
	PropertyLookupResult get(String name);

	/**
	 * Creates a functional source from a function that returns a string.
	 *
	 * @param function The function to delegate property lookups to. Must not be <c>null</c>.
	 * @return A new functional property source instance.
	 */
	static FunctionalPropertySource of(UnaryOperator<String> function) {
		return name -> {
			var v = function.apply(name);
			return v == null ? PropertyLookupResult.missing() : PropertyLookupResult.present(opt(v));
		};
	}
}
