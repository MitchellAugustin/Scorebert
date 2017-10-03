package com.mitchellaugustin.scorebert;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

//@author Mitchell Augustin

public class SaveFile {
	
	//Creates a table
	public static void generateNewContext(String filename, String server, String[] columnNames) throws ClassNotFoundException, SQLException{
		Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + filename);
        Statement queryStatement = conn.createStatement();
        String statementFormat = "(";
        for(int i = 0; i < columnNames.length; i++){
        	if((i + 1) == columnNames.length){
        		statementFormat += columnNames[i] + ")";
        	}
        	else{
            	statementFormat += columnNames[i] + ", ";
        	}
        }
        Log.info("Context generation statement: " + statementFormat);
        queryStatement.executeUpdate("CREATE TABLE " + server + " " + statementFormat + ";");
	}
	
	
	/**
	 * Checks to see if the specified table exists.
	 * @param filename - The database file
	 * @param table - The table
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 */
	public static boolean doesTableExist(String filename, String table) throws SQLException, ClassNotFoundException{
		Class.forName("org.sqlite.JDBC");
		boolean exists = false;
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + filename);
		DatabaseMetaData meta = conn.getMetaData();
		ResultSet res = meta.getTables(null, null, table, null);
		if(res.next()){
			exists = true;
		}
		conn.close();
		return exists;
	}
	
	/**
	 * Adds a new row to the table with the specified data
	 */
	public static void putData(String filename, String table, String[] columns, String[] columnNames) throws ClassNotFoundException, SQLException{
		if(columnNames != null){
			if(!new File(filename).exists()){
				generateNewContext(filename, table, columnNames);
			}
			else if(!doesTableExist(filename, table)){
				generateNewContext(filename, table, columnNames);
			}
		}
		
		Class.forName("org.sqlite.JDBC");
	    Connection conn = DriverManager.getConnection("jdbc:sqlite:" + filename);
	    //Statement queryStatement = conn.createStatement();
	    String statement = "insert into " + table + " values (";
	    for(int i = 0; i < columns.length; i++){
	    	if(i == 0)
	        	statement += "?";
	    	else
	    		statement += ", ?";
	    }
	    statement += ");";
	    PreparedStatement prep = conn.prepareStatement(statement); 
	    
	    for(int i = 0; i < columns.length; i++){
	    	prep.setString(i+1, columns[i]);
	    }
	    prep.addBatch();
	
	    conn.setAutoCommit(false);
	    prep.executeBatch();
	    conn.setAutoCommit(true);
	    conn.close();
	}

	/**
	 * Updates a row with the specified parameters with new data
	 */
	public static void updateData(String filename, String table, String[] searchTerms, String[] columnNames, String[] newValues) throws ClassNotFoundException, SQLException{
		Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + filename);
        String restrictions = "where ";
        //Build the restrictions of the statement
        for(int i = 0; i < searchTerms.length; i++){
        	restrictions += columnNames[i] + "=\"" + searchTerms[i] + "\"";
        	if((i + 1) != searchTerms.length){
        		restrictions += " and ";
        	}
        }
        
        //Build the queryStatement
        for(int i = 0; i < newValues.length; i++){
            Statement queryStatement = conn.createStatement();
            String statement = "update " + table + " set ";
        	statement += columnNames[i] + "=\"" + newValues[i] + "\"" + restrictions;
            Log.info("Statement: " + statement);
            queryStatement.executeUpdate(statement);
        }
	}
	
	/**
	 * Retrieves the requested information cell from the database
	 * @param filename - The database file
	 * @param table - The table to retrieve data from
	 * @param column1 - The first column's name
	 * @param column2 - The second column's name
	 * @param value - The value of the first column to match
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public static String getInfoFromDatabase(String filename, String table, String column1, String column2, String value) throws ClassNotFoundException, SQLException{
			String result = "";
			Class.forName("org.sqlite.JDBC");
	        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + filename);
	        
			Statement queryStatement = conn.createStatement();
	        
			ResultSet rs = queryStatement.executeQuery("select * from " + table + ";");
			while (rs.next()) {
				String currentColumn1 = rs.getString(column1);
				String currentColumn2 = rs.getString(column2);
	            if(currentColumn1.equals(value)){
	            	//Log.info("Match found! " + currentColumn2);
	            	result = currentColumn2;
	            }
	        } 
	        rs.close();
	        conn.close();
	        
	        if(result.equals("")){
	        	result = "NOT_FOUND";
	        }
	       
	        return result;
		}
	
	public static String formatList(List<String> list){
		return list.toString().replace("[", "").replace("]", "").replace(", ", ",").replace("NO_OBJECT", "").replace("NO_SENTENCE_OBJECT_MODIFIERS", "").replace("NOT_FOUND,", "").replace("NOT_FOUND", "");
	}

	/**
	 * Drops a table as a 2d List
	 * @param filename - The database file
	 * @param table - The table to retrieve data from
	 * @param column1 - The first column's name
	 * @param column2 - The second column's name
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public static List<List<String>> dropTableAsListMatrix(String filename, String table, String column1, String column2) throws ClassNotFoundException, SQLException{
		List<List<String>> result = new ArrayList<List<String>>();
		List<String> column1List = new ArrayList<String>();
		List<String> column2List = new ArrayList<String>();
		Class.forName("org.sqlite.JDBC");
	    Connection conn = DriverManager.getConnection("jdbc:sqlite:" + filename);
	    
		Statement queryStatement = conn.createStatement();
	    
		ResultSet rs = queryStatement.executeQuery("select * from " + table + ";");
		while (rs.next()) {
			String currentColumn1 = rs.getString(column1);
			String currentColumn2 = rs.getString(column2);
	    	//Log.info("Match found! " + currentColumn2);
	    	column1List.add(currentColumn1);
	    	column2List.add(currentColumn2);
	    } 
		result.add(column1List);
		result.add(column2List);
	    rs.close();
	    conn.close();
	    
	    return result;
	}
}
