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
package org.apache.juneau;

/**
 * Superclass of all classes that have a locked state.
 * <p>
 * Used to mark bean contexts, serializers, and parsers as read-only so that
 * 	settings can no longer be modified.
 * <p>
 * Also keeps track of when the object has been cloned and allows for lazy cloning through
 * 	the {@link #onUnclone()} method.  The idea behind this is that certain expensive fields don't
 * 	need to be cloned unless the object is actually being modified.
 * <p>
 * Calling {@link #lock()} on the object causes it to be put into a read-only state.
 * Once called, subsequent calls to {@link #checkLock()} will cause {@link LockedException LockedExceptions}
 * 	to be thrown.
 * <p>
 * As a rule, cloned objects are unlocked by default.
 */
public abstract class Lockable implements Cloneable {

	private boolean isLocked = false;
	private boolean isCloned = false;

	/**
	 * Locks this object so that settings on it cannot be modified.
	 *
	 * @return This object (for method chaining).
	 */
	public Lockable lock() {
		isLocked = true;
		return this;
	}

	/**
	 * @return <code><jk>true</jk></code> if this object has been locked.
	 */
	public boolean isLocked() {
		return isLocked;
	}

	/**
	 * Causes a {@link LockedException} to be thrown if this object has been locked.
	 * <p>
	 * 	Also calls {@link #onUnclone()} if this is the first time this method has been called since cloning.
	 *
	 * @throws LockedException If {@link #lock()} has been called on this object.
	 */
	public void checkLock() throws LockedException {
		if (isLocked)
			throw new LockedException();
		if (isCloned)
			onUnclone();
		isCloned = false;
	}

	/**
	 * Subclass can override this method to handle lazy-cloning on the first time {@link #checkLock()} is called after
	 * the object has been cloned.
	 */
	public void onUnclone() {}

	/**
	 * Creates an unlocked clone of this object.
	 *
	 * @throws CloneNotSupportedException If class cannot be cloned.
	 */
	@Override /* Object */
	public Lockable clone() throws CloneNotSupportedException {
		Lockable c = (Lockable)super.clone();
		c.isLocked = false;
		c.isCloned = true;
		return c;
	}
}
