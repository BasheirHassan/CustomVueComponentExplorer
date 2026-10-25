# Custom Vue Component Explorer — إضافة ويب ستورم

إضافة للـ **WebStorm** (منصة IntelliJ) تعرض **قائمة بكل المكوّنات المخصصة لملفات Vue** الموجودة في
المشروع الحالي، مع عدد مرات استخدام كل مكوّن داخل القوالب.

> الاسم: **Custom Vue Component Explorer** — معرّف الإضافة: `com.customvue.componentexplorer`
> الاسم بالعربية: **كوستم فيو كمباوننت إكسبلور**
> معرّف الحزمة (Java package): `com.customvue.componentexplorer`

### هوية الإضافة (Plugin Identity)

| العنصر | القيمة |
|---|---|
| **Name** (الاسم الظاهر) | `Custom Vue Component Explorer` |
| **Id** (معرّف الإضافة) | `com.customvue.componentexplorer` |
| **Vendor** (الجهة) | `Custom Vue Component Explorer` |
| **Java package** | `com.customvue.componentexplorer` |
| **Gradle group** | `com.customvue.componentexplorer` |
| **Gradle root project** | `custom-vue-component-explorer` |
| **Tool window id** | `Custom Vue Component Explorer` |
| **Action ids** | `CustomVueComponentExplorer.ShowToolWindow` |
| **Resource bundle** | `messages/CustomVueComponentExplorerBundle` (+ `_ar`) |

> الـ `Id` هو المعرّف الفريد للإضافة؛ تغييره يجعل الـ IDE يعتبرها إضافة جديدة
> (إصدار مستقل) بينما الـ `Name` هو ما يظهر للمستخدم في قائمة الإضافات.

---

## المزايا

| الميزة | الوصف |
|---|---|
| **مسح شامل** | يقرأ كل ملفات `.vue` في المشروع ويستخرج اسم المكوّن من `defineOptions` أو `defineComponent` أو `export default`، وإن لم يوجد اسم فسيُشتق من اسم الملف. |
| **التسجيلات العامة** | يكتشف `app.component('MyButton', MyButton)` و `Vue.component(...)`. |
| **خيار components** | يكتشف المكوّنات المعلَنة عبر `components: { MyButton }`. |
| **عناصر الويب** | يكتشف `customElements.define('my-element', MyElement)`. |
| **عدّ الاستخدام** | يحسب كم مرة يُستخدم وسم المكوّن داخل قوالب المشروع. |
| **كل مواضع الاستخدام** | إذا تكرّر المكوّن داخل الصفحة المفتوحة يظهر صف مستقل لكل موضع مع رقم سطره، فيمكن الانتقال إلى أي موضع مباشرة. |
| **المتابعة للصفحة المفتوحة** | القائمة تتابع الملف المفتوح في المحرر تلقائياً، وتبيّن لكل مكوّن علاقته بالصفحة: معرّف فيها، مُستورد منها، مُستخدم في قالبها، أو مسجّل عالمياً. |
| **عرض الصفحة فقط** | زر في شريط الأدوات يقصر القائمة على مكوّنات الصفحة المفتوحة (مفعّل افتراضياً). |
| **بحث فوري** | حقل تصفية فوري داخل قائمة النتائج. |
| **التنقل السريع** | نقر مزدوج على العنصر لفتح ملف المكوّن على سطر التعريف مباشرة. |
| **نسخ** | نسخ اسم الوسم أو مسار الملف من قائمة النقر الأيمن. |
| **بحث في كل المشروع** | خيار في قائمة النقر الأيمن يبحث عن المكوّن في جميع ملفات المشروع ويعرض النتائج في نافذة Find، مع البحث عن الاسم والوسم معًا. |
| **تحديث تلقائي** | تُحدَّث القائمة تلقائياً عند حفظ أي ملف `.vue` أو `.js` أو `.ts`. |
| **واجهة عربية** | الواجهة متوفرة بالعربية والإنجليزية حسب لغة الـ IDE. |

---

## التثبيت

> ✅ **تم تثبيت الإضافة مسبقاً** في:
> `%APPDATA%\JetBrains\WebStorm2026.2\plugins\custom-vue-component-explorer\`
> كل ما عليك هو **إعادة تشغيل WebStorm** لتظهر نافذة الأداة.
> (إن كان المجلد القديم `custom-vue-component` ما زال موجوداً، احذفه حتى لا تظهر
> نسختان من الإضافة في القائمة.)

### الطريقة الأولى: تثبيت من القرص (الأسهل)

1. ابنِ الإضافة (انظر [البناء من المصدر](#البناء-من-المصدر)) للحصول على الملف:
   `build/distributions/custom-vue-component-explorer-1.1.0.zip`
2. في WebStorm: **File → Settings → Plugins → ⚙ → Install Plugin from Disk…**
3. اختر الملف `.zip` ثم أعد تشغيل WebStorm.

### الطريقة الثانية: التثبيت يدوياً

انسخ محتوى `build/distributions/custom-vue-component-explorer-1.1.0.zip` إلى مجلد plugins الخاص بـ WebStorm:

```
%APPDATA%\JetBrains\WebStorm2026.2\plugins\custom-vue-component-explorer\
```

ثم أعد تشغيل WebStorm.

---

## الاستخدام

1. افتح مشروعاً يحتوي على مكوّنات Vue.
2. افتح نافذة الأداة **Custom Vue Component Explorer** من:
   * **View → Tool Windows → Custom Vue Component Explorer**، أو
   * **Tools → Custom Vue Component Explorer**.
3. **افتح أي ملف `.vue` في المحرر**: تتحدّث القائمة فوراً لتعرض مكوّنات تلك الصفحة.
4. ستظهر القائمة بالشكل التالي:

| العمود | المعنى |
|---|---|
| **Component** | اسم المكوّن كما هو معلَن (`MyButton`). |
| **Tag** | الوسم المستخدم في القوالب (`my-button`). |
| **Type** | نوع اكتشاف المكوّن (ملف مفرد / عالمي / خيار / عنصر ويب). |
| **Location** | مسار الملف نسبةً لجذر المشروع. |
| **Line** | رقم السطر الذي سينتقل إليه النقر المزدوج. |
| **Usages** | عدد مرات استخدام الوسم في المشروع. |
| **In this file** | علاقة المكوّن بالصفحة المفتوحة. |

#### علاقة المكوّن بالصفحة المفتوحة

| القيمة | المعنى |
|---|---|
| **This file** | المكوّن معرّف داخل الملف المفتوح نفسه. |
| **Imported** | الملف المفتوح يستورد المكوّن (`import AppButton from ...`). |
| **Used** | وسم المكوّن مستخدم داخل قالب الملف المفتوح. |
| **Global** | المكوّن مسجّل عالمياً (`app.component`) فهو متاح في كل الصفحات. |
| **Available** | مكوّن من المشروع غير مرتبط بالصفحة المفتوحة (يُخفى عند تفعيل «الصفحة المفتوحة فقط»). |

* **نقر مزدوج** على أي صف → الانتقال إلى **مكان المكوّن داخل الصفحة المفتوحة**:
  * إذا كان المكوّن مستخدماً في قالب الصفحة → يُعرض **صف لكل موضع استخدام** (سطر مستقل)،
    والنقر على أي صف يقفز إلى سطر الوسم المقابل له.
  * وإذا كان مستورداً فقط → يقفز إلى سطر الاستيراد داخل الصفحة.
  * وإذا لم تكن الصفحة تستخدمه → يفتح ملف تعريفه عند سطر التعريف.
* **تلميح** على اسم المكوّن يوضّح وجهة الانتقال (`App.vue:12`).
* **زر الفأرة الأيمن** → البحث في كل المشروع / نسخ الوسم / نسخ المسار / تحديث.
* **حقل البحث** في الأعلى → تصفية فورية بالاسم أو الوسم أو المسار.
* **زر «الصفحة المفتوحة فقط»** (أيقونة الوسم في شريط الأدوات) → تبديل بين عرض صفحة واحدة أو كل المشروع.

---

## البناء من المصدر

### المتطلبات

* JDK 21 أو أحدث (تم التحقق من البناء بـ JDK 25)
* WebStorm 2026.2.1 مثبّت محلياً (أو غيّر مسار الـ IDE في `build.gradle.kts`)

```bash
# بناء حزمة التثبيت
./gradlew buildPlugin

# تشغيل WebStorm مع الإضافة (سند رمل)
./gradlew runIde

# التحقق من بنية الإضافة
./gradlew verifyPlugin
```

الناتج: `build/distributions/custom-vue-component-explorer-1.1.0.zip`

> ملف `build.gradle.kts` يستخدم `local(...)` للإشارة إلى نسخة WebStorm المثبتة محلياً لتجنّب تنزيل
> حزمة IDE كاملة. لاستخدام نسخة عن بُعد بدّل السطر إلى:
> ```kotlin
> create(IntelliJPlatformType.WebStorm, "2026.2.1")
> ```

---

## بنية المشروع

```
src/main/java/com/customvue/componentexplorer/
├── CustomVueComponentExplorerBundle.java  نصوص الواجهة (عربي/إنجليزي)
├── model/
│   ├── ComponentKind.java                   أنواع المكوّنات
│   └── VueComponentInfo.java                بيانات المكوّن (اسم، وسم، ملف، سطر، استخدامات)
├── scan/
│   ├── ComponentParser.java                 تحليل النص واستخراج المكوّنات (بلا اعتماد على PSI)
│   ├── ComponentDeclaration.java            تصريح المكوّن داخل ملف
│   ├── ParsedFile.java                      نتيجة تحليل ملف واحد
│   └── CustomVueComponentExplorerScanner.java  مسح المشروع + كاش ذكي (يعيد التحليل فقط عند التغيير)
├── service/
│   └── CustomVueComponentExplorerService.java  خدمة المشروع: المسح في الخلفية + إشعار المستمعين
├── ui/
│   ├── CustomVueComponentExplorerToolWindowFactory.java
│   ├── CustomVueComponentExplorerPanel.java  نافذة الأداة (بحث + جدول + قائمة سياق)
│   └── CustomVueComponentExplorerTableModel.java
└── actions/                                 Refresh / Copy Tag / Copy Path / Show Tool Window
```

### كيف يعمل المسح؟

1. تُجمع كل ملفات `.vue` و `.js` و `.ts` و `.jsx` و `.tsx` عبر `FileTypeIndex`.
2. تُستثنى المجلدات غير المصدرية (`node_modules` و `dist` و `build` …).
3. يُحلَّل نص كل ملف بمحلّل كتل أقواس وقواعد regex لتحديد:
   * `defineOptions({ name: 'X' })`
   * `defineComponent({ name: 'X' })`
   * `export default { name: 'X' }`
   * `app.component('X', …)` و `Vue.component('X', …)`
   * `components: { X }`
   * `customElements.define('x', …)`
4. تُحسب الاستخدامات بعد جمع كل الملفات (لكل وسم عدد الظهور في القوالب).
5. تُخزَّن النتائج في كاش داخل `VueComponentScanner` مرتبط بوقت تعديل الملف، لذلك إعادة
   الفحص سريعة ولا تلمس الملفات غير المتغيّرة.

---

## مشروع تجريبي

يوجد في المجلد `sample-project/` مشروع Vue صغير يحتوي على كل الحالات المدعومة:

| الملف | ما يختبره |
|---|---|
| `src/components/AppButton.vue` | `defineOptions({ name: 'AppButton' })` |
| `src/components/UserCard.vue` | `export default defineComponent({ name: 'UserCard' })` |
| `src/components/LegacyWidget.vue` | بلا اسم معلَن (يُشتق من اسم الملف) |
| `src/components/ToastNotification.vue` | `<script setup>` بدون اسم |
| `src/main.ts` | `app.component('X', X)` |
| `src/App.vue` | استخدام المكوّنات في القوالب (لحساب عدد الاستخدامات) |

افتح `sample-project` في WebStorm بعد إعادة التشغيل وستظهر لك القائمة جاهزة.

---

## ملاحظات

* الإضافة لا تعتمد على إضافة Vue الرسمية، بل تحلّل النصوص مباشرة — لذلك تعمل حتى لو كانت
  إضافة Vue معطّلة.
* الإضافة تعمل على أي IDE من عائلة IntelliJ Platform، لكنها مصممة لـ WebStorm.
* `since-build = 262` (أي WebStorm 2026.2 وأحدث).
* إذا ظهرت القائمة فارغة، راجع سجل الـ IDE عبر **Help → Show Log in Explorer** وابحث عن
  `Vue components scan finished` — يذكر هذا السطر عدد الملفات التي تم فحصها وعدد المكوّنات
  التي تم العثور عليها.
