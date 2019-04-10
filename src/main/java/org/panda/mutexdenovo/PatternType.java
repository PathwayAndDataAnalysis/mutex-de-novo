package org.panda.mutexdenovo;

/**
 * To specific if the user is interested for mutual exclusivity or co-occurrence in various operations.
 */
public enum PatternType
{
	MUTEX,
	COOC,
	;

	public static PatternType get(String tag)
	{
		switch (tag)
		{
			case "mutex":
			case "mutual-exclusivity":
				return MUTEX;
			case "cooc":
			case "co-occurrence":
				return COOC;
			default: return null;
		}
	}
}
