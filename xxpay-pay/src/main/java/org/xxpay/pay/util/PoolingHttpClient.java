package org.xxpay.pay.util;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.stereotype.Component;
import org.xxpay.pay.util.schedule.FixedRateSchedule;
import org.xxpay.pay.util.schedule.impl.FixedRateScheduleImpl;


/**
 * 
 * @author arganzheng
 *
 */
@Component
public class PoolingHttpClient {

	private static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 200;
	private static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = DEFAULT_MAX_TOTAL_CONNECTIONS;
	private static final int DEFAULT_CONNECTION_TIMEOUT_MILLISECONDS = (20 * 1000);
	private static final int DEFAULT_READ_TIMEOUT_MILLISECONDS = (20 * 1000);
	private static final int DEFAULT_WAIT_TIMEOUT_MILLISECONDS = (20 * 1000);
	private static final int DEFAULT_KEEP_ALIVE_MILLISECONDS = (5 * 60 * 1000);
	private static final int DEFAULT_RETRY_COUNT = 3;
	private static final String DEFAULT_CHARSET = "UTF-8";
	private int keepAlive = DEFAULT_KEEP_ALIVE_MILLISECONDS;
	private int maxTotalConnections = DEFAULT_MAX_TOTAL_CONNECTIONS;
	private int maxConnectionsPerRoute = DEFAULT_MAX_CONNECTIONS_PER_ROUTE;
	private int connectTimeout = DEFAULT_CONNECTION_TIMEOUT_MILLISECONDS;
	private int readTimeout = DEFAULT_READ_TIMEOUT_MILLISECONDS;
	private int waitTimeout = DEFAULT_WAIT_TIMEOUT_MILLISECONDS;
	private int retries = DEFAULT_RETRY_COUNT;
	private PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
	private CloseableHttpClient httpClient = null;

	public PoolingHttpClient() {
		// Increase max total connection
		connManager.setMaxTotal(maxTotalConnections);
		// Increase default max connection per route
		connManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);

		// config timeout
		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(connectTimeout)
				.setConnectionRequestTimeout(waitTimeout)
				.setSocketTimeout(readTimeout).build();

		httpClient = HttpClients.custom()
				.setRetryHandler(new DefaultHttpRequestRetryHandler(retries, true))
				.setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE)
				.setConnectionManager(connManager)
				.setDefaultRequestConfig(config).build();

		// detect idle and expired connections and close them
		runIdleConnectionMonitor(connManager);
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

	private static String getResponseCharset(String ctype) {
		String charset = DEFAULT_CHARSET;

		if (!StringUtils.isEmpty(ctype)) {
			String[] params = ctype.split(";");
			for (String param : params) {
				param = param.trim();
				if (param.startsWith("charset")) {
					String[] pair = param.split("=", 2);
					if (pair.length == 2) {
						if (!StringUtils.isEmpty(pair[1])) {
							charset = pair[1].trim();
						}
					}
					break;
				}
			}
		}
		return charset;
	}

	public CloseableHttpResponse execute(HttpUriRequest request) throws IOException {
		CloseableHttpResponse response;
		response = httpClient.execute(request);
		return response;
	}

	public void shutdown() throws IOException {
		httpClient.close();
	}

	public int getKeepAlive() {
		return keepAlive;
	}

	public void setKeepAlive(int keepAlive) {
		this.keepAlive = keepAlive;
	}

	public int getMaxTotalConnections() {
		return maxTotalConnections;
	}

	public void setMaxTotalConnections(int maxTotalConnections) {
		this.maxTotalConnections = maxTotalConnections;
	}

	public int getMaxConnectionsPerRoute() {
		return maxConnectionsPerRoute;
	}

	public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
		this.maxConnectionsPerRoute = maxConnectionsPerRoute;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public int getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	public int getWaitTimeout() {
		return waitTimeout;
	}

	public void setWaitTimeout(int waitTimeout) {
		this.waitTimeout = waitTimeout;
	}

	public PoolingHttpClientConnectionManager getConnManager() {
		return connManager;
	}

	public void setConnManager(PoolingHttpClientConnectionManager connManager) {
		this.connManager = connManager;
	}

	public int getRetryCount() {
		return retries;
	}

	public void setRetryCount(int retries) {
		checkArgument(retries >= 0);
		this.retries = retries;
	}
}