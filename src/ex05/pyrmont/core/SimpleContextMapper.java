package ex05.pyrmont.core;

import javax.servlet.http.HttpServletRequest;
import org.apache.catalina.Container;
import org.apache.catalina.HttpRequest;
import org.apache.catalina.Mapper;
import org.apache.catalina.Request;
import org.apache.catalina.Wrapper;

/**
 * 简单映射器
 * 用于指定协议和处理的servlet的映射方法
 * 从请求路径中找到指定的servlet名称，然后寻找指定的servlet容器
 */
public class SimpleContextMapper implements Mapper {

  /**
   * The Container with which this Mapper is associated.
   * 相关联的容器
   */
  private SimpleContext context = null;

  public Container getContainer() {
    return (context);
  }

  public void setContainer(Container container) {
    if (!(container instanceof SimpleContext))
      throw new IllegalArgumentException
        ("Illegal type of container");
    context = (SimpleContext) container;
  }

  public String getProtocol() {
    return null;
  }

  public void setProtocol(String protocol) {
  }


  /**
   * Return the child Container that should be used to process this Request,
   * based upon its characteristics.  If no such child Container can be
   * identified, return <code>null</code> instead.
   *
   * 处理请求的映射
   * 根据其特征返回应用于处理此请求的子容器。
   * 如果无法识别这样的子容器，则返回null代替
   *
   * @param request Request being processed
   * @param update Update the Request to reflect the mapping selection?
   *
   * @exception IllegalArgumentException if the relative portion of the
   *  path cannot be URL decoded
   */
  public Container map(Request request, boolean update) {
    // Identify the context-relative URI to be mapped
    String contextPath =
      ((HttpServletRequest) request.getRequest()).getContextPath();
    String requestURI = ((HttpRequest) request).getDecodedRequestURI();
    String relativeURI = requestURI.substring(contextPath.length());
    // Apply the standard request URI mapping rules from the specification
    Wrapper wrapper = null;
    String servletPath = relativeURI;
    String pathInfo = null;
    String name = context.findServletMapping(relativeURI);
    if (name != null)
      wrapper = (Wrapper) context.findChild(name);
    return (wrapper);
  }
}