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

/**
 * How a {@link DetailSection}'s fields arrange their label and value.
 *
 * <p>
 * A closed vocabulary the framework translates to a class on the fields grid.  It is deliberately not a CSS
 * value passthrough, and it is deliberately per-section rather than per-field: there is one grid per section, so
 * the arrangement is a property of the grid and {@code emitDetailField} keeps emitting the same title-div plus
 * value-div for every field whichever value is chosen.
 *
 * @since 10.0.0
 */
public enum FieldLayout {

	/**
	 * Label and value side by side, the label on a bounded fraction of the field block and top-aligned so a
	 * wrapped two-line label does not drag the value's baseline down.  The default.
	 */
	INLINE,

	/** Label above value.  An explicit author opt-in, useful for a one-column section of long values. */
	STACKED
}
