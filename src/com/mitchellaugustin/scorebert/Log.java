package com.mitchellaugustin.scorebert;

public class Log {
	
	public static void info(String info){
		System.out.println("[INFO] " + info);
		//SaveFile.saveToSonarDir("log.txt" , "\n[" + new SimpleDateFormat("M/d/y hh:mm a").format(new Date()) + "]" + "[INFO]" + info);
	}
	public static void warning(String warning){
		System.out.println("[WARN] " + warning);
		//SaveFile.saveToSonarDir("log.txt" , "\n[" + new SimpleDateFormat("M/d/y hh:mm a").format(new Date()) + "]" + "[WARN]" + warning);
	}
	public static void error(String error){
		System.err.println("[ERROR] " + error);
		//SaveFile.saveToSonarDir("log.txt" , "\n[" + new SimpleDateFormat("M/d/y hh:mm a").format(new Date()) + "]" + "[ERROR]" + error);
	}
	public static void logToServer(String serverID, String userID, String result){
		
	}
}
