package Zad1.AdminConsole;

import Zad1.Common.Common;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.List;

public class AdminClient {
    SocketChannel clientChannel;

    public AdminClient(int port) throws IOException {
        clientChannel = SocketChannel.open();
        SocketAddress socketAddr = new InetSocketAddress("localhost", port);
        clientChannel.connect(socketAddr);
    }

    public void sendTopicList(List<String> topicList){
        StringBuffer sb = new StringBuffer();
        sb.append("TOPICS");
        sb.append(" ");
        for(String topic : topicList){
            sb.append(topic);
            sb.append(" ");
        }
        sb.deleteCharAt(sb.length()-1);
        try {
            Common.sendData(sb, clientChannel);
        }
        catch(IOException e){
            System.out.println("Could not send topic list");
        }
    }

    public void sendMessage(String topic, String message){
        StringBuffer sb = new StringBuffer();
        sb.append("BROADCAST");
        sb.append(" ");
        sb.append(topic);
        sb.append(" ");
        sb.append(message);
        try{
            Common.sendData(sb, clientChannel);
        }
        catch(IOException ex){
            System.out.println("Could not send message");
        }
    }
}
