#!/bin/sh
# 커밋 전에 대회 규정과 개인정보, 코드 길이를 검사한다.
# --all 이면 저장소의 모든 파일을, 없으면 이번 커밋에 들어갈 파일만 본다.
# 걸리면 이유를 적고 1로 끝난다. 검사기라서 이 파일과 빌드 도구는 내용 검사에서 뺀다.

MAX_LINES=300
fail=0

problem() {
    echo "막음: $1"
    fail=1
}

# 윈도우에서 만든 이름 목록의 줄바꿈과 빈 줄을 정리한다. 빈 줄은 모든 줄에 걸린다.
rm -f .check_failed .check_names
if [ -f .names.local ]; then
    tr -d '\r' < .names.local | grep -v '^$' > .check_names
fi

# quotepath를 꺼야 한글 파일 이름을 건너뛰지 않는다.
if [ "$1" = "--all" ]; then
    files=$(git -c core.quotepath=off ls-files)
else
    files=$(git -c core.quotepath=off diff --cached --name-only --diff-filter=ACMR)
    # 커밋 이름이 실명이 되지 않게 역할 별칭만 받는다.
    ident=$(git var GIT_AUTHOR_IDENT)
    case "$ident" in
        kq-*"@kidquest.invalid>"*) ;;
        *) problem "커밋 이름이 역할 별칭이 아니다. docs/rules.md의 git config를 실행한다." ;;
    esac
fi

echo "$files" | while IFS= read -r f; do
    [ -f "$f" ] || continue

    # 올라가면 안 되는 파일
    case "$f" in
        *.jks|*.keystore|*local.properties|*google-services.json|.names.local|*settings.local.json|.env|.env.*|*/.env|*/.env.*)
            echo "막음: 올리면 안 되는 파일이다 : $f"; echo x >> .check_failed; continue ;;
    esac

    # 빌드 도구와 검사기는 내용을 보지 않는다.
    case "$f" in
        gradlew|gradlew.bat|gradle/wrapper/*|scripts/check_repo.sh) continue ;;
    esac

    # 메일 주소. 저장소에서 쓰는 가짜 주소만 허용한다.
    if grep -IEo '[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}' "$f" | grep -qv '@kidquest\.invalid$'; then
        echo "막음: 메일 주소가 있다 : $f"; echo x >> .check_failed
    fi
    # 휴대전화 번호
    if grep -IEq '01[016789][- ]?[0-9]{3,4}[- ]?[0-9]{4}' "$f"; then
        echo "막음: 전화번호로 보이는 값이 있다 : $f"; echo x >> .check_failed
    fi
    # API 키와 비밀키
    if grep -IEq 'AKIA[0-9A-Z]{16}|gh[pousr]_[A-Za-z0-9]{20,}|sk-[A-Za-z0-9_-]{20,}|AIza[0-9A-Za-z_-]{30,}|BEGIN [A-Z ]*PRIVATE KEY' "$f"; then
        echo "막음: 키로 보이는 값이 있다 : $f"; echo x >> .check_failed
    fi
    # 팀원 이름. 목록은 각자 .names.local에 두고 올리지 않는다.
    if [ -s .check_names ] && grep -IFqf .check_names "$f"; then
        echo "막음: 이름 목록의 값이 있다 : $f"; echo x >> .check_failed
    fi

    # Kotlin 파일 규칙 : 길이와 맨 위 주석
    case "$f" in
        *.kt)
            lines=$(wc -l < "$f")
            if [ "$lines" -gt "$MAX_LINES" ]; then
                echo "막음: $MAX_LINES 줄이 넘는다($lines 줄). 파일을 나눈다 : $f"; echo x >> .check_failed
            fi
            if ! head -n 1 "$f" | grep -Eq '^(//|/\*)'; then
                echo "막음: 맨 위에 무엇을 하는 파일인지 주석이 없다 : $f"; echo x >> .check_failed
            fi ;;
    esac
done

# while이 파이프 안에서 돌아서 결과를 파일로 받는다.
if [ -f .check_failed ]; then
    fail=1
fi
rm -f .check_failed .check_names

if [ "$fail" -ne 0 ]; then
    exit 1
fi
echo "검사 통과"
