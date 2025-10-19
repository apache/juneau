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
package org.apache.juneau.common;

/**
 * Enumeration of state machine states for use in parsing operations.
 *
 * <p>
 * This enum provides a standardized set of state constants (S1 through S20) that can be used
 * in state machine implementations throughout the Juneau codebase. This eliminates the need
 * to declare local int constants and provides better code readability.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jk>import static</jk> org.apache.juneau.common.StateEnum.*;
 *
 * 	<jc>// Use in state machine</jc>
 * 	<jk>var</jk> state = S1;
 * 	<jk>if</jk> (state == S1) {
 * 		<jc>// Handle state S1</jc>
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommon">juneau-common</a>
 * </ul>
 */
public enum StateEnum {
	/** State 1 */
	S1,
	/** State 2 */
	S2,
	/** State 3 */
	S3,
	/** State 4 */
	S4,
	/** State 5 */
	S5,
	/** State 6 */
	S6,
	/** State 7 */
	S7,
	/** State 8 */
	S8,
	/** State 9 */
	S9,
	/** State 10 */
	S10,
	/** State 11 */
	S11,
	/** State 12 */
	S12,
	/** State 13 */
	S13,
	/** State 14 */
	S14,
	/** State 15 */
	S15,
	/** State 16 */
	S16,
	/** State 17 */
	S17,
	/** State 18 */
	S18,
	/** State 19 */
	S19,
	/** State 20 */
	S20;
}
