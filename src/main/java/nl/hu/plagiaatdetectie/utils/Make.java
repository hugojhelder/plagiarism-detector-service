package nl.hu.plagiaatdetectie.utils;

import java.util.function.Consumer;

public final class Make {

	/**
	 * Helper method to mutate something inline
	 *
	 * <pre>
	 * <code>
	 * HashMap<String, Integer> populatedMap = make(new HashMap<>(), map -> {
	 *     map.put("key1", 1);
	 *     map.put("key2", 2);
	 * })
	 * </code>
	 * </pre>
	 *
	 * @param input the object to mutate
	 * @param mod   the mutation
	 * @param <T>   anything
	 * @return the mutated object
	 */
	public static <T> T make(T input, Consumer<T> mod) {
		mod.accept(input);
		return input;
	}
}