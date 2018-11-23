package nioiotest;

import java.io.IOError;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

/*
 * @Time    : 2018/11/22 3:32 PM
 * @Author  : YouMing
 * @Email   : myoueva@gmail.com
 * @File    : socketniotest.java
 * @Software: IntelliJ IDEA
 */
public class socketniotest {

    private String ip = "127.0.0.1";
    private int port = 9900;
    public static void main(String[] args) throws IOException {
        System.out.println("开始");
        System.out.println("======");
        socketniotest socket = new socketniotest();
//        socketniotest.client cli = socket.new client();
//        cli.start();
        socketniotest.server ser = socket.new server();
        ser.start();
        System.out.println("==========");
    }

    class server extends Thread{

        @Override
        public void run() {
            try {
                nioserver server = new nioserver(port);
                server.server();
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    class client extends Thread{

        @Override
        public void run() {
            try {
                nioclient cli = new nioclient(ip, port);
                while (true) {
                    SocketChannel channel = cli.initclient();
                    cli.sendMessage(channel, "我来了");
                    cli.closed(channel);
                }
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}

class nioserver implements serverable{
    private final int TIME_OUT = 3000;
    private final int MAX_BYTE_SIZE = 1024;

    private int port;
    private ServerSocketChannel channels;

    public nioserver(int port){
        this.port = port;
    }

    public  void server() throws IOException {
        /*
         *
         * 展示serversocketchannel和selector绑定
         * 1.创建socket通道池（设置为非阻塞模式）
         * 2.将通道池绑定到给定的端口
         * 3.将通道池注册到selector(如对多个事件类型感兴趣则用或位移运算符来连接)
         * */
        ServerSocketChannel channels = ServerSocketChannel.open();
        channels.configureBlocking(false);
        ServerSocket server = channels.socket();
        InetSocketAddress address = new InetSocketAddress("localhost", port);
        server.bind(address);
        Selector selector = Selector.open();
        // 特别注意:服务开启时，注册链接通道key，当监听key注册以后，需要注册read&write等
        channels.register(selector, SelectionKey.OP_ACCEPT);
        while (true){
            //设置等待阻塞时间
            if(selector.select(3000)==0){
                System.out.println("time out");
                continue;
            }
            System.out.println("begin handle");
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            while (keys.hasNext()){
                SelectionKey currentKey = keys.next();
                if (currentKey.isAcceptable()){
                    this.accept(currentKey);
                }
                if (currentKey.isReadable()){
                    this.read(currentKey);
                }
            }
            // 注意：每次处理完成通道后需要清除
            keys.remove();
        }
    }

    @Override
    public void accept(SelectionKey key) {
        try {
            ServerSocketChannel channels = (ServerSocketChannel) key.channel();
            SocketChannel channel = channels.accept();
            channel.configureBlocking(false);
            channel.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocateDirect(1024));
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void read(SelectionKey key) {
        try {
            SocketChannel channel = (SocketChannel) key.channel();
            ByteBuffer buffer = (ByteBuffer) key.attachment();
            buffer.clear();
            if (channel.read(buffer) == -1){
                channel.close();
            }else {
                buffer.flip();
                while (buffer.hasRemaining()){
                    System.out.println((char) buffer.get());
                }
                buffer = ByteBuffer.wrap("ok".getBytes());
                channel.write(buffer);
                channel.close();
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void write(SelectionKey key) {

    }

//    public

}

class nioclient implements clientable{
    private String ip;
    private int port;

    public nioclient(String ip, int port){
        this.ip = ip;
        this.port = port;
    }


    @Override
    public void sendMessage(SocketChannel channel, String msg) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put(msg.getBytes());
        if (channel.finishConnect()){
            System.out.println(msg);
            buffer.flip();
            channel.write(buffer);
            buffer.clear();
        }else {
            System.out.println("客户端未连接");
        }
    }

    @Override
    public void closed(SocketChannel channel) throws IOException {
        if (channel.finishConnect()){
            channel.close();
        }
    }

    public SocketChannel initclient() throws IOException{
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(new InetSocketAddress(ip, port));
        return channel;
    }
}

interface clientable{
    void sendMessage(SocketChannel channel, String msg) throws IOException;
    void closed(SocketChannel channel) throws IOException;
}

interface serverable{
    void accept(SelectionKey key);
    void read(SelectionKey key);
    void write(SelectionKey key);
}
