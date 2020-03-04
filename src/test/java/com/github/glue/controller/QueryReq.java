package com.github.glue.controller;

import lombok.Data;

import java.io.Serializable;

/**
 * @author shizi
 * @since 2020/3/4 下午3:19
 */
@Data
public class QueryReq implements Serializable {

    private String name;
    private Long age;
}
