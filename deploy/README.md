# Hướng dẫn deploy backend: Cloud Run + Cloud Build + Supabase

```
Vercel (fe, FE-manager-v3) ──HTTPS──▶ Cloud Run asia-southeast1: event-mng-api ──Session pooler──▶ Supabase (Singapore)
                                              ▲
GitHub main ──trigger──▶ Cloud Build ─────────┘  (build Dockerfile → Artifact Registry → deploy)
```

| Thông tin | Giá trị |
|---|---|
| Project GCP | `event-mng-510915` (project number `431351932095`) |
| Region | `asia-southeast1` (Singapore) |
| Cloud Run service | `event-mng-api` |
| URL backend | `https://event-mng-api-431351932095.asia-southeast1.run.app` |
| Artifact Registry | `event-mng` (tự xoá image cũ, giữ 3 bản mới nhất) |
| Service account chạy app | `event-mng-run` (chỉ được đọc secret `event-mng-env`) |
| Service account chạy build | `event-mng-deployer` |
| Secret cấu hình | `event-mng-env` (cả file `deploy/cloudrun.env`, mount tại `/secrets/app.properties`) |
| Database | Supabase, region Southeast Asia (Singapore), kết nối qua **Session pooler** |

| File | Vai trò |
|---|---|
| [`cloudbuild.yaml`](../cloudbuild.yaml) | Pipeline build/deploy **và** cấu hình runtime (CPU, RAM, billing, scaling, secret) |
| [`deploy/gcp.sh`](gcp.sh) | `setup`, `check-env`, `env`, `deploy`, `status`, `keepalive` |
| [`deploy/cloudrun.env.example`](cloudrun.env.example) | Mẫu file cấu hình bí mật |
| [`deploy/db-copy.sh`](db-copy.sh) | Tuỳ chọn: copy dữ liệu cũ từ Neon sang Supabase |

Cần cài: [gcloud CLI](https://cloud.google.com/sdk/docs/install) (đã `gcloud auth login`), `psql`/`pg_dump` nếu copy dữ liệu cũ.

---

## Database tạo như thế nào?

Bạn chỉ tạo một project Supabase **trống**. Lần đầu app khởi động, Flyway tự chạy các file trong `src/main/resources/db/migration`:

- `V1`: tạo bảng.
- `V2`–`V6`: seed dữ liệu mẫu (tài khoản, sự kiện, vé...).
- `V7`: thu hồi quyền của Data API trên Supabase.

Không cần tạo bảng hay chạy SQL bằng tay. Chỉ khi muốn giữ dữ liệu thật từ DB cũ mới cần làm [bước 2](#2-tuỳ-chọn-copy-dữ-liệu-cũ-từ-neon).

## Chế độ chạy: tiết kiệm (scale về 0)

`cloudbuild.yaml` đặt **Request-based** billing (`--cpu-throttling`), **min 0 / max 1** instance:

- Không có request thì instance tắt, không mất tiền.
- Tối đa 1 instance: giới hạn chi phí và giữ cache Spring nhất quán.
- **Đánh đổi:** người đầu tiên vào sau khi server ngủ phải chờ app khởi động khoảng 20–40 giây.

Ngoài request, CPU bị bóp gần như về 0, nên code đã được chỉnh:

| Biến/cơ chế | Tác dụng |
|---|---|
| `APP_ASYNC_ENABLED=false` | Hoàn tất thanh toán, tạo PDF, gửi mail chạy ngay trong request (`BackgroundTaskRunner`); mail gửi sau khi DB commit |
| `APP_PING_ENABLED=false` | Tắt `PingTask` (tự gọi chính mình mỗi 5 phút) |
| `EventStatusRefreshInterceptor` | Nếu `EventStatusTask` đã quá 1 phút chưa chạy, request cập nhật trạng thái sự kiện trước khi xử lý (checkout dựa vào status `OPENING`) |

Chạy local/VPS không đặt các biến trên nên vẫn chạy nền như cũ.

**Muốn hết chờ khởi động** (tốn khoảng 55–60 USD/tháng): trong `cloudbuild.yaml` đổi `--cpu-throttling` → `--no-cpu-throttling`, `--min=0` → `--min=1`, `APP_ASYNC_ENABLED=false` → `true`.

---

## Các bước

| # | Bước | Ai làm | Trạng thái |
|---|---|---|---|
| 0 | [Code và nhánh](#0-code-và-nhánh) | bạn | |
| 1 | [Tạo Supabase](#1-tạo-supabase) | bạn | |
| 2 | [(Tuỳ chọn) Copy dữ liệu cũ từ Neon](#2-tuỳ-chọn-copy-dữ-liệu-cũ-từ-neon) | bạn | |
| 3 | [Project GCP + setup](#3-project-gcp--setup) | | ✅ đã xong 07/10/2026 |
| 4 | [File cấu hình → Secret Manager](#4-file-cấu-hình--secret-manager) | bạn | |
| 5 | [Deploy lần đầu](#5-deploy-lần-đầu) | bạn | |
| 6 | [Kiểm tra trên console Cloud Run](#6-kiểm-tra-trên-console-cloud-run) | bạn | |
| 7 | [Chống Supabase tự tạm dừng](#7-chống-supabase-tự-tạm-dừng) | bạn | |
| 8 | [Cảnh báo chi phí](#8-cảnh-báo-chi-phí) | bạn | |
| 9 | [Cloud Build trigger](#9-cloud-build-trigger-tự-deploy-khi-merge-main) | canhhocit + bạn | |
| 10 | [Chuyển frontend + PayOS](#10-chuyển-frontend--payos) | bạn | |
| 11 | [Tắt VPS, xoá Neon](#11-tắt-vps-xoá-neon) | bạn | |

---

## 0. Code và nhánh

- Code trên nhánh `minh`, mở Pull Request vào `main`. Trigger chỉ deploy `main`. Không dùng nhánh `deploy` (bỏ từ 07/2026).
- Commit và push trước khi làm tiếp:

  ```bash
  cd be
  git add -A && git commit -m "chore: chuyển database sang Supabase"
  git push origin minh
  ```

## 1. Tạo Supabase

**1.1. Tạo project**

1. Vào [supabase.com/dashboard](https://supabase.com/dashboard) → **New project**.
2. Điền:
   - **Project name**: `event-mng`
   - **Database password**: bấm **Generate a password**, lưu lại ngay (dùng ở bước 4)
   - **Region**: **Southeast Asia (Singapore)**, cùng thành phố với Cloud Run
   - Gói **Free**
3. Nếu có mục *Security* / *Data API*, bỏ chọn Data API. Sau đó bấm **Create new project** và chờ 1–2 phút.

**1.2. Tắt Data API** (bắt buộc)

App không dùng Data API (REST tự sinh của Supabase). Nếu để bật, bảng trong `public` có thể bị đọc/sửa bằng anon key, chẳng hạn đọc `users` hay sửa `orders`. Migration `V7` đã thu hồi các quyền đó, nhưng vẫn nên tắt hẳn:

- Menu trái → **Integrations** → **Data API** → tắt **Enable Data API**.

**1.3. Lấy connection string**

Bấm **Connect** ở đầu trang project → tab **Connection String** → **Method**: chọn **Session pooler**.

| Kiểu kết nối | Dùng được không | Lý do |
|---|---|---|
| Direct connection (`db.<ref>.supabase.co`) | ❌ | Chỉ có IPv6, Cloud Run không gọi ra IPv6 được |
| Transaction pooler (cổng `6543`) | ❌ | Không hỗ trợ prepared statement của driver Java |
| **Session pooler** (cổng `5432`) | ✅ | IPv4, giữ nguyên session |

Supabase đưa chuỗi dạng:

```
postgresql://postgres.abcdefghijklmnopqrst:[YOUR-PASSWORD]@aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres
```

Tách thành 3 dòng cho file cấu hình. Host thật có thể là `aws-1-...`; copy đúng host của bạn:

```properties
DATABASE_URL=jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres?sslmode=require
POSTGRES_USER=postgres.abcdefghijklmnopqrst
POSTGRES_PASSWORD=mat-khau-o-buoc-1.1
```

Quên mật khẩu: **Project Settings** → **Database** → **Reset database password**.

## 2. (Tuỳ chọn) Copy dữ liệu cũ từ Neon

Bỏ qua nếu không cần dữ liệu cũ. Nếu cần, **phải làm trước bước 5**, vì sau khi app chạy thì Supabase đã có bảng.

```bash
export SOURCE_DATABASE_URL='postgresql://USER:PASS@ep-...us-east-2.aws.neon.tech/neondb?sslmode=require'   # Neon: tắt Connection pooling
export TARGET_DATABASE_URL='postgresql://postgres.REF:PASS@aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres?sslmode=require'

./deploy/db-copy.sh check   # cột tao_sau_seed > 0 ở orders/tickets/users = có dữ liệu thật
./deploy/db-copy.sh copy    # nên tắt backend cũ trước (docker stop event-mng trên VPS)
```

Script làm những việc sau:

- Chỉ đọc DB nguồn, từ chối chạy nếu Supabase đã có bảng.
- Restore trong một transaction, lỗi thì rollback toàn bộ.
- Thu hồi quyền Data API, rồi so số dòng từng bảng.
- File dump giữ ở `~/event-mng-db-backup/`.

Mật khẩu có ký tự đặc biệt (`@`, `/`, `:`, `#`...) thì phải mã hoá URL, ví dụ `@` → `%40`.

## 3. Project GCP + setup

Đã làm xong cho `event-mng-510915`. Chạy lại bao nhiêu lần cũng được (ví dụ khi nghi thiếu quyền):

```bash
./deploy/gcp.sh setup event-mng-510915
```

| Tài nguyên | Tên | Ghi chú |
|---|---|---|
| API | Cloud Run, Cloud Build, Artifact Registry, Secret Manager, IAM | |
| Artifact Registry | `event-mng` (Docker, `asia-southeast1`) | Giữ 3 image mới nhất, xoá image cũ hơn 7 ngày |
| Service account chạy app | `event-mng-run` | Secret Manager Secret Accessor trên `event-mng-env` |
| Service account chạy build | `event-mng-deployer` | Cloud Build Service Account, Cloud Run Admin, Cloud Build / Developer Connect Read Token Accessor, Service Account User trên `event-mng-run` |
| Secret | `event-mng-env` | Bước 4 đẩy dữ liệu |

## 4. File cấu hình → Secret Manager

```bash
cp deploy/cloudrun.env.example deploy/cloudrun.env   # file thật, đã nằm trong .gitignore
```

Mở `deploy/cloudrun.env`, điền:

| Key | Lấy ở đâu |
|---|---|
| `DATABASE_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Bước 1.3 (Session pooler) |
| `HIKARI_MAX_POOL_SIZE` | Để `5`: Session pooler gói Free giới hạn số kết nối |
| `FRONTEND_URL`, `CORS_ALLOWED_ORIGINS` | Domain Vercel của web khách hàng (và web quản trị) |
| `JWT_SECRET_KEY` | Chạy `openssl rand -hex 64` |
| `SMTP_USERNAME`, `SMTP_PASSWORD`, `MAIL_FROM` | Brevo → SMTP & API |
| `PAYOS_CLIENT_ID`, `PAYOS_API_KEY`, `PAYOS_CHECKSUM_KEY` | my.payos.vn → kênh thanh toán. Nên tạo key mới: key cũ đang nằm công khai trong `application.properties` |
| `CLOUDINARY_*` | Cloudinary → Dashboard |

Kiểm tra rồi đẩy lên:

```bash
./deploy/gcp.sh check-env                 # phải ra OK
./deploy/gcp.sh env event-mng-510915      # hỏi "Tiếp tục? [y/N]" → y
```

`check-env` báo lỗi nếu bạn:

- dán nhầm Direct connection hoặc cổng 6543,
- để user thiếu `.<ref>`,
- còn giá trị mẫu, URL sai định dạng, hoặc JWT quá ngắn.

**Làm trên console thay cho lệnh `env`:** **Secret Manager** → dòng `event-mng-env` → menu **Actions** (⋮) → **Add new version** → dán nội dung file vào **Secret value** → **Add new version**.

## 5. Deploy lần đầu

```bash
./deploy/gcp.sh deploy event-mng-510915
```

Lệnh này gửi code trên máy lên Cloud Build, chạy 3 bước khai báo trong `cloudbuild.yaml`:

| Bước | Việc | Thời gian |
|---|---|---|
| `build` | Docker build theo `Dockerfile` | khoảng 5 phút |
| `push` | Đẩy image lên Artifact Registry `event-mng` | khoảng 30 giây |
| `deploy` | Tạo/cập nhật service Cloud Run với toàn bộ cấu hình, đợi app khởi động | 1–2 phút |

Theo dõi: **Cloud Build** → **History** → ô **Region** chọn **global** (build gửi từ máy nằm ở đây) → bấm vào build → cột trái có 3 bước, bấm từng bước để xem log.

Xong, script in URL và kết quả `/api/v1/ping`.

- Mở `https://event-mng-api-431351932095.asia-southeast1.run.app/swagger-ui/index.html` để thử API.
- **Đổi mật khẩu `admin` ngay**: seed đặt sẵn là `123456`.
- Kiểm tra Supabase: **Table Editor** có các bảng `users`, `events`, `orders`... và bảng `flyway_schema_history` có 7 dòng.

## 6. Kiểm tra trên console Cloud Run

**Cloud Run** → **Services** → **event-mng-api**:

| Tab | Phải thấy |
|---|---|
| Đầu trang | URL `https://event-mng-api-431351932095.asia-southeast1.run.app` |
| **Containers** | **CPU limit** `1`, Memory `1 GiB`, **Startup CPU boost** ✓, port `8080` |
| ↳ Variables & Secrets | `SPRING_CONFIG_ADDITIONAL_LOCATION`, `BACKEND_URL`, `SEED_ENABLED=false`, `APP_ASYNC_ENABLED=false`, `APP_PING_ENABLED=false` |
| ↳ Volume (secret) | mount path `/secrets`, file `app.properties`, secret `event-mng-env`, version `latest` |
| **Scaling** | Billing **Request-based**; Service scaling: min `0`, max `1` |
| **Security** | **Allow public access**; Service account `event-mng-run@event-mng-510915.iam.gserviceaccount.com` |
| **Revision history** | Revision mới nhất nhận 100% traffic |
| **Logs** | `Successfully applied 7 migrations` và `Started EventMngApplication` |

- Không sửa CPU/RAM/scaling/billing trên console: lần deploy sau `cloudbuild.yaml` sẽ ghi đè. Muốn đổi thì sửa file đó.
- Trên console, mọi chỉnh sửa đều kết thúc bằng **View diff & redeploy** → **Deploy changes**.

## 7. Chống Supabase tự tạm dừng

Gói Free tạm dừng project nếu **7 ngày gần như không có truy vấn**. Vì app scale về 0, một tuần không ai vào là DB bị dừng và app lỗi, phải vào dashboard bấm **Resume project**.

Tạo lịch Cloud Scheduler gọi `/api/v1/ping` 12 giờ một lần (0h và 12h). Mỗi lần gọi, app thức dậy, chạy vài truy vấn rồi ngủ lại. Cloud Scheduler miễn phí 3 lịch mỗi billing account.

```bash
./deploy/gcp.sh keepalive event-mng-510915
```

Xem/chạy thử: **Cloud Scheduler** → `event-mng-keepalive` → **Force run**.

## 8. Cảnh báo chi phí

Budget **không chặn** chi tiêu, chỉ gửi email. Giới hạn cứng là `--max=1`.

1. **Billing** → **Budgets & alerts** → **Create budget**.
2. Name `event-mng`; Scope → Projects: chỉ chọn `event-mng-510915`.
3. Amount `5` USD; Thresholds 50%, 90%, 100% → **Finish**.

## 9. Cloud Build trigger (tự deploy khi merge `main`)

**9.1. canhhocit cài GitHub App.** Repo nằm trong tài khoản cá nhân của canhhocit nên chỉ họ làm được:

- Mở <https://github.com/apps/google-cloud-build> → **Configure** → tài khoản `canhhocit` → **Only select repositories** → `be-event-mng-v3` → **Save**.

**9.2. Kết nối GitHub**

1. **Cloud Build** → **Repositories** → tab **2nd gen** → **Create host connection**.
2. Provider **GitHub**, **Region** `asia-southeast1`, **Name** `github` → **Connect**.
3. Console hỏi cấp quyền Secret Manager cho Cloud Build service agent: đồng ý.
4. Đăng nhập GitHub của bạn → **Authorize**. Ở bước chọn installation, chọn bản của `canhhocit`.

**9.3. Link repository**

1. Bấm **Link Repositories**.
2. **Connection** `github`; **Repository** `canhhocit/be-event-mng-v3`; tên để tự sinh.
3. Bấm **Link**.

Không thấy repo trong danh sách:

- Tạm cấp cho canhhocit quyền *Owner* trên project (**IAM & Admin → IAM → Grant access**) để họ tự làm 9.2–9.3, xong thì gỡ.
- Hoặc tiếp tục deploy tay bằng bước 5.

**9.4. Tạo trigger:** **Cloud Build** → **Triggers** → chọn region `asia-southeast1` ở đầu trang → **Create trigger**:

| Trường | Giá trị |
|---|---|
| **Name** | `deploy-event-mng-api` |
| **Region** | `asia-southeast1` |
| **Event** | **Push to a branch** |
| **Source** | **2nd gen** → **Repository** `canhhocit-be-event-mng-v3` → **Branch** `^main$` |
| **Configuration** | **Cloud Build configuration file (yaml or json)** |
| **Location** | **Repository** → `cloudbuild.yaml` |
| Substitution variables, Approval | Để trống, không tick |
| **Service account** | `event-mng-deployer@event-mng-510915.iam.gserviceaccount.com` |

→ **Create**.

**9.5. Chạy:** merge PR `minh → main` → trigger tự chạy → xem **History** (region `asia-southeast1`). Chưa merge thì `main` chưa có `cloudbuild.yaml`, bấm **Run** sẽ lỗi.

Lỗi `Due to quota restrictions...`: tạo lại kết nối kiểu **1st gen** (tab 1st gen → **Connect repository**) và trigger với Region **global**.

## 10. Chuyển frontend + PayOS

1. **Vercel**, với cả `fe` và `FE-manager-v3`:
   - **Settings → Environment Variables** → `VITE_API_BASE_URL` = `https://event-mng-api-431351932095.asia-southeast1.run.app` (không có `/` cuối).
   - Sau đó **Deployments** → bản mới nhất → **⋯ → Redeploy**: Vite chỉ đọc biến lúc build.
2. **PayOS** → kênh thanh toán → Webhook URL: `https://event-mng-api-431351932095.asia-southeast1.run.app/api/v1/payments/payos-webhook` → lưu.
3. Thử trọn luồng: đăng ký → mail xác thực → mua vé → thanh toán → nhận mail + PDF.

## 11. Tắt VPS, xoá Neon

- `.github/workflows/deploy.yml` đã chuyển sang chạy tay: push `main` không còn deploy lên VPS nữa.
- Chạy ổn vài ngày thì làm các việc sau:
  - `docker stop event-mng` trên VPS.
  - Gỡ self-hosted runner (repo → Settings → Actions → Runners; chủ repo làm).
  - Giữ project Neon (hoặc file dump ở bước 2) khoảng 1 tuần rồi xoá.

---

## Vận hành hằng ngày

| Việc | Cách làm |
|---|---|
| Đổi biến cấu hình | Sửa `deploy/cloudrun.env` → `./deploy/gcp.sh env event-mng-510915` (tự tạo revision mới để app đọc lại) |
| Đổi CPU/RAM/scaling/billing | Sửa `cloudbuild.yaml` → merge vào `main` (hoặc `./deploy/gcp.sh deploy event-mng-510915`) |
| Xem log app | Cloud Run → **Logs**, hoặc `gcloud run services logs read event-mng-api --region asia-southeast1 --project event-mng-510915 --limit 200` |
| Xem dữ liệu | Supabase → **Table Editor** / **SQL Editor** |
| Rollback | Cloud Run → **Revision history** → ⋮ ở revision cũ → **Manage traffic** → **Send all traffic to one revision** → **Save** |
| Kiểm tra nhanh | `./deploy/gcp.sh status event-mng-510915` |
| Backup DB | Supabase Free không có backup tải về được: thỉnh thoảng `pg_dump -Fc "<Session pooler URL>" -f backup.dump` |

## Chi phí

| Dịch vụ | Ước tính | Ghi chú |
|---|---|---|
| Cloud Run | ~0 USD khi ít người dùng | Chỉ tính lúc xử lý request + khởi động. Free tier mỗi tháng: 180.000 vCPU-giây, 360.000 GiB-giây, 2 triệu request, tính chung cả billing account (dùng chung với `nest-backend`) |
| Supabase | 0 USD (Free) | 500 MB database; tạm dừng sau 7 ngày không hoạt động (đã có bước 7) |
| Cloud Build | ~0 | 2.500 phút build miễn phí/tháng |
| Cloud Scheduler | 0 | 3 lịch miễn phí mỗi billing account |
| Artifact Registry, Secret Manager | ~0 | Giữ 3 image, nằm trong 0,5 GB miễn phí |

## Lỗi thường gặp

| Triệu chứng (thường thấy ở tab Logs) | Nguyên nhân / cách xử lý |
|---|---|
| Lần đầu vào web rất chậm (20–40 giây) | Bình thường: app đang khởi động sau khi ngủ |
| `FATAL: Tenant or user not found` | `POSTGRES_USER` thiếu `.<project-ref>` (phải là `postgres.abcd...`) |
| `password authentication failed` | Sai `POSTGRES_PASSWORD`, reset ở Project Settings → Database |
| `prepared statement "S_1" already exists` | Đang dùng Transaction pooler 6543: đổi sang Session pooler 5432 |
| Timeout / `Network is unreachable` khi kết nối DB | Đang dùng Direct connection (IPv6): đổi sang Session pooler |
| `max clients reached` / `MaxClientsInSessionMode` | Quá giới hạn kết nối Session pooler: giữ `HIKARI_MAX_POOL_SIZE=5`, đóng tool đang mở kết nối |
| App lỗi DB sau một thời gian dài không dùng | Supabase đã tạm dừng project: Dashboard → **Resume project**, rồi chạy bước 7 |
| `Could not resolve placeholder '...'` | Thiếu key trong secret: `./deploy/gcp.sh check-env` rồi `env` |
| `Config data resource 'file [/secrets/app.properties]' ... does not exist` | Secret chưa có version: chạy `env` |
| `Permission denied on secret` / `iam.serviceaccounts.actAs` | Chạy lại `./deploy/gcp.sh setup event-mng-510915` |
| Build lỗi lúc lấy source từ GitHub | Thiếu role Read Token Accessor: chạy lại `setup` |
| Mail/PDF không gửi sau thanh toán | Kiểm tra `APP_ASYNC_ENABLED=false`, xem log `Brevo` |
| Frontend vẫn gọi URL cũ | Chưa **Redeploy** trên Vercel |
| PayOS không cập nhật đơn | Webhook URL trên PayOS còn trỏ server cũ |
