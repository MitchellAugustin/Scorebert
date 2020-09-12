package com.mitchellaugustin.scorebert;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * VoiceDataController - The class that handles database modification and info retrieval for voice data.
 * @author Mitchell Augustin
 */
public class VoiceDataController {
    public static final String FILENAME = "voicedata.db";
    private static String[] columnNames = {"USER_ID", "CALL_START", "CALL_END", "CALL_TIME", "IS_AFK"};

    protected static void recordCall(LiveCall call) throws SQLException, ClassNotFoundException {
        String[] columns = {"" + call.getUserID(), "" + call.getStartTime(), "" + call.getEndTime(), "" + (call.getEndTime() - call.getStartTime()), "" + call.isAfkChannel()};
        SaveFile.putData(FILENAME, "s" + call.getServerID(), columns, columnNames);
    }

    protected static ArrayList<LiveCall> getAllCalls(long serverID) throws SQLException, ClassNotFoundException {
        return SaveFile.dropTableAsCallList(FILENAME, serverID, columnNames);
    }

    protected static long getTotalUserCallTime(long serverID, long userID, boolean afk) throws SQLException, ClassNotFoundException {
        long totalCallTime = 0;
        for (LiveCall curCall : getAllCalls(serverID)) {
            if (curCall.getUserID() == userID && (afk == curCall.isAfkChannel())) {
                totalCallTime += (curCall.getEndTime() - curCall.getStartTime());
            }
        }
        return totalCallTime;
    }

    protected static String callTimeTotal(long serverID, long userID, boolean afk) throws SQLException, ClassNotFoundException {
        long totalCallTime = 0;
        for (LiveCall curCall : getAllCalls(serverID)) {
            if (curCall.getUserID() == userID && (afk == curCall.isAfkChannel())) {
                totalCallTime += (curCall.getEndTime() - curCall.getStartTime());
            }
        }

        return toHMS(totalCallTime);
    }

    protected static String callTimeThisYear(long serverID, long userID, boolean afk) throws SQLException, ClassNotFoundException {
        long totalCallTime = 0;
        for (LiveCall curCall : getAllCalls(serverID)) {
            if (curCall.getUserID() == userID && (afk == curCall.isAfkChannel()) && Instant.ofEpochSecond(curCall.getStartTime()).atZone(ZoneId.systemDefault()).toLocalDate().getYear() == Instant.now().atZone(ZoneId.systemDefault()).toLocalDate().getYear()) {
                totalCallTime += (curCall.getEndTime() - curCall.getStartTime());
            }
        }

        return toHMS(totalCallTime);
    }

    protected static String callTimeThisMonth(long serverID, long userID, boolean afk) throws SQLException, ClassNotFoundException {
        long totalCallTime = 0;
        for (LiveCall curCall : getAllCalls(serverID)) {
            if (curCall.getUserID() == userID && (afk == curCall.isAfkChannel()) && Instant.ofEpochSecond(curCall.getStartTime()).atZone(ZoneId.systemDefault()).toLocalDate().getYear() == Instant.now().atZone(ZoneId.systemDefault()).toLocalDate().getYear() && Instant.ofEpochSecond(curCall.getStartTime()).atZone(ZoneId.systemDefault()).toLocalDate().getMonth().equals(Instant.now().atZone(ZoneId.systemDefault()).toLocalDate().getMonth())) {
                totalCallTime += (curCall.getEndTime() - curCall.getStartTime());
            }
        }

        return toHMS(totalCallTime);
    }

    protected static String callTimePastDay(long serverID, long userID, boolean afk) throws SQLException, ClassNotFoundException {
        long totalCallTime = 0;
        for (LiveCall curCall : getAllCalls(serverID)) {
            if (curCall.getUserID() == userID && (afk == curCall.isAfkChannel()) &&
                    Instant.ofEpochSecond(curCall.getStartTime()).atZone(ZoneId.systemDefault()).toLocalDate().getYear() == Instant.now().atZone(ZoneId.systemDefault()).toLocalDate().getYear() &&
                    Instant.ofEpochSecond(curCall.getStartTime()).atZone(ZoneId.systemDefault()).toLocalDate().getMonth().equals(Instant.now().atZone(ZoneId.systemDefault()).toLocalDate().getMonth()) &&

                    ((Instant.ofEpochSecond(curCall.getStartTime()).atZone(ZoneId.systemDefault()).toLocalDate().getDayOfMonth() == Instant.now().atZone(ZoneId.systemDefault()).toLocalDate().getDayOfMonth())
                            || (Instant.ofEpochSecond(curCall.getStartTime()).atZone(ZoneId.systemDefault()).toLocalDate().getDayOfMonth() == Instant.now().minus(24, ChronoUnit.HOURS).atZone(ZoneId.systemDefault()).toLocalDate().getDayOfMonth() && (Instant.ofEpochSecond(curCall.getStartTime()).atZone(ZoneId.systemDefault()).toLocalTime().getHour() >= Instant.now().minus(24, ChronoUnit.HOURS).atZone(ZoneId.systemDefault()).toLocalTime().getHour())))) {
                totalCallTime += (curCall.getEndTime() - curCall.getStartTime());
            }
        }

        return toHMS(totalCallTime);
    }

    protected static String toHMS(long totalCallTime) {
        long hours = TimeUnit.SECONDS.toHours(totalCallTime);
        long minutes = TimeUnit.SECONDS.toMinutes(totalCallTime) - (hours * 60);
        long seconds = totalCallTime % 60;

        return String.format("%d hours, %d minutes, %d seconds", hours, minutes, seconds);
    }
}
