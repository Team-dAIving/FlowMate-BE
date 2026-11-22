package kr.co.daeun.flowmatebe.global.github.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GithubAppProperties.class)
public class GithubAppConfig {
}
