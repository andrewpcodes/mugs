CREATE TABLE IF NOT EXISTS mugs
(
    id            TEXT PRIMARY KEY,
    owner_user_id TEXT    NOT NULL,
    purchase_date TEXT    NOT NULL,
    renewal_date  TEXT    NOT NULL,
    cost          NUMERIC NOT NULL,
    size_ounces   NUMERIC NOT NULL,
    created_at    TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS mug_history
(
    id               TEXT PRIMARY KEY,
    mug_id           TEXT NOT NULL,
    added_by_user_id TEXT NOT NULL,
    entry            TEXT NOT NULL,
    created_at       TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (mug_id) REFERENCES mugs (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_mugs_owner_user_id ON mugs (owner_user_id);
CREATE INDEX IF NOT EXISTS idx_mug_history_mug_id ON mug_history (mug_id);
