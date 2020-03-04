package com.github.glue;

import com.github.glue.controller.ClientGroup1Controller;
import com.github.glue.controller.QueryReq;
import com.github.glue.controller.ServerGroup1Controller;
import lombok.SneakyThrows;
import org.junit.Test;

/**
 * @author shizi
 * @since 2020/3/4 上午11:43
 */
public class NettyTest {

    /**
     * 启动服务端
     */
    @Test
    @SneakyThrows
    public void testServer() {
        NettyServer server = NettyServer.getInstance().bind("127.0.0.1:8081");
        server.addController(new ServerGroup1Controller());
        server.start();

        while (true){
            Thread.sleep(1000);
        }
    }

    /**
     * 测试正常返回
     */
    @Test
    @SneakyThrows
    public void testClient() {
        NettyClient nettyClient = NettyClient.getInstance();
        nettyClient.start();
        nettyClient.addConnect("127.0.0.1:8081");
        nettyClient.addController(ClientGroup1Controller.class);

        int i = 0;
        while (true) {
            if (i > 1000) {
                break;
            }

            QueryReq queryReq = new QueryReq();
            queryReq.setAge(12L);
            queryReq.setName("simon");
            nettyClient.send("127.0.0.1:8081", "group1", "getDataReq", queryReq);

            Thread.sleep(1000);
            i++;
        }
    }

    /**
     * 没有异常返回命令，则没有返回
     */
    @Test
    public void testInfo() {
        NettyClient nettyClient = NettyClient.getInstance();
        nettyClient.start();
        nettyClient.addConnect("127.0.0.1:8081");
        nettyClient.addController(ClientGroup1Controller.class);

        NettySender<QueryReq> sender = nettyClient.getConnector("127.0.0.1:8081").asSender("group1", "getInfoReq", QueryReq.class);
        sendWithTime(sender);
    }

    /**
     * 测试有异常返回命令，则client可以接收到服务端的返回
     */
    @Test
    public void testErrorRsp() {
        NettyClient nettyClient = NettyClient.getInstance();
        nettyClient.start();
        nettyClient.addConnect("127.0.0.1:8081");
        nettyClient.addController(ClientGroup1Controller.class);

        NettySender<QueryReq> sender = nettyClient.getConnector("127.0.0.1:8081").asSender("group1", "getInfoReqHaveErr", QueryReq.class);
        sendWithTime(sender);
    }

    @SneakyThrows
    private void sendWithTime(NettySender<QueryReq> sender){
        int i = 0;
        while (true) {
            if (i > 1000) {
                break;
            }

            QueryReq queryReq = new QueryReq();
            queryReq.setAge(12L);
            queryReq.setName("simon");
            sender.send(queryReq);

            Thread.sleep(1000);
            i++;
        }
    }
}
