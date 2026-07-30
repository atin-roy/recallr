UPDATE notebooks SET name = trim(name) WHERE name <> trim(name);

-- Existing case-insensitive duplicates would block the unique index. Keep the oldest
-- untouched and suffix the rest so nothing is deleted.
WITH ranked AS (
    SELECT id, row_number() OVER (PARTITION BY user_id, lower(name) ORDER BY created_at, id) AS rn
    FROM notebooks
)
UPDATE notebooks n
SET name = left(n.name, 190) || ' (' || r.rn || ')'
FROM ranked r
WHERE n.id = r.id AND r.rn > 1;

CREATE UNIQUE INDEX uk_notebooks_user_name ON notebooks (user_id, lower(name));
