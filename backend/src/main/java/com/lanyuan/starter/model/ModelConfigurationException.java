package com.lanyuan.starter.model;

/** 模型密钥或配置缺失时抛出的受控异常，避免在启动阶段访问外部服务。 */
public class ModelConfigurationException extends RuntimeException {
    public ModelConfigurationException(String message) {
        super(message);
    }
}
