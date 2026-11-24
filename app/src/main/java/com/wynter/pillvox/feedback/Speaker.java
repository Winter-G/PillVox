package com.wynter.pillvox.feedback;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

/** Android's built-in text-to-speech. Works offline if that language's voice is installed on the phone. */
public class Speaker {
    private boolean ready = false;   // the engine starts asynchronously; flips to true when initialised
    private final TextToSpeech tts;

    public Speaker(Context context) {
        tts = new TextToSpeech(context.getApplicationContext(), status -> ready = (status == TextToSpeech.SUCCESS));
    }

    /**
     * Speaks the text. Returns FALSE if the engine or that language's voice is missing; MainActivity then
     * shows a "voice not installed" banner (Sinhala/Tamil voices are often missing on low-end phones).
     * Planned fallback: pre-recorded audio clips for the fixed sentences in Msg.
     * Example: boolean ok = speaker.speak(Msg.FINGER.get(Lang.SI), Lang.SI);
     */
    public boolean speak(String text, Lang lang) {
        if (!ready) return false;
        int r = tts.setLanguage(Locale.forLanguageTag(lang.code));
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) return false;
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "pillvox");
        return true;
    }

    public void shutdown() { tts.stop(); tts.shutdown(); }
}
