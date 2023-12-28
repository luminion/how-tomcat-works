/**
 * copied from ex08.pyrmont.core.SimpleWrapperValve
 */
package ex09.pyrmont.core;

import java.io.IOException;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.Context;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Valve;
import org.apache.catalina.ValveContext;
import org.apache.catalina.Contained;
import org.apache.catalina.Container;

/**
 * 该类有所不同
 *
 * servlet实例可以调用javax.servlet.http.HttpServletRequest接口的getSession()方法获取session
 * 当调用getSession()方法时，request对象必须调用与Context容器关联的session管理器的方法获取session
 * request对象为了能访问session管理器，他必须要能访问Context容器
 * 为了达到此目的，在该类的invoke()方法调用servlet的service方法之前，
 * 需要调用org.apache.catalina.Request接口的setContext()方法传入Context实例，将Context容器关联
 *
 */
public class SimpleWrapperValve implements Valve, Contained {

  protected Container container;

  public void invoke(Request request, Response response, ValveContext valveContext)
    throws IOException, ServletException {

    SimpleWrapper wrapper = (SimpleWrapper) getContainer();
    ServletRequest sreq = request.getRequest();
    ServletResponse sres = response.getResponse();
    Servlet servlet = null;
    HttpServletRequest hreq = null;
    if (sreq instanceof HttpServletRequest)
      hreq = (HttpServletRequest) sreq;
    HttpServletResponse hres = null;
    if (sres instanceof HttpServletResponse)
      hres = (HttpServletResponse) sres;

    //-- new addition -----------------------------------
    Context context = (Context) wrapper.getParent();
    request.setContext(context);
    //-------------------------------------
    // Allocate a servlet instance to process this request
    try {
      servlet = wrapper.allocate();
      if (hres!=null && hreq!=null) {
        servlet.service(hreq, hres);
      }
      else {
        servlet.service(sreq, sres);
      }
    }
    catch (ServletException e) {
    }
  }

  public String getInfo() {
    return null;
  }

  public Container getContainer() {
    return container;
  }

  public void setContainer(Container container) {
    this.container = container;
  }
}