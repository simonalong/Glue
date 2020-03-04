package com.github.glue.event;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author shizi
 * @since 2020/3/3 下午6:23
 */
@Data
@AllArgsConstructor
public class CommandEvent {

    private String group = "_default_";
    private String cmd;

    public CommandEvent(String cmd) {
        this.cmd = cmd;
    }
}
