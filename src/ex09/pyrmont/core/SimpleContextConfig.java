/**
 * copied from ex08.pyrmont.core.SimpleContextConfig
 */
package ex09.pyrmont.core;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;

public class SimpleContextConfig implements LifecycleListener {

  public void lifecycleEvent(LifecycleEvent event) {
    if (Lifecycle.START_EVENT.equals(event.getType())) {
      Context context = (Context) event.getLifecycle();
      // 设置启动检查为true，否则程序不会启动
      context.setConfigured(true);
    }
  }
}