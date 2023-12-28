/*
 * $Header: /home/cvs/jakarta-tomcat-4.0/catalina/src/share/org/apache/catalina/logger/FileLogger.java,v 1.8 2002/06/09 02:19:43 remm Exp $
 * $Revision: 1.8 $
 * $Date: 2002/06/09 02:19:43 $
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


package org.apache.catalina.logger;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;


/**
 * Implementation of <b>Logger</b> that appends log messages to a file
 * named {prefix}.{date}.{suffix} in a configured directory, with an
 * optional preceding timestamp.
 *
 * Logger的实现
 * 将日志消息附加到配置目录中名为 {prefix}.{date}.{suffix} 的文件中
 * 带有可选的前置时间戳
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.8 $ $Date: 2002/06/09 02:19:43 $
 */

public class FileLogger
    extends LoggerBase
    implements Lifecycle {


    // ----------------------------------------------------- Instance Variables


    /**
     * The as-of date for the currently open log file, or a zero-length
     * string if there is no open log file.
     *
     * 当前打开的日志文件的截止日期，如果没有打开的日志文件，则为长度为零的字符串。
     */
    private String date = "";


    /**
     * The directory in which log files are created.
     * 创建日志文件的目录。
     */
    private String directory = "logs";


    /**
     * The descriptive information about this implementation.
     */
    protected static final String info =
        "org.apache.catalina.logger.FileLogger/1.0";


    /**
     * The lifecycle event support for this component.
     * 组件的生命周期事件支持类
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * The prefix that is added to log file filenames.
     * 文件名前缀
     */
    private String prefix = "catalina.";


    /**
     * The string manager for this package.
     */
    private StringManager sm =
        StringManager.getManager(Constants.Package);


    /**
     * Has this component been started?
     */
    private boolean started = false;


    /**
     * The suffix that is added to log file filenames.
     * 文件名后缀
     */
    private String suffix = ".log";


    /**
     * Should logged messages be date/time stamped?
     */
    private boolean timestamp = false;


    /**
     * The PrintWriter to which we are currently logging, if any.
     */
    private PrintWriter writer = null;


    // ------------------------------------------------------------- Properties


    /**
     * Return the directory in which we create log files.
     */
    public String getDirectory() {

        return (directory);

    }


    /**
     * Set the directory in which we create log files.
     *
     * @param directory The new log file directory
     */
    public void setDirectory(String directory) {

        String oldDirectory = this.directory;
        this.directory = directory;
        support.firePropertyChange("directory", oldDirectory, this.directory);

    }


    /**
     * Return the log file prefix.
     */
    public String getPrefix() {

        return (prefix);

    }


    /**
     * Set the log file prefix.
     *
     * @param prefix The new log file prefix
     */
    public void setPrefix(String prefix) {

        String oldPrefix = this.prefix;
        this.prefix = prefix;
        support.firePropertyChange("prefix", oldPrefix, this.prefix);

    }


    /**
     * Return the log file suffix.
     */
    public String getSuffix() {

        return (suffix);

    }


    /**
     * Set the log file suffix.
     *
     * @param suffix The new log file suffix
     */
    public void setSuffix(String suffix) {

        String oldSuffix = this.suffix;
        this.suffix = suffix;
        support.firePropertyChange("suffix", oldSuffix, this.suffix);

    }


    /**
     * Return the timestamp flag.
     */
    public boolean getTimestamp() {

        return (timestamp);

    }


    /**
     * Set the timestamp flag.
     *
     * @param timestamp The new timestamp flag
     */
    public void setTimestamp(boolean timestamp) {

        boolean oldTimestamp = this.timestamp;
        this.timestamp = timestamp;
        support.firePropertyChange("timestamp", new Boolean(oldTimestamp),
                                   new Boolean(this.timestamp));

    }


    // --------------------------------------------------------- Public Methods


    /**
     * Writes the specified message to a servlet log file, usually an event
     * log.  The name and type of the servlet log is specific to the
     * servlet container.
     *
     * @param msg A <code>String</code> specifying the message to be written
     *  to the log file
     */
    public void log(String msg) {

        // Construct the timestamp we will use, if requested
        // 获取当前时间戳
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        String tsString = ts.toString().substring(0, 19);
        String tsDate = tsString.substring(0, 10);

        // If the date has changed, switch log files
        // 若果时间戳不为文件的时间戳(date)变量，重新创建文件，并将新的时间戳复制给date变量
        if (!date.equals(tsDate)) {
            synchronized (this) {
                if (!date.equals(tsDate)) {
                    // 关闭流
                    close();
                    date = tsDate;
                    // 打开流
                    open();
                }
            }
        }

        // Log this message, timestamped if necessary
        // 将信息写入文件（判断是否添加时间戳）
        if (writer != null) {
            if (timestamp) {
                writer.println(tsString + " " + msg);
            } else {
                writer.println(msg);
            }
        }

    }


    // -------------------------------------------------------- Private Methods


    /**
     * Close the currently open log file (if any)
     * 关闭当前打开的日志文件相关流（如果有）
     */
    private void close() {

        if (writer == null)
            return;
        writer.flush();
        writer.close();
        writer = null;
        date = "";

    }


    /**
     * Open the new log file for the date specified by <code>date</code>.
     */
    private void open() {

        // Create the directory if necessary
        File dir = new File(directory);
        // 检查是否为绝对路径，若不是，拼接
        if (!dir.isAbsolute())
            dir = new File(System.getProperty("catalina.base"), directory);
        // 创建文件夹
        dir.mkdirs();

        // Open the current log file
        // 创建PrintWriter赋值给成员变量，便于之后调用时写入文件
        try {
            String pathname = dir.getAbsolutePath() + File.separator +
                prefix + date + suffix;
            writer = new PrintWriter(new FileWriter(pathname, true), true);
        } catch (IOException e) {
            writer = null;
        }

    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * Add a lifecycle event listener to this component.
     *
     * @param listener The listener to add
     */
    public void addLifecycleListener(LifecycleListener listener) {

        lifecycle.addLifecycleListener(listener);

    }


    /**
     * Get the lifecycle listeners associated with this lifecycle. If this
     * Lifecycle has no listeners registered, a zero-length array is returned.
     */
    public LifecycleListener[] findLifecycleListeners() {

        return lifecycle.findLifecycleListeners();

    }


    /**
     * Remove a lifecycle event listener from this component.
     *
     * @param listener The listener to add
     */
    public void removeLifecycleListener(LifecycleListener listener) {

        lifecycle.removeLifecycleListener(listener);

    }


    /**
     * Prepare for the beginning of active use of the public methods of this
     * component.  This method should be called after <code>configure()</code>,
     * and before any of the public methods of the component are utilized.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    public void start() throws LifecycleException {

        // Validate and update our current component state
        if (started)
            throw new LifecycleException
                (sm.getString("fileLogger.alreadyStarted"));
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;

    }


    /**
     * Gracefully terminate the active use of the public methods of this
     * component.  This method should be the last one called on a given
     * instance of this component.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public void stop() throws LifecycleException {

        // Validate and update our current component state
        if (!started)
            throw new LifecycleException
                (sm.getString("fileLogger.notStarted"));
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;
        // 关闭打开的流
        close();

    }


}

