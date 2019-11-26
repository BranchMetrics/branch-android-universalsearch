package io.branch.search.widget.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import io.branch.search.widget.model.App;

@Dao
public interface AppsDao {

    @Query("SELECT * FROM apps ORDER BY interactions DESC, popularity DESC, label ASC")
    List<App> getAll();

    @Query("SELECT COUNT(*) FROM apps")
    int getCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void update(App item);

    @Delete
    void delete(App item);
}
