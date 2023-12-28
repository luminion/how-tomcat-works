/*
 * $Header: /home/cvs/jakarta-tomcat-4.0/catalina/src/share/org/apache/catalina/startup/HostConfig.java,v 1.23 2002/05/30 22:12:28 remm Exp $
 * $Revision: 1.23 $
 * $Date: 2002/05/30 22:12:28 $
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


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import org.apache.naming.resources.ResourceAttributes;
import org.apache.catalina.Context;
import org.apache.catalina.Deployer;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Logger;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.util.StringManager;


/**
 * Startup event listener for a <b>Host</b> that configures the properties
 * of that Host, and the associated defined contexts.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @version $Revision: 1.23 $ $Date: 2002/05/30 22:12:28 $
 */

public class HostConfig
    implements LifecycleListener, Runnable {


    // ----------------------------------------------------- Instance Variables


    /**
     * The Java class name of the Context configuration class we should use.
     */
    protected String configClass = "org.apache.catalina.startup.ContextConfig";


    /**
     * The Java class name of the Context implementation we should use.
     */
    protected String contextClass = "org.apache.catalina.core.StandardContext";


    /**
     * The debugging detail level for this component.
     */
    protected int debug = 0;


    /**
     * The names of applications that we have auto-deployed (to avoid
     * double deployment attempts).
     */
    protected ArrayList deployed = new ArrayList();


    /**
     * The Host we are associated with.
     */
    protected Host host = null;


    /**
     * The string resources for this package.
     */
    protected static final StringManager sm =
        StringManager.getManager(Constants.Package);


    /**
     * The number of seconds between checks for web app deployment.
     */
    private int checkInterval = 15;


    /**
     * Should we deploy XML Context config files?
     */
    private boolean deployXML = false;


    /**
     * Should we monitor the <code>appBase</code> directory for new
     * applications and automatically deploy them?
     */
    private boolean liveDeploy = false;


    /**
     * The background thread.
     */
    private Thread thread = null;


    /**
     * The background thread completion semaphore.
     */
    private boolean threadDone = false;


    /**
     * Name to register for the background thread.
     */
    private String threadName = "HostConfig";


    /**
     * Should we unpack WAR files when auto-deploying applications in the
     * <code>appBase</code> directory?
     */
    private boolean unpackWARs = false;


    /**
     * Last modified dates of the web.xml files of the contexts, keyed by
     * context name.
     */
    private HashMap webXmlLastModified = new HashMap();


    // ------------------------------------------------------------- Properties


    /**
     * Return the Context configuration class name.
     */
    public String getConfigClass() {

        return (this.configClass);

    }


    /**
     * Set the Context configuration class name.
     *
     * @param configClass The new Context configuration class name.
     */
    public void setConfigClass(String configClass) {

        this.configClass = configClass;

    }


    /**
     * Return the Context implementation class name.
     */
    public String getContextClass() {

        return (this.contextClass);

    }


    /**
     * Set the Context implementation class name.
     *
     * @param contextClass The new Context implementation class name.
     */
    public void setContextClass(String contextClass) {

        this.contextClass = contextClass;

    }


    /**
     * Return the debugging detail level for this component.
     */
    public int getDebug() {

        return (this.debug);

    }


    /**
     * Set the debugging detail level for this component.
     *
     * @param debug The new debugging detail level
     */
    public void setDebug(int debug) {

        this.debug = debug;

    }


    /**
     * Return the deploy XML config file flag for this component.
     */
    public boolean isDeployXML() {

        return (this.deployXML);

    }


    /**
     * Set the deploy XML config file flag for this component.
     *
     * @param deployXML The new deploy XML flag
     */
    public void setDeployXML(boolean deployXML) {

        this.deployXML= deployXML;

    }


    /**
     * Return the live deploy flag for this component.
     */
    public boolean isLiveDeploy() {

        return (this.liveDeploy);

    }


    /**
     * Set the live deploy flag for this component.
     *
     * @param liveDeploy The new live deploy flag
     */
    public void setLiveDeploy(boolean liveDeploy) {

        this.liveDeploy = liveDeploy;

    }


    /**
     * Return the unpack WARs flag.
     */
    public boolean isUnpackWARs() {

        return (this.unpackWARs);

    }


    /**
     * Set the unpack WARs flag.
     *
     * @param unpackWARs The new unpack WARs flag
     */
    public void setUnpackWARs(boolean unpackWARs) {

        this.unpackWARs = unpackWARs;

    }


    // --------------------------------------------------------- Public Methods


    /**
     * Process the START event for an associated Host.
     *
     * @param event The lifecycle event that has occurred
     */
    public void lifecycleEvent(LifecycleEvent event) {

        // Identify the host we are associated with
        try {
            host = (Host) event.getLifecycle();
            if (host instanceof StandardHost) {
                int hostDebug = ((StandardHost) host).getDebug();
                if (hostDebug > this.debug) {
                    this.debug = hostDebug;
                }
                // 获取host容器的属性
                setDeployXML(((StandardHost) host).isDeployXML());
                setLiveDeploy(((StandardHost) host).getLiveDeploy());
                setUnpackWARs(((StandardHost) host).isUnpackWARs());
            }
        } catch (ClassCastException e) {
            log(sm.getString("hostConfig.cce", event.getLifecycle()), e);
            return;
        }

        // Process the event that has occurred
        if (event.getType().equals(Lifecycle.START_EVENT))
            // START_EVENT事件调用start方法
            start();
        else if (event.getType().equals(Lifecycle.STOP_EVENT))
            // STOP_EVENT调用stop()方法
            stop();

    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Return a File object representing the "application root" directory
     * for our associated Host.
     */
    protected File appBase() {

        File file = new File(host.getAppBase());
        if (!file.isAbsolute())
            file = new File(System.getProperty("catalina.base"),
                            host.getAppBase());
        return (file);

    }


    /**
     * Deploy applications for any directories or WAR files that are found
     * in our "application root" directory.
     */
    protected void deployApps() {

        if (!(host instanceof Deployer))
            return;
        if (debug >= 1)
            log(sm.getString("hostConfig.deploying"));

        // 获取应用程序的根目录
        File appBase = appBase();
        if (!appBase.exists() || !appBase.isDirectory())
            return;
        String files[] = appBase.list();

        // 按照描述符、war包、文件夹的方式进行部署
        // 此处的描述符对应该项目webapp下的manager.xml和myadmin.xml
        // 打开xml文件可知，每个描述符都有一个Context元素，并指定了Context的docBase属性
        // docBase代表资源所在路径，Context指定后，不会部署到默认目录，会部署到docBase指定的目录
        // tomcat4中，这些文件位于%CATALINA_HOME%/webapp目录下，tomcat5中位于%CATALINA_HOME%/server/webapps/目录的子目录下

        // 优先部署程序的描述符
        deployDescriptors(appBase, files);
        // 部署war包
        deployWARs(appBase, files);
        // 部署目录
        deployDirectories(appBase, files);

    }


    /**
     * Deploy XML context descriptors.
     */
    protected void deployDescriptors(File appBase, String[] files) {

        if (!deployXML)
           return;

        for (int i = 0; i < files.length; i++) {

            if (files[i].equalsIgnoreCase("META-INF"))
                continue;
            if (files[i].equalsIgnoreCase("WEB-INF"))
                continue;
            if (deployed.contains(files[i]))
                continue;
            File dir = new File(appBase, files[i]);
            if (files[i].toLowerCase().endsWith(".xml")) {

                deployed.add(files[i]);

                // Calculate the context path and make sure it is unique
                String file = files[i].substring(0, files[i].length() - 4);
                String contextPath = "/" + file;
                if (file.equals("ROOT")) {
                    contextPath = "";
                }
                if (host.findChild(contextPath) != null) {
                    continue;
                }

                // Assume this is a configuration descriptor and deploy it
                log(sm.getString("hostConfig.deployDescriptor", files[i]));
                try {
                    URL config =
                        new URL("file", null, dir.getCanonicalPath());
                    ((Deployer) host).install(config, null);
                } catch (Throwable t) {
                    log(sm.getString("hostConfig.deployDescriptor.error",
                                     files[i]), t);
                }

            }

        }

    }


    /**
     * Deploy WAR files.
     */
    protected void deployWARs(File appBase, String[] files) {

        for (int i = 0; i < files.length; i++) {

            if (files[i].equalsIgnoreCase("META-INF"))
                continue;
            if (files[i].equalsIgnoreCase("WEB-INF"))
                continue;
            if (deployed.contains(files[i]))
                continue;
            File dir = new File(appBase, files[i]);
            if (files[i].toLowerCase().endsWith(".war")) {

                deployed.add(files[i]);

                // Calculate the context path and make sure it is unique
                // 检查上下文名称是否重复，避免重复部署
                String contextPath = "/" + files[i];
                int period = contextPath.lastIndexOf(".");
                if (period >= 0)
                    contextPath = contextPath.substring(0, period);
                if (contextPath.equals("/ROOT"))
                    contextPath = "";
                if (host.findChild(contextPath) != null)
                    continue;

                if (isUnpackWARs()) {

                    // Expand and deploy this application as a directory
                    log(sm.getString("hostConfig.expand", files[i]));
                    try {
                        URL url = new URL("jar:file:" +
                                          dir.getCanonicalPath() + "!/");
                        // 解压文件，并获取解压目录
                        String path = expand(url);
                        url = new URL("file:" + path);
                        // 部署目录，指定Context路径(既上下文名称、访问路径) 和文件url
                        ((Deployer) host).install(contextPath, url);
                    } catch (Throwable t) {
                        log(sm.getString("hostConfig.expand.error", files[i]),
                            t);
                    }

                } else {

                    // Deploy the application in this WAR file
                    log(sm.getString("hostConfig.deployJar", files[i]));
                    try {
                        URL url = new URL("file", null,
                                          dir.getCanonicalPath());
                        // 部署jar包
                        url = new URL("jar:" + url.toString() + "!/");
                        ((Deployer) host).install(contextPath, url);
                    } catch (Throwable t) {
                        log(sm.getString("hostConfig.deployJar.error",
                                         files[i]), t);
                    }

                }

            }

        }

    }


    /**
     * Deploy directories.
     */
    protected void deployDirectories(File appBase, String[] files) {

        for (int i = 0; i < files.length; i++) {

            if (files[i].equalsIgnoreCase("META-INF"))
                continue;
            if (files[i].equalsIgnoreCase("WEB-INF"))
                continue;
            if (deployed.contains(files[i]))
                continue;
            File dir = new File(appBase, files[i]);
            if (dir.isDirectory()) {

                deployed.add(files[i]);

                // Make sure there is an application configuration directory
                // This is needed if the Context appBase is the same as the
                // web server document root to make sure only web applications
                // are deployed and not directories for web space.
                File webInf = new File(dir, "/WEB-INF");
                if (!webInf.exists() || !webInf.isDirectory() ||
                    !webInf.canRead())
                    continue;

                // Calculate the context path and make sure it is unique
                String contextPath = "/" + files[i];
                if (files[i].equals("ROOT"))
                    contextPath = "";
                if (host.findChild(contextPath) != null)
                    continue;

                // Deploy the application in this directory
                log(sm.getString("hostConfig.deployDir", files[i]));
                try {
                    URL url = new URL("file", null, dir.getCanonicalPath());
                    ((Deployer) host).install(contextPath, url);
                } catch (Throwable t) {
                    log(sm.getString("hostConfig.deployDir.error", files[i]),
                        t);
                }

            }

        }

    }


    /**
     * Check deployment descriptors last modified date.
     */
    protected void checkWebXmlLastModified() {

        if (!(host instanceof Deployer))
            return;

        Deployer deployer = (Deployer) host;
        // 寻找已部署的应用程序
        String[] contextNames = deployer.findDeployedApps();

        for (int i = 0; i < contextNames.length; i++) {

            String contextName = contextNames[i];
            Context context = deployer.findDeployedApp(contextName);

            if (!(context instanceof Lifecycle))
                continue;

            try {
                DirContext resources = context.getResources();
                if (resources == null) {
                    // This can happen if there was an error initializing
                    // the context
                    continue;
                }
                // 检查Context的配置文件web.xml是否被修改过
                // (Context内部的资源由Context容器负责检查更新，此处不做处理)
                ResourceAttributes webXmlAttributes = 
                    (ResourceAttributes) 
                    resources.getAttributes("/WEB-INF/web.xml");
                long newLastModified = webXmlAttributes.getLastModified();
                // 从webXmlLastModified中获取该配置文件的上次修改时间
                Long lastModified = (Long) webXmlLastModified.get(contextName);
                if (lastModified == null) {
                    // 若webXmlLastModified中无该配置文件修改时间，将其添加到webXmlLastModified中
                    webXmlLastModified.put
                        (contextName, new Long(newLastModified));
                } else {
                    // 若webXmlLastModified中有该配置文件修改时间
                    // 比较其现在的修改时间是否与webXmlLastModified保存一致
                    // 不一致则重启Context上下文
                    if (lastModified.longValue() != newLastModified) {
                        // 将其从修改列表移除
                        webXmlLastModified.remove(contextName);
                        // 调用context的stop停止该应用程序
                        ((Lifecycle) context).stop();
                        // Note: If the context was already stopped, a 
                        // Lifecycle exception will be thrown, and the context
                        // won't be restarted
                        // 调用context的start()方法重新启动上下文
                        ((Lifecycle) context).start();
                    }
                }
            } catch (LifecycleException e) {
                ; // Ignore
            } catch (NamingException e) {
                ; // Ignore
            }

        }

    }



    /**
     * Expand the WAR file found at the specified URL into an unpacked
     * directory structure, and return the absolute pathname to the expanded
     * directory.
     *
     * @param war URL of the web application archive to be expanded
     *  (must start with "jar:")
     *
     * @exception IllegalArgumentException if this is not a "jar:" URL
     * @exception IOException if an input/output error was encountered
     *  during expansion
     */
    protected String expand(URL war) throws IOException {

        // Calculate the directory name of the expanded directory
        if (getDebug() >= 1) {
            log("expand(" + war.toString() + ")");
        }
        String pathname = war.toString().replace('\\', '/');
        if (pathname.endsWith("!/")) {
            pathname = pathname.substring(0, pathname.length() - 2);
        }
        int period = pathname.lastIndexOf('.');
        if (period >= pathname.length() - 4)
            pathname = pathname.substring(0, period);
        int slash = pathname.lastIndexOf('/');
        if (slash >= 0) {
            pathname = pathname.substring(slash + 1);
        }
        if (getDebug() >= 1) {
            log("  Proposed directory name: " + pathname);
        }

        // Make sure that there is no such directory already existing
        File appBase = new File(host.getAppBase());
        if (!appBase.isAbsolute()) {
            appBase = new File(System.getProperty("catalina.base"),
                               host.getAppBase());
        }
        if (!appBase.exists() || !appBase.isDirectory()) {
            throw new IOException
                (sm.getString("standardHost.appBase",
                              appBase.getAbsolutePath()));
        }
        File docBase = new File(appBase, pathname);
        if (docBase.exists()) {
            // War file is already installed
            return (docBase.getAbsolutePath());
        }

        // Create the new document base directory
        docBase.mkdir();
        if (getDebug() >= 2) {
            log("  Have created expansion directory " +
                docBase.getAbsolutePath());
        }

        // Expand the WAR into the new document base directory
        JarURLConnection juc = (JarURLConnection) war.openConnection();
        juc.setUseCaches(false);
        /*
        JarFile jarFile = juc.getJarFile();
        if (getDebug() >= 2) {
            log("  Have opened JAR file successfully");
        }
        Enumeration jarEntries = jarFile.entries();
        if (getDebug() >= 2) {
            log("  Have retrieved entries enumeration");
        }
        while (jarEntries.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) jarEntries.nextElement();
            String name = jarEntry.getName();
            if (getDebug() >= 2) {
                log("  Am processing entry " + name);
            }
            int last = name.lastIndexOf('/');
            if (last >= 0) {
                File parent = new File(docBase,
                                       name.substring(0, last));
                if (getDebug() >= 2) {
                    log("  Creating parent directory " + parent);
                }
                parent.mkdirs();
            }
            if (name.endsWith("/")) {
                continue;
            }
            if (getDebug() >= 2) {
                log("  Creating expanded file " + name);
            }
            InputStream input = jarFile.getInputStream(jarEntry);
            expand(input, docBase, name);
            input.close();
        }
        jarFile.close();
        */
        JarFile jarFile = null;
        InputStream input = null;
        try {
            jarFile = juc.getJarFile();
            if (getDebug() >= 2) {
                log("  Have opened JAR file successfully");
            }
            Enumeration jarEntries = jarFile.entries();
            if (getDebug() >= 2) {
                log("  Have retrieved entries enumeration");
            }
            while (jarEntries.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) jarEntries.nextElement();
                String name = jarEntry.getName();
                if (getDebug() >= 2) {
                    log("  Am processing entry " + name);
                }
                int last = name.lastIndexOf('/');
                if (last >= 0) {
                    File parent = new File(docBase,
                                           name.substring(0, last));
                    if (getDebug() >= 2) {
                        log("  Creating parent directory " + parent);
                    }
                    parent.mkdirs();
                }
                if (name.endsWith("/")) {
                    continue;
                }
                if (getDebug() >= 2) {
                    log("  Creating expanded file " + name);
                }
                input = jarFile.getInputStream(jarEntry);
                expand(input, docBase, name);
                input.close();
                input = null;
            }
            jarFile.close();
            jarFile = null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable t) {
                    ;
                }
                input = null;
            }
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (Throwable t) {
                    ;
                }
                jarFile = null;
            }
        }

        // Return the absolute path to our new document base directory
        return (docBase.getAbsolutePath());

    }


    /**
     * Expand the specified input stream into the specified directory, creating
     * a file named from the specified relative path.
     *
     * @param input InputStream to be copied
     * @param docBase Document base directory into which we are expanding
     * @param name Relative pathname of the file to be created
     *
     * @exception IOException if an input/output error occurs
     */
    protected void expand(InputStream input, File docBase, String name)
        throws IOException {

        File file = new File(docBase, name);
        BufferedOutputStream output =
            new BufferedOutputStream(new FileOutputStream(file));
        byte buffer[] = new byte[2048];
        while (true) {
            int n = input.read(buffer);
            if (n <= 0)
                break;
            output.write(buffer, 0, n);
        }
        output.close();

    }


    /**
     * Log a message on the Logger associated with our Host (if any)
     *
     * @param message Message to be logged
     */
    protected void log(String message) {

        Logger logger = null;
        if (host != null)
            logger = host.getLogger();
        if (logger != null)
            logger.log("HostConfig[" + host.getName() + "]: " + message);
        else
            System.out.println("HostConfig[" + host.getName() + "]: "
                               + message);

    }


    /**
     * Log a message on the Logger associated with our Host (if any)
     *
     * @param message Message to be logged
     * @param throwable Associated exception
     */
    protected void log(String message, Throwable throwable) {

        Logger logger = null;
        if (host != null)
            logger = host.getLogger();
        if (logger != null)
            logger.log("HostConfig[" + host.getName() + "] "
                       + message, throwable);
        else {
            System.out.println("HostConfig[" + host.getName() + "]: "
                               + message);
            System.out.println("" + throwable);
            throwable.printStackTrace(System.out);
        }

    }


    /**
     * Process a "start" event for this Host.
     */
    protected void start() {
        // 当Host容器调用其自身start()方法时，会发送START_EVENT事件。之后此方法会被触发
        if (debug >= 1)
            log(sm.getString("hostConfig.start"));
        // 是否开启自动配置(默认为true)
        if (host.getAutoDeploy()) {
            // 部署host指定目录下的web应用程序(Context)
            deployApps();
        }

        // 是否实时动态部署(默认为true)
        if (isLiveDeploy()) {
            // 启动后台线程，监视部署目录的改变并重新载入相关类
            threadStart();
        }

    }


    /**
     * Process a "stop" event for this Host.
     */
    protected void stop() {

        if (debug >= 1)
            log(sm.getString("hostConfig.stop"));
        // 停止后台线程
        threadStop();
        // 解除部署
        undeployApps();

    }


    /**
     * Undeploy all deployed applications.
     */
    protected void undeployApps() {

        if (!(host instanceof Deployer))
            return;
        if (debug >= 1)
            log(sm.getString("hostConfig.undeploying"));

        String contextPaths[] = ((Deployer) host).findDeployedApps();
        for (int i = 0; i < contextPaths.length; i++) {
            if (debug >= 1)
                log(sm.getString("hostConfig.undeploy", contextPaths[i]));
            try {
                ((Deployer) host).remove(contextPaths[i]);
            } catch (Throwable t) {
                log(sm.getString("hostConfig.undeploy.error",
                                 contextPaths[i]), t);
            }
        }

    }


    /**
     * Start the background thread that will periodically check for
     * web application autoDeploy and changes to the web.xml config.
     *
     * @exception IllegalStateException if we should not be starting
     *  a background thread now
     */
    protected void threadStart() {

        // Has the background thread already been started?
        if (thread != null)
            return;

        // Start the background thread
        if (debug >= 1)
            log(" Starting background thread");
        threadDone = false;
        threadName = "HostConfig[" + host.getName() + "]";
        // 自身实现了runnable，将自身传入新线程，
        thread = new Thread(this, threadName);
        thread.setDaemon(true);
        // 因将该类的自身对象this传入了线程， 线程开始时会调用该类的run()方法
        thread.start();

        // tomcat5中有更改，不再新建线程，
        // 而是由StandardHost类的backgroundProcess()方法周期性的触发一个check事件
        // HostConfig对象接收到check事件后，会调用check()方法执行部署应用的检查

    }


    /**
     * Stop the background thread that is periodically checking for
     * for web application autoDeploy and changes to the web.xml config.
     */
    protected void threadStop() {

        if (thread == null)
            return;

        if (debug >= 1)
            log(" Stopping background thread");
        threadDone = true;
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            ;
        }

        thread = null;

    }


    /**
     * Sleep for the duration specified by the <code>checkInterval</code>
     * property.
     */
    protected void threadSleep() {

        try {
            Thread.sleep(checkInterval * 1000L);
        } catch (InterruptedException e) {
            ;
        }

    }


    // ------------------------------------------------------ Background Thread


    /**
     * The background thread that checks for web application autoDeploy
     * and changes to the web.xml config.
     */
    public void run() {
        if (debug >= 1)
            log("BACKGROUND THREAD Starting");

        // Loop until the termination semaphore is set
        // 每次经过间隔时间，循环检查是否有需要部署的文件
        while (!threadDone) {

            // Wait for our check interval
            threadSleep();

            // Deploy apps if the Host allows auto deploying
            // 部署文件
            deployApps();

            // Check for web.xml modification
            // 检查是否有文件被修改
            checkWebXmlLastModified();

        }

        if (debug >= 1)
            log("BACKGROUND THREAD Stopping");

    }


}
