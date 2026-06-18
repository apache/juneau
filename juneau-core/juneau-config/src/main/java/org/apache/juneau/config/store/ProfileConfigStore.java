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
package org.apache.juneau.config.store;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.config.format.*;
import org.apache.juneau.config.internal.*;
import org.apache.juneau.marshall.*;

/**
 * A {@link ConfigStore} decorator that layers <b>profile overlays</b> over a delegate store's base config.
 *
 * <p>
 * Given a set of active profiles (e.g. {@code ["stage"]} or {@code ["base","cloud"]}) and a base config name
 * {@code N}, a {@code read(N)} returns the base contents from the delegate with each active profile's
 * {@code N-<profile>} overlay merged on top &mdash; profile wins over base, last-active-profile wins over earlier
 * ones (see {@link ProfileMerge}).  Reads for any name <i>other</i> than the configured base name pass straight
 * through to the delegate.
 *
 * <p>
 * <b>Live reload:</b> the store registers a listener on the delegate for the base name <i>and</i> each profile name,
 * so a change to the base file or any active profile file re-runs the merge and re-fires the change to this store's
 * own listeners (the {@link ConfigMap} sees a fresh merged contents and reloads).
 *
 * <p>
 * The profile file name is derived from the base name by inserting {@code -<profile>} before the extension
 * (e.g. base {@code my.cfg} + profile {@code stage} &rarr; {@code my-stage.cfg}); a base with no extension simply
 * appends {@code -<profile>}.  Writes, existence checks, and name resolution delegate to the wrapped store.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // Decorator owns the wrapped delegate store and closes it in close(); the delegate's lifecycle is managed here, not leaked.
})
public class ProfileConfigStore extends ConfigStore {

	/**
	 * Builder class.
	 */
	public static class Builder extends ConfigStore.Builder<Builder> {

		ConfigStore delegate;
		String baseName;
		List<String> profiles = new ArrayList<>();
		ConfigFormat format = IniConfigFormat.INSTANCE;

		/** Constructor, default settings. */
		protected Builder() {}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.  Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull("copyFrom", copyFrom));
			this.delegate = copyFrom.delegate;
			this.baseName = copyFrom.baseName;
			this.profiles = new ArrayList<>(copyFrom.profiles);
			this.format = copyFrom.format;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.  Cannot be <jk>null</jk>.
		 */
		protected Builder(ProfileConfigStore copyFrom) {
			super(assertArgNotNull("copyFrom", copyFrom));
			type(copyFrom.getClass());
			this.delegate = copyFrom.delegate;
			this.baseName = copyFrom.baseName;
			this.profiles = new ArrayList<>(copyFrom.profiles);
			this.format = copyFrom.format;
		}

		/**
		 * Sets the wrapped delegate store that supplies the base + profile file contents.
		 *
		 * @param value The delegate store.
		 * @return This object.
		 */
		public Builder delegate(ConfigStore value) { delegate = value; return this; }

		/**
		 * Sets the base config name whose {@code -<profile>} overlays are merged.
		 *
		 * @param value The base config name.
		 * @return This object.
		 */
		public Builder baseName(String value) { baseName = value; return this; }

		/**
		 * Sets the active profiles, in activation order (last-active-profile wins).
		 *
		 * @param value The active profile names.
		 * @return This object.
		 */
		public Builder profiles(List<String> value) { profiles = value == null ? new ArrayList<>() : new ArrayList<>(value); return this; }

		/**
		 * Sets the config format used to parse the base + profile files.
		 *
		 * @param value The format.
		 * @return This object.
		 */
		public Builder format(ConfigFormat value) { format = value == null ? IniConfigFormat.INSTANCE : value; return this; }

		@Override /* Overridden from Context.Builder<?> */
		public ProfileConfigStore build() {
			return build(ProfileConfigStore.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public Builder copy() {
			return new Builder(this);
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	private final ConfigStore delegate;
	private final String baseName;
	private final List<String> profiles;
	private final ConfigFormat format;
	private final ConfigStoreListener reloadListener;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public ProfileConfigStore(Builder builder) {
		super(builder);
		this.delegate = assertArgNotNull("delegate", builder.delegate);
		this.baseName = assertArgNotNull("baseName", builder.baseName);
		this.profiles = List.copyOf(builder.profiles);
		this.format = builder.format;

		// Re-merge + re-notify whenever the base or any active profile file changes underneath us.
		this.reloadListener = contents -> {
			try {
				update(baseName, mergedContents());
			} catch (IOException e) {  // HTT: re-merge IO failure only on a real FileStore IO error mid-reload; not reproducible with in-memory stores.
				throw new ConfigException(e, "Failed to re-merge profile overlays for ''{0}''", baseName);
			}
		};
		delegate.register(baseName, reloadListener);
		for (var p : profiles)
			delegate.register(profileName(p), reloadListener);
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Closeable */
	public void close() throws IOException {
		delegate.unregister(baseName, reloadListener);
		for (var p : profiles)
			delegate.unregister(profileName(p), reloadListener);
		delegate.close();
	}

	@Override /* Overridden from ConfigStore */
	public boolean exists(String name) {
		return delegate.exists(name);
	}

	@Override /* Overridden from ConfigStore */
	public String read(String name) throws IOException {
		if (! eq(name, baseName))
			return delegate.read(name);
		return mergedContents();
	}

	@Override /* Overridden from ConfigStore */
	public String write(String name, String expectedContents, String newContents) throws IOException {
		return delegate.write(name, expectedContents, newContents);
	}

	/**
	 * Reads the base + each active profile from the delegate and merges them (profile-wins, last-active-wins).
	 */
	private String mergedContents() throws IOException {
		var base = delegate.read(baseName);
		if (profiles.isEmpty())
			return base;
		var overlays = new ArrayList<String>(profiles.size());
		for (var p : profiles)
			overlays.add(delegate.read(profileName(p)));
		return ProfileMerge.merge(delegate, baseName, base, overlays, format);
	}

	/**
	 * Derives the profile file name from the base name by inserting {@code -<profile>} before the extension.
	 *
	 * @param profile The profile name.
	 * @return The profile config name.
	 */
	String profileName(String profile) {
		var dot = baseName.lastIndexOf('.');
		if (dot < 0)
			return baseName + "-" + profile;
		return baseName.substring(0, dot) + "-" + profile + baseName.substring(dot);
	}
}
