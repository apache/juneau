package org.apache.juneau;

public interface AssertionPredicate<T> {

	public void test(T value);
}
