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

import org.apache.juneau.commons.runtime.*;

/**
 * Property source backed by {@link Args}.
 */
public class ArgsPropertySource implements PropertySource {

	private final Supplier<Args> argsSupplier;

	/**
	 * Constructor.
	 *
	 * @param argsSupplier Supplier for args.
	 */
	public ArgsPropertySource(Supplier<Args> argsSupplier) {
		this.argsSupplier = argsSupplier;
	}

	/**
	 * Creates a source using the default command-line discovery.
	 *
	 * @return A new source.
	 */
	public static ArgsPropertySource createDefault() {
		return new ArgsPropertySource(ArgsPropertySource::createDefaultArgs);
	}

	public static Args createDefaultArgs() {
		var s = System.getProperty("sun.java.command");
		if (ne(s)) {
			var i = s.indexOf(' ');
			return new Args(i == -1 ? "" : s.substring(i + 1));
		}
		return new Args(System.getProperty("juneau.args", ""));
	}

	@Override
	public PropertyLookupResult get(String name) {
		var args = argsSupplier.get();
		if (args == null)
			return PropertyLookupResult.missing();
		try {
			var index = Integer.parseInt(name);
			var v = args.get(index);
			return v.isPresent() ? PropertyLookupResult.present(v) : PropertyLookupResult.missing();
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			// Fall through.
		}
		var values = args.getAll(name);
		if (! values.isEmpty())
			return PropertyLookupResult.present(opt(String.join(",", values)));
		var v = args.get(name);
		return v.isPresent() ? PropertyLookupResult.present(v) : PropertyLookupResult.missing();
	}
}
