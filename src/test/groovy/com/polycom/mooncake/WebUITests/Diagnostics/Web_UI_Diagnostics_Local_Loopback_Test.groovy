package com.polycom.mooncake.WebUITests.Diagnostics

import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import com.polycom.honeycomb.moonCake.webui.page.DiagnosticsPages.LocalLoopbackTestPage
import com.polycom.mooncake.WebUITests.MoonCakeUITestSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared

/**
 * Created by Ryan Qi on 6/07/2019
 */
class Web_UI_Diagnostics_Local_Loopback_Test extends MoonCakeUITestSpec{
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_Diagnostics_Local_Loopback_Test.class)

    def setupSpec() {
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage

        deviceStatusPage.showDiagnosticsSubMenu()
        deviceStatusPage.clickDiagnosticsSubMenu("LocalLoopbackTest")
    }

    def "Test page function in local loop back test Page"() {
        when: "Open local loop back test page"
        LocalLoopbackTestPage localLoopbackTestPage = browser.at LocalLoopbackTestPage

        then: "Check button is displayed"
        localLoopbackTestPage.getStartButton().isDisplayed()
        localLoopbackTestPage.getStopButton().isDisplayed()
    }
}
