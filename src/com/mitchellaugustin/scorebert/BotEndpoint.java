package com.mitchellaugustin.scorebert;

import java.awt.Color;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

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
public class BotEndpoint extends ListenerAdapter {
	private final ArrayList<LiveCall> activeCalls = new ArrayList<>();
	private JDA jda;

	public static void main(String[] args) {
		if (args.length == 1) {
			new BotEndpoint(args[0]);
		} else {
			Log.error("Please specify a Discord bot token an argument. For example:");
			Log.error("java -jar ScoreBert.jar [token]");
			System.exit(1);
		}
	}

	public BotEndpoint(String token) {
		jda = JDABuilder.createDefault(token)
			.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
			.setMemberCachePolicy(MemberCachePolicy.ALL)
			.setChunkingFilter(ChunkingFilter.ALL)
			.addEventListeners(this)
			.build();
	}

	@Override
	public void onReady(ReadyEvent event) {
		event.getJDA().getPresence().setActivity(net.dv8tion.jda.api.entities.Activity.playing("!help"));
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if (event.getAuthor().isBot()) return;
		
		Message message = event.getMessage();
		String content = message.getContentRaw();
		
		// Check to make sure that the database has a table for this server
		try {
			if (!SaveFile.doesTableExist(ScoreController.FILENAME, "s" + event.getGuild().getId())) {
				String[] columns = {"" + message.getAuthor().getId(), "0", "10"};
				String[] columnNames = {"USER_ID", "POINTS", "REMAINING_POINTS"};
				SaveFile.putData(ScoreController.FILENAME, "s" + event.getGuild().getId(), columns, columnNames);
			}
			if (!SaveFile.doesTableExist(VoiceDataController.FILENAME, "s" + event.getGuild().getId())) {
				String[] columns = {"364186658960048139", "" + Instant.now().getEpochSecond(), "" + Instant.now().getEpochSecond(), "0", "TRUE"};
				String[] columnNames = {"USER_ID", "CALL_START", "CALL_END", "CALL_TIME", "IS_AFK"};
				SaveFile.putData(VoiceDataController.FILENAME, "s" + event.getGuild().getId(), columns, columnNames);
			}
		} catch (ClassNotFoundException | SQLException e1) {
			e1.printStackTrace();
			String[] columns = {"" + message.getAuthor().getId(), "0", "10"};
			String[] columnNames = {"USER_ID", "POINTS", "REMAINING_POINTS"};
			try {
				SaveFile.putData(ScoreController.FILENAME, "s" + event.getGuild().getId(), columns, columnNames);
			} catch (ClassNotFoundException | SQLException e) {
				e.printStackTrace();
			}

			String[] vColumns = {"364186658960048139", "" + Instant.now().getEpochSecond(), "" + Instant.now().getEpochSecond(), "0", "TRUE"};
			String[] vColumnNames = {"USER_ID", "CALL_START", "CALL_END", "CALL_TIME", "IS_AFK"};
			try {
				SaveFile.putData(VoiceDataController.FILENAME, "s" + event.getGuild().getId(), vColumns, vColumnNames);
			} catch (ClassNotFoundException | SQLException e) {
				e.printStackTrace();
			}
		}

		// Handle commands
		if (content.startsWith("!rateall")) {
			handleRateAll(message);
		} else if (content.startsWith("!slimyboys")) {
			message.getChannel().sendMessage("https://cdn.discordapp.com/attachments/167788706101460992/340737175303880714/slimyboys.jpg").queue();
		} else if (content.startsWith("!rate")) {
			handleRate(message);
		} else if (content.startsWith("!award")) {
			handleAward(message);
		} else if (content.startsWith("!mypoints")) {
			handleMyPoints(message);
		} else if (content.startsWith("!scoreboard")) {
			handleScoreboard(message);
		} else if (content.startsWith("!vscoreboard")) {
			handleVScoreboard(message);
		} else if (content.startsWith("!mystats")) {
			handleMyStats(message);
		} else if (content.startsWith("!stats")) {
			handleStats(message);
		} else if (content.startsWith("!tts")) {
			handleTTS(message);
		} else if (content.startsWith("!help")) {
			handleHelp(message);
		}
	}

	private void handleRateAll(Message message) {
		List<CustomEmoji> emojis = message.getMentions().getCustomEmojis();
		if (emojis.isEmpty()) {
			message.getChannel().sendMessage("No emojis were specified as search parameters").queue();
			return;
		}
		
		CustomEmoji emoji = emojis.get(0);
		message.getChannel().sendMessage("I am now searching for the message with the most " + emoji.getAsMention() + ". This may take a while...").queue();
		
		message.getChannel().getHistory().retrievePast(1000000).queue(history -> {
			String highestMessage = "";
			String highestAuthor = "";
			int highestCount = 0;
			
			for (Message msg : history) {
				for (MessageReaction reaction : msg.getReactions()) {
					if (reaction.getEmoji().equals(emoji)) {
						int count = reaction.getCount();
						if (count > highestCount) {
							highestCount = count;
							highestMessage = msg.getContentRaw();
							highestAuthor = msg.getAuthor().getName();
						}
					}
				}
			}
			
			if (highestCount > 0) {
				message.getChannel().sendMessage("> " + highestMessage + "\n-" + highestAuthor + "\n" + highestCount + " " + emoji.getAsMention() + "\n").queue();
			} else {
				message.getChannel().sendMessage("No " + emoji.getAsMention() + " reactions were found within the last 1,000,000 messages.").queue();
			}
		});
	}

	private void handleRate(Message message) {
		List<CustomEmoji> emojis = message.getMentions().getCustomEmojis();
		if (emojis.isEmpty()) {
			message.getChannel().sendMessage("No emojis were specified as search parameters").queue();
			return;
		}
		
		CustomEmoji emoji = emojis.get(0);
		message.getChannel().sendMessage("I am now searching for the message with the most " + emoji.getAsMention() + ". This may take a while...").queue();
		
		message.getChannel().getHistory().retrievePast(10000).queue(history -> {
			String highestMessage = "";
			String highestAuthor = "";
			int highestCount = 0;
			
			for (Message msg : history) {
				for (MessageReaction reaction : msg.getReactions()) {
					if (reaction.getEmoji().equals(emoji)) {
						int count = reaction.getCount();
						if (count > highestCount) {
							highestCount = count;
							highestMessage = msg.getContentRaw();
							highestAuthor = msg.getAuthor().getName();
						}
					}
				}
			}
			
			if (highestCount > 0) {
				message.getChannel().sendMessage("> " + highestMessage + "\n-" + highestAuthor + "\n" + highestCount + " " + emoji.getAsMention() + "\n").queue();
			} else {
				message.getChannel().sendMessage("No " + emoji.getAsMention() + " reactions were found within the last 10,000 messages. (Try using !rateall)").queue();
			}
		});
	}

	private void handleAward(Message message) {
		List<Member> mentions = message.getMentions().getMembers();
		for (Member member : mentions) {
			try {
				if (!message.getAuthor().getId().equals(member.getId())) {
					message.getChannel().sendMessage(ScoreController.awardPoint(
						message.getAuthor().getId(),
						member.getId(),
						message.getGuild().getId()
					)).queue();
				} else {
					message.getChannel().sendMessage("You can't award yourself a point!").queue();
				}
			} catch (ClassNotFoundException | SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private void handleMyPoints(Message message) {
		try {
			message.getChannel().sendMessage("<@" + message.getAuthor().getId() + ">, you have " +
				ScoreController.getCurrentUserScore(message.getAuthor().getId(), message.getGuild().getId()) +
				" points and " +
				ScoreController.getRemainingPoints(message.getAuthor().getId(), message.getGuild().getId()) +
				" points left to award to others.").queue();
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
	}

	private void handleScoreboard(Message message) {
		try {
			List<List<String>> scores = SaveFile.dropTableAsListMatrix(
				ScoreController.FILENAME,
				"s" + message.getGuild().getId(),
				"POINTS",
				"USER_ID"
			);
			
			int[] sortedIndices = IntStream.range(0, scores.get(0).size())
				.boxed()
				.sorted(Comparator.comparingInt(i -> Integer.parseInt(scores.get(0).get(i))))
				.mapToInt(ele -> ele)
				.toArray();
			
			StringBuilder response = new StringBuilder();
			int currentNum = 1;
			
			for (int i = scores.get(0).size() - 1; i >= 0; i--) {
				String memberId = scores.get(1).get(sortedIndices[i]);
				Member member = message.getGuild().getMemberById(memberId);
				String username = member != null ? member.getEffectiveName() : "[Removed user]";
				response.append("#").append(currentNum).append(": ")
					.append(username).append(" (")
					.append(scores.get(0).get(sortedIndices[i]))
					.append(" points)\n");
				currentNum++;
			}
			
			message.getChannel().sendMessageEmbeds(
				new net.dv8tion.jda.api.EmbedBuilder()
					.setTitle("Scoreboard")
					.setDescription(response.toString())
					.setColor(Color.GREEN)
					.build()
			).queue();
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
	}

	private void handleVScoreboard(Message message) {
		String[] args = message.getContentRaw().split(" ");
		boolean afk = args.length > 1 && args[1].equalsIgnoreCase("afk");
		
		try {
			List<LiveCall> calls = VoiceDataController.getAllCalls(message.getGuild().getIdLong());
			Map<String, Long> userTimes = new HashMap<>();
			
			for (LiveCall call : calls) {
				if (call.isAfkChannel() == afk && call.getEndTime() != 0) {
					String userId = String.valueOf(call.getUserID());
					userTimes.merge(userId, call.getEndTime() - call.getStartTime(), Long::sum);
				}
			}
			
			if (userTimes.isEmpty()) {
				message.getChannel().sendMessage("No " + (afk ? "AFK" : "active") + " voice time recorded yet!").queue();
				return;
			}
			
			List<Map.Entry<String, Long>> sortedTimes = new ArrayList<>(userTimes.entrySet());
			sortedTimes.sort(Map.Entry.<String, Long>comparingByValue().reversed());
			
			StringBuilder response = new StringBuilder();
			int currentNum = 1;
			
			for (Map.Entry<String, Long> entry : sortedTimes) {
				Member member = message.getGuild().getMemberById(entry.getKey());
				String username = member != null ? member.getEffectiveName() : "[Removed user]";
				response.append("#").append(currentNum).append(": ")
					.append(username).append(" (")
					.append(VoiceDataController.toHMS(entry.getValue()))
					.append(")\n");
				currentNum++;
			}
			
			message.getChannel().sendMessageEmbeds(
				new net.dv8tion.jda.api.EmbedBuilder()
					.setTitle("Voice Scoreboard (" + (afk ? "AFK" : "TOTAL") + ")")
					.setDescription(response.toString())
					.setColor(Color.BLUE)
					.build()
			).queue();
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
	}

	private void handleMyStats(Message message) {
		handleStats(message);
	}

	private void handleStats(Message message) {
		String[] args = message.getContentRaw().split(" ");
		Member targetMember = message.getMember();
		
		if (args.length > 1 && !args[0].equals("!mystats")) {
			List<Member> mentions = message.getMentions().getMembers();
			if (!mentions.isEmpty()) {
				targetMember = mentions.get(0);
			}
		}
		
		try {
			String points = ScoreController.getCurrentUserScore(targetMember.getId(), message.getGuild().getId());
			String remaining = ScoreController.getRemainingPoints(targetMember.getId(), message.getGuild().getId());
			String activeTime = VoiceDataController.callTimeTotal(message.getGuild().getIdLong(), targetMember.getIdLong(), false);
			String afkTime = VoiceDataController.callTimeTotal(message.getGuild().getIdLong(), targetMember.getIdLong(), true);
			
			message.getChannel().sendMessageEmbeds(
				new net.dv8tion.jda.api.EmbedBuilder()
					.setTitle("Stats for " + targetMember.getEffectiveName())
					.addField("Points", "Total: " + points + "\nRemaining: " + remaining, true)
					.addField("Voice Time", "Active: " + activeTime + "\nAFK: " + afkTime, true)
					.setColor(Color.YELLOW)
					.build()
			).queue();
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
	}

	private void handleTTS(Message message) {
		if (!message.getGuild().getSelfMember().getVoiceState().inAudioChannel()) {
			message.getChannel().sendMessage("I need to be in a voice channel to use TTS!").queue();
			return;
		}
		
		String content = message.getContentRaw().substring(4).trim();
		if (content.isEmpty()) {
			message.getChannel().sendMessage("Please provide a message to speak!").queue();
			return;
		}
		
		message.getChannel().sendMessage("🔊 " + content).setTTS(true).queue();
	}

	private void handleHelp(Message message) {
		String helpText = "**ScoreBert Help**\n"
			+ "`!award @user` - Gives the mentioned user 1 point\n"
			+ "`!mypoints` - Shows your points and remaining spendable points\n"
			+ "`!rate [emoji]` - Finds the message with the most of the specified emoji reactions within the last 10,000 messages (Only works with custom emojis)\n"
			+ "`!rateall [emoji]` - Same as above, but rates every message in the chat (takes significantly longer)\n"
			+ "`!scoreboard` - Shows the complete scoreboard for user-awarded points\n"
			+ "`!vscoreboard [afk|total]` - Ranks users based on tracked voice channel time\n"
			+ "`!mystats` - Shows your statistic breakdown\n"
			+ "`!stats @user` - Shows the specified user's statistic breakdown\n"
			+ "`!slimyboys` - yum!\n"
			+ "`!tts [message]` - Reads your message through the TTS engine. Same as /tts, but can be used for messages that are too long for the Discord command.";
		message.getChannel().sendMessage(helpText).queue();
	}

	@Override
	public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
		// User joined a voice channel
		if (event.getChannelJoined() != null && event.getChannelLeft() == null) {
			boolean isAfk = event.getChannelJoined().getName().toLowerCase().equals("afk");
			LiveCall call = new LiveCall(
				event.getGuild().getIdLong(),
				event.getMember().getIdLong(),
				Instant.now().getEpochSecond(),
				isAfk
			);
			activeCalls.add(call);
		}
		// User left a voice channel
		else if (event.getChannelLeft() != null && event.getChannelJoined() == null) {
			for (LiveCall call : activeCalls) {
				if (call.getUserID() == event.getMember().getIdLong() &&
					call.getServerID() == event.getGuild().getIdLong() &&
					call.getEndTime() == 0) {
					call.setEndTime(Instant.now().getEpochSecond());
					try {
						VoiceDataController.recordCall(call);
					} catch (ClassNotFoundException | SQLException e) {
						e.printStackTrace();
					}
					activeCalls.remove(call);
					break;
				}
			}
		}
		// User switched channels (leave + join)
		else if (event.getChannelLeft() != null && event.getChannelJoined() != null) {
			// End the previous call
			for (LiveCall call : activeCalls) {
				if (call.getUserID() == event.getMember().getIdLong() &&
					call.getServerID() == event.getGuild().getIdLong() &&
					call.getEndTime() == 0) {
					call.setEndTime(Instant.now().getEpochSecond());
					try {
						VoiceDataController.recordCall(call);
					} catch (ClassNotFoundException | SQLException e) {
						e.printStackTrace();
					}
					activeCalls.remove(call);
					break;
				}
			}
			// Start a new call in the joined channel
			boolean isAfk = event.getChannelJoined().getName().toLowerCase().equals("afk");
			LiveCall call = new LiveCall(
				event.getGuild().getIdLong(),
				event.getMember().getIdLong(),
				Instant.now().getEpochSecond(),
				isAfk
			);
			activeCalls.add(call);
		}
	}
}