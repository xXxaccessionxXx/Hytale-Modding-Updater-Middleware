package com.hytale.updater.diff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The Discernment Engine: Compares 'Current State' with 'Incoming State' mapped
 * datasets.
 * Efficiently computes the deltas between the two states to return lists of
 * what strictly
 * needs to be inserted, updated, or deleted.
 *
 * @param <T> The target schema representing the record type.
 * @param <K> The unique identifier type for the record (e.g., a primary key
 *            String).
 */
public class DatasetDiffEngine<T, K> {

    // A function to extract the unique ID natively from the Record
    private final Function<T, K> idExtractor;
    // A function returning a hash (like MD5/SHA256 or custom structural hash)
    // representing deep comparison to avoid expensive field-by-field equality
    // checks.
    private final Function<T, String> stateHashExtractor;

    public DatasetDiffEngine(Function<T, K> idExtractor, Function<T, String> stateHashExtractor) {
        this.idExtractor = idExtractor;
        this.stateHashExtractor = stateHashExtractor;
    }

    /**
     * Executes the difference calculation payload against an established state.
     * Big O Complexity: O(N + M) where N = Current items, M = Incoming Items.
     *
     * @param currentState  What the existing framework or database currently holds.
     * @param incomingState The new records provided by the DataMapper.
     * @return DiffResult containing Three buckets (To_Insert, To_Update,
     *         To_Delete).
     */
    public DiffResult<T> discernDeltas(List<T> currentState, List<T> incomingState) {

        List<T> toInsert = new ArrayList<>();
        List<T> toUpdate = new ArrayList<>();
        List<T> toDelete = new ArrayList<>();

        // 1. Index Current State into a HashMap for O(1) lookups.
        Map<K, T> currentIdMap = new HashMap<>();
        for (T currentRecord : currentState) {
            currentIdMap.put(idExtractor.apply(currentRecord), currentRecord);
        }

        // 2. Iterate through Incoming State to identify Inserts and Updates
        for (T incomingRecord : incomingState) {
            K incomingId = idExtractor.apply(incomingRecord);

            if (!currentIdMap.containsKey(incomingId)) {
                // Not found in current state -> It's a new record
                toInsert.add(incomingRecord);
            } else {
                // Found in current state -> Check if the underlying values changed via Hashing
                T currentRecord = currentIdMap.get(incomingId);

                String incomingHash = stateHashExtractor.apply(incomingRecord);
                String currentHash = stateHashExtractor.apply(currentRecord);

                if (!incomingHash.equals(currentHash)) {
                    // The deep hash differs, the record was mutated in the new state
                    toUpdate.add(incomingRecord);
                }

                // Remove from the currentIdMap. Whatever is left in the map after this
                // entire loop means the record was completely dropped from the incoming state.
                currentIdMap.remove(incomingId);
            }
        }

        // 3. Any records remaining in the indexed Current State are Deletions
        toDelete.addAll(currentIdMap.values());

        return new DiffResult<>(toInsert, toUpdate, toDelete);
    }

    // --- Container DTO ---

    /**
     * Immutable data carrier representing exactly what action needs taking locally.
     */
    public static class DiffResult<T> {
        private final List<T> toInsert;
        private final List<T> toUpdate;
        private final List<T> toDelete;

        DiffResult(List<T> toInsert, List<T> toUpdate, List<T> toDelete) {
            this.toInsert = toInsert;
            this.toUpdate = toUpdate;
            this.toDelete = toDelete;
        }

        public List<T> getToInsert() {
            return toInsert;
        }

        public List<T> getToUpdate() {
            return toUpdate;
        }

        public List<T> getToDelete() {
            return toDelete;
        }

        public boolean hasChanges() {
            return !toInsert.isEmpty() || !toUpdate.isEmpty() || !toDelete.isEmpty();
        }
    }
}
