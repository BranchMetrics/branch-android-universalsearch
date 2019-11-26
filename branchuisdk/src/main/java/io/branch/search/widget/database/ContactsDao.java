package io.branch.search.widget.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import io.branch.search.widget.model.Contact;

@Dao
public interface ContactsDao {

    @Query("SELECT * FROM contacts ORDER BY interactions DESC, displayName ASC")
    List<Contact> getAll();

    @Query("SELECT COUNT(*) FROM contacts")
    int getCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void update(Contact item);

    @Delete
    void delete(Contact item);

}
