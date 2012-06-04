package de.jbee.inject.util;

import de.jbee.inject.Instance;
import de.jbee.inject.Scope;
import de.jbee.inject.Supplier;

public interface BasicBinder {

	<T> TypedBasicBinder<T> bind( Instance<T> instance );

	//void install( Module module ); // this would allow doing narrowed installations - could be confusing

	interface TypedBasicBinder<T> {

		void to( Supplier<? extends T> supplier );

	}

	/**
	 * The ROOT- {@link RootBasicBinder}.
	 * 
	 * @author Jan Bernitt (jan.bernitt@gmx.de)
	 */
	public interface RootBasicBinder
			extends ScopedBasicBinder {

		ScopedBasicBinder in( Scope scope );

	}

	/**
	 * A {@link Scope} had been defined.
	 * 
	 * @author Jan Bernitt (jan.bernitt@gmx.de)
	 */
	interface ScopedBasicBinder
			extends TargetedBasicBinder {

		TargetedBasicBinder injectingInto( Instance<?> target );
	}

	/**
	 * Bindings have been restricted and are just effective within a special scope defined before
	 * using {@link ScopedBasicBinder#injectingInto(Class)}-clauses.
	 * 
	 * @author Jan Bernitt (jan.bernitt@gmx.de)
	 */
	interface TargetedBasicBinder
			extends BasicBinder /* LocalisedBinder */{

	}

	interface LocalisedBasicBinder
			extends BasicBinder {

		LocalisedBasicBinder havingParent( Class<?> type );

		LocalisedBasicBinder havingDirectParent( Class<?> type );

	}

}
