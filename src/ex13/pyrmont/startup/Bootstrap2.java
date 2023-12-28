package ex13.pyrmont.startup;

//Use engine
import ex13.pyrmont.core.SimpleContextConfig;
import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.http.HttpConnector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.loader.WebappLoader;


/**
 * tomcat的引擎容器
 * org.apache.catalina.Engine接口
 *    用于表示tomcat的servlet引擎，当部署tomcat时需要支持多个虚拟机的的话，需要engine容器
 *    一般情况下，部署的tomcat都会使用一个engine容器
 *
 * org.apache.catalina.core.StandardEngine类
 *    Engine接口的标准实现，相比于StandardContext和StandardHost，该类比较小
 *    在实例化的时候，该类会添加一个基础阀
 *    该容器的addChild()方法做了限制，子容器只能是Host容器，否则会抛出异常
 *    该容器不能添加父容器
 *    若调用其setParent()方法，为其添加父容器时会抛出异常
 *
 * org.apache.catalina.core.StandardEngineValve类
 *    Engine容器的基础阀
 *    该类的invoke()方法会依次：
 *    验证request独享和response对象类型
 *    得到host实例
 *    调用host实例的invoke方法处理请求
 *
 *
 *
 */
public final class Bootstrap2 {
  public static void main(String[] args) {
    //invoke: http://localhost:8080/app1/Primitive or http://localhost:8080/app1/Modern
    System.setProperty("catalina.base", System.getProperty("user.dir"));
    Connector connector = new HttpConnector();

    Wrapper wrapper1 = new StandardWrapper();
    wrapper1.setName("Primitive");
    wrapper1.setServletClass("PrimitiveServlet");
    Wrapper wrapper2 = new StandardWrapper();
    wrapper2.setName("Modern");
    wrapper2.setServletClass("ModernServlet");

    Context context = new StandardContext();
    // StandardContext's start method adds a default mapper
    context.setPath("/app1");
    context.setDocBase("app1");

    context.addChild(wrapper1);
    context.addChild(wrapper2);

    LifecycleListener listener = new SimpleContextConfig();
    ((Lifecycle) context).addLifecycleListener(listener);

    Host host = new StandardHost();
    host.addChild(context);
    host.setName("localhost");
    host.setAppBase("webapps");

    Loader loader = new WebappLoader();
    context.setLoader(loader);
    // context.addServletMapping(pattern, name);
    context.addServletMapping("/Primitive", "Primitive");
    context.addServletMapping("/Modern", "Modern");

    Engine engine = new StandardEngine();
    engine.addChild(host);
    engine.setDefaultHost("localhost");

    connector.setContainer(engine);
    try {
      connector.initialize();
      ((Lifecycle) connector).start();
      ((Lifecycle) engine).start();
  
      // make the application wait until we press a key.
      System.in.read();
      ((Lifecycle) engine).stop();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}