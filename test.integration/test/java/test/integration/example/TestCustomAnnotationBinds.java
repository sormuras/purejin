package test.integration.example;

import test.example.app.Support;
import org.junit.jupiter.api.Test;
import se.jbee.inject.Injector;
import se.jbee.inject.bind.ModuleWith;
import se.jbee.inject.binder.BinderModule;
import se.jbee.inject.bootstrap.Bootstrap;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This example shows that custom annotations can be provided via
 * {@link ServiceLoader} using {@link ModuleWith} as contract. This has the
 * benefit of plugging into the bootstrapping without additional setup code but
 * has the drawback that the class implementing the annotation effect cannot
 * have constructor arguments.
 */
class TestCustomAnnotationBinds {

	// annotation and its effect is defined in the com.example.app test dependency

	@Support
	public static class MySupportService {

	}

	static class TestCustomAnnotationBindsModule extends BinderModule {

		@Override
		protected void declare() {
			patternbind(MySupportService.class);
		}
	}

	/**
	 * This verifies that the {@link Support} annotation's effect is loaded via
	 * {@link ServiceLoader}. It is defined in the com.example.app test
	 * dependency jar file. If the {@link MySupportService} can be resolved it
	 * was bound which meant the annotation had done its effect. Otherwise no
	 * binding would exist for the class.
	 */
	@Test
	void customAnnotationsAddedViaServiceLoader() {
	   Injector injector = Bootstrap.injector(Bootstrap.env(),
				TestCustomAnnotationBindsModule.class);
		MySupportService service = injector.resolve(MySupportService.class);
		assertNotNull(service);
	}
}
