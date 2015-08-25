package fi.helsinki.cs.tmc.utilities;

public class Cooldown {
    
    private long durationMillis;
    private long startTime;

    public Cooldown(long durationMillis) {
        this.durationMillis = durationMillis;
        this.startTime = 0;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public void start() {
        startTime = System.currentTimeMillis();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= startTime + durationMillis;
    }
}