# Subject & Topic Domain Design

**Date:** 2026-06-29
**Status:** Approved

## Overview

Introduce a two-level content hierarchy — Subject and Topic — and restructure Note and MCQ routes to be nested under subjects. A Note or MCQ must belong to a Subject, and may optionally belong to a Topic within that Subject.

```
User
└── Subject
    ├── Topic (optional grouping)
    └── Notes / MCQs (belong to subject; optionally scoped to a topic)
```

## Data Model

### Subject
- `id UUID PRIMARY KEY`
- `user_id UUID NOT NULL` → FK to `users(id)` ON DELETE CASCADE
- `name VARCHAR(200) NOT NULL`
- `description TEXT` (nullable)
- `created_at`, `updated_at` (from BaseEntity)

### Topic
- `id UUID PRIMARY KEY`
- `subject_id UUID NOT NULL` → FK to `subjects(id)` ON DELETE CASCADE
- `name VARCHAR(200) NOT NULL`
- `description TEXT` (nullable)
- `created_at`, `updated_at` (from BaseEntity)
- No direct `user_id` — user access is derived through the subject

### Note (additions)
- `subject_id UUID NOT NULL` → FK to `subjects(id)` ON DELETE CASCADE
- `topic_id UUID` (nullable) → FK to `topics(id)` ON DELETE SET NULL

### MCQ (additions)
- `subject_id UUID NOT NULL` → FK to `subjects(id)` ON DELETE CASCADE
- `topic_id UUID` (nullable) → FK to `topics(id)` ON DELETE SET NULL

## API Routes

### Subject
```
POST   /subjects                               Create subject
GET    /subjects/{subjectId}                   Get subject by ID
PUT    /subjects/{subjectId}                   Update subject
DELETE /subjects/{subjectId}                   Delete subject (cascades to topics, notes, mcqs)
```

### Topic (nested under subject)
```
POST   /subjects/{subjectId}/topics            Create topic
GET    /subjects/{subjectId}/topics/{topicId}  Get topic by ID
PUT    /subjects/{subjectId}/topics/{topicId}  Update topic
DELETE /subjects/{subjectId}/topics/{topicId}  Delete topic (sets topic_id = null on notes/mcqs)
```

### Note (moved under subject)
```
POST   /subjects/{subjectId}/notes             Create note (topicId optional in body)
GET    /subjects/{subjectId}/notes/{noteId}    Get note by ID
PUT    /subjects/{subjectId}/notes/{noteId}    Update note
DELETE /subjects/{subjectId}/notes/{noteId}    Delete note
```

### MCQ (moved under subject)
```
POST   /subjects/{subjectId}/mcqs              Create MCQ (topicId optional in body)
GET    /subjects/{subjectId}/mcqs/{mcqId}      Get MCQ by ID
PUT    /subjects/{subjectId}/mcqs/{mcqId}      Update MCQ
DELETE /subjects/{subjectId}/mcqs/{mcqId}      Delete MCQ
```

## Security & Scoping

- **Subject**: scoped directly via `findByIdAndUserId(subjectId, userId)`
- **Topic**: subject is validated first (`findByIdAndUserId`), then topic via `findByIdAndSubjectId(topicId, subjectId)` — no `user_id` on the topic table
- **Note/MCQ**: subject is validated (user-scoped), then note/mcq via `findByIdAndSubjectId`. If `topicId` is provided, validate topic belongs to the same subject before assigning.

## Validation Rules

- `name` is required on Subject and Topic (non-blank)
- When creating/updating a Note or MCQ with a `topicId`, the topic must belong to the same subject — service throws `BadRequestException` if not
- A Note or MCQ without a `topicId` is valid (topic is optional)

## DB Migrations

- **V3** (`V3__subjects_and_topics.sql`): create `subjects` and `topics` tables
- **V4** (`V4__notes_mcqs_subject_topic.sql`): add `subject_id` (NOT NULL) and `topic_id` (nullable) to `notes` and `mcqs` tables

## Files Changed / Added

### New — Subject domain
- `subject/Subject.java`
- `subject/SubjectRepository.java`
- `subject/SubjectService.java`
- `subject/SubjectController.java`
- `subject/SubjectMapper.java`
- `subject/SubjectNotFoundException.java`
- `subject/dto/SubjectRequest.java`
- `subject/dto/SubjectResponse.java`
- `subject/dto/SubjectUpdateRequest.java`

### New — Topic domain
- `topic/TopicRepository.java`
- `topic/TopicService.java`
- `topic/TopicController.java`
- `topic/TopicMapper.java`
- `topic/TopicNotFoundException.java`
- `topic/dto/TopicRequest.java`
- `topic/dto/TopicResponse.java`
- `topic/dto/TopicUpdateRequest.java`
- *(Topic.java stub already exists)*

### Modified — Note domain
- `note/Note.java` — add `subject`, `topic` fields
- `note/NoteService.java` — add subject/topic validation, update query scoping
- `note/NoteController.java` — move to `/subjects/{subjectId}/notes` nested routing
- `note/NoteMapper.java` — include subjectId/topicId in mappings
- `note/dto/NoteRequest.java` — add `subjectId` (removed, comes from path), `topicId` (optional)
- `note/dto/NoteResponse.java` — add `subjectId`, `topicId`

### Modified — MCQ domain
- `mcq/MCQ.java` — add `subject`, `topic` fields
- `mcq/MCQService.java` — add subject/topic validation, update query scoping
- `mcq/MCQController.java` — move to `/subjects/{subjectId}/mcqs` nested routing
- `mcq/MCQMapper.java` — include subjectId/topicId in mappings
- `mcq/MCQCreateRequest.java` — add `topicId` (optional)
- `mcq/dto/MCQResponse.java` — add `subjectId`, `topicId`
- `mcq/dto/MCQUpdateRequest.java` — add `topicId` (optional)

### New — Migrations
- `db/migration/V3__subjects_and_topics.sql`
- `db/migration/V4__notes_mcqs_subject_topic.sql`
