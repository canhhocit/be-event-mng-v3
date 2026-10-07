#!/usr/bin/env bash
# Copy toàn bộ database Neon cũ (Mỹ) sang project Neon mới (Singapore) bằng pg_dump/pg_restore.
#
#   export SOURCE_DATABASE_URL='postgresql://USER:PASS@ep-...us-east-2.aws.neon.tech/neondb?sslmode=require'
#   export TARGET_DATABASE_URL='postgresql://USER:PASS@ep-...ap-southeast-1.aws.neon.tech/neondb?sslmode=require'
#   ./deploy/neon-copy.sh check   # DB nguồn có dữ liệu người dùng thật không (ngoài seed của Flyway)?
#   ./deploy/neon-copy.sh copy    # dump nguồn -> restore vào đích -> so số dòng từng bảng
#
# - Chỉ ĐỌC database nguồn. Database đích phải TRỐNG, nên chạy TRƯỚC lần deploy Cloud Run đầu tiên
#   (app khởi động sẽ tự chạy Flyway tạo bảng + seed).
# - Lấy URL trên Neon console > Connect, TẮT "Connection pooling"; lỡ dán host -pooler script tự bỏ.
# - Nên tắt backend cũ trước khi copy để không phát sinh dữ liệu mới trong lúc dump.
# - File dump giữ lại làm backup trong $BACKUP_DIR (mặc định ~/event-mng-db-backup).
set -euo pipefail

BACKUP_DIR=${BACKUP_DIR:-$HOME/event-mng-db-backup}

die() { echo "LỖI: $*" >&2; exit 1; }

usage() { sed -n '2,7p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 1; }

# pg_dump/pg_restore không chạy được qua PgBouncer (host -pooler) của Neon.
direct_url() {
  local url=$1
  if [[ $url == *-pooler.* ]]; then
    echo "  (bỏ -pooler khỏi host để dùng kết nối trực tiếp)" >&2
    url=${url/-pooler./.}
  fi
  echo "$url"
}

# host[:port]/database để in ra màn hình, không lộ mật khẩu.
where_of() { sed -E 's#^[a-z]+://([^@/]*@)?([^/?]+)(/[^?]*)?.*#\2\3#' <<<"$1"; }

q() { psql -X -A -t -q -v ON_ERROR_STOP=1 -d "$1" -c "$2"; }

server_major() { echo $(( $(q "$1" 'show server_version_num') / 10000 )); }

require_url() {
  local name=$1
  [[ -n ${!name:-} ]] || die "Chưa đặt $name (xem hướng dẫn ở đầu file)"
  [[ ${!name} == postgres://* || ${!name} == postgresql://* ]] \
    || die "$name phải có dạng postgresql://user:pass@host/db?sslmode=require (không phải jdbc:)"
}

check() {
  require_url SOURCE_DATABASE_URL
  local src
  src=$(direct_url "$SOURCE_DATABASE_URL")
  echo "Nguồn: $(where_of "$src") (PostgreSQL $(server_major "$src"))"
  echo
  echo "Lịch sử Flyway:"
  psql -X -q -d "$src" -v ON_ERROR_STOP=1 -c \
    "SELECT version, description, installed_on::timestamp(0) FROM flyway_schema_history WHERE success ORDER BY installed_rank"
  # Seed dùng NOW() lúc Flyway chạy: dòng tạo sau lần migrate cuối là do người dùng/app tạo ra.
  psql -X -q -d "$src" -v ON_ERROR_STOP=1 -c "
    WITH m AS (SELECT max(installed_on) + interval '5 minutes' AS t FROM flyway_schema_history WHERE success)
    SELECT 'users' AS bang, count(*) AS tong, count(*) FILTER (WHERE created_at > m.t) AS tao_sau_seed FROM users, m
    UNION ALL SELECT 'events', count(*), count(*) FILTER (WHERE created_at > m.t) FROM events, m
    UNION ALL SELECT 'orders', count(*), count(*) FILTER (WHERE created_at > m.t) FROM orders, m
    UNION ALL SELECT 'tickets', count(*), count(*) FILTER (WHERE created_at > m.t) FROM tickets, m
    UNION ALL SELECT 'vouchers', count(*), count(*) FILTER (WHERE created_at > m.t) FROM vouchers, m
    UNION ALL SELECT 'blog_posts', count(*), count(*) FILTER (WHERE created_at > m.t) FROM blog_posts, m"
  echo "Cột tao_sau_seed > 0 (nhất là orders/tickets/users) = có dữ liệu thật -> nên chạy: $0 copy"
}

compare_counts() {
  local src=$1 dst=$2 table a b bad=0
  printf '%-28s %10s %10s\n' "BẢNG" "NGUỒN" "ĐÍCH"
  for table in $(q "$src" "SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY 1"); do
    a=$(q "$src" "SELECT count(*) FROM public.\"$table\"")
    b=$(q "$dst" "SELECT count(*) FROM public.\"$table\"" 2>/dev/null || echo '?')
    if [[ $a == "$b" ]]; then
      printf '%-28s %10s %10s\n' "$table" "$a" "$b"
    else
      printf '%-28s %10s %10s  <-- KHÁC\n' "$table" "$a" "$b"
      bad=1
    fi
  done
  return $bad
}

copy() {
  require_url SOURCE_DATABASE_URL
  require_url TARGET_DATABASE_URL
  local src dst src_major dst_major client_major tables dump
  src=$(direct_url "$SOURCE_DATABASE_URL")
  dst=$(direct_url "$TARGET_DATABASE_URL")
  [[ $(where_of "$src") != "$(where_of "$dst")" ]] || die "Nguồn và đích là cùng một database"

  src_major=$(server_major "$src")
  dst_major=$(server_major "$dst")
  client_major=$(pg_dump --version | grep -oE '[0-9]+' | head -n1)
  echo "Nguồn: $(where_of "$src") (PostgreSQL $src_major)"
  echo "Đích:  $(where_of "$dst") (PostgreSQL $dst_major)"
  (( client_major >= src_major )) || die "pg_dump $client_major cũ hơn server nguồn ($src_major): cài postgresql-client-$src_major"
  (( dst_major >= src_major )) || die "DB đích (PostgreSQL $dst_major) cũ hơn nguồn ($src_major): tạo project Neon đích cùng version"

  tables=$(q "$dst" "SELECT count(*) FROM pg_tables WHERE schemaname = 'public'")
  [[ $tables == 0 ]] || die "DB đích đã có $tables bảng (có thể Cloud Run đã chạy Flyway trên đó).
  Trên Neon console tạo database/branch trống rồi chạy lại. Script không xóa dữ liệu."

  mkdir -p "$BACKUP_DIR"
  dump="$BACKUP_DIR/neon-$(date +%Y%m%d-%H%M%S).dump"
  echo
  echo "==> Dump nguồn -> $dump"
  (umask 077 && pg_dump -Fc --no-owner --no-acl -d "$src" -f "$dump")
  echo "==> Restore vào đích (1 transaction: lỗi là rollback toàn bộ)"
  pg_restore --no-owner --no-acl --single-transaction --exit-on-error -d "$dst" "$dump"
  echo
  echo "==> So số dòng"
  if compare_counts "$src" "$dst"; then
    echo "Copy xong. Flyway trên DB mới đã có lịch sử nên app sẽ không seed lại."
  else
    die "Số dòng lệch: backend cũ còn ghi vào DB nguồn trong lúc copy? Tắt backend cũ, tạo DB đích trống và chạy lại."
  fi
}

case ${1:-} in
  check) check ;;
  copy) copy ;;
  *) usage ;;
esac
