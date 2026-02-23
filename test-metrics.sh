#!/bin/bash
# 로컬에서 마이그레이션 메트릭을 확인하는 스크립트
# 사용법: ./test-metrics.sh
set -e

# ── Baseline (마이그레이션 시작 시점 기준) ──────────────────────
ORIGINAL_JAVA_FILES=215
ORIGINAL_JAVA_LINES=9487
# ────────────────────────────────────────────────────────────────

# 1. Migration Progress
JAVA_FILES=$(find src/main/java -name "*.java" 2>/dev/null | wc -l || echo 0)
KOTLIN_FILES=$(find src/main/kotlin -name "*.kt" 2>/dev/null | wc -l || echo 0)
MIGRATED=$((ORIGINAL_JAVA_FILES - JAVA_FILES))
PROGRESS=$((MIGRATED * 100 / ORIGINAL_JAVA_FILES))

filled=$((PROGRESS / 5))
bar=$(printf '█%.0s' $(seq 1 $filled 2>/dev/null); printf '░%.0s' $(seq 1 $((20 - filled)) 2>/dev/null))

# 2. Null Safety
BANG_COUNT=$(grep -r "!!" src/main/kotlin --include="*.kt" 2>/dev/null | wc -l || echo 0)
SAFE_CALL=$(grep -r "?\." src/main/kotlin --include="*.kt" 2>/dev/null | wc -l || echo 0)
ELVIS=$(grep -r "?:" src/main/kotlin --include="*.kt" 2>/dev/null | wc -l || echo 0)

if [ $((SAFE_CALL + ELVIS + BANG_COUNT)) -gt 0 ]; then
  SAFETY_SCORE=$(((SAFE_CALL + ELVIS) * 100 / (SAFE_CALL + ELVIS + BANG_COUNT)))
else
  SAFETY_SCORE=100
fi

if [ "$BANG_COUNT" -gt 10 ]; then SAFETY_EMOJI="🔴"
elif [ "$BANG_COUNT" -gt 5 ]; then SAFETY_EMOJI="🟡"
else SAFETY_EMOJI="🟢"
fi

# 3. Kotlin Idioms
DATA_CLASS=$(grep -rP "\bdata class\b" src/main/kotlin --include="*.kt" 2>/dev/null | wc -l || echo 0)
SEALED_CLASS=$(grep -rP "\bsealed class\b" src/main/kotlin --include="*.kt" 2>/dev/null | wc -l || echo 0)
EXTENSION_FN=$(grep -rPE "\bfun [A-Za-z]+\." src/main/kotlin --include="*.kt" 2>/dev/null | wc -l || echo 0)
WHEN_EXPR=$(grep -rP "when\s*\(" src/main/kotlin --include="*.kt" 2>/dev/null | wc -l || echo 0)

# 4. Code Reduction
JAVA_LINES=$(find src/main/java -name "*.java" -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}')
JAVA_LINES=${JAVA_LINES:-0}
KOTLIN_LINES=$(find src/main/kotlin -name "*.kt" -exec wc -l {} + 2>/dev/null | tail -1 | awk '{print $1}')
KOTLIN_LINES=${KOTLIN_LINES:-0}
CURRENT_TOTAL=$((JAVA_LINES + KOTLIN_LINES))
REDUCTION=$((ORIGINAL_JAVA_LINES - CURRENT_TOTAL))
REDUCTION_RATE=$((REDUCTION * 100 / ORIGINAL_JAVA_LINES))

# ── 출력 ─────────────────────────────────────────────────────────
echo ""
echo "========================================"
echo "  📊 Kotlin 마이그레이션 메트릭"
echo "========================================"
echo ""
echo "  🚀 진행률: $PROGRESS%"
echo "  [$bar] $PROGRESS%"
echo "  $MIGRATED / $ORIGINAL_JAVA_FILES 파일 완료"
echo ""
echo "  📁 파일 현황"
echo "  ┌─────────────────────────────┬───────┐"
printf "  │ %-27s │ %5s │\n" "원본 Java 파일 (baseline)" "$ORIGINAL_JAVA_FILES"
printf "  │ %-27s │ %5s │\n" "남은 Java 파일" "$JAVA_FILES"
printf "  │ %-27s │ %5s │\n" "변환된 Kotlin 파일" "$KOTLIN_FILES"
echo "  └─────────────────────────────┴───────┘"
echo ""
echo "  📉 코드 감소량"
echo "  ┌─────────────────────────────┬───────┐"
printf "  │ %-27s │ %5s │\n" "마이그레이션 전 (baseline)" "$ORIGINAL_JAVA_LINES"
printf "  │ %-27s │ %5s │\n" "현재 Java 라인 수" "$JAVA_LINES"
printf "  │ %-27s │ %5s │\n" "현재 Kotlin 라인 수" "$KOTLIN_LINES"
printf "  │ %-27s │ %5s │\n" "현재 전체 라인 수" "$CURRENT_TOTAL"
printf "  │ %-27s │ %4s%% │\n" "감소율" "$REDUCTION_RATE"
echo "  └─────────────────────────────┴───────┘"
echo ""
echo "  🛡️  Null 안전성: $SAFETY_SCORE% $SAFETY_EMOJI"
echo "  ┌─────────────────────────────┬───────┐"
printf "  │ %-27s │ %5s │\n" "!! 연산자 (위험)" "$BANG_COUNT"
printf "  │ %-27s │ %5s │\n" "안전 호출 (?.)" "$SAFE_CALL"
printf "  │ %-27s │ %5s │\n" "엘비스 연산자 (?:)" "$ELVIS"
echo "  └─────────────────────────────┴───────┘"
echo ""
echo "  🎯 Kotlin 관용 표현"
echo "  ┌─────────────────────────────┬───────┐"
printf "  │ %-27s │ %5s │\n" "data class" "$DATA_CLASS"
printf "  │ %-27s │ %5s │\n" "sealed class" "$SEALED_CLASS"
printf "  │ %-27s │ %5s │\n" "확장 함수" "$EXTENSION_FN"
printf "  │ %-27s │ %5s │\n" "when 표현식" "$WHEN_EXPR"
echo "  └─────────────────────────────┴───────┘"
echo ""
if [ "$BANG_COUNT" -gt 10 ]; then
  echo "  ⚠️  경고: !! 연산자가 ${BANG_COUNT}개 발견됐습니다."
  echo "      안전 호출(?.) 또는 엘비스 연산자(?:)로 교체해 주세요."
  echo ""
fi
echo "========================================"
echo ""
