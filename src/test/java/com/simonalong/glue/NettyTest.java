package com.simonalong.glue;

import com.simonalong.glue.controller.ClientGroup1Controller;
import com.simonalong.glue.controller.QueryReq;
import com.simonalong.glue.controller.ServerGroup1Controller;
import com.simonalong.glue.controller.ServerSenderReq;
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
        GlueServer server = GlueServer.getInstance();
        server.bind("127.0.0.1:8081");
        server.addController(new ServerGroup1Controller());
        server.start();

        while (true) {
            Thread.sleep(1000);
        }
    }

    /**
     * 测试正常返回
     */
    @Test
    @SneakyThrows
    public void testClient() {
        GlueClient client = GlueClient.getInstance();
        client.addConnect("127.0.0.1:8081");
        client.addController(ClientGroup1Controller.class);
        client.start();

        int i = 0;
        while (true) {
            if (i > 1000) {
                break;
            }

            QueryReq queryReq = new QueryReq();
            queryReq.setAge(12L);
            queryReq.setName("simon");
            client.send("127.0.0.1:8081", "group1", "getDataReq", queryReq);

            Thread.sleep(1000);
            i++;
        }
    }

    /**
     * 没有异常返回命令，则没有返回
     */
    @Test
    public void testInfo() {
        GlueClient glueClient = GlueClient.getInstance();
        glueClient.addConnect("127.0.0.1:8081");
        glueClient.addController(ClientGroup1Controller.class);
        glueClient.start();

        NettySender<QueryReq> sender = glueClient.getSender("127.0.0.1:8081", "group1", "getInfoReq", QueryReq.class);
        sendWithTime(sender);
    }

    /**
     * 测试有异常返回命令，则client可以接收到服务端的返回
     */
    @Test
    public void testErrorRsp() {
        GlueClient glueClient = GlueClient.getInstance();
        glueClient.start();
        glueClient.addConnect("127.0.0.1:8081");
        glueClient.addController(ClientGroup1Controller.class);

        NettySender<QueryReq> sender = glueClient.getSender("127.0.0.1:8081", "group1", "getInfoReqHaveErr", QueryReq.class);
        sendWithTime(sender);
    }

    @SneakyThrows
    private void sendWithTime(NettySender<QueryReq> sender) {
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

    /**
     * 测试服务端的数据发送
     */
    @Test
    @SneakyThrows
    public void testServerSender() {
        GlueServer server = GlueServer.getInstance();
        server.bind("127.0.0.1:8081");
        server.addController(new ServerGroup1Controller());
        server.start();

        while (true) {
            Thread.sleep(1000);
            ServerSenderReq senderReq = new ServerSenderReq();
            senderReq.setNum(11);
            senderReq.setName("simon");
            // 服务端广播消息
            server.sendAll("group1", "rsvServer", senderReq);
        }
    }
}
