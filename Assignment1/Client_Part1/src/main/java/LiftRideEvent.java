public class LiftRideEvent {
    int skierID;
    int resortID;
    int liftID;
    int seasonID;
    int dayID;
    int time;

    public LiftRideEvent(int skierID, int resortID, int liftID, int seasonID, int dayID, int time) {
        this.skierID = skierID;
        this.resortID = resortID;
        this.liftID = liftID;
        this.seasonID = seasonID;
        this.dayID = dayID;
        this.time = time;
    }

    public String toJson() {
        return String.format("{\"skierID\":%d,\"resortID\":%d,\"liftID\":%d,\"seasonID\":%d,\"dayID\":%d,\"time\":%d}",
                skierID, resortID, liftID, seasonID, dayID, time);
    }
}