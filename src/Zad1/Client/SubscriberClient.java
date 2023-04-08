package Zad1.Client;

import Zad1.Common.Common;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

public class SubscriberClient {
    SocketChannel clientChannel;
    SubscriberListener listener;
    Thread listenerThread;

    public SubscriberClient(int port, IUpdater updater) throws IOException {
        clientChannel = SocketChannel.open();
        SocketAddress socketAddr = new InetSocketAddress("localhost", port);
        clientChannel.connect(socketAddr);
        clientChannel.configureBlocking(false);
        listener = new SubscriberListener(clientChannel, updater);
        listenerThread = new Thread(listener);
        listenerThread.start();
    }

    public void stopListening(){
        listener.continueWork = false;
    }

    public void subscribe(String topic){
        StringBuffer sb = new StringBuffer();
        sb.append("SUBSCRIBE");
        sb.append(" ");
        sb.append(topic);
        try {
            Common.sendData(sb, clientChannel);
        }
        catch(IOException ex){
            System.out.println("Error while subscribing");
        }
    }

    public void unsubscribe(String topic){
        StringBuffer sb = new StringBuffer();
        sb.append("UNSUBSCRIBE");
        sb.append(" ");
        sb.append(topic);
        try{
            Common.sendData(sb, clientChannel);
        }
        catch(IOException ex){
            System.out.println("Error while unsubscribing");
        }
    }
}
