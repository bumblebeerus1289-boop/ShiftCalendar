package com.example.shiftcalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String PREFS = "shift_calendar_data";
    private static final Locale RU = new Locale("ru");

    private final Calendar shownMonth = Calendar.getInstance();
    private SharedPreferences prefs;

    private TextView monthTitle;
    private GridLayout calendarGrid;
    private TextView summary;
    private LinearLayout swipeArea;

    private final int[] paletteColors = new int[] {
            Color.rgb(187, 222, 251), // light blue
            Color.rgb(200, 230, 201), // green
            Color.rgb(255, 224, 178), // orange
            Color.rgb(255, 245, 157), // yellow
            Color.rgb(225, 190, 231), // purple
            Color.rgb(255, 205, 210), // pink/red
            Color.rgb(207, 216, 220), // grey
            Color.rgb(178, 235, 242)  // cyan
    };

    private final String[] paletteNames = new String[] {
            "Голубой", "Зелёный", "Оранжевый", "Жёлтый",
            "Фиолетовый", "Розовый", "Серый", "Бирюзовый"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        ensureDefaults();

        shownMonth.set(Calendar.DAY_OF_MONTH, 1);
        buildMainScreen();
        renderMonth();
    }

    private void ensureDefaults() {
        if (!prefs.contains("salary")) {
            Calendar now = Calendar.getInstance();
            String today = String.format(Locale.US, "%04d-%02d-%02d",
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH) + 1,
                    now.get(Calendar.DAY_OF_MONTH));

            prefs.edit()
                    .putFloat("salary", 0f)
                    .putFloat("defaultCoeff", 1.0f)
                    .putInt("cycleWork", 2)
                    .putInt("cycleOff", 2)
                    .putString("anchorDate", today)
                    .putInt("colorShift", paletteColors[0])
                    .putInt("colorOvertime", paletteColors[2])
                    .putInt("colorGig", paletteColors[3])
                    .apply();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private TextView label(String text, float sp, int gravity) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(sp);
        v.setTextColor(Color.rgb(35, 35, 35));
        v.setGravity(gravity);
        return v;
    }

    private void buildMainScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(10));
        root.setBackgroundColor(Color.WHITE);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        Button prev = new Button(this);
        prev.setText("‹");
        prev.setTextSize(22);
        prev.setOnClickListener(v -> changeMonth(-1));

        monthTitle = label("", 21, Gravity.CENTER);
        monthTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        Button next = new Button(this);
        next.setText("›");
        next.setTextSize(22);
        next.setOnClickListener(v -> changeMonth(1));

        header.addView(prev, new LinearLayout.LayoutParams(dp(54), dp(54)));
        header.addView(monthTitle, new LinearLayout.LayoutParams(0, dp(54), 1));
        header.addView(next, new LinearLayout.LayoutParams(dp(54), dp(54)));
        root.addView(header);

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button settings = new Button(this);
        settings.setText("Настройки");
        settings.setOnClickListener(v -> showSettingsDialog());

        Button today = new Button(this);
        today.setText("Сегодня");
        today.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            shownMonth.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), 1);
            renderMonth();
        });

        actions.addView(settings);
        actions.addView(today);
        root.addView(actions);

        swipeArea = new LinearLayout(this);
        swipeArea.setOrientation(LinearLayout.VERTICAL);
        calendarGrid = new GridLayout(this);
        calendarGrid.setColumnCount(7);
        calendarGrid.setUseDefaultMargins(false);

        swipeArea.addView(calendarGrid,
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        root.addView(swipeArea, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        summary = label("", 14, Gravity.START);
        summary.setPadding(dp(8), dp(8), dp(8), dp(8));
        root.addView(summary, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView hint = label("Месяцы перелистываются стрелками ‹  ›", 11, Gravity.CENTER);
        hint.setTextColor(Color.GRAY);
        root.addView(hint);

        setContentView(root);
    }

    private void changeMonth(int delta) {
        shownMonth.add(Calendar.MONTH, delta);
        shownMonth.set(Calendar.DAY_OF_MONTH, 1);
        renderMonth();
    }

    private void renderMonth() {
        SimpleDateFormat sdf = new SimpleDateFormat("LLLL yyyy", RU);
        String title = sdf.format(shownMonth.getTime());
        if (!title.isEmpty()) {
            title = title.substring(0, 1).toUpperCase(RU) + title.substring(1);
        }
        monthTitle.setText(title);

        calendarGrid.removeAllViews();

        String[] weekdays = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        for (String name : weekdays) {
            TextView h = label(name, 12, Gravity.CENTER);
            h.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            h.setTextColor(Color.DKGRAY);
            addGridView(h, dp(34));
        }

        Calendar first = (Calendar) shownMonth.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);
        int offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7;

        for (int i = 0; i < offset; i++) {
            TextView empty = label("", 12, Gravity.CENTER);
            addGridView(empty, dp(64));
        }

        int days = shownMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int d = 1; d <= days; d++) {
            addDayCell(d);
        }

        renderSummary();
    }

    private void addGridView(View view, int height) {
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = height;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.setMargins(dp(2), dp(2), dp(2), dp(2));
        calendarGrid.addView(view, p);
    }

    private void addDayCell(int day) {
        final String key = dateKey(shownMonth.get(Calendar.YEAR), shownMonth.get(Calendar.MONTH), day);
        boolean shift = isShift(key);
        float overtimeHours = prefs.getFloat(key + ".overtimeHours", 0f);
        String gigFirm = prefs.getString(key + ".gigFirm", "");
        String comment = prefs.getString(key + ".comment", "");

        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(dp(2), dp(3), dp(2), dp(2));

        int bg = shift ? prefs.getInt("colorShift", paletteColors[0]) : Color.rgb(250, 250, 250);
        cell.setBackgroundColor(bg);

        TextView number = label(String.valueOf(day), 15, Gravity.CENTER);
        if (isToday(day)) {
            number.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            number.setText("● " + day);
        }
        cell.addView(number, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout badges = new LinearLayout(this);
        badges.setGravity(Gravity.CENTER);
        badges.setOrientation(LinearLayout.HORIZONTAL);

        if (overtimeHours > 0f) {
            TextView b = label("П", 11, Gravity.CENTER);
            b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            b.setBackgroundColor(prefs.getInt("colorOvertime", paletteColors[2]));
            b.setPadding(dp(4), 0, dp(4), 0);
            badges.addView(b);
        }
        if (gigFirm != null && !gigFirm.trim().isEmpty()) {
            TextView b = label("Х", 11, Gravity.CENTER);
            b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            b.setBackgroundColor(prefs.getInt("colorGig", paletteColors[3]));
            b.setPadding(dp(4), 0, dp(4), 0);
            badges.addView(b);
        }
        if (comment != null && !comment.trim().isEmpty()) {
            TextView b = label("•", 16, Gravity.CENTER);
            b.setPadding(dp(3), 0, dp(3), 0);
            badges.addView(b);
        }

        cell.addView(badges, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(22)));

        cell.setOnClickListener(v -> showDayDialog(day, key));
        addGridView(cell, dp(68));
    }

    private boolean isToday(int day) {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.YEAR) == shownMonth.get(Calendar.YEAR)
                && now.get(Calendar.MONTH) == shownMonth.get(Calendar.MONTH)
                && now.get(Calendar.DAY_OF_MONTH) == day;
    }

    private String dateKey(int year, int monthZeroBased, int day) {
        return String.format(Locale.US, "%04d-%02d-%02d", year, monthZeroBased + 1, day);
    }

    private Calendar parseDate(String value) {
        try {
            String[] p = value.split("-");
            Calendar c = Calendar.getInstance();
            c.clear();
            c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]), 12, 0, 0);
            return c;
        } catch (Exception e) {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, 12);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            return c;
        }
    }

    private long localDayNumber(Calendar c) {
        Calendar x = (Calendar) c.clone();
        x.set(Calendar.HOUR_OF_DAY, 12);
        x.set(Calendar.MINUTE, 0);
        x.set(Calendar.SECOND, 0);
        x.set(Calendar.MILLISECOND, 0);
        return x.getTimeInMillis() / 86400000L;
    }

    private boolean generatedShift(String key) {
        Calendar date = parseDate(key);
        Calendar anchor = parseDate(prefs.getString("anchorDate", key));
        int work = Math.max(1, prefs.getInt("cycleWork", 2));
        int off = Math.max(0, prefs.getInt("cycleOff", 2));
        int cycle = work + off;
        if (cycle <= 0) cycle = 1;

        long diff = localDayNumber(date) - localDayNumber(anchor);
        long pos = ((diff % cycle) + cycle) % cycle;
        return pos < work;
    }

    private boolean isShift(String key) {
        int override = prefs.getInt(key + ".shiftOverride", -1);
        if (override == 0) return false;
        if (override == 1) return true;
        return generatedShift(key);
    }

    private void showDayDialog(int day, String key) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(6), dp(18), dp(6));

        boolean shift = isShift(key);
        float overtimeHours = prefs.getFloat(key + ".overtimeHours", 0f);
        float overtimeCoeff = prefs.getFloat(key + ".overtimeCoeff",
                prefs.getFloat("defaultCoeff", 1f));
        String gigFirm = prefs.getString(key + ".gigFirm", "");
        float gigAmount = prefs.getFloat(key + ".gigAmount", 0f);
        String comment = prefs.getString(key + ".comment", "");

        TextView info = label(
                "Основная смена: " + (shift ? "да" : "нет") +
                "\nПодработка: " + formatNumber(overtimeHours) + " ч., ×" + formatNumber(overtimeCoeff) +
                "\nХалтура: " + ((gigFirm == null || gigFirm.isEmpty()) ? "нет" : gigFirm) +
                (gigAmount > 0 ? " — " + money(gigAmount) : "") +
                "\nКомментарий: " + ((comment == null || comment.isEmpty()) ? "нет" : comment),
                14, Gravity.START);
        info.setPadding(0, 0, 0, dp(10));
        box.addView(info);

        Button shiftBtn = new Button(this);
        shiftBtn.setText(shift ? "Сделать выходным" : "Сделать рабочей сменой");
        shiftBtn.setOnClickListener(v -> {
            prefs.edit().putInt(key + ".shiftOverride", shift ? 0 : 1).apply();
            renderMonth();
            ((AlertDialog) findDialogParent(v)).dismiss();
        });
        box.addView(shiftBtn);

        Button autoBtn = new Button(this);
        autoBtn.setText("Вернуть по графику");
        autoBtn.setOnClickListener(v -> {
            prefs.edit().remove(key + ".shiftOverride").apply();
            renderMonth();
            ((AlertDialog) findDialogParent(v)).dismiss();
        });
        box.addView(autoBtn);

        Button overtimeBtn = new Button(this);
        overtimeBtn.setText("Подработка");
        overtimeBtn.setOnClickListener(v -> showOvertimeDialog(key));
        box.addView(overtimeBtn);

        Button gigBtn = new Button(this);
        gigBtn.setText("Халтура");
        gigBtn.setOnClickListener(v -> showGigDialog(key));
        box.addView(gigBtn);

        Button commentBtn = new Button(this);
        commentBtn.setText("Комментарий");
        commentBtn.setOnClickListener(v -> showCommentDialog(key));
        box.addView(commentBtn);

        String dateTitle = day + " " + monthName(shownMonth.get(Calendar.MONTH)) + " " +
                shownMonth.get(Calendar.YEAR);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(dateTitle)
                .setView(box)
                .setNegativeButton("Закрыть", null)
                .create();

        dialog.show();

        // Rebind buttons to this concrete dialog to avoid relying on view ancestry.
        shiftBtn.setOnClickListener(v -> {
            prefs.edit().putInt(key + ".shiftOverride", shift ? 0 : 1).apply();
            dialog.dismiss();
            renderMonth();
        });
        autoBtn.setOnClickListener(v -> {
            prefs.edit().remove(key + ".shiftOverride").apply();
            dialog.dismiss();
            renderMonth();
        });
        overtimeBtn.setOnClickListener(v -> {
            dialog.dismiss();
            showOvertimeDialog(key);
        });
        gigBtn.setOnClickListener(v -> {
            dialog.dismiss();
            showGigDialog(key);
        });
        commentBtn.setOnClickListener(v -> {
            dialog.dismiss();
            showCommentDialog(key);
        });
    }

    private Object findDialogParent(View v) {
        return null;
    }

    private String monthName(int month) {
        String[] months = new DateFormatSymbols(RU).getMonths();
        return months[month];
    }

    private EditText numericInput(String hint, float existing) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (existing > 0f) e.setText(formatNumber(existing));
        return e;
    }

    private float parseFloat(EditText e, float fallback) {
        try {
            String s = e.getText().toString().trim().replace(',', '.');
            if (s.isEmpty()) return fallback;
            return Float.parseFloat(s);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private void showOvertimeDialog(String key) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), 0, dp(22), 0);

        float oldHours = prefs.getFloat(key + ".overtimeHours", 0f);
        float oldCoeff = prefs.getFloat(key + ".overtimeCoeff",
                prefs.getFloat("defaultCoeff", 1f));

        EditText hours = numericInput("Часы подработки", oldHours);
        EditText coeff = numericInput("Коэффициент", oldCoeff);
        box.addView(hours);
        box.addView(coeff);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Подработка")
                .setView(box)
                .setPositiveButton("Сохранить", null)
                .setNeutralButton("Удалить", null)
                .setNegativeButton("Отмена", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                float h = parseFloat(hours, 0f);
                float c = parseFloat(coeff, prefs.getFloat("defaultCoeff", 1f));
                prefs.edit()
                        .putFloat(key + ".overtimeHours", Math.max(0f, h))
                        .putFloat(key + ".overtimeCoeff", Math.max(0f, c))
                        .apply();
                dialog.dismiss();
                renderMonth();
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                prefs.edit()
                        .remove(key + ".overtimeHours")
                        .remove(key + ".overtimeCoeff")
                        .apply();
                dialog.dismiss();
                renderMonth();
            });
        });

        dialog.show();
    }

    private void showGigDialog(String key) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), 0, dp(22), 0);

        EditText firm = new EditText(this);
        firm.setHint("Название фирмы");
        firm.setText(prefs.getString(key + ".gigFirm", ""));

        EditText amount = numericInput("Оплата (необязательно)",
                prefs.getFloat(key + ".gigAmount", 0f));

        box.addView(firm);
        box.addView(amount);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Халтура")
                .setView(box)
                .setPositiveButton("Сохранить", null)
                .setNeutralButton("Удалить", null)
                .setNegativeButton("Отмена", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = firm.getText().toString().trim();
                if (name.isEmpty()) {
                    firm.setError("Укажи название фирмы");
                    return;
                }
                prefs.edit()
                        .putString(key + ".gigFirm", name)
                        .putFloat(key + ".gigAmount", Math.max(0f, parseFloat(amount, 0f)))
                        .apply();
                dialog.dismiss();
                renderMonth();
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                prefs.edit()
                        .remove(key + ".gigFirm")
                        .remove(key + ".gigAmount")
                        .apply();
                dialog.dismiss();
                renderMonth();
            });
        });

        dialog.show();
    }

    private void showCommentDialog(String key) {
        EditText input = new EditText(this);
        input.setMinLines(3);
        input.setGravity(Gravity.TOP);
        input.setHint("Комментарий к этому дню");
        input.setText(prefs.getString(key + ".comment", ""));

        new AlertDialog.Builder(this)
                .setTitle("Комментарий")
                .setView(input)
                .setPositiveButton("Сохранить", (d, w) -> {
                    prefs.edit().putString(key + ".comment",
                            input.getText().toString().trim()).apply();
                    renderMonth();
                })
                .setNeutralButton("Удалить", (d, w) -> {
                    prefs.edit().remove(key + ".comment").apply();
                    renderMonth();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private int countShiftsForMonth(int year, int monthZeroBased) {
        Calendar temp = Calendar.getInstance();
        temp.set(year, monthZeroBased, 1);
        int days = temp.getActualMaximum(Calendar.DAY_OF_MONTH);
        int count = 0;
        for (int d = 1; d <= days; d++) {
            String key = dateKey(year, monthZeroBased, d);
            if (isShift(key)) count++;
        }
        return count;
    }

    private float overtimeCost(String key, float hours, float coeff) {
        if (hours <= 0f) return 0f;
        Calendar c = parseDate(key);
        int shifts = countWeekDaysForMonth(c.get(Calendar.YEAR), c.get(Calendar.MONTH));
        float salary = prefs.getFloat("salary", 0f);
        if (salary <= 0f || shifts <= 0) return 0f;

        float dayRate = salary / shifts;
        float hourRate = dayRate / 8f;
        return hourRate * coeff * hours;
    }

    private void renderSummary() {
        int year = shownMonth.get(Calendar.YEAR);
        int month = shownMonth.get(Calendar.MONTH);
        int shifts = countShiftsForMonth(year, month);

        Calendar temp = (Calendar) shownMonth.clone();
        int days = temp.getActualMaximum(Calendar.DAY_OF_MONTH);

        float overtimeHours = 0f;
        float overtimeMoney = 0f;
        float gigMoney = 0f;
        int gigCount = 0;

        for (int d = 1; d <= days; d++) {
            String key = dateKey(year, month, d);
            float h = prefs.getFloat(key + ".overtimeHours", 0f);
            float coeff = prefs.getFloat(key + ".overtimeCoeff",
                    prefs.getFloat("defaultCoeff", 1f));
            overtimeHours += h;
            overtimeMoney += overtimeCost(key, h, coeff);

            String firm = prefs.getString(key + ".gigFirm", "");
            if (firm != null && !firm.trim().isEmpty()) {
                gigCount++;
                gigMoney += prefs.getFloat(key + ".gigAmount", 0f);
            }
        }

        summary.setText(
                "Смен: " + shifts +
                "   •   Подработка: " + formatNumber(overtimeHours) + " ч. / " + money(overtimeMoney) +
                "\nХалтур: " + gigCount + " / " + money(gigMoney) +
                "   •   Доп. доход: " + money(overtimeMoney + gigMoney)
        );
    }

    private String formatNumber(float value) {
        if (Math.abs(value - Math.round(value)) < 0.0001f) {
            return String.valueOf(Math.round(value));
        }
        return String.format(RU, "%.2f", value);
    }

    private String money(float value) {
        return String.format(RU, "%,.2f ₽", value);
    }

    private void showSettingsDialog() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(4), dp(22), dp(12));
        scroll.addView(box);

        EditText salary = numericInput("Оклад", prefs.getFloat("salary", 0f));
        EditText coeff = numericInput("Коэффициент по умолчанию",
                prefs.getFloat("defaultCoeff", 1f));

        EditText work = numericInput("Рабочих дней в цикле", prefs.getInt("cycleWork", 2));
        EditText off = numericInput("Выходных дней в цикле", prefs.getInt("cycleOff", 2));

        EditText anchor = new EditText(this);
        anchor.setHint("Дата первой рабочей смены: ГГГГ-ММ-ДД");
        anchor.setText(prefs.getString("anchorDate", ""));

        Button pickDate = new Button(this);
        pickDate.setText("Выбрать дату начала");
        pickDate.setOnClickListener(v -> {
            Calendar c = parseDate(anchor.getText().toString());
            DatePickerDialog picker = new DatePickerDialog(
                    this,
                    (DatePicker view, int year, int month, int day) ->
                            anchor.setText(dateKey(year, month, day)),
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
            );
            picker.show();
        });

        box.addView(sectionTitle("Расчёт"));
        box.addView(salary);
        box.addView(coeff);

        TextView formula = label(
                "Формула: оклад ÷ число основных смен этого месяца ÷ 8 × коэффициент × часы.",
                12, Gravity.START);
        formula.setTextColor(Color.GRAY);
        formula.setPadding(0, 0, 0, dp(8));
        box.addView(formula);

        box.addView(sectionTitle("График"));
        box.addView(work);
        box.addView(off);
        box.addView(anchor);
        box.addView(pickDate);

        box.addView(sectionTitle("Цвета"));

        Button shiftColor = colorButton("Основная смена", "colorShift", paletteColors[0]);
        Button overtimeColor = colorButton("Подработка", "colorOvertime", paletteColors[2]);
        Button gigColor = colorButton("Халтура", "colorGig", paletteColors[3]);

        box.addView(shiftColor);
        box.addView(overtimeColor);
        box.addView(gigColor);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Настройки")
                .setView(scroll)
                .setPositiveButton("Сохранить", null)
                .setNegativeButton("Отмена", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    int w = Math.max(1, Math.round(parseFloat(work, 2f)));
                    int o = Math.max(0, Math.round(parseFloat(off, 2f)));
                    String a = anchor.getText().toString().trim();

                    if (!a.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        anchor.setError("Формат даты: ГГГГ-ММ-ДД");
                        return;
                    }

                    prefs.edit()
                            .putFloat("salary", Math.max(0f, parseFloat(salary, 0f)))
                            .putFloat("defaultCoeff", Math.max(0f, parseFloat(coeff, 1f)))
                            .putInt("cycleWork", w)
                            .putInt("cycleOff", o)
                            .putString("anchorDate", a)
                            .apply();

                    dialog.dismiss();
                    renderMonth();
                }));

        dialog.show();
    }

    private TextView sectionTitle(String text) {
        TextView v = label(text, 16, Gravity.START);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setPadding(0, dp(10), 0, dp(4));
        return v;
    }

    private Button colorButton(String title, String prefKey, int fallback) {
        Button b = new Button(this);
        b.setText(title + " — выбрать цвет");
        b.setBackgroundColor(prefs.getInt(prefKey, fallback));
        b.setOnClickListener(v -> {
            int current = prefs.getInt(prefKey, fallback);
            int selected = 0;
            for (int i = 0; i < paletteColors.length; i++) {
                if (paletteColors[i] == current) {
                    selected = i;
                    break;
                }
            }
            final int[] choice = {selected};
            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setSingleChoiceItems(paletteNames, selected, (d, which) -> choice[0] = which)
                    .setPositiveButton("Выбрать", (d, which) -> {
                        int c = paletteColors[choice[0]];
                        prefs.edit().putInt(prefKey, c).apply();
                        b.setBackgroundColor(c);
                        renderMonth();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });
        return b;
    }
}
