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
package org.apache.juneau;

import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static java.util.Collections.*;

import java.text.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;

/**
 * A one-time-use non-thread-safe object that's meant to be used once and then thrown away.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not typically thread safe.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public abstract class ContextSession {

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static abstract class Builder {
		Context ctx;
		JsonMap properties;
		boolean unmodifiable;
		Boolean debug;

		/**
		 * Constructor.
		 *
		 * @param ctx The context creating this session.
		 */
		protected Builder(Context ctx) {
			this.ctx = ctx;
			debug = ctx.debug;
		}

		/**
		 * Build the object.
		 *
		 * @return The built object.
		 */
		public abstract ContextSession build();

		/**
		 * Debug mode.
		 *
		 * <p>
		 * Enables the following additional information during parsing:
		 * <ul>
		 * 	<li> When bean setters throws exceptions, the exception includes the object stack information in order to determine how that method was invoked.
		 * </ul>
		 *
		 * <p>
		 * If not specified, defaults to {@link Context.Builder#debug()}.
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#debug()}
		 * 	<li class='jm'>{@link org.apache.juneau.Context.Builder#debug()}
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Can be <jk>null</jk>.  Value will be ignored.
		 * @return This object.
		 */
		@FluentSetter
		public Builder debug(Boolean value) {
			if (value != null)
				debug = value;
			return this;
		}

		/**
		 * Create an unmodifiable session.
		 *
		 * <p>
		 * The created ContextSession object will be unmodifiable which makes it suitable for caching and reuse.
		 *
		 * @return This object.
		 */
		@FluentSetter
		public Builder unmodifiable() {
			unmodifiable = true;
			return this;
		}

		/**
		 * Session properties.
		 *
		 * <p>
		 * Session properties are generic key-value pairs that can be passed through the session and made
		 * available to any customized serializers/parsers or swaps.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Can be <jk>null</jk>.
		 * @return This object.
		 */
		@FluentSetter
		public Builder properties(Map<String,Object> value) {
			properties = JsonMap.of(value);
			return this;
		}

		/**
		 * Adds a property to this session.
		 *
		 * @param key The property key.
		 * @param value The property value.
		 * @return This object.
		 */
		@FluentSetter
		public Builder property(String key, Object value) {
			if (properties == null)
				properties = JsonMap.create();
			if (value == null) {
				properties.remove(key);
			} else {
				properties.put(key, value);
			}
			return this;
		}

		/**
		 * Applies a consumer to this builder if it's the specified type.
		 *
		 * @param <T> The expected type.
		 * @param type The expected type.
		 * @param apply	The consumer to apply.
		 * @return This object.
		 */
		@FluentSetter
		public <T> Builder apply(Class<T> type, Consumer<T> apply) {
			if (type.isInstance(this))
				apply.accept(type.cast(this));
			return this;
		}

		// <FluentSetters>

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final JsonMap properties;
	private List<String> warnings;	// Any warnings encountered.

	private final Context ctx;
	private final boolean debug;
	private final boolean unmodifiable;

	/**
	 * Default constructor.
	 *
	 * @param builder The builder for this object
	 */
	protected ContextSession(Builder builder) {
		ctx = builder.ctx;
		unmodifiable = builder.unmodifiable;
		JsonMap sp = builder.properties == null ? JsonMap.EMPTY_MAP : builder.properties;
		if (unmodifiable)
			sp = sp.unmodifiable();
		properties = sp;
		debug = builder.debug;
	}

	/**
	 * Returns the session properties on this session.
	 *
	 * @return The session properties on this session.  Never <jk>null</jk>.
	 */
	public final JsonMap getSessionProperties() {
		return properties;
	}

	/**
	 * Returns the context that created this session.
	 *
	 * @return The context that created this session.
	 */
	public Context getContext() {
		return ctx;
	}

	/**
	 * Logs a warning message.
	 *
	 * @param msg The warning message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public void addWarning(String msg, Object... args) {
		if (unmodifiable)
			return;
		if (warnings == null)
			warnings = new LinkedList<>();
		warnings.add((warnings.size() + 1) + ": " + format(msg, args));
	}

	/**
	 * Returns the warnings that occurred in this session.
	 *
	 * @return The warnings that occurred in this session, or <jk>null</jk> if no warnings occurred.
	 */
	public final List<String> getWarnings() {
		return warnings == null ? emptyList() : warnings;
	}

	/**
	 * Throws a {@link BeanRuntimeException} if any warnings occurred in this session and debug is enabled.
	 */
	public void checkForWarnings() {
		if (debug && ! getWarnings().isEmpty())
			throw new BeanRuntimeException("Warnings occurred in session: \n" + join(getWarnings(), "\n"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Configuration properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Debug mode enabled.
	 *
	 * @see Context.Builder#debug()
	 * @return
	 * 	<jk>true</jk> if debug mode is enabled.
	 */
	public boolean isDebug() {
		return debug;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the properties on this bean as a map for debugging.
	 *
	 * @return The properties on this bean as a map for debugging.
	 */
	protected JsonMap properties() {
		return filteredMap("debug", debug);
	}

	@Override /* Object */
	public String toString() {
		return ObjectUtils.toPropertyMap(this).asReadableString();
	}
}