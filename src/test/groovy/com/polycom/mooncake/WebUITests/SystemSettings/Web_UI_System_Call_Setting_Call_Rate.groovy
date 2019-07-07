package com.polycom.mooncake.WebUITests.SystemSettings

import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import com.polycom.honeycomb.moonCake.webui.page.SystemSettingsPages.CallSettingsPage
import com.polycom.mooncake.WebUITests.MoonCakeUITestSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared
import spock.lang.Unroll

class Web_UI_System_Call_Setting_Call_Rate extends MoonCakeUITestSpec {
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_System_Call_Setting_Call_Rate.class)

    def setupSpec() {
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage

        deviceStatusPage.showSystemSettingsSubMenu()
        deviceStatusPage.clickSystemSettingsSubMenu("CallSettings")

    }

    @Unroll
    def "Test call rate #callRate on Web UI"() {
        when: "Open call rate setting page"
        CallSettingsPage callSettingsPage = browser.at CallSettingsPage

        and: "Set call rate"
        callSettingsPage.getCallRate().selected = "number:"+ callRate

        then: "Check value"
        callSettingsPage.getCallRate().selectedText == callRateOnWeb

        where:
        callRate | callRateOnWeb | _
        "64"     | "Audio Only"  | _
        "256"    | "256Kbps"     | _
        "384"    | "384Kbps"     | _
        "512"    | "512Kbps"     | _
        "768"    | "768Kbps"     | _
        "1024"   | "1024Kbps"    | _
        "1536"   | "1536Kbps"    | _
        "2048"   | "2048Kbps"    | _
        "3072"   | "3072Kbps"    | _
        "4096"   | "4096Kbps"    | _
    }
}
