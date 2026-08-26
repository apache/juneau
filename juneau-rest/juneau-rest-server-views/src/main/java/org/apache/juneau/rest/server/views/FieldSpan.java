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
 * How many of a {@link DetailSection}'s grid columns one {@link DetailField} occupies.
 *
 * <p>
 * A span is a <b>maximum</b>, exactly as {@link DetailSection#columns} is: it clamps downward as the grid steps
 * down, and it never creates a column beyond the rendered count.  At one column {@link #FULL} and {@link #ONE}
 * render identically.
 *
 * <p>
 * A closed vocabulary the framework translates to a class, never a CSS value an author supplies.
 *
 * @since 10.0.0
 */
public enum FieldSpan {

	/** One column.  The default. */
	ONE,

	/** Every column the grid is currently rendering. */
	FULL
}
