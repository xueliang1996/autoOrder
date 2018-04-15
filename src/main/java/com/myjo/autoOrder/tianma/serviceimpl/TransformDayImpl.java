package com.myjo.autoOrder.tianma.serviceimpl;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.myjo.autoOrder.tianma.service.TransformDay;

@Service
public class TransformDayImpl implements TransformDay {
	@Value("${time}")
	private long time;

	/**
	 * 判断当前日期是星期几<br>
	 * <br>
	 * 
	 * @param pTime
	 *            修要判断的时间<br>
	 * @return dayForWeek 判断结果<br>
	 * @Exception 发生异常<br>
	 */
	@Override
	public int dayForWeek(String pTime) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Calendar c = Calendar.getInstance();
		try {
			c.setTime(format.parse(pTime));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int dayForWeek = 0;
		if (c.get(Calendar.DAY_OF_WEEK) == 1) {
			dayForWeek = 7;
		} else {
			dayForWeek = c.get(Calendar.DAY_OF_WEEK) - 1;
		}
		return dayForWeek;
	}

	/*
	 * 
	 * 判断时间date是否在时间compareDate之前
	 * 
	 */
	@Override
	public boolean isDateBefore(String date) {
		DateFormat df = new SimpleDateFormat("HH:mm:ss");// 创建日期转换对象HH:mm:ss为时分秒
		String compareDate = "04:00:00";
		try {
			Date dt1 = df.parse(date);// 将字符串转换为date类型
			Date dt2 = df.parse(compareDate);
			if (dt1.getTime() < dt2.getTime()) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/*
	 * 
	 * 字符串时间格式转换为毫秒值,进行减法判断
	 * 
	 */
	@Override
	public boolean getMillisecond(String time1, long time2) {
		Calendar calendar = Calendar.getInstance();
		try {
			calendar.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(time1));
			if (time2 - calendar.getTimeInMillis() > time) {
				return true;
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	/*
	 * 
	 * 获取当前日期
	 * 
	 */
	@Override
	public String getCurrentTime() {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String rt = sdf.format(date);
		return rt;
	}
}
