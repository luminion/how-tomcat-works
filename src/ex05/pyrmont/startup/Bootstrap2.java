package ex05.pyrmont.startup;

import ex05.pyrmont.core.SimpleContext;
import ex05.pyrmont.core.SimpleContextMapper;
import ex05.pyrmont.core.SimpleLoader;
import ex05.pyrmont.core.SimpleWrapper;
import ex05.pyrmont.valves.ClientIPLoggerValve;
import ex05.pyrmont.valves.HeaderLoggerValve;
import org.apache.catalina.Context;
import org.apache.catalina.Loader;
import org.apache.catalina.Mapper;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Valve;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.http.HttpConnector;

/**
 * 包含了两个wrapper实例和一个context实例的启动类
 * 使用映射器来选择处理的子容器
 * 一个映射器对应一个协议，使servlet可以处理多个协议
 * 注意：映射器仅存在于tomcat4中
 */
public final class Bootstrap2 {
  public static void main(String[] args) {
    // 创建连接器
    HttpConnector connector = new HttpConnector();
    // 创建第一个wrapper容器
    Wrapper wrapper1 = new SimpleWrapper();
    // 第一个容器名
    wrapper1.setName("Primitive");
    // 第一个容器对应的servlet的类名
    wrapper1.setServletClass("PrimitiveServlet");

    // 创建第二个wrapper容器
    Wrapper wrapper2 = new SimpleWrapper();
    // 第二个容器的名称
    wrapper2.setName("Modern");
    // 第二个容器对应的servlet的类名
    wrapper2.setServletClass("ModernServlet");

    // context应用程序级别的容器
    Context context = new SimpleContext();
    // 将两个wrapper添加到应用程序容器中
    context.addChild(wrapper1);
    context.addChild(wrapper2);

    // 普通阀1
    Valve valve1 = new HeaderLoggerValve();
    // 普通阀2
    Valve valve2 = new ClientIPLoggerValve();

    // 将阀添加到context应用程序容器中
    ((Pipeline) context).addValve(valve1);
    ((Pipeline) context).addValve(valve2);

    // 上下文路径映射器，帮助容器选择对应子组件处理对应请求
    Mapper mapper = new SimpleContextMapper();
    // 映射的协议
    mapper.setProtocol("http");
    // 关联上下文和映射器
    context.addMapper(mapper);

    // 类加载器
    Loader loader = new SimpleLoader();
    // 关联上下文和类加载器
    context.setLoader(loader);
    // context.addServletMapping(pattern, name);

    // 指定对应servlet和处理路径的映射
    context.addServletMapping("/Primitive", "Primitive");
    context.addServletMapping("/Modern", "Modern");

    // 将context应用程序上下文与连接器关联
    connector.setContainer(context);
    // 初始化启动连接器
    try {
      connector.initialize();
      connector.start();

      // make the application wait until we press a key.
      System.in.read();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}