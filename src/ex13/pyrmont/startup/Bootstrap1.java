package ex13.pyrmont.startup;

//explain Host
import ex13.pyrmont.core.SimpleContextConfig;
import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.http.HttpConnector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.loader.WebappLoader;

/**
 * tomcat的host容器
 *
 * org.apache.catalina.Host接口标识一个主机
 *    该接口中比较重要的是map()方法， 用于返回一个处理请求的Context容器实例
 *
 * org.apache.catalina.core.StandardHost类
 *    Host接口的标准实现类，该类同样继承了ContainerBase
 *    该类工作原理与StandardContext相似，构造函数会将一个基础阀的实例添加到其管道对象中
 *    tomcat4与在tomcat5中，start()方法基本相同，不过tomcat5包含了创建JMX对象的代码)
 *    该类比较最重要的方法是map()方法，会返回一个用于处理请求的Container容器(一般是Context对象)
 *
 * org.apache.catalina.core.StandardHostMapper类
 *    host容器的映射器，用于返回一个处理请求的容器(一般为上下文容器)
 *
 * org.apache.catalina.core.StandardHostValve类
 *    该类为host容器的基础阀
 *    该基础阀在接受到请求时
 *      会调用host的map方法,匹配处理的context
 *      将当前线程绑定上一个类加载器
 *      为session续期
 *      将请求交给上下文容器处理
 *
 * 为什么需要host容器：
 *    在tomcat4和tomcat5中，若一个Context实例实用ContextConfig对象进行设置，就必须实用一个Host对象
 *    因Context对象需要知道应用程序web.xml文件的位置，在其applicationConfig()方法中，其会试图打开web.xml文件
 *
 * org.apache.catalina.core.ApplicationContext类
 *    该类实现了javax.servlet.servletContext接口，
 *    在该类的getResource(String path)方法中，需要从父容器中获取hostName
 *    所以Context容器需要关联host容器
 *    在Engine容器中，可以设置一个默认的host容器或context容器（engine容器可以与一个服务实例相关联）
 *
 *
 */
public final class Bootstrap1 {
  public static void main(String[] args) {
    //invoke: http://localhost:8080/app1/Primitive or http://localhost:8080/app1/Modern
    System.setProperty("catalina.base", System.getProperty("user.dir"));
    Connector connector = new HttpConnector();

    // wrapper容器
    Wrapper wrapper1 = new StandardWrapper();
    wrapper1.setName("Primitive");
    wrapper1.setServletClass("PrimitiveServlet");
    Wrapper wrapper2 = new StandardWrapper();
    wrapper2.setName("Modern");
    wrapper2.setServletClass("ModernServlet");

    // context容器
    Context context = new StandardContext();
    // StandardContext's start method adds a default mapper
    context.setPath("/app1");
    context.setDocBase("app1");

    context.addChild(wrapper1);
    context.addChild(wrapper2);

    LifecycleListener listener = new SimpleContextConfig();
    ((Lifecycle) context).addLifecycleListener(listener);

    // host容器
    Host host = new StandardHost();
    host.addChild(context);
    host.setName("localhost");
    host.setAppBase("webapps");

    Loader loader = new WebappLoader();
    context.setLoader(loader);
    // context.addServletMapping(pattern, name);
    context.addServletMapping("/Primitive", "Primitive");
    context.addServletMapping("/Modern", "Modern");

    connector.setContainer(host);
    try {
      connector.initialize();
      ((Lifecycle) connector).start();
      ((Lifecycle) host).start();
  
      // make the application wait until we press a key.
      System.in.read();
      ((Lifecycle) host).stop();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}