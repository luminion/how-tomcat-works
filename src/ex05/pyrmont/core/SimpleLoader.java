package ex05.pyrmont.core;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;
import org.apache.catalina.Container;
import org.apache.catalina.Loader;
import org.apache.catalina.DefaultContext;

/**
 * 简单的类加载器
 * 用于加载指定目录下的servlet类
 */
public class SimpleLoader implements Loader {

  // 项目路径文件夹
  public static final String WEB_ROOT =
    System.getProperty("user.dir") + File.separator  +  "webroot";

  // 指定位置的类加载器
  ClassLoader classLoader = null;
  // 与该加载器关联的容器
  Container container = null;

  public SimpleLoader() {
    try {
      URL[] urls = new URL[1];
      URLStreamHandler streamHandler = null;
      File classPath = new File(WEB_ROOT);
      // 存放servlet的仓库
      String repository = (new URL("file", null, classPath.getCanonicalPath() + File.separator)).toString() ;
      urls[0] = new URL(null, repository, streamHandler);
      // 加载仓库中的类的加载器
      classLoader = new URLClassLoader(urls);
    }
    catch (IOException e) {
      System.out.println(e.toString() );
    }


  }

  public ClassLoader getClassLoader() {
    return classLoader;
  }

  public Container getContainer() {
    return container;
  }

  public void setContainer(Container container) {
    this.container = container;
  }

  public DefaultContext getDefaultContext() {
    return null;
  }

  public void setDefaultContext(DefaultContext defaultContext) {
  }

  public boolean getDelegate() {
    return false;
  }

  public void setDelegate(boolean delegate) {
  }

  public String getInfo() {
    return "A simple loader";
  }

  public boolean getReloadable() {
    return false;
  }

  public void setReloadable(boolean reloadable) {
  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
  }

  public void addRepository(String repository) {
  }

  public String[] findRepositories() {
    return null;
  }

  public boolean modified() {
    return false;
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
  }

}