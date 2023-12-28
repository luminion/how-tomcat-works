/*
 * $Header: /home/cvs/jakarta-tomcat-4.0/catalina/src/share/org/apache/catalina/startup/Catalina.java,v 1.48 2002/05/23 17:22:37 glenn Exp $
 * $Revision: 1.48 $
 * $Date: 2002/05/23 17:22:37 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * [Additional notices, if required by prior licensing conditions]
 *
 */


package org.apache.catalina.startup;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.security.Security;
import org.apache.catalina.Container;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Server;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.tomcat.util.log.SystemLogHandler;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;


/**
 * Startup/Shutdown shell program for Catalina.  The following command line
 * options are recognized:
 * <ul>
 * <li><b>-config {pathname}</b> - Set the pathname of the configuration file
 *     to be processed.  If a relative path is specified, it will be
 *     interpreted as relative to the directory pathname specified by the
 *     "catalina.base" system property.   [conf/server.xml]
 * <li><b>-help</b> - Display usage information.
 * <li><b>-stop</b> - Stop the currently running instance of Catalina.
 * </u>
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.48 $ $Date: 2002/05/23 17:22:37 $
 */

public class Catalina {


    // ----------------------------------------------------- Instance Variables


    /**
     * Pathname to the server configuration file.
     */
    protected String configFile = "conf/server.xml";


    /**
     * Set the debugging detail level on our Digester.
     */
    protected boolean debug = false;


    /**
     * The shared extensions class loader for this server.
     */
    protected ClassLoader parentClassLoader =
        ClassLoader.getSystemClassLoader();


    /**
     * The server component we are starting or stopping
     */
    protected Server server = null;


    /**
     * Are we starting a new server?
     */
    protected boolean starting = false;


    /**
     * Are we stopping an existing server?
     */
    protected boolean stopping = false;


    /**
     * Are we using naming ?
     */
    protected boolean useNaming = true;


    // ----------------------------------------------------------- Main Program


    /**
     * The application main program.
     *
     * @param args Command line arguments
     */
    public static void main(String args[]) {

        (new Catalina()).process(args);
    }


    /**
     * The instance main program.
     *
     * @param args Command line arguments
     */
    public void process(String args[]) {
        // 如果尚未设置catalina.home系统属性，将其设置为user.dir的值
        setCatalinaHome();
        // 如果尚未设置catalina.base系统属性，将其设置为catalina.home的值
        setCatalinaBase();
        try {
            // 处理指定的命令行参数，如果要继续处理则返回true ； 否则返回false
            if (arguments(args))
                // 检测通过后，执行程序(开启或关闭程序)
                execute();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }


    // --------------------------------------------------------- Public Methods


    /**
     * Set the shared extensions class loader.
     *
     * @param parentClassLoader The shared extensions class loader.
     */
    public void setParentClassLoader(ClassLoader parentClassLoader) {

        this.parentClassLoader = parentClassLoader;

    }


    /**
     * Set the server instance we are configuring.
     *
     * @param server The new server
     */
    public void setServer(Server server) {

        this.server = server;

    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Process the specified command line arguments, and return
     * <code>true</code> if we should continue processing; otherwise
     * return <code>false</code>.
     *
     * @param args Command line arguments to process
     */
    protected boolean arguments(String args[]) {
        // 标记参数是否为配置参数
        boolean isConfig = false;

        if (args.length < 1) {
            // 若未设置参数，打印此应用程序的使用信息并返回(结束运行)
            usage();
            return (false);
        }

        for (int i = 0; i < args.length; i++) {
            if (isConfig) {
                // 若为指定配置文件，将configFile设置为当前参数的值(原为conf/server.xml)
                configFile = args[i];
                // 标记为未配置
                isConfig = false;
            } else if (args[i].equals("-config")) {
                // 检测到-config指令，标记isConfig参数为true(下个参数是指定配置文件)
                isConfig = true;
            } else if (args[i].equals("-debug")) {
                // 检测到-debug指令，标记debug为true
                debug = true;
            } else if (args[i].equals("-nonaming")) {
                // 检测到-nonaming指令，标记useNaming为false(不对JDNI命名提供支持)
                useNaming = false;
            } else if (args[i].equals("-help")) {
                // 检测到-help指令 输出程序信息并直接返回false，不再检测之后的指令
                usage();
                return (false);
            } else if (args[i].equals("start")) {
                // 检查到start指定,标记starting为true
                starting = true;
            } else if (args[i].equals("stop")) {
                // 检查到stop参数，标记stopping为true
                stopping = true;
            } else {
                // 不支持的参数,直接打印信息并直接返回false，不再检测之后的指令
                usage();
                return (false);
            }
        }
        // 参数检测完成 返回true
        return (true);

    }


    /**
     * Return a File object representing our configuration file.
     */
    protected File configFile() {

        File file = new File(configFile);
        if (!file.isAbsolute())
            file = new File(System.getProperty("catalina.base"), configFile);
        return (file);

    }


    /**
     * Create and configure the Digester we will be using for startup.
     */
    protected Digester createStartDigester() {

        // Initialize the digester
        Digester digester = new Digester();
        if (debug)
            digester.setDebug(999);
        digester.setValidating(false);

        // Configure the actions we will be using
        // 创建Server对象
        digester.addObjectCreate("Server",
                                 "org.apache.catalina.core.StandardServer",
                                 "className");
        digester.addSetProperties("Server");
        digester.addSetNext("Server",
                            "setServer",
                            "org.apache.catalina.Server");

        // 创建Server对象的属性

        // 命名属性
        digester.addObjectCreate("Server/GlobalNamingResources",
                                 "org.apache.catalina.deploy.NamingResources");
        digester.addSetProperties("Server/GlobalNamingResources");
        digester.addSetNext("Server/GlobalNamingResources",
                            "setGlobalNamingResources",
                            "org.apache.catalina.deploy.NamingResources");
        // Server的Listener监听器
        digester.addObjectCreate("Server/Listener",
                                 null, // MUST be specified in the element
                                 "className");
        digester.addSetProperties("Server/Listener");
        digester.addSetNext("Server/Listener",
                            "addLifecycleListener",
                            "org.apache.catalina.LifecycleListener");

        // Server的Service组件
        digester.addObjectCreate("Server/Service",
                                 "org.apache.catalina.core.StandardService",
                                 "className");
        digester.addSetProperties("Server/Service");
        digester.addSetNext("Server/Service",
                            "addService",
                            "org.apache.catalina.Service");

        // Service组件的Listener监听器
        digester.addObjectCreate("Server/Service/Listener",
                                 null, // MUST be specified in the element
                                 "className");
        digester.addSetProperties("Server/Service/Listener");
        digester.addSetNext("Server/Service/Listener",
                            "addLifecycleListener",
                            "org.apache.catalina.LifecycleListener");

        // Service组件的Container连接器
        digester.addObjectCreate("Server/Service/Connector",
                                 "org.apache.catalina.connector.http.HttpConnector",
                                 "className");
        digester.addSetProperties("Server/Service/Connector");
        digester.addSetNext("Server/Service/Connector",
                            "addConnector",
                            "org.apache.catalina.Connector");

        // Service组件的Container连接器的Factory创建工厂
        digester.addObjectCreate("Server/Service/Connector/Factory",
                                 "org.apache.catalina.net.DefaultServerSocketFactory",
                                 "className");
        digester.addSetProperties("Server/Service/Connector/Factory");
        digester.addSetNext("Server/Service/Connector/Factory",
                            "setFactory",
                            "org.apache.catalina.net.ServerSocketFactory");

        // Service组件的Container连接器的Listener监听器
        digester.addObjectCreate("Server/Service/Connector/Listener",
                                 null, // MUST be specified in the element
                                 "className");
        digester.addSetProperties("Server/Service/Connector/Listener");
        digester.addSetNext("Server/Service/Connector/Listener",
                            "addLifecycleListener",
                            "org.apache.catalina.LifecycleListener");

        // Add RuleSets for nested elements
        // 关联对象之间的关系
        digester.addRuleSet(new NamingRuleSet("Server/GlobalNamingResources/"));
        digester.addRuleSet(new EngineRuleSet("Server/Service/"));
        digester.addRuleSet(new HostRuleSet("Server/Service/Engine/"));
        digester.addRuleSet(new ContextRuleSet("Server/Service/Engine/Default"));
        digester.addRuleSet(new NamingRuleSet("Server/Service/Engine/DefaultContext/"));
        digester.addRuleSet(new ContextRuleSet("Server/Service/Engine/Host/Default"));
        digester.addRuleSet(new NamingRuleSet("Server/Service/Engine/Host/DefaultContext/"));
        digester.addRuleSet(new ContextRuleSet("Server/Service/Engine/Host/"));
        digester.addRuleSet(new NamingRuleSet("Server/Service/Engine/Host/Context/"));

        digester.addRule("Server/Service/Engine",
                         new SetParentClassLoaderRule(digester,
                                                      parentClassLoader));


        return (digester);

    }


    /**
     * Create and configure the Digester we will be using for shutdown.
     */
    protected Digester createStopDigester() {

        // Initialize the digester
        Digester digester = new Digester();
        if (debug)
            digester.setDebug(999);

        // Configure the rules we need for shutting down
        // 读取需要关闭的规则
        digester.addObjectCreate("Server",
                                 "org.apache.catalina.core.StandardServer",
                                 "className");
        digester.addSetProperties("Server");
        digester.addSetNext("Server",
                            "setServer",
                            "org.apache.catalina.Server");

        return (digester);

    }


    /**
     * Execute the processing that has been configured from the command line.
     */
    protected void execute() throws Exception {
        // 检查starting标记和stopping标记
        if (starting)
            start();
        else if (stopping)
            stop();

    }


    /**
     * Set the <code>catalina.base</code> System property to the current
     * working directory if it has not been set.
     */
    protected void setCatalinaBase() {
        // 如果尚未设置catalina.base系统属性，将其设置为catalina.home的值
        if (System.getProperty("catalina.base") != null)
            return;
        System.setProperty("catalina.base",
                           System.getProperty("catalina.home"));

    }


    /**
     * Set the <code>catalina.home</code> System property to the current
     * working directory if it has not been set.
     */
    protected void setCatalinaHome() {
        // 获取catalina.home，若为空，则将其设置为user.dir的值
        if (System.getProperty("catalina.home") != null)
            return;
        System.setProperty("catalina.home",
                           System.getProperty("user.dir"));

    }


    /**
     * Start a new server instance.
     */
    protected void start() {


        // Create and execute our Digester
        // 创建xml文档解析器
        Digester digester = createStartDigester();
        // 获取配置文件
        File file = configFile();
        try {
            InputSource is =
                new InputSource("file://" + file.getAbsolutePath());
            FileInputStream fis = new FileInputStream(file);
            is.setByteStream(fis);
            // 将当前对象推入Digester栈中
            digester.push(this);
            // 利用读取到的数据来为当前对象的属性赋值(Server\Service\Connector\Listener等，见createStartDigester()方法内读取的数据)
            digester.parse(is);
            fis.close();
        } catch (Exception e) {
            System.out.println("Catalina.start: " + e);
            e.printStackTrace(System.out);
            System.exit(1);
        }

        // Setting additional variables
        // 设置变量
        if (!useNaming) {
            System.setProperty("catalina.useNaming", "false");
        } else {
            System.setProperty("catalina.useNaming", "true");
            String value = "org.apache.naming";
            String oldValue =
                System.getProperty(javax.naming.Context.URL_PKG_PREFIXES);
            if (oldValue != null) {
                value = value + ":" + oldValue;
            }
            System.setProperty(javax.naming.Context.URL_PKG_PREFIXES, value);
            value = System.getProperty
                (javax.naming.Context.INITIAL_CONTEXT_FACTORY);
            if (value == null) {
                System.setProperty
                    (javax.naming.Context.INITIAL_CONTEXT_FACTORY,
                     "org.apache.naming.java.javaURLContextFactory");
            }
        }

        // If a SecurityManager is being used, set properties for
        // checkPackageAccess() and checkPackageDefinition
        // 设置安全管理器
        if( System.getSecurityManager() != null ) {
            String access = Security.getProperty("package.access");
            if( access != null && access.length() > 0 )
                access += ",";
            else
                access = "sun.,";
            Security.setProperty("package.access",
                access + "org.apache.catalina.,org.apache.jasper.");
            String definition = Security.getProperty("package.definition");
            if( definition != null && definition.length() > 0 )
                definition += ",";
            else
                definition = "sun.,";
            Security.setProperty("package.definition",
                // FIX ME package "javax." was removed to prevent HotSpot
                // fatal internal errors
                definition + "java.,org.apache.catalina.,org.apache.jasper.");
        }

        // Replace System.out and System.err with a custom PrintStream
        // 设置系统的标准打印格式和错误格式
        SystemLogHandler log = new SystemLogHandler(System.out);
        System.setOut(log);
        System.setErr(log);

        // 实例化关闭的钩子
        Thread shutdownHook = new CatalinaShutdownHook();

        // Start the new server
        // 启动服务器
        if (server instanceof Lifecycle) {
            try {
                // 初始化
                server.initialize();
                // 启动
                ((Lifecycle) server).start();
                try {
                    // Register shutdown hook
                    // 注册关闭钩子，确保关闭时关闭相关组件
                    Runtime.getRuntime().addShutdownHook(shutdownHook);
                } catch (Throwable t) {
                    // This will fail on JDK 1.2. Ignoring, as Tomcat can run
                    // fine without the shutdown hook.
                }
                // Wait for the server to be told to shut down
                // 一直循环监听关闭指令(若监听到关闭指定会从此行继续往下执行)
                server.await();
            } catch (LifecycleException e) {
                System.out.println("Catalina.start: " + e);
                e.printStackTrace(System.out);
                if (e.getThrowable() != null) {
                    System.out.println("----- Root Cause -----");
                    e.getThrowable().printStackTrace(System.out);
                }
            }
        }

        // Shut down the server
        // 关闭服务器
        if (server instanceof Lifecycle) {
            try {
                try {
                    // Remove the ShutdownHook first so that server.stop()
                    // doesn't get invoked twice
                    // 正常关闭时，先移除关闭的钩子，防止二次关闭
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                } catch (Throwable t) {
                    // This will fail on JDK 1.2. Ignoring, as Tomcat can run
                    // fine without the shutdown hook.
                }
                // 调用关闭方法
                ((Lifecycle) server).stop();
            } catch (LifecycleException e) {
                System.out.println("Catalina.stop: " + e);
                e.printStackTrace(System.out);
                if (e.getThrowable() != null) {
                    System.out.println("----- Root Cause -----");
                    e.getThrowable().printStackTrace(System.out);
                }
            }
        }

    }


    /**
     * Stop an existing server instance.
     */
    protected void stop() {

        // Create and execute our Digester
        // 创建关闭的xml解析器，该解析器仅对根元素进行解析，也就是server
        Digester digester = createStopDigester();
        File file = configFile();
        try {
            InputSource is =
                new InputSource("file://" + file.getAbsolutePath());
            FileInputStream fis = new FileInputStream(file);
            is.setByteStream(fis);
            // 将当前对象压入Digester对象的内部栈中
            digester.push(this);
            // 将当前对象的server属性转为读取到的server属性
            digester.parse(is);
            fis.close();
        } catch (Exception e) {
            System.out.println("Catalina.stop: " + e);
            e.printStackTrace(System.out);
            System.exit(1);
        }

      // Stop the existing server
      // 关闭正在运行的服务器(向其发送关闭指令)
      try {
          Socket socket = new Socket("127.0.0.1", server.getPort());
          OutputStream stream = socket.getOutputStream();
          String shutdown = server.getShutdown();
          for (int i = 0; i < shutdown.length(); i++)
              stream.write(shutdown.charAt(i));
          stream.flush();
          stream.close();
          socket.close();
      } catch (IOException e) {
          System.out.println("Catalina.stop: " + e);
          e.printStackTrace(System.out);
          System.exit(1);
      }


    }


    /**
     * Print usage information for this application.
     */
    protected void usage() {
        // 打印当前系统信息
        System.out.println
            ("usage: java org.apache.catalina.startup.Catalina"
             + " [ -config {pathname} ] [ -debug ]"
             + " [ -nonaming ] { start | stop }");

    }


    // --------------------------------------- CatalinaShutdownHook Inner Class


    /**
     * Shutdown hook which will perform a clean shutdown of Catalina if needed.
     */
    protected class CatalinaShutdownHook extends Thread {
        // 关闭钩子，确保系统关闭时调用生命周期的关闭方法，关闭所有组件
        // 该类在Catalina的start()方法中被实例化，并注册
        public void run() {

            if (server != null) {
                try {
                    ((Lifecycle) server).stop();
                } catch (LifecycleException e) {
                    System.out.println("Catalina.stop: " + e);
                    e.printStackTrace(System.out);
                    if (e.getThrowable() != null) {
                        System.out.println("----- Root Cause -----");
                        e.getThrowable().printStackTrace(System.out);
                    }
                }
            }

        }
    }
}


// ------------------------------------------------------------ Private Classes


/**
 * Rule that sets the parent class loader for the top object on the stack,
 * which must be a <code>Container</code>.
 */

final class SetParentClassLoaderRule extends Rule {

    public SetParentClassLoaderRule(Digester digester,
                                    ClassLoader parentClassLoader) {

        super(digester);
        this.parentClassLoader = parentClassLoader;

    }

    ClassLoader parentClassLoader = null;

    public void begin(Attributes attributes) throws Exception {

        if (digester.getDebug() >= 1)
            digester.log("Setting parent class loader");

        Container top = (Container) digester.peek();
        top.setParentClassLoader(parentClassLoader);

    }


}
