package com.polycom.mooncake.WebUITests.Diagnostics

import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import com.polycom.honeycomb.moonCake.webui.page.DiagnosticsPages.SystemLogPage
import com.polycom.mooncake.WebUITests.MoonCakeUITestSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared

/**
 * Created by Ryan Qi on 6/07/2019
 */
class Web_UI_Diagnostics_Log extends MoonCakeUITestSpec{
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_Diagnostics_Log.class)

    def setupSpec() {
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage

        deviceStatusPage.showDiagnosticsSubMenu()
        deviceStatusPage.clickDiagnosticsSubMenu("SystemLog")
    }

    def "Test page function in system log Page"() {
        when: "Open system log page"
        SystemLogPage systemLogPage = browser.at SystemLogPage

        then: "Check button is displayed"
        systemLogPage.getExportButton().isDisplayed()
    }
}
