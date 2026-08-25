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
package org.apache.juneau.rest.server.widgets;

/**
 * One item in a {@link QuickStats} strip: a {@link StatTile} (a labelled scalar), a {@link StatBar} (a value against a
 * maximum), or a {@link SegmentedBadge} (a labelled breakdown of counts).
 *
 * <p>
 * Sealed on the same terms as {@link BarWidget}: the permitted implementers are frozen so the views emitter's
 * {@code instanceof} dispatch and {@link QuickStats#validate()} are total.  No fourth implementer compiles.
 *
 * @since 10.0.0
 */
public sealed interface StatItem permits StatTile, StatBar, SegmentedBadge {

	/**
	 * Returns the stable item id, unique within its {@link QuickStats}.
	 *
	 * @return The item id.  Can be <jk>null</jk> or blank on a malformed item (rejected by
	 * 	{@link QuickStats#validate()}).
	 */
	String id();
}
