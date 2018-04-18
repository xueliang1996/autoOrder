package com.myjo.autoOrder.tianma.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.Ostermiller.util.CSVPrint;
import com.Ostermiller.util.CSVPrinter;

public class CsvFileUtil {
	private CSVPrint csvPrint;

	/**
	 * 
	 * @param fileName
	 *            文件路径
	 * @param append
	 *            是否支持追加
	 * @throws IOException
	 */
	public CsvFileUtil(String fileName, boolean append) throws IOException {
		File file = new File(fileName);
		if (!file.exists()) {
			file.createNewFile();
			csvPrint = new CSVPrinter(new FileWriter(fileName, append));
			init();
		} else {
			csvPrint = new CSVPrinter(new FileWriter(fileName, append));
			if (!append) {
				init();
			}
		}

	}

	public void init() throws IOException {
		write(new String[] { "淘宝订单编号", "买家昵称", "实付金额", "天马订单编号", "货号", "尺码", "货源名称", "代理价", "库存数量", "配货率", "发货时效",
				"付款时间", "库存更新时间", "下单方式" });
	}

	public void write(String[] values) throws IOException {
		csvPrint.writeln(values);
	}

	public void flush() {
		try {
			csvPrint.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void close() {
		try {
			csvPrint.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}