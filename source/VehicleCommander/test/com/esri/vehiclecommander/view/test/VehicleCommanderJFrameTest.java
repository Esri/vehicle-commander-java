package com.esri.vehiclecommander.view.test;

import com.esri.runtime.ArcGISRuntime;
import com.esri.client.local.LocalServer;
import com.esri.client.local.LocalServerStatus;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * VehicleCommanderJFrame unit tests.
 */
public class VehicleCommanderJFrameTest {

    public VehicleCommanderJFrameTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        LocalServer.getInstance().shutdown();
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test to make sure we're not publishing a valid license string with this source
     * code.
     */
    @Test
    public void testLicenseInvalid() {
        ArcGISRuntime.setLicense(VehicleCommanderJFrame.BUILT_IN_LICENSE_STRING);
        ArcGISRuntime.initialize();
        if (ArcGISRuntime.isLicensed()) {
            fail("Warning: your code contains a valid license file!");
        }
    }

}