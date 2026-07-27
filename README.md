# CallGuard

Android app: SIM 1 par unknown incoming calls block karta hai, SIM 2 par sabhi calls allow rehti hain.

## APK kaise banayein (koi coding nahi chahiye)

1. **GitHub par free account banayein** (agar pehle se nahi hai): https://github.com/signup

2. **Naya repository banayein**:
   - github.com par login karke top-right "+" > "New repository" click karein
   - Repository name: `CallGuard` (ya kuch bhi)
   - "Create repository" click karein — README add mat karein (empty rakhein)

3. **Is poore folder (CallGuard) ki saari files upload karein**:
   - Naye repo page par "uploading an existing file" link dikhega, us par click karein
   - Is `CallGuard` folder ke andar ki SAARI files aur folders (including hidden `.github` folder) drag-and-drop karein
   - Neeche "Commit changes" green button dabayein

   > Note: `.github` folder hidden hota hai file explorer mein — agar drag-drop se hidden folder nahi dikh raha, toh apne file manager mein "show hidden files" ON karein, ya GitHub Desktop app use karein.

4. **Build automatically start ho jayega**:
   - Repo ke andar "Actions" tab par click karein
   - "Build APK" workflow run hote dikhega (yellow dot = running, green tick = done) — 3-5 minute lagenge

5. **APK download karein**:
   - Jab run complete ho jaye (green tick), us run par click karein
   - Neeche "Artifacts" section mein "CallGuard-debug-apk" milega — usme click karke zip download karein
   - Zip ke andar `app-debug.apk` hoga

6. **Phone mein install karein**:
   - `app-debug.apk` file apne Android phone mein transfer karein (WhatsApp/email/USB/Google Drive - kisi bhi tarike se)
   - Phone par file open karein — "install from unknown sources" allow karne ke liye kahega, allow kar dein
   - Install ho jayega

## App use karna

1. App open karne par permissions maangega (contacts, phone state, call log) — sab allow karein
2. "Default call screening app set karein" button dabayein — Android ek popup dikhayega jisme CallGuard ko select karna hai
3. Toggle switch ON karein — ab SIM 1 par un numbers ki calls block ho jayengi jo aapke Contacts mein save nahi hain
4. SIM 2 par hamesha sabhi calls (known + unknown) allow rahengi

## Important limitations

- Yeh sirf incoming calls ko "reject" karta hai — outgoing calls par koi effect nahi
- Dual-SIM detection Android version aur phone brand (Samsung, Xiaomi, etc.) ke hisaab se thoda alag behave kar sakta hai; kuch phones par SIM slot detect na ho toh dono SIM par blocking logic SIM1 jaisa treat hoga (safe default)
- Blocked calls call log mein "missed" dikhengi, caller ko "number busy/unreachable" jaisa experience milega (yeh call screening ka standard behavior hai)
- Yeh ek "debug" build hai (testing ke liye) — apne personal use ke liye bilkul theek hai
