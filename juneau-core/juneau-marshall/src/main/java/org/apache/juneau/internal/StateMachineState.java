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
package org.apache.juneau.internal;

/**
 * Enums for state-machine states.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
@SuppressWarnings("javadoc")
public enum StateMachineState {
	S01, S02, S03, S04, S05, S06, S07, S08, S09, S10, S11, S12, S13, S14, S15, S16, S17, S18, S19, S20;

	/**
	 * Returns <jk>true</jk> if the state is any one of the specified states.
	 *
	 * @param states The states to check.
	 * @return <jk>true</jk> if the state is any one of the specified states.
	 */
	public boolean isAny(StateMachineState...states) {
		for (StateMachineState s : states)
			if (this == s)
				return true;
		return false;
	}
}
