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

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * An extension of {@link ReentrantReadWriteLock} with convenience methods for creating
 * auto-closeable locks.
 */
public class SimpleNoOpLock extends ReentrantReadWriteLock {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 */
	public SimpleNoOpLock() {
		super();
	}

	/**
	 * Constructor
	 *
	 * @param fair <jk>true</jk> if this lock should use a fair ordering policy.
	 */
	public SimpleNoOpLock(boolean fair) {
		super(fair);
	}

	/**
	 * Construct a write lock.
	 *
	 * @return A new closeable write lock.
	 */
	public SimpleLock write(){
		return new SimpleLock(writeLock());
	}

	/**
	 * Construct a read lock.
	 *
	 * @return A new closeable read lock.
	 */
	public SimpleLock read(){
		return new SimpleLock(readLock());
	}
}
