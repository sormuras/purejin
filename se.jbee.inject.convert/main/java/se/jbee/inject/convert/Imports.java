package se.jbee.inject.convert;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Works similar to an import statement in a source file. The referenced {@link Class}es are the
 * imported fully qualified types. Their simple name is used in other annotations and this
 * annotation is used to determine the fully qualified name.
 *
 * <p>In particular this is used in combination with {@link Converts}.
 *
 * @see Converts
 */
@Inherited
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD, FIELD, CONSTRUCTOR, ANNOTATION_TYPE, PARAMETER})
public @interface Imports {

  /**
   * @return A list of {@link Class} to import so their {@link Class#getSimpleName()} can be used in
   *     properties of other annotations to refer to a fully qualified class.
   */
  Class<?>[] value();
}
