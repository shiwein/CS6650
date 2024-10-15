// Static nested class for LiftRide
public class LiftRide {
    private Integer time;
    private Integer liftID;

    public LiftRide(Integer time, Integer liftID) {
        this.time = time;
        this.liftID = liftID;
    }

    public Integer getTime() {
        return this.time;
    }

    public Integer getLiftID() {
        return this.liftID;
    }

    public void setTime(Integer time) {
        this.time = time;
    }

    public void setLiftID(Integer liftID) {
        this.liftID = liftID;
    }
}