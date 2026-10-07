# Deploy backend lên Cloud Run (Singapore)

```
Vercel (fe, FE-manager-v3) ──HTTPS──▶ Cloud Run asia-southeast1: event-mng-api ──▶ Neon aws-ap-southeast-1
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

| File | Vai trò |
|---|---|
| [`cloudbuild.yaml`](../cloudbuild.yaml) | Pipeline build/deploy **và** cấu hình runtime (CPU, RAM, billing, scaling, secret) |
| [`deploy/gcp.sh`](gcp.sh) | `setup` (1 lần), `check-env`, `env` (đẩy cấu hình), `deploy` (deploy tay), `status` |
| [`deploy/cloudrun.env.example`](cloudrun.env.example) | Mẫu file cấu hình bí mật |
| [`deploy/neon-copy.sh`](neon-copy.sh) | Tuỳ chọn: copy dữ liệu từ Neon Mỹ sang Neon Singapore |

Quy trình nhánh: code trên nhánh riêng → Pull Request vào `main` → trigger tự deploy `main`. Không dùng nhánh `deploy` (bỏ từ 07/2026).

Cần cài: [gcloud CLI](https://cloud.google.com/sdk/docs/install) (đã `gcloud auth login`), `psql`/`pg_dump` nếu copy DB.

---

## Chế độ chạy: tiết kiệm (scale về 0)

`cloudbuild.yaml` đang đặt:

- **Request-based** billing (`--cpu-throttling`): chỉ tính tiền khi đang xử lý request.
- **min 0 / max 1** instance: không có request thì instance tắt; tối đa 1 instance để giới hạn chi phí và giữ cache Spring nhất quán.

Đánh đổi: **request đầu tiên sau khi ngủ phải chờ app khởi động khoảng 20–40 giây.** Các request sau đó chạy bình thường. Instance còn thức thêm một lúc (thường vài phút đến khoảng 15 phút) sau request cuối rồi mới tắt.

Ngoài request, CPU bị bóp gần như về 0, nên code đã được chỉnh để không phụ thuộc vào việc chạy nền:

| Biến/cơ chế | Tác dụng |
|---|---|
| `APP_ASYNC_ENABLED=false` | Hoàn tất thanh toán, tạo PDF, gửi mail chạy ngay trong request (`BackgroundTaskRunner`); mail gửi sau khi DB commit |
| `APP_PING_ENABLED=false` | Tắt `PingTask` (tự gọi chính mình mỗi 5 phút, làm server và DB không bao giờ ngủ) |
| `EventStatusRefreshInterceptor` | Nếu `EventStatusTask` đã quá 1 phút chưa chạy, request sẽ cập nhật trạng thái sự kiện trước khi xử lý, vì giỏ hàng/checkout dựa vào status `OPENING` |

Chạy local hoặc VPS không đặt các biến trên, nên app vẫn chạy nền như cũ.

**Muốn hết thời gian chờ khởi động** (tốn khoảng 55–60 USD/tháng): trong `cloudbuild.yaml` đổi `--cpu-throttling` → `--no-cpu-throttling`, `--min=0` → `--min=1`, `APP_ASYNC_ENABLED=false` → `true`.

---

## Thứ tự thực hiện

| # | Bước | Trạng thái |
|---|---|---|
| 1 | [Tạo Neon project ở Singapore](#1-neon-tạo-project-ở-singapore) | |
| 2 | [(Tuỳ chọn) Copy dữ liệu từ Neon Mỹ](#2-tuỳ-chọn-copy-dữ-liệu-từ-neon-mỹ) | |
| 3 | Tạo project Google Cloud | ✅ `event-mng-510915` |
| 4 | [Setup tài nguyên + phân quyền](#4-setup-tài-nguyên--phân-quyền) | ✅ đã chạy 07/10/2026 |
| 5 | [Điền cấu hình và đẩy lên Secret Manager](#5-điền-cấu-hình-và-đẩy-lên-secret-manager) | |
| 6 | [Deploy lần đầu](#6-deploy-lần-đầu) | |
| 7 | [Kiểm tra trên console Cloud Run](#7-kiểm-tra-trên-console-cloud-run) | |
| 8 | [Đặt cảnh báo chi phí](#8-đặt-cảnh-báo-chi-phí) | |
| 9 | [Cloud Build trigger (tự deploy khi push main)](#9-cloud-build-trigger-tự-deploy-khi-push-main) | |
| 10 | [Chuyển frontend + PayOS sang URL mới](#10-chuyển-frontend--payos-sang-url-mới) | |
| 11 | [Tắt VPS](#11-tắt-vps) | |

---

## 1. Neon: tạo project ở Singapore

Neon **không đổi được region** của project cũ, phải tạo project mới.

1. [console.neon.tech](https://console.neon.tech) → **New Project**.
2. Region: **AWS Asia Pacific (Singapore)** (`aws-ap-southeast-1`). Postgres version: **giống project cũ** (xem ở project cũ → Settings).
3. Vào project mới → **Connect**: chọn database `neondb`, role `neondb_owner`.
   - Bật **Connection pooling** (host có `-pooler`) → dùng cho app (`DATABASE_URL`).
   - Tắt Connection pooling → dùng cho `neon-copy.sh` (bước 2).
4. Neon đưa chuỗi dạng `postgresql://neondb_owner:MATKHAU@HOST/neondb?sslmode=require...`. App cần tách ra:

   ```properties
   DATABASE_URL=jdbc:postgresql://HOST/neondb?sslmode=require
   POSTGRES_USER=neondb_owner
   POSTGRES_PASSWORD=MATKHAU
   ```

> **Không copy dữ liệu?** Không cần làm gì thêm: lần đầu app khởi động, Flyway tự chạy `V1`→`V6`, tạo bảng và seed dữ liệu mẫu.

## 2. (Tuỳ chọn) Copy dữ liệu từ Neon Mỹ

Chỉ cần khi DB cũ có dữ liệu thật (user tự đăng ký, đơn hàng, vé đã thanh toán...). **Phải làm trước bước 6**, vì sau khi app chạy thì DB mới đã có bảng do Flyway tạo.

```bash
export SOURCE_DATABASE_URL='postgresql://...@ep-...us-east-2.aws.neon.tech/neondb?sslmode=require'      # DB Mỹ, KHÔNG pooler
export TARGET_DATABASE_URL='postgresql://...@ep-...ap-southeast-1.aws.neon.tech/neondb?sslmode=require' # DB Sing, KHÔNG pooler

./deploy/neon-copy.sh check   # cột tao_sau_seed > 0 ở orders/tickets/users = có dữ liệu thật
./deploy/neon-copy.sh copy    # nên tắt backend cũ trước (docker stop event-mng trên VPS)
```

Script chỉ đọc DB nguồn, từ chối chạy nếu DB đích đã có bảng, restore trong một transaction, rồi so số dòng từng bảng. File dump giữ ở `~/event-mng-db-backup/`.

## 4. Setup tài nguyên + phân quyền

Đã chạy cho `event-mng-510915`. Chạy lại bao nhiêu lần cũng được (ví dụ khi nghi thiếu quyền):

```bash
cd be
./deploy/gcp.sh setup event-mng-510915
```

| Tài nguyên | Tên | Ghi chú |
|---|---|---|
| API | Cloud Run, Cloud Build, Artifact Registry, Secret Manager, IAM | |
| Artifact Registry | `event-mng` (Docker, `asia-southeast1`) | cleanup policy: giữ 3 image mới nhất, xoá image cũ hơn 7 ngày |
| Service account chạy app | `event-mng-run` | Secret Manager Secret Accessor trên `event-mng-env` |
| Service account chạy build | `event-mng-deployer` | Cloud Build Service Account, Cloud Run Admin, Cloud Build Read Only Token Accessor, Developer Connect Read Token Accessor, Service Account User trên `event-mng-run` |
| Secret | `event-mng-env` | bước 5 đẩy dữ liệu |

Xem trên console: **IAM & Admin → IAM** (roles của `event-mng-deployer`), **Secret Manager → event-mng-env → Permissions**, **Artifact Registry → Repositories → event-mng**.

## 5. Điền cấu hình và đẩy lên Secret Manager

```bash
cp deploy/cloudrun.env.example deploy/cloudrun.env   # file thật, đã nằm trong .gitignore
# mở deploy/cloudrun.env, điền giá trị thật (xem chú thích trong file)
./deploy/gcp.sh check-env                            # báo lỗi nếu thiếu key, còn giá trị mẫu, URL sai định dạng...
./deploy/gcp.sh env event-mng-510915                 # tạo version mới của secret event-mng-env
```

- `JWT_SECRET_KEY`: tạo mới bằng `openssl rand -hex 64`. **Không** dùng giá trị mặc định trong `application.properties` (repo public: ai cũng ký được token ADMIN).
- PayOS / Brevo: dùng **key mới** (key cũ đang nằm công khai trong `application.properties`).
- `FRONTEND_URL`: URL web khách hàng trên Vercel (dùng cho redirect sau thanh toán và link trong email).
- `BACKEND_URL` **không** cần điền: `cloudbuild.yaml` tự đặt bằng URL Cloud Run.

Làm trên console thay cho lệnh `env`:

1. **Secret Manager** → dòng `event-mng-env` → menu **Actions** (⋮) → **Add new version** → dán nội dung file vào **Secret value** → **Add new version**. Ghi lại số version mới.
2. Nếu service đã chạy, app phải khởi động lại mới đọc bản mới: **Cloud Run → event-mng-api** → tab **Containers** → **Variables & Secrets** → sửa (hoặc thêm) biến `ENV_SECRET_VERSION` = số version mới → **Done** → **View diff & redeploy** → **Deploy changes**.

## 6. Deploy lần đầu

Deploy từ code trên máy (gồm cả thay đổi chưa commit), chưa cần trigger:

```bash
./deploy/gcp.sh deploy event-mng-510915
```

- Mất khoảng 6–10 phút (Maven tải dependency khoảng 4–5 phút). Log build: **Cloud Build → History**.
- Xong, script in URL và gọi thử `/api/v1/ping`. Lần gọi đầu có thể mất khoảng 20–40 giây vì app vừa khởi động.
- Mở `https://event-mng-api-431351932095.asia-southeast1.run.app/swagger-ui/index.html` để thử API.
- **Đổi mật khẩu ngay**: seed tạo sẵn `admin` và các tài khoản mẫu với mật khẩu `123456`.

Log khởi động (**Cloud Run → event-mng-api → Logs**):

- DB mới: `Successfully applied 6 migrations to schema "public"`.
- DB copy từ Mỹ: `Schema "public" is up to date` (hoặc chỉ chạy thêm `V6` nếu DB cũ chưa có).

## 7. Kiểm tra trên console Cloud Run

**Cloud Run → Services → `event-mng-api`**:

| Nơi xem | Phải thấy |
|---|---|
| Đầu trang | URL `https://event-mng-api-431351932095.asia-southeast1.run.app` |
| Tab **Containers** | Image `asia-southeast1-docker.pkg.dev/event-mng-510915/event-mng/event-mng-api`, port `8080`, **CPU limit** `1`, Memory `1 GiB`, **Startup CPU boost** ✓ |
| ↳ **Variables & Secrets** | `SPRING_CONFIG_ADDITIONAL_LOCATION`, `BACKEND_URL`, `SEED_ENABLED=false`, `APP_ASYNC_ENABLED=false`, `APP_PING_ENABLED=false` |
| ↳ Volume (secret) | mount path `/secrets`, file `app.properties` ← secret `event-mng-env`, version `latest` |
| Tab **Scaling** | Billing **Request-based**; Service scaling: min `0`, max `1` |
| Tab **Security** | Authentication **Allow public access**; Service account `event-mng-run@event-mng-510915.iam.gserviceaccount.com` |
| Tab **Revision history** | Revision mới nhất nhận 100% traffic |
| Tab **Logs** | `Successfully applied ... migrations` / `Started EventMngApplication` |

> Đổi CPU/RAM/scaling/billing thì **sửa `cloudbuild.yaml`**, không sửa trên console: lần deploy sau sẽ ghi đè giá trị trên console. Trên console, mọi chỉnh sửa đều kết thúc bằng **View diff & redeploy** → **Deploy changes**.

## 8. Đặt cảnh báo chi phí

Budget **không chặn** chi tiêu, chỉ gửi email cảnh báo. Giới hạn cứng là `--max=1` (tối đa 1 instance).

1. Console → **Billing** → **Budgets & alerts** → **Create budget**.
2. Name `event-mng`. Scope → Projects: chỉ chọn `event-mng-510915`.
3. Amount: **Specified amount** `5` USD (hoặc mức bạn chấp nhận).
4. Thresholds: 50%, 90%, 100% (Actual). Bật gửi email cho Billing admins → **Finish**.

## 9. Cloud Build trigger (tự deploy khi push main)

### 9.1. Cài GitHub App (việc của canhhocit)

Repo `canhhocit/be-event-mng-v3` nằm trong tài khoản GitHub **cá nhân** của canhhocit. Bạn là collaborator nên push được code, nhưng **chỉ chủ tài khoản cài được GitHub App** lên repo đó. Nhờ canhhocit:

1. Mở <https://github.com/apps/google-cloud-build> → **Configure** (hoặc **Install**).
2. Chọn tài khoản `canhhocit` → **Only select repositories** → chọn `be-event-mng-v3` → **Save** / **Install**.

### 9.2. Kết nối GitHub (Cloud Build repositories, 2nd gen)

1. **Cloud Build → Repositories** → tab **2nd gen** → **Create host connection**.
2. Provider **GitHub**, Region **asia-southeast1**, Name `github` → **Connect** → đăng nhập GitHub của bạn, cho phép *Google Cloud Build*.
   - Console hỏi cấp quyền Secret Manager cho Cloud Build service agent: đồng ý.
   - Ở bước chọn nơi cài app / installation: chọn installation của `canhhocit` (đã cài ở 9.1).
3. Trong connection vừa tạo → **Link repository** → chọn `canhhocit/be-event-mng-v3` → **Link**.

Nếu không thấy repo ở bước 3:

- Thêm tài khoản Google của canhhocit vào project (**IAM & Admin → IAM → Grant access**, role *Owner*), để canhhocit tự làm 9.2 bằng GitHub của mình. Xong thì gỡ quyền.
- Hoặc tạm thời deploy tay sau mỗi lần merge: `./deploy/gcp.sh deploy event-mng-510915`.

### 9.3. Tạo trigger

**Cloud Build → Triggers** (chọn region `asia-southeast1` ở đầu trang) → **Create trigger**:

| Trường | Giá trị |
|---|---|
| Name | `deploy-event-mng-api` |
| Region | `asia-southeast1` (phải trùng region của connection) |
| Event | **Push to a branch** |
| Source | **2nd gen** → Repository `canhhocit-be-event-mng-v3` → Branch `^main$` |
| Configuration | **Cloud Build configuration file (yaml or json)** |
| Location | **Repository** → `cloudbuild.yaml` |
| Service account | `event-mng-deployer@event-mng-510915.iam.gserviceaccount.com` (bắt buộc chọn) |

→ **Create**. Thử ngay: trong danh sách trigger bấm **Run** → branch `main`. Lưu ý: `main` phải đã có các commit deploy này (merge PR từ nhánh `minh`).

Lỗi `Due to quota restrictions, Cloud Build cannot run builds in this region`: project mới đôi khi có quota build theo region bằng 0. Cách xử lý:

- Tạo lại theo kiểu **1st gen**: Repositories → tab **1st gen** → **Connect repository**, rồi tạo trigger với Region **global**.
- Hoặc xin tăng quota: **IAM & Admin → Quotas**, lọc theo *Cloud Build API*.

## 10. Chuyển frontend + PayOS sang URL mới

1. **Vercel**: với **cả hai** project (`fe` và `FE-manager-v3`) → **Settings → Environment Variables** → `VITE_API_BASE_URL` = `https://event-mng-api-431351932095.asia-southeast1.run.app` (không có `/` ở cuối) → **Save**. Sau đó **Deployments** → bản mới nhất → **⋯ → Redeploy**: Vite chỉ đọc biến môi trường lúc build.
2. **PayOS** ([my.payos.vn](https://my.payos.vn)) → kênh thanh toán → Webhook URL: `https://event-mng-api-431351932095.asia-southeast1.run.app/api/v1/payments/payos-webhook` → lưu (PayOS sẽ gọi thử URL này).
3. Thử trọn luồng: đăng ký (mail xác thực có link về URL mới) → mua vé → thanh toán → nhận mail + PDF.

## 11. Tắt VPS

- `.github/workflows/deploy.yml` đã chuyển sang chạy tay (`workflow_dispatch`): push `main` không còn deploy lên VPS nữa.
- Chạy ổn vài ngày thì làm các việc sau:
  - `docker stop event-mng` trên VPS.
  - Gỡ self-hosted runner (repo → Settings → Actions → Runners; chủ repo làm).
  - Giữ Neon Mỹ (hoặc file dump) khoảng 1 tuần làm backup rồi xoá.

---

## Vận hành hằng ngày

| Việc | Cách làm |
|---|---|
| Đổi biến cấu hình | Sửa `deploy/cloudrun.env` → `./deploy/gcp.sh env event-mng-510915` (tự tạo revision mới) |
| Đổi CPU/RAM/scaling/billing | Sửa `cloudbuild.yaml` → merge vào `main` (hoặc `./deploy/gcp.sh deploy event-mng-510915`) |
| Xem log | Cloud Run → service → **Logs**, hoặc `gcloud run services logs read event-mng-api --region asia-southeast1 --project event-mng-510915 --limit 200` |
| Rollback | Cloud Run → tab **Revision history** → ⋮ ở revision cũ → **Manage traffic** → **Send all traffic to one revision** → **Save**. Chỉ còn 3 image gần nhất |
| Kiểm tra nhanh | `./deploy/gcp.sh status event-mng-510915` |
| Gắn domain riêng | Đặt substitution `_BACKEND_URL=https://api.ten-mien.com` trong trigger |

## Chi phí (chế độ tiết kiệm)

| Dịch vụ | Ước tính | Ghi chú |
|---|---|---|
| Cloud Run | ~0 USD khi ít người dùng | Chỉ tính lúc xử lý request và lúc khởi động. Free tier mỗi tháng: 180.000 vCPU-giây, 360.000 GiB-giây, 2 triệu request, **tính chung cả billing account** (dùng chung với `nest-backend` ở `driverprj-503808`) |
| Neon | Gói Free đủ dùng | DB ngủ sau 5 phút không có truy vấn, khi Cloud Run đã tắt. Gói Free có 100 CU-giờ/tháng |
| Cloud Build | ~0 | 2.500 phút build miễn phí/tháng (chung billing account) |
| Artifact Registry | ~0 | Giữ 3 image, nằm trong 0,5 GB miễn phí |
| Secret Manager | ~0 | |

Theo dõi: **Billing → Reports**, lọc project `event-mng-510915`.

## Lỗi thường gặp

| Triệu chứng | Nguyên nhân / cách xử lý |
|---|---|
| Lần đầu vào web rất chậm (20–40 giây) | Bình thường ở chế độ scale về 0: app đang khởi động |
| `Found more than one migration with version 4` | Code chưa có commit đổi `V4__seed_diverse_events.sql` thành `V6__...` |
| Revision không lên, log `Could not resolve placeholder 'CLOUDINARY_...'` | Thiếu key trong secret: chạy `./deploy/gcp.sh check-env` rồi `env` |
| `Config data resource 'file [/secrets/app.properties]' ... does not exist` | Secret chưa mount/chưa có version: chạy `env` |
| `Permission denied on secret` | `event-mng-run` thiếu quyền: chạy lại `./deploy/gcp.sh setup event-mng-510915` |
| Build lỗi `iam.serviceaccounts.actAs` / `PERMISSION_DENIED` khi deploy | `event-mng-deployer` thiếu quyền: chạy lại `setup` |
| Build lỗi lúc lấy source từ GitHub | Thiếu role *Read Token Accessor*: chạy lại `setup` |
| `FATAL: password authentication failed` / timeout DB | Sai `POSTGRES_PASSWORD`, hoặc `DATABASE_URL` có `user:pass@` (phải tách riêng) |
| Mail/PDF không gửi sau thanh toán | Kiểm tra `APP_ASYNC_ENABLED=false` trong Variables & Secrets, xem log `Brevo` |
| Frontend vẫn gọi URL cũ | Chưa **Redeploy** trên Vercel sau khi đổi `VITE_API_BASE_URL` |
| PayOS không cập nhật đơn | Webhook URL trên PayOS còn trỏ server cũ |
