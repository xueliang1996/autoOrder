package com.myjo.autoOrder.tianma.domain;

import org.springframework.stereotype.Component;

@Component
public class TaobaoAPI {
	private String taobaoUrl = "http://gw.api.taobao.com/router/rest";
	private String appkey = "";
	private String secret = "";
	private String sessionKey = "";

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
