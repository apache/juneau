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
package org.apache.juneau.marshall;

import static org.apache.juneau.commons.utils.SystemUtils.*;

import org.apache.juneau.commons.collections.*;

/**
 * Parent class for all classes that traverse POJOs.
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Base class that serves as the parent class for all serializers and other classes that traverse POJOs.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 */
@SuppressWarnings({
	"rawtypes",
	"java:S115" // Constants use UPPER_snakeCase convention (e.g., PROP_detectRecursions)
})
public abstract class MarshallingTraverseContext extends MarshallingContextable {

	// Property name constants
	private static final String PROP_detectRecursions = "detectRecursions";
	private static final String PROP_ignoreRecursions = "ignoreRecursions";
	private static final String PROP_initialDepth = "initialDepth";
	private static final String PROP_maxDepth = "maxDepth";

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	})
	public abstract static class Builder<SELF extends Builder<SELF>> extends MarshallingContextable.Builder<SELF> {

		private boolean detectRecursions;
		private boolean ignoreRecursions;
		private int initialDepth;
		private int maxDepth;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			detectRecursions = env("MarshallingTraverseContext.detectRecursions", false);
			ignoreRecursions = env("MarshallingTraverseContext.ignoreRecursions", false);
			initialDepth = env("MarshallingTraverseContext.initialDepth", 0);
			maxDepth = env("MarshallingTraverseContext.maxDepth", 100);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.  Cannot be <jk>null</jk>.
		 */
		protected Builder(MarshallingTraverseContext copyFrom) {
			super(copyFrom);
			detectRecursions = copyFrom.getDetectRecursions();
			ignoreRecursions = copyFrom.getIgnoreRecursions();
			initialDepth = copyFrom.getInitialDepth();
			maxDepth = copyFrom.getMaxDepth();
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.  Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder<?> copyFrom) {
			super(copyFrom);
			detectRecursions = copyFrom.detectRecursions;
			ignoreRecursions = copyFrom.ignoreRecursions;
			initialDepth = copyFrom.initialDepth;
			maxDepth = copyFrom.maxDepth;
		}

		/**
		 * Automatically detect POJO recursions.
		 *
		 * <p>
		 * When enabled, specifies that recursions should be checked for during traversal.
		 *
		 * <p>
		 * Recursions can occur when traversing models that aren't true trees but rather contain loops.
		 * <br>When recursion detection is disabled, such loops are instead bounded by {@link #maxDepth(int) maxDepth}
		 * 	(default <c>100</c>): the over-depth branch is silently truncated, producing finite but semantically-incomplete output.
		 * <br>If the stack is exhausted before <c>maxDepth</c> is reached, the resulting {@link StackOverflowError} is
		 * 	converted to a {@link MarshallingRecursionException MarshallingRecursionException} with the message <js>"Depth too deep.  Stack overflow occurred."</js>.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>
		 * 		Checking for recursion can cause a small performance penalty.
		 * 	<li class='note'>
		 * 		This is the recommended way to fail-fast on cyclic bean graphs such as parent/child
		 * 		{@link ParentProperty @ParentProperty} references.  Under the default config such cycles are instead
		 * 		silently truncated at {@link #maxDepth(int) maxDepth} (see the {@link ParentProperty} Javadoc).
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer that automatically checks for recursions.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.detectRecursions()
		 * 		.build();
		 *
		 * 	<jc>// Create a POJO model with a recursive loop.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> Object <jf>f</jf>;
		 * 	}
		 * 	MyBean <jv>bean</jv> = <jk>new</jk> MyBean();
		 * 	<jv>bean</jv>.<jf>f</jf> = <jv>bean</jv>;
		 *
		 * 	<jc>// Throws a SerializeException and not a StackOverflowError</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.write(<jv>bean</jv>);
		 * </p>
		 *
		 * @return This object.
		 */
		public SELF detectRecursions() {
			return detectRecursions(true);
		}

		/**
		 * Same as {@link #detectRecursions()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public SELF detectRecursions(boolean value) {
			detectRecursions = value;
			return self();
		}

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			// @formatter:off
			return HashKey.of(
				super.hashKey(),
				detectRecursions,
				ignoreRecursions,
				initialDepth,
				maxDepth
			);
			// @formatter:on
		}

		/**
		 * Ignore recursion errors.
		 *
		 * <p>
		 * When enabled, when we encounter the same object when traversing a tree, we set the value to <jk>null</jk>.
		 *
		 * <p>
		 * For example, if a model contains the links A-&gt;B-&gt;C-&gt;A, then the JSON generated will look like
		 * 	the following when this setting is <jk>true</jk>...
		 *
		 * <p class='bjson'>
		 * 	{A:{B:{C:<jk>null</jk>}}}
		 * </p>
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>
		 * 		Checking for recursion can cause a small performance penalty.
		 * 	<li class='note'>
		 * 		Use this to serialize cyclic bean graphs (such as parent/child {@link ParentProperty @ParentProperty}
		 * 		references) by emitting the repeated node as <jk>null</jk> so the output round-trips cleanly.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer ignores recursions.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.ignoreRecursions()
		 * 		.build();
		 *
		 * 	<jc>// Create a POJO model with a recursive loop.</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> Object <jf>f</jf>;
		 * 	}
		 * 	MyBean <jv>bean</jv> = <jk>new</jk> MyBean();
		 * 	<jv>bean</jv>.<jf>f</jf> = <jv>bean</jv>;
		 *
		 * 	<jc>// Produces "{f:null}"</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.write(<jv>bean</jv>);
		 * </p>
		 *
		 * @return This object.
		 */
		public SELF ignoreRecursions() {
			return ignoreRecursions(true);
		}

		/**
		 * Same as {@link #ignoreRecursions()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public SELF ignoreRecursions(boolean value) {
			ignoreRecursions = value;
			return self();
		}

		/**
		 * Initial depth.
		 *
		 * <p>
		 * The initial indentation level at the root.
		 *
		 * <p>
		 * Useful when constructing document fragments that need to be indented at a certain level when whitespace is enabled.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer with whitespace enabled and an initial depth of 2.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.ws()
		 * 		.initialDepth(2)
		 * 		.build();
		 *
		 * 	<jc>// Produces "\t\t{\n\t\t\t'foo':'bar'\n\t\t}\n"</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.write(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is <c>0</c>.
		 * @return This object.
		 */
		public SELF initialDepth(int value) {
			initialDepth = value;
			return self();
		}

		/**
		 * Max traversal depth.
		 *
		 * <p>
		 * Specifies the maximum depth traversed in the POJO tree.
		 *
		 * <p>
		 * When this depth is exceeded, the over-depth nodes are silently dropped (truncated) from the output — no
		 * exception is thrown.  This is a size guard that bounds the output of deeply-nested (or cyclic) models; it is
		 * <b>not</b> a cycle detector.  For genuine recursive references, enable {@link #detectRecursions()} to fail-fast
		 * with a {@link MarshallingRecursionException}, or {@link #ignoreRecursions()} to emit repeated nodes as
		 * <jk>null</jk> so the output round-trips cleanly.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer that truncates output beyond a depth of 20.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.maxDepth(20)
		 * 		.build();
		 * </p>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='jm'>{@link Builder#maxDepth(int)}
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this setting.
		 * 	<br>The default is <c>100</c>.
		 * @return This object.
		 */
		public SELF maxDepth(int value) {
			maxDepth = value;
			return self();
		}


	}

	private final boolean detectRecursions;
	private final boolean ignoreRecursions;
	private final int initialDepth;
	private final int maxDepth;
	private final boolean actualDetectRecursions;

	/**
	 * Constructor
	 *
	 * @param builder The builder for this object.  Cannot be <jk>null</jk>.
	 */
	protected MarshallingTraverseContext(Builder builder) {
		super(builder);

		detectRecursions = builder.detectRecursions;
		ignoreRecursions = builder.ignoreRecursions;
		initialDepth = builder.initialDepth;
		maxDepth = builder.maxDepth;

		actualDetectRecursions = detectRecursions || ignoreRecursions || super.isDebug();
	}

	@Override /* Overridden from Context */
	public abstract Builder<?> copy();

	/**
	 * Initial depth.
	 *
	 * @see Builder#initialDepth(int)
	 * @return
	 * 	The initial indentation level at the root.
	 */
	public final int getInitialDepth() { return initialDepth; }

	/**
	 * Max traversal depth.
	 *
	 * @see Builder#maxDepth(int)
	 * @return
	 * 	The depth beyond which nodes in the POJO tree are dropped (truncated) from the output.
	 *	<br>Values deeper than this limit are silently omitted rather than causing an exception to be thrown.
	 *	<br>This is a size guard, not a cycle detector.
	 */
	public final int getMaxDepth() { return maxDepth; }

	/**
	 * Automatically detect POJO recursions.
	 *
	 * @see Builder#detectRecursions()
	 * @return
	 * 	<jk>true</jk> if recursions should be checked for during traversal.
	 */
	public final boolean isDetectRecursions() { return detectRecursions; }

	/**
	 * Whether recursion detection is effectively enabled during traversal.
	 * This is {@link #isDetectRecursions()} OR {@link #isIgnoreRecursions()} OR {@link #isDebug()}.
	 */
	protected final boolean shouldDetectRecursions() { return actualDetectRecursions; }

	/**
	 * Ignore recursion errors.
	 *
	 * @see Builder#ignoreRecursions()
	 * @return
	 * 	<jk>true</jk> if when we encounter the same object when traversing a tree, we set the value to <jk>null</jk>.
	 * 	<br>Otherwise, an exception is thrown with the message <js>"Recursion occurred, stack=..."</js>.
	 */
	public final boolean isIgnoreRecursions() { return ignoreRecursions; }

	/**
	 * Detect recursions flag (raw value).
	 *
	 * @return The detect recursions flag.
	 */
	protected final boolean getDetectRecursions() { return detectRecursions; }

	/**
	 * Ignore recursions flag (raw value).
	 *
	 * @return The ignore recursions flag.
	 */
	protected final boolean getIgnoreRecursions() { return ignoreRecursions; }

	@Override /* Overridden from MarshallingContextable */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_detectRecursions, detectRecursions)
			.a(PROP_ignoreRecursions, ignoreRecursions)
			.a(PROP_initialDepth, initialDepth)
			.a(PROP_maxDepth, maxDepth);
	}
}