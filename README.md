# Glue（网络通信框架）

Glue 是一个基于netty设计的通信模块，借鉴spring-mvc用法，设计通讯交互思路，让通讯交互更加简单。

# 快速入门
比如说我们要实现如下的模块交互图，那么使用如下框架就会非常简单，只需要在Controller内部函数之间配置即可
## 数据交互图
上面的例子就可以用如下时序图表示
![](https://cdn.nlark.com/yuque/__puml/b7509b8eea0b7b717410e9d0537998dd.svg#lake_card_v2=eyJjb2RlIjoiQHN0YXJ0dW1sXG5hdXRvbnVtYmVyXG5cbnBhcnRpY2lwYW50IFwi5a6i5oi356uvXCIgYXMgY2xpZW50XG5wYXJ0aWNpcGFudCBcIuacjeWKoeerr1wiIGFzIHNlcnZlclxuXG5hY3RpdmF0ZSBjbGllbnRcbmFjdGl2YXRlIHNlcnZlclxuXG5jbGllbnQgLT4gc2VydmVyOiBnZXREYXRhUmVx77ya5q2j5bi46K-35rGC5pWw5o2uXG5zZXJ2ZXIgLT4gY2xpZW50OiBnZXREYXRhUnNw77ya6K-35rGC55qE6L-U5ZueXG5cbmNsaWVudCAtPiBzZXJ2ZXI6IGdldEluZm9SZXHvvJrmraPluLjor7fmsYJcbnNlcnZlciAtPiBzZXJ2ZXI6IOW8guW4uOaDheWGteS4jei_lOWbnlxubm90ZSByaWdodCBvZiBzZXJ2ZXI6IOW8guW4uOaDheWGteS4jei_lOWbnlxuXG5jbGllbnQgLT4gc2VydmVyOiBnZXRJbmZvUmVxSGF2ZUVycu-8muato-W4uOivt-axglxuc2VydmVyIC0-IGNsaWVudDogZ2V0SW5mb0Vycu-8muW8guW4uOaDheWGteWRveS7pFxuXG5AZW5kdW1sIiwidHlwZSI6InB1bWwiLCJpZCI6IlhrNWk5IiwidXJsIjoiaHR0cHM6Ly9jZG4ubmxhcmsuY29tL3l1cXVlL19fcHVtbC9iNzUwOWI4ZWVhMGI3YjcxNzQxMGU5ZDA1Mzc5OThkZC5zdmciLCJoZWlnaHQiOjQzNiwiY2FyZCI6ImRpYWdyYW0ifQ==)
## 服务端：
```java
/**
 * 启动服务端
 */
@Test
public void testServer() {
    NettyServer server = NettyServer.getInstance().bind("127.0.0.1:8081");
    // 添加消息接收的位置
    server.addController(new ServerGroup1Controller());
    server.start();

    while (true){
        Thread.sleep(1000);
    }
}
```

### 服务端消息处理器Controller
```java
/**
* 用于处理来自客户端的命令
*/
@Slf4j
@NettyController(value = "group1", executor = "fixed")
public class ServerGroup1Controller {

    /**
     * 接收命令：getDataReq，返回时候返回命令：getDataRsp
     */
    @CommandMapping(request = "getDataReq", response = "getDataRsp")
    public QueryRsp getDataReq(QueryReq queryReq) {
        log.info("收到了" + queryReq.toString());
        QueryRsp rsp = new QueryRsp();
        rsp.setData("ok");
        rsp.setSuccess("true");
        return rsp;
    }

    /**
     * 模拟异常返回:接收命令：getInfoReq，返回时候返回命令：getDataRsp，但是异常情况下需要配置异常返回命令，否则不返回数据
     */
    @CommandMapping(request = "getInfoReq", response = "getInfoRsp")
    public QueryRsp getInfo(QueryReq queryReq) {
        log.info("收到了" + queryReq.toString());
        QueryRsp rsp = new QueryRsp();
        rsp.setData("ok");
        rsp.setSuccess("true");
        throw new RuntimeException("异常xxxxxx");
    }

    /**
     * 模拟异常返回: 接收命令：getInfoReqHaveErr，异常情况下返回命令：getInfoErr
     */
    @CommandMapping(request = "getInfoReqHaveErr", response = "getInfoRsp", error = "getInfoErr")
    public QueryRsp getInfoError(QueryReq queryReq) {
        log.info("收到了" + queryReq.toString());
        QueryRsp rsp = new QueryRsp();
        rsp.setData("ok");
        rsp.setSuccess("true");
        throw new RuntimeException("异常bbb");
    }
}
```

## 客户端：
```java
/**
 * 测试正常返回
 */
@Test
@SneakyThrows
public void testClient() {
    NettyClient nettyClient = NettyClient.getInstance();
    nettyClient.start();
    // 添加服务端
    nettyClient.addConnect("127.0.0.1:8081");
    // 添加消息接收位置
    nettyClient.addController(ClientGroup1Controller.class);

    int i = 0;
    while (true) {
        if (i > 1000) {
            break;
        }

        // 客户端发起请求
        QueryReq queryReq = new QueryReq();
        queryReq.setAge(12L);
        queryReq.setName("simon");
        nettyClient.send("127.0.0.1:8081", "group1", "getDataReq", queryReq);

        Thread.sleep(1000);
        i++;
    }
}
```

请求和响应代码
```java
@Data
public class QueryReq implements Serializable {

    private String name;
    private Long age;
}

@Data
public class QueryRsp implements Serializable {

    private String data;
    private String success;
}
```

### 客户端消息处理器
```java
@NettyController("group1")
public class ClientGroup1Controller {

    /**
     * 接收命令：getDataRsp
     */
    @CommandMapping(request = "getDataRsp")
    public void getDataRsp(QueryRsp req) {
        System.out.println("好的，收到" + req.toString());
    }

    /**
     * 异常返回：接收异常命令：getInfoErr
     * 注意：异常返回类型，这里采用内置类型{@link NettyErrorResponse}
     */
    @CommandMapping(request = "getInfoErr")
    public void queryErr(NettyErrorResponse errorResponse) {
        System.out.println("好的，收到" + errorResponse.toString());
    }
}
```

# 详细介绍
## 注解：
这里有两个注解：
### @NettyController 
修饰类，表示一组交互的消息，一个组内的消息处理采用一个线程池，可以自行配置，只有添加了这个注解的Controller才可以解析消息请求和响应
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NettyController {

    /**
     * 对数据请求分组
     * <p>
     * 注意：同group()，都设置，则按照group()为准
     *
     * @return 分组
     */
    String value() default DEFAULT_GROUP_STR;

    /**
     * 对数据请求分组
     *
     * @return 分组
     */
    String group() default DEFAULT_GROUP_STR;

    /**
     * 分组命令中设置的线程池。线程池类型只接受：single, fixed, cache
     * <p>
     * 注意：配置了该参数也建议配置下下面的参数，如果不采用默认，也可以在api中设置自定义
     *
     * @return 线程池类型
     */
    String executor() default "null";

    /**
     * 默认核心线程池大小
     * <p>
     * 注意： 只有参数executor配置，该参数才生效，如果设置为0，则采用当前机器的cpu个数
     *
     * @return 线程池核心个数
     */
    int coreSize() default 0;
}

```

### @CommandMapping
修饰函数
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CommandMapping {

    /**
     * 过滤外部的命令，只有为该字段的时候才会接收
     *
     * @return 请求命令
     */
    String request() default "";

    /**
     * 在函数处理完之后，会将该返回值和对应的命令封装为对应的请求数据给请求方
     * <p>
     * 注意：只有修饰的函数有返回值的时候，该属性才会生效
     *
     * @return 响应命令
     */
    String response() default "";

    /**
     * 异常情况下的命令
     * @return 异常命令
     */
    String error() default "";
}

```

## API
### 服务端：
```java
NettyServer server = NettyServer.getInstance().bind("127.0.0.1:8081");
// 添加对应的controller即可
server.addController(new ServerGroup1Controller());
server.start();
```

### 客户端
有两种方式发送数据
#### 方式1
构造
```java

NettyClient nettyClient = NettyClient.getInstance();
nettyClient.start();
nettyClient.addConnect("127.0.0.1:8081");
nettyClient.addController(ClientGroup1Controller.class);

QueryReq queryReq = new QueryReq();
queryReq.setAge(12L);
queryReq.setName("simon");
// 指定 group 指定cmd和 对应的消息体，其中消息体可以随意，但是接受的controller参数必须保持为其父类或者同类
nettyClient.send("127.0.0.1:8081", "group1", "getDataReq", queryReq);
```

send的api
```java
// NettyClient

// 同步发送
public Boolean send(String addr, String group, String cmd, Object data) {}
public Boolean send(String addr, String cmd, Object data) {}
public Boolean send(String addr, NettyCommand request) {}

// 异步发送
public void sendAsync(String addr, NettyCommand request, Runnable successCall, Runnable failCall) {}
```

#### 方式2
构造
```java
NettyClient nettyClient = NettyClient.getInstance();
nettyClient.start();
nettyClient.addConnect("127.0.0.1:8081");
nettyClient.addController(ClientGroup1Controller.class);

// 构造发射器
NettySender<QueryReq> sender = nettyClient.getConnector("127.0.0.1:8081").asSender("group1", "getInfoReq", QueryReq.class);
QueryReq queryReq = new QueryReq();
queryReq.setAge(12L);
queryReq.setName("simon");
// 发送数据（只能发送指定类型 QueryReq 的数据）
sender.send(queryReq);

```

发送api

```java
NettySender

// 同步发送
public Boolean send(T data) {}
// 异步发送
public void sendAsync(T data, Runnable successCall, Runnable failCall) {}
```

