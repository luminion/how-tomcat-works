package ex19.pyrmont;

/**
 * Manager应用程序的servlet类
 *   tomcat4和5带了Manager应用程序，可以用来管理已经部署的web应用程序
 *   与其他的Manager应用程序不同，Manager应用程序并不在默认的应用程序部署目录%CATALINA_HOME%/webapps中
 *   而是在%CATALINA_HOME%/server/webapps目录中，
 *   Manager应用程序使用一个描述符文件manager.xml来完成部署
 *   在tomcat4中，该文件位于%CATALINA_HOME%/webapps目录中，
 *   在tomcat5中，该程序位于CATALINA_HOME%/server/webapps目录中,
 *   该描述符内添加了两个servlet，分别为ManagerServlet和HTMLManagerServlet
 *   在该描述符中有<security-constraint><security-constraint/>元素，限制了拥有manager角色的用户才能访问
 *   在tomcat中，用户和角色列表储存于tomcat-users.xml文件中，该文件位于%CATALINA_HOME%/conf下
 *   因此，要访问Manager,必须添加一个manager角色和一个拥有该角色的用户
 *
 * org.apache.catalina.ContainerServlet接口
 *      Catalina类实例会调用实现了ContainerServlet接口的servlet类的setWrapper()方法，
 *      将该引用传递给该servlet类的Wrapper类
 *      实现了该接口的Servlet类，可以实现对host容器下Context容器的管理
 *
 *
 * org.apache.catalina.servlets.ManagerServlet类
 *   实现了ContainerServlet接口，用于管理已部署的Context
 *
 *      初始化ManagerServlet
 *      通常来说，servlet对象由一个StandardWrapper表示，
 *      在第一次调用servlet实例时，
 *      会先调用StandWrapper对象的loadServlet()方法，
 *      在loadServlet()中，会判断载入的servlet是否属于ContainerServlet，
 *      若属于，则调用ContainerServlet的setWrapper()方法
 *          ContainerServlet的setWrapper()会从传入的wrapper处获取Context容器和Host容器(Deployer)
 *      然后StandWrapper调用init()方法初始化servlet对象（见第11章）
 *
 * org.apache.catalina.servlets.HTMLManagerServlet类
 *   实现了ContainerServlet接口，用于管理已部署的Context
 *
 *
 * @author: booty
 * @date: 2021-11-18 14:04
 **/
public class ManagerTest {


}
