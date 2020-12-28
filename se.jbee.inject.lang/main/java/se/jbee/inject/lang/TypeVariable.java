package se.jbee.inject.lang;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.WildcardType;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.UnaryOperator;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;

/**
 * A utility to help extract actual {@link Type} of
 * {@link java.lang.reflect.TypeVariable}s.
 *
 * @author Jan Bernitt
 * @since 8.1
 */
public final class TypeVariable {

	private TypeVariable() {
		throw new UnsupportedOperationException("util");
	}

	/**
	 * Returns a map with type variable names as keys and a
	 * {@link UnaryOperator} as value that given an actual {@link Type} of the
	 * declared {@link java.lang.reflect.Type} will extract the actual
	 * {@link Type} for the type variable.
	 *
	 * @param type a generic type as returned by Java reflect for generic type
	 *            of for methods, fields or parameters
	 * @return a mapping which for each type variable (name) holds a
	 *         {@link UnaryOperator} which resolves the actual type of that
	 *         variable for the actual {@link Type} for the provided
	 *         {@link java.lang.reflect.Type}.
	 */
	@SuppressWarnings("ChainOfInstanceofChecks")
	public static Map<java.lang.reflect.TypeVariable<?>, UnaryOperator<Type<?>>> typeVariables(
			java.lang.reflect.Type type) {
		if (type instanceof Class)
			return emptyMap();
		if (type instanceof GenericArrayType) {
			Map<java.lang.reflect.TypeVariable<?>, UnaryOperator<Type<?>>> vars = emptyMap();
			typeVariables(
					((GenericArrayType) type).getGenericComponentType()).forEach(
							(k, v) -> vars.put(k, t -> v.apply(t).baseType()));
			return vars;
		}
		if (type instanceof WildcardType) {
			Map<java.lang.reflect.TypeVariable<?>, UnaryOperator<Type<?>>> vars = emptyMap();
			for (java.lang.reflect.Type upperBound : ((WildcardType) type).getUpperBounds()) {
				vars.putAll(typeVariables(upperBound));
			}
			return vars;
		}
		if (type instanceof ParameterizedType) {
			Map<java.lang.reflect.TypeVariable<?>, UnaryOperator<Type<?>>> vars = emptyMap();
			java.lang.reflect.Type[] generics = ((ParameterizedType) type).getActualTypeArguments();
			for (int i = 0; i < generics.length; i++) {
				java.lang.reflect.Type generic = generics[i];
				int index = i;
				typeVariables(generic).forEach((k, v) -> vars.put(k,
						t -> v.apply(t.parameter(index))));
			}
			return vars;
		}
		if (type instanceof java.lang.reflect.TypeVariable) {
			return singletonMap(
					((java.lang.reflect.TypeVariable<?>) type),
					UnaryOperator.identity());
		}
		throw new UnsupportedOperationException(
				"Type has no support yet: " + type);
	}

	private static Map<java.lang.reflect.TypeVariable<?>, UnaryOperator<Type<?>>> emptyMap() {
		return new TreeMap<>(Type::typeVariableComparator);
	}

	/**
	 * Returns the actual types for each type variable for a particular actual
	 * class {@link Type}.
	 *
	 * @param variables  as generated by {@link #typeVariables(java.lang.reflect.Type)}
	 * @param actualType the actual {@link Type} for the same {@link
	 *                   java.lang.reflect.Type} passed to {@link
	 *                   #typeVariables(java.lang.reflect.Type)}
	 * @return A map with type variable names as the key and the actual type for
	 * each of them given the overall actual type was the given type.
	 */
	public static Map<java.lang.reflect.TypeVariable<?>, Type<?>> actualTypesFor(
			Map<java.lang.reflect.TypeVariable<?>, UnaryOperator<Type<?>>> variables, Type<?> actualType) {
		return variables.entrySet().stream().collect(
				toMap(Map.Entry::getKey, e -> e.getValue().apply(actualType)));
	}
}
