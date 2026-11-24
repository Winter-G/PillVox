package com.wynter.pillvox.feedback;


/** The three app languages. "code" is the standard language code Android's text-to-speech understands. */
public enum Lang {
    EN("en"), SI("si"), TA("ta");
    public final String code;
    Lang(String code) { this.code = code; }
}

