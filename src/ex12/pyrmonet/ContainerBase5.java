package ex12.pyrmonet;

/**
 * @program: Test
 * @description: tomcat5的后台任务方法
 * @packagename: pyrmonet
 * @author: booty
 * @date: 2021-11-05 11:55
 *
 * Context容器运行需要其他组件的支持，比如载入器和session管理器，
 * 通常来说，这些组件需要使用各自的线程执行一些后台处理任务，
 * 例如，为了支持自动重载，载入器需要使用一个线程周期性地检查它所管理的session对象过期时间
 * 在tomcat4中，这些组件最终拥有各自的线程
 * 在tomcat5中，使用了不同的方法，所有的后台处理共享同一个线程，
 *             若某个组件或者servlet容器需要周期性的执行一个操作，
 *             只需要将代码写到其backgroundProcess()方法中即可
 *
 * 该共享线程在ContainerBase对象中创建，
 * ContainerBase在其start()方法中，调用threadStart()方法启动该后台线程
 * threadStart()方法需要传入一个实现runnable接口的ContainerBackgroundProcessor类的实例构造一个新线程
 *      ContainerBackgroundProcessor是ContainerBase的一个内部类：protected class ContainerBackgroundProcessor implements Runnable
 *          该类的run方法中是一个while循环，周期性地调用其自身的processChildren()方法，
 *          该类在其自身的processChildren()方法中，
 *          会调用当前关联容器对象(实际也就是StandardContext)的backgroundProcess方法
 *          调用后，
 *          会获取与其关联容器的所有子容器，并依次调用子容器的backgroundProcess()方法
 *
 *          以下为StandardContext和ContainerBase的backgroundProcess()方法
 *                  StandardContext的backgroundProcess()方法
 *                      依次获取关联的Loader、Manager、WebResourceRoot、InstanceManager对象，并调用这些对象的backgroundProcess方法()
 *                      之后调用父类(ContainerBase)的backgroundProcess方法
 *                  ContainerBase的backgroundProcess()方法
 *                      依次获取关联的Cluster、Realm、管道中的阀 ，并调用这些对象的backgroundProcess()方法
 *                      调用完成后向感兴趣的组件发送事件通知(生命周期)
 *
 * 通过实现backgroundProcess()方法，
 * ContainerBase类的子类可以使用一个专用线程来执行周期性任务
 * 例如检查类的时间戳或者检查session对象的超时时间
 *
 *
 *
 **/
public class ContainerBase5  {



}
