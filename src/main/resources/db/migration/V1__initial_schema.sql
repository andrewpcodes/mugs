CREATE TABLE mug_owners
(
    id         TEXT PRIMARY KEY,
    username   TEXT NOT NULL UNIQUE,
    email      TEXT NOT NULL UNIQUE,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mugs
(
    id            TEXT PRIMARY KEY,
    owner_user_id TEXT    NOT NULL,
    purchase_date TEXT    NOT NULL,
    renewal_date  TEXT    NOT NULL,
    cost          NUMERIC NOT NULL,
    size_ounces   NUMERIC NOT NULL,
    created_at    TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TEXT    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_user_id) REFERENCES mug_owners (id) ON DELETE CASCADE
);

CREATE TABLE mug_history
(
    id               TEXT PRIMARY KEY,
    mug_id           TEXT NOT NULL,
    added_by_user_id TEXT NOT NULL,
    entry            TEXT NOT NULL,
    created_at       TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (mug_id) REFERENCES mugs (id) ON DELETE CASCADE
);

CREATE TABLE mug_fills
(
    id               TEXT PRIMARY KEY,
    mug_id           TEXT NOT NULL,
    filled_by_user_id TEXT NOT NULL,
    fill_date        TEXT NOT NULL,
    amount_ounces    NUMERIC NOT NULL,
    created_at       TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (mug_id) REFERENCES mugs (id) ON DELETE CASCADE
);


CREATE INDEX idx_mugs_owner_user_id ON mugs (owner_user_id);
CREATE INDEX idx_mug_history_mug_id ON mug_history (mug_id);
CREATE INDEX idx_mug_fills_mug_id ON mug_fills (mug_id);