package com.polycom.mooncake.WebUITests.SystemSettings

import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import com.polycom.honeycomb.moonCake.webui.page.SystemSettingsPages.CallSettingsPage
import com.polycom.mooncake.WebUITests.MoonCakeUITestSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared
import spock.lang.Unroll

class Web_UI_System_Call_Setting_Other extends MoonCakeUITestSpec {
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_System_Call_Setting_Other.class)

    def setupSpec() {
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage

        deviceStatusPage.showSystemSettingsSubMenu()
        deviceStatusPage.clickSystemSettingsSubMenu("CallSettings")
    }

    @Unroll
    def "Test Noise Block #mode on WebUI"() {
        when: "Open call rate setting page"
        CallSettingsPage callSettingsPage = browser.at CallSettingsPage

        and: "Set Noise Block mode"
        callSettingsPage.getNoiseBlock().selected = "boolean:" + mode

        then: "Check value"
        callSettingsPage.getNoiseBlock().selectedText == modeOnWeb

        where:
        mode    | modeOnWeb | _
        "true"  | "Enable"  | _
        "false" | "Disable" | _
    }

    @Unroll
    def "Test Encryption Mode #mode on WebUI"() {
        when: "Open call rate setting page"
        CallSettingsPage callSettingsPage = browser.at CallSettingsPage

        and: "Set Encryption Mode"
        callSettingsPage.getEncryptionMode().selected = "string:" + mode

        then: "Check the value"
        callSettingsPage.getEncryptionMode().selectedText == modeOnWeb

        where:
        mode   | modeOnWeb | _
        "auto" | "Auto"    | _
        "on"   | "On"      | _
        "off"  | "Off"     | _
    }
}
