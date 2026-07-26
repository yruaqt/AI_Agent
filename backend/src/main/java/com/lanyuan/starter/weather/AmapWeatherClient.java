package com.lanyuan.starter.weather;

import com.lanyuan.starter.orchard.Orchard;

/** 可替换的高德天气客户端接口，便于测试超时、失败和正常响应。 */
interface AmapWeatherClient {
    WeatherResult query(Orchard orchard, int forecastDays);
}
