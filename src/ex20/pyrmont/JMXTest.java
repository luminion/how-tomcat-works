package ex20.pyrmont;

/**
 * JMX
 *      全称Java Management Extensions,为一种管理规范
 *      相比与ContainerServlet接口，该规范提供了更为灵活的管理方式来管理tomcat
 *      许多机遇服务器的应用程序，都使用了JMX技术来管理各自的资源(如：tomcat,Jboss,JONAS,Geronimo等)
 *
 *
 *      若要使一个java对象成为一个可以由JMX管理的资源，则必须创建一个名为ManagedBean或Mbean的对象
 *          在org.apache.catalina.mbeans包下预定义了一些Mbean
 *          例如：ConnectorMBean,StandardContextMBean,StandardEngineMBean,StandardHostMBean,StandardServerMBean,StandardServiceMBean
 *
 *      MBean会提供它所管理的一些java对象的属性和方法供管理应用程序使用，
 *      管理应用程序本身并不能直接访问托管的java对象，
 *      因此，可以选择java对象的哪些属性和方法可以由管理程序使用
 *
 *      当拥有一个MBean时，需要将其实例化，并注册到另一个作为MBean服务器的java对象中
 *      MBean服务器保存了所有注册的MBean，管理应用程序可以通过MBean服务器来访问MBean实例
 *
 *      MBean分为4种类型，标准、动态、开放、模型
 *          其中标准类型最容易编写，但灵活性最低
 *          Catalina使用的是模型类型
 *
 *      从结构上讲，JMX规范分为3层：设备层、代理层、和分布式服务层
 *          MBean位于设备层
 *          MBean服务器位于代理层
 *          设备层规范定了编写可由JMX管理的资源标准，即如何编写MBean
 *          代理层定义了创建代理的规范，代理封装了MBean服务器，提供了处理Mbean的服务，代理机器所管理的MBean通常位于用一个java虚拟机中
 *
 * javax.management.MBeanServerFactory类
 *      该类的createMbean()方法可以创建一个MBeanServer接口的实例
 *
 * javax.management.MBeanServer接口
 *      定义了MBean的服务器
 *      registerMBean()方法
 *          将MBean注册到服务器中
 *          返回一个javax.management.ObjectInstance，该类封装了一个MBean实例的对象名称和它的类名
 *      queryNames()方法
 *          获取匹配的MBean的实例
 *          返回一个java.util.Set实例，其中包含了匹配某个模式对象名称的一组MBean实例的对象名称
 *      queryMBeans()方法
 *          获取匹配的MBean的名称
 *          返回一个java.util.Set实例，其中包含了匹配某个模式对象名称的一组MBean实例的对象名称
 *
 * javax.management.ObjectName类
 *      MBean中的每个bean实例都通过一个对象名称来唯一的标识，
 *      对象名称为该类的一个实例
 *      对象名称由两部分组成，域和一个键值对
 *          域是一个字符串，也可以是空串，域后接分好，然后是一个或多个键值对
 *          键值对中，键是一个费控字符串，且不能包含等号，逗号，分号，星号和问号，在一个对象名称中，同一个键只能出现一次
 *          键与值是由等号分割的，键值对之间用分号分割
 *
 * 创建标准MBean的过程
 *      创建一个接口，命名规范为：java类名+MBean后缀
 *      修改java类，让其实现创建的解耦
 *      创建一个代理，该代理类必须包含一个MBeanServer实例
 *      为MBean创建ObjetName实例
 *      实例化MBeanServer类
 *      将MBean注册到MBeanServer中
 *      (见standardmbeantest包)
 *
 * 模型MBean
 *      不再需要创建接口，使用javax.management.modelmbean.ModelMBean接口标识一个模型MBean
 *      javax.management.modelmbean.RequiredModelMBean类是ModelMBean接口的标准实现
 *      可以通过实例化RequiredModelMBean类来创建MBean
 *      编写一个模型MBean后，需要告诉ModelMBean对象托管资源的哪些属性和方法可以暴露给代理
 *      可以通过创建javax.management.modelmbean.ModelMBeanInfo类的对象来完成这个任务
 *      ModelMBeanInfo对象描述了将会暴露给代理的构造函数、属性、操作甚至监听器，创建该类后，将其与ModelMBean对象关联即可
 *      (见modelmbeantest1包)
 *      (通过Commons Modeler库使用xml文档指定暴露的属性见modelmbeantest2包)
 *
 * org.apache.catalina.mbeans.ServerLifecycleListener类
 *      catalina会在该监听器的createMBeans()方法创建MBean
 *
 *
 *
 *
 * @author: booty
 * @date: 2021-11-18 15:20
 **/
public class JMXTest {
}
