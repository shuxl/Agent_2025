//package com.lance.config;
//
//import feign.Logger;
//import feign.Request;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.concurrent.TimeUnit;
//
///**
// * Feign配置类
// *
// * @author lance
// */
//@Configuration
//public class FeignConfig {
//
//    /**
//     * Feign日志级别配置
//     */
//    @Bean
//    public Logger.Level feignLoggerLevel() {
//        return Logger.Level.FULL;
//    }
//
//    /**
//     * Feign请求超时配置
//     */
//    @Bean
//    public Request.Options requestOptions() {
//        return new Request.Options(
//                10, TimeUnit.SECONDS,  // 连接超时时间
//                60, TimeUnit.SECONDS   // 读取超时时间
//        );
//    }
//}
