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

import java.util.concurrent.locks.Lock;

/**
 * A simple auto-closeable wrapper around a lock.
 */
public class SimpleLock implements AutoCloseable {

	/**
	 * A simple no-op lock.
	 */
	public static final SimpleLock NO_OP = new SimpleLock(null);

	private final Lock lock;

	/**
	 * Constructor.
	 *
	 * @param lock The lock being wrapped.
	 */
	public SimpleLock(Lock lock) {
		this.lock = lock;
		if (lock != null)
			lock.lock();
	}

	@Override
	public void close() {
		if (lock != null)
			lock.unlock();
	}
}
