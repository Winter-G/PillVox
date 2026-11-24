package com.wynter.pillvox.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * DATA LAYER: Room is a friendly wrapper around the phone's built-in SQLite database, so everything is
 * stored ON THE PHONE (that is what makes PillVox work offline).
 * Three tables: medicines (the local pill DB), schedules (take X at HH:MM daily), dose_log (confirmed doses).
 *
 * A Medicine row is what the matcher compares a scanned pill against.
 */
@Entity(tableName = "medicines")
public class Medicine {
    @PrimaryKey(autoGenerate = true) public long id;
    public String name, strength, form;          // e.g. "Demo Medicine A", "500 mg", "tablet"
    public String shape, colour, imprint;        // what the camera should see on the pill

    public Medicine() { }                        // Room uses this one

    @Ignore
    public Medicine(String name, String strength, String form, String shape, String colour, String imprint) {
        this.name = name; this.strength = strength; this.form = form;
        this.shape = shape; this.colour = colour; this.imprint = imprint;
    }
}

