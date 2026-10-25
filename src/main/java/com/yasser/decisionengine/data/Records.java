package com.yasser.decisionengine.data;

/**
 * Synthetic "system of record" types. In production these would come from
 * the order database, CRM, ticketing system and CI server.
 *
 * Dates are stored as "days ago" so the demo never drifts as time passes.
 */
public final class Records {

    private Records() {
    }

    /** status: DELIVERED, IN_TRANSIT, RETURN_IN_TRANSIT, REFUNDED */
    public record Order(String id, String customerId, double total, double refunded,
            String paymentMethod, String status, int daysSincePurchase) {
        public double refundable() {
            return Math.max(0, total - refunded);
        }
    }

    /** tier: "regular" or "vip" */
    public record Customer(String id, String name, String tier,
            int refundsLast90Days, boolean fraudHold) {
    }

    public record Ticket(String id, String customerId, String subject, String text) {
    }

    public record Service(String name, boolean rollbackAvailable, boolean critical,
            int freezeDaysRemaining) {
    }

    /** status: PASSED, FAILED, RUNNING */
    public record CiRun(String id, String commit, String service, String status,
            int testsRun, int changedFiles) {
    }
}
