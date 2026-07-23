# `:core:database`

Room persistence module for Face. Hosts the SQLite database, entities, and DAOs.

## Contents

| Type | Visibility | Role |
|------|------------|------|
| [FaceDatabase] | `internal` | Room database (`face-database`, version 1) |
| [PersonEntity] | public | Enrolled person identity |
| [FaceEmbeddingEntity] | public | Face embedding blob (FK → person, cascade delete) |
| [PersonDao] | public | Observe / upsert / delete persons |
| [FaceEmbeddingDao] | public | Upsert / query embeddings |
| [EmbeddingConverters] | public | FloatArray ↔ little-endian blob |
| [DatabaseModule] / [DaosModule] | `internal` | Hilt providers |

## Schema notes

- Person `id` is a UUID string.
- Embeddings are stored as little-endian `Float` blobs via [EmbeddingConverters].
- Pre-release builds use destructive migration when the schema version changes.

## Dependencies

- Room (via `hrka.android.room`)
- Hilt (via `hrka.android.hilt`)

Schemas are exported under `schemas/` for AutoMigration support.

## Usage

```kotlin
implementation(projects.core.database)
```

Inject DAOs (not [FaceDatabase]) from Hilt.

[FaceDatabase]: src/main/java/ir/hrka/database/FaceDatabase.kt
[PersonEntity]: src/main/java/ir/hrka/database/model/PersonEntity.kt
[FaceEmbeddingEntity]: src/main/java/ir/hrka/database/model/FaceEmbeddingEntity.kt
[PersonDao]: src/main/java/ir/hrka/database/dao/PersonDao.kt
[FaceEmbeddingDao]: src/main/java/ir/hrka/database/dao/FaceEmbeddingDao.kt
[EmbeddingConverters]: src/main/java/ir/hrka/database/util/EmbeddingConverters.kt
[DatabaseModule]: src/main/java/ir/hrka/database/di/DatabaseModule.kt
