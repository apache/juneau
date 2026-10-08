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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

/**
 * One named toolkit pack: ordered CSS and JS asset paths, the packs it depends on, its {@link Kind}, and the
 * resolver that turns each path into a URL.
 *
 * <p>
 * Register one through {@code ConsoleFreemarkerMixin.Builder.registerToolkitPack(ToolkitPack)}.  A pack registered
 * under an existing name replaces that pack completely, including its {@code dependsOn}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	ToolkitPack.<jsm>create</jsm>(<js>"datatables-export"</js>)
 * 		.js(<jv>jszip</jv>, <jv>pdfmake</jv>, <jv>vfsFonts</jv>)
 * 		.dependsOn(<js>"datatables-buttons"</js>)
 * 		.kind(ToolkitPack.Kind.<jsf>VENDOR</jsf>)
 * 		.resolver(<jv>myResolver</jv>)
 * 		.build();
 * </p>
 *
 * @since 10.0.0
 */
public final class ToolkitPack {

	/**
	 * Where a pack's assets are emitted on the page.
	 *
	 * <p>
	 * {@link #VENDOR} CSS goes before the page {@code css=} and {@link #VENDOR} JS before every {@link #RUNTIME} script.
	 * {@link #RUNTIME} assets keep the position the {@code "views"} runtime always had.  A {@link #VENDOR} pack can't
	 * depend on a {@link #RUNTIME} pack.
	 */
	public enum Kind {
		/** A library or glue that runtime packs build on. */
		VENDOR,
		/** A first-party UI runtime ({@code "views"}, {@code "calendar"}). */
		RUNTIME
	}

	private final String name;
	private final List<String> cssPaths;
	private final List<String> jsPaths;
	private final List<String> dependsOn;
	private final Kind kind;
	private final ToolkitPackRegistry.AssetUrlResolver resolver;

	private ToolkitPack(Builder b) {
		name = b.name;
		cssPaths = List.copyOf(b.cssPaths);
		jsPaths = List.copyOf(b.jsPaths);
		dependsOn = List.copyOf(b.dependsOn);
		kind = b.kind;
		resolver = b.resolver;
	}

	/**
	 * Starts a pack.
	 *
	 * @param name The pack name.  Must not be blank.
	 * @return A new builder.
	 */
	public static Builder create(String name) {
		if (ib(name))
			throw new IllegalArgumentException("Toolkit pack name must not be blank.");
		return new Builder(name.trim());
	}

	/**
	 * Returns the pack name.
	 *
	 * @return The pack name.
	 */
	public String name() { return name; }

	/**
	 * Returns the ordered CSS asset paths.
	 *
	 * @return The ordered CSS asset paths.
	 */
	public List<String> cssPaths() { return cssPaths; }

	/**
	 * Returns the ordered JS asset paths.
	 *
	 * @return The ordered JS asset paths.
	 */
	public List<String> jsPaths() { return jsPaths; }

	/**
	 * Returns the names of the packs this one loads after, in order.
	 *
	 * @return The names of the packs this one loads after, in order.
	 */
	public List<String> dependsOn() { return dependsOn; }

	/**
	 * Returns the pack kind.
	 *
	 * @return The pack kind.
	 */
	public Kind kind() { return kind; }

	/**
	 * Returns the asset-URL resolver.
	 *
	 * @return The asset-URL resolver.
	 */
	public ToolkitPackRegistry.AssetUrlResolver resolver() { return resolver; }

	/** Builder for {@link ToolkitPack}. */
	public static final class Builder {

		final String name;
		final List<String> cssPaths = new ArrayList<>();
		final List<String> jsPaths = new ArrayList<>();
		final List<String> dependsOn = new ArrayList<>();
		Kind kind;
		ToolkitPackRegistry.AssetUrlResolver resolver = ToolkitPackRegistry.VIEWS_RESOLVER;

		Builder(String name) {
			this.name = name;
		}

		/**
		 * Appends CSS asset paths.
		 *
		 * @param paths The paths.
		 * @return This object.
		 */
		public Builder css(String...paths) { return css(Arrays.asList(paths)); }

		/**
		 * Appends CSS asset paths.
		 *
		 * @param paths The paths.
		 * @return This object.
		 */
		public Builder css(List<String> paths) { cssPaths.addAll(paths); return this; }

		/**
		 * Appends JS asset paths.
		 *
		 * @param paths The paths.
		 * @return This object.
		 */
		public Builder js(String...paths) { return js(Arrays.asList(paths)); }

		/**
		 * Appends JS asset paths.
		 *
		 * @param paths The paths.
		 * @return This object.
		 */
		public Builder js(List<String> paths) { jsPaths.addAll(paths); return this; }

		/**
		 * Appends the names of packs this one depends on.
		 *
		 * @param names The pack names.
		 * @return This object.
		 */
		public Builder dependsOn(String...names) { dependsOn.addAll(Arrays.asList(names)); return this; }

		/**
		 * Sets the pack kind.  Required.
		 *
		 * @param value The kind.
		 * @return This object.
		 */
		public Builder kind(Kind value) { kind = rnn(value); return this; }

		/**
		 * Sets the asset-URL resolver.  Defaults to {@link ToolkitPackRegistry#VIEWS_RESOLVER}.
		 *
		 * @param value The resolver.
		 * @return This object.
		 */
		public Builder resolver(ToolkitPackRegistry.AssetUrlResolver value) { resolver = rnn(value); return this; }

		/**
		 * Builds the pack.
		 *
		 * @return A new pack.
		 * @throws IllegalArgumentException If no kind was set.
		 */
		public ToolkitPack build() {
			if (n(kind))
				throw new IllegalArgumentException(String.format("Toolkit pack '%s' needs kind(VENDOR) or kind(RUNTIME).", name));
			return new ToolkitPack(this);
		}
	}
}
