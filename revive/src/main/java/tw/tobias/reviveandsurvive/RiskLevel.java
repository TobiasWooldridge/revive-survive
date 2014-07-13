package tw.tobias.reviveandsurvive;

public enum RiskLevel {
    CITY_DRIVING(500, "very high risk area"),
    HIGH_RISK(100, "high risk area"),
    MEDIUM_RISK(1, "medium risk area"),
    LOW_RISK(0, "low risk area");

    int threshold;
    String message;
    RiskLevel(int threshold, String message) {
        this.threshold = threshold;
        this.message = message;
    }
}
