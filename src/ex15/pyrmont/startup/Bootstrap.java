package ex15.pyrmont.startup;

//explain Digester and StandardContext
// use ContextConfig so we don't need to instantiate wrapper

import org.apache.catalina.Connector;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Loader;
import org.apache.catalina.connector.http.HttpConnector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.ContextConfig;

/**
 * 简易配置化
 * 使用server.xml和web.xml中的信息配置
 *
 * 使用server.xml对Tomcat进行配置
 *    在之前的章节中，开始服务的配置都在bootstrap类中，有以下缺陷
 *        所有的配置都必须硬编码
 *        调整组件配置或属性值都需要重新编译bootstrap类
 *    Tomcat设计了一种更优雅的配置方法:
 *        使用server.xml文档来对应用程序进行配置
 *        xml文件中的么个元素都会转换为一个java对象，元素的属性会用于设置java对象的属性
 *        如此，可以通过简单的编辑server.xml文件修改tomcat的配置
 *        例如，server.xml文件中的Context元素表示一个Context实例
 *    Tomcat实用开源库Digester将xml文档中的元素转化为java对象
 *
 * Digester库
 *    用于解析xml文档并生成对象
 *    见本包Test01、Test02、Test03
 *
 * org.apache.catalina.startup.ContextConfig类
 *    StandardContext的配置类(同时也是监听器)，用于配置StandardContext实例
 *        与其他类型容器不同，StandardContext实例必须有一个监听器
 *        该监听器负责配置StandardContext实例
 *        设置成功后湖将StandardContext的变量configured设置为true
 *        StandardContext启动时会检查变量configured，仅当其为true时，才会启动
 *            前面章节中使用的SimpleContextConfig就是作为StandardContext的监听器，
 *            其不做配置，仅将configured设置为true让程序启动
 *
 *    该类为实际部署时的标准监听器，该类会执行许多StandardContext的配置任务：
 *        安装一个验证器阀到StandardContext实例的管道对象中
 *        添加一个许可阀(org.apache.catalina.valves.CertificatesValve)到管道对象中
 *        解析默认的web.xml文件
 *            默认该文件位于CATALINA_HOME下conf目录中
 *            应用程序的该文件位于应用程序目录下的WEB-INF目录中
 *            即使该两个文件都没找到，ContextConfig仍然会继续执行
 *            文件作用：
 *                定义并映射了很多默认的servlet
 *                配置了许多MINE文件的映射
 *                定义了默认的Session超时时间
 *                定义了欢迎文件的列表
 *        为每个servlet元素创建一个StandardWrapper类
 *
 *    使用该类后，配置相对变简单了，不再需要实例化一个Wrapper实例了
 *
 */
public final class Bootstrap {

  // invoke: http://localhost:8080/app1/Modern or 
  // http://localhost:8080/app2/Primitive
  // note that we don't instantiate a Wrapper here,
  // ContextConfig reads the WEB-INF/classes dir and loads all servlets.
  public static void main(String[] args) {
    System.setProperty("catalina.base", System.getProperty("user.dir"));
    Connector connector = new HttpConnector();

    Context context = new StandardContext();
    // StandardContext's start method adds a default mapper
    context.setPath("/app1");
    context.setDocBase("app1");
    // 注意此处没有实例化一个 Wrapper，而是实例化一个ContextConfig 读取 WEB-INF/classes 目录并加载所有 servlet
    LifecycleListener listener = new ContextConfig();
    ((Lifecycle) context).addLifecycleListener(listener);

    Host host = new StandardHost();
    host.addChild(context);
    host.setName("localhost");
    host.setAppBase("webapps");

    Loader loader = new WebappLoader();
    context.setLoader(loader);
    connector.setContainer(host);
    try {
      connector.initialize();
      ((Lifecycle) connector).start();
      ((Lifecycle) host).start();
      Container[] c = context.findChildren();
      int length = c.length;
      for (int i=0; i<length; i++) {
        Container child = c[i];
        System.out.println(child.getName());
      }

      // make the application wait until we press a key.
      System.in.read();
      ((Lifecycle) host).stop();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}