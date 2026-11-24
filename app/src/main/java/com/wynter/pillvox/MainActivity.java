package com.wynter.pillvox;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.wynter.pillvox.data.*;
import com.wynter.pillvox.domain.DoseRules;
import com.wynter.pillvox.domain.DoseRules.DoseResult;
import com.wynter.pillvox.domain.DoseRules.DoseStatus;
import com.wynter.pillvox.feedback.*;
import com.wynter.pillvox.ml.*;
import com.wynter.pillvox.reminders.ReminderScheduler;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MAIN ACTIVITY = the "conductor". It connects the pieces:
 *   screens (res/layout)  ->  ScanPipeline (ml)  ->  Speaker + Haptics (feedback)  ->  database + reminders
 *
 * Typical flow: tap "Scan pill" -> pipeline returns a ScanOutcome -> we vibrate, SPEAK the result in the
 * chosen language automatically, and show the result screen. "Read aloud again" repeats it.
 *
 * THREADING RULE (the one Java/Android rule to remember): database work runs on a background thread
 * ("io.execute(...)"), and anything that touches the screen runs on the main thread ("runOnUiThread(...)").
 */
public class MainActivity extends AppCompatActivity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private PillDao dao;
    private Speaker speaker;
    private ScanPipeline pipeline;

    private Lang lang = Lang.EN;
    private ScanOutcome outcome;     // the latest scan result (null until the first scan)

    private View screenScan, screenResult, screenReminders;
    private PreviewView previewView;
    private TextView cameraMsg, voiceBanner, resultTitle, resultBody, resultInstruction, resultNote;
    private LinearLayout resultCard, doseList;
    private Button btnEn, btnSi, btnTa;

    // Asks for camera + notification permission. The popup text comes from Android itself.
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), r -> updateCamera());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dao = PillDb.get(this).dao();
        speaker = new Speaker(this);
        // --- Swap this ONE line for new TflitePillAnalyzer(...) once your trained model exists ---
        pipeline = new ScanPipeline(new FakeAnalyzer(), dao);

        screenScan = findViewById(R.id.screenScan);
        screenResult = findViewById(R.id.screenResult);
        screenReminders = findViewById(R.id.screenReminders);
        previewView = findViewById(R.id.previewView);
        cameraMsg = findViewById(R.id.cameraMsg);
        voiceBanner = findViewById(R.id.voiceBanner);
        resultCard = findViewById(R.id.resultCard);
        resultTitle = findViewById(R.id.resultTitle);
        resultBody = findViewById(R.id.resultBody);
        resultInstruction = findViewById(R.id.resultInstruction);
        resultNote = findViewById(R.id.resultNote);
        doseList = findViewById(R.id.doseList);
        btnEn = findViewById(R.id.btnEn); btnSi = findViewById(R.id.btnSi); btnTa = findViewById(R.id.btnTa);

        btnEn.setOnClickListener(v -> setLang(Lang.EN));
        btnSi.setOnClickListener(v -> setLang(Lang.SI));
        btnTa.setOnClickListener(v -> setLang(Lang.TA));
        findViewById(R.id.navScan).setOnClickListener(v -> show(screenScan));
        findViewById(R.id.navReminders).setOnClickListener(v -> { show(screenReminders); refreshReminders(); });
        findViewById(R.id.btnScan).setOnClickListener(v -> doScan());
        findViewById(R.id.btnScanAgain).setOnClickListener(v -> show(screenScan));
        findViewById(R.id.btnReadAgain).setOnClickListener(v -> { if (outcome != null) announce(outcome); });
        findViewById(R.id.btnAskNow).setOnClickListener(v -> askDoseQuestion());
        findViewById(R.id.btnAddDemo).setOnClickListener(v -> addDemoReminder());

        setLang(Lang.EN);
        show(screenScan);
        updateCamera();
        permissionLauncher.launch(new String[]{Manifest.permission.CAMERA, Manifest.permission.POST_NOTIFICATIONS});

        io.execute(this::seedDemo);

        // Opened by tapping a dose notification: go to Reminders and ask "Have you taken it?"
        if (getIntent().getLongExtra("scheduleId", -1) != -1) {
            show(screenReminders); refreshReminders(); askDoseQuestion();
        }
    }

    @Override
    protected void onDestroy() { speaker.shutdown(); io.shutdown(); super.onDestroy(); }

    // ---------- screens ----------

    private void show(View screen) {
        screenScan.setVisibility(screen == screenScan ? View.VISIBLE : View.GONE);
        screenResult.setVisibility(screen == screenResult ? View.VISIBLE : View.GONE);
        screenReminders.setVisibility(screen == screenReminders ? View.VISIBLE : View.GONE);
    }

    private void setLang(Lang l) {
        lang = l;
        style(btnEn, l == Lang.EN); style(btnSi, l == Lang.SI); style(btnTa, l == Lang.TA);
        if (outcome != null && screenResult.getVisibility() == View.VISIBLE) renderResult();  // re-translate the instruction
    }

    /** Selected language = purple with white text; others = lavender with purple text. */
    private void style(Button b, boolean selected) {
        b.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, selected ? R.color.purple : R.color.lavender)));
        b.setTextColor(ContextCompat.getColor(this, selected ? R.color.white : R.color.purple));
    }

    // ---------- camera ----------

    private void updateCamera() {
        boolean ok = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        cameraMsg.setVisibility(ok ? View.GONE : View.VISIBLE);
        if (ok) startCamera();
    }

    /** Shows the live camera. For now it only DISPLAYS it; later you add an ImageAnalysis to feed frames to the model. */
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                provider.unbindAll();                                   // release any earlier use of the camera
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview);  // auto start/stop
            } catch (Exception e) { e.printStackTrace(); }
        }, ContextCompat.getMainExecutor(this));
    }

    // ---------- scanning ----------

    private void doScan() {
        io.execute(() -> {
            ScanOutcome o = pipeline.scan(null);   // real version: pass the current camera frame as a Bitmap
            runOnUiThread(() -> { outcome = o; announce(o); renderResult(); show(screenResult); });
        });
    }

    /** Vibrate + speak. This is the "automatic spoken output" from the spec. */
    private void announce(ScanOutcome o) {
        switch (o.type) {
            case MATCH:
                Haptics.play(this, Haptics.MATCH);
                say(o.medicine.name + ". " + o.medicine.strength + ". " + o.medicine.form + ".");
                break;
            case NOT_SURE:
                Haptics.play(this, Haptics.UNCERTAIN);
                say(Msg.forIssue(o.issue).get(lang));
                break;
            case DAMAGED:
                Haptics.play(this, Haptics.WARNING);
                say(Msg.DAMAGED.get(lang));
                break;
        }
    }

    /** Speak in the current language; show the banner if the phone has no voice for it. */
    private void say(String text) {
        boolean ok = speaker.speak(text, lang);
        voiceBanner.setVisibility(ok ? View.GONE : View.VISIBLE);
    }

    private void renderResult() {
        if (outcome == null) return;
        boolean warning = outcome.type != ScanOutcome.Type.MATCH;
        resultCard.setBackgroundResource(warning ? R.drawable.bg_card_warning : R.drawable.bg_card);
        switch (outcome.type) {
            case MATCH:
                Medicine m = outcome.medicine;
                resultTitle.setText("Match found");
                resultBody.setText("Medicine: " + m.name + "\nStrength: " + m.strength + "\nDosage form: " + m.form
                        + "\nShape: " + m.shape + "\nColour: " + m.colour + "\nImprint: " + m.imprint
                        + "\nConfidence: " + outcome.confidence + "%");
                resultInstruction.setText("");
                resultNote.setText("A visual match only. Check with your pharmacist if unsure.");
                break;
            case NOT_SURE:
                resultTitle.setText("I could not identify this pill");
                resultBody.setText("I will not guess. Please try again.");
                resultInstruction.setText(Msg.forIssue(outcome.issue).get(lang));
                resultNote.setText("");
                break;
            case DAMAGED:
                resultTitle.setText("This pill may be damaged");
                resultBody.setText("");
                resultInstruction.setText(Msg.DAMAGED.get(lang));
                resultNote.setText("This is not a safety check. Ask your pharmacist.");
                break;
        }
    }

    // ---------- reminders ----------

    private void askDoseQuestion() { say(Msg.TIME.get(lang) + " " + Msg.ASK.get(lang)); }

    /** Small holder for one reminder card. */
    private static class DoseRow { long id; int minutes; String title; DoseStatus status; }

    private void refreshReminders() {
        io.execute(() -> {
            String today = LocalDate.now().toString();
            List<Medicine> meds = dao.medicines();
            List<DoseLog> logs = dao.logsFor(today);
            List<DoseRow> rows = new ArrayList<>();
            for (Schedule s : dao.allSchedules()) {
                boolean taken = false;
                for (DoseLog l : logs) if (l.scheduleId == s.id && "TAKEN".equals(l.status)) taken = true;
                String name = "Medicine";
                for (Medicine m : meds) if (m.id == s.medicineId) name = m.name;
                DoseRow r = new DoseRow();
                r.id = s.id; r.minutes = s.hour * 60 + s.minute;
                r.title = String.format("%02d:%02d  %s", s.hour, s.minute, name);
                r.status = DoseRules.statusOf(s.hour, s.minute, taken, LocalTime.now());
                rows.add(r);
            }
            Collections.sort(rows, (a, b) -> Integer.compare(a.minutes, b.minutes));
            runOnUiThread(() -> renderReminders(rows));
        });
    }

    private void renderReminders(List<DoseRow> rows) {
        doseList.removeAllViews();
        for (DoseRow r : rows) {
            View card = getLayoutInflater().inflate(R.layout.item_dose, doseList, false);
            boolean due = r.status == DoseStatus.DUE;
            card.setBackgroundResource(due ? R.drawable.bg_card_warning : R.drawable.bg_card);
            ((TextView) card.findViewById(R.id.doseTitle)).setText(r.title);
            String label = r.status == DoseStatus.TAKEN ? "Taken" : due ? "Due now"
                    : r.status == DoseStatus.PENDING ? "Pending" : "Missed (unconfirmed)";
            ((TextView) card.findViewById(R.id.doseStatus)).setText(label);
            Button taken = card.findViewById(R.id.doseTaken);
            taken.setVisibility(due || r.status == DoseStatus.MISSED ? View.VISIBLE : View.GONE);
            taken.setOnClickListener(v -> confirmDose(r.id));
            doseList.addView(card);
        }
    }

    /** User says "Yes, I have taken it". The duplicate-dose block lives in DoseRules.markTaken. */
    private void confirmDose(long scheduleId) {
        io.execute(() -> {
            DoseResult res = DoseRules.markTaken(dao, scheduleId, LocalDate.now().toString());
            runOnUiThread(() -> {
                if (res == DoseResult.RECORDED) { Haptics.play(this, Haptics.MATCH); say(Msg.RECORDED.get(lang)); }
                else { Haptics.play(this, Haptics.WARNING); say(Msg.ALREADY.get(lang)); }
                refreshReminders();
            });
        });
    }

    /** Creates a real alarm one minute from now so you can watch the whole reminder flow. */
    private void addDemoReminder() {
        io.execute(() -> {
            LocalTime t = LocalTime.now().plusMinutes(1);
            Medicine med = dao.medicines().get(0);
            Schedule s = new Schedule(med.id, t.getHour(), t.getMinute());
            s.id = dao.addSchedule(s);                 // Room returns the new row id
            ReminderScheduler.schedule(this, s);
            runOnUiThread(this::refreshReminders);
        });
    }

    /** One demo medicine so a scan has something to match. Replace with real data (README, Next steps). */
    private void seedDemo() {
        if (dao.medicines().isEmpty())
            dao.addMedicine(new Medicine("Demo Medicine A", "500 mg", "tablet", "round", "white", "AB5"));
        // (FakeAnalyzer reads "AB 5", which normalises to "AB5" and still matches.)
    }
}
       });
    }
}