package Zad1.Server;

import Zad1.Common.Common;
import Zad1.Common.SocketChannelClosed;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
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


    private final Set<SocketChannel> clientSet = new HashSet<>();
    private final Map<String, Set<SocketChannel>> topicToSubscriberMap = new HashMap<>();



    private void removeClosedChannelFromMaps(SocketChannel sc){
        for(String topic : topicToSubscriberMap.keySet()){
            topicToSubscriberMap.get(topic).remove(sc);
        }
        this.clientSet.remove(sc);
    }




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
                        System.out.println("Accepting connection");
                        SocketChannel cc = ssc.accept();
                        cc.configureBlocking(false);
                        cc.register(selector, SelectionKey.OP_READ);
                        this.clientSet.add(cc);
                        this.sendTopic(cc, String.join(" ",this.topicToSubscriberMap.keySet()));
                    }
                    if(key.isReadable()){ //read if new data available
                        System.out.println("Subscribers before: " + this.topicToSubscriberMap.toString());
                        System.out.println("Reading data");
                        SocketChannel cc = (SocketChannel) key.channel();
                        serviceRequest(cc);
                        System.out.println("Subscribers after: " + this.topicToSubscriberMap.toString());
                    }
                }
            }
            catch(Exception exc){
                exc.printStackTrace();
            }
        }
    }

    private static final Pattern reqPatt = Pattern.compile(" +", 3);
    private static final int BSIZE = 1024;
    private final ByteBuffer bbuf = ByteBuffer.allocate(BSIZE);
    private final StringBuffer reqString = new StringBuffer();

    private void serviceRequest(SocketChannel sc) {
        if(!sc.isOpen()) return;
        System.out.println("Handling request");
        reqString.setLength(0);
        bbuf.clear();
        try{
            Common.readData(bbuf, sc, reqString);
            System.out.println(reqString);
            String[] req = reqPatt.split(reqString);
            String cmd = req[0];
            String topicString;
            switch(cmd){
            case "SUBSCRIBE": //subskrybowanie do tematu
                topicString = req[1];
                this.subscribe(topicString, sc);
                break;
            case "UNSUBSCRIBE": //odsubskrybowanie
                topicString = req[1];
                this.unsubscribe(topicString, sc);
                break;
            case "BROADCAST": //roześlij tę wiadomość do klientów
                topicString = req[1];
                String message = String.join(" ", Arrays.copyOfRange(req, 2, req.length));
                this.broadcastMessage(topicString, message);
                break;
            case "TOPICS": //dodanie nowego tematu
                System.out.println("Received topics");
                List<String> topics = Arrays.asList(Arrays.copyOfRange(req, 1, req.length));
                this.updateTopicToSubscriberMap(topics);
                this.broadcastTopic(String.join(" ",this.topicToSubscriberMap.keySet()));
                break;
            }

        }
        catch(SocketChannelClosed ex){
            System.out.println(ex.getMessage());
            try { sc.close();
                sc.socket().close();
                this.removeClosedChannelFromMaps(sc);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        catch(Exception ex){
            ex.printStackTrace();
            try { sc.close();
                sc.socket().close();
                this.removeClosedChannelFromMaps(sc);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updateTopicToSubscriberMap(List<String> topics) {
        //remove forgotten topics
        List<String> forgottenTopics = new ArrayList<>(this.topicToSubscriberMap.keySet());
        forgottenTopics.removeAll(topics);
        for (String topic:
             forgottenTopics) {
            this.topicToSubscriberMap.remove(topic);
        }
        //add new topics
        for(String topic : topics){
            if(!this.topicToSubscriberMap.containsKey(topic)){
                this.topicToSubscriberMap.put(topic, new HashSet<>());
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

    private void broadcastMessage(String topic, String message){
        System.out.println("Starting broadcast");
        if(this.topicToSubscriberMap.get(topic) != null) {
            for (SocketChannel sc : this.topicToSubscriberMap.get(topic)) {
                System.out.println("Sending to next client");
                try {
                    this.writeMessage(sc, topic + ":", message);
                } catch (IOException ex) {
                    System.out.println("Error during writing to SocketChannel");
                }
            }
        }
    }

    private void broadcastTopic(String topics)
    {
        System.out.println("Broadcasting topics");
        for(SocketChannel sc : this.clientSet){
            System.out.println("Sending topics to next client");
            try {
                this.writeTopic(sc, topics);
            }
            catch(IOException ex){
                System.out.println("Error during writing to SocketChannel");
            }
        }
    }

    private void sendTopic(SocketChannel cc, String topics) {
        try {
            this.writeTopic(cc, topics);
        }
        catch(IOException ex){
            System.out.println("Error during writing to SocketChannel");
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
        Common.sendData(remsg, sc);
    }
    private void writeTopic(SocketChannel sc, String topics) throws IOException {
        remsg.setLength(0);
        remsg.append("TOPICS");
        remsg.append(" ");
        remsg.append(topics);
        Common.sendData(remsg, sc);
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
