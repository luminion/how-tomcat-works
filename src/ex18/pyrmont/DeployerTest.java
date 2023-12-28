package ex18.pyrmont;

/**
 * 部署器
 * 要使用一个web应用程序，必须要将表示该应用程序的Context实例部署到一个Host实例中
 *
 * org.apache.catalina.Deployer接口
 *      该接口的实例即为部署器，部署器与一个Host实例相关联，用来安装Context实例（创建Context实例并将其添加到Host中）
 *      StandardHost类实现了该接口，
 *          所以StandardHost实例是一个部署器，也是一个容器，web应用可以部署到其中，或从其中取消部署
 *          StandardHost使用StandardHostDeployer类来辅助部署web应用
 *
 *
 * org.apache.catalina.startup.HostConfig类
 *      该监听器负责将Context容器添加到Host容器中
 *          在之前章节部署的应用程序中，通过在bootstrap类创建并调用host的addChild()方法关联host和context
 *          实际上，tomcat中没有这样的操作，真实部署环境中，StandardHost会使用HostConfig类型的监听器进行添加和关联
 *          调用StandardHost的start()方法时会触发START_EVENT事件
 *          HostConfig会对此事件进行响应，并调用自身的start()方法，
 *              start()方法会逐个部署并安装Host容器指定目录中所有web应用程序(见HostConfig类具体方法)
 *       创建过程：
 *          Catalina使用Digester解析server.xml文件
 *          遇到符合Server/Service/Engine/Host模式的标签时
 *          会创建一个HostConfig类的实例，将其添加到Host实例中，作为生命周期监听器
 *
 * org.apache.catalina.startup.HostRuleSet类
 *      该类是RuleSetBase类的子类，该类必须提供addRuleInstances()方法的实现，用于指定解析Host的规则
 *
 *
 * org.apache.catalina.core.StandardHostDeployer类
 *      StandardHost类使用该类来辅助将Context应用程序部署到Host容器中
 *
 *
 *
 *
 *
 *
 * @author: booty
 * @date: 2021-11-17 17:02
 **/
public class DeployerTest {

}
