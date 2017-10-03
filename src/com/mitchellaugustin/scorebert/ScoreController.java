package com.mitchellaugustin.scorebert;

import java.sql.SQLException;

/**
 * ScoreController - The class that handles database modification and info retrieval.
 * @author Mitchell Augustin
 */
public class ScoreController {
	public static final String FILENAME = "scores.db"; 
	private static String[] columnNames = {"USER_ID", "POINTS", "REMAINING_POINTS"};
	
	/**
	 * Awards a point to the recipient and removes a spendable point from the sender.
	 * @param senderID - The Discord ID of the sender
	 * @param recipientID - The Discord ID of the recipient
	 * @param serverID - The Discord ID of the server
	 * @return ScoreBert's response
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	protected static String awardPoint(String senderID, String recipientID, String serverID) throws ClassNotFoundException, SQLException{
		String response = "";
		String senderRemainingPoints = SaveFile.getInfoFromDatabase(FILENAME, "s" + serverID, "USER_ID", "REMAINING_POINTS", senderID);
		String recipientPoints = SaveFile.getInfoFromDatabase(FILENAME, "s" + serverID, "USER_ID", "POINTS", recipientID);
		
		if(senderRemainingPoints.equals("NOT_FOUND")){
			String[] columns = {senderID, "0", "10"};
			SaveFile.putData(FILENAME, "s" + serverID, columns, columnNames);
			senderRemainingPoints = "10";
		}
		if(recipientPoints.equals("NOT_FOUND")){
			String[] columns = {recipientID, "0", "10"};
			SaveFile.putData(FILENAME, "s" + serverID, columns, columnNames);
			recipientPoints = "0";
		}
		
		String senderPoints = SaveFile.getInfoFromDatabase(FILENAME, "s" + serverID, "USER_ID", "POINTS", senderID);
		String recipientRemainingPoints = SaveFile.getInfoFromDatabase(FILENAME, "s" + serverID, "USER_ID", "REMAINING_POINTS", recipientID);
		
		String[] senderSearchTerms = {senderID, senderPoints, senderRemainingPoints};
		String[] recipientSearchTerms = {recipientID, recipientPoints, recipientRemainingPoints};
		
		String[] senderNewValues = {senderID, senderPoints, "" + (Integer.parseInt(senderRemainingPoints) - 1)};
		String[] recipientNewValues = {recipientID, "" + (Integer.parseInt(recipientPoints) + 1), recipientRemainingPoints};
		
		if(Integer.parseInt(senderRemainingPoints) > 0){
			SaveFile.updateData(FILENAME, "s" + serverID, senderSearchTerms, columnNames, senderNewValues);
			SaveFile.updateData(FILENAME, "s" + serverID, recipientSearchTerms, columnNames, recipientNewValues);
			response = "<@" + recipientID + ">, you earned a point! You have been awarded " + (Integer.parseInt(recipientPoints) + 1) + " this month. <@" + senderID + ">, you have " + (Integer.parseInt(senderRemainingPoints) - 1) + " left to award this month.";
		}
		else{
			response = "You don't have any points left to award this month. Please try again on the first of next month.";
		}
		
		return response;
	}
	
	/**
	 * Retrieves the specified user's current score
	 * @param userID - The user's Discord ID
	 * @param serverID - The server's Discord ID
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	protected static String getCurrentUserScore(String userID, String serverID) throws ClassNotFoundException, SQLException{
		String score = "";
		score = SaveFile.getInfoFromDatabase(FILENAME, "s" + serverID, "USER_ID", "POINTS", userID);
		if(score.equals("NOT_FOUND")){
			score = "0";
		}
		return score;
	}
	
	/**
	 * Retrieves an integer containing the number of points a user has left to spend this month
	 * @param userID - The user's Discord ID
	 * @param serverID - The server's Discord ID
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	protected static String getRemainingPoints(String userID, String serverID) throws ClassNotFoundException, SQLException{
		String remainingPoints = "";
		remainingPoints = SaveFile.getInfoFromDatabase(FILENAME, "s" + serverID, "USER_ID", "REMAINING_POINTS", userID);
		if(remainingPoints.equals("NOT_FOUND")){
			remainingPoints = "10";
		}
		return remainingPoints;
	}
}
