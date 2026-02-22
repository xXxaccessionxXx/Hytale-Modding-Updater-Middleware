package com.hytale.updater.simulation;

import com.hytale.updater.diff.DatasetDiffEngine;
import com.hytale.updater.mapper.DataMapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Simulates a server boot sequence where a user downloaded an outdated Mod from
 * Curseforge.
 * The Server invokes the Updater Middleware before finalizing the load.
 */
public class ModLoadingSimulation {

    // --- Mocks: Simulating parsed JSON from an old CurseForge Mod ---
    private static final Map<String, Object> OUTDATED_SWORD_JSON = Map.of(
            "old_item_id", "mod_epic_sword",
            "damage", 50,
            "durability", 200
    // Notice: Missing the new required 'rarity' field that Hytale v1.2.0 demands
    );

    private static final Map<String, Object> OUTDATED_SHIELD_JSON = Map.of(
            "old_item_id", "mod_epic_shield",
            "defense", 25,
            "durability", 500);

    // --- Mocks: Simulating current Server State before Mod Loads ---
    private static final List<HytaleItemAsset> CURRENT_SERVER_REGISTRY = List.of(
            new HytaleItemAsset("minecraft_iron_sword", 20, "COMMON"),
            new HytaleItemAsset("mod_epic_sword", 50, "EPIC") // The Server already had an old version of this mod
                                                              // installed!
    );

    public static void main(String[] args) {
        System.out.println("--- [Hytale Server] Booting v1.2.0 ---");
        System.out.println("[CurseforgeLoader] Found mod: 'EpicWeapons_v1.0.zip'");
        System.out.println("[Middleware] Intercepting load. Checking asset compatibility...");

        // 1. Configure the Mapper for the new v1.2.0 Engine Schema
        DataMapper<HytaleItemAsset> assetMapper = new DataMapper<>(payload -> new HytaleItemAsset());

        // Map old keys to new schema. Provide default fallback for new required fields
        // (like 'rarity')
        assetMapper.withRule("old_item_id", String.class, true, HytaleItemAsset::setId)
                .withRule("damage", Integer.class, false, HytaleItemAsset::setPower)
                .withRule("defense", Integer.class, false, HytaleItemAsset::setPower) // Map both old stats to new
                                                                                      // 'power'
                .withRule("rarity", String.class, false, (asset, val) -> {
                    asset.setRarity(val != null ? (String) val : "UNCOMMON"); // Fallback applied!
                });

        // 2. Parse the Outdated JSONs through the Mapper
        System.out.println("\n--- [Middleware: Alignment Phase] ---");
        List<Map<String, Object>> rawModFiles = List.of(OUTDATED_SWORD_JSON, OUTDATED_SHIELD_JSON);
        List<HytaleItemAsset> incomingModAssets = new ArrayList<>();

        for (Map<String, Object> rawJson : rawModFiles) {
            Optional<HytaleItemAsset> mappedAsset = assetMapper.map(rawJson);
            mappedAsset.ifPresent(asset -> {
                // If "rarity" wasn't in the JSON, our fallback logic above handles it
                if (asset.getRarity() == null)
                    asset.setRarity("UNCOMMON");
                incomingModAssets.add(asset);
                System.out.println("Mapped Asset: " + asset.getId() + " | Power: " + asset.getPower() + " | Rarity: "
                        + asset.getRarity());
            });
        }

        // 3. Diff Engine against the Current Server Registry
        System.out.println("\n--- [Middleware: Discernment Phase] ---");
        DatasetDiffEngine<HytaleItemAsset, String> diffEngine = new DatasetDiffEngine<>(
                HytaleItemAsset::getId, // Unique key
                HytaleItemAsset::computeHash // Deep compare
        );

        DatasetDiffEngine.DiffResult<HytaleItemAsset> diffResult = diffEngine.discernDeltas(CURRENT_SERVER_REGISTRY,
                incomingModAssets);

        // 4. Results Phase
        System.out.println("Engine Analytics:");

        System.out.println("  [To Insert] (New Items from Mod): ");
        diffResult.getToInsert().forEach(item -> System.out.println("    + " + item.getId()));

        System.out.println("  [To Update] (Existing Mod Items being overwritten): ");
        diffResult.getToUpdate().forEach(item -> System.out.println("    ~ " + item.getId()));

        System.out.println("  [To Delete] (Items on Server but missing from Mod/Update): ");
        diffResult.getToDelete().forEach(item -> System.out.println("    - " + item.getId()));

        System.out.println("\n[Middleware] Modification complete. Auto-Repacking into RAM...");
        System.out.println("[Hytale Server] Continuing boot sequence.");
    }

    // --- Strictly Typed Destination Schema (v1.2.0 API) ---
    private static class HytaleItemAsset {
        private String id;
        private int power;
        private String rarity;

        public HytaleItemAsset() {
        }

        public HytaleItemAsset(String id, int power, String rarity) {
            this.id = id;
            this.power = power;
            this.rarity = rarity;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public void setPower(int power) {
            this.power = power;
        }

        public int getPower() {
            return power;
        }

        public void setRarity(String rarity) {
            this.rarity = rarity;
        }

        public String getRarity() {
            return rarity;
        }

        // Mocks a deep hash representing the exact state of the object's data
        public String computeHash() {
            return id + ":" + power + ":" + rarity;
        }
    }
}
