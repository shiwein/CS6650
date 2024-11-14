package Model;

public class LiftRideEvent {
    private int skierID;
    private int resortID;
    private int liftID;
    private String seasonID;
    private String dayID;
    private int time;

    public LiftRideEvent(int skierID, int resortID, int liftID, String seasonID, String dayID, int time) {
        this.skierID = skierID;
        this.resortID = resortID;
        this.liftID = liftID;
        this.seasonID = seasonID;
        this.dayID = dayID;
        this.time = time;
    }

    public int getSkierID() {
        return skierID;
    }

    public int getResortID() {
        return resortID;
    }

    public int getLiftID() {
        return liftID;
    }

    public String getSeasonID() {
        return seasonID;
    }

    public String getDayID() {
        return dayID;
    }

    public int getTime() {
        return time;
    }
}
