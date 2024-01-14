package com.yjy.rpc.core.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 应用名（接口）+服务名（实现类）+版本号+地址+端口
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInfo {
    /**
     * 应用名称 provider-1
     */
    private String appName;

    /**
     * 服务名称：服务名(接口名)-版本号
     */
    private String serviceName;

    /**
     * 版本号
     */
    private String version;

    /**
     * 服务提供方主机地址
     */
    private String address;

    /**
     * 服务提供方端口号
     */
    private Integer port;

}
