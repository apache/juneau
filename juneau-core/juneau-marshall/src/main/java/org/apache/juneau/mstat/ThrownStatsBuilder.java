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
package org.apache.juneau.mstat;

import java.util.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * Builder for {@link ThrownStats} objects.
 */
@FluentSetters
public class ThrownStatsBuilder {

	Throwable throwable;
	long hash;
	List<String> stackTrace;
	ThrownStats causedBy;

	BeanCreator<ThrownStats> creator = BeanCreator.create(ThrownStats.class).builder(this);

	/**
	 * Create a new {@link ThrownStats} using this builder.
	 *
	 * @return A new {@link ThrownStats}
	 */
	public ThrownStats build() {
		return creator.run();
	}

	/**
	 * Specifies the bean store to use for instantiating the {@link ThrownStats} object.
	 *
	 * <p>
	 * Can be used to instantiate {@link ThrownStats} implementations with injected constructor argument beans.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public ThrownStatsBuilder beanStore(BeanStore value) {
		creator.store(value);
		return this;
	}

	/**
	 * Specifies a subclass of {@link ThrownStats} to create when the {@link #build()} method is called.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public ThrownStatsBuilder type(Class<? extends ThrownStats> value) {
		creator.type(value == null ? ThrownStats.class : value);
		return this;
	}

	/**
	 * Specifies the thrown exception.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public ThrownStatsBuilder throwable(Throwable value) {
		this.throwable = value;
		return this;
	}

	/**
	 * Specifies the calculated hash.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public ThrownStatsBuilder hash(long value) {
		this.hash = value;
		return this;
	}

	/**
	 * Specifies the normalized stacktrace.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public ThrownStatsBuilder stackTrace(List<String> value) {
		this.stackTrace = value;
		return this;
	}

	/**
	 * Specifies the caused-by exception.
	 *
	 * @param value The new value for this setting.
	 * @return  This object (for method chaining).
	 */
	@FluentSetter
	public ThrownStatsBuilder causedBy(ThrownStats value) {
		this.causedBy = value;
		return this;
	}

	// <FluentSetters>

	// </FluentSetters>
}
