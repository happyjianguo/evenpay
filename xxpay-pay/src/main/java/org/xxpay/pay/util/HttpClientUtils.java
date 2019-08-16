package org.xxpay.pay.util;

import org.xxpay.pay.util.schedule.FixedRateSchedule;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.xxpay.pay.util.schedule.impl.FixedRateScheduleImpl;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


/**
 * @Author : ChinaLHR
 * @Date : Create in 22:28 2018/10/16
 * @Email : 13435500980@163.com
 */
public class HttpClientUtils {

    private static HttpClient retryHttpClient;


    private static HttpClient getRetryHttpClient() {
        if (Objects.isNull(retryHttpClient)) {
            retryHttpClient = getCloseableHttpClient(true);
        }
        return retryHttpClient;
    }




    public static CloseableHttpClient getCloseableHttpClient(boolean isRetry) {
        // 长连接保持30秒
        PoolingHttpClientConnectionManager pollingConnectionManager = new PoolingHttpClientConnectionManager(30, TimeUnit.SECONDS);
        // 总连接数
        pollingConnectionManager.setMaxTotal(200);
        // 默认同路由的并发数
        pollingConnectionManager.setDefaultMaxPerRoute(100);
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        httpClientBuilder.setConnectionManager(pollingConnectionManager);
        // 重试次数，默认是3次，设置为2次，没有开启
        if (isRetry) {
            httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(2, true));
        } else {
            httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(0, false));
        }
        // 保持长连接配置，需要在头添加Keep-Alive
        httpClientBuilder.setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE);

        CloseableHttpClient httpClient = httpClientBuilder.build();

        runIdleConnectionMonitor(pollingConnectionManager);
        return httpClient;
    }

    private static final Long IDLE_INITIALDELAY = 1000L;
    private static final Long IDLE_PERIOD = 1000L;



    /**
     * 如果连接在服务器端关闭，则客户端连接无法检测到连接状态的变化（并通过关闭socket 来做出适当的反应）,
     * 需要使用定时监控清理实现关闭
     * 参考：https://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html  2.5 Connection eviction policy
     *
     * @param clientConnectionManager
     */
    private static void runIdleConnectionMonitor(HttpClientConnectionManager clientConnectionManager) {
        FixedRateSchedule schedule = new FixedRateScheduleImpl();
        schedule.setPoolTag("IDLE_CONNECTION_MONITOR_POOL");
        schedule.init();
        schedule.schedule(() -> {
            //关闭过期的链接
            clientConnectionManager.closeExpiredConnections();
            //关闭闲置超过30s的链接
            clientConnectionManager.closeIdleConnections(30, TimeUnit.SECONDS);
        }, IDLE_INITIALDELAY, IDLE_PERIOD, TimeUnit.MILLISECONDS);
    }

    /**
     * 配置keep-alive
     * @return
     */
    private static ConnectionKeepAliveStrategy getConnectionKeepAliveStrategy(){
        return (response, context) -> {
            HeaderElementIterator it = new BasicHeaderElementIterator
                    (response.headerIterator(HTTP.CONN_KEEP_ALIVE));
            while (it.hasNext()) {
                HeaderElement he = it.nextElement();
                String param = he.getName();
                String value = he.getValue();
                if (value != null && param.equalsIgnoreCase
                        ("timeout")) {
                    return Long.parseLong(value) * 1000;
                }
            }
            return 60 * 1000;//如果没有约定，则默认定义时长为60s
        };
    }
}
