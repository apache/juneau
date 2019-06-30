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
import org.apache.juneau.parser.*;

/**
 * Parent class for all classes that traverse POJOs.
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
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanTraverseContext.detectRecursions.b"</js>
	 * 	<li><b>Data type:</b>  <c>Boolean</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanTraverseBuilder#detectRecursions(boolean)}
	 * 			<li class='jm'>{@link BeanTraverseBuilder#detectRecursions()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies that recursions should be checked for during traversal.
	 *
	 * <p>
	 * Recursions can occur when traversing models that aren't true trees but rather contain loops.
	 * <br>In general, unchecked recursions cause stack-overflow-errors.
	 * <br>These show up as {@link ParseException ParseExceptions} with the message <js>"Depth too deep.  Stack overflow occurred."</js>.
	 *
	 * <p>
	 * The behavior when recursions are detected depends on the value for {@link #BEANTRAVERSE_ignoreRecursions}.
	 *
	 * <p>
	 * For example, if a model contains the links A-&gt;B-&gt;C-&gt;A, then the JSON generated will look like
	 * 	the following when <jsf>BEANTRAVERSE_ignoreRecursions</jsf> is <jk>true</jk>...
	 *
	 * <p class='bcode w800'>
	 * 	{A:{B:{C:<jk>null</jk>}}}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Checking for recursion can cause a small performance penalty.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that never adds _type to nodes.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.detectRecursions()
	 * 		.ignoreRecursions()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEANTRAVERSE_detectRecursions</jsf>, <jk>true</jk>)
	 * 		.set(<jsf>BEANTRAVERSE_ignoreRecursions</jsf>, <jk>true</jk>)
	 * 		.build();
	 *
	 * 	<jc>// Create a POJO model with a recursive loop.</jc>
	 * 	<jk>public class</jk> A {
	 * 		<jk>public</jk> Object <jf>f</jf>;
	 * 	}
	 * 	A a = <jk>new</jk> A();
	 * 	a.<jf>f</jf> = a;
	 *
	 * 	<jc>// Produces "{f:null}"</jc>
	 * 	String json = s.serialize(a);
	 * </p>
	 */
	public static final String BEANTRAVERSE_detectRecursions = PREFIX + ".detectRecursions.b";

	/**
	 * Configuration property:  Ignore recursion errors.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanTraverseContext.ignoreRecursions.b"</js>
	 * 	<li><b>Data type:</b>  <c>Boolean</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanTraverseBuilder#ignoreRecursions(boolean)}
	 * 			<li class='jm'>{@link BeanTraverseBuilder#ignoreRecursions()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Used in conjunction with {@link #BEANTRAVERSE_detectRecursions}.
	 * <br>Setting is ignored if <jsf>BEANTRAVERSE_detectRecursions</jsf> is <jk>false</jk>.
	 *
	 * <p>
	 * If <jk>true</jk>, when we encounter the same object when traversing a tree, we set the value to <jk>null</jk>.
	 * <br>Otherwise, a {@link BeanRecursionException} is thrown with the message <js>"Recursion occurred, stack=..."</js>.
	 */
	public static final String BEANTRAVERSE_ignoreRecursions = PREFIX + ".ignoreRecursions.b";

	/**
	 * Configuration property:  Initial depth.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanTraverseContext.initialDepth.i"</js>
	 * 	<li><b>Data type:</b>  <c>Integer</c>
	 * 	<li><b>Default:</b>  <c>0</c>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanTraverseBuilder#initialDepth(int)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The initial indentation level at the root.
	 * <br>Useful when constructing document fragments that need to be indented at a certain level.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer with whitespace enabled and an initial depth of 2.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.ws()
	 * 		.initialDepth(2)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEANTRAVERSE_useWhitespace</jsf>, <jk>true</jk>)
	 * 		.set(<jsf>BEANTRAVERSE_initialDepth</jsf>, 2)
	 * 		.build();
	 *
	 * 	<jc>// Produces "\t\t{\n\t\t\t'foo':'bar'\n\t\t}\n"</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 */
	public static final String BEANTRAVERSE_initialDepth = PREFIX + ".initialDepth.i";

	/**
	 * Configuration property:  Max traversal depth.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"BeanTraverseContext.maxDepth.i"</js>
	 * 	<li><b>Data type:</b>  <c>Integer</c>
	 * 	<li><b>Default:</b>  <c>100</c>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link BeanTraverseBuilder#maxDepth(int)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Abort traversal if specified depth is reached in the POJO tree.
	 * <br>If this depth is exceeded, an exception is thrown.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a serializer that throws an exception if the depth is greater than 20.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.maxDepth(20)
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>BEANTRAVERSE_maxDepth</jsf>, 20)
	 * 		.build();
	 * </p>
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
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 */
	protected BeanTraverseContext(PropertyStore ps) {
		super(ps);

		maxDepth = getIntegerProperty(BEANTRAVERSE_maxDepth, 100);
		initialDepth = getIntegerProperty(BEANTRAVERSE_initialDepth, 0);
		detectRecursions = getBooleanProperty(BEANTRAVERSE_detectRecursions, false);
		ignoreRecursions = getBooleanProperty(BEANTRAVERSE_ignoreRecursions, false);
	}

	@Override /* Context */
	public BeanTraverseBuilder builder() {
		return null;
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
	 * Configuration property:  Automatically detect POJO recursions.
	 *
	 * @see #BEANTRAVERSE_detectRecursions
	 * @return
	 * 	<jk>true</jk> if recursions should be checked for during traversal.
	 */
	protected final boolean isDetectRecursions() {
		return detectRecursions;
	}

	/**
	 * Configuration property:  Ignore recursion errors.
	 *
	 * @see #BEANTRAVERSE_ignoreRecursions
	 * @return
	 * 	<jk>true</jk> if when we encounter the same object when traversing a tree, we set the value to <jk>null</jk>.
	 * 	<br>Otherwise, an exception is thrown with the message <js>"Recursion occurred, stack=..."</js>.
	 */
	protected final boolean isIgnoreRecursions() {
		return ignoreRecursions;
	}

	/**
	 * Configuration property:  Initial depth.
	 *
	 * @see #BEANTRAVERSE_initialDepth
	 * @return
	 * 	The initial indentation level at the root.
	 */
	protected final int getInitialDepth() {
		return initialDepth;
	}

	/**
	 * Configuration property:  Max traversal depth.
	 *
	 * @see #BEANTRAVERSE_maxDepth
	 * @return
	 * 	The depth at which traversal is aborted if depth is reached in the POJO tree.
	 *	<br>If this depth is exceeded, an exception is thrown.
	 */
	protected final int getMaxDepth() {
		return maxDepth;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public ObjectMap toMap() {
		return super.toMap()
			.append("BeanTraverseContext", new DefaultFilteringObjectMap()
				.append("detectRecursions", detectRecursions)
				.append("maxDepth", maxDepth)
				.append("ignoreRecursions", ignoreRecursions)
				.append("initialDepth", initialDepth)
			);
	}
}
