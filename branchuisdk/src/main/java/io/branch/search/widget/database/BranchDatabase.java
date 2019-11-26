package io.branch.search.widget.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import io.branch.search.widget.model.App;
import io.branch.search.widget.model.Contact;

@Database(
        entities = {App.class, Contact.class},
        exportSchema = false,
        version = 4)
public abstract class BranchDatabase extends RoomDatabase {

    public abstract AppsDao appItemDao();

    public abstract ContactsDao contactItemDao();
}
