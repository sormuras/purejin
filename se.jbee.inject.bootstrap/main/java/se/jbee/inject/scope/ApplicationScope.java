package se.jbee.inject.scope;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import se.jbee.inject.*;

/**
 * Asks the {@link Provider} once per binding. Thereby instances become singletons local to the
 * application.
 *
 * <p>Will lead to instances that can be seen as application-wide-singletons.
 *
 * <p>Contains an instance per {@link Generator}. Instances are never updated.
 */
public final class ApplicationScope implements Scope {

  private final AtomicReference<AtomicReferenceArray<Object>> instances = new AtomicReference<>();

  @SuppressWarnings("unchecked")
  @Override
  public <T> T provide(int serialID, int resources, Dependency<? super T> dep, Provider<T> provider)
      throws UnresolvableDependency {
    return (T)
        instances
            .updateAndGet(objs -> objs != null ? objs : new AtomicReferenceArray<>(resources))
            .updateAndGet(serialID, obj -> obj != null ? obj : provider.provide());
  }
}
