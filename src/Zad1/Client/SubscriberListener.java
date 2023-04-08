package Zad1.Client;

import Zad1.Common.Common;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class SubscriberListener implements Runnable{
    IUpdater updater;
    SocketChannel connection;
    Boolean continueWork = true;
    Selector selector = null;
    public SubscriberListener(SocketChannel sc, IUpdater updater) throws IOException {
        this.updater = updater;
        this.connection = sc;
        this.selector = Selector.open();
        this.connection.register(this.selector, SelectionKey.OP_READ);
    }
    @Override
    public void run() {
        while(continueWork){
            try{
                int selectionsNumber = selector.selectNow();
                if(selectionsNumber == 0){
                    continue;
                }
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iter = keys.iterator();
                while(iter.hasNext()){
                    SelectionKey key = iter.next();
                    iter.remove();
                    if(key.isReadable()){ //read if new data available
                        System.out.println("Reading data");
                        SocketChannel cc = (SocketChannel) key.channel();
                        serviceRequest(cc);
                    }

                }
            }
            catch (Exception ex){
                System.out.println(ex);
            }
        }
    }

    private static Pattern reqPatt = Pattern.compile(" +", 3);
    private static final int BSIZE = 1024;
    private ByteBuffer bbuf = ByteBuffer.allocate(BSIZE);
    private StringBuffer reqString = new StringBuffer();

    private void serviceRequest(SocketChannel sc) {
        if (!sc.isOpen()) return;
        System.out.println("Handling request");
        reqString.setLength(0);
        bbuf.clear();
        try {
            Common.readData(bbuf, sc, reqString);
            System.out.println(reqString);
            String[] req = reqPatt.split(reqString);
            String cmd = req[0];
            switch (cmd) {
            case "TOPICS":
                List<String> topics = Arrays.asList(Arrays.copyOfRange(req, 1, req.length));
                this.updater.update(topics, null);
                break;
            case "MESSAGE":
                List<String> messages = Arrays.asList(Arrays.copyOfRange(req, 1, req.length));
                this.updater.update(null, messages);
                break;
            default:
                System.out.println("Unrecognised data");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
