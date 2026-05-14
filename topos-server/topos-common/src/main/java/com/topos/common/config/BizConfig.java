package com.topos.common.config;


import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 配置中心
 * 动态配置类
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "biz")
public class BizConfig {


	private String awsAccessKeyId;
	private String awsSecretAccessKey;

	private String awss3AccessKeyId;
	private String awss3SecretAccessKey;
	private String bucketName;
	private String s3Domain;

	private Boolean sesClient = true;

	private List<String> symbols;

	private Boolean serverPaysGas;
	private String gasPayerSecretKey;
	private Map<String, String> gasSolanaFixedMap = new HashMap<>();
	private String gasSolanaPriority;
	private String tradableTransferDefaultMint = "9JLMhRhK9Rhqbd91i2DjmV8tgDT3RQkPFi2C2rsrP3aN";
	private String tradableTransferRpcUrl = "https://api.mainnet-beta.solana.com";
	private String tradableTransferRwaPoolProgramId = "B4PjoftwGEbc6qFW1ckpzqYgEGmBJtfqn233WZi1zmgL";
	private String tradableTransferHookProgramId = "FtV8crKdZ2LJmCPSRoDBXpoVRBXWR7aV78YPTmduMLtH";

	//*******************kyb登录*******************8//
	private String loginEmailTemplate;
	private String submitsApplicationTemplate;
	private Long loginVerifiyTimeOut;
	private String loginTime;
	private Integer emailTokenTimeOut;
	private String kybLarkBotToken;
	private String boUrl;
	private Integer expireSeconds;
	private String kybPortalLink;

	//**************************order book ******************//

	private Map<String,String> symbolMap;
	private Map<String,String> recipientPriceTokenAccountMap;

	private Boolean heliusIsVerifySign;

	private BigDecimal priceIncrease;
	private String poolsUrl;

	// order take-only control
	private List<String> takeOnlyWallets;
	private BigDecimal takeOnlyPriceFloor;
	private BigDecimal takeOnlyPriceCeiling;
	private Boolean takeSquadsEnabled = Boolean.FALSE;
	private String takeTreasuryWallet;
	private String takeSquadsMultisigAddress;
	private String takeSquadsVaultAddress;
	private BigDecimal takeSingleDirectLimit = new BigDecimal("10000");
	private BigDecimal takeDailyDirectLimit = new BigDecimal("100000");

	private List<String> floorPriceAddress;
	// 对应 Nacos 中的字符串字段，禁用LomBok的Setter
	@Setter(AccessLevel.NONE)
	private String floorPriceAddressMapJson;

	private volatile Map<String,String> floorPriceAddressMerchantMap = new HashMap<>();
	// 只有在 Nacos 更新该字符串时，Spring 才会调用这个 setter
	public void setFloorPriceAddressMapJson(String floorPriceAddressJson) {
		this.floorPriceAddressMapJson = floorPriceAddressJson;
		log.info("开始刷新floorPriceAddressMerchantMap，floorPriceAddressJson={}", floorPriceAddressJson);
		this.refreshMap(this.floorPriceAddressMapJson); // 字符串变了，立刻刷新 Map
	}

	private void refreshMap(String floorPriceAddressMapJson) {
		if (JSONUtil.isTypeJSON(floorPriceAddressMapJson)) {
			try {
				this.floorPriceAddressMerchantMap = JSONUtil.toBean(
						floorPriceAddressMapJson,
						new TypeReference<Map<String, String>>() {},
						false
				);
				log.info("Nacos 配置floorPriceAddressMapJson已更新，当前 floorPriceAddressMap= {}", this.floorPriceAddressMerchantMap);
			} catch (Exception e) {
				log.error("Nacos 配置floorPriceAddressMapJson 格式错误，解析失败: {}", floorPriceAddressMapJson);
			}
		}
	}
	/**
	 * KMS executor wallet for DIRECT take.
	 * When configured with SpendingLimit, platform will withdraw needed funds to this wallet and execute take directly.
	 */
	private String takeExecutorWallet;

	/**
	 * Squads spending limit config: mintAddress -> spendingLimit account (PDA) base58.
	 * Used to withdraw funds from multisig vault to takeExecutorWallet without proposal.
	 */
	private Map<String, String> takeSpendingLimitByMint;

	/**
	 * Squads vault index for spending limit withdrawals.
	 */
	private Integer takeSpendingLimitVaultIndex = 0;

	/**
	 * Merchant pool fallback price map.
	 * Key = AccessKeyId + productId (string concat). Value = price.
	 */
	private Map<String, BigDecimal> merchantPoolFallbackPriceMap = new HashMap<>();

	/**
	 * Merchant fallback maker wallet map.
	 * Key = AccessKeyId + productId (string concat). Value = wallet address.
	 */
	private Map<String, String> merchantPoolFallbackWalletMap = new HashMap<>();

	/**
	 * If true, matching requires both orders to have same accessKeyId
	 * (or both empty). Useful when isolating liquidity per merchant.
	 */
	private Boolean merchantMatchByAccessKeyEnabled = Boolean.FALSE;

	private final AtomicLong solanaLoadBalance = new AtomicLong();
	private List<String> solanaRpcList;

	public String getSolanaRpc() {
		return this.getSolanaRpcList().get((int) (solanaLoadBalance.incrementAndGet() %
				this.getSolanaRpcList().size()));
	}

	private List<String> ankrAdvanceEndpoint;
	private final AtomicLong anchorLoadBalance = new AtomicLong();

	/**
	 * Moralis Solana Gateway API key for holdersCount query
	 */
	private String moralisApiKey;

	/**
	 * Trade whitelist addresses (KYC passed).
	 * Only addresses in this list can access trading endpoints.
	 */
	private List<String> tradeWhitelistAddresses;
	/**
	 * super user can place order at any price
	 */
	private List<String> superUsers;

	/**
	 * Auto price sync target product id.
	 */
	private Integer autoPriceSyncProductId;

	/**
	 * Auto price sync mint on Solana.
	 */
	private String autoPriceSyncMint;

	/**
	 * Multi-token auto price sync list.
	 * Each item should include productId and mint.
	 */
	private List<AutoPriceSyncItem> autoPriceSyncItems;

	/**
	 * Maximum allowed price change ratio for auto sync.
	 * e.g. 0.1 means 10%.
	 */
	private BigDecimal autoPriceSyncMaxChangeRatio;

	/**
	 * Optional lark bot token for auto price sync alerts.
	 * If empty, fallback to larkBotToken.
	 */
	private String autoPriceSyncLarkToken;

	/**
	 * Order remaining quantity monitor list.
	 * Used for fallback/liquidity orders alerting in auto price sync job.
	 */
	private List<OrderRemainingMonitorItem> orderRemainingMonitorItems;

	@Data
	public static class AutoPriceSyncItem {
		private Integer productId;
		private String mint;
		/**
		 * Per-item max change ratio. e.g. 0.05 means 5%.
		 * If empty, fallback to biz.autoPriceSyncMaxChangeRatio.
		 */
		private BigDecimal maxChangeRatio;
	}

	@Data
	public static class OrderRemainingMonitorItem {
		private Integer orderId;
		/**
		 * Alert when order remaining quantity is less than this value.
		 */
		private BigDecimal threshold;
		/**
		 * Optional display name in alert message.
		 */
		private String name;
		/**
		 * Optional per-monitor lark bot token. If empty, fallback to autoPriceSyncLarkToken/larkBotToken.
		 */
		private String larkToken;
	}

	public String getAnkrAdvanceNode() {
		return this.getAnkrAdvanceEndpoint().get((int) (anchorLoadBalance.incrementAndGet() %
				this.getAnkrAdvanceEndpoint().size()));
	}

}
