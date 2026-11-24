package com.wynter.pillvox.feedback;

import com.wynter.pillvox.ml.Issue;

/**
 * Every spoken sentence in English, Sinhala and Tamil, in ONE place so it is easy to proof-read.
 * Example: Msg.FINGER.get(Lang.SI) -> the Sinhala sentence.  Have native speakers review these!
 */
public enum Msg {
    OFF_CENTRE("Please move the pill towards the centre of the camera.", "කරුණාකර පෙත්ත කැමරාවේ මැදට ගෙන එන්න.", "மாத்திரையை கேமராவின் நடுவில் வையுங்கள்."),
    FINGER("Please move your finger away from the pill.", "කරුණාකර ඇඟිල්ල පෙත්තෙන් ඉවතට ගන්න.", "விரலை மாத்திரையிலிருந்து நகர்த்துங்கள்."),
    IMPRINT_HIDDEN("Please turn the pill so the printed markings face the camera.", "කරුණාකර මුද්‍රිත ලකුණු කැමරාව දෙසට පෙනෙන සේ පෙත්ත හරවන්න.", "அச்சிடப்பட்ட குறியீடுகள் கேமராவை நோக்கும்படி மாத்திரையைத் திருப்புங்கள்."),
    BLURRY("Please hold the camera steady.", "කරුණාකර කැමරාව ස්ථාවරව තබාගන්න.", "கேமராவை அசையாமல் பிடியுங்கள்."),
    DARK("Please move to a brighter area.", "කරුණාකර වඩා ආලෝකමත් තැනකට යන්න.", "அதிக வெளிச்சமான இடத்திற்குச் செல்லுங்கள்."),
    DAMAGED("This pill may be damaged. Please do not take it based on this scan.", "මේ පෙත්ත හානි වී තිබිය හැක. කරුණාකර මෙම ස්කෑන් එක පදනම්ව එය ගන්න එපා.", "இந்த மாத்திரை சேதமடைந்திருக்கலாம். இந்த ஸ்கேனை நம்பி இதை உட்கொள்ள வேண்டாம்."),
    TIME("It is time to take your medicine.", "ඖෂධය ගැනීමට කාලයයි.", "மருந்து உட்கொள்ளும் நேரம் இது."),
    ASK("Have you taken your medicine?", "ඔබ ඖෂධය ගත්තාද?", "நீங்கள் மருந்தை எடுத்துக்கொண்டீர்களா?"),
    RECORDED("Dose recorded.", "මාත්‍රාව සටහන් කළා.", "மருந்தளவு பதிவு செய்யப்பட்டது."),
    ALREADY("This dose is already recorded.", "මෙම මාත්‍රාව දැනටමත් සටහන් කර ඇත.", "இந்த மருந்தளவு ஏற்கனவே பதிவாகிவிட்டது.");

    private final String en, si, ta;
    Msg(String en, String si, String ta) { this.en = en; this.si = si; this.ta = ta; }

    public String get(Lang l) { return l == Lang.SI ? si : l == Lang.TA ? ta : en; }

    /** Maps a detected problem to the sentence we speak for it. */
    public static Msg forIssue(Issue i) {
        switch (i) {
            case OFF_CENTRE: return OFF_CENTRE;
            case FINGER: return FINGER;
            case BLURRY: return BLURRY;
            case DARK: return DARK;
            case DAMAGED: return DAMAGED;
            default: return IMPRINT_HIDDEN;
        }
    }
}

