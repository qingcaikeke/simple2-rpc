package com.yjy.rpc.core.extension;

import java.lang.annotation.*;
/**
 * SPI 注解，被标注的类表示为需要加载的扩展类接口
 */
//todo 注解的适用范围、生命周期和是否应该包含在 Javadoc 文档中
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME) //retention：保留  -> 注解在运行时保留，因此可以通过反射获取
@Documented
public @interface SPI {


}
