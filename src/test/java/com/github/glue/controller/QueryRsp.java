package com.github.glue.controller;

import lombok.Data;

import java.io.Serializable;

/**
 * @author shizi
 * @since 2020/3/4 下午8:02
 */
@Data
public class QueryRsp implements Serializable {

    private String data;
    private String success;
}
