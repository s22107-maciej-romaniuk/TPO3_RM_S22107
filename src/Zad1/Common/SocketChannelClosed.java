package Zad1.Common;

public class SocketChannelClosed extends Exception {
    public SocketChannelClosed(String socket_has_been_closed) {
        super(socket_has_been_closed);
    }
}
