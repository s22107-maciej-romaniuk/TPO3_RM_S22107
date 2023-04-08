package Zad1.Common;

import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class Common {
    public static Charset charset  = Charset.forName("ISO-8859-2");


    public static void sendData(StringBuffer sb, SocketChannel sc) throws IOException {
        sb.append("\n");
        ByteBuffer buf = Zad1.Common.Common.charset.encode(CharBuffer.wrap(sb));
        sc.write(buf);
    }

    public static void readData(ByteBuffer bbuf, SocketChannel sc, StringBuffer reqString)
            throws IOException, SocketChannelClosed {
        readLoop:
        while(true){
            int n = sc.read(bbuf);     // nie natrafimy na koniec wiersza
            if (n > 0) {
                bbuf.flip();
                CharBuffer cbuf = Zad1.Common.Common.charset.decode(bbuf);
                while(cbuf.hasRemaining()) {
                    char c = cbuf.get();
                    if (c == '\r' || c == '\n') break readLoop;
                    reqString.append(c);
                }
            }
            if(n == -1){
                throw new SocketChannelClosed("Socket has been closed");
            }
        }
    }
}
