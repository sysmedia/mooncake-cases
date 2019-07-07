package com.polycom.mooncake.WebUITests.Diagnostics

import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import com.polycom.honeycomb.moonCake.webui.page.DiagnosticsPages.ColorBarTestPage
import com.polycom.mooncake.WebUITests.MoonCakeUITestSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared

/**
 * Created by Ryan Qi on 6/07/2019
 */
class Web_UI_Diagnostics_ColorBar_Test extends MoonCakeUITestSpec{
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_Diagnostics_AudioMeter_Test.class)

    def setupSpec() {
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage

        deviceStatusPage.showDiagnosticsSubMenu()
        deviceStatusPage.clickDiagnosticsSubMenu("ColorBarTest")
    }

    def "Test page function in color bar test Page"() {
        when: "Open color bar test page"
        ColorBarTestPage colorBarTestPage = browser.at ColorBarTestPage

        then: "Check button is displayed"
        colorBarTestPage.getStartButton().isDisplayed()
        colorBarTestPage.getStopButton().isDisplayed()
    }
}
