package org.apache.juneau;

import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

public class AssertionHelpers {

	/**
	 * Asserts value when stringified matches the specified pattern.
	 */
	public static Object assertMatches(String pattern, Object value) {
		var m = getMatchPattern(pattern).matcher(s(value));
		if (! m.matches()) {
			var msg = "Pattern didn't match: \n\tExpected:\n"+pattern+"\n\tActual:\n"+value;
			System.err.println(msg);  // For easier debugging.
			fail(msg);
		}
		return value;
	}
}
