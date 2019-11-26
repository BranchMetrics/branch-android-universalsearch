package io.branch.search.widget.model;

import org.junit.Assert;

import org.json.JSONObject;
import org.junit.Test;

import java.util.List;

import io.branch.search.widget.BaseTest;
import io.branch.search.widget.AssetUtils;

public class AutoCompleteTest extends BaseTest {
    private static final String TAG = "Branch::Test";

    @Test
    public void testParseResults() throws Throwable {
        JSONObject object = AssetUtils.readJsonAsset(getTestContext(),
                "branch_autocomplete_success.json");
        Assert.assertNotNull(object);

        List<AutoComplete> result = AutoComplete.parseResults(object);
        Assert.assertTrue(result.size() > 0);
        for (AutoComplete item : result) {
            Assert.assertTrue(item.getName().length() > 0);
        }
    }
}

