# Scan Bốn Góc – dự án Android

Ứng dụng quét tài liệu: chụp ảnh, kéo 4 góc hoặc chọn khổ giấy, cân bằng sáng, xuất PDF để lưu và chia sẻ.
Giao diện nằm trong `app/src/main/assets/index.html`, vỏ Android nằm trong `MainActivity.java`.
Ứng dụng không xin quyền Internet và không xin quyền camera, tài liệu không rời khỏi máy.

## Cách 1: build bằng GitHub, không cần cài gì trên máy

1. Tạo một kho (repository) mới trên github.com, chọn Private cũng được.
2. Tải toàn bộ nội dung thư mục này lên kho đó (nút "Add file" > "Upload files", kéo cả thư mục `.github` theo).
3. Vào tab **Actions**, chọn **Build APK**, bấm **Run workflow**. Chờ khoảng 3 đến 5 phút.
4. Mở lượt chạy vừa xong, kéo xuống mục **Artifacts**, tải **ScanBonGoc-apk**. Giải nén được file `app-debug.apk`.

## Cách 2: build bằng Android Studio

1. Cài Android Studio, chọn **Open** và trỏ tới thư mục này. Chờ Gradle đồng bộ xong.
2. Menu **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
3. Bấm **locate** ở thông báo để lấy `app-debug.apk` (thư mục `app/build/outputs/apk/debug`).

## Cài vào điện thoại

1. Chép `app-debug.apk` sang điện thoại (Zalo, Drive, cáp USB đều được).
2. Mở file trong ứng dụng Tệp. Nếu máy hỏi, cho phép "Cài đặt ứng dụng không rõ nguồn" cho ứng dụng đang mở file.
3. Nếu Google Play Protect cảnh báo, chọn "Vẫn cài đặt". Đây là bản tự build, ký bằng khóa debug.

Yêu cầu: Android 10 trở lên.
