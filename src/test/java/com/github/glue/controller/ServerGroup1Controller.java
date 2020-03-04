package com.github.glue.controller;

import com.github.glue.annotation.CommandMapping;
import com.github.glue.annotation.NettyController;
import lombok.extern.slf4j.Slf4j;

/**
 * @author shizi
 * @since 2020/3/4 下午2:21
 */
@Slf4j
@NettyController(value = "group1", executor = "fixed")
public class ServerGroup1Controller {

    /**
     * 请求命令
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
     * 模拟异常返回
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
     * 模拟异常返回
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
