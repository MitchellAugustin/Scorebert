package com.mitchellaugustin.scorebert;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import com.google.common.util.concurrent.FutureCallback;

import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.Javacord;
import de.btobastian.javacord.entities.CustomEmoji;
import de.btobastian.javacord.entities.User;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.entities.message.MessageHistory;
import de.btobastian.javacord.entities.message.Reaction;
import de.btobastian.javacord.listener.message.MessageCreateListener;

/**
 * ScoreBert - A scoreboard for your Discord server.
 * Each member gets 10 points each month to give to other members whenever they do something cool. 
 * Compete with your friends to see who will rank highest on the scoreboard!
 * 
 * Add ScoreBert to your server: https://discordapp.com/oauth2/authorize?client_id=364186658960048139&scope=bot
 * 
 * @author Mitchell Augustin
 * This program was written by Mitchell Augustin and is licensed under the Apache License version 2.0
 * https://www.apache.org/licenses/LICENSE-2.0.html
 */
public class BotEndpoint {
	
	//Specify your bot token as an argument.
	public static void main(String[] args){
		if(args.length == 1){
			@SuppressWarnings("unused")
			BotEndpoint endpoint = new BotEndpoint(args[0]);
		}
		else{
			Log.error("Please specify a Discord bot token an argument. For example:");
			Log.error("java -jar ScoreBert.jar [token]");
			System.exit(1);
		}
	}

    public BotEndpoint(String token) {
        DiscordAPI api = Javacord.getApi(token, true);
        
        api.connect(new FutureCallback<DiscordAPI>() {
            @Override
            public void onSuccess(DiscordAPI api) {
            	api.setGame("!help");
                api.registerListener(new MessageCreateListener() {
                    @Override
                    public void onMessageCreate(DiscordAPI api, Message message) {
                        //Check to make sure that the database has a table for this server.
                    	try {
							if(!SaveFile.doesTableExist(ScoreController.FILENAME, "s" + message.getChannelReceiver().getServer().getId())){
								String[] columns = {message.getAuthor().getId(), "0", "10"};
								String[] columnNames = {"USER_ID", "POINTS", "REMAINING_POINTS"};
								SaveFile.putData(ScoreController.FILENAME, "s" + message.getChannelReceiver().getServer().getId(), columns, columnNames);
							}
						} catch (ClassNotFoundException | SQLException e1) {
							e1.printStackTrace();
							String[] columns = {message.getAuthor().getId(), "0", "10"};
							String[] columnNames = {"USER_ID", "POINTS", "REMAINING_POINTS"};
							try {
								SaveFile.putData(ScoreController.FILENAME, "s" + message.getChannelReceiver().getServer().getId(), columns, columnNames);
							} catch (ClassNotFoundException | SQLException e) {
								e.printStackTrace();
							}
						}
                    	
                    	//Rates all messages
                        if (message.getContent().toString().startsWith("!rateall")){
                        	CustomEmoji emoji = api.getServerById(message.getChannelReceiver().getServer().getId()).getCustomEmojiByName(message.getContent().toString().split(":")[1].split(":")[0]);
                        	message.reply("I am now searching for the message with the most " + ":" + emoji + ":. This may take a while...");
                        	Future<MessageHistory> history = api.getChannelById(message.getChannelReceiver().getServer().getId()).getMessageHistory(1000000);
							String highestMessage = "";
							String highestAuthor = "";
							int highestCount = 0;
							try {
								MessageHistory messageHistory = history.get();
								List<Message> messageList = messageHistory.getMessagesSorted();
								for(Message msg : messageList){
									try{
										for(Reaction r : msg.getReactions()){
											if(r.getCustomEmoji().getName().equals(emoji.getName())){
												System.err.println("EMOJI FOUND");
												int count = msg.getReactions().get(msg.getReactions().indexOf(r)).getCount();
												if(count > highestCount){
													highestCount = count;
													highestMessage = msg.getContent().toString();
													highestAuthor = msg.getAuthor().getName();
												}
											}
										}
									}
									catch(NullPointerException e){
										System.err.println("NullPointerException. Continuing...");
									}
								}
								
							} catch (InterruptedException e) {
								e.printStackTrace();
							} catch (ExecutionException e) {
								e.printStackTrace();
							}
                        	
							message.reply("The following message by " + highestAuthor + " has " + highestCount + " " + emoji.getMentionTag() + ": " + highestMessage);
                        }
                        
                        //Send some slimy boys
                        else if(message.getContent().startsWith("!slimyboys")){
                        	message.reply("https://cdn.discordapp.com/attachments/167788706101460992/340737175303880714/slimyboys.jpg");
                        }
                        
                        //Rates the last 10000 messages
                        else if (message.getContent().toString().startsWith("!rate")){
                        	CustomEmoji emoji = api.getServerById(message.getChannelReceiver().getServer().getId()).getCustomEmojiByName(message.getContent().toString().split(":")[1].split(":")[0]);
                        	Future<MessageHistory> history = api.getChannelById(message.getChannelReceiver().getServer().getId()).getMessageHistory(10000);
                        	try {
								MessageHistory messageHistory = history.get();
								List<Message> messageList = messageHistory.getMessagesSorted();
								String highestMessage = "";
								String highestAuthor = "";
								int highestCount = 0;
								for(Message msg : messageList){
									try{
										for(Reaction r : msg.getReactions()){
											if(r.getCustomEmoji().getName().equals(emoji.getName())){
												System.err.println("EMOJI FOUND");
												int count = msg.getReactions().get(msg.getReactions().indexOf(r)).getCount();
												if(count > highestCount){
													highestCount = count;
													highestMessage = msg.getContent().toString();
													highestAuthor = msg.getAuthor().getName();
												}
											}
										}
									}
									catch(NullPointerException e){
										System.err.println("NullPointerException. Continuing...");
									}
								}
								
								message.reply("The following message by " + highestAuthor + " has " + highestCount + " " + emoji.getMentionTag() + ": " + highestMessage);
							} catch (InterruptedException e) {
								e.printStackTrace();
							} catch (ExecutionException e) {
								e.printStackTrace();
							}
                        }
                        
                        //Awards the specified user 1 point if the sender has enough remaining
                        //See ScoreController.awardPoint()
                        else if(message.getContent().toString().startsWith("!award")){
                        	List<User> mentions = message.getMentions();
                        	for(User user : mentions){
                        		try {
                        			if(!message.getAuthor().getId().equals(user.getId())){
    									message.reply(ScoreController.awardPoint(message.getAuthor().getId(), user.getId(), message.getChannelReceiver().getServer().getId()));
                        			}
                        			else{
                        				message.reply("You can't award yourself a point!");
                        			}
								} catch (ClassNotFoundException e) {
									e.printStackTrace();
								} catch (SQLException e) {
									e.printStackTrace();
								}
                        	}
                        }
                        
                        //Returns the number of points the sender has earned, as well as those still available to send.
                        //See ScoreController.getCurrentUserScore()
                        else if(message.getContent().toString().startsWith("!mypoints")){
                        	try {
								message.reply(message.getAuthor().getMentionTag() + ", you have " + ScoreController.getCurrentUserScore(message.getAuthor().getId(), message.getChannelReceiver().getServer().getId()) + " points and " + ScoreController.getRemainingPoints(message.getAuthor().getId(), message.getChannelReceiver().getServer().getId()) + " points left to award to others.");
							} catch (ClassNotFoundException | SQLException e) {
								e.printStackTrace();
							}
                        }
                        
                        //Displays the current ranking of all users in the server.
                        else if(message.getContent().toString().startsWith("!scoreboard")){
                        	List<List<String>> scores;
							try {
								scores = SaveFile.dropTableAsListMatrix(ScoreController.FILENAME, "s" + message.getChannelReceiver().getServer().getId(), "POINTS", "USER_ID");
								//Sorts each user in the order of their earned points. Stores an array of the indices.
								int[] sortedIndices = IntStream.range(0, scores.get(0).size())
						                .boxed().sorted((i, j) -> ((Integer.parseInt(scores.get(0).get(i)) > Integer.parseInt(scores.get(0).get(j))) ? +1 : (Integer.parseInt(scores.get(0).get(i)) < Integer.parseInt(scores.get(0).get(j))) ? -1 : 0))
						                .mapToInt(ele -> ele).toArray(); 
								
								String response = "Scoreboard:\n";
								int currentNum = 1;
								Log.info("ScoresAfter: " + scores);
								for(int i = scores.get(0).size() - 1; i >= 0; i--){
									String memberID = scores.get(1).get(sortedIndices[i]);
									Log.info("MemberID: " + memberID);
									String username = "[Removed user]";
									try{
										username = message.getChannelReceiver().getServer().getMemberById(memberID).getName();
									}
									catch(Exception e){
										Log.error("Error finding username for ID " + memberID + ". User was probably removed from server.");
									}
									response += "#" + currentNum + ": " + username + " (" + scores.get(0).get(sortedIndices[i]) + " points)\n";
									currentNum++;
								}
								message.reply(response);
							} catch (ClassNotFoundException | SQLException e) {
								e.printStackTrace();
							}
                        	
                        }

                        //Sends a TTS message (or multiple successive TTS messages) with the specified content
                        else if(message.getContent().startsWith("!tts")){
                        	String msg = message.getContent().replace("!tts ", "");
                        	String[] broken = msg.split("\\.", -1);
                        	for(String str : broken){
                        		try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
                        		//For some reason, replies can't be TTS messages in this version of Javacord. I don't know why.
                        		api.getChannelById(message.getChannelReceiver().getId()).sendMessage(str, true);
                        	}
                        }
                        
                        //Displays the help message
                        else if(message.getContent().toString().startsWith("!help")){
                        	message.reply("`!award @user: Gives the mentioned user 1 point\n!mypoints: Shows your points and remaining spendable points\n!rate [emoji]: Finds the message with the most of the specified emoji reactions within the last 10,000 messages (Only works with custom emojis)\n!rateall [emoji]: Same as above, but rates every message in the chat (takes significantly longer)\n!scoreboard: Shows the complete scoreboard\n!slimyboys: yum!\n!tts [message]: Reads your message through the TTS engine. Same as /tts, but can be used for messages that are too long for the Discord command.`");
                        }
                        
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        });
    }

}