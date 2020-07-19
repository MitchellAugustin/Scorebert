package com.mitchellaugustin.scorebert;

import java.awt.Color;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.emoji.CustomEmoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.entity.message.Reaction;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;

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
		ArrayList<LiveCall> activeCalls = new ArrayList<>();
    	new DiscordApiBuilder().setToken(token).login().thenAccept(api -> {
    		api.updateActivity("!help");
    		api.addMessageCreateListener(event -> {
    			Message message = event.getMessage();
    			//Check to make sure that the database has a table for this server.
            	try {
					if(!SaveFile.doesTableExist(ScoreController.FILENAME, "s" + message.getServer().get().getId())){
						String[] columns = {"" + "" + message.getAuthor().getId(), "0", "10"};
						String[] columnNames = {"USER_ID", "POINTS", "REMAINING_POINTS"};
						SaveFile.putData(ScoreController.FILENAME, "s" + message.getServer().get().getId(), columns, columnNames);
					}
				} catch (ClassNotFoundException | SQLException e1) {
					e1.printStackTrace();
					String[] columns = {"" + message.getAuthor().getId(), "0", "10"};
					String[] columnNames = {"USER_ID", "POINTS", "REMAINING_POINTS"};
					try {
						SaveFile.putData(ScoreController.FILENAME, "s" + message.getServer().get().getId(), columns, columnNames);
					} catch (ClassNotFoundException | SQLException e) {
						e.printStackTrace();
					}
				}
            	
            	//Rates all messages
                if (message.getContent().toString().startsWith("!rateall")){
                	if (message.getCustomEmojis().size() == 0) {
                		message.getChannel().sendMessage("No emojis were specified as search parameters");
                		return;
                	}
                	CustomEmoji emoji = message.getCustomEmojis().get(0);
                	message.getChannel().sendMessage("I am now searching for the message with the most " + emoji.getMentionTag() + ". This may take a while...");
                	CompletableFuture<MessageSet> history = message.getChannel().getMessages(1000000);
					String highestMessage = "";
					String highestAuthor = "";
					int highestCount = 0;
					try {
						MessageSet messageHistory = history.get();
						Iterator<Message> messageIterator = messageHistory.iterator();
						while (messageIterator.hasNext()) {
							Message msg = messageIterator.next();
							try{
								for(Reaction r : msg.getReactions()){
									if(r.getEmoji().equalsEmoji(emoji)){
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

					if (highestCount > 0) {
						message.getChannel().sendMessage("> " + highestMessage + "\n-" + highestAuthor + "\n" + highestCount + " " + emoji.getMentionTag() + "\n");
					}
					else {
						message.getChannel().sendMessage("No " + emoji.getMentionTag() + " reactions were found within the last 1,000,000 messages.");
					}
                }
                
                //Send some slimy boys
                else if(message.getContent().startsWith("!slimyboys")){
                	message.getChannel().sendMessage("https://cdn.discordapp.com/attachments/167788706101460992/340737175303880714/slimyboys.jpg");
                }
                
                //Rates the last 10000 messages
                else if (message.getContent().toString().startsWith("!rate")){
                	if (message.getCustomEmojis().size() == 0) {
                		message.getChannel().sendMessage("No emojis were specified as search parameters");
                		return;
                	}
                	CustomEmoji emoji = message.getCustomEmojis().get(0);
                	message.getChannel().sendMessage("I am now searching for the message with the most " + emoji.getMentionTag() + ". This may take a while...");
                	CompletableFuture<MessageSet> history = message.getChannel().getMessages(10000);
					String highestMessage = "";
					String highestAuthor = "";
					int highestCount = 0;
					try {
						MessageSet messageHistory = history.get();
						System.out.println("History: " + messageHistory.size());
						Iterator<Message> messageIterator = messageHistory.iterator();
						while (messageIterator.hasNext()) {
							Message msg = messageIterator.next();
							try{
								for(Reaction r : msg.getReactions()){
									if(r.getEmoji().equalsEmoji(emoji)){
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
                	
					if (highestCount > 0) {
						message.getChannel().sendMessage("> " + highestMessage + "\n-" + highestAuthor + "\n" + highestCount + " " + emoji.getMentionTag() + "\n");
					}
					else {
						message.getChannel().sendMessage("No " + emoji.getMentionTag() + " reactions were found within the last 10,000 messages. (Try using !rateall)");
					}
                }
                
                //Awards the specified user 1 point if the sender has enough remaining
                //See ScoreController.awardPoint()
                else if(message.getContent().toString().startsWith("!award")){
                	List<User> mentions = message.getMentionedUsers();
                	for(User user : mentions){
                		try {
                			if(!("" + message.getAuthor().getId()).equals("" + user.getId())){
								message.getChannel().sendMessage(ScoreController.awardPoint("" + message.getAuthor().getId(), "" + user.getId(), "" + message.getServer().get().getId()));
                			}
                			else{
                				message.getChannel().sendMessage("You can't award yourself a point!");
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
						message.getChannel().sendMessage("<@" + "" + message.getAuthor().getId() + ">" + ", you have " + ScoreController.getCurrentUserScore("" + message.getAuthor().getId(), "" + message.getServer().get().getId()) + " points and " + ScoreController.getRemainingPoints("" + message.getAuthor().getId(), "" + message.getServer().get().getId()) + " points left to award to others.");
					} catch (ClassNotFoundException | SQLException e) {
						e.printStackTrace();
					}
                }
                
                //Displays the current ranking of all users in the server.
                else if(message.getContent().toString().startsWith("!scoreboard")){
                	List<List<String>> scores;
					try {
						scores = SaveFile.dropTableAsListMatrix(ScoreController.FILENAME, "s" + message.getServer().get().getId(), "POINTS", "USER_ID");
						//Sorts each user in the order of their earned points. Stores an array of the indices.
						int[] sortedIndices = IntStream.range(0, scores.get(0).size())
				                .boxed().sorted((i, j) -> ((Integer.parseInt(scores.get(0).get(i)) > Integer.parseInt(scores.get(0).get(j))) ? +1 : (Integer.parseInt(scores.get(0).get(i)) < Integer.parseInt(scores.get(0).get(j))) ? -1 : 0))
				                .mapToInt(ele -> ele).toArray(); 
						
						String response = "";
						int currentNum = 1;
						Log.info("ScoresAfter: " + scores);
						for(int i = scores.get(0).size() - 1; i >= 0; i--){
							String memberID = scores.get(1).get(sortedIndices[i]);
							Log.info("MemberID: " + memberID);
							String username = "[Removed user]";
							try{
								username = message.getServer().get().getMemberById(memberID).get().getName();
							}
							catch(Exception e){
								Log.error("Error finding username for ID " + memberID + ". User was probably removed from server.");
							}
							response += "#" + currentNum + ": " + username + " (" + scores.get(0).get(sortedIndices[i]) + " points)\n";
							currentNum++;
						}
						new MessageBuilder().setEmbed(new EmbedBuilder()
								.setTitle("Scoreboard")
								.setDescription(response)
								.setColor(Color.GREEN)).send(message.getChannel());
					} catch (ClassNotFoundException | SQLException e) {
						e.printStackTrace();
					}
                	
                }
                else if (message.getContent().startsWith("!mystats")) {
                	String liveCall = "";
					for (LiveCall call : activeCalls) {
						if (call.getServerID() == message.getServer().get().getId() && call.getUserID() == message.getAuthor().getId()) {
							long totalCallTime = Instant.now().getEpochSecond() - call.getStartTime();
							long hours = TimeUnit.SECONDS.toHours(totalCallTime);
							long minutes = TimeUnit.SECONDS.toMinutes(totalCallTime) - (hours * 60);
							long seconds = totalCallTime - (minutes * 60);
							liveCall = "Currently in a call for " + String.format("%d hours, %d minutes, %d seconds", hours, minutes, seconds);
						}
					}
					if (liveCall.isEmpty()) {
						liveCall = "Not currently in a call";
					}
					try {
						new MessageBuilder().setEmbed(new EmbedBuilder()
								.setTitle("User statistics for " + message.getAuthor().getDisplayName())
								.setDescription(
										"Score: " +
												ScoreController.getCurrentUserScore("" + message.getUserAuthor().get().getId(), "" + message.getServer().get().getId()) + "\n" +
										"Spendable Points: " +
												ScoreController.getRemainingPoints("" + message.getUserAuthor().get().getId(), "" + message.getServer().get().getId()) + "\n" +
										"Call time this month: " +
												VoiceDataController.callTimeThisMonth(message.getServer().get().getId(), message.getUserAuthor().get().getId(), false) + "\n" +
										"Call time this year: " +
												VoiceDataController.callTimeThisYear(message.getServer().get().getId(), message.getUserAuthor().get().getId(), false) + "\n" +
										"Total call time: " +
												VoiceDataController.callTimeTotal(message.getServer().get().getId(), message.getUserAuthor().get().getId(), false) + "\n" +
										"AFK time this month: " +
												VoiceDataController.callTimeThisMonth(message.getServer().get().getId(), message.getUserAuthor().get().getId(), true) + "\n" +
										"AFK time this year: " +
												VoiceDataController.callTimeThisYear(message.getServer().get().getId(), message.getUserAuthor().get().getId(), true) + "\n" +
										"Total AFK time: " +
												VoiceDataController.callTimeTotal(message.getServer().get().getId(), message.getUserAuthor().get().getId(), true) + "\n" +
										liveCall)
								.setColor(Color.GREEN)).send(message.getChannel());
					} catch (SQLException throwables) {
						throwables.printStackTrace();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}

				else if (message.getContent().startsWith("!stats")) {
					if (message.getMentionedUsers().isEmpty()) {
						new MessageBuilder().setContent("Please specify a user!").send(message.getChannel());
					}
					String liveCall = "";
					for (LiveCall call : activeCalls) {
						if (call.getServerID() == message.getServer().get().getId() && call.getUserID() == message.getMentionedUsers().get(0).getId()) {
							long totalCallTime = Instant.now().getEpochSecond() - call.getStartTime();
							long hours = TimeUnit.SECONDS.toHours(totalCallTime);
							long minutes = TimeUnit.SECONDS.toMinutes(totalCallTime) - (hours * 60);
							long seconds = totalCallTime - (minutes * 60);
							liveCall = "Currently in a call for " + String.format("%d hours, %d minutes, %d seconds", hours, minutes, seconds);
						}
					}
					if (liveCall.isEmpty()) {
						liveCall = "Not currently in a call";
					}
					try {
						new MessageBuilder().setEmbed(new EmbedBuilder()
								.setTitle("User statistics for " + message.getMentionedUsers().get(0).getDisplayName(message.getServer().get()))
								.setDescription(
										"Score: " +
												ScoreController.getCurrentUserScore("" + message.getMentionedUsers().get(0).getId(), "" + message.getServer().get().getId()) + "\n" +
												"Spendable Points: " +
												ScoreController.getRemainingPoints("" + message.getMentionedUsers().get(0).getId(), "" + message.getServer().get().getId()) + "\n" +
												"Call time this month: " +
												VoiceDataController.callTimeThisMonth(message.getServer().get().getId(), message.getMentionedUsers().get(0).getId(), false) + "\n" +
												"Call time this year: " +
												VoiceDataController.callTimeThisYear(message.getServer().get().getId(), message.getMentionedUsers().get(0).getId(), false) + "\n" +
												"Total call time: " +
												VoiceDataController.callTimeTotal(message.getServer().get().getId(), message.getMentionedUsers().get(0).getId(), false) + "\n" +
												"AFK time this month: " +
												VoiceDataController.callTimeThisMonth(message.getServer().get().getId(), message.getMentionedUsers().get(0).getId(), true) + "\n" +
												"AFK time this year: " +
												VoiceDataController.callTimeThisYear(message.getServer().get().getId(), message.getMentionedUsers().get(0).getId(), true) + "\n" +
												"Total AFK time: " +
												VoiceDataController.callTimeTotal(message.getServer().get().getId(), message.getMentionedUsers().get(0).getId(), true) + "\n" +
												liveCall)
								.setColor(Color.GREEN)).send(message.getChannel());
					} catch (SQLException throwables) {
						throwables.printStackTrace();
					} catch (ClassNotFoundException e) {
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
                		message.getChannel().sendMessage(str, null, true, null);                	}
                }
                
                //Displays the help message
                else if(message.getContent().toString().startsWith("!help")){
                	new MessageBuilder().setEmbed(new EmbedBuilder()
                			.setTitle("ScoreBert Help")
                			.setDescription("!award @user: Gives the mentioned user 1 point\n!mypoints: Shows your points and remaining spendable points\n!rate [emoji]: Finds the message with the most of the specified emoji reactions within the last 10,000 messages (Only works with custom emojis)\n!rateall [emoji]: Same as above, but rates every message in the chat (takes significantly longer)\n!scoreboard: Shows the complete scoreboard\n!mystats: Shows your statistic breakdown\n!stats @user: Shows the specified user's statistic breakdown\n!slimyboys: yum!\n!tts [message]: Reads your message through the TTS engine. Same as /tts, but can be used for messages that are too long for the Discord command.")
							.setColor(Color.GREEN))
                	.send(message.getChannel());
                }
    		});
    		api.addServerVoiceChannelMemberJoinListener(event -> {
    			activeCalls.add(new LiveCall(event.getServer().getId(), event.getUser().getId(), Instant.now().getEpochSecond(), event.getServer().getAfkChannel().get().equals(event.getChannel())));
			});
    		api.addServerVoiceChannelMemberLeaveListener(event -> {
    			LiveCall thisCall = null;
				long endTime = Instant.now().getEpochSecond();
    			for (LiveCall call : activeCalls) {
    				if (call.getServerID() == event.getServer().getId() && call.getUserID() == event.getUser().getId()) {
    					thisCall = call;
    					activeCalls.remove(thisCall);
    					break;
					}
				}

    			if (thisCall != null) {
    				long timeInCall = endTime - thisCall.getStartTime();
    				Log.info(event.getUser().getName() + " completed a call - time: " + timeInCall + (thisCall.isAfkChannel() ? " (AFK)" : ""));
    				thisCall.setEndTime(endTime);
    				//TODO Save call time / log data to database.
					try {
						VoiceDataController.recordCall(thisCall);
					} catch (SQLException throwables) {
						throwables.printStackTrace();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			});
    	});	
    }

}