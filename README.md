# ProCam – Pro Camera for Android

Ứng dụng máy ảnh chuyên nghiệp phong cách DSLR hàng đầu cho Android, kế thừa và tối ưu hóa các tính năng tinh túy từ **ProCam (iOS)**.

---

## 📸 Tính năng cốt lõi (Features)

### 1. Điều khiển thủ công hoàn toàn (Full Manual Controls)
- **Tốc độ màn trập (Shutter Speed):** Tùy biến từ $1/8000\text{s}$ đến $30\text{s}$ bằng tay hoặc AUTO.
- **Độ nhạy sáng (ISO):** Tinh chỉnh tự do từ ISO 50 đến 6400 (tùy thuộc cảm biến máy).
- **Lấy nét thủ công (Manual Focus):** Vòng lấy nét mượt mà từ Macro cận cảnh đến Vô cực ($\infty$).
- **Cân bằng trắng (White Balance):** Điều chỉnh nhiệt độ màu Kelvin từ $2000\text{K}$ đến $10000\text{K}$ và R/G/B Color Correction Gains.
- **Bù trừ sáng (Exposure Compensation - EV):** Thước đo từ $-3.0\text{EV}$ đến $+3.0\text{EV}$ theo từng nấc $1/3\text{EV}$.

### 2. Công cụ hỗ trợ nét & sáng chuyên nghiệp (Professional Assist Tools)
- **Focus Peaking thời gian thực (60 FPS):** Sử dụng bộ lọc tích chập Sobel (Sobel Kernel Convolution) trực tiếp trên GPU shader OpenGL ES, tô viền màu tương phản rực rỡ (Xanh neon / Đỏ / Cyan) lên chủ thể đang nét thực tế.
- **Dải sọc vằn (Zebra Stripes):** Thuật toán shader phát hiện các vùng sáng vượt ngưỡng $95\%$ độ sáng (Overexposure Clipping) và phủ các dải sọc chéo $45^\circ$ chuyển động để cảnh báo người chụp.
- **Biểu đồ đo sáng thời gian thực (Live Luminance Histogram):** 256 bậc độ sáng hiển thị dưới dạng đồ thị sóng mini ở góc màn hình.
- **Thước cân bằng điện tử (Artificial Horizon / Tiltmeter):** Đo góc nghiêng theo thời gian thực bằng cảm biến con quay hồi chuyển, tự động chuyển sang màu xanh lá khi máy nằm ngang hoàn hảo ($\pm 0.75^\circ$).
- **Lưới căn tỉ lệ (Composition Grids):** Tích hợp quy tắc $1/3$ (Rule of Thirds), tỉ lệ vàng (Golden Ratio) và hồng tâm (Crosshair).

### 3. Chế độ chụp chuyên dụng (Shooting Modes)
- **Định dạng RAW & ProRAW:** Chụp ảnh định dạng `RAW_SENSOR` (.dng) nguyên bản bằng `DngCreator`, lưu toàn bộ dải tương phản động (Dynamic Range) và EXIF metadata để hậu kỳ chuyên sâu trong Adobe Lightroom / Photoshop.
- **Chụp chống rung thông minh (Anti-Shake):** Phân tích dao động gia tốc và góc quay từ con quay hồi chuyển, chỉ tự động kích hoạt chụp khi điện thoại đứng yên tuyệt đối.
- **Phơi sáng ảo thông minh (Slow Shutter / Frame Stacking):**
  - *Light Trails (Vệt đèn xe / Sao chạy):* Ghép nhiều khung hình liên tiếp bằng thuật toán `Lighten Blend Mode` mà không làm cháy sáng hậu cảnh.
  - *Motion Blur (Làm mượt dòng nước chảy / Mây bay):* Thuật toán `Running Average Accumulator` làm mờ chuyển động mượt mà không cần kính lọc ND vật lý.

---

## 🏗 Kiến trúc kỹ thuật (Architecture)

```
app/src/main/java/com/sondeptrai/procam/
├── MainActivity.kt                      # Quản lý quyền, vòng đời và fullscreen
├── ProCamApplication.kt                 # Application class
├── core/
│   ├── camera/
│   │   ├── Camera2Controller.kt        # State machine điều khiển Camera2 HAL, CaptureRequest
│   │   ├── CameraHardwareCapabilities.kt # Truy vấn INFO_SUPPORTED_HARDWARE_LEVEL, ISO/Shutter range
│   │   ├── CameraParameters.kt         # Data model, enums và CameraUiState
│   │   └── RawCaptureManager.kt        # Bắt luồng RAW_SENSOR và ghi file .dng qua DngCreator
│   ├── gl/
│   │   ├── CameraGlView.kt             # Custom GLSurfaceView nhúng vào Compose
│   │   ├── CameraGlRenderer.kt         # Quản lý SurfaceTexture preview ở tốc độ 60 FPS
│   │   ├── GlShaders.kt                # Mã nguồn GPU GLSL cho Sobel Peaking & Zebra stripes
│   │   ├── GlUtils.kt                  # Biên dịch & liên kết Shader Programs
│   │   └── LiveHistogramCalculator.kt  # Tính toán phân bổ 256 bins độ sáng
│   ├── computational/
│   │   ├── FrameStackingEngine.kt      # Xử lý ghép ảnh Light Trails và Motion Blur
│   │   └── AntiShakeDetector.kt        # Đọc cảm biến hồi chuyển phát hiện đứng yên
│   └── sensor/
│       └── OrientationLevelSensor.kt   # Tính góc pitch/roll cho thước cân bằng chân trời
└── ui/
    ├── theme/                          # Dark DSLR color palette, typography và ProCamTheme
    ├── viewmodel/
    │   └── CameraViewModel.kt          # Trung tâm quản lý state giữa Camera2 và Compose
    └── camera/
        ├── CameraScreen.kt             # Viewfinder tràn viền + HUD overlays
        ├── CameraTopBar.kt             # Thanh công cụ Flash, RAW/JPG, Peaking, Zebra, Grid
        ├── ManualControlDial.kt        # Vòng xoay cơ học chỉnh tay ISO, Shutter, Focus, WB, EV
        ├── ManualParamSelectorBar.kt   # Nút bấm trực tiếp [ISO] [SEC] [FOCUS] [WB] [EV]
        ├── ModeSelector.kt             # Thanh cuộn chọn chế độ chụp
        ├── ShutterButton.kt            # Nút chụp cảm ứng xúc giác DSLR
        ├── TiltMeterWidget.kt          # Thước đo thăng bằng điện tử
        ├── HistogramWidget.kt          # Widget vẽ đồ thị đo sáng Canvas
        └── ViewfinderOverlay.kt        # Lưới bố cục và điểm chạm lấy nét riêng biệt
```

---

## 🚀 Hướng dẫn mở và chạy dự án (How to Run)

1. Mở **Android Studio** (Hedgehog, Iguana, Koala hoặc mới hơn).
2. Chọn **Open** và dẫn tới thư mục: `D:\procam_android`.
3. Để Gradle tự động tải các dependencies và đồng bộ (Sync Project with Gradle Files).
4. Kết nối điện thoại Android thật (Khuyến nghị thiết bị hỗ trợ `Hardware Level FULL` hoặc `LEVEL_3` như Samsung Galaxy S/Ultra, Google Pixel, Xiaomi dòng cao cấp) và bật **USB Debugging**.
5. Nhấn **Run (Shift + F10)**.

---

## 🔗 Đẩy mã nguồn lên GitLab

Kho lưu trữ Git cục bộ đã được khởi tạo và cấu hình remote sẵn sàng:
```bash
cd D:\procam_android
git add .
git commit -m "feat: Initial commit with full Camera2 manual controls, OpenGL shaders, RAW DNG, and Jetpack Compose UI"
git push -u origin main
```
*(Nếu GitLab yêu cầu chứng thực, sử dụng Personal Access Token có quyền `write_repository`).*
