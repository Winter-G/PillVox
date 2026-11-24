package com.wynter.pillvox.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/** The database itself. Example: PillDao dao = PillDb.get(context).dao();  (one shared instance) */
@Database(entities = {Medicine.class, Schedule.class, DoseLog.class}, version = 1, exportSchema = false)
public abstract class PillDb extends RoomDatabase {
    public abstract PillDao dao();

    private static volatile PillDb instance;

    public static PillDb get(Context c) {
        if (instance == null) {
            synchronized (PillDb.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(c.getApplicationContext(), PillDb.class, "pillvox.db").build();
                }
            }
        }
        return instance;
    }
}
