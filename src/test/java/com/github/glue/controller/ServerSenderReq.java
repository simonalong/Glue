package com.github.glue.controller;

import lombok.Data;

import java.io.Serializable;

/**
 * @author shizi
 * @since 2020/3/13 上午12:03
 */
@Data
public class ServerSenderReq implements Serializable {

    private String name;
    private Integer num;
}
