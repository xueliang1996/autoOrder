package com.myjo.autoOrder.tianma.domain;

import org.springframework.stereotype.Component;

@Component
public class TaobaoAPI {
	private String taobaoUrl = "http://gw.api.taobao.com/router/rest";
	private String appkey = "23279400";
	private String secret = "7cb1d50fc70c7548b31d414c2adbae06";
	private String sessionKey = "610061860172d8cb9f255a51961f5d4885daaff6a5bad72738840638";

	public String getTaobaoUrl() {
		return taobaoUrl;
	}

	public void setTaobaoUrl(String taobaoUrl) {
		this.taobaoUrl = taobaoUrl;
	}

	public String getAppkey() {
		return appkey;
	}

	public void setAppkey(String appkey) {
		this.appkey = appkey;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public String getSessionKey() {
		return sessionKey;
	}

	public void setSessionKey(String sessionKey) {
		this.sessionKey = sessionKey;
	}

}
