# Hytale Middleware Watcher and Updater Architecture

## 1. System Architecture & Logical Flow

The Middleware Watcher and Updater is designed as a standalone pipeline application (likely running as a daemon or CLI tool) that intercepts the mod loading process or runs preemptively before the server starts.

### 1.1 Modules
*   **Version Watcher**: The entry point. It runs a scheduled task to poll the official Hytale versions manifest.
*   **Diff Engine (The Mapper)**: The analysis core. It ingests two JARs (Old API and New API), compares them structurally, and produces a `TranslationDictionary`.
*   **Translation Dictionary**: The shared artifact between the Mapper and the Updaters. It contains mappings of classes, methods, and fields from version A to version B.
*   **JSON Asset Migrator**: Processes declarative mod configurations and assets, modifying JSON schemas to match the updated engine expectations.
*   **Dynamic Bytecode Transformer**: The code rewriter. Operates at the `.class` file level using ObjectWeb ASM to patch instructions statically.
*   **Auto-Repacker**: The finalizer. Takes the in-memory or temporary disk representations of the modified assets and bytecode and streams them into a finalized `.jar` or `.zip`.

### 1.2 Logical Flow
1.  **Poll & Trigger**: `Version Watcher` detects a new version (e.g., v1.1.0 -> v1.2.0). It downloads `hytale-server-v1.2.0.jar`.
2.  **Diff Generation**: `Watcher` invokes the `Diff Engine` with `hytale-server-v1.1.0.jar` and `hytale-server-v1.2.0.jar`.
3.  **Dictionary Creation**: `Diff Engine` outputs `TranslationDictionary.json`.
4.  **Mod Discovery**: The system scans the server's `mods/` directory for outdated mods.
5.  **Unpacking**: For each mod, the internal assets (`.json`, `.png`) and compiled classes (`.class`) are extracted to a temporary workspace.
6.  **Asset Migration**: The `JSON Asset Migrator` consumes the JSON files, applying structural rules (derived from the dictionary or manual definition) to update structures (e.g., an item ID format change).
7.  **Bytecode Transformation**: The `Dynamic Bytecode Transformer` iterates over all `.class` files. It consults the `TranslationDictionary` and rewrites any `INVOKEVIRTUAL`, `GETFIELD`, class instantiation, and method signatures that refer to the old Hytale API to point to the new API.
8.  **Repacking**: The `Auto-Repacker` zips the temporary workspace back into a final JAR file in the `mods_updated/` folder.
9.  **Server Launch**: The system bootstraps the Hytale server with the updated mods.

---

## 2. Diff Engine Logic (Programmatic Dictionary Generation)

Generating a reliable `TranslationDictionary` between two obfuscated or structurally modified JARs requires heuristic and structural analysis.

### Abstract Syntax Tree / Class Node Analysis
Using ASM's `ClassNode` (Tree API), the Diff Engine loads every class from both Version A and Version B.

**Phase 1: Indexing & Hashing (The Baseline)**
1.  Read all `OldVersion` classes. Hash their structural perimeter (number of methods, field types, string constants, control flow graph sizes).
2.  Read all `NewVersion` classes and do the same.
3.  Find Exact Matches: Any class with identical name and identical structure is marked as "Unchanged."

**Phase 2: Name Matching (Tracking Renames / Refactors)**
When a class or method changes its name, we must track it using heavily weighted heuristics:
1.  **String Constant Analysis**: Classes often contain identical string constants across versions. If `ClassA (old)` and `ClassX (new)` both contain the string `"hytale:combat_system_init"`, they are highly likely the same class.
2.  **Signature Matching**: If `OldOwner.foo(String, int) -> boolean` disappears, and `NewOwner.bar(String, int) -> boolean` appears, and the surrounding instructions (the method body hashes) are ~90% similar, `OldOwner.foo` maps to `NewOwner.bar`.
3.  **Hierarchy Matching**: If an interface `ITickable` was renamed to `TickListener`, the implementers form a cluster. If the children's implementations match, the parent interface rename is mapped.

**Phase 3: Building the Dictionary**
Once mappings are resolved via heuristics, compile a Dictionary. A typical schema would look like this internally:

```json
{
  "classes": {
    "com/hytale/api/OldPlayerClass": "com/hytale/api/entities/Player"
  },
  "methods": {
    "com/hytale/api/OldPlayerClass.getHealth()I": "com/hytale/api/entities/Player.calculateHealth()I"
  },
  "fields": {
    "com/hytale/api/OldPlayerClass.maxSpeed:F": "com/hytale/api/entities/Player.baseSpeed:F"
  }
}
```

**Phase 4: Resolving Complex Changes (e.g., Altered Signatures)**
If a method signature changes (e.g., `setDamage(int)` -> `setDamage(float)`), the Diff Engine maps the signature change. The Bytecode Transformer will later need to insert a cast instruction (`I2F` - Integer to Float) before calling the new method to ensure JVM frame stack validity.
