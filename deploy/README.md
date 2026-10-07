# Deploy backend lên Cloud Run (Singapore)

```
Vercel (fe, FE-manager-v3) ──HTTPS──▶ Cloud Run asia-southeast1: event-mng-api ──▶ Neon aws-ap-southeast-1
                                              ▲
GitHub main ──trigger──▶ Cloud Build ─────────┘  (build Dockerfile → Artifact Registry → deploy)
```

| File | Vai trò |
|---|---|
| [`cloudbuild.yaml`](../cloudbuild.yaml) | Pipeline build/deploy **và** cấu hình runtime (CPU, RAM, billing, scaling, secret) |
| [`deploy/gcp.sh`](gcp.sh) | `setup` (1 lần), `check-env`, `env` (đẩy cấu hình), `deploy` (deploy tay), `status` |
| [`deploy/cloudrun.env.example`](cloudrun.env.example) | Mẫu file cấu hình bí mật → lưu thành secret `event-mng-env` |
| [`deploy/neon-copy.sh`](neon-copy.sh) | Tuỳ chọn: copy dữ liệu từ Neon Mỹ sang Neon Singapore |

Quy trình nhánh: code trên nhánh riêng → Pull Request vào `main` → trigger tự deploy `main`. Không dùng nhánh `deploy` (bỏ từ 07/2026).

Cần cài: [gcloud CLI](https://cloud.google.com/sdk/docs/install) (đã `gcloud auth login`), `psql`/`pg_dump` nếu copy DB.

---

## Thứ tự thực hiện

1. [Tạo Neon project ở Singapore](#1-neon-tạo-project-ở-singapore)
2. [(Tuỳ chọn) Copy dữ liệu từ Neon Mỹ](#2-tuỳ-chọn-copy-dữ-liệu-từ-neon-mỹ)
3. [Tạo project Google Cloud](#3-tạo-project-google-cloud)
4. [Setup tài nguyên + phân quyền](#4-setup-tài-nguyên--phân-quyền)
5. [Điền cấu hình và đẩy lên Secret Manager](#5-điền-cấu-hình-và-đẩy-lên-secret-manager)
6. [Deploy lần đầu](#6-deploy-lần-đầu)
7. [Kiểm tra trên console Cloud Run](#7-kiểm-tra-trên-console-cloud-run)
8. [Tạo Cloud Build trigger (tự deploy khi push main)](#8-cloud-build-trigger-tự-deploy-khi-push-main)
9. [Chuyển frontend + PayOS sang URL mới](#9-chuyển-frontend--payos-sang-url-mới)
10. [Tắt VPS](#10-tắt-vps)

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

## 3. Tạo project Google Cloud

Dùng project **riêng** cho app này, đừng dùng chung `driverprj-503808` (đang chạy `nest-backend`).

1. [console.cloud.google.com](https://console.cloud.google.com) → ô chọn project trên thanh trên cùng → **New project** → tên `event-mng` → **Create**.
2. Ghi lại **Project ID** (có thể có hậu tố số, ví dụ `event-mng-471203`). Các lệnh dưới đây gọi nó là `<PROJECT_ID>`.
3. **Billing** → liên kết project với billing account. Nếu báo hết quota project trên billing account thì gỡ billing khỏi project cũ không dùng.

## 4. Setup tài nguyên + phân quyền

```bash
cd be
./deploy/gcp.sh setup <PROJECT_ID>
```

Script chạy lại nhiều lần vẫn an toàn và tạo:

| Tài nguyên | Tên | Ghi chú |
|---|---|---|
| API | Cloud Run, Cloud Build, Artifact Registry, Secret Manager, IAM | |
| Artifact Registry | `event-mng` (Docker, `asia-southeast1`) | chứa image |
| Service account chạy app | `event-mng-run` | chỉ được đọc secret `event-mng-env` |
| Service account chạy build | `event-mng-deployer` | Cloud Build Service Account, Cloud Run Admin, Read Token Accessor (GitHub), Service Account User trên `event-mng-run` |
| Secret | `event-mng-env` | chưa có dữ liệu, bước 5 sẽ đẩy |

<details>
<summary>Muốn tự bấm trên console thay vì chạy script</summary>

- **APIs & Services → Library**: bật Cloud Run Admin API, Cloud Build API, Artifact Registry API, Secret Manager API.
- **Artifact Registry → Repositories → Create repository**: Name `event-mng`, Format **Docker**, Location type **Region** → `asia-southeast1`.
- **IAM & Admin → Service Accounts → Create service account**: tạo `event-mng-run` và `event-mng-deployer`.
- **IAM & Admin → IAM → Grant access**: principal `event-mng-deployer@<PROJECT_ID>.iam.gserviceaccount.com`, roles: *Cloud Build Service Account*, *Cloud Run Admin*, *Cloud Build Read Only Token Accessor*, *Developer Connect Read Token Accessor*.
- **Service Accounts → `event-mng-run` → Permissions → Grant access**: principal `event-mng-deployer@...`, role *Service Account User*.
- **Secret Manager → Create secret**: tên `event-mng-env` → sau khi tạo: **Permissions → Grant access** cho `event-mng-run@...` role *Secret Manager Secret Accessor*.

</details>

## 5. Điền cấu hình và đẩy lên Secret Manager

```bash
cp deploy/cloudrun.env.example deploy/cloudrun.env   # file thật, đã nằm trong .gitignore
# mở deploy/cloudrun.env, điền giá trị thật (xem chú thích trong file)
./deploy/gcp.sh check-env                            # báo lỗi nếu thiếu key, còn giá trị mẫu, URL sai định dạng...
./deploy/gcp.sh env <PROJECT_ID>                     # tạo version mới của secret event-mng-env
```

- `JWT_SECRET_KEY`: tạo mới bằng `openssl rand -hex 64`. **Không** dùng giá trị mặc định trong `application.properties` (repo public: ai cũng ký được token ADMIN).
- `FRONTEND_URL`: URL web khách hàng trên Vercel (dùng cho redirect sau thanh toán và link trong email).
- `BACKEND_URL` **không** cần điền: `cloudbuild.yaml` tự đặt bằng URL Cloud Run.

Làm trên console: **Secret Manager → `event-mng-env` → New version** → dán nội dung file → **Add new version**. Sau đó vào Cloud Run → service → **Edit & deploy new revision** → **Deploy** để app đọc bản mới (script `env` tự làm bước này).

## 6. Deploy lần đầu

Deploy từ code trên máy, chưa cần trigger:

```bash
./deploy/gcp.sh deploy <PROJECT_ID>
```

- Mất khoảng 6–10 phút (Maven tải dependency khoảng 4–5 phút, app khởi động khoảng 20–30 giây). Log build: **Cloud Build → History**.
- Xong, script in URL dạng `https://event-mng-api-<PROJECT_NUMBER>.asia-southeast1.run.app` và gọi thử `/api/v1/ping`.
- Mở `<URL>/swagger-ui/index.html` để thử API.
- **Đổi mật khẩu ngay**: seed tạo sẵn `admin` và các tài khoản mẫu với mật khẩu `123456`.

Log khởi động (**Cloud Run → event-mng-api → Logs**):

- DB mới: `Successfully applied 6 migrations to schema "public"`.
- DB copy từ Mỹ: chỉ chạy thêm `V6` nếu DB cũ chưa có.

## 7. Kiểm tra trên console Cloud Run

**Cloud Run → Services → `event-mng-api`**:

| Nơi xem | Phải thấy |
|---|---|
| Đầu trang | URL dạng `https://event-mng-api-<PROJECT_NUMBER>.asia-southeast1.run.app` |
| Tab **Revisions** | Revision mới nhất nhận 100% traffic |
| Tab **Security** / phần Authentication | **Allow public access** |
| **Edit & deploy new revision** (chỉ xem, đừng sửa) → Billing | **Instance-based** |
| ↳ **Service scaling** | Minimum number of instances `1`, Maximum number of instances `1` |
| ↳ Containers → **Variables & Secrets** | `SPRING_CONFIG_ADDITIONAL_LOCATION`, `BACKEND_URL`, `SEED_ENABLED=false` |
| ↳ Containers → **Volume mounts** | secret `event-mng-env` mount tại `/secrets/app.properties` |
| ↳ Security → Service account | `event-mng-run@<PROJECT_ID>.iam.gserviceaccount.com` |

> Đổi CPU/RAM/scaling/billing thì **sửa `cloudbuild.yaml`**, không sửa trên console: lần deploy sau sẽ ghi đè giá trị trên console.

Vì sao **Instance-based** và đúng **1 instance**:

- `EventStatusTask` chạy mỗi phút, có cả `PingTask` và `TokenCleanupService`.
- Mail + PDF hoá đơn được gửi bằng `CompletableFuture` *sau khi* đã trả response cho webhook PayOS.
- Với Request-based, CPU bị bóp ngay khi không có request, nên những việc trên bị treo.
- Cache (`ConcurrentMapCacheManager`) nằm trong RAM của từng instance; nhiều instance sẽ trả dữ liệu cũ lệch nhau.

## 8. Cloud Build trigger (tự deploy khi push main)

### 8.1. Vướng mắc: repo thuộc tài khoản `canhhocit`

Repo `canhhocit/be-event-mng-v3` nằm trong tài khoản GitHub **cá nhân** của canhhocit. Chỉ chủ repo cài được GitHub App lên repo đó; collaborator thì không. Chọn một trong các cách:

- **A (khuyên dùng)**: nhờ canhhocit vào <https://github.com/apps/google-cloud-build> → **Install/Configure** → tài khoản `canhhocit` → **Only select repositories** → `be-event-mng-v3` → **Save**. Sau đó bạn kết nối như mục 8.2.
  - Nếu ở bước *Link repository* bạn không thấy repo: thêm tài khoản Google của canhhocit vào project GCP (IAM → Grant access, role *Owner*, gỡ sau khi xong) để canhhocit tự làm mục 8.2 bằng GitHub của mình.
- **B**: fork repo về tài khoản của bạn, trigger chạy trên fork. Mỗi lần `main` gốc có code mới, bấm **Sync fork** trên GitHub.
- **C**: không dùng trigger, mỗi lần `main` có code mới thì chạy `./deploy/gcp.sh deploy <PROJECT_ID>`.

### 8.2. Kết nối GitHub (Cloud Build repositories, 2nd gen)

1. **Cloud Build → Repositories** → tab **2nd gen** → **Create host connection**.
2. Provider **GitHub**, Region **asia-southeast1**, Name `github` → **Connect** → đăng nhập GitHub, cho phép *Google Cloud Build*.
   - Lần đầu console có thể hỏi cấp quyền Secret Manager cho Cloud Build service agent: đồng ý.
3. Trong connection vừa tạo → **Link repository** → chọn `canhhocit/be-event-mng-v3` → **Link**.

### 8.3. Tạo trigger

**Cloud Build → Triggers** (chọn region `asia-southeast1` ở đầu trang) → **Create trigger**:

| Trường | Giá trị |
|---|---|
| Name | `deploy-event-mng-api` |
| Region | `asia-southeast1` (phải trùng region của connection) |
| Event | **Push to a branch** |
| Source | **2nd gen** → Repository `canhhocit-be-event-mng-v3` → Branch `^main$` |
| Configuration | **Cloud Build configuration file (yaml or json)** |
| Location | **Repository** → `cloudbuild.yaml` |
| Service account | `event-mng-deployer@<PROJECT_ID>.iam.gserviceaccount.com` (bắt buộc chọn với project mới) |

→ **Create**. Thử ngay: trong danh sách trigger bấm **Run** → branch `main`.

Lỗi `Due to quota restrictions, Cloud Build cannot run builds in this region`: project mới đôi khi có quota build theo region bằng 0. Cách xử lý:

- Tạo lại theo kiểu **1st gen**: Repositories → tab **1st gen** → **Connect repository**, rồi tạo trigger với Region **global**.
- Hoặc xin tăng quota: **IAM & Admin → Quotas**, lọc theo *Cloud Build API*.

## 9. Chuyển frontend + PayOS sang URL mới

1. **Vercel**: với **cả hai** project (`fe` và `FE-manager-v3`) → **Settings → Environment Variables** → `VITE_API_BASE_URL` = URL Cloud Run (không có `/` ở cuối) → **Save**. Sau đó **Deployments** → bản mới nhất → **⋯ → Redeploy**: Vite chỉ đọc biến môi trường lúc build.
2. **PayOS** ([my.payos.vn](https://my.payos.vn)) → kênh thanh toán → Webhook URL: `<URL>/api/v1/payments/payos-webhook` → lưu (PayOS sẽ gọi thử URL này).
3. Thử trọn luồng: đăng ký (mail xác thực có link về URL mới) → mua vé → thanh toán → nhận mail + PDF.

## 10. Tắt VPS

- `.github/workflows/deploy.yml` đã chuyển sang chạy tay (`workflow_dispatch`): push `main` không còn deploy lên VPS nữa.
- Chạy ổn vài ngày thì làm các việc sau:
  - `docker stop event-mng` trên VPS.
  - Gỡ self-hosted runner (repo → Settings → Actions → Runners; chủ repo làm).
  - Giữ Neon Mỹ (hoặc file dump) khoảng 1 tuần làm backup rồi xoá.

---

## Vận hành hằng ngày

| Việc | Cách làm |
|---|---|
| Đổi biến cấu hình | Sửa `deploy/cloudrun.env` → `./deploy/gcp.sh env <PROJECT_ID>` (tự tạo revision mới) |
| Đổi CPU/RAM/scaling | Sửa `cloudbuild.yaml` → merge vào `main` (hoặc `./deploy/gcp.sh deploy`) |
| Xem log | Cloud Run → service → **Logs**, hoặc `gcloud run services logs read event-mng-api --region asia-southeast1 --project <PROJECT_ID> --limit 200` |
| Rollback | Cloud Run → **Revisions** → chọn revision cũ → **Manage traffic** → 100% → Save. Lần merge `main` tiếp theo sẽ chuyển traffic sang bản mới |
| Kiểm tra nhanh | `./deploy/gcp.sh status <PROJECT_ID>` |
| Gắn domain riêng | Đặt substitution `_BACKEND_URL=https://api.ten-mien.com` trong trigger |

## Chi phí ước tính (giữ nguyên code hiện tại, chạy 24/7)

| Dịch vụ | Ước tính/tháng | Ghi chú |
|---|---|---|
| Cloud Run 1 vCPU + 1 GiB, Instance-based | **~55–60 USD** | Singapore là vùng giá *Tier 2*; dưới 1 vCPU thì bắt buộc Request-based |
| Neon | **~20 USD** (gói Launch, 0,25 CU 24/7) | Gói Free chỉ có 100 CU-giờ/tháng; xem chú ý bên dưới |
| Cloud Build | ~0 | 2.500 phút build miễn phí/tháng |
| Artifact Registry, Secret Manager | < 1 USD | Thỉnh thoảng xoá image cũ |

> **Chú ý Neon Free**: app truy vấn DB mỗi phút (`EventStatusTask`, cộng với kết nối Hikari giữ sẵn), nên compute Neon không bao giờ được ngủ.
> 0,25 CU × 730 giờ ≈ 182 CU-giờ, vượt mức 100 của gói Free. Khoảng ngày 16–17 hằng tháng DB sẽ bị **tạm dừng tới tháng sau**.
> Cách xử lý: nâng gói Launch, hoặc sửa code để DB được ngủ.

Muốn rẻ hơn nhiều thì phải sửa code:

1. Chuyển các `@Scheduled` sang Cloud Scheduler gọi endpoint.
2. Gửi mail/PDF ngay trong request (hoặc dùng Cloud Tasks).
3. Bỏ `PingTask`.

Làm xong thì dùng được **Request-based** (min 0–1 instance) và Neon được ngủ, tổng chi phí còn vài USD/tháng.

## Lỗi thường gặp

| Triệu chứng | Nguyên nhân / cách xử lý |
|---|---|
| `Found more than one migration with version 4` | Code chưa có commit đổi `V4__seed_diverse_events.sql` thành `V6__...` |
| Revision không lên, log `Could not resolve placeholder 'CLOUDINARY_...'` | Thiếu key trong secret: chạy `./deploy/gcp.sh check-env` rồi `env` |
| `Config data resource 'file [/secrets/app.properties]' ... does not exist` | Secret chưa mount/chưa có version: chạy `env` |
| Deploy báo lỗi chờ revision mới / không chuyển traffic | Tạm đổi `--max=1` thành `--max=2` trong `cloudbuild.yaml`, deploy xong đổi lại |
| `Permission denied on secret` | `event-mng-run` thiếu quyền: chạy lại `./deploy/gcp.sh setup` |
| Build lỗi `iam.serviceaccounts.actAs` / `PERMISSION_DENIED` khi deploy | `event-mng-deployer` thiếu quyền: chạy lại `setup` |
| Build lỗi lúc lấy source từ GitHub | Thiếu role *Read Token Accessor*: chạy lại `setup` |
| `FATAL: password authentication failed` / timeout DB | Sai `POSTGRES_PASSWORD`, hoặc `DATABASE_URL` có `user:pass@` (phải tách riêng) |
| Frontend vẫn gọi URL cũ | Chưa **Redeploy** trên Vercel sau khi đổi `VITE_API_BASE_URL` |
| PayOS không cập nhật đơn | Webhook URL trên PayOS còn trỏ server cũ |
