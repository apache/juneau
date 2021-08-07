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


import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;

/**
 * Parent class for all classes that traverse POJOs.
 * {@review}
 *
 * <h5 class='topic'>Description</h5>
 *
 * Base class that serves as the parent class for all serializers and other classes that traverse POJOs.
 */
@ConfigurableContext
public abstract class BeanTraverseContext extends BeanContext {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "BeanTraverseContext";

	/**
	 * Configuration property:  Automatically detect POJO recursions.
	 *
	 * <p>
	 * When enabled, specifies that recursions should be checked for during traversal.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanTraverseContext#BEANTRAVERSE_detectRecursions BEANTRAVERSE_detectRecursions}
	 * 	<li><b>Name:</b>  <js>"BeanTraverseContext.detectRecursions.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>BeanTraverseContext.detectRecursions</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANTRAVERSECONTEXT_DETECTRECURSIONS</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#detectRecursions()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanTraverseBuilder#detectRecursions()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEANTRAVERSE_detectRecursions = PREFIX + ".detectRecursions.b";

	/**
	 * Configuration property:  Ignore recursion errors.
	 *
	 * <p>
	 * When enabled, when we encounter the same object when traversing a tree, we set the value to <jk>null</jk>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanTraverseContext#BEANTRAVERSE_ignoreRecursions BEANTRAVERSE_ignoreRecursions}
	 * 	<li><b>Name:</b>  <js>"BeanTraverseContext.ignoreRecursions.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>BeanTraverseContext.ignoreRecursions</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANTRAVERSECONTEXT_IGNORERECURSIONS</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#ignoreRecursions()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanTraverseBuilder#ignoreRecursions()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEANTRAVERSE_ignoreRecursions = PREFIX + ".ignoreRecursions.b";

	/**
	 * Configuration property:  Initial depth.
	 *
	 * <p>
	 * The initial indentation level at the root.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanTraverseContext#BEANTRAVERSE_initialDepth BEANTRAVERSE_initialDepth}
	 * 	<li><b>Name:</b>  <js>"BeanTraverseContext.initialDepth.i"</js>
	 * 	<li><b>Data type:</b>  <jk>int</jk>
	 * 	<li><b>System property:</b>  <c>BeanTraverseContext.initialDepth</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANTRAVERSECONTEXT_INITIALDEPTH</c>
	 * 	<li><b>Default:</b>  <c>0</c>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#initialDepth()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanTraverseBuilder#initialDepth(int)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEANTRAVERSE_initialDepth = PREFIX + ".initialDepth.i";

	/**
	 * Configuration property:  Max traversal depth.
	 *
	 * <p>
	 * When enabled, abort traversal if specified depth is reached in the POJO tree.
	 * If this depth is exceeded, an exception is thrown.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.BeanTraverseContext#BEANTRAVERSE_maxDepth BEANTRAVERSE_maxDepth}
	 * 	<li><b>Name:</b>  <js>"BeanTraverseContext.maxDepth.i"</js>
	 * 	<li><b>Data type:</b>  <jk>int</jk>
	 * 	<li><b>System property:</b>  <c>BeanTraverseContext.maxDepth</c>
	 * 	<li><b>Environment variable:</b>  <c>BEANTRAVERSECONTEXT_MAXDEPTH</c>
	 * 	<li><b>Default:</b>  <c>100</c>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.annotation.BeanConfig#maxDepth()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.BeanTraverseBuilder#maxDepth(int)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String BEANTRAVERSE_maxDepth = PREFIX + ".maxDepth.i";

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final int initialDepth, maxDepth;
	private final boolean
		detectRecursions,
		ignoreRecursions;

	/**
	 * Constructor
	 *
	 * @param cp
	 * 	The property store containing all the settings for this object.
	 */
	protected BeanTraverseContext(ContextProperties cp) {
		super(cp);

		maxDepth = cp.getInteger(BEANTRAVERSE_maxDepth).orElse(100);
		initialDepth = cp.getInteger(BEANTRAVERSE_initialDepth).orElse(0);
		ignoreRecursions = cp.getBoolean(BEANTRAVERSE_ignoreRecursions).orElse(false);
		detectRecursions = cp.getBoolean(BEANTRAVERSE_detectRecursions).orElse(ignoreRecursions);
	}

	@Override /* Context */
	public BeanTraverseBuilder copy() {
		return new BeanTraverseBuilder(this);
	}

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
	 * @see #BEANTRAVERSE_detectRecursions
	 * @return
	 * 	<jk>true</jk> if recursions should be checked for during traversal.
	 */
	public final boolean isDetectRecursions() {
		return detectRecursions;
	}

	/**
	 * Ignore recursion errors.
	 *
	 * @see #BEANTRAVERSE_ignoreRecursions
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
	 * @see #BEANTRAVERSE_initialDepth
	 * @return
	 * 	The initial indentation level at the root.
	 */
	public final int getInitialDepth() {
		return initialDepth;
	}

	/**
	 * Max traversal depth.
	 *
	 * @see #BEANTRAVERSE_maxDepth
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
