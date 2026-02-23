#!/usr/bin/env bash
set -euo pipefail

DESC="${1:-}"
if [ -z "$DESC" ]; then
  echo "Usage: bash scripts/new-migration.sh \"add game genre table\""
  exit 1
fi

# Timestamp: YYYYMMDDHHmmss (UTC)
TS="$(date -u +%Y%m%d%H%M%S)"

# slugify (spaces -> underscore, lowercase, remove weird chars)
SLUG="$(echo "$DESC" \
  | tr '[:upper:]' '[:lower:]' \
  | sed -E 's/[^a-z0-9]+/_/g; s/^_+|_+$//g')"

DIR="src/main/resources/db/migration"
mkdir -p "$DIR"

FILE="$DIR/V${TS}__${SLUG}.sql"

cat > "$FILE" <<EOF
-- ${DESC}
-- created_at_utc: ${TS}

EOF

echo "Created: $FILE"
