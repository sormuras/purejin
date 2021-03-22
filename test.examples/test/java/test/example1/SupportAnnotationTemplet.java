package test.example1;

import java.lang.annotation.Annotation;
import java.util.ServiceLoader;
import se.jbee.inject.Extends;
import se.jbee.inject.Scope;
import se.jbee.inject.binder.BinderModuleWith;

/**
 * This is bound to the annotation using {@link ServiceLoader}.
 *
 * <p>There are two ways to link this class to an {@link Annotation}.
 *
 * <p>1. Annotated this class with the {@link Annotation} it implements (works only if there is no
 * other annotation present)
 *
 * <p>2. Use {@link Extends} annotation to point out the {@link Annotation} {@link Class} to link
 * this class with
 *
 * <p>As {@link Extends} allows other annotations to be present it should be preferred where
 * possible.
 */
@Extends(Support.class)
public class SupportAnnotationTemplet extends BinderModuleWith<Class<?>> {

  @Override
  protected void declare(Class<?> annotated) {
    per(Scope.application).withPublishedAccess().bind(annotated).toConstructor();
  }
}
