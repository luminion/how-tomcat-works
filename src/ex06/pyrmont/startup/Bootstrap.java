package ex06.pyrmont.startup;

import ex06.pyrmont.core.SimpleContext;
import ex06.pyrmont.core.SimpleContextLifecycleListener;
import ex06.pyrmont.core.SimpleContextMapper;
import ex06.pyrmont.core.SimpleLoader;
import ex06.pyrmont.core.SimpleWrapper;
import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Mapper;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.http.HttpConnector;

/**
 * 基于ex05.pyrmont开发。
 * 添加生命周期事件
 * 涉及2个接口和一个相关类
 *
 * Lifecycle接口
 *    Catalina在设计上允许一个组件包含其他组件，父组件负责启动、关闭其子组件
 *    这种单一启动、关闭机制，是通过lifecycle接口实现的
 *      编码时保证类会调用其他所有依赖类、子组件等，实现Lifecycle接口；
 *      启动时在自身的start()、stop()方法内调用后所有相关类的start()、stop() 方法即可
 *    其中多个事件、类似START_EVENT和STOP_EVENT等
 *    该接口中最重要的方法是start() 和stop，组件必须提供这两个方法的实现，供其父组件调用
 *    其他的3个方法是与事件监听器相关的，分别为
 *          addLifecycleListener()
 *          removeLifecycleListener()
 *          finalLifecycleListener()
 *
 * LifecycleListener接口
 *    生命周期的事件监听器
 *    该接口中只有一个方法lifecycleEvent()，即监听到指定事件时会发生的方法
 *
 * LifecycleEvent类
 *    生命周期事件类
 *
 * LifecycleSupport支持类
 *    协助向已注册的 LifecycleListener 发出 LifecycleEvent 通知。
 *
 *
 *
 *
 */
public final class Bootstrap {
  public static void main(String[] args) {
    // 连接器
    Connector connector = new HttpConnector();

    // servlet容器wrapper
    Wrapper wrapper1 = new SimpleWrapper();
    wrapper1.setName("Primitive");
    wrapper1.setServletClass("PrimitiveServlet");
    Wrapper wrapper2 = new SimpleWrapper();
    wrapper2.setName("Modern");
    wrapper2.setServletClass("ModernServlet");

    // 应用程序容器context
    Context context = new SimpleContext();
    context.addChild(wrapper1);
    context.addChild(wrapper2);

    // 映射器
    Mapper mapper = new SimpleContextMapper();
    mapper.setProtocol("http");
    context.addMapper(mapper);

    // 生命周期监听器
    LifecycleListener listener = new SimpleContextLifecycleListener();
    ((Lifecycle) context).addLifecycleListener(listener);


    // 类加载器
    Loader loader = new SimpleLoader();
    context.setLoader(loader);
    // context.addServletMapping(pattern, name);
    context.addServletMapping("/Primitive", "Primitive");
    context.addServletMapping("/Modern", "Modern");
    connector.setContainer(context);

    // 启动
    try {
      connector.initialize();
      ((Lifecycle) connector).start();
      ((Lifecycle) context).start();

      // make the application wait until we press a key.
      System.in.read();
      ((Lifecycle) context).stop();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}