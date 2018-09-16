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

import static org.apache.juneau.internal.StringUtils.*;

import java.text.*;
import java.util.*;

/**
 * Session that lives for the duration of a single use of {@link BeanTraverseContext}.
 *
 * <p>
 * Used by serializers and other classes that traverse POJOs for the following purposes:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Keeping track of how deep it is in a model for indentation purposes.
 * 	<li>
 * 		Ensuring infinite loops don't occur by setting a limit on how deep to traverse a model.
 * 	<li>
 * 		Ensuring infinite loops don't occur from loops in the model (when detectRecursions is enabled.
 * </ul>
 *
 * <p>
 * This class is NOT thread safe.
 * It is typically discarded after one-time use although it can be reused within the same thread.
 */
public class BeanTraverseSession extends BeanSession {

	private final BeanTraverseContext ctx;
	private final Map<Object,Object> set;                                           // Contains the current objects in the current branch of the model.
	private final LinkedList<StackElement> stack = new LinkedList<>();              // Contains the current objects in the current branch of the model.

	// Writable properties
	private boolean isBottom;                                                       // If 'true', then we're at a leaf in the model (i.e. a String, Number, Boolean, or null).
	private BeanPropertyMeta currentProperty;
	private ClassMeta<?> currentClass;

	/** The current indentation depth into the model. */
	public int indent;


	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * 	Can be <jk>null</jk>.
	 * @param args
	 * 	Runtime arguments.
	 * 	These specify session-level information such as locale and URI context.
	 * 	It also include session-level properties that override the properties defined on the bean and
	 * 	serializer contexts.
	 */
	protected BeanTraverseSession(BeanTraverseContext ctx, BeanSessionArgs args) {
		super(ctx, args == null ? BeanSessionArgs.DEFAULT : args);
		args = args == null ? BeanSessionArgs.DEFAULT : args;
		this.ctx = ctx;
		this.indent = getInitialDepth();
		if (isDetectRecursions() || isDebug()) {
			set = new IdentityHashMap<>();
		} else {
			set = Collections.emptyMap();
		}
	}

	/**
	 * Sets the current bean property being traversed for proper error messages.
	 *
	 * @param currentProperty The current property being traversed.
	 */
	protected final void setCurrentProperty(BeanPropertyMeta currentProperty) {
		this.currentProperty = currentProperty;
	}

	/**
	 * Sets the current class being traversed for proper error messages.
	 *
	 * @param currentClass The current class being traversed.
	 */
	protected final void setCurrentClass(ClassMeta<?> currentClass) {
		this.currentClass = currentClass;
	}

	/**
	 * Push the specified object onto the stack.
	 *
	 * @param attrName The attribute name.
	 * @param o The current object being traversed.
	 * @param eType The expected class type.
	 * @return
	 * 	The {@link ClassMeta} of the object so that <code>instanceof</code> operations only need to be performed
	 * 	once (since they can be expensive).
	 * @throws BeanRecursionException If recursion occurred.
	 */
	protected final ClassMeta<?> push(String attrName, Object o, ClassMeta<?> eType) throws BeanRecursionException {
		indent++;
		isBottom = true;
		if (o == null)
			return null;
		Class<?> c = o.getClass();
		ClassMeta<?> cm = (eType != null && c == eType.getInnerClass()) ? eType : ((o instanceof ClassMeta) ? (ClassMeta<?>)o : getClassMeta(c));
		if (cm.isCharSequence() || cm.isNumber() || cm.isBoolean())
			return cm;
		if (isDetectRecursions() || isDebug()) {
			if (stack.size() > getMaxDepth())
				return null;
			if (willRecurse(attrName, o, cm))
				return null;
			isBottom = false;
			stack.add(new StackElement(stack.size(), attrName, o, cm));
			if (isDebug())
				getLogger().info(getStack(false));
			set.put(o, o);
		}
		return cm;
	}

	/**
	 * Returns <jk>true</jk> if {@link BeanTraverseContext#BEANTRAVERSE_detectRecursions} is enabled, and the specified
	 * object is already higher up in the traversal chain.
	 *
	 * @param attrName The bean property attribute name, or some other identifier.
	 * @param o The object to check for recursion.
	 * @param cm The metadata on the object class.
	 * @return <jk>true</jk> if recursion detected.
	 * @throws BeanRecursionException If recursion occurred.
	 */
	protected final boolean willRecurse(String attrName, Object o, ClassMeta<?> cm) throws BeanRecursionException {
		if (! (isDetectRecursions() || isDebug()))
			return false;
		if (! set.containsKey(o))
			return false;
		if (isIgnoreRecursions() && ! isDebug())
			return true;

		stack.add(new StackElement(stack.size(), attrName, o, cm));
		throw new BeanRecursionException("Recursion occurred, stack={0}", getStack(true));
	}

	/**
	 * Pop an object off the stack.
	 */
	protected final void pop() {
		indent--;
		if ((isDetectRecursions() || isDebug()) && ! isBottom)  {
			Object o = stack.removeLast().o;
			Object o2 = set.remove(o);
			if (o2 == null)
				onError(null, "Couldn't remove object of type ''{0}'' on attribute ''{1}'' from object stack.",
					o.getClass().getName(), stack);
		}
		isBottom = false;
	}

	/**
	 * Logs a warning message.
	 *
	 * @param t The throwable that was thrown (if there was one).
	 * @param msg The warning message.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	protected void onError(Throwable t, String msg, Object... args) {
		super.addWarning(msg, args);
	}

	private final class StackElement {
		final int depth;
		final String name;
		final Object o;
		final ClassMeta<?> aType;

		StackElement(int depth, String name, Object o, ClassMeta<?> aType) {
			this.depth = depth;
			this.name = name;
			this.o = o;
			this.aType = aType;
		}

		String toString(boolean simple) {
			StringBuilder sb = new StringBuilder().append('[').append(depth).append(']').append(' ');
			sb.append(isEmpty(name) ? "<noname>" : name).append(':');
			sb.append(aType.toString(simple));
			if (aType != aType.getSerializedClassMeta(BeanTraverseSession.this))
				sb.append('/').append(aType.getSerializedClassMeta(BeanTraverseSession.this).toString(simple));
			return sb.toString();
		}
	}

	/**
	 * Returns the current stack trace.
	 *
	 * @param full
	 * 	If <jk>true</jk>, returns a full stack trace.
	 * @return The current stack trace.
	 */
	protected String getStack(boolean full) {
		StringBuilder sb = new StringBuilder();
		for (StackElement e : stack) {
			if (full) {
				sb.append("\n\t");
				for (int i = 1; i < e.depth; i++)
					sb.append("  ");
				if (e.depth > 0)
					sb.append("->");
				sb.append(e.toString(false));
			} else {
				sb.append(" > ").append(e.toString(true));
			}
		}
		return sb.toString();
	}

	/**
	 * Returns information used to determine at what location in the parse a failure occurred.
	 *
	 * @return A map, typically containing something like <code>{line:123,column:456,currentProperty:"foobar"}</code>
	 */
	public final ObjectMap getLastLocation() {
		ObjectMap m = new ObjectMap();
		if (currentClass != null)
			m.put("currentClass", currentClass);
		if (currentProperty != null)
			m.put("currentProperty", currentProperty);
		if (stack != null && ! stack.isEmpty())
			m.put("stack", stack);
		return m;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Initial depth.
	 *
	 * @see BeanTraverseContext#BEANTRAVERSE_initialDepth
	 * @return
	 * 	The initial indentation level at the root.
	 */
	protected final int getInitialDepth() {
		return ctx.getInitialDepth();
	}

	/**
	 * Configuration property:  Max traversal depth.
	 *
	 * @see BeanTraverseContext#BEANTRAVERSE_maxDepth
	 * @return
	 * 	The depth at which traversal is aborted if depth is reached in the POJO tree.
	 *	<br>If this depth is exceeded, an exception is thrown.
	 */
	protected final int getMaxDepth() {
		return ctx.getMaxDepth();
	}

	/**
	 * Configuration property:  Automatically detect POJO recursions.
	 *
	 * @see BeanTraverseContext#BEANTRAVERSE_detectRecursions
	 * @return
	 * 	<jk>true</jk> if recursions should be checked for during traversal.
	 */
	protected final boolean isDetectRecursions() {
		return ctx.isDetectRecursions();
	}

	/**
	 * Configuration property:  Ignore recursion errors.
	 *
	 * @see BeanTraverseContext#BEANTRAVERSE_ignoreRecursions
	 * @return
	 * 	<jk>true</jk> if when we encounter the same object when traversing a tree, we set the value to <jk>null</jk>.
	 * 	<br>Otherwise, a {@link BeanRecursionException} is thrown with the message <js>"Recursion occurred, stack=..."</js>.
	 */
	protected final boolean isIgnoreRecursions() {
		return ctx.isIgnoreRecursions();
	}
}
