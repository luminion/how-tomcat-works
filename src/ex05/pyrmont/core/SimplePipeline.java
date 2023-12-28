package ex05.pyrmont.core;

import java.io.IOException;
import javax.servlet.ServletException;
import org.apache.catalina.Contained;
import org.apache.catalina.Container;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.Valve;
import org.apache.catalina.ValveContext;

/**
 * 简单的管道类
 * 该类用于控制管道内阀的调用
 * 通过该类的invoke()方法，可以依次调用管道内的阀
 */
public class SimplePipeline implements Pipeline {

  public SimplePipeline(Container container) {
    setContainer(container);
  }

  // The basic Valve (if any) associated with this Pipeline.
  // 基础阀
  protected Valve basic = null;
  // The Container with which this Pipeline is associated.
  // 关联的容器
  protected Container container = null;
  // the array of Valves
  // 储存阀的数组（不包含基础阀）
  protected Valve valves[] = new Valve[0];


  public void setContainer(Container container) {
    this.container = container;
  }

  public Valve getBasic() {
    return basic;
  }

  public void setBasic(Valve valve) {
    this.basic = valve;
    ((Contained) valve).setContainer(container);
  }

  public void addValve(Valve valve) {
    if (valve instanceof Contained)
      ((Contained) valve).setContainer(this.container);

    synchronized (valves) {
      Valve results[] = new Valve[valves.length +1];
      System.arraycopy(valves, 0, results, 0, valves.length);
      results[valves.length] = valve;
      valves = results;
    }
  }

  public Valve[] getValves() {
    return valves;
  }

  /**
   * 调用管道中的阀依次处理任务
   */
  public void invoke(Request request, Response response)
    throws IOException, ServletException {
    // Invoke the first Valve in this pipeline for this request
    // 调用管道中的第一个阀的invokeNext()方法
    // 新创建的内部类SimplePipelineValveContext，有个默认指针stage，初始化为0，指向本类中values[]数组中的第一个元素
    // 之后内部类SimplePipelineValveContext会将自身的实例对象传递给values[]数组的下标指定元素，调用其invoke()方法
    // 此处SimplePipelineValveContext类调用的invoke()方法是实现了value接口的类实例对象的invoke()方法，需要3个参数，不是Pipeline的invoke()方法
    (new SimplePipelineValveContext()).invokeNext(request, response);
  }

  public void removeValve(Valve valve) {
  }

  /**
   * 实现了ValueContext接口的内部类，用于控制阀的依次调用
   * 从 org.apache.catalina.core.StandardPipeline 类的 StandardPipelineValveContext 内部类复制而来。
   *
   */
  protected class SimplePipelineValveContext implements ValveContext {

    protected int stage = 0;

    public String getInfo() {
      return null;
    }

    public void invokeNext(Request request, Response response)
      throws IOException, ServletException {
      // 使用subscript标注当前调用的阀下标
      int subscript = stage;
      // 标注下一个调用的阀的下标
      stage = stage + 1;
      // Invoke the requested Valve for the current request thread

      // 若阀的下标小于储存阀数组长度，调用当前下标的阀
      // 该invoke()方是调用的StandardPipeline的invoke()方法：
      // (new StandardPipelineValveContext()).invokeNext(request, response)
      // 当第一次调用时，stage初始化为为0;subscript=0,
      // 调用后，stage会指向1，此时将自身实例传递给下一个阀的invoke()方法调用
      // (该invoke方法为Value接口的方法，需要传递一个ValveContext对象，用于区分上一个调用的阀)
      // 当下标在指向下标在储存阀的数组中存在时，调用，
      // 当下标等于数组长度时调用基础阀，（因每次调用，实际下标都会+1，因此基础阀在下标等于长度时调用）
      // 否则抛出异常
      if (subscript < valves.length) {
        // 当前调用阀长度等于数组长度，说明阀已调用完，此时若有基础阀，调用基础阀
        valves[subscript].invoke(request, response, this);
      }
      else if ((subscript == valves.length) && (basic != null)) {
        // 当前调用阀长度等于数组长度，说明阀已调用完，此时若有基础阀，调用基础阀
        basic.invoke(request, response, this);
      }
      else {
        // 下标不正确无法确定指定阀，抛出异常
        throw new ServletException("No valve");
      }
    }
  } // end of inner class

}