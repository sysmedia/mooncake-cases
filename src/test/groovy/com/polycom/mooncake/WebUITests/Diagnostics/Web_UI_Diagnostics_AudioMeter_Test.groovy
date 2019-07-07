package com.polycom.mooncake.WebUITests.Diagnostics

import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import com.polycom.honeycomb.moonCake.webui.page.DiagnosticsPages.AudioMeterTestPage
import com.polycom.mooncake.WebUITests.MoonCakeUITestSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared

/**
 * Created by Ryan Qi on 6/07/2019
 */
class Web_UI_Diagnostics_AudioMeter_Test extends MoonCakeUITestSpec{
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_Diagnostics_AudioMeter_Test.class)

    def setupSpec() {
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage

        deviceStatusPage.showDiagnosticsSubMenu()
        deviceStatusPage.clickDiagnosticsSubMenu("AudioMeterTest")
    }

    def "Test page function in audio meter test Page"() {
        when: "Open audio meter test page"
        AudioMeterTestPage audioMeterTestPage = browser.at AudioMeterTestPage

        then: "Check button is displayed"
        audioMeterTestPage.getStartButton().isDisplayed()
        audioMeterTestPage.getStopButton().isDisplayed()
    }
}
