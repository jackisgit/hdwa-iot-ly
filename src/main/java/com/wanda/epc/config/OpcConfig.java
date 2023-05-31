package com.wanda.epc.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @program: iot_dapc
 * @description: opc通讯配置类
 * @author: LianYanFei
 * @create: 2022-09-13 10:30
 **/
@Data
@Component
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties(prefix = "opc")
public class OpcConfig {

    private String host;
    private String domain;
    private String user;
    private String password;
    private String clsid;
    private String progId;

}
