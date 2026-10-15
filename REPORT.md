# BÁO CÁO KỸ THUẬT BÀI 3: TÁCH BIỆT LUỒNG CI/CD ĐA MÔI TRƯỜNG (STAGING & PRODUCTION) VỚI GITHUB ENVIRONMENTS

- **Khóa học:** DevOps Chuyên Sâu
- **Mã bài tập:** SS12 - Exercise 3
- **Công nghệ áp dụng:** GitHub Actions, GitHub Environments, Environment Protection Rules, Required Reviewers, Deployment Approval Gate

---

## 1. Mục tiêu và Ý nghĩa Kỹ thuật

### 1.1. Mục tiêu
- Thiết lập quy trình phân tách rõ ràng luồng phân phối phần mềm giữa môi trường kiểm thử tích hợp (**Staging**) và môi trường phục vụ người dùng cuối (**Production**).
- Triển khai chốt chặn an toàn (**Gatekeeper / Manual Approval**) trên môi trường Production bằng tính năng **Environment Protection Rules (Required Reviewers)**.
- Gắn liên kết động trạng thái triển khai (**Deployment URL**) trực tiếp lên giao diện theo dõi của GitHub.

### 1.2. Tại sao cần tách biệt môi trường và phê duyệt thủ công (Manual Gate)?
Trong các dự án thực tế:
1. **Kiểm thử liên tục trên Staging:** Mã nguồn trên nhánh `develop` cần được deploy tự động ngay lập tức lên Staging để QA/QC tiến hành test tính năng, kiểm thử tải và tích hợp hệ thống.
2. **Bảo vệ môi trường Production:** Production là môi trường nhạy cảm, trực tiếp ảnh hưởng đến khách hàng và doanh thu. Việc deploy lên Production phải có sự đồng ý của Tech Lead, DevOps Engineer hoặc Product Owner để kiểm tra release notes, thời điểm triển khai (off-peak hours) và chuẩn bị kế hoạch rollback khi cần.
3. **Tuân thủ quy chuẩn bảo mật (Compliance):** Các tiêu chuẩn như ISO 27001, SOC 2 yêu cầu tính năng phân quyền xét duyệt độc lập trước khi thay đổi hệ thống Production.

---

## 2. Các Bước Cấu hình GitHub Environments (Settings)

### 2.1. Thiết lập Môi trường `staging`
- Điều hướng: **Settings** -> **Environments** -> **New environment** -> đặt tên `staging`.
- Không bật Reviewers, cho phép triển khai tự động (Continuous Deployment) từ nhánh `develop`.
- Có thể thêm Environment URL: `https://api.staging.example.com`.

### 2.2. Thiết lập Môi trường `production`
- Điều hướng: **Settings** -> **Environments** -> **New environment** -> đặt tên `production`.
- Tại phần **Environment protection rules**:
  - Bật tùy chọn **Required reviewers**.
  - Tìm và thêm tài khoản của bạn (hoặc quản trị viên) vào danh sách Reviewers.
  - Tùy chọn: Đặt **Wait timer** (nếu muốn trì hoãn triển khai sau khi duyệt).
  - Environment URL: `https://api.prod.example.com`.

---

## 3. Thiết kế Kịch bản CI/CD (`.github/workflows/cd-multi-env.yml`)

```yaml
name: Multi-Environment CD Pipeline (Staging & Production)

on:
  push:
    branches:
      - main
      - develop
  workflow_dispatch:

jobs:
  build-and-test:
    name: Build & Verify Codebase
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repository
        uses: actions/checkout@v5

      - name: Setup Java JDK 17
        uses: actions/setup-java@v5
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'gradle'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Run Build and Test
        run: ./gradlew build

  deploy-staging:
    name: Deploy to Staging
    needs: build-and-test
    if: github.ref == 'refs/heads/develop'
    runs-on: ubuntu-latest
    environment:
      name: staging
      url: https://api.staging.example.com
    steps:
      - name: Checkout repository
        uses: actions/checkout@v5

      - name: Deploy to Staging Cluster
        run: |
          echo "=========================================="
          echo "Deploying application to STAGING environment..."
          echo "Target URL: https://api.staging.example.com"
          echo "Branch: ${{ github.ref_name }}"
          echo "Commit: ${{ github.sha }}"
          echo "Status: Automatic continuous deployment completed successfully!"
          echo "=========================================="

  deploy-prod:
    name: Deploy to Production
    needs: build-and-test
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    environment:
      name: production
      url: https://api.prod.example.com
    steps:
      - name: Checkout repository
        uses: actions/checkout@v5

      - name: Deploy to Production Cluster
        run: |
          echo "=========================================="
          echo "DEPLOYING APPLICATION TO PRODUCTION ENVIRONMENT..."
          echo "Target URL: https://api.prod.example.com"
          echo "Branch: ${{ github.ref_name }}"
          echo "Commit: ${{ github.sha }}"
          echo "Status: APPROVED AND DEPLOYED SAFELY TO PRODUCTION!"
          echo "=========================================="
```

---

## 4. Quá trình Phê duyệt và Nhật ký Thực thi

### 4.1. Trạng thái Chờ phê duyệt (Waiting for Review)
Khi push commit lên nhánh `main`, job `build-and-test` chạy thành công. Khi đến job `deploy-prod`, GitHub Actions phát hiện môi trường `production` có cấu hình **Required reviewers**.
Pipeline tự động tạm dừng với thông báo:
```text
deploy-prod: Waiting for review
This job is waiting on reviews from required reviewers: @<username>
[Review deployments]
```

### 4.2. Thao tác Phê duyệt
1. Nhấp vào nút **Review deployments**.
2. Một hộp thoại popup hiện ra:
   - Checkbox: `production`
   - Comment: `Approved for production release v1.0.0. All regression tests passed.`
3. Nhấp nút **Approve and deploy**.

### 4.3. Nhật ký sau khi Phê duyệt thành công
```text
Approved by @<username> at 2026-10-15T13:45:00Z
Run echo "=========================================="
==========================================
DEPLOYING APPLICATION TO PRODUCTION ENVIRONMENT...
Target URL: https://api.prod.example.com
Branch: main
Commit: 8d721a9c...
Status: APPROVED AND DEPLOYED SAFELY TO PRODUCTION!
==========================================
```
- Trên tab Summary của workflow xuất hiện mục **Deployments**:
  - `production` - Deployed to `https://api.prod.example.com` (Active)

---

## 5. Hướng dẫn Minh chứng Hình ảnh Nộp bài

Theo hướng dẫn nộp bài của đề:
- **Sản phẩm nộp bài:** Tệp kịch bản `.github/workflows/cd-multi-env.yml` và ảnh chụp màn hình cửa sổ phê duyệt deployment môi trường Production.
- **Vị trí chụp ảnh:**
  1. Chụp màn hình thông báo **Waiting for review** màu vàng với nút **Review deployments**.
  2. Chụp modal popup hiển thị checkbox môi trường `production` và nút **Approve and deploy**.
  3. Chụp đồ thị workflow với các job đã tích xanh hoàn tất.
