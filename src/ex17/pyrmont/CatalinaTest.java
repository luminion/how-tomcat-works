package ex17.pyrmont;

import org.apache.catalina.startup.Catalina;

/**
 * org.apache.catalina.startup.Catalina类
 *      此类是tomcat的启动类
 *      包含一个Digester对象，用于解析位于%CATALINA_HOME%/conf下的server.xml文件
 *      该类封装了一个Server对象
 *          Server对象有一个Service对象
 *              Service对象包含一个Servlet容器和多个连接器
 *      可以使用该类来启动、关闭Server对象
 *
 *
 *
 *
 * @author: booty
 * @date: 2021-11-17 15:12
 **/
public class CatalinaTest {

    public static void main(String[] args) {
        // 通过实例化Catalina类,调用process方法来运行tomcat，但需要传入合适的参数 start 表示要启动
        Catalina catalina=new Catalina();
        catalina.process(new String[] {"start"});
    }
}
