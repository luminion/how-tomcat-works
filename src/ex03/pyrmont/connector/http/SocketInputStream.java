package ex03.pyrmont.connector.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.EOFException;
import org.apache.catalina.util.StringManager;

/**
 * Extends InputStream to be more efficient reading lines during HTTP
 * header processing.
 *
 * 输入流的子类，
 * 用于获取请求的信息
 *
 * 将请求行封装为ex03.pyrmont.connector.http.HttpRequestLine对象
 * 将请求头HttpHeader并封装请求头信息
 *
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 * @deprecated
 */
public class SocketInputStream extends InputStream {


    // -------------------------------------------------------------- Constants


    /**
     * CR.
     */
    private static final byte CR = (byte) '\r';


    /**
     * LF.
     */
    private static final byte LF = (byte) '\n';


    /**
     * SP.
     */
    private static final byte SP = (byte) ' ';


    /**
     * HT.
     */
    private static final byte HT = (byte) '\t';


    /**
     * COLON.
     */
    private static final byte COLON = (byte) ':';


    /**
     * Lower case offset.
     */
    private static final int LC_OFFSET = 'A' - 'a';


    /**
     * Internal buffer.
     */
    protected byte buf[];


    /**
     * Last valid byte.
     */
    protected int count;


    /**
     * Position in the buffer.
     */
    protected int pos;


    /**
     * Underlying input stream.
     */
    protected InputStream is;


    // ----------------------------------------------------------- Constructors


    /**
     * Construct a servlet input stream associated with the specified socket
     * input.
     *
     * @param is socket input stream
     * @param bufferSize size of the internal buffer
     */
    public SocketInputStream(InputStream is, int bufferSize) {

        this.is = is;
        buf = new byte[bufferSize];

    }


    // -------------------------------------------------------------- Variables


    /**
     * The string manager for this package.
     */
    protected static StringManager sm =
        StringManager.getManager(Constants.Package);


    // ----------------------------------------------------- Instance Variables


    // --------------------------------------------------------- Public Methods


    /**
     * Read the request line, and copies it to the given buffer. This
     * function is meant to be used during the HTTP request header parsing.
     * Do NOT attempt to read the request body using it.
     *
     * 读取请求行，并将其复制到给定的缓冲区。
     * 并将信息封装到传入的requestLine对象
     * 此函数旨在在 HTTP 请求标头解析期间使用。
     * 不要尝试使用它来读取请求正文。
     *
     * @param requestLine Request line object
     * @throws IOException If an exception occurs during the underlying socket
     * read operations, or if the given buffer is not big enough to accomodate
     * the whole line. 在底层套接字读取操作期间发生异常，或者给定的缓冲区不足以容纳整行时抛出异常
     *
     */
    public void readRequestLine(HttpRequestLine requestLine)
        throws IOException {

        // Recycling check
        if (requestLine.methodEnd != 0)
            requestLine.recycle();

        // Checking for a blank line
        int chr = 0;
        do { // Skipping CR or LF
            try {
                chr = read();
            } catch (IOException e) {
                chr = -1;
            }
        } while ((chr == CR) || (chr == LF));
        if (chr == -1)
            throw new EOFException
                (sm.getString("requestStream.readline.error"));
        pos--;

        // Reading the method name

        int maxRead = requestLine.method.length;
        int readStart = pos;
        int readCount = 0;

        boolean space = false;

        while (!space) {
            // if the buffer is full, extend it
            if (readCount >= maxRead) {
                if ((2 * maxRead) <= HttpRequestLine.MAX_METHOD_SIZE) {
                    char[] newBuffer = new char[2 * maxRead];
                    System.arraycopy(requestLine.method, 0, newBuffer, 0,
                                     maxRead);
                    requestLine.method = newBuffer;
                    maxRead = requestLine.method.length;
                } else {
                    throw new IOException
                        (sm.getString("requestStream.readline.toolong"));
                }
            }
            // We're at the end of the internal buffer
            if (pos >= count) {
                int val = read();
                if (val == -1) {
                    throw new IOException
                        (sm.getString("requestStream.readline.error"));
                }
                pos = 0;
                readStart = 0;
            }
            if (buf[pos] == SP) {
                space = true;
            }
            requestLine.method[readCount] = (char) buf[pos];
            readCount++;
            pos++;
        }

        requestLine.methodEnd = readCount - 1;

        // Reading URI

        maxRead = requestLine.uri.length;
        readStart = pos;
        readCount = 0;

        space = false;

        boolean eol = false;

        while (!space) {
            // if the buffer is full, extend it
            if (readCount >= maxRead) {
                if ((2 * maxRead) <= HttpRequestLine.MAX_URI_SIZE) {
                    char[] newBuffer = new char[2 * maxRead];
                    System.arraycopy(requestLine.uri, 0, newBuffer, 0,
                                     maxRead);
                    requestLine.uri = newBuffer;
                    maxRead = requestLine.uri.length;
                } else {
                    throw new IOException
                        (sm.getString("requestStream.readline.toolong"));
                }
            }
            // We're at the end of the internal buffer
            if (pos >= count) {
                int val = read();
                if (val == -1)
                    throw new IOException
                        (sm.getString("requestStream.readline.error"));
                pos = 0;
                readStart = 0;
            }
            if (buf[pos] == SP) {
                space = true;
            } else if ((buf[pos] == CR) || (buf[pos] == LF)) {
                // HTTP/0.9 style request
                eol = true;
                space = true;
            }
            requestLine.uri[readCount] = (char) buf[pos];
            readCount++;
            pos++;
        }

        requestLine.uriEnd = readCount - 1;

        // Reading protocol

        maxRead = requestLine.protocol.length;
        readStart = pos;
        readCount = 0;

        while (!eol) {
            // if the buffer is full, extend it
            if (readCount >= maxRead) {
                if ((2 * maxRead) <= HttpRequestLine.MAX_PROTOCOL_SIZE) {
                    char[] newBuffer = new char[2 * maxRead];
                    System.arraycopy(requestLine.protocol, 0, newBuffer, 0,
                                     maxRead);
                    requestLine.protocol = newBuffer;
                    maxRead = requestLine.protocol.length;
                } else {
                    throw new IOException
                        (sm.getString("requestStream.readline.toolong"));
                }
            }
            // We're at the end of the internal buffer
            if (pos >= count) {
                // Copying part (or all) of the internal buffer to the line
                // buffer
                int val = read();
                if (val == -1)
                    throw new IOException
                        (sm.getString("requestStream.readline.error"));
                pos = 0;
                readStart = 0;
            }
            if (buf[pos] == CR) {
                // Skip CR.
            } else if (buf[pos] == LF) {
                eol = true;
            } else {
                requestLine.protocol[readCount] = (char) buf[pos];
                readCount++;
            }
            pos++;
        }

        requestLine.protocolEnd = readCount;

    }


    /**
     * Read a header, and copies it to the given buffer. This
     * function is meant to be used during the HTTP request header parsing.
     * Do NOT attempt to read the request body using it.
     *
     * 读取请求头，并将其复制到给定的缓冲区。
     * 并将信息封装到传入的RequestHeader对象
     * 此函数旨在在 HTTP 请求标头解析期间使用。
     * 不要尝试使用它来读取请求正文。
     *
     * @param header RequestHeader对象
     * @throws IOException If an exception occurs during the underlying socket
     * read operations, or if the given buffer is not big enough to accomodate
     * the whole line.
     */
    public void readHeader(HttpHeader header)
        throws IOException {

        // Recycling check      检查header是否被赋值过
        if (header.nameEnd != 0)
            header.recycle();

        // Checking for a blank line    检查空白行
        int chr = read();
        if ((chr == CR) || (chr == LF)) { // Skipping CR
            if (chr == CR)
                read(); // Skipping LF
            header.nameEnd = 0;
            header.valueEnd = 0;
            return;
        } else {
            pos--;
        }

        // Reading the header name  读取请求头名称

        int maxRead = header.name.length;
        int readStart = pos;
        int readCount = 0;

        boolean colon = false;
        // 未读取到; 则循环
        while (!colon) {
            // if the buffer is full, extend it
            if (readCount >= maxRead) {
                if ((2 * maxRead) <= HttpHeader.MAX_NAME_SIZE) {
                    char[] newBuffer = new char[2 * maxRead];
                    System.arraycopy(header.name, 0, newBuffer, 0, maxRead);
                    header.name = newBuffer;
                    maxRead = header.name.length;
                } else {
                    throw new IOException
                        (sm.getString("requestStream.readline.toolong"));
                }
            }
            // We're at the end of the internal buffer
            if (pos >= count) {
                int val = read();
                if (val == -1) {
                    throw new IOException
                        (sm.getString("requestStream.readline.error"));
                }
                pos = 0;
                readStart = 0;
            }
            if (buf[pos] == COLON) {
                colon = true;
            }
            char val = (char) buf[pos];
            if ((val >= 'A') && (val <= 'Z')) {
                val = (char) (val - LC_OFFSET);
            }
            header.name[readCount] = val;
            readCount++;
            pos++;
        }
        // 设置请求头名称结束位置的下标
        header.nameEnd = readCount - 1;

        // Reading the header value (which can be spanned over multiple lines)
        // 读取请求头的值

        maxRead = header.value.length;
        readStart = pos;
        readCount = 0;

        int crPos = -2;

        boolean eol = false;
        boolean validLine = true;

        // 本行未到末尾则读取
        while (validLine) {

            boolean space = true;

            // Skipping spaces
            // Note : Only leading white spaces are removed. Trailing white
            // spaces are not.
            // 跳过开头的空格，但不跳过末尾的
            while (space) {
                // We're at the end of the internal buffer
                if (pos >= count) {
                    // Copying part (or all) of the internal buffer to the line
                    // buffer
                    int val = read();
                    if (val == -1)
                        throw new IOException
                            (sm.getString("requestStream.readline.error"));
                    pos = 0;
                    readStart = 0;
                }
                if ((buf[pos] == SP) || (buf[pos] == HT)) {
                    pos++;
                } else {
                    space = false;
                }
            }

            //
            while (!eol) {
                // if the buffer is full, extend it 若缓冲区大小不够，扩充缓冲区
                if (readCount >= maxRead) {
                    if ((2 * maxRead) <= HttpHeader.MAX_VALUE_SIZE) {
                        char[] newBuffer = new char[2 * maxRead];
                        System.arraycopy(header.value, 0, newBuffer, 0,
                                         maxRead);
                        header.value = newBuffer;
                        maxRead = header.value.length;
                    } else {
                        throw new IOException
                            (sm.getString("requestStream.readline.toolong"));
                    }
                }
                // We're at the end of the internal buffer  缓冲区读取到了末尾
                if (pos >= count) {
                    // Copying part (or all) of the internal buffer to the line
                    // buffer
                    int val = read();
                    if (val == -1)
                        throw new IOException
                            (sm.getString("requestStream.readline.error"));
                    pos = 0;
                    readStart = 0;
                }
                if (buf[pos] == CR) {
                } else if (buf[pos] == LF) {
                    eol = true;
                } else {
                    // FIXME : Check if binary conversion is working fine
                    int ch = buf[pos] & 0xff;
                    header.value[readCount] = (char) ch;
                    readCount++;
                }
                pos++;
            }

            int nextChr = read();

            if ((nextChr != SP) && (nextChr != HT)) {
                pos--;
                validLine = false;
            } else {
                eol = false;
                // if the buffer is full, extend it 缓冲区不够则扩展
                if (readCount >= maxRead) {
                    if ((2 * maxRead) <= HttpHeader.MAX_VALUE_SIZE) {
                        char[] newBuffer = new char[2 * maxRead];
                        System.arraycopy(header.value, 0, newBuffer, 0,
                                         maxRead);
                        header.value = newBuffer;
                        maxRead = header.value.length;
                    } else {
                        throw new IOException
                            (sm.getString("requestStream.readline.toolong"));
                    }
                }
                header.value[readCount] = ' ';
                readCount++;
            }

        }
        // 设置请求头的值的结束位置的下标
        header.valueEnd = readCount;

    }


    /**
     * Read byte.
     */
    public int read()
        throws IOException {
        if (pos >= count) {
            fill();
            if (pos >= count)
                return -1;
        }
        return buf[pos++] & 0xff;
    }


    /**
     *
     */
    /*
    public int read(byte b[], int off, int len)
        throws IOException {

    }
    */


    /**
     *
     */
    /*
    public long skip(long n)
        throws IOException {

    }
    */


    /**
     * Returns the number of bytes that can be read from this input
     * stream without blocking.
     */
    public int available()
        throws IOException {
        return (count - pos) + is.available();
    }


    /**
     * Close the input stream.
     */
    public void close()
        throws IOException {
        if (is == null)
            return;
        is.close();
        is = null;
        buf = null;
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Fill the internal buffer using data from the undelying input stream.
     */
    protected void fill()
        throws IOException {
        pos = 0;
        count = 0;
        int nRead = is.read(buf, 0, buf.length);
        if (nRead > 0) {
            count = nRead;
        }
    }


}
