package com.mitchellaugustin.scorebert;

/**
 * LiveCall - Structure class that contains basic information on a user's current call status.
 * @Author Mitchell Augustin
 */
public class LiveCall {
    private long serverID;
    private long userID;
    private long startTime;
    private long endTime;
    private boolean afkChannel;

    public LiveCall(long serverID, long userID, long startTime, boolean afkChannel, long endTime) {
        this.serverID = serverID;
        this.userID = userID;
        this.startTime = startTime;
        this.afkChannel = afkChannel;
        this.endTime = endTime;
    }

    public LiveCall(long serverID, long userID, long startTime, boolean afkChannel) {
        this.serverID = serverID;
        this.userID = userID;
        this.startTime = startTime;
        this.afkChannel = afkChannel;
    }

    public long getServerID() {
        return serverID;
    }

    public long getUserID() {
        return userID;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public boolean isAfkChannel() {
        return afkChannel;
    }
}
