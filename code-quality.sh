#!/bin/bash
# 로컬에서 코드 품질을 확인하는 스크립트 (push 전 실행 권장)
# 사용법: ./code-quality.sh

KTLINT_PASS=true
DETEKT_PASS=true

echo ""
echo "========================================"
echo "  📋 Code Quality 검사"
echo "========================================"

# ── ktlint ───────────────────────────────────────────────────────
echo ""
echo "  🔍 ktlint 실행 중..."
if ./gradlew ktlintCheck 2>/dev/null; then
  KTLINT_COUNT=0
  KTLINT_STATUS="✅  PASS"
else
  KTLINT_PASS=false
  KTLINT_XML="build/reports/ktlint/ktlintMainSourceSetCheck/ktlintMainSourceSetCheck.xml"
  if [ -f "$KTLINT_XML" ]; then
    KTLINT_COUNT=$(grep -c "<error " "$KTLINT_XML" 2>/dev/null || echo 0)
  else
    KTLINT_COUNT="?"
  fi
  KTLINT_STATUS="❌  FAIL (${KTLINT_COUNT}건)"
fi

# ── detekt ───────────────────────────────────────────────────────
echo "  🔍 detekt 실행 중..."
if ./gradlew detekt 2>/dev/null; then
  DETEKT_COUNT=0
  DETEKT_STATUS="✅  PASS"
else
  DETEKT_PASS=false
  DETEKT_XML="build/reports/detekt/detekt.xml"
  if [ -f "$DETEKT_XML" ]; then
    DETEKT_COUNT=$(grep -c "<error " "$DETEKT_XML" 2>/dev/null || echo 0)
  else
    DETEKT_COUNT="?"
  fi
  DETEKT_STATUS="❌  FAIL (${DETEKT_COUNT}건)"
fi

# ── 결과 출력 ────────────────────────────────────────────────────
echo ""
echo "  ┌──────────────┬────────────────────────┐"
printf "  │ %-12s │ %-22s │\n" "ktlint" "$KTLINT_STATUS"
printf "  │ %-12s │ %-22s │\n" "detekt" "$DETEKT_STATUS"
echo "  └──────────────┴────────────────────────┘"

# ── ktlint 위반 상세 ─────────────────────────────────────────────
if [ "$KTLINT_PASS" = false ]; then
  echo ""
  echo "  📄 ktlint 위반 상세:"
  KTLINT_XML="build/reports/ktlint/ktlintMainSourceSetCheck/ktlintMainSourceSetCheck.xml"
  if [ -f "$KTLINT_XML" ]; then
    grep "<error " "$KTLINT_XML" \
      | sed 's/.*filename="\([^"]*\)".*line="\([^"]*\)".*message="\([^"]*\)".*/    \1:\2 → \3/' \
      | sed 's|.*/src/|\  src/|'
  fi
  echo ""
  echo "  💡 자동 수정: ./gradlew ktlintFormat"
fi

# ── detekt 위반 상세 ─────────────────────────────────────────────
if [ "$DETEKT_PASS" = false ]; then
  echo ""
  echo "  📄 detekt 위반 상세:"
  DETEKT_XML="build/reports/detekt/detekt.xml"
  if [ -f "$DETEKT_XML" ]; then
    grep "<error " "$DETEKT_XML" \
      | sed 's/.*filename="\([^"]*\)".*line="\([^"]*\)".*message="\([^"]*\)".*/    \1:\2 → \3/' \
      | sed 's|.*/src/|  src/|'
  fi
fi

# ── 최종 결과 ────────────────────────────────────────────────────
echo ""
if [ "$KTLINT_PASS" = true ] && [ "$DETEKT_PASS" = true ]; then
  echo "  ✅  모든 검사 통과 — push 해도 좋습니다!"
else
  echo "  ❌  위반 사항을 수정 후 push하세요."
  echo ""
  echo "  리포트 위치:"
  echo "    ktlint → build/reports/ktlint/"
  echo "    detekt → build/reports/detekt/detekt.html"
fi
echo ""
echo "========================================"
echo ""

# 위반이 있으면 exit 1 (git hook 등에서 활용 가능)
if [ "$KTLINT_PASS" = false ] || [ "$DETEKT_PASS" = false ]; then
  exit 1
fi
