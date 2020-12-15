/*
 *  Copyright (c) 2012-2019, Jan Bernitt
 *
 *  Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
 */
package se.jbee.inject.binder;

import se.jbee.inject.*;
import se.jbee.inject.bind.Module;
import se.jbee.inject.bind.*;
import se.jbee.inject.lang.Utils;

import static se.jbee.inject.Scope.container;
import static se.jbee.inject.lang.Type.raw;

/**
 * The default utility {@link Module} almost always used.
 *
 * A {@link BinderModule} is also a {@link Bundle} so it should be used and
 * installed as such. It will than {@link Bundle#bootstrap(Bootstrapper)} itself
 * as a module.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public abstract class BinderModule extends InitializedBinder
		implements Bundle, Module {

	private final Class<? extends Bundle> basis;

	protected BinderModule() {
		this(null);
	}

	protected BinderModule(Class<? extends Bundle> basis) {
		this.basis = basis;
	}

	@Override
	public final void bootstrap(Bootstrapper bootstrap) {
		bootstrap.installDefaults();
		if (basis != null)
			bootstrap.install(basis);
		bootstrap.install(this);
		installAnnotated(getClass(), bootstrap);
	}

	@Override
	public void declare(Bindings bindings, Env env) {
		__init__(configure(env), bindings);
		declare();
	}

	protected Env configure(Env env) {
		return env;
	}

	protected final <P> P env(Class<P> property) {
		return env().property(property, bind().source.pkg());
	}

	@Override
	public String toString() {
		return "module " + getClass().getSimpleName();
	}

	/**
	 * Binds a {@link ScopeLifeCycle} with the needed {@link Scope#container}.
	 *
	 * @since 8.1
	 * @param lifeCycle the instance to bind, not null
	 */
	protected final void bindLifeCycle(ScopeLifeCycle lifeCycle) {
		bindLifeCycle(lifeCycle.scope).to(lifeCycle);
	}

	protected final TypedBinder<ScopeLifeCycle> bindLifeCycle(Name scope) {
		return per(container).bind(scope, ScopeLifeCycle.class);
	}

	/**
	 * Starts the binding of a {@link Scope}.
	 *
	 * @since 8.1
	 * @param scope name of the scope to create
	 * @return fluent API to invoke one of the {@code to} methods to provide the
	 *         {@link Scope} or the indirection creating it.
	 */
	protected final TypedBinder<Scope> bindScope(Name scope) {
		return per(container).bind(scope, Scope.class);
	}

	/**
	 * @see Module#declare(Bindings, Env)
	 */
	protected abstract void declare();
}
