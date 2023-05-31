/*
 * Project Name: dpass-iot File Name: OPCAutoReconnectListener Package Name: cn.dpass.iot.opc.service Date: 2021/7/5
 * 14:48 Copyright (c) 2021,All Rights Reserved.
 */
package com.wanda.epc.device;

import org.openscada.opc.lib.da.AutoReconnectListener;
import org.openscada.opc.lib.da.AutoReconnectState;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * @dlassname: OPCAutoReconnectListener
 * @description: 重连监听
 * @date: 2023-05-29 14:48
 */
@Slf4j
@Component
public class OPCAutoReconnectListener implements AutoReconnectListener {

    @Override
    public void stateChanged(AutoReconnectState state) {
        log.info("[链路状态:]{}", state);
    }
}
