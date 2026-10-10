package ir.ettehad.realestate

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.*
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class MainActivity : Activity() {
    private val navy = Color.rgb(23, 51, 79)
    private val gold = Color.rgb(231, 197, 106)
    private val cream = Color.rgb(250, 248, 241)
    private val prefs by lazy { getSharedPreferences("ettehad_settings", Context.MODE_PRIVATE) }
    private lateinit var root: LinearLayout
    private val settingsFields = linkedMapOf<String, EditText>()

    private data class Setting(val key: String, val label: String, val default: String, val hint: String)
    private val settings = listOf(
        Setting("vat", "ارزش افزوده (درصد)", "10", "مثلاً 10"),
        Setting("minSale", "حداقل مبلغ معامله (تومان)", "1200000000", "1200000000"),
        Setting("minRate", "درصد کمیسیون تا حداقل مبلغ (%)", "1", "1"),
        Setting("extraRate", "درصد کمیسیون مبلغ مازاد (%)", "0.25", "0.25"),
        Setting("depositRate", "درصد کمیسیون مبلغ رهن (%)", "1", "1"),
        Setting("rentFactor", "ضریب کمیسیون اجاره ماهانه", "0.3333333333", "0.333333"),
        Setting("renewLandlord", "ضریب تمدید موجر", "0.5", "0.5"),
        Setting("renewTenant", "ضریب تمدید مستأجر", "0.25", "0.25")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = navy
        window.navigationBarColor = navy
        showHome()
    }

    private fun makeRoot(): LinearLayout {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(cream)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(12), dp(16), dp(18))
        }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
        return root
    }

    private fun header(subtitle: String) {
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(navy, 16)
        }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.ettehad_logo)
            layoutParams = LinearLayout.LayoutParams(dp(58), dp(58))
        }
        top.addView(logo)
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        texts.addView(TextView(this).apply {
            text = "املاک اتحاد"
            textSize = 24f; setTextColor(gold); typeface = Typeface.DEFAULT_BOLD
        })
        texts.addView(TextView(this).apply {
            text = subtitle; textSize = 13f; setTextColor(Color.WHITE)
        })
        top.addView(texts)
        root.addView(top, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
    }

    private fun showHome() {
        makeRoot()
        header("سامانه محاسبه کمیسیون معاملات")
        addButton("محاسبه کمیسیون خرید و فروش", true) { showSale() }
        addButton("محاسبه کمیسیون رهن و اجاره", true) { showRent() }
        addButton("محاسبه تمدید قرارداد", true) { showRenewal() }
        addButton("تنظیمات نرخ‌ها و مبالغ", false) { showSettings() }
        addText("تنظیمات به‌صورت خودکار روی همین گوشی ذخیره می‌شوند.", 14, navy, false)
        addText("مبالغ بر اساس فایل اکسل ارسالی تنظیم اولیه شده‌اند.", 13, Color.DKGRAY, false)
    }

    private fun showSale() {
        makeRoot(); header("محاسبه کمیسیون خرید و فروش")
        val amount = input("مبلغ کل معامله (تومان)", "")
        val result = resultBox()
        addButton("محاسبه کمیسیون", true) {
            val sale = amount.text.toString().toLongOrNullSafe()
            if (sale <= 0.0) { result.text = "لطفاً مبلغ معامله را به تومان وارد کنید."; return@addButton }
            val min = setting("minSale")
            val base = if (sale <= min) sale * setting("minRate") / 100.0
                       else min * setting("minRate") / 100.0 + (sale - min) * setting("extraRate") / 100.0
            val vat = base * setting("vat") / 100.0
            result.text = "کمیسیون بدون ارزش افزوده: ${money(base)} تومان\nارزش افزوده: ${money(vat)} تومان\nمبلغ نهایی کمیسیون: ${money(base + vat)} تومان"
        }
        addButton("بازگشت", false) { showHome() }
    }

    private fun showRent() {
        makeRoot(); header("محاسبه کمیسیون رهن و اجاره")
        addText("مبالغ رهن و اجاره را به میلیون تومان وارد کنید.", 14, navy, false)
        val deposit = input("مبلغ رهن (میلیون تومان)", "")
        val rent = input("اجاره ماهانه (میلیون تومان)", "")
        val result = resultBox()
        addButton("محاسبه کمیسیون", true) {
            val d = deposit.text.toString().toLongOrNullSafe()
            val r = rent.text.toString().toLongOrNullSafe()
            if (d < 0 || r < 0 || (d == 0.0 && r == 0.0)) {
                result.text = "لطفاً مبلغ رهن یا اجاره را وارد کنید."; return@addButton
            }
            val depFee = d * 1_000_000.0 * setting("depositRate") / 100.0
            val rentFee = r * 1_000_000.0 * setting("rentFactor")
            val base = depFee + rentFee
            val vat = base * setting("vat") / 100.0
            result.text = "کمیسیون رهن: ${money(depFee)} تومان\nکمیسیون اجاره: ${money(rentFee)} تومان\nکمیسیون بدون ارزش افزوده: ${money(base)} تومان\nارزش افزوده: ${money(vat)} تومان\nمبلغ نهایی کمیسیون: ${money(base + vat)} تومان"
        }
        addButton("بازگشت", false) { showHome() }
    }

    private fun showRenewal() {
        makeRoot(); header("محاسبه کمیسیون تمدید")
        val base = input("کمیسیون رهن و اجاره با ارزش افزوده (تومان)", "")
        val result = resultBox()
        addButton("محاسبه سهم طرفین", true) {
            val total = base.text.toString().toLongOrNullSafe()
            if (total <= 0) { result.text = "مبلغ کمیسیون را وارد کنید."; return@addButton }
            result.text = "سهم تمدید موجر: ${money(total * setting("renewLandlord"))} تومان\nسهم تمدید مستأجر: ${money(total * setting("renewTenant"))} تومان"
        }
        addButton("بازگشت", false) { showHome() }
    }

    private fun showSettings() {
        makeRoot(); header("تنظیمات قابل ویرایش")
        addText("مقادیر در حافظه همین گوشی ذخیره می‌شوند و برای محاسبات بعدی باقی می‌مانند.", 14, navy, false)
        settingsFields.clear()
        settings.forEach { s ->
            val e = input(s.label, prefs.getString(s.key, s.default) ?: s.default)
            settingsFields[s.key] = e
        }
        addButton("ذخیره تنظیمات", true) {
            val editor = prefs.edit()
            var valid = true
            settingsFields.forEach { (key, field) ->
                val value = field.text.toString().trim().replace(",", ".")
                if (value.isEmpty() || value.toDoubleOrNull() == null || value.toDouble() < 0) valid = false
                else editor.putString(key, value)
            }
            if (valid) {
                editor.apply()
                Toast.makeText(this, "تنظیمات ذخیره شد", Toast.LENGTH_SHORT).show()
                hideKeyboard()
                showHome()
            } else Toast.makeText(this, "لطفاً همه مقادیر را به‌درستی وارد کنید.", Toast.LENGTH_LONG).show()
        }
        addButton("بازگشت بدون ذخیره", false) { showHome() }
    }

    private fun setting(key: String): Double =
        (prefs.getString(key, settings.first { it.key == key }.default) ?: "0")
            .replace(",", ".").toDoubleOrNull() ?: 0.0

    private fun input(label: String, value: String): EditText {
        addText(label, 14, navy, true)
        val e = EditText(this).apply {
            setText(value); textSize = 16f; setTextColor(Color.BLACK)
            hint = "مقدار را وارد کنید"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setSingleLine(true)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = rounded(Color.WHITE, 10, Color.LTGRAY)
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
        }
        root.addView(e, LinearLayout.LayoutParams(-1, dp(50)).apply { bottomMargin = dp(10) })
        return e
    }

    private fun resultBox(): TextView {
        val t = TextView(this).apply {
            text = "نتیجه محاسبه اینجا نمایش داده می‌شود."
            textSize = 16f; setTextColor(navy); setPadding(dp(14), dp(14), dp(14), dp(14))
            gravity = Gravity.RIGHT
            background = rounded(Color.WHITE, 12, gold)
        }
        root.addView(t, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12); bottomMargin = dp(12) })
        return t
    }

    private fun addText(text: String, size: Int, color: Int, bold: Boolean) {
        root.addView(TextView(this).apply {
            this.text = text; textSize = size.toFloat(); setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(dp(2), dp(5), dp(2), dp(5))
        }, LinearLayout.LayoutParams(-1, -2))
    }

    private fun addButton(label: String, primary: Boolean, action: () -> Unit) {
        val b = Button(this).apply {
            text = label; textSize = 15f
            setTextColor(if (primary) gold else navy)
            background = rounded(if (primary) navy else Color.WHITE, 12, if (primary) navy else gold)
            setOnClickListener { action() }
        }
        root.addView(b, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(5); bottomMargin = dp(5) })
    }

    private fun rounded(color: Int, radius: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun money(v: Double): String {
        val f = DecimalFormat("#,###.##", DecimalFormatSymbols(Locale.US))
        return f.format(v)
    }

    private fun String.toLongOrNullSafe(): Double =
        trim().replace(",", "").replace("٬", "").replace("٫", ".").toDoubleOrNull() ?: 0.0

    private fun hideKeyboard() {
        try { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(root.windowToken, 0) } catch (_: Exception) {}
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
