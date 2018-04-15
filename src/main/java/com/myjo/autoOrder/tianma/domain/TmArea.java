package com.myjo.autoOrder.tianma.domain;

import org.springframework.stereotype.Component;

/**
 * 城市信息
 */
@Component
public class TmArea {

	// {
	// "id": 110101,
	// "ppname": null,
	// "flag": 0,
	// "level": 4,
	// "ppid": null,
	// "zipcode": "100010",
	// "name": "东城区",
	// "pid": 110100
	// }

	private long id;
	private String ppname;
	private int flag;
	private int level;
	private long ppid;
	private String zipcode;
	private String name;
	private long pid;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getPpname() {
		return ppname;
	}

	public void setPpname(String ppname) {
		this.ppname = ppname;
	}

	public int getFlag() {
		return flag;
	}

	public void setFlag(int flag) {
		this.flag = flag;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public long getPpid() {
		return ppid;
	}

	public void setPpid(long ppid) {
		this.ppid = ppid;
	}

	public String getZipcode() {
		return zipcode;
	}

	public void setZipcode(String zipcode) {
		this.zipcode = zipcode;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getPid() {
		return pid;
	}

	public void setPid(long pid) {
		this.pid = pid;
	}
}
