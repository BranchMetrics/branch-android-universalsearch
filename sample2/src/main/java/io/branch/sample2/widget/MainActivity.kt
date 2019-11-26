package io.branch.sample2.widget

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import io.branch.search.widget.BranchSearchCallback
import io.branch.search.widget.BranchSearchController
import io.branch.search.widget.BranchSearchResultsView

class MainActivity : AppCompatActivity() {

    private lateinit var controller: BranchSearchController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val searchView = findViewById<EditText>(R.id.search)
        val resultsView = findViewById<BranchSearchResultsView>(R.id.results)

        // Initialize the Branch search controller with all of the above
        controller = BranchSearchController.init(supportFragmentManager, resultsView)
        controller.addCallback(object: BranchSearchCallback() {
            override fun onQueryUpdateRequested(newQuery: CharSequence) {
                searchView.setText(newQuery)
            }
        })
        controller.checkPermissions(this)

        // Make sure Branch search controller receives the editor action events
        searchView.setOnEditorActionListener { v, actionId, event ->
            controller.onEditorAction(v, actionId, event)
        }

        // Make sure Branch search controller receives the text change events
        searchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                controller.onTextChanged(s)
            }
        })

        // Make sure we start with a "" value
        if (savedInstanceState == null) {
            searchView.setText("")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        controller.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
