package com.androidbook.triviaquiz22.test;

import android.test.AndroidTestCase;
import android.util.Log;

public class QuickTest extends AndroidTestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSimple() {
        Log.d("tst", "Simple Test");
        assertTrue("should be true", 1 == 1);
    }

}
