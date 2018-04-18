package com.myjo.autoOrder.tianma.service;

public interface TransformDay {
	// 判断当前日期是星期几<br>
	public int dayForWeek(String pTime);

	// 判断时间date1是否在时间date2之前
	public boolean isDateBefore(String date);

	// 字符串时间格式转换为毫秒值,进行减法判断
	public boolean getMillisecond(String time1, long time2);

	public boolean getMillisecond(String time1);

	// 获取当前日期
	public String getCurrentTime();

	// 获取当前时间的前一天
	public String getBeforeCurrentDay();

	// 获取当前时间的前一个月
	public String getBeforeCurrentMonth();
}
