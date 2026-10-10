# Lumivara Online FPS — Android 1.0.0

รุ่นสำหรับเล่นหนึ่งจอ / หนึ่งไอดี เน้นลดภาระของตัวแอปและเปิดให้เกมใช้ FPS สูงขึ้น

แอปชื่อ **Lumivara Online FPS** ใช้แพ็กเกจ `com.lumivaraonline.fps` แยกจากรุ่นหลายจอ ติดตั้งควบคู่กันได้ แต่ข้อมูลล็อกอินไม่ได้ย้ายตามมา ต้องเข้าสู่ระบบในรุ่นนี้ใหม่ หากใช้ Guest เดิม ให้ผูกบัญชีในแอปเดิมก่อนเปลี่ยนรุ่น

## ดาวน์โหลด

[ดาวน์โหลด APK v1.0.0](https://github.com/newkiss2582/LumivaraOnline-FPS-APK/releases/download/v1.0.0/Lumivara-Online-FPS-Android-1.0.apk) · [Releases ล่าสุด](https://github.com/newkiss2582/LumivaraOnline-FPS-APK/releases/latest)

รองรับ Android 8 ขึ้นไป เลือกไฟล์ **Lumivara-Online-FPS-Android-1.0.apk**

## การตั้งค่า

- **FPS สูงสุด / 120 / 60** — ค่าเริ่มต้นสูงสุด ใช้ตัวเลือกไม่จำกัด FPS ที่เกมรองรับ
- ขอรีเฟรชเรตสูงสุดที่จอรองรับ ณ ความละเอียดจอเดิม เมื่อแสดงเกม; คืนการเลือกให้ระบบเมื่อย่อเป็นปุ่มลอยหรือปิดจอ
- **ความละเอียดเรนเดอร์ 100% / 85% / 70%** — ค่าเริ่มต้น 85%; 70% ลดขนาด viewport ที่เรนเดอร์มากขึ้นและขยายภาพให้เต็มพื้นที่ ภาพ/ตัวอักษรอาจเปลี่ยนขนาดตามการจัดหน้าเกม; 100% ใช้ความละเอียดเต็ม
- คำนวณการสัมผัสกลับเข้าสู่พิกัดเกมตามสเกล รวมถึงหลายจุดสัมผัส
- เปิดเต็มจอแนวนอนได้จากหน้าหลัก; ย่อจอเล็ก ปรับขนาด และย่อเป็นปุ่มลอยได้ โดยใช้เกมตัวเดิมหนึ่งเซสชัน
- เมนูแอป TH / EN แยกจากภาษาในเกม
- การเก็บข้อมูลวิเคราะห์เป็นแบบกดเปิด ไม่มีการเขียนประวัติทุก 10 วินาทีระหว่างเล่น

ใช้ Android System WebView ที่มีในเครื่อง พร้อม hardware acceleration ไม่มีการรวมเอนจินอีกชุดใน APK

## วิธีเริ่ม

1. ติดตั้ง `Lumivara-Online-FPS-Android-1.0.apk`
2. เปิดแอป อนุญาตแสดงทับแอปและการแจ้งเตือนตามที่แอปแนะนำ
3. เลือก FPS และความละเอียดภาพ แล้วกด **เล่นเต็มจอแนวนอน**
4. ดูตัวนับ FPS ภายในเกมเพื่อประเมินผลจริง ถ้าภาพไม่ชัดให้ใช้ 100%; ถ้าต้องการลดภาระการวาดภาพให้ลอง 70%
5. หากเครื่องร้อนจน FPS ตก ให้ลองจำกัด 60 หรือ 120 ตามจอที่ใช้

การตั้งค่า FPS ในหน้าหลักใช้กับการเปิดเกมแต่ละครั้ง เปลี่ยนได้ระหว่างที่เกมยังทำงานอยู่ ค่า FPS สูงสุดไม่ได้รับประกันว่าเครื่องจะทำได้ตามนั้น จอ 60 Hz ไม่สามารถแสดงภาพ 120 เฟรมที่แยกจากกันในหนึ่งวินาที

เกมจะเริ่มโดยปิดการเข้าหน้าจอดำประหยัดแบตอัตโนมัติในการใช้งานครั้งแรก หากเลือกเปิดตัวเลือกนี้ภายในเกมภายหลัง แอปจะเก็บตัวเลือกนั้นไว้ การใช้เต็มประสิทธิภาพอาจใช้แบตและเกิดความร้อนมากขึ้น

การย่อ/ปิดจอมีตัวช่วยคงเฟรมและตัวจับเวลาเดิม แต่ Android และผู้ผลิตมือถือยังอาจจำกัด CPU หรือเครือข่าย จึงไม่รับประกัน FPS หรือการทำงานตลอดเวลาในเบื้องหลัง

## Google Login

ล็อกอินผ่าน Custom Tab ของเบราว์เซอร์ที่รองรับ แล้วส่งกลับเข้าเกมในแอป ตรวจสอบ **เปิดตามค่าเริ่มต้น → ลิงก์ที่รองรับ → lumivaraonline.com** ให้เปิดด้วย **Lumivara Online FPS** หากติดตั้งแอปเดิมร่วมกัน Android อาจเลือกแอปเดิมเป็นตัวรับลิงก์ ต้องเปลี่ยนการเลือกก่อนล็อกอินรุ่น FPS

ไฟล์ `web-association/assetlinks.json` เป็นรายการสำหรับแพ็กเกจ FPS เท่านั้น ผู้ดูแลโดเมนต้องนำรายการนี้ไปรวมกับรายการเดิมที่ `https://lumivaraonline.com/.well-known/assetlinks.json` เพื่อยืนยันลิงก์โดยอัตโนมัติ อย่าแทนที่รายการแอปเดิม การสร้าง APK นี้ไม่ได้แก้ไขไฟล์บนเซิร์ฟเวอร์เกม

## Compatibility and validation

- Minimum Android 8 (API 26), target API 36
- One registered game service, one fullscreen game host, no secondary profile processes
- Built and signature-verified with the existing signing certificate
- Browser fixtures cover FPS setting handlers, dropdowns, language isolation and duplicate-injection behavior
- Java tests cover display selection, production refresh requests, render geometry, session/navigation policies and background scheduling
- Device performance, thermal throttling, native WebView touch latency and long-session connectivity still require real-phone testing. No phone was connected for an FPS benchmark

## Build

Java/JDK, Android platform 36 and Android build tools with aapt2, d8, zipalign and apksigner are required. The signing directory must contain the existing `lumivara-android.jks` and `signing-password.txt`. Private signing files are excluded from the source archive.

```powershell
./build.ps1 -PlatformJar <android.jar> -BuildTools <build-tools-dir> -SigningDir <private-signing-dir> -OutputApk <absolute-output.apk>
```

Install Playwright for Node and set `PLAYWRIGHT_MODULE` to the module directory. Set `TEST_BROWSER` to a Chromium/Edge executable if needed, then run:

```powershell
./tests/run-tests.ps1
```

ดาวน์โหลด APK, Source ZIP และ SHA256 ได้จาก [GitHub Releases](https://github.com/newkiss2582/LumivaraOnline-FPS-APK/releases/latest)
