/* modify this to include authenticatorConfig method */
package ex10.pyrmont.core;

import org.apache.catalina.Authenticator;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Valve;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.deploy.LoginConfig;

/**
 * 配置Context上下文
 * 相比ex09
 * 添加了验证器配置的流程：authenticatorConfig()
 */
public class SimpleContextConfig implements LifecycleListener {

  private Context context;
  public void lifecycleEvent(LifecycleEvent event) {
    if (Lifecycle.START_EVENT.equals(event.getType())) {
      context = (Context) event.getLifecycle();
      authenticatorConfig();
      context.setConfigured(true);
    }
  }

  private synchronized void authenticatorConfig() {
    // 检查此上下文是否需要身份验证器
    SecurityConstraint constraints[] = context.findConstraints();
    if ((constraints == null) || (constraints.length == 0))
      return;
    // 获取与该上下文关联的登录配置，若没有配置，则创建并设置
    LoginConfig loginConfig = context.getLoginConfig();
    if (loginConfig == null) {
      loginConfig = new LoginConfig("NONE", null, null, null);
      context.setLoginConfig(loginConfig);
    }

    // 检查是否已经配置了验证器
    Pipeline pipeline = ((StandardContext) context).getPipeline();
    if (pipeline != null) {
      Valve basic = pipeline.getBasic();
      if ((basic != null) && (basic instanceof Authenticator))
        return;
      Valve valves[] = pipeline.getValves();
      for (int i = 0; i < valves.length; i++) {
        if (valves[i] instanceof Authenticator)
        return;
      }
    }
    else {
      // 没有管道，无法安装验证器阀则返回
      return;
    }

    // 检查上下文是否已经配置了一个领域来进行身份验证
    if (context.getRealm() == null) {
      return;
    }

    // 确定配置的阀的类名，此处使用默认的BasicAuthenticator
    String authenticatorName = "org.apache.catalina.authenticator.BasicAuthenticator";
    // 通过反射实例化 并安装所请求类的验证器
    Valve authenticator = null;
    try {
      // 通过反射创建验证器
      Class authenticatorClass = Class.forName(authenticatorName);
      authenticator = (Valve) authenticatorClass.newInstance();
      // 作为阀添加到上下文中
      ((StandardContext) context).addValve(authenticator);
      System.out.println("Added authenticator valve to Context");
    }
    catch (Throwable t) {
    }
  }
}