package com.topos.strategy.wss.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * WebSocket 端点、心跳、广播与资源上限（生产按连接数与 LB 超时调参）。
 */
@ConfigurationProperties(prefix = "topos.wss")
public class WsProperties {

	/**
	 * 注册端点路径（不含 context-path）。
	 */
	private String endpointPath = "/ws/v1/strategy";

	/**
	 * 握手后允许的 Origin 模式；空则仅同源。
	 */
	private List<String> allowedOriginPatterns = new ArrayList<>(List.of("*"));

	/**
	 * JWT 中存放客户端标识的 claim 名；缺省回退为 {@code sub}（须非空且长度不超过 {@link #maxClientIdLength}）。
	 */
	private String clientIdClaim = "clientId";

	/**
	 * 允许的 clientId 最大字符长度（防止异常长 claim）。
	 */
	private int maxClientIdLength = 256;

	/**
	 * 单 JVM 最大并发连接；0 表示不限制。
	 */
	private int maxSessions = 20_000;

	/**
	 * 同一 clientId 重复建连时是否关闭旧连接（true：踢旧；false：拒绝新连接）。
	 */
	private boolean closePreviousSessionOnDuplicate = true;

	/**
	 * 服务端 WebSocket ping 间隔（毫秒）；应明显小于 LB/容器 idle 超时。
	 */
	private long pingIntervalMs = 25_000;

	/**
	 * 单次广播时每批发送的会话数，避免长时间占满 executor。
	 */
	private int broadcastBatchSize = 500;

	/**
	 * 单会话单次 {@code sendMessage} 最长阻塞时间（毫秒），超时则移除会话。
	 */
	private long sendTimeoutMs = 5_000;

	/**
	 * 文本消息最大字节（握手后由容器校验）。
	 */
	private int maxTextMessageBufferSize = 65_536;

	/**
	 * 会话最大空闲（毫秒）；0 使用容器默认。建议设为 ping 间隔的 3 倍以上。
	 */
	private long maxSessionIdleTimeoutMs = 120_000;

	public String getEndpointPath() {
		return endpointPath;
	}

	public void setEndpointPath(String endpointPath) {
		this.endpointPath = endpointPath;
	}

	public List<String> getAllowedOriginPatterns() {
		return allowedOriginPatterns;
	}

	public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
		this.allowedOriginPatterns = allowedOriginPatterns;
	}

	public String getClientIdClaim() {
		return clientIdClaim;
	}

	public void setClientIdClaim(String clientIdClaim) {
		this.clientIdClaim = clientIdClaim;
	}

	public int getMaxClientIdLength() {
		return maxClientIdLength;
	}

	public void setMaxClientIdLength(int maxClientIdLength) {
		this.maxClientIdLength = maxClientIdLength;
	}

	public int getMaxSessions() {
		return maxSessions;
	}

	public void setMaxSessions(int maxSessions) {
		this.maxSessions = maxSessions;
	}

	public boolean isClosePreviousSessionOnDuplicate() {
		return closePreviousSessionOnDuplicate;
	}

	public void setClosePreviousSessionOnDuplicate(boolean closePreviousSessionOnDuplicate) {
		this.closePreviousSessionOnDuplicate = closePreviousSessionOnDuplicate;
	}

	public long getPingIntervalMs() {
		return pingIntervalMs;
	}

	public void setPingIntervalMs(long pingIntervalMs) {
		this.pingIntervalMs = pingIntervalMs;
	}

	public int getBroadcastBatchSize() {
		return broadcastBatchSize;
	}

	public void setBroadcastBatchSize(int broadcastBatchSize) {
		this.broadcastBatchSize = broadcastBatchSize;
	}

	public long getSendTimeoutMs() {
		return sendTimeoutMs;
	}

	public void setSendTimeoutMs(long sendTimeoutMs) {
		this.sendTimeoutMs = sendTimeoutMs;
	}

	public int getMaxTextMessageBufferSize() {
		return maxTextMessageBufferSize;
	}

	public void setMaxTextMessageBufferSize(int maxTextMessageBufferSize) {
		this.maxTextMessageBufferSize = maxTextMessageBufferSize;
	}

	public long getMaxSessionIdleTimeoutMs() {
		return maxSessionIdleTimeoutMs;
	}

	public void setMaxSessionIdleTimeoutMs(long maxSessionIdleTimeoutMs) {
		this.maxSessionIdleTimeoutMs = maxSessionIdleTimeoutMs;
	}
}
