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
package org.apache.juneau.encoders;

import java.io.*;

/**
 * Interface that identifies an output stream has having a <c>finish()</c> method.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.Encoders">Encoders</a>
 * </ul>
 */
public interface Finishable {

	/**
	 * Finishes writing compressed data to the output stream without closing the underlying stream.
	 *
	 * <p>
	 * Use this method when applying multiple filters in succession to the same output stream.
	 *
	 * @throws IOException Thrown by underlying stream.
	 */
	void finish() throws IOException;
}
