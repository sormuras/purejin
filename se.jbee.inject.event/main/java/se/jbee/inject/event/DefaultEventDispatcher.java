package se.jbee.inject.event;

import java.lang.reflect.Method;
import se.jbee.inject.config.Connector;
import se.jbee.lang.Type;

public class DefaultEventDispatcher implements Connector {

  @Override
  public void connect(Object instance, Type<?> as, Method connected) {}
}
