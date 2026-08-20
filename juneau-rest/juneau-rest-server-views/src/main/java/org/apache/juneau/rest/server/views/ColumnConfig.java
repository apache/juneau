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
package org.apache.juneau.rest.server.views;

import org.apache.juneau.commons.bean.*;

/**
 * The opt-in per-view column-configurator settings in the {@code VIEW_META} wire contract (design doc §6.8).
 *
 * <p>
 * Set {@link ViewDef#columnConfig(ColumnConfig)} to enable the View-only column chooser (visible set + order +
 * per-column label override + per-column format) for that view; the mere <b>presence</b> of this bean is the
 * opt-in signal &mdash; an empty {@link #create()} is sufficient, there is no separate {@code enabled} flag.
 *
 * <p>
 * This type currently declares no wire fields of its own; it reserves room for the deferred Search/Sort/Options
 * tab flags without a further {@link ViewDef} signature change.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link ViewDef}
 * 	<li class='jc'>{@link Column}
 * </ul>
 *
 * @since 10.0.0
 */
@BeanType
public class ColumnConfig {

	/**
	 * Creates a new, empty column-configurator opt-in.
	 *
	 * @return A new {@link ColumnConfig}.
	 */
	public static ColumnConfig create() {
		return new ColumnConfig();
	}
}
