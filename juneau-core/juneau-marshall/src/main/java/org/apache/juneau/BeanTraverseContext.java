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


import org.apache.juneau.collections.*;

/**
 * Parent class for all classes that traverse POJOs.
 * {@review}
 *
 * <h5 class='topic'>Description</h5>
 *
 * Base class that serves as the parent class for all serializers and other classes that traverse POJOs.
 */
public abstract class BeanTraverseContext extends BeanContextable {

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final int initialDepth, maxDepth;
	final boolean
		detectRecursions,
		ignoreRecursions;

	private final boolean actualDetectRecursions;

	/**
	 * Constructor
	 *
	 * @param builder The builder for this object.
	 */
	protected BeanTraverseContext(BeanTraverseBuilder builder) {
		super(builder);

		maxDepth = builder.maxDepth;
		initialDepth = builder.initialDepth;
		ignoreRecursions = builder.ignoreRecursions;
		detectRecursions = builder.detectRecursions;

		actualDetectRecursions = detectRecursions || ignoreRecursions || super.isDebug();
	}

	@Override
	public abstract BeanTraverseBuilder copy();

	@Override /* Context */
	public BeanTraverseSession createSession() {
		return new BeanTraverseSession(this, createDefaultSessionArgs());
	}

	@Override /* Context */
	public BeanTraverseSession createSession(BeanSessionArgs args) {
		return new BeanTraverseSession(this, args);
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Automatically detect POJO recursions.
	 *
	 * @see BeanTraverseBuilder#detectRecursions()
	 * @return
	 * 	<jk>true</jk> if recursions should be checked for during traversal.
	 */
	public final boolean isDetectRecursions() {
		return actualDetectRecursions;
	}

	/**
	 * Ignore recursion errors.
	 *
	 * @see BeanTraverseBuilder#ignoreRecursions()
	 * @return
	 * 	<jk>true</jk> if when we encounter the same object when traversing a tree, we set the value to <jk>null</jk>.
	 * 	<br>Otherwise, an exception is thrown with the message <js>"Recursion occurred, stack=..."</js>.
	 */
	public final boolean isIgnoreRecursions() {
		return ignoreRecursions;
	}

	/**
	 * Initial depth.
	 *
	 * @see BeanTraverseBuilder#initialDepth(int)
	 * @return
	 * 	The initial indentation level at the root.
	 */
	public final int getInitialDepth() {
		return initialDepth;
	}

	/**
	 * Max traversal depth.
	 *
	 * @see BeanTraverseBuilder#maxDepth(int)
	 * @return
	 * 	The depth at which traversal is aborted if depth is reached in the POJO tree.
	 *	<br>If this depth is exceeded, an exception is thrown.
	 */
	public final int getMaxDepth() {
		return maxDepth;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"BeanTraverseContext",
				OMap
					.create()
					.filtered()
					.a("detectRecursions", detectRecursions)
					.a("maxDepth", maxDepth)
					.a("ignoreRecursions", ignoreRecursions)
					.a("initialDepth", initialDepth)
			);
	}
}
