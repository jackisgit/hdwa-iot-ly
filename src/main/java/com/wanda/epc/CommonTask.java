package com.wanda.epc;

import com.wanda.epc.device.OpcCommunication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * @program: iot_epc
 * @description: ba采集
 * @author: liuruishuo
 * @create: 2022-11-08 17:07
 **/
@Configuration
@EnableScheduling
public class CommonTask {

    @Autowired
    private OpcCommunication device;// opc

    @Scheduled(cron = "0/15 * * * * ?")
    public boolean process() throws Exception {
        device.processData();
        return true;
    }

    /**
     * 发送心跳并断线重连，单线程操作防止异步多线程造成重复连接问题
     *
     * @throws Exception
     */
    @Scheduled(cron = "0/60 * * * * ?")
    public void sendHeartbeat() {
        device.sendHeartbeat();
    }

}
