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

import java.util.function.*;

import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.runtime.*;

/**
 * Property source backed by {@link ManifestFile}.
 */
public class ManifestFilePropertySource implements PropertySource {

	private final Supplier<ManifestFile> manifestSupplier;

	/**
	 * Constructor.
	 *
	 * <p>
	 * The supplied {@code manifestSupplier} is wrapped in a {@link Memoizer} so the manifest is loaded at most once
	 * per source instance.  Without this, every {@link #get(String)} call would re-scan the classpath, which is very
	 * expensive and shows up as a 2x test-suite regression when this source is registered in {@link Settings}.
	 *
	 * @param manifestSupplier The supplier for manifest file.  Must not be <jk>null</jk>.
	 */
	public ManifestFilePropertySource(Supplier<ManifestFile> manifestSupplier) {
		this.manifestSupplier = new Memoizer<>(manifestSupplier);
	}

	/**
	 * Creates a source with default classloader scanning behavior.
	 *
	 * @return A new source.
	 */
	public static ManifestFilePropertySource createDefault() {
		return new ManifestFilePropertySource(() -> {
			try {
				return new ManifestFile(ManifestFilePropertySource.class);
			} catch (@SuppressWarnings("unused") Exception unused) {
				return null;
			}
		});
	}

	@Override
	public PropertyLookupResult get(String name) {
		var mf = manifestSupplier.get();
		if (mf == null)
			return PropertyLookupResult.missing();
		var v = mf.get(name);
		return v.isPresent() ? PropertyLookupResult.present(v) : PropertyLookupResult.missing();
	}
}
