package com.polycom.mooncake.WebUITests.SystemSettings


import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import com.polycom.honeycomb.moonCake.webui.page.SystemSettingsPages.CallSettingsPage
import com.polycom.mooncake.WebUITests.MoonCakeUITestSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared
import spock.lang.Unroll

class Web_UI_System_Call_Setting_Auto_Answer extends MoonCakeUITestSpec {
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_System_Call_Setting_Auto_Answer.class)

    def setupSpec() {
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage

        deviceStatusPage.showSystemSettingsSubMenu()
        deviceStatusPage.clickSystemSettingsSubMenu("CallSettings")

    }

    @Unroll
    def "Test auto answer #mode on WebUI"() {
        when: "Open call rate setting page"
        CallSettingsPage callSettingsPage = browser.at CallSettingsPage

        and: "Set auto answer mode"
        callSettingsPage.getAutoAnswer().selected = "boolean:" + mode

        then: "Check value"
        callSettingsPage.getAutoAnswer().selectedText == modeOnWeb

        where:
        mode    | modeOnWeb | _
        "true"  | "Enable"  | _
        "false" | "Disable" | _
    }
}
