#!/usr/bin/env bash
# Thao tác Google Cloud cho backend (Cloud Run + Cloud Build + Secret Manager).
#
#   ./deploy/gcp.sh setup     <PROJECT_ID>          # 1 lần: bật API, tạo Artifact Registry, service account, phân quyền
#   ./deploy/gcp.sh check-env [FILE]                # kiểm tra file cấu hình (mặc định deploy/cloudrun.env), không gọi cloud
#   ./deploy/gcp.sh env       <PROJECT_ID> [FILE]   # đẩy file cấu hình thành version secret mới, khởi động lại service
#   ./deploy/gcp.sh deploy    <PROJECT_ID>          # build + deploy bằng Cloud Build từ code đang có trên máy
#   ./deploy/gcp.sh status    <PROJECT_ID>          # URL, các revision gần nhất, gọi thử /api/v1/ping
#
# Đặt YES=1 để bỏ qua câu hỏi xác nhận. Tên tài nguyên phải khớp substitutions trong cloudbuild.yaml.
set -euo pipefail

REGION=asia-southeast1
SERVICE=event-mng-api
AR_REPO=event-mng
RUNTIME_SA_NAME=event-mng-run
DEPLOYER_SA_NAME=event-mng-deployer
ENV_SECRET=event-mng-env

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
DEFAULT_ENV_FILE="$ROOT_DIR/deploy/cloudrun.env"
APP_PROPERTIES="$ROOT_DIR/src/main/resources/application.properties"

REQUIRED_KEYS=(
  FRONTEND_URL DATABASE_URL POSTGRES_USER POSTGRES_PASSWORD JWT_SECRET_KEY
  SMTP_USERNAME SMTP_PASSWORD MAIL_FROM
  PAYOS_CLIENT_ID PAYOS_API_KEY PAYOS_CHECKSUM_KEY
  CLOUDINARY_CLOUD_NAME CLOUDINARY_API_KEY CLOUDINARY_API_SECRET
)

die()  { echo "LỖI: $*" >&2; exit 1; }
warn() { echo "CẢNH BÁO: $*" >&2; }
step() { echo; echo "==> $*"; }

usage() { sed -n '2,10p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 1; }

sa_email() { echo "$2@$1.iam.gserviceaccount.com"; }

# IAM cần vài giây sau khi tạo service account mới nhận ra nó.
retry() {
  local i
  for i in 1 2 3 4 5 6; do
    "$@" && return 0
    echo "  ...thử lại sau 10s" >&2
    sleep 10
  done
  return 1
}

confirm() {
  local project=$1 name
  name=$(gcloud projects describe "$project" --format='value(name)') \
    || die "Không truy cập được project '$project' (đã chạy 'gcloud auth login' chưa?)"
  echo "Project: $project ($name) | Region: $REGION | Service: $SERVICE"
  [[ ${YES:-} == 1 ]] && return 0
  local answer
  read -r -p "Tiếp tục? [y/N] " answer
  [[ $answer == [yY] ]] || die "Đã hủy."
}

# Giá trị của KEY trong file .properties; dòng sau cùng thắng, giống cách Spring đọc.
prop() {
  grep -E "^[[:space:]]*$1[[:space:]]*[=:]" "$2" | tail -n1 \
    | sed -E 's/^[^=:]*[=:][[:space:]]*//' | tr -d '\r' || true
}

# Giá trị mặc định ${KEY:...} đang ghi cứng trong application.properties (repo public).
public_default() {
  grep -oE "\\\$\{$1:[^}]*\}" "$APP_PROPERTIES" | head -n1 | sed -E "s/^\\\$\{$1://; s/\}\$//" || true
}

check_env() {
  local file=$1 errors=0 key value
  [[ -f $file ]] || die "Không thấy $file. Tạo từ mẫu: cp deploy/cloudrun.env.example deploy/cloudrun.env"
  echo "Kiểm tra $file"

  for key in "${REQUIRED_KEYS[@]}"; do
    if [[ -z $(prop "$key" "$file") ]]; then echo "  - thiếu $key"; errors=$((errors + 1)); fi
  done

  # "KEY=" rỗng đè lên giá trị mặc định của Spring (vd. BREVO_API_KEY= làm mất fallback sang SMTP_PASSWORD).
  while IFS= read -r key; do
    echo "  - $key đang rỗng: xóa hoặc comment dòng này"; errors=$((errors + 1))
  done < <(grep -E '^[[:space:]]*[A-Za-z_][A-Za-z0-9_.-]*[[:space:]]*[=:][[:space:]]*$' "$file" \
    | sed -E 's/[[:space:]]*[=:].*//; s/^[[:space:]]*//' || true)

  while IFS= read -r key; do
    echo "  - $key vẫn là giá trị mẫu"; errors=$((errors + 1))
  done < <(grep -vE '^[[:space:]]*#' "$file" | grep -E 'thay-bang|ten-web-|ep-xxxx|xxxxxx@|email-da-xac-thuc' \
    | sed -E 's/[[:space:]]*[=:].*//; s/^[[:space:]]*//' || true)

  value=$(prop DATABASE_URL "$file")
  if [[ -n $value ]]; then
    [[ $value == jdbc:postgresql://* ]] || { echo "  - DATABASE_URL phải bắt đầu bằng jdbc:postgresql://"; errors=$((errors + 1)); }
    [[ $value != *@* ]] || { echo "  - DATABASE_URL không được chứa user:password@ (khai báo ở POSTGRES_USER/POSTGRES_PASSWORD)"; errors=$((errors + 1)); }
    [[ $value == *sslmode=require* ]] || warn "DATABASE_URL nên có ?sslmode=require (Neon bắt buộc SSL)"
    [[ $value != *us-east* ]] || warn "DATABASE_URL vẫn trỏ về Neon ở Mỹ (us-east)"
    [[ $value == *-pooler.* ]] || warn "DATABASE_URL nên dùng host -pooler cho app (bật Connection pooling trên Neon)"
  fi

  value=$(prop FRONTEND_URL "$file")
  if [[ -n $value && ( $value != https://* || $value == *localhost* ) ]]; then
    echo "  - FRONTEND_URL phải là URL https thật của web khách hàng (đang là: $value)"; errors=$((errors + 1))
  fi

  value=$(prop JWT_SECRET_KEY "$file")
  if [[ -n $value ]]; then
    (( ${#value} >= 64 )) || { echo "  - JWT_SECRET_KEY cần >= 64 ký tự cho HS512 (tạo: openssl rand -hex 64)"; errors=$((errors + 1)); }
    [[ $value != "$(public_default JWT_SECRET_KEY)" ]] || { echo "  - JWT_SECRET_KEY trùng giá trị mặc định đang public trên GitHub"; errors=$((errors + 1)); }
  fi

  for key in SMTP_PASSWORD PAYOS_API_KEY PAYOS_CHECKSUM_KEY; do
    value=$(prop "$key" "$file")
    if [[ -n $value && $value == "$(public_default "$key")" ]]; then
      warn "$key trùng giá trị đang public trong application.properties: nên tạo key mới (rotate)"
    fi
  done

  if [[ -n $(grep -vE '^[[:space:]]*#' "$file" | grep -F '\' || true) ]]; then
    warn "Có dấu \\ trong giá trị: .properties coi đó là ký tự escape"
  fi

  (( errors == 0 )) || die "$errors lỗi trong $file"
  echo "OK"
}

ensure_sa() {
  local project=$1 name=$2 display=$3
  if gcloud iam service-accounts describe "$(sa_email "$project" "$name")" --project "$project" >/dev/null 2>&1; then
    echo "  đã có $name"
  else
    gcloud iam service-accounts create "$name" --project "$project" --display-name "$display"
  fi
}

ensure_secret() {
  local project=$1
  if ! gcloud secrets describe "$ENV_SECRET" --project "$project" >/dev/null 2>&1; then
    gcloud secrets create "$ENV_SECRET" --project "$project" --replication-policy=automatic
  fi
  retry gcloud secrets add-iam-policy-binding "$ENV_SECRET" --project "$project" \
    --member="serviceAccount:$(sa_email "$project" "$RUNTIME_SA_NAME")" \
    --role=roles/secretmanager.secretAccessor --condition=None >/dev/null
}

service_exists() {
  gcloud run services describe "$SERVICE" --project "$1" --region "$REGION" >/dev/null 2>&1
}

setup() {
  local project=$1 deployer role
  confirm "$project"

  step "Bật API"
  gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com \
    secretmanager.googleapis.com iam.googleapis.com --project "$project"

  step "Artifact Registry: $AR_REPO ($REGION)"
  if gcloud artifacts repositories describe "$AR_REPO" --project "$project" --location "$REGION" >/dev/null 2>&1; then
    echo "  đã có $AR_REPO"
  else
    gcloud artifacts repositories create "$AR_REPO" --project "$project" --location "$REGION" \
      --repository-format=docker --description="Docker image cua backend event-mng"
  fi

  step "Service account"
  ensure_sa "$project" "$RUNTIME_SA_NAME" "Cloud Run runtime - $SERVICE"
  ensure_sa "$project" "$DEPLOYER_SA_NAME" "Cloud Build deployer - $SERVICE"

  step "Phân quyền cho $DEPLOYER_SA_NAME (service account chạy Cloud Build)"
  deployer="serviceAccount:$(sa_email "$project" "$DEPLOYER_SA_NAME")"
  # builds.builder: log, push Artifact Registry, đọc source upload. run.admin: deploy + cho phép truy cập public.
  # cloudbuild.readTokenAccessor: trigger GitHub (Cloud Build 2nd gen) đọc được source.
  for role in roles/cloudbuild.builds.builder roles/run.admin roles/cloudbuild.readTokenAccessor; do
    echo "  $role"
    retry gcloud projects add-iam-policy-binding "$project" --member="$deployer" --role="$role" \
      --condition=None >/dev/null
  done
  # Chỉ cần khi kết nối GitHub bằng Developer Connect.
  echo "  roles/developerconnect.readTokenAccessor"
  gcloud projects add-iam-policy-binding "$project" --member="$deployer" \
    --role=roles/developerconnect.readTokenAccessor --condition=None >/dev/null \
    || warn "Không cấp được roles/developerconnect.readTokenAccessor (chỉ cần nếu dùng Developer Connect)"
  echo "  roles/iam.serviceAccountUser trên $RUNTIME_SA_NAME"
  retry gcloud iam service-accounts add-iam-policy-binding "$(sa_email "$project" "$RUNTIME_SA_NAME")" \
    --project "$project" --member="$deployer" --role=roles/iam.serviceAccountUser --condition=None >/dev/null

  step "Secret $ENV_SECRET (chỉ $RUNTIME_SA_NAME được đọc)"
  ensure_secret "$project"

  echo
  echo "Xong. Tiếp theo: điền deploy/cloudrun.env rồi chạy ./deploy/gcp.sh env $project"
}

push_env() {
  local project=$1 file=$2 version
  check_env "$file"
  confirm "$project"
  ensure_secret "$project"

  # Bỏ \r (file soạn trên Windows) để mật khẩu không dính ký tự thừa.
  ENV_TMP=$(mktemp)
  trap 'rm -f "$ENV_TMP"' EXIT
  tr -d '\r' <"$file" >"$ENV_TMP"
  gcloud secrets versions add "$ENV_SECRET" --project "$project" --data-file="$ENV_TMP" >/dev/null
  version=$(gcloud secrets versions describe latest --secret "$ENV_SECRET" --project "$project" \
    --format='value(name.basename())')
  echo "Đã tạo $ENV_SECRET version $version"

  if service_exists "$project"; then
    step "Tạo revision mới để app đọc cấu hình vừa đẩy"
    gcloud run services update "$SERVICE" --project "$project" --region "$REGION" \
      --update-env-vars="ENV_SECRET_VERSION=$version"
  else
    echo "Service chưa tồn tại. Deploy lần đầu: ./deploy/gcp.sh deploy $project"
  fi
}

deploy() {
  local project=$1
  confirm "$project"
  gcloud secrets versions describe latest --secret "$ENV_SECRET" --project "$project" >/dev/null 2>&1 \
    || die "Secret $ENV_SECRET chưa có dữ liệu. Chạy trước: ./deploy/gcp.sh env $project"
  cd "$ROOT_DIR"
  # Source upload tôn trọng .gitignore: .env và deploy/cloudrun.env không bị gửi lên.
  gcloud builds submit . --project "$project" --config cloudbuild.yaml \
    --service-account="projects/$project/serviceAccounts/$(sa_email "$project" "$DEPLOYER_SA_NAME")"
  status "$project"
}

status() {
  local project=$1 url
  url=$(gcloud run services describe "$SERVICE" --project "$project" --region "$REGION" \
    --format='value(status.url)')
  echo "URL: $url"
  gcloud run revisions list --service "$SERVICE" --project "$project" --region "$REGION" --limit 3
  echo
  echo "GET $url/api/v1/ping"
  curl -fsS --max-time 60 "$url/api/v1/ping" && echo
}

cmd=${1:-}
[[ $# -gt 0 ]] && shift
case $cmd in
  setup | env | deploy | status) [[ -n ${1:-} ]] || usage ;;
esac
case $cmd in
  setup) setup "$1" ;;
  check-env) check_env "${1:-$DEFAULT_ENV_FILE}" ;;
  env) push_env "$1" "${2:-$DEFAULT_ENV_FILE}" ;;
  deploy) deploy "$1" ;;
  status) status "$1" ;;
  *) usage ;;
esac
