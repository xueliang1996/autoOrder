package com.myjo.autoOrder.tianma.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CookieUtil {

	public String[] getCookie(){
		String rootPath = System.getProperty("user.dir").replace("\\", "/");
		String filePath = rootPath + "/cookie.txt";
		FileReader fr = null;
		BufferedReader br = null;
		String[] cookie = null;
		try{
			fr = new FileReader(filePath);
			br = new BufferedReader(fr);
			// 读取cookie.txt中的cookie信息
			String cookies = br.readLine();
			cookie = cookies.split("=");
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			try {
				fr.close();
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return cookie;
	}
}
