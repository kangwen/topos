package com.topos.strategy.wss;

import com.topos.strategy.wss.session.WsSessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;

/**
 * 供业务代码注入：主动断开 WebSocket 长连接（封装 {@link WsSessionRegistry}）。
 */
@Service
public class WsConnectionService {

	private final WsSessionRegistry sessionRegistry;

	public WsConnectionService(WsSessionRegistry sessionRegistry) {
		this.sessionRegistry = sessionRegistry;
	}

	/**
	 * 按客户端标识主动断开连接（关闭码 4402，原因 server disconnect）。
	 *
	 * @return 该 clientId 是否对应一条已存在的连接并已关闭
	 */
	public boolean disconnectClient(String clientId) {
		return sessionRegistry.disconnectByClientId(clientId);
	}

	/**
	 * 主动断开并指定关闭状态（便于客户端区分踢线、封禁、维护等）。
	 */
	public boolean disconnectClient(String clientId, CloseStatus closeStatus) {
		return sessionRegistry.disconnectByClientId(clientId, closeStatus);
	}

	/**
	 * 断开本实例上全部 WebSocket（例如发布前清空本地会话表）。
	 *
	 * @return 关闭的会话条数
	 */
	public int disconnectAllClients() {
		return sessionRegistry.disconnectAllClients();
	}

	/**
	 * 全部断开并自定义关闭状态。
	 */
	public int disconnectAllClients(CloseStatus closeStatus) {
		return sessionRegistry.disconnectAllClients(closeStatus);
	}

	/**
	 * 当前实例已登记连接数。
	 */
	public int activeSessionCount() {
		return sessionRegistry.size();
	}
}
