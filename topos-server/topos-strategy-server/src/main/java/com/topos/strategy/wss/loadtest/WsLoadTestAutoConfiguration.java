package com.topos.strategy.wss.loadtest;

import com.topos.strategy.controller.WsLoadTestController;
import com.topos.strategy.wss.props.WsLoadTestProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty(prefix = "topos.wss.load-test", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(WsLoadTestProperties.class)
@Import(WsLoadTestController.class)
public class WsLoadTestAutoConfiguration {
}
