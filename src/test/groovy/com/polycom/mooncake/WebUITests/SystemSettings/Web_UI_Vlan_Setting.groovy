package com.polycom.mooncake.WebUITests.SystemSettings

import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import com.polycom.honeycomb.moonCake.webui.page.SystemSettingsPages.NetworkSettingsPage
import com.polycom.mooncake.WebUITests.MoonCakeUITestSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by Ryan Qi on 6/10/2019
 */
class Web_UI_Vlan_Setting extends MoonCakeUITestSpec {
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_Vlan_Setting.class)

    def setupSpec() {
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage

        deviceStatusPage.showSystemSettingsSubMenu()
        deviceStatusPage.clickSystemSettingsSubMenu("NetworkSetting")
    }

    @Unroll
    def "Vlan #modeOnWeb test"() {
        when: "Open network setting page"
        NetworkSettingsPage networkSettingsPage = browser.at NetworkSettingsPage

        and: "Set Vlan mode"
        networkSettingsPage.getVlanSettings().selected = mode

        then: "Check value"
        networkSettingsPage.getVlanSettings().selectedText == modeOnWeb

        where:
        mode       | modeOnWeb | _
        "number:0" | "Disable" | _
        "number:1" | "LLDP"    | _
        "number:2" | "Static"  | _
    }

    def "visitable test of Vlan related control"() {
        when: "Open network setting page"
        NetworkSettingsPage networkSettingsPage = browser.at NetworkSettingsPage

        and: "Set vlan mode as static"
        networkSettingsPage.getVlanSettings().selected = "number:2"

        then: "Check Vlan related controls are visitable"
        networkSettingsPage.getVlanID().isDisplayed()
        networkSettingsPage.getVideoPriority().isDisplayed()
        networkSettingsPage.getAudioPriority().isDisplayed()
        networkSettingsPage.getControlPriority().isDisplayed()
    }
}
