package com.polycom.mooncake.WebUITests.SystemSettings

import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import com.polycom.honeycomb.moonCake.webui.page.SystemSettingsPages.GeneralPage
import com.polycom.mooncake.WebUITests.MoonCakeUITestSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared
import spock.lang.Unroll


/**
 * Created by qxu on 5/22/2019
 */
class Web_UI_System_General extends MoonCakeUITestSpec {
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_System_General.class)

    def setupSpec() {
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage

        deviceStatusPage.showSystemSettingsSubMenu()
        deviceStatusPage.clickSystemSettingsSubMenu("General")
    }

    @Unroll
    def "Set Time Before Sleep to #timeOnWeb on WebUI"() {
        when: "Open General page"
        GeneralPage generalPage = browser.at GeneralPage

        and: "set Time"
        generalPage.getTimeBeforeSleep().selected = "number:" + time
        pauseTest(2)

        then: "check Time"
        generalPage.getTimeBeforeSleep().selectedText == timeOnWeb

        where:
        time      | timeOnWeb    | _
        "1800005" | "Off"        | _
        "120000"  | "2 minutes"  | _
        "300000"  | "5 minutes"  | _
        "900000"  | "15 minutes" | _
        "1800000" | "30 minutes" | _
    }

    @Unroll
    def "Set Anti-Flicker to #antiFlickerOnWeb on WebUI"() {
        when: "Open General page"
        GeneralPage generalPage = browser.at GeneralPage

        and: "set Anti-Flicker"
        generalPage.getAntiFlicker().selected = "number:" + antiFlicker
        pauseTest(2)

        then: "check Anti-Flicker"
        generalPage.getAntiFlicker().selectedText == antiFlickerOnWeb

        where:
        antiFlicker | antiFlickerOnWeb | _
        "1"         | "50Hz"           | _
        "2"         | "60Hz"           | _
    }
}
