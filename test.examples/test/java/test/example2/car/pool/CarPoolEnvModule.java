package test.example2.car.pool;

import static se.jbee.lang.Type.raw;

import se.jbee.inject.binder.LocalEnvModule;
import se.jbee.inject.config.ProducesBy;
import test.example2.Car;

public class CarPoolEnvModule extends LocalEnvModule {
  @Override
  protected void declare() {
    bind(ProducesBy.class)
        .to(ProducesBy.declaredMethods(false).returnTypeAssignableTo(raw(Car[].class)));
  }
}
