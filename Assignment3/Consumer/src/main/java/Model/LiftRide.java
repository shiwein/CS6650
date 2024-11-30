package Model;

public class LiftRide {
    private final int resortID;
    private final String seasonID;
    private final String dayID;
    private final int skierID;
    private final int liftID;
    private final int time;

    public LiftRide(int resortID, String seasonID, String dayID, int skierID, int liftID, int time) {
        this.resortID = resortID;
        this.seasonID = seasonID;
        this.dayID = dayID;
        this.skierID = skierID;
        this.liftID = liftID;
        this.time = time;
    }

    public int getResortID() {
        return resortID;
    }

    public String getSeasonID() {
        return seasonID;
    }

    public String getDayID() {
        return dayID;
    }

    public int getSkierID() {
        return skierID;
    }

    public int getLiftID() {
        return liftID;
    }

    public int getTime() {
        return time;
    }

}