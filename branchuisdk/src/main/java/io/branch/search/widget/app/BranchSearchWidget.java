package io.branch.search.widget.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Keep;
import android.widget.RemoteViews;

import io.branch.search.widget.R;

/**
 * Implementation of App Widget functionality.
 * Need to @Keep this since it can be declared in the hosting app manifest.
 */
@Keep
public class BranchSearchWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context,
                                AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.branchapp_widget);

        Intent intentUpdate = new Intent(context, BranchSearchActivity.class);
        intentUpdate.setAction(Intent.ACTION_MAIN);

        PendingIntent pendingUpdate = PendingIntent.getActivity(context,
                0, intentUpdate, 0);

        // Assign the pending intent to the button onClick handler
        views.setOnClickPendingIntent(R.id.appwidget_view, pendingUpdate);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }
}

