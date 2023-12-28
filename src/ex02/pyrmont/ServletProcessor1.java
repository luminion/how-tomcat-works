package ex02.pyrmont;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;
import java.io.File;
import java.io.IOException;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * 请求处理器1
 * servlet请求处理类
 * 从uri中获取servlet的名称
 * 通过反射获取指定servlet的类对象
 */
public class ServletProcessor1 {

  public void process(Request request, Response response) {

    String uri = request.getUri();
    // 获取请求中的servlet的名称
    String servletName = uri.substring(uri.lastIndexOf("/") + 1);
    URLClassLoader loader = null;

    try {
      // create a URLClassLoader
      URL[] urls = new URL[1];
      URLStreamHandler streamHandler = null;
      File classPath = new File(Constants.WEB_ROOT);
      // the forming of repository is taken from the createClassLoader method in
      // org.apache.catalina.startup.ClassLoaderFactory  webroot文件夹所在的路径
      String repository = (new URL("file", null, classPath.getCanonicalPath() + File.separator)).toString() ;
      // the code for forming the URL is taken from the addRepository method in
      // org.apache.catalina.loader.StandardClassLoader class.
      urls[0] = new URL(null, repository, streamHandler);
      loader = new URLClassLoader(urls);
    }
    catch (IOException e) {
      System.out.println(e.toString() );
    }
    Class myClass = null;
    try {
      // 载入servlet
      myClass = loader.loadClass(servletName);
    }
    catch (ClassNotFoundException e) {
      System.out.println(e.toString());
    }

    Servlet servlet = null;

    try {
      // 创建servlet对象
      servlet = (Servlet) myClass.newInstance();
      // 处理请求
      servlet.service((ServletRequest) request, (ServletResponse) response);
    }
    catch (Exception e) {
      System.out.println(e.toString());
    }
    catch (Throwable e) {
      System.out.println(e.toString());
    }

  }
}