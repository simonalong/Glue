package com.github.glue.controller;

import com.github.glue.NettyErrorResponse;
import com.github.glue.annotation.CommandMapping;
import com.github.glue.annotation.NettyController;

/**
 * @author shizi
 * @since 2020/3/4 下午2:22
 */
@NettyController("group1")
public class ClientGroup1Controller {

    /**
     * 正常返回
     */
    @CommandMapping(request = "getDataRsp")
    public void getDataRsp(QueryRsp req) {
        System.out.println("好的，收到" + req.toString());
    }

    /**
     * 异常返回
     * 注意：异常返回类型，这里指定类型{@link NettyErrorResponse}
     */
    @CommandMapping(request = "getInfoErr")
    public void queryErr(NettyErrorResponse errorResponse) {
        System.out.println("好的，收到" + errorResponse.toString());
    }
}
