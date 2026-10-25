package com.yasser.decisionengine.audit;

/**
 * Result of verifying the audit hash chain.
 *
 * @param brokenAt id of the first entry that fails verification, or null if the
 *                 chain is valid
 */
public record ChainStatus(boolean valid, int entriesChecked, String brokenAt, String message) {
}
