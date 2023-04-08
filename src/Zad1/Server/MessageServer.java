package Zad1.Server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class MessageServer {

    private Selector selector = null;
    private ServerSocketChannel ssc = null;

    public MessageServer(String host, int port) {

        try {
            ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            ssc.socket().bind(new InetSocketAddress(host, port));

            selector = Selector.open();
            ssc.register(selector, SelectionKey.OP_ACCEPT);
        }
        catch(Exception exc){
            exc.printStackTrace();
            System.exit(1);
        }
        System.out.println("Server is running and ready to service connections");
        serviceConnections();
    }


    private Set<SocketChannel> clientSet = new HashSet<>();
    private Map<String, List<SocketChannel>> topicToSubscriberMap = new HashMap<>();

    private void addTopic(String topic){
        this.topicToSubscriberMap.put(topic, new LinkedList<>());
    }
    private void removeTopic(String topic){
        this.topicToSubscriberMap.remove(topic);
    }

    private Map<SocketChannel, List<String>> socketToMessageMap = new HashMap<>();
    private void queueNewMessage(String topic, String message){
        for(SocketChannel channel : this.topicToSubscriberMap.get(topic)) {
            this.socketToMessageMap.get(channel).add(message);
        }
    }
    private void removeClosedChannelFromMaps(SocketChannel sc){
        this.socketToMessageMap.remove(sc);
        for(String topic : topicToSubscriberMap.keySet()){
            topicToSubscriberMap.get(topic).remove(sc);
        }
        this.clientSet.remove(sc);
//        this.unregisterBroadcastChannelWithIdentifier(null, sc);
//        this.unregisterIdentifierWithSubscriptionChannel(sc);
    }

//    private Map<String, SocketChannel> identifierToBroadcastChannelMap = new HashMap<>();
//    private void registerBroadcastChannelWithIdentifier(String identifier, SocketChannel sc){
//        this.identifierToBroadcastChannelMap.put(identifier, sc);
//    }
//    private void unregisterBroadcastChannelWithIdentifier(String identifier, SocketChannel sc){
//        if(identifier == null){
//            identifier = this.getKey(this.identifierToBroadcastChannelMap, sc);
//        }
//        this.identifierToBroadcastChannelMap.remove(identifier);
//    }

//    private Map<SocketChannel, String> subscriptionChannelToIdentifierMap = new HashMap<>();
//    private void registerIdentifierWithSubscriptionChannel(String identifier, SocketChannel sc){
//        this.subscriptionChannelToIdentifierMap.put(sc, identifier);
//    }
//    private void unregisterIdentifierWithSubscriptionChannel(SocketChannel sc){
//        this.subscriptionChannelToIdentifierMap.remove(sc);
//    }
//
//    public <K, V> K getKey(Map<K, V> map, V value) {
//        for (Entry<K, V> entry : map.entrySet()) {
//            if (entry.getValue().equals(value)) {
//                return entry.getKey();
//            }
//        }
//        return null;
//    }


    private void serviceConnections() {
        boolean serverIsRunning = true;
        while(serverIsRunning) {
            try {
                selector.select();

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iter = keys.iterator();
                while(iter.hasNext()){
                    SelectionKey key = iter.next();
                    iter.remove();

                    if(!key.isValid()){ //remove if invalid
                        removeClosedChannelFromMaps((SocketChannel)key.channel());
                        continue;
                    }
                    if(key.isAcceptable()){ //accept if new connection attempt
                        SocketChannel cc = ssc.accept();
                        cc.configureBlocking(false);
                        cc.register(selector, SelectionKey.OP_READ);
                    }
                    if(key.isReadable()){ //read if new data available
                        SocketChannel cc = (SocketChannel) key.channel();
                        serviceRequest(cc);
                    }
//                    if(key.isWritable()){
//                        // tutaj broadcast będzie albo i nie bo mogę writey bezpośrednio w wiadomości od admina robić
//                    }
                }
            }
            catch(Exception exc){
                exc.printStackTrace();
            }
        }
    }

    private static Pattern reqPatt = Pattern.compile(" +", 3);
    private static Charset charset  = Charset.forName("ISO-8859-2");
    private static final int BSIZE = 1024;
    private ByteBuffer bbuf = ByteBuffer.allocate(BSIZE);
    private StringBuffer reqString = new StringBuffer();

    private void serviceRequest(SocketChannel sc) {
        if(!sc.isOpen()) return;
        reqString.setLength(0);
        bbuf.clear();
        try{
            readLoop:
            while(true){
                int n = sc.read(bbuf);     // nie natrafimy na koniec wiersza
                if (n > 0) {
                    bbuf.flip();
                    CharBuffer cbuf = charset.decode(bbuf);
                    while(cbuf.hasRemaining()) {
                        char c = cbuf.get();
                        if (c == '\r' || c == '\n') break readLoop;
                        reqString.append(c);
                    }
                }
            }

            String[] req = reqPatt.split(reqString);
            String cmd = req[0];
            String topic;
            switch(cmd){
            case "SUBSCRIBE": //subskrybowanie do tematu
                topic = req[1];
                this.subscribe(topic, sc);
                break;
            case "UNSUBSCRIBE": //odsubskrybowanie
                topic = req[1];
                this.unsubscribe(topic, sc);
                break;
//            case "REGISTER": //rejestracja nasłuchu klienta
//                String identifier = req[1];
//                String direction = req[2];
//                this.register(sc, identifier, direction);
//                break;
            case "BROADCAST": //roześlij tę wiadomość do klientów
                topic = req[1]; //tematy rozdzielone średnikami
                String message = req[2];
                this.broadcastMessage(topic, message);
                break;
            case "TOPICS": //dodanie nowego tematu
                topic = String.join(" ", Arrays.copyOfRange(req, 1, req.length));
                this.broadcastTopic(topic);
                break;
            }

        }
        catch(Exception ex){
            ex.printStackTrace();
            try { sc.close();
                sc.socket().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void subscribe(String topic, SocketChannel sc){
        try {
            this.topicToSubscriberMap.get(topic).add(sc);
            this.clientSet.add(sc);
        }
        catch(NullPointerException ex){
            System.out.println("Attempt to subscribe to nonexistent topic");
        }
    }

    private void unsubscribe(String topic, SocketChannel sc){
        try{
            this.topicToSubscriberMap.get(topic).remove(sc);
        }
        catch(NullPointerException ex){
            System.out.println("Attempt to unsubscribe from nonexistent topic");
        }
    }

//    private void register(SocketChannel sc, String identifier, String direction){
//        if(direction.equals("SUBSCRIPTIONCHANNEL")){
//            this.registerIdentifierWithSubscriptionChannel(identifier, sc);
//        }
//        else if(direction.equals("LISTENERCHANNEL")){
//            this.registerBroadcastChannelWithIdentifier(identifier, sc);
//        }
//    }

    private void broadcastMessage(String topic, String message){
        for(SocketChannel sc : this.topicToSubscriberMap.get(topic)){
            try {
                this.writeMessage(sc, topic, message);
            }
            catch(IOException ex){
                System.out.println("Error during writing to SocketChannel");
            }
        }
    }

    private void broadcastTopic(String topics)
    {
        for(SocketChannel sc : this.clientSet){
            try {
                this.writeTopic(sc, topics);
            }
            catch(IOException ex){
                System.out.println("Error during writing to SocketChannel");
            }
        }
    }

    private StringBuffer remsg = new StringBuffer(); // na odpowiedź
    private void writeMessage(SocketChannel sc, String topic, String message) throws IOException {
        remsg.setLength(0);
        remsg.append("MESSAGE");
        remsg.append(" ");
        remsg.append(topic);
        remsg.append(" ");
        remsg.append(message);
        ByteBuffer buf = charset.encode(CharBuffer.wrap(remsg));
        sc.write(buf);
    }
    private void writeTopic(SocketChannel sc, String topics) throws IOException {
        remsg.setLength(0);
        remsg.append("TOPICS");
        remsg.append(" ");
        remsg.append(topics);
        ByteBuffer buf = charset.encode(CharBuffer.wrap(remsg));
        sc.write(buf);
    }

    public static void main(String[] args){
        try {
            String host = "localhost";
            int port = Integer.parseInt(args[0]);
            new MessageServer(host, port);
        }
        catch(Exception ex){
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
