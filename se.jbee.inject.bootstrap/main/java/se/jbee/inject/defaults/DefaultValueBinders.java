/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.defaults;

import se.jbee.inject.*;
import se.jbee.inject.bind.*;
import se.jbee.inject.binder.*;
import se.jbee.inject.config.ConstructsBy;
import se.jbee.inject.config.ContractsBy;
import se.jbee.inject.lang.Reflect;
import se.jbee.inject.lang.Type;

import java.lang.reflect.Constructor;

import static se.jbee.inject.Instance.anyOf;
import static se.jbee.inject.bind.BindingType.*;
import static se.jbee.inject.bind.Bindings.supplyConstant;
import static se.jbee.inject.bind.Bindings.supplyScopedConstant;
import static se.jbee.inject.binder.Supply.*;
import static se.jbee.inject.lang.Type.raw;
import static se.jbee.inject.lang.Utils.isClassBanal;
import static se.jbee.inject.lang.Utils.isClassConstructable;

/**
 * Utility with default {@link ValueBinder}s.
 */
public final class DefaultValueBinders {

	public static final ValueBinder.Completion<Descriptor.BridgeDescriptor> BRIDGE = DefaultValueBinders::bindGenericReference;
	public static final ValueBinder.Completion<Descriptor.ArrayDescriptor> ARRAY = DefaultValueBinders::bindArrayElements;
	public static final ValueBinder.Completion<Constructs<?>> CONSTRUCTS = DefaultValueBinders::bindConstruction;
	public static final ValueBinder.Completion<Produces<?>> PRODUCES = DefaultValueBinders::bindProduction;
	public static final ValueBinder.Completion<Accesses<?>> SHARES = DefaultValueBinders::bindAccess;
	public static final ValueBinder<Instance<?>> REFERENCE = DefaultValueBinders::bindReference;
	public static final ValueBinder<Instance<?>> REFERENCE_PREFER_CONSTANTS = DefaultValueBinders::bindReferencePreferConstants;
	public static final ValueBinder<Constant<?>> CONSTANT = DefaultValueBinders::bindConstant;
	public static final ValueBinder<Binding<?>> CONTRACTS = DefaultValueBinders::bindContractTypes;

	private DefaultValueBinders() {
		throw new UnsupportedOperationException("util");
	}

	/**
	 * This {@link ValueBinder} adds bindings to super-types for {@link
	 * Binding}s declared with {@link DeclarationType#CONTRACT} or {@link
	 * DeclarationType#PROVIDED}.
	 */
	private static <T> void bindContractTypes(Env env, Binding<?> ref,
			Binding<T> item, Bindings dest) {
		DeclarationType dt = item.source.declarationType;
		if (dt != DeclarationType.CONTRACT && dt != DeclarationType.PROVIDED) {
			dest.add(env, item);
			return;
		}
		Class<T> impl = item.type().rawType;
		ContractsBy contractsBy = env.property(ContractsBy.class, impl.getPackage());
		if (contractsBy.isContract(impl, impl))
			dest.add(env, item);
		for (Type<? super T> supertype : item.type().supertypes())
			// Object is of course a superclass but not indented when doing super-binds
			if (supertype.rawType != Object.class //
					&& contractsBy.isContract(supertype.rawType, impl))
				dest.add(env, item.typed(supertype));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static <T> Binding<T> bindArrayElements(Env env, Binding<T> item,
			Descriptor.ArrayDescriptor ref) {
		return item.complete(PREDEFINED,
				createArraySupplier((Type) item.type(), ref.elements));
	}

	@SuppressWarnings("unchecked")
	private static <E> Supplier<E> createArraySupplier(Type<E[]> array,
			Hint<?>[] elements) {
		return (Supplier<E>) Supply.fromElements(array,
				(Hint<? extends E>[]) elements);
	}

	private static <T> Binding<T> bindGenericReference(Env env, Binding<T> item,
			Descriptor.BridgeDescriptor ref) {
		return item.complete(BindingType.REFERENCE,
				byParameterizedInstanceReference(
						anyOf(raw(ref.type).castTo(item.type()))));
	}

	private static <T> Binding<T> bindConstruction(Env env, Binding<T> item,
			Constructs<?> ref) {
		env.accessible(ref.target);
		return item.complete(CONSTRUCTOR, byConstruction(ref.typed(item.type()))) //
				.verifiedBy(env.verifierFor(ref));
	}

	private static <T> Binding<T> bindProduction(Env env, Binding<T> item,
			Produces<?> ref) {
		env.accessible(ref.target);
		return item.complete(METHOD, byProduction(ref.typed(item.type()))) //
				.verifiedBy(env.verifierFor(ref));
	}

	private static <T> Binding<T> bindAccess(Env env, Binding<T> item,
			Accesses<?> ref) {
		env.accessible(ref.target);
		// reference itself is used as supplier, as it also provides the default implementation
		return item.complete(FIELD, ref.typed(item.type())) //
				.verifiedBy(env.verifierFor(ref));
	}

	private static <T> void bindConstant(Env env, Constant<?> ref,
			Binding<T> item, Bindings dest) {
		T constant = item.type().rawType.cast(ref.value);
		Supplier<T> supplier = ref.scoped
				? supplyScopedConstant(constant)
				: supplyConstant(constant);
		dest.addExpanded(env,
				item.complete(BindingType.PREDEFINED, supplier));
		Class<?> impl = ref.value.getClass();
		// implicitly bind to the exact type of the constant
		// should that differ from the binding type
		if (ref.autoBindExactType && item.source.declarationType == DeclarationType.EXPLICIT && item.type().rawType != impl) {
			@SuppressWarnings("unchecked")
			Class<T> type = (Class<T>) ref.value.getClass();
			dest.addExpanded(env,
					Binding.binding(item.signature.typed(raw(type)),
							BindingType.PREDEFINED, supplier, item.scope,
							item.source.typed(DeclarationType.IMPLICIT)));
		}
	}

	private static <T> void bindReferencePreferConstants(Env env,
			Instance<?> ref, Binding<T> item, Bindings dest) {
		Type<?> refType = ref.type();
		if (isClassBanal(refType.rawType)) {
			dest.addExpanded(env, item, new Constant<>(
					Reflect.construct(refType.rawType, env::accessible,
							RuntimeException::new)).manual());
			//TODO shouldn't this use New instead?
			return;
		}
		bindReference(env, ref, item, dest);
	}

	private static <T> void bindReference(Env env, Instance<?> ref,
			Binding<T> item, Bindings dest) {
		Type<?> refType = ref.type();
		if (isCompatibleSupplier(item.type(), refType)) {
			@SuppressWarnings("unchecked")
			Class<? extends Supplier<? extends T>> supplier = (Class<? extends Supplier<? extends T>>) refType.rawType;
			dest.addExpanded(env, item.complete(BindingType.REFERENCE,
					Supply.bySupplierReference(supplier)));
			implicitlyBindToConstructor(env, ref, item, dest);
			return;
		}
		final Type<? extends T> type = refType.castTo(item.type());
		final Instance<T> bound = item.signature.instance;
		if (!bound.type().equalTo(type) || !ref.name.isCompatibleWith(
				bound.name)) {
			dest.addExpanded(env, item.complete(BindingType.REFERENCE,
					Supply.byInstanceReference(ref.typed(type))));
			implicitlyBindToConstructor(env, ref, item, dest);
			return;
		}
		if (type.isInterface())
			throw InconsistentBinding.referenceLoop(item, ref, bound);
		bindToConstructsBy(env, type.rawType, item, dest);
	}

	private static <T> boolean isCompatibleSupplier(Type<T> requiredType,
			Type<?> providedType) {
		if (!providedType.isAssignableTo(raw(Supplier.class)))
			return false;
		if (requiredType.isAssignableTo(raw(Supplier.class)))
			return false;
		return providedType.toSuperType( Supplier.class) //
				.parameter(0).isAssignableTo(requiredType);
	}

	private static <T> void implicitlyBindToConstructor(Env env,
			Instance<T> ref, Binding<?> item, Bindings dest) {
		Class<T> impl = ref.type().rawType;
		if (isClassConstructable(impl)) {
			Binding<T> binding = Binding.binding(
					new Locator<>(ref).indirect(item.signature.target.indirect),
					BindingType.CONSTRUCTOR, null, item.scope,
					item.source.typed(DeclarationType.IMPLICIT));
			bindToConstructsBy(env, impl, binding, dest);
		}
	}

	private static <T> void bindToConstructsBy(Env env, Class<? extends T> ref,
			Binding<T> item, Bindings dest) {
		Constructor<?> c = env.property(ConstructsBy.class,
				item.source.pkg()).reflect(ref.getDeclaredConstructors());
		if (c != null)
			dest.addExpanded(env, item,
					Constructs.constructs(raw(c.getDeclaringClass()), c));
	}
}
