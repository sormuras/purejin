package test.integration.bind;

import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.binder.BootstrapperBundle;
import se.jbee.inject.bootstrap.Bootstrap;
import se.jbee.inject.defaults.CoreFeature;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * A test that demonstrates how install {@link CoreFeature#LOGGER} so that each
 * class gets its class-specific- {@link Logger} injected.
 * <p>
 * This is also meant as a template to learn how to implement a similar feature
 * for another logging framework. Just have a look at how the {@link
 * BinderModule} implementing the {@link CoreFeature#LOGGER} toggle is
 * implemented.
 */
class TestBasicLoggerBinds {

	private static class TestBasicLoggerBindsBundle extends BootstrapperBundle {

		@Override
		protected void bootstrap() {
			install(CoreFeature.LOGGER);
			install(TestBasicLoggerBindsModule.class);
		}

	}

	private static class TestBasicLoggerBindsModule extends BinderModule {

		@Override
		protected void declare() {
			construct(Foo.class);
		}

	}

	public static class Foo {

		final Logger logger;

		@SuppressWarnings("unused")
		public Foo(Logger logger) {
			this.logger = logger;
		}
	}

	@Test
	void eachClassGetsTheLoggerWithItsCanonicalNameInjected() {
		Injector context = Bootstrap.injector(TestBasicLoggerBindsBundle.class);
		Foo foo = context.resolve(Foo.class);
		assertSame(Logger.getLogger(Foo.class.getCanonicalName()), foo.logger);
	}
}
