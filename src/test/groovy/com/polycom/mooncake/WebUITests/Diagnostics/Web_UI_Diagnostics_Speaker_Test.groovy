package com.polycom.mooncake.WebUITests.Diagnostics

import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import com.polycom.honeycomb.moonCake.webui.page.DiagnosticsPages.SpeakerTestPage
import com.polycom.mooncake.WebUITests.MoonCakeUITestSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared

/**
 * Created by Ryan Qi on 6/07/2019
 */
class Web_UI_Diagnostics_Speaker_Test extends MoonCakeUITestSpec{
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_Diagnostics_Speaker_Test.class)

    def setupSpec() {
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage

        deviceStatusPage.showDiagnosticsSubMenu()
        deviceStatusPage.clickDiagnosticsSubMenu("SpeakerTest")
    }

    def "Test page function in speaker test Page"() {
        when: "Open speaker test page"
        SpeakerTestPage speakerTestPage = browser.at SpeakerTestPage

        then: "Check button is displayed"
        speakerTestPage.getStartButton().isDisplayed()
        speakerTestPage.getStopButton().isDisplayed()
    }
}
